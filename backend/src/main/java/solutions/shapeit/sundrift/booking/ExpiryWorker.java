package solutions.shapeit.sundrift.booking;

import java.time.Clock;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Releases stale inventory transactionally after restarts and during normal operation.
 * @author Daniel Jr Nkulu
 */
@Component
public class ExpiryWorker {
    private final JdbcTemplate db; private final BookingService bookings; private final Clock clock;
    public ExpiryWorker(JdbcTemplate db,BookingService bookings,Clock clock) { this.db=db;this.bookings=bookings;this.clock=clock; }
    @Scheduled(fixedDelay=30000,initialDelay=30000) public void expire() {
        db.queryForList("SELECT DISTINCT listing_id FROM booking WHERE status='HOLD' AND expires_at<=? ORDER BY listing_id LIMIT 200",UUID.class,java.sql.Timestamp.from(clock.instant()))
            .forEach(bookings::expireListing);
    }
}
