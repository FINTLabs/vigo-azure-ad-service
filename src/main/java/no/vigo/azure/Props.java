package no.vigo.azure;

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


}
