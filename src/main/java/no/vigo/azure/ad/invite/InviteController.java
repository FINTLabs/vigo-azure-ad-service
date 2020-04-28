package no.vigo.azure.ad.invite;

import com.microsoft.graph.models.extensions.Invitation;
import com.microsoft.graph.models.extensions.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @PostMapping("/api/user/invite")
    public ResponseEntity<String> inviteUser(@RequestBody UserInvitation userInvitation) {
        return ResponseEntity.ok(inviteService.invite(userInvitation));
    }

    @GetMapping("/api/user/invite")
    public ResponseEntity<Map<String, Invitation>> getInvitations() {
        Map<String, Invitation> invitations = inviteService.getInvitations();
        invitations.forEach((key, value) -> {
            value.setRawObject(null, null);
        });
        return ResponseEntity.ok(inviteService.getInvitations());
    }

    @PutMapping("/api/user")
    public ResponseEntity<User> updateUser(@RequestBody User user) {
        return ResponseEntity.ok(inviteService.updateUser(user));

    }
}
