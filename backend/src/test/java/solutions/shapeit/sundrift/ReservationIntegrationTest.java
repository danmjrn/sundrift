package solutions.shapeit.sundrift;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import solutions.shapeit.sundrift.booking.BookingService;
import solutions.shapeit.sundrift.catalogue.CatalogueService;
import solutions.shapeit.sundrift.common.ApiException;
import solutions.shapeit.sundrift.common.Database;
import solutions.shapeit.sundrift.demo.DemoFixtures;
import solutions.shapeit.sundrift.identity.UserAccount;
import solutions.shapeit.sundrift.identity.UserRepository;
import solutions.shapeit.sundrift.management.ListingInput;
import solutions.shapeit.sundrift.management.ManagementService;

/** Real PostgreSQL tests coordinate independent transactions with barriers, never timing sleeps.
 * @author Daniel Jr Nkulu
 */
@SpringBootTest
@ActiveProfiles("demo")
@AutoConfigureMockMvc
@Testcontainers
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class ReservationIntegrationTest {
    @Container static final PostgreSQLContainer POSTGRES=new PostgreSQLContainer("postgres:18.6-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",POSTGRES::getJdbcUrl);registry.add("spring.datasource.username",POSTGRES::getUsername);registry.add("spring.datasource.password",POSTGRES::getPassword);
    }
    static class MutableClock extends Clock {
        final java.util.concurrent.atomic.AtomicLong stepMillis=new java.util.concurrent.atomic.AtomicLong();
        final AtomicReference<Instant> now=new AtomicReference<>(Instant.parse("2026-10-31T08:00:00Z"));
        @Override public ZoneId getZone(){return ZoneId.of("UTC");}
        @Override public Clock withZone(ZoneId zone){return Clock.fixed(now.get(),zone);}
        @Override public Instant instant(){return now.getAndUpdate(value -> value.plusMillis(stepMillis.get()));}
    }
    @TestConfiguration static class TimeConfig { @Bean @Primary MutableClock testClock(){return new MutableClock();} }
    @Autowired BookingService bookings;@Autowired ManagementService management;@Autowired CatalogueService catalogue;
    @Autowired DemoFixtures fixtures;@Autowired UserRepository users;@Autowired Database db;@Autowired MockMvc mvc;
    @Autowired solutions.shapeit.sundrift.identity.RegistrationService registration;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
    @Autowired MutableClock clock;@Autowired TransactionTemplate transactions;
    UserAccount guest,other,host,admin;UUID listing;String context;
    final LocalDate in=LocalDate.parse("2026-11-06"),out=LocalDate.parse("2026-11-09");
    @BeforeEach void setup() {
        clock.stepMillis.set(0);clock.now.set(Instant.parse("2026-10-31T08:00:00Z"));context="test-"+UUID.randomUUID();fixtures.seed(context);
        guest=users.findByEmail("guest@"+context+".demo.invalid").orElseThrow();host=users.findByEmail("host@"+context+".demo.invalid").orElseThrow();admin=users.findByEmail("admin@"+context+".demo.invalid").orElseThrow();
        UUID otherId=UUID.randomUUID();db.jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,role,demo_context) VALUES(?,?,?,'disabled','GUEST',?)",otherId,otherId+"@demo.invalid","Other guest",context);other=users.findById(otherId).orElseThrow();
        listing=(UUID)db.one("SELECT id FROM listing WHERE context=? AND slug='the-clifton-house'",context).get("id");
    }
    UUID quote(UserAccount who,LocalDate start,LocalDate end){return (UUID)bookings.quote(who,listing,start,end,2).get("id");}
    @SuppressWarnings("unchecked") Map<String,Object> held(UserAccount who,UUID quote,String key){return (Map<String,Object>)bookings.hold(who,quote,key);}
    UUID hold(){return UUID.fromString(held(guest,quote(guest,in,out),UUID.randomUUID().toString()).get("id").toString());}
    @Test void enclosingOverlappingAndAdjacentRanges() {
        hold();
        assertThatThrownBy(()->quote(other,in.minusDays(1),out.plusDays(1))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->quote(other,in.plusDays(1),out.plusDays(1))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->quote(other,in.minusDays(1),in.plusDays(1))).isInstanceOf(ApiException.class);
        quote(other,out,out.plusDays(2));quote(other,in.minusDays(2),in);
    }
    @Test void twoIndependentGuestsCannotObtainSameDates() throws Exception {
        for(int run=0;run<3;run++) {
            LocalDate start=in.plusDays(run*45L),end=start.plusDays(2);UUID q1=quote(guest,start,end),q2=quote(other,start,end);
            List<String> results=race(()->attempt(()->bookings.hold(guest,q1,UUID.randomUUID().toString())),()->attempt(()->bookings.hold(other,q2,UUID.randomUUID().toString())));
            assertThat(results).containsExactlyInAnyOrder("OK","CONFLICT");
        }
    }
    @Test void bookingAndHostBlockRaceUsesOneInventoryInvariant() throws Exception {
        UUID q=quote(guest,in,out);
        assertThat(race(()->attempt(()->bookings.hold(guest,q,"block-race-key")),()->attempt(()->management.block(host,listing,in,out)))).containsExactlyInAnyOrder("OK","CONFLICT");
    }
    @Test void databaseConstraintRejectsOverlapsEvenWithoutServiceChecks() {
        hold();
        assertThatThrownBy(()->db.jdbc.update("INSERT INTO inventory(id,listing_id,check_in,check_out,active,kind) VALUES(?,?,?,?,true,'BLOCK')",UUID.randomUUID(),listing,in.minusDays(1),out.plusDays(1))).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void concurrentIdenticalRetriesReturnOnePersistedOutcome() throws Exception {
        UUID q=quote(guest,in,out);
        List<String> results=race(()->db.json(bookings.hold(guest,q,"identical-key")),()->db.json(bookings.hold(guest,q,"identical-key")));
        assertThat(db.parse(results.get(0))).isEqualTo(db.parse(results.get(1)));
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM booking WHERE quote_id=?",Integer.class,q)).isEqualTo(1);
        UUID otherQuote=quote(guest,out,out.plusDays(2));
        assertThatThrownBy(()->bookings.hold(guest,otherQuote,"identical-key")).isInstanceOf(ApiException.class).hasMessageContaining("different request");
    }
    @Test void paymentConfirmationAndCancellationAreIdempotent() {
        UUID id=hold();Object first=bookings.confirm(guest,id,"SUCCESS","payment-one",true);
        assertThat(db.parse(db.json(bookings.confirm(guest,id,"SUCCESS","payment-one",true)))).isEqualTo(db.parse(db.json(first)));
        assertThatThrownBy(()->bookings.confirm(guest,id,"DECLINE","payment-one",true)).isInstanceOf(ApiException.class);
        assertThat(bookings.refundPreview(guest,id).get("refund")).isEqualTo(bookings.view(guest,id).get("total"));
        bookings.cancel(guest,id,"cancel-one");bookings.cancel(guest,id,"cancel-one");bookings.cancel(guest,id,"cancel-two");
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM payment_attempt WHERE booking_id=? AND outcome='REFUND'",Integer.class,id)).isEqualTo(1);
        quote(other,in,out);
    }
    @Test void expiredHoldCannotWinConfirmationRace() throws Exception {
        UUID id=hold();clock.now.set(clock.instant().plusSeconds(600));
        race(()->{bookings.expireListing(listing);return "expiry";},()->{bookings.confirm(guest,id,"SUCCESS","expiry-key",true);return "confirm";});
        assertThat(bookings.view(guest,id).get("status")).isEqualTo("EXPIRED");
        assertThat(db.jdbc.queryForObject("SELECT active FROM inventory WHERE booking_id=?",Boolean.class,id)).isFalse();
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM payment_attempt WHERE booking_id=?",Integer.class,id)).isZero();
    }
    @Test void retryKeepsHoldDeclineReleasesItAndStateCannotBeRevived() {
        UUID id=hold();bookings.confirm(guest,id,"RETRY","retry-key",true);assertThat(bookings.view(guest,id).get("status")).isEqualTo("HOLD");
        bookings.confirm(guest,id,"DECLINE","decline-key",true);assertThat(bookings.view(guest,id).get("status")).isEqualTo("FAILED");
        quote(other,in,out);assertThatThrownBy(()->bookings.confirm(guest,id,"SUCCESS","revive-key",true)).isInstanceOf(ApiException.class);
    }
    @Test void quotesExpireAndPriceChangesRequireNewConsent() {
        UUID q=quote(guest,in,out);clock.now.set(clock.instant().plusSeconds(300));
        assertThatThrownBy(()->bookings.hold(guest,q,"expired-quote")).isInstanceOf(ApiException.class).hasMessageContaining("expired");
        UUID fresh=quote(guest,in,out);db.jdbc.update("UPDATE listing SET nightly_rate=nightly_rate+100,price_version=price_version+1 WHERE id=?",listing);
        assertThatThrownBy(()->bookings.hold(guest,fresh,"changed-price")).isInstanceOf(ApiException.class).hasMessageContaining("price changed");
    }
    @Test void cancellationUsesAgreedPolicyAfterHostEdits() {
        UUID id=hold();bookings.confirm(guest,id,"SUCCESS","snapshot-key",true);db.jdbc.update("UPDATE listing SET cancellation_policy='MODERATE',nightly_rate=9999 WHERE id=?",listing);
        assertThat(((Map<?,?>)bookings.view(guest,id).get("snapshot")).get("total")).isInstanceOf(BigDecimal.class);
        assertThat(((Map<?,?>)bookings.view(guest,id).get("snapshot")).get("cancellationPolicy")).isEqualTo("FLEXIBLE");
        assertThat(bookings.refundPreview(guest,id).get("refund")).isEqualTo(bookings.view(guest,id).get("total"));
    }
    @Test void unpublishStopsNewBookingAndPreservesHistoricalDetail() {
        UUID id=hold();bookings.confirm(guest,id,"SUCCESS","unpublish-key",true);management.publish(admin,listing,"UNPUBLISHED");
        assertThatThrownBy(()->quote(other,out,out.plusDays(1))).isInstanceOf(ApiException.class);
        assertThat(bookings.view(guest,id).get("status")).isEqualTo("CONFIRMED");
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE resource_id=?",Integer.class,listing)).isEqualTo(1);
    }
    @Test void ownersAndRolesAreEnforcedByServiceAndHttp() throws Exception {
        UUID id=hold();assertThatThrownBy(()->bookings.view(other,id)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->bookings.cancel(other,id,"wrong-owner")).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->management.block(guest,listing,in,out)).isInstanceOf(ApiException.class);
        mvc.perform(get("/api/v1/bookings/"+id).with(user(other.email).roles("GUEST"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin").with(user(guest.email).roles("GUEST"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/bookings")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/bookings/"+id+"/cancel").with(user(guest.email).roles("GUEST")).header("Idempotency-Key","missing-csrf")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/bookings/"+id+"/cancel").with(user(other.email).roles("GUEST")).with(csrf()).header("Idempotency-Key","other-cancel")).andExpect(status().isForbidden());
    }
    @Test void crossHostAndCrossContextAdministrationAreRejected() {
        String foreign="foreign-"+UUID.randomUUID();fixtures.seed(foreign);
        UserAccount otherHost=users.findByEmail("host@"+foreign+".demo.invalid").orElseThrow();
        assertThatThrownBy(()->management.block(otherHost,listing,in,out)).isInstanceOf(ApiException.class);
        UserAccount foreignAdmin=users.findByEmail("admin@"+foreign+".demo.invalid").orElseThrow();
        assertThatThrownBy(()->management.publish(foreignAdmin,listing,"UNPUBLISHED")).isInstanceOf(ApiException.class);
        UUID sameContextHost=UUID.randomUUID();db.jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,role,demo_context) VALUES(?,?,'Other host','disabled','HOST',?)",sameContextHost,sameContextHost+"@demo.invalid",context);
        assertThatThrownBy(()->management.block(users.findById(sameContextHost).orElseThrow(),listing,in,out)).isInstanceOf(ApiException.class);
    }
    @Test void reviewEligibilityAndUniquenessAreProtected() throws Exception {
        UUID future=hold();
        mvc.perform(post("/api/v1/bookings/"+future+"/review").with(user(guest.email).roles("GUEST")).with(csrf()).contentType("application/json").content("{\"rating\":5,\"body\":\"Too early\"}")).andExpect(status().isConflict());
        UUID completed=(UUID)db.one("SELECT b.id FROM booking b WHERE b.guest_id=? AND b.check_out<? AND EXISTS(SELECT 1 FROM review r WHERE r.booking_id=b.id) LIMIT 1",guest.id,in).get("id");
        mvc.perform(post("/api/v1/bookings/"+completed+"/review").with(user(guest.email).roles("GUEST")).with(csrf()).contentType("application/json").content("{\"rating\":5,\"body\":\"Duplicate\"}")).andExpect(status().isConflict());
    }
    @Test void migrationsAndIdempotentSeedSurviveNewConnections() throws Exception {
        UUID id=hold();fixtures.seed(context);
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM listing WHERE context=?",Integer.class,context)).isEqualTo(24);
        try(var connection=java.sql.DriverManager.getConnection(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());var statement=connection.prepareStatement("SELECT status FROM booking WHERE id=?")) {
            statement.setObject(1,id);try(var rows=statement.executeQuery()){assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("HOLD");}
        }
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success",Integer.class)).isEqualTo(5);
    }
    @Test void hostDraftEditsPreserveOwnershipAndRequirePublication() {
        var input=new ListingInput("A new test stay","Fictional test description","CABIN","Drakensberg",new BigDecimal("-28.90"),new BigDecimal("29.43"),2,1,1,List.of("Parking"),new BigDecimal("1250.00"),new BigDecimal("200.00"),"No parties.","FLEXIBLE",false);
        var draft=management.save(host,null,input);assertThat(draft.get("state")).isEqualTo("DRAFT");assertThat(draft.get("hostId")).isEqualTo(host.id);
        assertThatThrownBy(()->management.publish(admin,(UUID)draft.get("id"),"PUBLISHED")).isInstanceOf(ApiException.class).hasMessageContaining("photograph");
    }
    @Test void explicitPolicyConsentAndDtoBoundariesAreEnforced() throws Exception {
        UUID id=hold();
        mvc.perform(post("/api/v1/bookings/"+id+"/confirm").with(user(guest.email).roles("GUEST")).with(csrf())
            .header("Idempotency-Key","policy-consent").contentType("application/json")
            .content("{\"scenario\":\"SUCCESS\",\"policyAccepted\":false}")).andExpect(status().isBadRequest());
        assertThat(bookings.view(guest,id).get("status")).isEqualTo("HOLD");
        mvc.perform(post("/api/v1/quotes").with(user(guest.email).roles("GUEST")).with(csrf()).contentType("application/json")
            .content("{\"listingId\":\""+listing+"\",\"checkIn\":\"2026-11-06\",\"checkOut\":\"2026-11-08\",\"guests\":2,\"total\":1,\"guestId\":\""+other.id+"\"}"))
            .andExpect(status().isBadRequest());
    }
    @Test void completedStayAcceptsOneReviewAndRejectsDuplicates() throws Exception {
        UUID completed=(UUID)db.one("SELECT b.id FROM booking b WHERE b.guest_id=? AND b.check_out<? AND EXISTS(SELECT 1 FROM review r WHERE r.booking_id=b.id) LIMIT 1",guest.id,in).get("id");
        db.jdbc.update("DELETE FROM review WHERE booking_id=?",completed);
        String path="/api/v1/bookings/"+completed+"/review";
        mvc.perform(post(path).with(user(guest.email).roles("GUEST")).with(csrf()).contentType("application/json")
            .content("{\"rating\":5,\"body\":\"A thoughtful fictional stay.\"}")).andExpect(status().isOk());
        mvc.perform(post(path).with(user(guest.email).roles("GUEST")).with(csrf()).contentType("application/json")
            .content("{\"rating\":5,\"body\":\"A second review.\"}")).andExpect(status().isConflict());
    }
    @Test void expiryUsesOneCutoffForBothInventoryAndBookingState() {
        UUID id=hold();Instant base=clock.now.get();
        db.jdbc.update("UPDATE booking SET expires_at=? WHERE id=?",java.sql.Timestamp.from(base.plusMillis(1)),id);
        clock.stepMillis.set(2);bookings.expireListing(listing);clock.stepMillis.set(0);
        assertThat(bookings.view(guest,id).get("status")).isEqualTo("HOLD");
        assertThat(db.jdbc.queryForObject("SELECT active FROM inventory WHERE booking_id=?",Boolean.class,id)).isTrue();
        bookings.expireListing(listing);assertThat(bookings.view(guest,id).get("status")).isEqualTo("EXPIRED");
        assertThat(db.jdbc.queryForObject("SELECT active FROM inventory WHERE booking_id=?",Boolean.class,id)).isFalse();
    }
    @Test void unpublicationOrReducedCapacityBeforeConfirmationReleasesHold() {
        UUID id=hold();management.publish(admin,listing,"UNPUBLISHED");bookings.confirm(guest,id,"SUCCESS","closed-stay",true);
        assertThat(bookings.view(guest,id).get("status")).isEqualTo("FAILED");
        assertThat(db.jdbc.queryForObject("SELECT active FROM inventory WHERE booking_id=?",Boolean.class,id)).isFalse();
        management.publish(admin,listing,"PUBLISHED");UUID next=hold();db.jdbc.update("UPDATE listing SET capacity=1 WHERE id=?",listing);
        bookings.confirm(guest,next,"SUCCESS","capacity-change",true);assertThat(bookings.view(guest,next).get("status")).isEqualTo("FAILED");
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM payment_attempt WHERE booking_id=?",Integer.class,next)).isZero();
    }
    @Test void imageAdapterValidatesActualContentAndPersistsSafeMedia() throws Exception {
        var image=new java.awt.image.BufferedImage(120,120,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);
        var file=new org.springframework.mock.web.MockMultipartFile("file","../../unsafe.png","image/png",bytes.toByteArray());
        var result=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/v1/host/listings/"+listing+"/images")
            .file(file).with(user(host.email).roles("HOST")).with(csrf())).andExpect(status().isOk()).andReturn();
        @SuppressWarnings("unchecked") var uploaded=(Map<String,Object>)db.parse(result.getResponse().getContentAsString());
        assertThat(uploaded.get("url").toString()).matches("/media/[a-f0-9-]{36}\\.png");
        mvc.perform(get(uploaded.get("url").toString())).andExpect(status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/host/listings/"+listing+"/images")
            .with(user(host.email).roles("HOST")).with(csrf()).contentType("application/json")
            .content(db.json(Map.of("url",uploaded.get("url"))))).andExpect(status().isOk());
        assertThat(db.one("SELECT images FROM listing WHERE id=?",listing).get("images").toString()).doesNotContain(uploaded.get("url").toString());
        // Reference removal preserves the underlying file for historical links and controlled cleanup.
        mvc.perform(get(uploaded.get("url").toString())).andExpect(status().isOk());
        var fake=new org.springframework.mock.web.MockMultipartFile("file","fake.png","image/png","not an image".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/v1/host/listings/"+listing+"/images")
            .file(fake).with(user(host.email).roles("HOST")).with(csrf())).andExpect(status().isBadRequest());
    }
    @Test void loginRestoresAccountsOwnContextInsteadOfAStaleSessionContext() {
        var request=new org.springframework.mock.web.MockHttpServletRequest();
        request.getSession().setAttribute("demoContext","a-previous-context");
        String originalSession=request.getSession().getId();
        solutions.shapeit.sundrift.identity.AuthController.signIn(guest,request,new org.springframework.mock.web.MockHttpServletResponse());
        assertThat(request.getSession().getAttribute("demoContext")).isEqualTo(context);
        assertThat(request.getSession().getId()).isNotEqualTo(originalSession);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
    @Test void registrationHashesPasswordsAndRollsBackDuplicateContextCreation() {
        String email=UUID.randomUUID()+"@registration.demo.invalid";
        UserAccount created=registration.register(email,"synthetic-test-password","Registered demo guest");
        assertThat(created.role).isEqualTo("GUEST");assertThat(created.demoContext).isNotNull();
        assertThat(passwords.matches("synthetic-test-password",created.passwordHash)).isTrue();
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM listing WHERE context=?",Integer.class,created.demoContext)).isEqualTo(24);
        int before=db.jdbc.queryForObject("SELECT count(*) FROM demo_context",Integer.class);
        assertThatThrownBy(()->registration.register(email,"another-synthetic-password","Duplicate"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(db.jdbc.queryForObject("SELECT count(*) FROM demo_context",Integer.class)).isEqualTo(before);
    }
    private String attempt(Runnable work) {try{work.run();return "OK";}catch(ApiException|DataIntegrityViolationException e){return "CONFLICT";}}
    private List<String> race(Callable<String> first,Callable<String> second) throws Exception {
        CyclicBarrier gate=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->{gate.await(10,TimeUnit.SECONDS);return first.call();});
            var b=pool.submit(()->{gate.await(10,TimeUnit.SECONDS);return second.call();});
            return List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));
        }
    }
}
