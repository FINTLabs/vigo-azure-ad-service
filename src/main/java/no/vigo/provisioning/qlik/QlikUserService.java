package no.vigo.provisioning.qlik;

import com.microsoft.graph.models.extensions.Group;
import com.microsoft.graph.models.extensions.Invitation;
import com.microsoft.graph.models.extensions.User;
import lombok.extern.slf4j.Slf4j;
import no.vigo.Props;
import no.vigo.azure.ad.AzureUserResponse;
import no.vigo.azure.ad.GroupService;
import no.vigo.azure.ad.UserService;
import no.vigo.notification.MailingService;
import no.vigo.notification.TemplateService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QlikUserService {

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;
    private final GroupService groupService;
    private final MailingService mailingService;
    private final TemplateService templateService;
    private final Props props;

    public QlikUserService(JdbcTemplate jdbcTemplate, UserService userService, GroupService groupService, MailingService mailingService, TemplateService templateService, Props props) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
        this.groupService = groupService;
        this.mailingService = mailingService;
        this.templateService = templateService;
        this.props = props;
    }

    @Scheduled(
            fixedDelayString = "${fint.azure.qlik.provisioning.fixed-delay:900000}",
            initialDelayString = "${fint.azure.qlik.provisioning.initial-delay:5000}"
    )
    public void synchronize() {
        if (props.getQlikSyncronizeUsers()) {
            log.info("Start provisioning users");
            log.debug("Fetching users from VIGOS");
            String sql = "select * from CBRUKER\n" +
                    "where ACCQVV = 'J'\n" +
                    "or ACCQVD = 'J'\n" +
                    "or ACCQSV = 'J'\n" +
                    "or ACCQSD = 'J'\n" +
                    "or ACCNPD = 'J'\n" +
                    "or ACCNPV = 'J'";
            List<QlikUser> qlikUsers = jdbcTemplate.query(sql, new QlikUserMapper());
            log.info("Found {} QLik users to provisioning", qlikUsers.size());
            qlikUsers.forEach(qlikUser -> {
                AzureUserResponse response = userService.usersExists(qlikUser.getAzureADUPN());

                if (response.notExists()) {
                    if (shouldUserExist(qlikUser)) {
                        if (!props.getDryRun()) makeAzureADUser(qlikUser);
                    }
                    return;
                }
                if (response.hasError()) return;

                response.withUser(user -> {
                    if (shouldUserExist(qlikUser)) {
                        log.trace("Updating user {}", qlikUser.getEmail());
                        userService.updateUser(getPatchedUser(qlikUser), qlikUser.getAzureADUPN());
                        User owner = userService.getUserIdByEmail(props.getQlikUsersOwner());
                        userService.setManager(user.id, owner.id);

                        List<String> hasGroups = getHasGroups(qlikUser);
                        List<String> neededGroups = getNeededGroups(qlikUser);

                        if (groupsNeedsUpdate(hasGroups, neededGroups)) {
                            log.debug("Groups needs to be updated");
                            addNeededGroups(hasGroups, neededGroups, user.id);
                            removeNotNeededGroups(hasGroups, neededGroups, user.id);
                        }
                    } else {
                        if (props.getUserDelete()) {
                            log.info("Deleting user {}", user.mail);
                            userService.deleteUser(user.id);
                        }
                    }
                });

            });
            log.info("End provisioning users");
        } else {
            log.info("Provisioning users is disabled.");
        }
    }

    public Invitation makeAzureADUser(QlikUser qLikUser) {
        log.info("Creating user {}", qLikUser.getEmail());

        Invitation invitation = new Invitation();
        invitation.invitedUserEmailAddress = qLikUser.getEmail().toLowerCase();
        invitation.sendInvitationMessage = false;
        invitation.inviteRedirectUrl = props.getQlikRedirectUrl();
        invitation.invitedUserDisplayName = String.format("%s %s", qLikUser.getFirstName(), qLikUser.getLastName());

        return userService.invite(invitation, props.getQlikUsersOwner());
    }

    public Invitation reInvite(String email) {
        Invitation invite = userService.reInvite(email, props.getQlikRedirectUrl());
        User user = userService.getUserIdByEmail(getUPNByEmail(email));
        notify(user.givenName, email, invite.inviteRedeemUrl);

        return invite;
    }

    private void notify(String firstName, String email, String inviteRedeemUrl) {
        if (props.getQlikSendInvitation()) {
            String render = templateService.render(
                    firstName,
                    inviteRedeemUrl,
                    userService.getUserIdByEmail(props.getQlikUsersOwner()),
                    "qlik-email-template"
            );

            String response = mailingService.send("Velkommen som Qlik bruker!", render, email);
            log.info("Sending email response {}", response);
        }
    }

    private Boolean shouldUserExist(QlikUser qLikUser) {
        return hasValidEmailDomain(qLikUser.getEmail()) && hasAccess(qLikUser);
    }

    private Boolean hasValidEmailDomain(String email) {
        boolean validEmailDomain = props.getAllowedDomains().stream().anyMatch(email::endsWith);

        if (!validEmailDomain) {
            log.info("User {} don't have a valid email address", email);
        }

        return validEmailDomain;

    }

    private Boolean hasAccess(QlikUser qLikUser) {
        boolean hasAccess = qLikUser.getQlikViewRead() ||
                qLikUser.getQlikViewDeveloper() ||
                qLikUser.getQlikSenseRead() ||
                qLikUser.getQlikSenseDeveloper() ||
                qLikUser.getNPrintRead() ||
                qLikUser.getNPrintDeveloper();

        if (!hasAccess) log.info("User {} don't have any access groups. Skip inviting.", qLikUser.getEmail());

        return hasAccess;
    }

    private void addNeededGroups(List<String> hasGroups, List<String> needsGroups, String userId) {

        needsGroups.stream()
                .filter(groupId -> !hasGroups.contains(groupId))
                .forEach(groupId -> groupService.addUserToGroup(userId, groupId));
    }

    private void removeNotNeededGroups(List<String> hasGroups, List<String> needsGroups, String userId) {
        hasGroups.stream()
                .filter(id -> !needsGroups.contains(id))
                .forEach((groupId) -> groupService.removeUserFromGroup(userId, groupId));

    }

    private Boolean groupsNeedsUpdate(List<String> hasGroups, List<String> needsGroups) {
        Collections.sort(hasGroups);
        Collections.sort(needsGroups);

        return !hasGroups.equals(needsGroups);
    }

    private List<String> getNeededGroups(QlikUser qLikUser) {
        List<String> groups = new ArrayList<>();

        if (qLikUser.getNPrintDeveloper()) {
            groups.add(String.format("%s_%s", qLikUser.getCountyNumber(), CBrukerFields.NPRINT_DEVELOPER));
            groups.add(CBrukerFields.NPRINT_DEVELOPER);
        }
        if (qLikUser.getNPrintRead()) {
            groups.add(String.format("%s_%s", qLikUser.getCountyNumber(), CBrukerFields.NPRINT_READ));
            groups.add(CBrukerFields.NPRINT_READ);
        }
        if (qLikUser.getQlikSenseDeveloper()) groups.add(CBrukerFields.QLIK_SENSE_DEVELOPER);
        if (qLikUser.getQlikSenseRead()) groups.add(CBrukerFields.QLIK_SENSE_READ);
        if (qLikUser.getQlikViewDeveloper()) groups.add(CBrukerFields.QLIK_VIEW_DEVELOPER);
        if (qLikUser.getQlikViewRead()) groups.add(CBrukerFields.QLIK_VIEW_READ);

        List<String> neededGroupIds = groups.stream()
                .map(groupService::getAccessGroup)
                .map(group -> group.id)
                .collect(Collectors.toList());
        String groupByCountyNumber = groupService.getGroupByCountyNumber(qLikUser.getCountyNumber()).id;
        neededGroupIds.add(groupByCountyNumber);

        return neededGroupIds;

    }

    private final Supplier<Predicate<Group>> excludeDynamicGroups = () -> group -> !group.groupTypes.contains("DynamicMembership");

    private List<String> getHasGroups(QlikUser qLikUser) {
        List<Group> memberOf = userService.getMemberOf(qLikUser.getAzureADUPN())
                .stream()
                .map(o -> userService.getSerializer().deserializeObject(o.getRawObject().toString(), Group.class))
                .filter(excludeDynamicGroups.get())
                .collect(Collectors.toList());

        return memberOf.stream()
                .map(o -> o.id)
                .collect(Collectors.toList());
    }

    private User getPatchedUser(QlikUser qLikUser) {
        User user = new User();
        user.givenName = qLikUser.getFirstName();
        user.surname = qLikUser.getLastName();
        user.mail = qLikUser.getEmail().toLowerCase();
        user.displayName = qLikUser.getFullname();
        user.mobilePhone = qLikUser.getMobile();
        user.department = qLikUser.getCountyNumber();

        return user;
    }

    private String getUPNByEmail(String email){
        return UriUtils.encode(
                String.format(
                        "%s#EXT#@vigoiks.onmicrosoft.com",
                        email.replace("@", "_")
                ),
                StandardCharsets.UTF_8.toString()
        );
    }
}

