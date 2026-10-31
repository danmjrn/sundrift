package solutions.shapeit.sundrift.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Persisted authentication identity. API controllers expose a separate safe projection.
 * @author Daniel Jr Nkulu
 */
@Entity
@Table(name = "app_user")
public class UserAccount {
    @Id public UUID id;
    @Column(nullable = false) public String email;
    @Column(name = "display_name", nullable = false) public String displayName;
    @Column(name = "password_hash", nullable = false) public String passwordHash;
    @Column(nullable = false) public String role;
    @Column(name = "demo_context") public String demoContext;
    protected UserAccount() { }
}
