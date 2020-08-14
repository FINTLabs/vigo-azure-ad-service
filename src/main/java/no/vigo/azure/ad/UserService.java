package no.vigo.azure.ad;

import com.google.gson.JsonObject;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.extensions.DirectoryObject;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.Invitation;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.requests.extensions.IDirectoryObjectCollectionWithReferencesPage;
import lombok.extern.slf4j.Slf4j;
import no.vigo.azure.exception.AzureADUserNotFound;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService extends AzureServiceAbstract {
    public UserService(IGraphServiceClient graphClient) {
        super(graphClient);
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
        } catch (ClientException e) {
            throw new AzureADUserNotFound();
        }
    }

    public List<DirectoryObject> getMemberOf(String id) {
        return getPagedDirectoryObjects(graphClient.users(id).memberOf().buildRequest().get());
    }

    public List<JsonObject> getUsersByManager(String managerUpn) {
        IDirectoryObjectCollectionWithReferencesPage response = graphClient
                .users(managerUpn)
                .directReports()
                .buildRequest()
                .get();

        return getPagedDirectoryObjects(response).stream()
                .map(DirectoryObject::getRawObject)
                .collect(Collectors.toList());
    }


    public Invitation invite(Invitation invitation, String manager) {
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

    public Invitation reInvite(String email, String inviteRedirectUrl) {
        Invitation invitation = new Invitation();

        try {
            invitation.invitedUserEmailAddress = email;
            invitation.inviteRedirectUrl = inviteRedirectUrl;

            return graphClient.invitations()
                    .buildRequest()
                    .post(invitation);
        } catch (ClientException e) {
            log.error("Unable to re-invite {}", invitation.invitedUserEmailAddress);
            return null;
        }
    }


    public User getUserIdByEmail(String email) {
        return graphClient.users(email).buildRequest().get();
    }

    public void setManager(String userId, String ownerId) {
        JsonObject json = new JsonObject();
        json.addProperty("@odata.id", "https://graph.microsoft.com/v1.0/users/" + ownerId);

        graphClient.customRequest("/users/" + userId + "/manager/$ref")
                .buildRequest()
                .put(json);
    }
}
