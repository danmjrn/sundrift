package solutions.shapeit.noirstay.identity;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import solutions.shapeit.noirstay.common.Errors;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Same-origin HttpOnly sessions with synchronizer-token CSRF protection on mutations.
 * @author Daniel Jr Nkulu
 */
@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean SecurityFilterChain security(HttpSecurity http, ObjectMapper json) throws Exception {
        return http.csrf(csrf -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/v1/catalogue/**", "/api/v1/auth/csrf", "/api/v1/demo/status",
                    "/api/v1/openapi/**", "/api/v1/docs/**", "/swagger-ui/**", "/actuator/health/**", "/media/**").permitAll()
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/demo/enter").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/host/**").hasRole("HOST")
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, e) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); response.setContentType("application/json");
                    json.writeValue(response.getWriter(), Errors.body("UNAUTHENTICATED", "Please sign in to continue."));
                }).accessDeniedHandler((request, response, e) -> {
                    response.setStatus(403); response.setContentType("application/json");
                    json.writeValue(response.getWriter(), Errors.body("FORBIDDEN", "Access denied or CSRF token missing. Refresh and try again."));
                }))
            .logout(logout -> logout.logoutUrl("/api/v1/auth/logout").invalidateHttpSession(true).deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, auth) -> response.setStatus(204)))
            .headers(headers -> headers.contentTypeOptions(content -> { })
                .frameOptions(frame -> frame.deny()))
            .build();
    }
}
