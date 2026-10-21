package no.casevault.config;

import no.casevault.model.District;
import no.casevault.model.Role;
import no.casevault.model.User;
import no.casevault.repository.UserRepository;
import no.casevault.service.PasswordHasher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Order(1)
public class AdminAccountSeeder implements ApplicationRunner {

    private static final String USERNAME = "Admin";
    private static final String EMAIL = "admin@casevault.local";
    private static final String PASSWORD = "adminadmin";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AdminAccountSeeder(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsernameIgnoreCase(USERNAME)) {
            return;
        }

        userRepository.save(new User(
            USERNAME, EMAIL, "Default Administrator", passwordHasher.hash(PASSWORD), District.NORTH, Role.ADMIN
        ));
    }
}
