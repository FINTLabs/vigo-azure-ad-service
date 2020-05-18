package no.vigo.provisioning.qlik;

import com.microsoft.graph.models.extensions.DirectoryObject;
import com.microsoft.graph.models.extensions.User;
import lombok.extern.slf4j.Slf4j;
import no.vigo.Props;
import no.vigo.azure.ad.GroupService;
import no.vigo.azure.ad.UserService;
import no.vigo.azure.exception.AzureADUserNotFound;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QlikUserService {

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;
    private final GroupService groupService;
    private final Props props;

    public QlikUserService(JdbcTemplate jdbcTemplate, UserService userService, GroupService groupService, Props props) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
        this.groupService = groupService;
        this.props = props;
    }

    @Scheduled(
            fixedDelayString = "${fint.azure.qlik.provisioning.fixed-delay:900000}",
            initialDelayString = "${fint.azure.qlik.provisioning.initial-delay:5000}"
    )
    public void synchronize() {
        log.info("Start provisioning users");
        log.debug("Fetching users from VIGOS");
        String sql = "select * from CBRUKER\n" +
                "where ACCQVV = 'J'\n" +
                "or ACCQVD = 'J'\n" +
                "or ACCQSV = 'J'\n" +
                "or ACCQSD = 'J'\n" +
                "or ACCNPD = 'J'\n" +
                "or ACCNPV = 'J'";
        List<QLikUser> qLikUsers = jdbcTemplate.query(sql, new QlikUserMapper());
        log.info("Found {} QLik users to provisioning", qLikUsers.size());
        qLikUsers.forEach(qLikUser -> {
            try {
                User user = userService.usersExists(qLikUser.getAzureADUPN());

                if (shouldUserExist(qLikUser)) {
                    log.trace("Updating user {}", qLikUser.getEmail());
                    userService.updateUser(getPatchedUser(qLikUser), qLikUser.getAzureADUPN());

                    List<String> hasGroups = getHasGroups(qLikUser);
                    List<String> neededGroups = getNeededGroups(qLikUser);

                    if (groupsNeedsUpdate(hasGroups, neededGroups)) {
                        log.debug("Groups needs to be updated");
                        addNeededGroups(hasGroups, neededGroups, user.id);
                        removeNotNeededGroups(hasGroups, neededGroups, user.id);
                    }
                } else {
                    log.info("Deleting user {}", user.mail);
                    userService.deleteUser(user.id);

                }
            } catch (AzureADUserNotFound e) {
                if (shouldUserExist(qLikUser)) {
                    log.info("Inviting user {}", qLikUser.getEmail());
                    userService.invite(qLikUser, props.getQlikUsersOwner());
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        });
        log.info("End provisioning users");
    }

    private Boolean shouldUserExist(QLikUser qLikUser) {
        return hasValidEmailDomain(qLikUser.getEmail()) && hasAccess(qLikUser);
    }

    private Boolean hasValidEmailDomain(String email) {
        boolean validEmailDomain = props.getAllowedDomains().stream().anyMatch(email::endsWith);

        if (!validEmailDomain) {
            log.info("User {} don't have a valid email address", email);
        }

        return validEmailDomain;

    }

    private Boolean hasAccess(QLikUser qLikUser) {
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

    private List<String> getNeededGroups(QLikUser qLikUser) {
        List<String> groups = new ArrayList<>();

        if (qLikUser.getNPrintDeveloper()) groups.add(CBrukerFields.NPRINT_DEVELOPER);
        if (qLikUser.getNPrintRead()) groups.add(CBrukerFields.NPRINT_READ);
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

    private List<String> getHasGroups(QLikUser qLikUser) {
        List<DirectoryObject> memberOf = userService.getMemberOf(qLikUser.getAzureADUPN());

        return memberOf.stream()
                .map(o -> o.id)
                .collect(Collectors.toList());
    }

    private User getPatchedUser(QLikUser qLikUser) {
        User user = new User();
        user.givenName = qLikUser.getFirstName();
        user.surname = qLikUser.getLastName();
        user.mail = qLikUser.getEmail().toLowerCase();
        user.displayName = qLikUser.getFullname();
        user.mobilePhone = qLikUser.getMobile();
        user.department = qLikUser.getCountyNumber();

        return user;
    }
}

