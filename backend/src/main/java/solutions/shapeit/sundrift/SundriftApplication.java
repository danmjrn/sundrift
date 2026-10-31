package solutions.shapeit.sundrift;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Starts the modular Sundrift application and supplies its testable time source.
 * @author Daniel Jr Nkulu
 */
@SpringBootApplication
@EnableScheduling
public class SundriftApplication {
    public static void main(String[] args) { SpringApplication.run(SundriftApplication.class, args); }
    @Bean Clock clock() { return Clock.systemUTC(); }
}
