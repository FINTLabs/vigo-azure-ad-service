package no.vigo.azure.ad

import no.vigo.Props
import spock.lang.Specification

class UserServiceSpec extends Specification {
    /*
    void setup() {
    }

     */

    def "UpdateUser"() {
        given:
        def service = new UserService(new Props())

        when:
        true
        then:
        true
    }

    /*
    def "DeleteUser"() {
    }

    def "UsersExists"() {
    }

    def "GetMemberOf"() {
    }

    def "GetUsersByManager"() {
    }

    def "Invite"() {
    }

    def "ReInvite"() {
    }

    def "GetUserIdByEmail"() {
    }

    def "SetManager"() {
    }

     */
}
