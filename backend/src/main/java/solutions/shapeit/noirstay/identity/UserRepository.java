package solutions.shapeit.noirstay.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Uses correctly typed UUID identities for account persistence.
 * @author Daniel Jr Nkulu
 */
public interface UserRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmail(String email);
}
