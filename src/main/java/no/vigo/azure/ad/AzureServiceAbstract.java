package no.vigo.azure.ad;

import com.microsoft.graph.auth.confidentialClient.ClientCredentialProvider;
import com.microsoft.graph.auth.enums.NationalCloud;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import no.vigo.Props;
import org.springframework.stereotype.Service;

@Service
public abstract class AzureServiceAbstract {

    protected final Props props;

    protected ClientCredentialProvider authProvider;
    protected IGraphServiceClient graphClient;


    public AzureServiceAbstract(Props props) {
        this.props = props;

        authProvider = new ClientCredentialProvider(
                props.getClientId(),
                props.getScopes(),
                props.getClientSecret(),
                props.getTenant(),
                NationalCloud.Global);
        graphClient = GraphServiceClient
                .builder()
                .authenticationProvider(authProvider)
                .buildClient();
        graphClient.setServiceRoot("https://graph.microsoft.com/beta/");

    }
}
