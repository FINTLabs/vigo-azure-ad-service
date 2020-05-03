package no.vigo.azure.ad;

import com.google.gson.JsonObject;
import com.microsoft.graph.models.extensions.Invitation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping(value = "/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<JsonObject>> getMyUser(@RequestHeader(name = "x-upn") String upn) {
        List<JsonObject> usersByManager = userService.getUsersByManager(upn);
        return ResponseEntity.ok(usersByManager);
    }

    @PostMapping("/invite")
    public ResponseEntity<Invitation> inviteUser(@RequestBody UserInvitation userInvitation) {
        return ResponseEntity.ok(userService.invite(userInvitation));
    }

    @PostMapping("/invite/{mail}")
    public ResponseEntity<Invitation> inviteUser(@PathVariable String mail) {
        Invitation invitation = userService.reInvite(mail);
        if (Objects.nonNull(invitation)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.badRequest().build();
    }
}
