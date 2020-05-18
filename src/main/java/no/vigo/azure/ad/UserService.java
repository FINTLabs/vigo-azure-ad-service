package no.vigo.azure.ad;

import com.google.gson.JsonObject;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.extensions.DirectoryObject;
import com.microsoft.graph.models.extensions.Invitation;
import com.microsoft.graph.models.extensions.InvitedUserMessageInfo;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.requests.extensions.IDirectoryObjectCollectionWithReferencesPage;
import com.microsoft.graph.requests.extensions.IDirectoryObjectCollectionWithReferencesRequestBuilder;
import lombok.extern.slf4j.Slf4j;
import no.vigo.Props;
import no.vigo.azure.exception.AzureADUserNotFound;
import no.vigo.provisioning.qlik.QLikUser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService extends AzureServiceAbstract {
    public UserService(Props props) {
        super(props);
    }

    public void updateUser(User user, String id) {
        try {
            graphClient.users(id)
                    .buildRequest()
                    .patch(user);
        } catch (ClientException e) {
            log.error("Unable to update user {}", user.userPrincipalName);
        }
    }

    public void deleteUser(String userId) {
        graphClient.users(userId)
                .buildRequest()
                .delete();
    }

    public User usersExists(String id) throws AzureADUserNotFound {
        try {
            return graphClient.users(id).buildRequest().get();
        } catch (GraphServiceException e) {
            throw new AzureADUserNotFound();
        }
    }

    public List<DirectoryObject> getMemberOf(String id) {
        IDirectoryObjectCollectionWithReferencesPage groupCollectionPage = graphClient.users(id).memberOf().buildRequest().get();

        List<DirectoryObject> groups = new ArrayList<>(groupCollectionPage.getCurrentPage());
        while (groupCollectionPage.getNextPage() != null) {
            groups.addAll(groupCollectionPage.getNextPage().buildRequest().get().getCurrentPage());
        }

        return groups;

    }

    public List<JsonObject> getUsersByManager(String managerUpn) {
        IDirectoryObjectCollectionWithReferencesPage response = graphClient
                .users(managerUpn)
                .directReports()
                .buildRequest()
                .get();

        List<DirectoryObject> directoryObjects = new ArrayList<>(response.getCurrentPage());
        IDirectoryObjectCollectionWithReferencesRequestBuilder nextPage = response.getNextPage();
        while (nextPage != null) {
            IDirectoryObjectCollectionWithReferencesPage page = nextPage.buildRequest().get();
            directoryObjects.addAll(page.getCurrentPage());
            nextPage = page.getNextPage();
        }


        return directoryObjects.stream()
                .map(DirectoryObject::getRawObject)
                .collect(Collectors.toList());
    }

    private Invitation invite(Invitation invitation, String manager) {
        try {
            Invitation invitationResponse = graphClient.invitations()
                    .buildRequest()
                    .post(invitation);

            User owner = getUserIdByEmail(manager);
            setManager(invitationResponse.invitedUser.id, owner.id);

            return invitationResponse;
        } catch (ClientException e) {
            log.error("Unable to invite {}", invitation.invitedUserEmailAddress);
            return null;
        }
    }

    public Invitation invite(QLikUser qLikUser, String manager) {
        Invitation invitation = new Invitation();
        invitation.invitedUserEmailAddress = qLikUser.getEmail().toLowerCase();
        invitation.inviteRedirectUrl = props.getQlikRedirectUrl();
        invitation.invitedUserDisplayName = String.format("%s %s", qLikUser.getFirstName(), qLikUser.getLastName());
        invitation.sendInvitationMessage = props.getQlikSendInvitation();
        InvitedUserMessageInfo invitedUserMessageInfo = new InvitedUserMessageInfo();
        invitedUserMessageInfo.customizedMessageBody = props.getQlikInvitationMessageBody();
        invitedUserMessageInfo.messageLanguage = "no";
        invitation.invitedUserMessageInfo = invitedUserMessageInfo;

        return invite(invitation, manager);
    }

    public Invitation invite(UserInvitation userInvitation) {
        Invitation invitation = new Invitation();
        invitation.invitedUserEmailAddress = userInvitation.getEmail();
        invitation.inviteRedirectUrl = userInvitation.getApplicationUrl();
        invitation.invitedUserDisplayName = String.format("%s %s", userInvitation.getFirstName(), userInvitation.getLastName());

        return invite(invitation, userInvitation.getOwner());
    }

    public Invitation reInvite(String email) {
        Invitation invitation = new Invitation();

        try {
            invitation.invitedUserEmailAddress = email;
            invitation.inviteRedirectUrl = "https://qs.fintlabs.no";
            invitation.sendInvitationMessage = true;

            return graphClient.invitations()
                    .buildRequest()
                    .post(invitation);
        } catch (ClientException e) {
            log.error("Unable to re-invite {}", invitation.invitedUserEmailAddress);
            return null;
        }
    }


    private User getUserIdByEmail(String email) {
        return graphClient.users(email).buildRequest().get();
    }

    private void setManager(String userId, String ownerId) {
        JsonObject json = new JsonObject();
        json.addProperty("@odata.id", "https://graph.microsoft.com/v1.0/users/" + ownerId);

        graphClient.customRequest("/users/" + userId + "/manager/$ref")
                .buildRequest()
                .put(json);
    }
}
