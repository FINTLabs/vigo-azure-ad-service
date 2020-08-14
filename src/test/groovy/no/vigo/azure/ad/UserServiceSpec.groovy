package no.vigo.azure.ad

import com.google.gson.JsonObject
import com.microsoft.graph.core.ClientException
import com.microsoft.graph.http.CustomRequest
import com.microsoft.graph.models.extensions.DirectoryObject
import com.microsoft.graph.models.extensions.IGraphServiceClient
import com.microsoft.graph.models.extensions.Invitation
import com.microsoft.graph.models.extensions.User
import com.microsoft.graph.requests.extensions.*
import no.vigo.azure.exception.AzureADUserNotFound
import spock.lang.Specification

class UserServiceSpec extends Specification {
    private UserService userService
    private IGraphServiceClient graphClient
    def id = "id"
    def user = new User(id: id)

    void setup() {
        graphClient = Mock(IGraphServiceClient)
        userService = new UserService(graphClient)
        graphClient.users(_ as String) >> Mock(IUserRequestBuilder)
        graphClient.users(_ as String).buildRequest() >> Mock(IUserRequest)
        graphClient.users(_ as String).memberOf() >> Mock(IDirectoryObjectCollectionWithReferencesRequestBuilder)
        graphClient.users(_ as String).memberOf().buildRequest() >> Mock(IDirectoryObjectCollectionWithReferencesRequest)
        graphClient.users(_ as String).directReports() >> Mock(IDirectoryObjectCollectionWithReferencesRequestBuilder)
        graphClient.users(_ as String).directReports().buildRequest() >> Mock(IDirectoryObjectCollectionWithReferencesRequest)
        graphClient.invitations() >> Mock(IInvitationCollectionRequestBuilder)
        graphClient.invitations().buildRequest() >> Mock(IInvitationCollectionRequest)
        graphClient.customRequest(_ as String) >> Mock(CustomRequestBuilder)
        graphClient.customRequest(_ as String).buildRequest() >> Mock(CustomRequest)
    }

    def "Update user should run without exceptions"() {
        when:
        userService.updateUser(user, id)

        then:
        1 * graphClient.users(_ as String).buildRequest().patch(_ as User)
        noExceptionThrown()
    }

    def "Delete user should call graph once"() {
        when:
        userService.deleteUser(id)

        then:
        1 * graphClient.users(_ as String).buildRequest().delete()
    }

    def "User exists should throw exception if not found"() {
        when:
        def user = userService.usersExists(id)

        then:
        1 * graphClient.users(_ as String).buildRequest().get() >> { throw new ClientException("User not found", new Throwable()) }
        !user
        thrown(AzureADUserNotFound)
    }

    def "User exists should not throw exception if found"() {
        when:
        def user = userService.usersExists(id)

        then:
        1 * graphClient.users(_ as String).buildRequest().get() >> new User(id: "test")
        user.id == "test"
        noExceptionThrown()
    }

    def "Get groups the user is member of"() {
        given:
        def directoryObjectCollectionWithReferencesPage = Mock(IDirectoryObjectCollectionWithReferencesPage)

        when:
        def memberOf = userService.getMemberOf(id)

        then:
        directoryObjectCollectionWithReferencesPage.getCurrentPage() >> Arrays.asList(new DirectoryObject(), new DirectoryObject())
        1 * graphClient.users(_ as String).memberOf().buildRequest().get() >> directoryObjectCollectionWithReferencesPage
        memberOf.size() == 2
    }

    def "Get users by manager"() {
        given:
        def directoryObjectCollectionWithReferencesPage = Mock(IDirectoryObjectCollectionWithReferencesPage)

        when:
        def usersByManager = userService.getUsersByManager(id)

        then:
        directoryObjectCollectionWithReferencesPage.getCurrentPage() >> Arrays.asList(new DirectoryObject(rawObject: new JsonObject()), new DirectoryObject(rawObject: new JsonObject()))
        1 * graphClient
                .users(_ as String)
                .directReports()
                .buildRequest()
                .get() >> directoryObjectCollectionWithReferencesPage
        usersByManager.size() == 2
        usersByManager.get(0) instanceof JsonObject

    }

    def "Invite user"() {
        when:
        userService.invite(new Invitation(), id)

        then:
        1 * graphClient.invitations().buildRequest().post(_ as Invitation) >> new Invitation(invitedUser: new User(id: id))
        1 * graphClient.users(id).buildRequest().get() >> new User(id: "owner")
        1 * graphClient.customRequest(_ as String)
                .buildRequest()
                .put(_ as JsonObject)
    }

    def "ReInvite user"() {
        when:
        userService.reInvite("donald@duck.com", "url")

        then:
        1 * graphClient.invitations().buildRequest().post(_ as Invitation)
    }

    def "Get User Id By Email"() {
        when:
        userService.getUserIdByEmail("anton@duck.com")

        then:
        1 * graphClient.users(_ as String).buildRequest().get()
    }

    def "Set manager"() {
        when:
        userService.setManager("userId", "manager")

        then:
        1 * graphClient.customRequest("url").buildRequest().put(_ as JsonObject)
    }
}
