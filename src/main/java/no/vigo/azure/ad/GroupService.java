package no.vigo.azure.ad;

import com.google.gson.JsonElement;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.extensions.DirectoryObject;
import com.microsoft.graph.models.extensions.Group;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.extensions.IDirectoryObjectCollectionWithReferencesPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GroupService extends AzureServiceAbstract {
    public GroupService(IGraphServiceClient graphClient) {
        super(graphClient);
    }


    private List<Group> getGroupByDispslayNameFiltered(String value) {
        LinkedList<Option> requestOptions = new LinkedList<>();
        requestOptions.add(new QueryOption("$filter", "startswith(displayName,'" + value + "_')"));

//        IGroupCollectionPage groupCollectionPage = graphClient.groups()
//                .buildRequest(requestOptions)
//                .get();
//
//        List<Group> groups = new ArrayList<>(groupCollectionPage.getCurrentPage());
//        while (groupCollectionPage.getNextPage() != null) {
//            groups.addAll(groupCollectionPage.getNextPage().buildRequest().get().getCurrentPage());
//        }
        return getPagedGroupObjects(graphClient.groups()
                .buildRequest(requestOptions)
                .get());//groups;
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

    @Retryable(value = {ClientException.class},
            maxAttempts = 10,
            backoff = @Backoff(delay = 1000))
    public List<String> getGroupNamesByUser(String username) {

        IDirectoryObjectCollectionWithReferencesPage response = graphClient.users(username)
                .memberOf()
                .buildRequest()
                .get();

        return getPagedDirectoryObjects(response).stream()
                .map(DirectoryObject::getRawObject)
                .map(o -> o.get("displayName"))
                .map(JsonElement::getAsString)
                .collect(Collectors.toList());


    }

    @Recover
    public List<String> recover(ClientException t) {
        log.error("Unable to get groups {}", t.getMessage());
        return Collections.emptyList();
    }
}
