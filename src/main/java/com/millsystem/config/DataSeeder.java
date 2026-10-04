package com.millsystem.config;

import com.millsystem.entity.User;
import com.millsystem.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("superadmin").isEmpty()) {
            User superAdmin = new User();
            superAdmin.setUsername("superadmin");
            superAdmin.setPassword(passwordEncoder.encode("Admin@123"));
            superAdmin.setRole(User.Role.SUPER_ADMIN);
            superAdmin.setFullName("Super Administrator");
            superAdmin.setActive(true);
            userRepository.save(superAdmin);
            log.info("Super admin created. Username: superadmin | Password: Admin@123");
        }
    }
}
