package solutions.shapeit.noirstay.demo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import solutions.shapeit.noirstay.common.ApiException;
import solutions.shapeit.noirstay.identity.AuthController;
import solutions.shapeit.noirstay.identity.Identity;
import solutions.shapeit.noirstay.identity.UserAccount;
import solutions.shapeit.noirstay.identity.UserRepository;

/** Passwordless entry is restricted to disposable synthetic contexts in the explicit demo profile.
 * @author Daniel Jr Nkulu
 */
@RestController
@Profile("demo")
@RequestMapping("/api/v1/demo")
public class DemoController {
    /** Validated entry request fields; the service owns validation and state transitions.
     * @author Daniel Jr Nkulu
     */
    public record Entry(@NotNull @Pattern(regexp="GUEST|HOST|ADMIN") String role) { }
    private final DemoFixtures fixtures;private final UserRepository users;private final Identity identity;private final Clock clock;private final int contextLimit;
    private final ConcurrentHashMap<String,long[]> limits=new ConcurrentHashMap<>();
    public DemoController(DemoFixtures fixtures,UserRepository users,Identity identity,Clock clock,@org.springframework.beans.factory.annotation.Value("${noirstay.demo-contexts-per-hour:10}") int contextLimit) { this.fixtures=fixtures;this.users=users;this.identity=identity;this.clock=clock;this.contextLimit=contextLimit; }
    @GetMapping("/status") Map<String,Object> status() { return Map.of("enabled",true,"content","Fictional stays, hosts and reviews. All payments are simulated."); }
    @PostMapping("/enter") Map<String,Object> enter(@Valid @RequestBody Entry input,HttpServletRequest req,HttpServletResponse res) {
        String context=(String)req.getSession().getAttribute("demoContext");
        if(context==null) { throttle(req);context="visitor-"+UUID.randomUUID();fixtures.seed(context);req.getSession().setAttribute("demoContext",context); }
        UserAccount user=users.findByEmail(input.role().toLowerCase(java.util.Locale.ROOT)+"@"+context+".demo.invalid").orElseThrow();
        AuthController.signIn(user,req,res);return identity.view(user);
    }
    @PostMapping("/restore") Map<String,Object> restore(Authentication auth,HttpServletRequest req,HttpServletResponse res) {
        if(identity.require(auth).demoContext==null) throw ApiException.forbidden();
        req.getSession().removeAttribute("demoContext");return enter(new Entry("GUEST"),req,res);
    }
    private synchronized void throttle(HttpServletRequest req) {
        long now=clock.millis();limits.entrySet().removeIf(e -> now-e.getValue()[0]>3600000);
        if(limits.size()>10000) throw new ApiException(429,"RATE_LIMIT","Demo capacity reached. Try again later.");
        long[] limit=limits.computeIfAbsent(req.getRemoteAddr(),k -> new long[]{now,0});
        if(++limit[1]>contextLimit) throw new ApiException(429,"RATE_LIMIT","The demo context limit has been reached. Try again in an hour.");
    }
}
