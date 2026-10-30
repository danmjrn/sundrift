package solutions.shapeit.noirstay.catalogue;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import solutions.shapeit.noirstay.booking.BookingService;
import solutions.shapeit.noirstay.booking.Pricing;
import solutions.shapeit.noirstay.common.ApiException;
import solutions.shapeit.noirstay.common.Database;
import solutions.shapeit.noirstay.identity.Identity;
import solutions.shapeit.noirstay.identity.UserAccount;

/** Persisted wishlists and one review per eligible completed reservation.
 * @author Daniel Jr Nkulu
 */
@RestController
@RequestMapping("/api/v1")
public class GuestExtras {
    public record ReviewInput(@Min(1) @Max(5) int rating,@NotBlank @Size(max=1500) String body) { }
    private final Database db; private final Identity identity; private final CatalogueService catalogue;
    private final BookingService bookings; private final Pricing pricing;
    public GuestExtras(Database db,Identity identity,CatalogueService catalogue,BookingService bookings,Pricing pricing) { this.db=db;this.identity=identity;this.catalogue=catalogue;this.bookings=bookings;this.pricing=pricing; }
    @GetMapping("/wishlist") List<Map<String,Object>> wishlist(Authentication auth) {
        UserAccount user=identity.require(auth); return db.rows("SELECT l.* FROM wishlist w JOIN listing l ON l.id=w.listing_id WHERE w.user_id=? AND l.state='PUBLISHED' ORDER BY l.id LIMIT 100",user.id);
    }
    @PostMapping("/wishlist/{id}") Map<String,Boolean> add(Authentication auth,@PathVariable UUID id) {
        UserAccount user=identity.require(auth); catalogue.byId(identity.context(user),id);
        db.jdbc.update("INSERT INTO wishlist(user_id,listing_id) VALUES(?,?) ON CONFLICT DO NOTHING",user.id,id);return Map.of("saved",true);
    }
    @DeleteMapping("/wishlist/{id}") Map<String,Boolean> remove(Authentication auth,@PathVariable UUID id) {
        db.jdbc.update("DELETE FROM wishlist WHERE user_id=? AND listing_id=?",identity.require(auth).id,id);return Map.of("saved",false);
    }
    @Transactional
    @PostMapping("/bookings/{id}/review") Map<String,Boolean> review(Authentication auth,@PathVariable UUID id,@Valid @RequestBody ReviewInput input) {
        UserAccount user=identity.role(auth,"GUEST"); Map<String,Object> booking=bookings.view(user,id);
        if(!booking.get("status").equals("CONFIRMED") || ((LocalDate)booking.get("checkOut")).isAfter(pricing.today("Africa/Johannesburg")))
            throw ApiException.conflict("Reviews open after checkout for confirmed stays.");
        db.jdbc.update("INSERT INTO review(id,booking_id,listing_id,author_id,rating,body) VALUES(?,?,?,?,?,?)",UUID.randomUUID(),id,booking.get("listingId"),user.id,input.rating(),input.body());
        return Map.of("saved",true);
    }
}
