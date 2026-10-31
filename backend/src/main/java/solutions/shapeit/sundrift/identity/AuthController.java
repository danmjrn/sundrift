package solutions.shapeit.sundrift.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import solutions.shapeit.sundrift.common.ApiException;

/** Account DTO endpoints and bounded single-instance login throttling for the portfolio VPS.
 * @author Daniel Jr Nkulu
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    /** Validated login request fields; the service owns validation and state transitions.
     * @author Daniel Jr Nkulu
     */
    public record Login(@Email @NotBlank String email, @NotBlank @Size(max=72) String password) { }
    /** Validated register request fields; the service owns validation and state transitions.
     * @author Daniel Jr Nkulu
     */
    public record Register(@Email @NotBlank @Size(max=254) String email, @NotBlank @Size(min=10,max=72) String password, @NotBlank @Size(max=100) String name) { }
    /** Validated account request fields; the service owns validation and state transitions.
     * @author Daniel Jr Nkulu
     */
    public record Account(@NotBlank @Size(max=100) String name) { }
    private final Identity identity;
    private final UserRepository users;
    private final RegistrationService registration;
    private final PasswordEncoder passwords;
    private final Clock clock;
    private final ConcurrentHashMap<String, long[]> attempts = new ConcurrentHashMap<>();
    public AuthController(Identity identity, UserRepository users, RegistrationService registration, PasswordEncoder passwords, Clock clock) {
        this.identity=identity; this.users=users; this.registration=registration; this.passwords=passwords; this.clock=clock;
    }
    @GetMapping("/csrf") Map<String,String> csrf(CsrfToken token) { return Map.of("token", token.getToken(), "headerName", token.getHeaderName()); }
    @GetMapping("/me") Map<String,Object> me(Authentication auth) { return identity.view(identity.require(auth)); }
    @PatchMapping("/me") Map<String,Object> update(Authentication auth, @Valid @RequestBody Account input) {
        UserAccount user=identity.require(auth); registration.rename(user,input.name()); return identity.view(user);
    }
    @PostMapping("/register") Map<String,Object> register(@Valid @RequestBody Register input, HttpServletRequest req, HttpServletResponse res) {
        throttle(req);
        UserAccount user=registration.register(input.email(),input.password(),input.name()); signIn(user,req,res); return identity.view(user);
    }
    @PostMapping("/login") Map<String,Object> login(@Valid @RequestBody Login input, HttpServletRequest req, HttpServletResponse res) {
        throttle(req);
        UserAccount user=users.findByEmail(input.email().toLowerCase(Locale.ROOT)).orElse(null);
        if (user==null || !passwords.matches(input.password(),user.passwordHash))
            throw new ApiException(401,"INVALID_LOGIN","The email or password is incorrect.");
        signIn(user,req,res); return identity.view(user);
    }
    private synchronized void throttle(HttpServletRequest req) {
        long now=clock.millis(); attempts.entrySet().removeIf(e -> now-e.getValue()[0]>60_000);
        if(attempts.size()>10_000) throw new ApiException(429,"RATE_LIMIT","Please wait a minute before trying again.");
        long[] bucket=attempts.computeIfAbsent(req.getRemoteAddr(), k -> new long[]{now,0});
        if(++bucket[1]>20) throw new ApiException(429,"RATE_LIMIT","Please wait a minute before trying again.");
    }
    public static void signIn(UserAccount user,HttpServletRequest req,HttpServletResponse res) {
        req.getSession(); req.changeSessionId();
        // Restore the account's own context on re-login, including before a demo role switch.
        if(user.demoContext!=null) req.getSession().setAttribute("demoContext",user.demoContext);
        else req.getSession().removeAttribute("demoContext");
        var context=SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user.email,null,List.of(new SimpleGrantedAuthority("ROLE_"+user.role))));
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context,req,res);
    }
}
