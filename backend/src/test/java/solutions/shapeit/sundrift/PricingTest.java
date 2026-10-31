package solutions.shapeit.sundrift;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import solutions.shapeit.sundrift.booking.Pricing;
import solutions.shapeit.sundrift.common.ApiException;

/** Executable pricing examples cover calendar boundaries and deterministic rounding.
 * @author Daniel Jr Nkulu
 */
class PricingTest {
    private final Pricing pricing=new Pricing(Clock.fixed(Instant.parse("2026-10-31T08:00:00Z"),ZoneOffset.UTC));
    @Test void roundsEachFeeThenTotals() {
        var price=pricing.calculate(new BigDecimal("999.99"),new BigDecimal("350.00"),LocalDate.parse("2026-11-06"),LocalDate.parse("2026-11-08"),"FLEXIBLE");
        assertThat(price.get("nights")).isEqualTo(2L);
        assertThat(price.get("subtotal")).isEqualTo(new BigDecimal("1999.98"));
        assertThat(price.get("serviceFee")).isEqualTo(new BigDecimal("160.00"));
        assertThat(price.get("tax")).isEqualTo(new BigDecimal("376.50"));
        assertThat(price.get("total")).isEqualTo(new BigDecimal("2886.48"));
    }
    @Test void validatesDatesAndCapacity() {
        LocalDate today=LocalDate.parse("2026-10-31");
        for(LocalDate end:new LocalDate[]{today,today.minusDays(1),today.plusDays(31)})
            assertThatThrownBy(()->pricing.validate(today,end,2,2,"Africa/Johannesburg")).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->pricing.validate(today.minusDays(1),today.plusDays(1),2,2,"Africa/Johannesburg")).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->pricing.validate(today,today.plusDays(1),3,2,"Africa/Johannesburg")).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->pricing.validate(today.plusDays(366),today.plusDays(367),1,2,"Africa/Johannesburg")).isInstanceOf(ApiException.class);
        pricing.validate(today,today.plusDays(1),2,2,"Africa/Johannesburg");
    }
    @Test void cancellationBoundariesAreInclusiveAndCalendarBased() {
        BigDecimal total=new BigDecimal("1000.01");LocalDate today=LocalDate.parse("2026-10-31");
        assertThat(pricing.refund(total,"FLEXIBLE",today.plusDays(2),"Africa/Johannesburg")).isEqualByComparingTo("1000.01");
        assertThat(pricing.refund(total,"FLEXIBLE",today.plusDays(1),"Africa/Johannesburg")).isEqualByComparingTo("0");
        assertThat(pricing.refund(total,"MODERATE",today.plusDays(7),"Africa/Johannesburg")).isEqualByComparingTo("1000.01");
        assertThat(pricing.refund(total,"MODERATE",today.plusDays(6),"Africa/Johannesburg")).isEqualByComparingTo("500.01");
        assertThat(pricing.refund(total,"MODERATE",today.plusDays(2),"Africa/Johannesburg")).isEqualByComparingTo("500.01");
        assertThat(pricing.refund(total,"MODERATE",today.plusDays(1),"Africa/Johannesburg")).isEqualByComparingTo("0");
    }
    @Test void listingTimezoneDefinesToday() {
        Pricing midnight=new Pricing(Clock.fixed(Instant.parse("2026-10-31T22:30:00Z"),ZoneOffset.UTC));
        assertThat(midnight.today("Africa/Johannesburg")).isEqualTo(LocalDate.parse("2026-11-01"));
    }
}
