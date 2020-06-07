package no.vigo.provisioning.qlik;

import com.google.gson.JsonObject;
import com.microsoft.graph.models.extensions.Invitation;
import lombok.extern.slf4j.Slf4j;
import no.vigo.azure.ad.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping(value = "/api/qlik/user")
public class QlikInvitationController {

    private final UserService userService;
    private final QlikUserService qlikUserService;

    public QlikInvitationController(UserService userService, QlikUserService qlikUserService) {
        this.userService = userService;
        this.qlikUserService = qlikUserService;
    }

    @GetMapping
    public ResponseEntity<List<JsonObject>> getMyUser(@RequestHeader(name = "x-upn") String upn) {
        List<JsonObject> usersByManager = userService.getUsersByManager(upn);
        return ResponseEntity.ok(usersByManager);
    }

    @PostMapping("/invite")
    public ResponseEntity<Invitation> inviteUser(@RequestBody QlikUser qlikUser) {
        return ResponseEntity.ok(qlikUserService.invite(qlikUser));
    }

    @PostMapping("/invite/{mail}")
    public ResponseEntity<Invitation> inviteUser(@PathVariable String mail) {
        Invitation invitation = qlikUserService.reInvite(mail);
        if (Objects.nonNull(invitation)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.badRequest().build();
    }
}
