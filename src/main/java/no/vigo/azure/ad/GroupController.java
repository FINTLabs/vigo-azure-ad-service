package no.vigo.azure.ad;

import com.microsoft.graph.models.extensions.Group;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@Slf4j
@RestController
@RequestMapping(value = "/api/group")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Group> getGroupById(@PathVariable String id) {
        Group groupById = groupService.getGroupById(id);

        if (Objects.nonNull(groupById)) {
            return ResponseEntity.ok(groupById);
        }
        return ResponseEntity.notFound().build();
    }
}
