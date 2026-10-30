package solutions.shapeit.noirstay.identity;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import solutions.shapeit.noirstay.common.ApiException;

/** Resolves ownership from the authenticated principal, never from request payloads.
 * @author Daniel Jr Nkulu
 */
@Service
public class Identity {
    private final UserRepository users;
    private final boolean demo;
    public Identity(UserRepository users, @Value("${noirstay.demo:false}") boolean demo) { this.users = users; this.demo = demo; }
    public UserAccount require(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()))
            throw new ApiException(401, "UNAUTHENTICATED", "Please sign in to continue.");
        return users.findByEmail(auth.getName()).orElseThrow(ApiException::missing);
    }
    public UserAccount role(Authentication auth, String role) {
        UserAccount user = require(auth); if (!user.role.equals(role)) throw ApiException.forbidden(); return user;
    }
    public String context(Authentication auth) {
        if (auth == null || "anonymousUser".equals(auth.getPrincipal())) return demo ? "showcase" : "live";
        return context(require(auth));
    }
    public String context(UserAccount user) { return user.demoContext == null ? "live" : user.demoContext; }
    public Map<String, Object> view(UserAccount user) {
        return Map.of("id", user.id, "email", user.email, "name", user.displayName, "role", user.role,
            "demo", user.demoContext != null);
    }
}
