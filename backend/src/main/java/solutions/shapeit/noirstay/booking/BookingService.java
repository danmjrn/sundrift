package solutions.shapeit.noirstay.booking;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solutions.shapeit.noirstay.common.ApiException;
import solutions.shapeit.noirstay.common.Database;
import solutions.shapeit.noirstay.identity.Identity;
import solutions.shapeit.noirstay.identity.UserAccount;

/** Owns quotes, inventory and atomic simulated settlement. All inventory mutations lock
 * the listing first, then the booking; PostgreSQL also enforces the range invariant.
 * @author Daniel Jr Nkulu
 */
@Service
public class BookingService {
    private final Database db;
    private final Pricing pricing;
    private final Identity identity;
    private final Clock clock;
    public BookingService(Database db,Pricing pricing,Identity identity,Clock clock) { this.db=db;this.pricing=pricing;this.identity=identity;this.clock=clock; }
    @Transactional
    public Map<String,Object> quote(UserAccount user,UUID listingId,LocalDate in,LocalDate out,int guests) {
        guest(user); Map<String,Object> listing=lockListing(user,listingId); expireLocked(listingId);
        published(listing); pricing.validate(in,out,guests,(int)listing.get("capacity"),(String)listing.get("timezone"));
        available(listingId,in,out);
        Map<String,Object> snapshot=pricing.calculate((BigDecimal)listing.get("nightlyRate"),(BigDecimal)listing.get("cleaningFee"),in,out,(String)listing.get("cancellationPolicy"));
        UUID id=UUID.randomUUID();Instant expires=clock.instant().plusSeconds(300);
        db.jdbc.update("INSERT INTO quote(id,user_id,listing_id,check_in,check_out,guests,price_version,snapshot,expires_at) VALUES(?,?,?,?,?,?,?,?::jsonb,?)",
            id,user.id,listingId,in,out,guests,listing.get("priceVersion"),db.json(snapshot),java.sql.Timestamp.from(expires));
        return Map.of("id",id,"listingId",listingId,"checkIn",in,"checkOut",out,"guests",guests,"price",snapshot,"expiresAt",expires);
    }
    @Transactional
    public Object hold(UserAccount user,UUID quoteId,String key) {
        guest(user);
        return idempotent(user,"hold",key,quoteId.toString(),() -> {
            Map<String,Object> quote=db.one("SELECT * FROM quote WHERE id=? AND user_id=?",quoteId,user.id);
            UUID listingId=(UUID)quote.get("listingId"); Map<String,Object> listing=lockListing(user,listingId); expireLocked(listingId);
            published(listing);
            if(!((Instant)quote.get("expiresAt")).isAfter(clock.instant())) throw ApiException.conflict("Your quote expired. Request a new price.");
            if(!quote.get("priceVersion").equals(listing.get("priceVersion"))) throw ApiException.conflict("The price changed. Review a new quote before booking.");
            LocalDate in=(LocalDate)quote.get("checkIn"),out=(LocalDate)quote.get("checkOut");
            pricing.validate(in,out,(int)quote.get("guests"),(int)listing.get("capacity"),(String)listing.get("timezone"));
            available(listingId,in,out);
            UUID id=UUID.randomUUID(); Instant expiry=clock.instant().plusSeconds(600);
            @SuppressWarnings("unchecked") Map<String,Object> price=(Map<String,Object>)quote.get("snapshot");
            // Snapshot titles and policies preserve useful trip details after unpublication or edits.
            price.put("listingTitle",listing.get("title"));price.put("slug",listing.get("slug"));price.put("destination",listing.get("destination"));price.put("timezone",listing.get("timezone"));
            db.jdbc.update("""
                INSERT INTO booking(id,reference,guest_id,listing_id,quote_id,check_in,check_out,guests,status,snapshot,total,expires_at,created_at)
                VALUES(?,?,?,?,?,?,?,?,'HOLD',?::jsonb,?,?,?)
                """,id,"NS-"+id.toString().substring(0,8).toUpperCase(),user.id,listingId,quoteId,in,out,quote.get("guests"),
                db.json(price),new BigDecimal(price.get("total").toString()),java.sql.Timestamp.from(expiry),java.sql.Timestamp.from(clock.instant()));
            db.jdbc.update("INSERT INTO inventory(id,listing_id,booking_id,check_in,check_out,active,kind) VALUES(?,?,?,?,?,true,'BOOKING')",UUID.randomUUID(),listingId,id,in,out);
            return view(user,id);
        });
    }
    @Transactional
    public Object confirm(UserAccount user,UUID bookingId,String scenario,String key) {
        guest(user);
        if(!List.of("SUCCESS","DECLINE","RETRY").contains(scenario)) throw new ApiException(400,"INVALID_SCENARIO","Choose success, decline or retry.");
        return idempotent(user,"confirm:"+bookingId,key,scenario,() -> {
            Map<String,Object> booking=lockBooking(user,bookingId); UUID listingId=(UUID)booking.get("listingId");
            expireLocked(listingId); booking=owned(user,bookingId);
            String status=(String)booking.get("status");
            if(status.equals("EXPIRED")) return view(user,bookingId);
            if(!status.equals("HOLD")) throw ApiException.conflict("Only an active hold can be confirmed.");
            // The server chooses the simulator result from a documented scenario; no payment status or total is accepted.
            db.jdbc.update("INSERT INTO payment_attempt(id,booking_id,outcome,amount,created_at) VALUES(?,?,?,?,?)",
                UUID.randomUUID(),bookingId,scenario,booking.get("total"),java.sql.Timestamp.from(clock.instant()));
            if(scenario.equals("SUCCESS")) db.jdbc.update("UPDATE booking SET status='CONFIRMED',confirmed_at=? WHERE id=?",java.sql.Timestamp.from(clock.instant()),bookingId);
            if(scenario.equals("DECLINE")) {
                db.jdbc.update("UPDATE booking SET status='FAILED' WHERE id=?",bookingId);
                db.jdbc.update("UPDATE inventory SET active=false WHERE booking_id=?",bookingId);
            }
            // RETRY records a temporary simulator failure and retains the original hold until expiry.
            return view(user,bookingId);
        });
    }
    public Map<String,Object> refundPreview(UserAccount user,UUID id) {
        Map<String,Object> booking=owned(user,id); return Map.of("refund",refund(booking),"currency","ZAR","policy",snapshot(booking).get("cancellationPolicy"));
    }
    @Transactional
    public Object cancel(UserAccount user,UUID id,String key) {
        guest(user);
        return idempotent(user,"cancel:"+id,key,"cancel",() -> {
            Map<String,Object> booking=lockBooking(user,id); expireLocked((UUID)booking.get("listingId")); booking=owned(user,id);
            String status=(String)booking.get("status");
            if(status.equals("CANCELLED")) return view(user,id);
            if(!List.of("CONFIRMED","HOLD").contains(status)) throw ApiException.conflict("This reservation cannot be cancelled.");
            Map<String,Object> snap=snapshot(booking);
            if(((LocalDate)booking.get("checkIn")).isBefore(pricing.today((String)snap.get("timezone"))))
                throw ApiException.conflict("A stay that has started cannot be cancelled.");
            BigDecimal amount=status.equals("CONFIRMED")?refund(booking):new BigDecimal("0.00");
            db.jdbc.update("UPDATE booking SET status='CANCELLED',refund=?,cancelled_at=? WHERE id=?",amount,java.sql.Timestamp.from(clock.instant()),id);
            db.jdbc.update("UPDATE inventory SET active=false WHERE booking_id=?",id);
            if(status.equals("CONFIRMED")) db.jdbc.update("INSERT INTO payment_attempt(id,booking_id,outcome,amount,created_at) VALUES(?,?,'REFUND',?,?)",UUID.randomUUID(),id,amount,java.sql.Timestamp.from(clock.instant()));
            return view(user,id);
        });
    }
    public List<Map<String,Object>> trips(UserAccount user) { return db.rows("SELECT * FROM booking WHERE guest_id=? ORDER BY created_at DESC,id LIMIT 100",user.id); }
    public Map<String,Object> view(UserAccount user,UUID id) {
        Map<String,Object> booking=owned(user,id);
        booking.put("payments",db.rows("SELECT outcome,amount,created_at FROM payment_attempt WHERE booking_id=? ORDER BY created_at,id LIMIT 50",id));
        booking.put("reviewed",db.jdbc.queryForObject("SELECT count(*)>0 FROM review WHERE booking_id=?",Boolean.class,id));
        return booking;
    }
    private Map<String,Object> owned(UserAccount user,UUID id) {
        Map<String,Object> booking=db.one("SELECT * FROM booking WHERE id=?",id);
        if(!booking.get("guestId").equals(user.id)) throw ApiException.forbidden(); return booking;
    }
    private Map<String,Object> lockBooking(UserAccount user,UUID id) {
        Map<String,Object> booking=owned(user,id); lockListing(user,(UUID)booking.get("listingId"));
        return db.one("SELECT * FROM booking WHERE id=? FOR UPDATE",id);
    }
    public Map<String,Object> lockListing(UserAccount user,UUID id) {
        Map<String,Object> listing=db.one("SELECT * FROM listing WHERE id=? FOR UPDATE",id);
        if(!listing.get("context").equals(identity.context(user))) throw ApiException.forbidden(); return listing;
    }
    public void available(UUID id,LocalDate in,LocalDate out) {
        if(Boolean.TRUE.equals(db.jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM inventory WHERE listing_id=? AND active AND check_in<? AND check_out>?)",Boolean.class,id,out,in)))
            throw ApiException.conflict("Those dates have just been reserved or blocked. Choose another stay.");
    }
    private void published(Map<String,Object> listing) { if(!listing.get("state").equals("PUBLISHED")) throw ApiException.conflict("This stay is no longer available for new reservations."); }
    private void guest(UserAccount user) { if(!user.role.equals("GUEST")) throw ApiException.forbidden(); }
    @SuppressWarnings("unchecked") private Map<String,Object> snapshot(Map<String,Object> booking) { return (Map<String,Object>)booking.get("snapshot"); }
    private BigDecimal refund(Map<String,Object> booking) {
        if(!booking.get("status").equals("CONFIRMED")) return BigDecimal.ZERO.setScale(2);
        Map<String,Object> snap=snapshot(booking);
        return pricing.refund((BigDecimal)booking.get("total"),(String)snap.get("cancellationPolicy"),(LocalDate)booking.get("checkIn"),(String)snap.get("timezone"));
    }
    @Transactional public void expireListing(UUID id) {
        db.one("SELECT id FROM listing WHERE id=? FOR UPDATE",id); expireLocked(id);
    }
    public void expireLocked(UUID id) {
        db.jdbc.update("""
            UPDATE inventory SET active=false WHERE booking_id IN
            (SELECT id FROM booking WHERE listing_id=? AND status='HOLD' AND expires_at<=?)
            """,id,java.sql.Timestamp.from(clock.instant()));
        db.jdbc.update("UPDATE booking SET status='EXPIRED' WHERE listing_id=? AND status='HOLD' AND expires_at<=?",id,java.sql.Timestamp.from(clock.instant()));
    }
    private Object idempotent(UserAccount user,String operation,String key,String payload,Supplier<Object> command) {
        if(key==null || !key.matches("[A-Za-z0-9_-]{8,100}")) throw new ApiException(400,"IDEMPOTENCY_KEY","Supply an Idempotency-Key of 8–100 letters, digits, dashes or underscores.");
        String hash;
        try { hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        // Transaction-scoped lock serializes simultaneous retries before either creates a response.
        db.jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",user.id+":"+operation+":"+key);
        List<Map<String,Object>> previous=db.rows("SELECT payload_hash,response FROM idempotency WHERE user_id=? AND operation=? AND key=?",user.id,operation,key);
        if(!previous.isEmpty()) {
            if(!hash.equals(previous.getFirst().get("payloadHash"))) throw ApiException.conflict("This idempotency key was already used with a different request.");
            return previous.getFirst().get("response");
        }
        Object response=command.get();
        db.jdbc.update("INSERT INTO idempotency(user_id,operation,key,payload_hash,response) VALUES(?,?,?,?,?::jsonb)",user.id,operation,key,hash,db.json(response));
        return response;
    }
}
