package no.vigo.azure.ad;

import com.google.gson.JsonObject;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.extensions.DirectoryObject;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.Invitation;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.requests.extensions.IDirectoryObjectCollectionWithReferencesPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
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

    @Retryable(value = {ClientException.class},
            maxAttempts = 10,
            backoff = @Backoff(delay = 1000))
    public AzureUserResponse usersExists(String id) {
        AzureUserResponse azureUserResponse = new AzureUserResponse();
        try {
            User user = graphClient.users(id).buildRequest().get();
            azureUserResponse.setUser(user);
            azureUserResponse.setResponseCode(200);
            return azureUserResponse;
        } catch (GraphServiceException e) {
            if (e.getResponseCode() == 404) {
                azureUserResponse.setResponseCode(e.getResponseCode());
                return azureUserResponse;
            }
            throw e;
        }
    }

    @Recover
    public AzureUserResponse recover(ClientException t) {
        log.error("Unable to get user {}", t.getMessage());
        AzureUserResponse response = new AzureUserResponse();
        response.setResponseCode(400);
        return response;
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
