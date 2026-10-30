package solutions.shapeit.noirstay.management;

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

/** Ownership-scoped host management and context-scoped moderation preserve historical trips.
 * @author Daniel Jr Nkulu
 */
@Service
public class ManagementService {
    private final Database db; private final BookingService bookings; private final Identity identity; private final Pricing pricing;
    public ManagementService(Database db,BookingService bookings,Identity identity,Pricing pricing) { this.db=db;this.bookings=bookings;this.identity=identity;this.pricing=pricing; }
    public Map<String,Object> host(UserAccount user) {
        requireRole(user,"HOST");
        return Map.of("listings",db.rows("SELECT * FROM listing WHERE host_id=? ORDER BY created_at,id LIMIT 100",user.id),
            "reservations",db.rows("SELECT b.*,u.display_name guest_name FROM booking b JOIN listing l ON l.id=b.listing_id JOIN app_user u ON u.id=b.guest_id WHERE l.host_id=? ORDER BY b.created_at DESC,b.id LIMIT 100",user.id),
            "blocks",db.rows("SELECT i.* FROM inventory i JOIN listing l ON l.id=i.listing_id WHERE l.host_id=? AND i.kind='BLOCK' AND i.active ORDER BY i.check_in LIMIT 200",user.id),
            "metrics",db.one("SELECT count(*) FILTER(WHERE b.status='CONFIRMED') confirmed_stays,COALESCE(sum(b.total-b.refund) FILTER(WHERE b.status='CONFIRMED'),0) simulated_confirmed_value FROM booking b JOIN listing l ON l.id=b.listing_id WHERE l.host_id=?",user.id));
    }
    @Transactional public Map<String,Object> save(UserAccount user,UUID id,ListingInput input) {
        requireRole(user,"HOST"); boolean create=id==null;
        if(create) {
            id=UUID.randomUUID();
            db.jdbc.update("""
                INSERT INTO listing(id,host_id,context,slug,title,description,type,destination,latitude,longitude,capacity,beds,bathrooms,amenities,nightly_rate,cleaning_fee,house_rules,cancellation_policy,state)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?,?,?,?,?)
                """,id,user.id,identity.context(user),"stay-"+id.toString().substring(0,12),input.title(),input.description(),input.type(),input.destination(),input.latitude(),input.longitude(),input.capacity(),input.beds(),input.bathrooms(),db.json(input.amenities()),Pricing.money(input.nightlyRate()),Pricing.money(input.cleaningFee()),input.houseRules(),input.cancellationPolicy(),"DRAFT");
        } else own(user,id);
        String state=input.submit()?"PENDING":"DRAFT";
        // Editing a published listing requires moderation again. Existing bookings retain snapshots.
        db.jdbc.update("""
            UPDATE listing SET title=?,description=?,type=?,destination=?,latitude=?,longitude=?,capacity=?,beds=?,bathrooms=?,amenities=?::jsonb,
            nightly_rate=?,cleaning_fee=?,house_rules=?,cancellation_policy=?,state=?,price_version=price_version+1 WHERE id=?
            """,input.title(),input.description(),input.type(),input.destination(),input.latitude(),input.longitude(),input.capacity(),input.beds(),input.bathrooms(),db.json(input.amenities()),Pricing.money(input.nightlyRate()),Pricing.money(input.cleaningFee()),input.houseRules(),input.cancellationPolicy(),state,id);
        return db.one("SELECT * FROM listing WHERE id=?",id);
    }
    public Map<String,Object> own(UserAccount user,UUID id) {
        requireRole(user,"HOST"); Map<String,Object> listing=bookings.lockListing(user,id);
        if(!listing.get("hostId").equals(user.id)) throw ApiException.forbidden(); return listing;
    }
    @Transactional public Map<String,Object> block(UserAccount user,UUID id,LocalDate in,LocalDate out) {
        own(user,id);bookings.expireLocked(id);pricing.validate(in,out,1,16,"Africa/Johannesburg");bookings.available(id,in,out);
        UUID block=UUID.randomUUID(); db.jdbc.update("INSERT INTO inventory(id,listing_id,check_in,check_out,active,kind) VALUES(?,?,?,?,true,'BLOCK')",block,id,in,out);
        return Map.of("id",block);
    }
    @Transactional public Map<String,Boolean> unblock(UserAccount user,UUID id,UUID blockId) {
        own(user,id); int changed=db.jdbc.update("UPDATE inventory SET active=false WHERE id=? AND listing_id=? AND kind='BLOCK' AND active",blockId,id);
        if(changed==0) throw ApiException.missing();return Map.of("released",true);
    }
    public Map<String,Object> admin(UserAccount user) {
        requireRole(user,"ADMIN");String context=identity.context(user);
        return Map.of("listings",db.rows("SELECT * FROM listing WHERE context=? ORDER BY created_at DESC,id LIMIT 100",context),
            "users",db.rows("SELECT id,display_name,role,email,created_at FROM app_user WHERE COALESCE(demo_context,'live')=? ORDER BY created_at,id LIMIT 100",context),
            "bookings",db.rows("SELECT b.* FROM booking b JOIN listing l ON l.id=b.listing_id WHERE l.context=? ORDER BY b.created_at DESC,b.id LIMIT 100",context),
            "payments",db.rows("SELECT p.* FROM payment_attempt p JOIN booking b ON b.id=p.booking_id JOIN listing l ON l.id=b.listing_id WHERE l.context=? ORDER BY p.created_at DESC,p.id LIMIT 100",context),
            "audit",db.rows("SELECT * FROM audit_event WHERE context=? ORDER BY created_at DESC,id LIMIT 100",context),
            "metrics",db.one("SELECT count(*) total_bookings,count(*) FILTER(WHERE b.status='CONFIRMED') confirmed_stays,COALESCE(sum(b.total-b.refund) FILTER(WHERE b.status='CONFIRMED'),0) simulated_confirmed_value FROM booking b JOIN listing l ON l.id=b.listing_id WHERE l.context=?",context));
    }
    @Transactional public Map<String,Object> publish(UserAccount user,UUID id,String state) {
        requireRole(user,"ADMIN");Map<String,Object> listing=bookings.lockListing(user,id);
        if(!List.of("PUBLISHED","UNPUBLISHED").contains(state)) throw new ApiException(400,"INVALID_STATE","Approve or unpublish this listing.");
        @SuppressWarnings("unchecked") List<Object> images=(List<Object>)listing.get("images");
        if(state.equals("PUBLISHED") && images.isEmpty()) throw ApiException.conflict("Add at least one photograph before publication.");
        db.jdbc.update("UPDATE listing SET state=? WHERE id=?",state,id);
        db.jdbc.update("INSERT INTO audit_event(id,actor_id,context,action,resource_id) VALUES(?,?,?,?,?)",UUID.randomUUID(),user.id,identity.context(user),state,id);
        return Map.of("id",id,"state",state);
    }
    private void requireRole(UserAccount user,String role) { if(!user.role.equals(role)) throw ApiException.forbidden(); }
}
