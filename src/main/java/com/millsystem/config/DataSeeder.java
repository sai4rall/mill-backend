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

    private final org.springframework.core.env.Environment environment;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
                      FeatureFlagService featureFlagService, org.springframework.core.env.Environment environment) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.featureFlagService = featureFlagService;
        this.environment = environment;
    }

    /**
     * Demo defaults are fine locally, but production must never create an account with a published or weak password:
     * the prod profile refuses to start in that case. (Existing accounts are untouched; the password is only used on creation.)
     */
    private String initialPassword(String account, String configured, String knownDefault) {
        boolean prod = java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod");
        boolean weak = configured == null || configured.length() < 12 || knownDefault.equals(configured);
        if (prod && weak) {
            throw new IllegalStateException("Refusing to create " + account + " with a default or weak password: set a strong (12+ characters) "
                    + ("superadmin".equals(account) ? "SUPER_ADMIN_PASSWORD" : "DEV_ADMIN_PASSWORD"));
        }
        if (knownDefault.equals(configured)) {
            log.warn("Creating {} with the published demo password. Change it after signing in; never use it in production.", account);
        }
        return configured;
    }

    @Override
    public void run(String... args) {
        featureFlagService.seedMissing();
        if (userRepository.findByUsername("superadmin").isEmpty()) {
            User superAdmin = new User();
            superAdmin.setUsername("superadmin");
            superAdmin.setPassword(passwordEncoder.encode(initialPassword("superadmin", superAdminPassword, DEFAULT_SUPER_ADMIN_PASSWORD)));
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
        dev.setPassword(passwordEncoder.encode(initialPassword(devAdminUsername, devAdminPassword, DEFAULT_DEV_ADMIN_PASSWORD)));
        dev.setRole(User.Role.DEV_ADMIN);
        dev.setFullName("Dev Administrator");
        dev.setActive(true);
        userRepository.save(dev);
        log.info("Dev admin created. Username: {} | Password: {}", devAdminUsername,
                DEFAULT_DEV_ADMIN_PASSWORD.equals(devAdminPassword) ? DEFAULT_DEV_ADMIN_PASSWORD : "(from DEV_ADMIN_PASSWORD)");
    }
}
