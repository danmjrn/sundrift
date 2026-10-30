package solutions.shapeit.noirstay.catalogue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solutions.shapeit.noirstay.booking.BookingService;
import solutions.shapeit.noirstay.booking.Pricing;
import solutions.shapeit.noirstay.common.ApiException;
import solutions.shapeit.noirstay.common.Database;
import solutions.shapeit.noirstay.identity.Identity;
import solutions.shapeit.noirstay.identity.UserAccount;

/** Owns guest content transactions and completed-stay eligibility; the database enforces uniqueness.
 * @author Daniel Jr Nkulu
 */
@Service
public class GuestContentService {
    private final Database db; private final Identity identity; private final CatalogueService catalogue;
    private final BookingService bookings; private final Pricing pricing;
    public GuestContentService(Database db,Identity identity,CatalogueService catalogue,BookingService bookings,Pricing pricing) { this.db=db;this.identity=identity;this.catalogue=catalogue;this.bookings=bookings;this.pricing=pricing; }
    public List<Map<String,Object>> wishlist(UserAccount user) {
        return db.rows("SELECT l.* FROM wishlist w JOIN listing l ON l.id=w.listing_id WHERE w.user_id=? AND l.state='PUBLISHED' ORDER BY l.id LIMIT 100",user.id);
    }
    @Transactional public Map<String,Boolean> add(UserAccount user,UUID id) {
        catalogue.byId(identity.context(user),id);
        db.jdbc.update("INSERT INTO wishlist(user_id,listing_id) VALUES(?,?) ON CONFLICT DO NOTHING",user.id,id);return Map.of("saved",true);
    }
    @Transactional public Map<String,Boolean> remove(UserAccount user,UUID id) {
        db.jdbc.update("DELETE FROM wishlist WHERE user_id=? AND listing_id=?",user.id,id);return Map.of("saved",false);
    }
    @Transactional
    public Map<String,Boolean> review(UserAccount user,UUID id,int rating,String body) {
        if(!user.role.equals("GUEST")) throw ApiException.forbidden(); Map<String,Object> booking=bookings.view(user,id);
        if(!booking.get("status").equals("CONFIRMED") || ((LocalDate)booking.get("checkOut")).isAfter(pricing.today("Africa/Johannesburg")))
            throw ApiException.conflict("Reviews open after checkout for confirmed stays.");
        db.jdbc.update("INSERT INTO review(id,booking_id,listing_id,author_id,rating,body) VALUES(?,?,?,?,?,?)",UUID.randomUUID(),id,booking.get("listingId"),user.id,rating,body);
        return Map.of("saved",true);
    }
}
