package solutions.shapeit.noirstay.demo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Seeds the read-only anonymous showcase once, through the transactional fixture service.
 * @author Daniel Jr Nkulu
 */
@Component
@Profile("demo")
public class DemoStartup implements ApplicationRunner {
    private final DemoFixtures fixtures;
    public DemoStartup(DemoFixtures fixtures) { this.fixtures=fixtures; }
    @Override public void run(ApplicationArguments args) { fixtures.seed("showcase"); }
}
