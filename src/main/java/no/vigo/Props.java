package no.vigo;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
public class Props {

    @Value("${fint.azure.client.id}")
    private String clientId;

    @Value("${fint.azure.client.secret}")
    private String clientSecret;

    @Value("${fint.azure.client.scopes:https://graph.microsoft.com/.default}")
    List<String> scopes;

    @Value("${fint.azure.tenant}")
    private String tenant;

    @Value("${fint.azure.invite.base-url:http://localhost:8080/api/inviation?t=}")
    private String baseUrl;

    @Value("${fint.azure.qlik.users.owner:hans@vigoikt.no}")
    private String qlikUsersOwner;

    @Value("${fint.azure.qlik.redirect-url:https://qs.fintlabs.no}")
    private String qlikRedirectUrl;

    @Value("${fint.azure.qlik.send-invitation:false}")
    private Boolean qlikSendInvitation;

    @Value("${fint.azure.qlik.allowed-domains}")
    private List<String> allowedDomains;


}
