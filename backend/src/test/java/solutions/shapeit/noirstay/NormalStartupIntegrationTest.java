package solutions.shapeit.noirstay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** A fresh normal-profile database has no fixtures or passwordless privileged entry points.
 * @author Daniel Jr Nkulu
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class NormalStartupIntegrationTest {
    @Container static final PostgreSQLContainer POSTGRES=new PostgreSQLContainer("postgres:18.6-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url",POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username",POSTGRES::getUsername);
        properties.add("spring.datasource.password",POSTGRES::getPassword);
    }
    @Autowired JdbcTemplate db;
    @Autowired MockMvc mvc;
    @Test void normalStartupDoesNotSeedDemoUsersOrCatalogue() throws Exception {
        assertThat(db.queryForObject("SELECT count(*) FROM app_user",Integer.class)).isZero();
        assertThat(db.queryForObject("SELECT count(*) FROM listing",Integer.class)).isZero();
        mvc.perform(get("/api/v1/demo/status")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/demo/enter").with(csrf()).contentType("application/json").content("{\"role\":\"ADMIN\"}")).andExpect(status().isNotFound());
    }
    @Test void registrationValidatesPasswordAndNeverAcceptsRoleInjection() throws Exception {
        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType("application/json").content("{\"email\":\"test@example.invalid\",\"password\":null,\"name\":\"Test\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType("application/json").content("{\"email\":\"test@example.invalid\",\"password\":\"long-enough-password\",\"name\":\"Test\",\"role\":\"ADMIN\"}")).andExpect(status().isBadRequest());
    }
    @Test void documentationIsPublicAndDescribesCookieAndCsrfRequirements() throws Exception {
        mvc.perform(get("/api/v1/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/openapi")).andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.info.title").value("NoirStay API"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.paths['/api/v1/bookings'].post.security[0].session").isArray())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.paths['/api/v1/bookings'].post.security[0].csrf").isArray());
    }

}
