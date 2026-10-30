package solutions.shapeit.noirstay;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Starts the modular NoirStay application and supplies its testable time source.
 * @author Daniel Jr Nkulu
 */
@SpringBootApplication
@EnableScheduling
public class NoirStayApplication {
    public static void main(String[] args) { SpringApplication.run(NoirStayApplication.class, args); }
    @Bean Clock clock() { return Clock.systemUTC(); }
}
