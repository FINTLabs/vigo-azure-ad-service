package no.vigo.azure.ad;

import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.extensions.DirectoryObject;
import com.microsoft.graph.models.extensions.Group;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.extensions.IDirectoryObjectCollectionWithReferencesPage;
import com.microsoft.graph.requests.extensions.IDirectoryObjectCollectionWithReferencesRequestBuilder;
import com.microsoft.graph.requests.extensions.IGroupCollectionPage;
import lombok.extern.slf4j.Slf4j;
import no.vigo.Props;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GroupService extends AzureServiceAbstract {
    public GroupService(Props props) {
        super(props);
    }


    private List<Group> getGroupByDispslayNameFiltered(String value) {
        LinkedList<Option> requestOptions = new LinkedList<>();
        requestOptions.add(new QueryOption("$filter", "startswith(displayName,'" + value + "_')"));

        IGroupCollectionPage groupCollectionPage = graphClient.groups()
                .buildRequest(requestOptions)
                .get();

        List<Group> groups = new ArrayList<>(groupCollectionPage.getCurrentPage());
        while (groupCollectionPage.getNextPage() != null) {
            groups.addAll(groupCollectionPage.getNextPage().buildRequest().get().getCurrentPage());
        }
        return groups;
    }

    public Group getGroupByCountyNumber(String countyNumber) {

        List<Group> groups = getGroupByDispslayNameFiltered(countyNumber)
                .stream()
                .filter(o -> !o.displayName.startsWith(String.format("%s_acc", countyNumber)))
                .collect(Collectors.toList());

        if (groups.size() == 1) {
            return groups.get(0);
        }
        return null;
    }

    public Group getAccessGroup(String access) {
        List<Group> groups = getGroupByDispslayNameFiltered(access);

        if (groups.size() == 1) {
            return groups.get(0);
        }
        return null;
    }

    public void removeUserFromGroup(String userId, String groupId) {
        DirectoryObject directoryObject = new DirectoryObject();
        directoryObject.id = userId;

        graphClient.groups(groupId)
                .members(userId)
                .reference()
                .buildRequest()
                .delete();

        log.debug("Removed user ({}) from group ({})", userId, groupId);
    }

    public void addUserToGroup(String userId, String groupId) {
        DirectoryObject directoryObject = new DirectoryObject();
        directoryObject.id = userId;

        DirectoryObject response = graphClient.groups(groupId)
                .members()
                .references()
                .buildRequest()
                .post(directoryObject);

        log.debug("Added user to group ({})", response.id);
    }

    public Group getGroupById(String id) {
        try {
            return graphClient.groups(id).buildRequest().get();
        } catch (ClientException e) {
            return null;
        }
    }

    public List<String> getGroupNamesByUser(String username) {
        try {
            IDirectoryObjectCollectionWithReferencesPage response = graphClient.users(username)
                    .memberOf()
                    .buildRequest()
                    .get();
            List<DirectoryObject> directoryObjects = new ArrayList<>(response.getCurrentPage());
            IDirectoryObjectCollectionWithReferencesRequestBuilder nextPage = response.getNextPage();
            while (nextPage != null) {
                IDirectoryObjectCollectionWithReferencesPage page = nextPage.buildRequest().get();
                directoryObjects.addAll(page.getCurrentPage());
                nextPage = page.getNextPage();
            }

            return directoryObjects.stream()
                    .map(DirectoryObject::getRawObject)
                    .map(o -> o.get("displayName"))
                    .map(o -> o.getAsString())
                    .collect(Collectors.toList());
        }
        catch (GraphServiceException e) {
            return Collections.emptyList();
        }

    }
}
