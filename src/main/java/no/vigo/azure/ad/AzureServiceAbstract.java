package no.vigo.azure.ad;

import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.DirectoryObjectCollectionWithReferencesPage;
import com.microsoft.graph.requests.DirectoryObjectCollectionWithReferencesRequestBuilder;
import com.microsoft.graph.requests.GroupCollectionPage;
import com.microsoft.graph.requests.GroupCollectionRequestBuilder;
import com.microsoft.graph.serializer.ISerializer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public abstract class AzureServiceAbstract {

    protected final GraphServiceClient graphClient;

    public AzureServiceAbstract(GraphServiceClient graphClient) {
        this.graphClient = graphClient;
    }

    public ISerializer getSerializer() {
        return graphClient.getSerializer();
    }

    protected List<DirectoryObject> getPagedDirectoryObjects(DirectoryObjectCollectionWithReferencesPage response) {
        List<DirectoryObject> directoryObjects = new ArrayList<>(response.getCurrentPage());
        DirectoryObjectCollectionWithReferencesRequestBuilder nextPage = response.getNextPage();
        while (nextPage != null) {
            DirectoryObjectCollectionWithReferencesPage page = nextPage.buildRequest().get();
            directoryObjects.addAll(page.getCurrentPage());
            nextPage = page.getNextPage();
        }

        return directoryObjects;
    }

    protected List<Group> getPagedGroupObjects(GroupCollectionPage response) {
        List<Group> directoryObjects = new ArrayList<>(response.getCurrentPage());
        GroupCollectionRequestBuilder nextPage = response.getNextPage();
        while (nextPage != null) {
            GroupCollectionPage page = nextPage.buildRequest().get();
            directoryObjects.addAll(page.getCurrentPage());
            nextPage = page.getNextPage();
        }

        return directoryObjects;
    }
}
