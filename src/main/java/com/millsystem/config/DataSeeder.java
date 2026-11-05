package com.millsystem.config;

import com.millsystem.entity.User;
import com.millsystem.feature.FeatureFlagService;
import org.springframework.beans.factory.annotation.Value;
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
    private final FeatureFlagService featureFlagService;

    private static final String DEFAULT_SUPER_ADMIN_PASSWORD = "Admin@123";
    @Value("${app.super-admin.password:" + DEFAULT_SUPER_ADMIN_PASSWORD + "}")
    private String superAdminPassword;

    @Value("${app.dev-admin.username:devadmin}")
    private String devAdminUsername;
    private static final String DEFAULT_DEV_ADMIN_PASSWORD = "DevAdmin@123";
    @Value("${app.dev-admin.password:" + DEFAULT_DEV_ADMIN_PASSWORD + "}")
    private String devAdminPassword;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
                      FeatureFlagService featureFlagService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.featureFlagService = featureFlagService;
    }

    @Override
    public void run(String... args) {
        featureFlagService.seedMissing();
        if (userRepository.findByUsername("superadmin").isEmpty()) {
            User superAdmin = new User();
            superAdmin.setUsername("superadmin");
            superAdmin.setPassword(passwordEncoder.encode(superAdminPassword));
            superAdmin.setRole(User.Role.SUPER_ADMIN);
            superAdmin.setFullName("Super Administrator");
            superAdmin.setActive(true);
            userRepository.save(superAdmin);
            log.info("Super admin created. Username: superadmin | Password: {}",
                    DEFAULT_SUPER_ADMIN_PASSWORD.equals(superAdminPassword) ? DEFAULT_SUPER_ADMIN_PASSWORD : "(from SUPER_ADMIN_PASSWORD)");
        }
        seedDevAdmin();
    }

    /** Creates the dev admin once, like the super admin. DEV_ADMIN_PASSWORD overrides the default password. */
    private void seedDevAdmin() {
        if (userRepository.findByUsername(devAdminUsername).isPresent()) return;
        User dev = new User();
        dev.setUsername(devAdminUsername);
        dev.setPassword(passwordEncoder.encode(devAdminPassword));
        dev.setRole(User.Role.DEV_ADMIN);
        dev.setFullName("Dev Administrator");
        dev.setActive(true);
        userRepository.save(dev);
        log.info("Dev admin created. Username: {} | Password: {}", devAdminUsername,
                DEFAULT_DEV_ADMIN_PASSWORD.equals(devAdminPassword) ? DEFAULT_DEV_ADMIN_PASSWORD : "(from DEV_ADMIN_PASSWORD)");
    }
}
