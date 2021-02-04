package no.vigo.azure.ad;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import no.vigo.Props;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/api/user")
public class UserController {

    private final UserService userService;
    private final Props props;


    public UserController(UserService userService, Props props) {
        this.userService = userService;
        this.props = props;
    }

    @GetMapping
    public ResponseEntity<List<JsonObject>> getMyUser(@RequestHeader(name = "x-upn") String upn) {
        List<JsonObject> usersByManager = userService.getUsersByManager(props.getQlikUsersOwner());
        return ResponseEntity.ok(usersByManager);
    }
}
