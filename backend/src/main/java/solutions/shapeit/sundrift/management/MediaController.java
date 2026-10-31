package solutions.shapeit.sundrift.management;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import solutions.shapeit.sundrift.identity.Identity;

/** Binds host photograph DTOs to the transactional local storage adapter.
 * @author Daniel Jr Nkulu
 */
@RestController
public class MediaController {
    private final MediaService media;private final Identity identity;
    public MediaController(MediaService media,Identity identity) {this.media=media;this.identity=identity;}
    /** Validated photograph reference removal; storage paths remain server-controlled.
     * @author Daniel Jr Nkulu
     */
    public record ImageRemoval(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=500) String url) { }
    @PostMapping(value="/api/v1/host/listings/{id}/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    Map<String,Object> upload(Authentication auth,@PathVariable UUID id,@RequestParam MultipartFile file) throws IOException {return media.upload(identity.role(auth,"HOST"),id,file);}
    @org.springframework.web.bind.annotation.DeleteMapping("/api/v1/host/listings/{id}/images")
    Map<String,Object> remove(Authentication auth,@PathVariable UUID id,@jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody ImageRemoval input) {return media.remove(identity.role(auth,"HOST"),id,input.url());}
    @GetMapping("/media/{name}") ResponseEntity<FileSystemResource> read(@PathVariable String name) {return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(media.read(name));}
}
