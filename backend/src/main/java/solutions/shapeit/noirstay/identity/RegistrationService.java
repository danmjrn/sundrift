package solutions.shapeit.noirstay.identity;

import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solutions.shapeit.noirstay.demo.DemoFixtures;

/** Creates a guest and its optional synthetic catalogue atomically, never accepting a role.
 * @author Daniel Jr Nkulu
 */
@Service
public class RegistrationService {
    private final UserRepository users; private final JdbcTemplate db; private final PasswordEncoder passwords;
    private final ObjectProvider<DemoFixtures> demoFixtures;
    public RegistrationService(UserRepository users,JdbcTemplate db,PasswordEncoder passwords,ObjectProvider<DemoFixtures> demoFixtures) {
        this.users=users;this.db=db;this.passwords=passwords;this.demoFixtures=demoFixtures;
    }
    @Transactional public UserAccount register(String email,String password,String name) {
        UUID id=UUID.randomUUID();String context=null;
        DemoFixtures fixtures=demoFixtures.getIfAvailable();
        if(fixtures!=null) {context="visitor-"+UUID.randomUUID();fixtures.seed(context);}
        db.update("INSERT INTO app_user(id,email,display_name,password_hash,role,demo_context) VALUES(?,?,?,?,'GUEST',?)",
            id,email.toLowerCase(Locale.ROOT),name,passwords.encode(password),context);
        return users.findById(id).orElseThrow();
    }
    @Transactional public void rename(UserAccount user,String name) {
        db.update("UPDATE app_user SET display_name=? WHERE id=?",name,user.id);user.displayName=name;
    }
}
