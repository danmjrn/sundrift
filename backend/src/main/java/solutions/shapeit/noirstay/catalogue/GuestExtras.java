package solutions.shapeit.noirstay.catalogue;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import solutions.shapeit.noirstay.identity.Identity;

/** HTTP DTO bindings for persisted wishlists and eligible reviews.
 * @author Daniel Jr Nkulu
 */
@RestController
@RequestMapping("/api/v1")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name="session")
public class GuestExtras {
    /** Validated review input request fields; the service owns validation and state transitions.
     * @author Daniel Jr Nkulu
     */
    public record ReviewInput(@Min(1) @Max(5) int rating,@NotBlank @Size(max=1500) String body) { }
    private final GuestContentService content;private final Identity identity;
    public GuestExtras(GuestContentService content,Identity identity) {this.content=content;this.identity=identity;}
    @GetMapping("/wishlist") List<Map<String,Object>> wishlist(Authentication auth) {return content.wishlist(identity.require(auth));}
    @PostMapping("/wishlist/{id}") Map<String,Boolean> add(Authentication auth,@PathVariable UUID id) {return content.add(identity.require(auth),id);}
    @DeleteMapping("/wishlist/{id}") Map<String,Boolean> remove(Authentication auth,@PathVariable UUID id) {return content.remove(identity.require(auth),id);}
    @PostMapping("/bookings/{id}/review") Map<String,Boolean> review(Authentication auth,@PathVariable UUID id,@Valid @RequestBody ReviewInput input) {return content.review(identity.require(auth),id,input.rating(),input.body());}
}
