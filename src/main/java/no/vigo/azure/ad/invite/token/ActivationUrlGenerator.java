package no.vigo.azure.ad.invite.token;

import com.microsoft.graph.models.extensions.User;
import no.vigo.azure.Props;
import no.rogfk.jwt.SpringJwtTokenizer;
import org.springframework.stereotype.Component;

@Component
public class ActivationUrlGenerator {

    private final Props configService;

    private final SpringJwtTokenizer springJwtTokenizer;

    public ActivationUrlGenerator(Props configService, SpringJwtTokenizer springJwtTokenizer) {
        this.configService = configService;
        this.springJwtTokenizer = springJwtTokenizer;
    }

    public String get(User user) {
        UserToken userToken = new UserToken();
        userToken.setId(user.id);

        return springJwtTokenizer.createWithUrl(configService.getBaseUrl(), "t", userToken);
    }

}