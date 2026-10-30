package solutions.shapeit.noirstay.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import solutions.shapeit.noirstay.identity.Identity;

/** Guest reservation DTOs deliberately omit identity, prices and arbitrary statuses.
 * @author Daniel Jr Nkulu
 */
@RestController
@RequestMapping("/api/v1")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name="session")
public class BookingController {
    /** Validated quote request request fields; the service owns validation and state transitions.
     * @author Daniel Jr Nkulu
     */
    public record QuoteRequest(@NotNull UUID listingId,@NotNull LocalDate checkIn,@NotNull LocalDate checkOut,@Min(1) @Max(16) int guests) { }
    /** Validated hold request request fields; the service owns validation and state transitions.
     * @author Daniel Jr Nkulu
     */
    public record HoldRequest(@NotNull UUID quoteId) { }
    /** Validated confirm request request fields; the service owns validation and state transitions.
     * @author Daniel Jr Nkulu
     */
    public record ConfirmRequest(@NotNull String scenario, @jakarta.validation.constraints.AssertTrue boolean policyAccepted) { }
    private final BookingService bookings; private final Identity identity;
    public BookingController(BookingService bookings,Identity identity) { this.bookings=bookings;this.identity=identity; }
    @PostMapping("/quotes") Map<String,Object> quote(Authentication auth,@Valid @RequestBody QuoteRequest input) { return bookings.quote(identity.require(auth),input.listingId(),input.checkIn(),input.checkOut(),input.guests()); }
    @PostMapping("/bookings") Object hold(Authentication auth,@Valid @RequestBody HoldRequest input,@RequestHeader("Idempotency-Key") String key) { return bookings.hold(identity.require(auth),input.quoteId(),key); }
    @PostMapping("/bookings/{id}/confirm") Object confirm(Authentication auth,@PathVariable UUID id,@Valid @RequestBody ConfirmRequest input,@RequestHeader("Idempotency-Key") String key) { return bookings.confirm(identity.require(auth),id,input.scenario(),key,input.policyAccepted()); }
    @PostMapping("/bookings/{id}/cancel") Object cancel(Authentication auth,@PathVariable UUID id,@RequestHeader("Idempotency-Key") String key) { return bookings.cancel(identity.require(auth),id,key); }
    @GetMapping("/bookings") List<Map<String,Object>> trips(Authentication auth) { return bookings.trips(identity.require(auth)); }
    @GetMapping("/bookings/{id}") Map<String,Object> detail(Authentication auth,@PathVariable UUID id) { return bookings.view(identity.require(auth),id); }
    @GetMapping("/bookings/{id}/refund") Map<String,Object> refund(Authentication auth,@PathVariable UUID id) { return bookings.refundPreview(identity.require(auth),id); }
}
