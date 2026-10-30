package solutions.shapeit.noirstay.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Adds correlation IDs without logging personal data, cookies or request bodies.
 * @author Daniel Jr Nkulu
 */
@Component
public class RequestTrace extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RequestTrace.class);
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = UUID.randomUUID().toString();
        MDC.put("correlationId", id); response.setHeader("X-Correlation-ID", id);
        response.setHeader("Cache-Control", "no-store");
        long start = System.nanoTime();
        try { chain.doFilter(request, response); }
        finally { LOG.info("request method={} status={} durationMs={} correlationId={}", request.getMethod(), response.getStatus(),
                (System.nanoTime() - start) / 1_000_000, id); MDC.clear(); }
    }
}
