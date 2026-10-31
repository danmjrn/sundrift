package solutions.shapeit.sundrift.catalogue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import solutions.shapeit.sundrift.identity.Identity;

/** Public catalogue contracts with URL-based filters and stable pagination.
 * @author Daniel Jr Nkulu
 */
@RestController
@RequestMapping("/api/v1/catalogue")
public class CatalogueController {
    private final CatalogueService catalogue;
    private final Identity identity;
    public CatalogueController(CatalogueService catalogue,Identity identity) { this.catalogue=catalogue;this.identity=identity; }
    @GetMapping Map<String,Object> search(Authentication auth,
        @RequestParam(defaultValue="") String destination,@RequestParam(required=false) LocalDate checkIn,
        @RequestParam(required=false) LocalDate checkOut,@RequestParam(defaultValue="1") int guests,
        @RequestParam(defaultValue="0") BigDecimal min,@RequestParam(defaultValue="1000000") BigDecimal max,
        @RequestParam(defaultValue="") String type,@RequestParam(defaultValue="") String amenities,
        @RequestParam(defaultValue="0") BigDecimal rating,@RequestParam(defaultValue="recommended") String sort,
        @RequestParam(defaultValue="0") int page) {
        return catalogue.search(identity.context(auth),destination,checkIn,checkOut,guests,min,max,type,amenities,rating,sort,page);
    }
    @GetMapping("/{slug}") Map<String,Object> detail(Authentication auth,@PathVariable String slug) { return catalogue.detail(identity.context(auth),slug); }
    @GetMapping("/units/{id}/price") Map<String,Object> price(Authentication auth,@PathVariable UUID id,@RequestParam LocalDate checkIn,@RequestParam LocalDate checkOut,@RequestParam(defaultValue="2") int guests) { return catalogue.preview(identity.context(auth),id,checkIn,checkOut,guests); }
    @GetMapping("/units/{id}/availability") List<Map<String,Object>> availability(Authentication auth,@PathVariable UUID id) { return catalogue.availability(identity.context(auth),id); }
}
