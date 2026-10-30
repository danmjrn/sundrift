package solutions.shapeit.noirstay.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documents the shared-origin API and cookie/CSRF transport without inventing a second backend.
 * @author Daniel Jr Nkulu
 */
@Configuration
public class OpenApiConfig {
    @Bean OpenAPI documentation() {
        return new OpenAPI().info(new Info().title("NoirStay API").version("1.0.0")
            .description("One independently rentable unit per listing. ZAR prices. Holds last ten minutes; quotes last five. All payment scenarios are simulated. Mutations require a CSRF token from /api/v1/auth/csrf; booking transitions also require Idempotency-Key.")
            .contact(new Contact().name("Daniel Jr Nkulu · Shape It Solutions")))
            .servers(List.of(new Server().url("/")))
            .components(new Components().addSecuritySchemes("session",new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("SESSION"))
                .addSecuritySchemes("csrf",new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-CSRF-TOKEN")));
    }
}
