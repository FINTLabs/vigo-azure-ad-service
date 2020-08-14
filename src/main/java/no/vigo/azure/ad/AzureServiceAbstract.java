package no.vigo.azure.ad;

import com.microsoft.graph.models.extensions.DirectoryObject;
import com.microsoft.graph.models.extensions.Group;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.IDirectoryObjectCollectionWithReferencesPage;
import com.microsoft.graph.requests.extensions.IDirectoryObjectCollectionWithReferencesRequestBuilder;
import com.microsoft.graph.requests.extensions.IGroupCollectionPage;
import com.microsoft.graph.requests.extensions.IGroupCollectionRequestBuilder;
import com.microsoft.graph.serializer.ISerializer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public abstract class AzureServiceAbstract {

    protected final IGraphServiceClient graphClient;

    public AzureServiceAbstract(IGraphServiceClient graphClient) {
        this.graphClient = graphClient;
    }

    public ISerializer getSerializer() {
        return graphClient.getSerializer();
    }

    protected List<DirectoryObject> getPagedDirectoryObjects(IDirectoryObjectCollectionWithReferencesPage response) {
        List<DirectoryObject> directoryObjects = new ArrayList<>(response.getCurrentPage());
        IDirectoryObjectCollectionWithReferencesRequestBuilder nextPage = response.getNextPage();
        while (nextPage != null) {
            IDirectoryObjectCollectionWithReferencesPage page = nextPage.buildRequest().get();
            directoryObjects.addAll(page.getCurrentPage());
            nextPage = page.getNextPage();
        }

        return directoryObjects;
    }

    protected List<Group> getPagedGroupObjects(IGroupCollectionPage response) {
        List<Group> directoryObjects = new ArrayList<>(response.getCurrentPage());
        IGroupCollectionRequestBuilder nextPage = response.getNextPage();
        while (nextPage != null) {
            IGroupCollectionPage page = nextPage.buildRequest().get();
            directoryObjects.addAll(page.getCurrentPage());
            nextPage = page.getNextPage();
        }

        return directoryObjects;
    }
}
