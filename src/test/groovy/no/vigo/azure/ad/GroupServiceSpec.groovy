package no.vigo.azure.ad

import com.microsoft.graph.models.Group
import com.microsoft.graph.requests.*
import spock.lang.Specification


class GroupServiceSpec extends Specification {

    private GroupService groupService
    private GraphServiceClient graphClient
    private GroupCollectionPage page

    void setup() {
        graphClient = Mock(GraphServiceClient)
        groupService = new GroupService(graphClient)
        graphClient.groups() >> Mock(GroupCollectionRequestBuilder)
        graphClient.groups().buildRequest(_ as List) >> Mock(GroupCollectionRequest)
        graphClient.groups(_ as String) >> Mock(GroupCollectionRequestBuilder)
        //graphClient.groups(_ as String).members(_ as String) >> Mock(DirectoryObjectWithReferenceRequestBuilder)
        //graphClient.groups(_ as String).members(_ as String).reference() >> Mock(DirectoryObjectReferenceRequestBuilder)
        //graphClient.groups(_ as String).members(_ as String).reference().buildRequest() >> Mock(DirectoryObjectReferenceRequest)
        page = Mock(GroupCollectionPage)

    }

    def "Get Group By County Number"() {
        when:
        def countyGroup = groupService.getGroupByCountyNumber("11")

        then:
        page.getCurrentPage() >> Arrays.asList(new Group(displayName: "11_rogaland"), new Group(displayName: "11_acc"))
        1 * graphClient.groups().buildRequest(_ as List).get() >> page
        countyGroup.displayName == "11_rogaland"
    }

    def "Get Access Group should return one group"() {
        when:
        def accessGroup = groupService.getAccessGroup("accnvp")

        then:
        page.getCurrentPage() >> Arrays.asList(new Group(displayName: "accnvp"))
        1 * graphClient.groups().buildRequest(_ as List).get() >> page
        accessGroup.displayName == "accnvp"
    }

    def "Get Access Group should return null if more than one group"() {
        when:
        def accessGroup = groupService.getAccessGroup("accnvp")

        then:
        page.getCurrentPage() >> Arrays.asList(new Group(displayName: "accnvp"), new Group(displayName: "accqsd"))
        1 * graphClient.groups().buildRequest(_ as List).get() >> page
        accessGroup == null
    }

//    def "Remove User From Group"() {
//        when:
//        groupService.removeUserFromGroup("1", "1")
//
//        then:
//        1 * graphClient.groups(_ as String).members(_ as String).reference().buildRequest().delete()
//    }
//
//    def "AddUserToGroup"() {
//    }
//
//    def "GetGroupNamesByUser"() {
//    }
}
