package solutions.shapeit.noirstay.demo;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solutions.shapeit.noirstay.booking.Pricing;
import solutions.shapeit.noirstay.common.Database;

/** Idempotent synthetic fixtures, isolated per visitor and never activated in normal startup.
 * @author Daniel Jr Nkulu
 */
@Service
@Profile("demo")
public class DemoFixtures {
    private final Database db;private final Clock clock;private final Pricing pricing;private final String reference;private final String inaccessiblePassword;
    public DemoFixtures(Database db,Clock clock,Pricing pricing,PasswordEncoder passwords,@Value("${noirstay.reference-date:}") String reference) {
        this.db=db;this.clock=clock;this.pricing=pricing;this.reference=reference;
        this.inaccessiblePassword=passwords.encode(UUID.randomUUID().toString());
    }
    @Transactional public void seed(String context) {
        db.jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))","seed:"+context);
        if(Boolean.TRUE.equals(db.jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM demo_context WHERE id=?)",Boolean.class,context))) return;
        db.jdbc.update("INSERT INTO demo_context(id) VALUES(?)",context);
        UUID host=user(context,"HOST","Naledi Adams · Demo host");
        UUID guest=user(context,"GUEST","Alex Morgan · Demo guest");user(context,"ADMIN","Demo administrator");
        String[] titles={"The Clifton House","A quieter kind of city","Saltwater mornings","Under the mountain","The Sea Point studio","The Sandton sanctuary","A room above the jacarandas","The glasshouse loft","Maboneng after hours","The garden apartment","The Umhlanga hideaway","Ocean, uninterrupted","A slower Durban morning","The coast is calling","Knysna in the canopy","The Wilderness cabin","Between forest and sea","The Plettenberg escape","A little room to breathe","The mountain refuge","Cabin at first light","The Drakensberg retreat","The stone cottage","An open-sky kind of stay"};
        String[] destinations={"Cape Town","Cape Town","Cape Town","Cape Town","Cape Town","Sandton","Johannesburg","Sandton","Johannesburg","Sandton","Durban","Durban","Durban","Durban","Garden Route","Garden Route","Garden Route","Garden Route","Garden Route","Drakensberg","Drakensberg","Drakensberg","Drakensberg","Drakensberg"};
        String[] types={"VILLA","APARTMENT","SUITE","CABIN","APARTMENT","APARTMENT","SUITE","APARTMENT","HOTEL_ROOM","APARTMENT","VILLA","APARTMENT","SUITE","HOTEL_ROOM","CABIN","CABIN","SUITE","VILLA","CABIN","CABIN","CABIN","VILLA","SUITE","CABIN"};
        int[] rates={2850,1450,1950,1250,980,1650,850,2100,780,1150,2450,1850,950,1200,1550,1350,1750,3200,1100,1450,1250,2650,890,1750};
        LocalDate today=reference.isBlank()?LocalDate.now(clock.withZone(java.time.ZoneId.of("Africa/Johannesburg"))):LocalDate.parse(reference);
        for(int i=0;i<24;i++) {
            UUID listing=UUID.randomUUID();String slug=titles[i].toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+","-");
            int photo=i%12+1; List<String> images=List.of(image(photo),image(photo%12+1),image((photo+1)%12+1));
            List<String> amenities=i%3==0?List.of("Wi-Fi","Parking","Kitchen","Pool","Ocean view"):i%3==1?List.of("Wi-Fi","Parking","Kitchen","Workspace"):List.of("Wi-Fi","Kitchen","Fireplace","Garden");
            double lat=i<5?-33.9249:i<10?-26.1076:i<14?-29.7265:i<19?-33.9880:-28.9167;
            double lon=i<5?18.4241:i<10?28.0567:i<14?31.0840:i<19?22.5840:29.4333;
            String policy=i%2==0?"FLEXIBLE":"MODERATE";BigDecimal rate=BigDecimal.valueOf(rates[i]);
            db.jdbc.update("""
                INSERT INTO listing(id,host_id,context,slug,title,description,type,destination,latitude,longitude,capacity,beds,bathrooms,amenities,images,nightly_rate,cleaning_fee,house_rules,cancellation_policy,state)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?::jsonb,?,350,?,?,'PUBLISHED')
                """,listing,host,context,slug,titles[i],"Thoughtfully made for days without a schedule. Natural textures, considered details and space to settle in. Start with coffee in the morning light, explore "+destinations[i]+", then come home to somewhere that feels like your own. This fictional, independently reservable stay is part of the NoirStay portfolio demonstration; photography is illustrative.",types[i],destinations[i],lat,lon,i%4==0?6:2+i%3,i%4==0?3:2,i%4==0?2:1,db.json(amenities),db.json(images),rate,"Check-in from 15:00. Check-out by 10:00. No parties or smoking. Quiet hours 22:00–08:00. Respect the neighbours.",policy);
            // Reviews are explicitly fictional and backed by completed synthetic reservations.
            UUID completed=seedBooking(guest,listing,slug,titles[i],destinations[i],rate,policy,today.minusDays(12+i),today.minusDays(10+i));
            if(i<23) db.jdbc.update("INSERT INTO review(id,booking_id,listing_id,author_id,rating,body) VALUES(?,?,?,?,?,?)",UUID.randomUUID(),completed,listing,guest,i%4==0?4:5,"Demonstration review: a beautifully considered stay, with a peaceful atmosphere and a thoughtful welcome. Fictional guest content.");
            if(i<3) seedBooking(guest,listing,slug,titles[i],destinations[i],rate,policy,today.plusDays(35+i*3),today.plusDays(37+i*3));
            if(i%4==0) db.jdbc.update("INSERT INTO inventory(id,listing_id,check_in,check_out,active,kind) VALUES(?,?,?,?,true,'BLOCK')",UUID.randomUUID(),listing,today.plusDays(14),today.plusDays(17));
        }
    }
    private String image(int i) { return "/images/stay-"+String.format("%02d",i)+".jpg"; }
    private UUID user(String context,String role,String name) {
        UUID id=UUID.randomUUID();db.jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,role,demo_context) VALUES(?,?,?,?,?,?)",id,role.toLowerCase(java.util.Locale.ROOT)+"@"+context+".demo.invalid",name,inaccessiblePassword,role,context);return id;
    }
    private UUID seedBooking(UUID guest,UUID listing,String slug,String title,String destination,BigDecimal rate,String policy,LocalDate in,LocalDate out) {
        UUID id=UUID.randomUUID();Map<String,Object> price=pricing.calculate(rate,new BigDecimal("350.00"),in,out,policy);
        price.put("listingTitle",title);price.put("slug",slug);price.put("destination",destination);price.put("timezone","Africa/Johannesburg");
        db.jdbc.update("""
            INSERT INTO booking(id,reference,guest_id,listing_id,check_in,check_out,guests,status,snapshot,total,expires_at,created_at,confirmed_at)
            VALUES(?,?,?,?,?,?,2,'CONFIRMED',?::jsonb,?,?,?,?)
            """,id,"NS-"+id.toString().substring(0,8).toUpperCase(),guest,listing,in,out,db.json(price),price.get("total"),java.sql.Timestamp.from(clock.instant()),java.sql.Timestamp.from(clock.instant().minusSeconds(86400)),java.sql.Timestamp.from(clock.instant().minusSeconds(86400)));
        db.jdbc.update("INSERT INTO inventory(id,listing_id,booking_id,check_in,check_out,active,kind) VALUES(?,?,?,?,?,true,'BOOKING')",UUID.randomUUID(),listing,id,in,out);
        db.jdbc.update("INSERT INTO payment_attempt(id,booking_id,outcome,amount,created_at) VALUES(?,?,'SUCCESS',?,?)",UUID.randomUUID(),id,price.get("total"),java.sql.Timestamp.from(clock.instant().minusSeconds(86400)));
        return id;
    }
}
