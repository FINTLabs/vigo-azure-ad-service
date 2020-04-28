package no.vigo.azure.ad.invite;

import com.google.gson.JsonObject;
import com.microsoft.graph.auth.confidentialClient.ClientCredentialProvider;
import com.microsoft.graph.auth.enums.NationalCloud;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.Invitation;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import lombok.Getter;
import no.vigo.azure.Props;
import no.vigo.azure.ad.invite.token.ActivationUrlGenerator;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class InviteService {

    private final Props props;
    private final ActivationUrlGenerator activationUrlGenerator;
    private final MailingService mailingService;
    private final TemplateService templateService;

    private ClientCredentialProvider authProvider;
    private IGraphServiceClient graphClient;

    @Getter
    private Map<String, Invitation> invitations;

    public InviteService(Props props, ActivationUrlGenerator activationUrlGenerator, MailingService mailingService, TemplateService templateService) {
        this.props = props;
        this.activationUrlGenerator = activationUrlGenerator;
        this.mailingService = mailingService;
        this.templateService = templateService;
        invitations = new HashMap<>();
    }

    @PostConstruct
    public void init() {
        authProvider = new ClientCredentialProvider(
                props.getClientId(),
                props.getScopes(),
                props.getClientSecret(),
                props.getTenant(),
                NationalCloud.Global);
        graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
    }

    public String invite(UserInvitation userInvitation) {
        Invitation invitation = new Invitation();
        invitation.invitedUserEmailAddress = userInvitation.getEmail();
        invitation.inviteRedirectUrl = userInvitation.getApplicationUrl();
        invitation.invitedUserDisplayName = String.format("%s %s", userInvitation.getFirstName(), userInvitation.getLastName());
        //invitation.sendInvitationMessage = userInvitation.isSendInvite();

        Invitation invitationResponse = graphClient.invitations()
                .buildRequest()
                .post(invitation);

        User owner = getUserIdByEmail(userInvitation.getOwner());
        setManager(invitationResponse.invitedUser.id, owner.id);
        invitations.put(invitationResponse.id, invitationResponse);

        String registrationUrl = activationUrlGenerator.get(invitationResponse.invitedUser);
        String render = templateService.render(userInvitation.getFirstName(), registrationUrl, owner);
        mailingService.send(render, invitationResponse.invitedUserEmailAddress);

        return registrationUrl;

    }

    public User updateUser(User user) {
        return graphClient.users(user.id)
                .buildRequest()
                .patch(user);
    }

    private User getUserIdByEmail(String email) {
        return graphClient.users(email).buildRequest().get();
    }

    private JsonObject setManager(String userId, String ownerId) {
        JsonObject json = new JsonObject();
        json.addProperty("@odata.id", "https://graph.microsoft.com/v1.0/users/" + ownerId);

        return graphClient.customRequest("/users/" + userId + "/manager/$ref")
                .buildRequest()
                .put(json);
    }
}
