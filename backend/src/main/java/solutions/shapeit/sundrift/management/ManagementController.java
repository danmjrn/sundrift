package solutions.shapeit.sundrift.management;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import solutions.shapeit.sundrift.identity.Identity;

/** Host and administrator API routes enforce roles in Spring and ownership in services.
 * @author Daniel Jr Nkulu
 */
@RestController
@RequestMapping("/api/v1")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name="session")
public class ManagementController {
    /** Validated block request fields; the service owns validation and state transitions.
     * @author Daniel Jr Nkulu
     */
    public record Block(@NotNull LocalDate checkIn,@NotNull LocalDate checkOut) { }
    /** Validated publication request fields; the service owns validation and state transitions.
     * @author Daniel Jr Nkulu
     */
    public record Publication(@NotNull String state) { }
    private final ManagementService management;private final Identity identity;
    public ManagementController(ManagementService management,Identity identity) { this.management=management;this.identity=identity; }
    @GetMapping("/host") Map<String,Object> host(Authentication auth) { return management.host(identity.role(auth,"HOST")); }
    @PostMapping("/host/listings") Map<String,Object> create(Authentication auth,@Valid @RequestBody ListingInput input) { return management.save(identity.role(auth,"HOST"),null,input); }
    @PutMapping("/host/listings/{id}") Map<String,Object> edit(Authentication auth,@PathVariable UUID id,@Valid @RequestBody ListingInput input) { return management.save(identity.role(auth,"HOST"),id,input); }
    @PostMapping("/host/listings/{id}/blocks") Map<String,Object> block(Authentication auth,@PathVariable UUID id,@Valid @RequestBody Block input) { return management.block(identity.role(auth,"HOST"),id,input.checkIn(),input.checkOut()); }
    @DeleteMapping("/host/listings/{id}/blocks/{block}") Map<String,Boolean> unblock(Authentication auth,@PathVariable UUID id,@PathVariable UUID block) { return management.unblock(identity.role(auth,"HOST"),id,block); }
    @GetMapping("/admin") Map<String,Object> admin(Authentication auth) { return management.admin(identity.role(auth,"ADMIN")); }
    @PostMapping("/admin/listings/{id}/publication") Map<String,Object> publication(Authentication auth,@PathVariable UUID id,@Valid @RequestBody Publication input) { return management.publish(identity.role(auth,"ADMIN"),id,input.state()); }
}
