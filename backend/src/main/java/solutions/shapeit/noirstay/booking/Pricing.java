package solutions.shapeit.noirstay.booking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import solutions.shapeit.noirstay.common.ApiException;

/** Server-owned ZAR pricing, half-open stays and explicit cancellation boundaries.
 * @author Daniel Jr Nkulu
 */
@Component
public class Pricing {
    private final Clock clock;
    public Pricing(Clock clock) { this.clock=clock; }
    public LocalDate today(String zone) { return LocalDate.now(clock.withZone(ZoneId.of(zone))); }
    public void validate(LocalDate in,LocalDate out,int guests,int capacity,String zone) {
        if(in==null || out==null || in.isBefore(today(zone)) || !out.isAfter(in) || ChronoUnit.DAYS.between(in,out)>30
            || in.isAfter(today(zone).plusDays(365)) || guests<1 || guests>capacity)
            throw new ApiException(400,"INVALID_STAY","Choose 1–30 nights within the next year, valid future dates and a guest count within capacity.");
    }
    public Map<String,Object> calculate(BigDecimal rate,BigDecimal cleaning,LocalDate in,LocalDate out,String policy) {
        long nights=ChronoUnit.DAYS.between(in,out);
        BigDecimal subtotal=money(rate.multiply(BigDecimal.valueOf(nights)));
        BigDecimal service=money(subtotal.multiply(new BigDecimal("0.08")));
        BigDecimal tax=money(subtotal.add(cleaning).add(service).multiply(new BigDecimal("0.15")));
        Map<String,Object> snapshot=new LinkedHashMap<>();
        snapshot.put("nightlyRate",money(rate)); snapshot.put("nights",nights); snapshot.put("subtotal",subtotal);
        snapshot.put("cleaningFee",money(cleaning)); snapshot.put("serviceFee",service); snapshot.put("tax",tax);
        snapshot.put("total",money(subtotal.add(cleaning).add(service).add(tax)));
        snapshot.put("currency","ZAR"); snapshot.put("cancellationPolicy",policy);
        return snapshot;
    }
    public BigDecimal refund(BigDecimal total,String policy,LocalDate in,String zone) {
        long days=ChronoUnit.DAYS.between(today(zone),in);
        int full=policy.equals("FLEXIBLE")?2:7;
        if(days>=full) return money(total);
        if(policy.equals("MODERATE") && days>=2) return money(total.multiply(new BigDecimal("0.50")));
        return money(BigDecimal.ZERO);
    }
    public static BigDecimal money(BigDecimal amount) { return amount.setScale(2,RoundingMode.HALF_UP); }
}
