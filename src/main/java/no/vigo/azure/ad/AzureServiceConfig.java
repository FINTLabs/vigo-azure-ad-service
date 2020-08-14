package no.vigo.azure.ad;

import com.microsoft.graph.auth.confidentialClient.ClientCredentialProvider;
import com.microsoft.graph.auth.enums.NationalCloud;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import no.vigo.Props;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureServiceConfig {

    protected final Props props;

    public AzureServiceConfig(Props props) {
        this.props = props;
    }

    private ClientCredentialProvider getClientCredentialProvider() {
        return new ClientCredentialProvider(
                props.getClientId(),
                props.getScopes(),
                props.getClientSecret(),
                props.getTenant(),
                NationalCloud.Global);
    }

    @Bean
    public IGraphServiceClient getIGraphServiceClient() {
        IGraphServiceClient graphClient = GraphServiceClient
                .builder()
                .authenticationProvider(getClientCredentialProvider())
                .buildClient();
        graphClient.setServiceRoot("https://graph.microsoft.com/beta");

        return graphClient;
    }
}
