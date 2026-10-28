package com.celcioblond.pms.config;

import com.celcioblond.pms.model.Role;
import com.celcioblond.pms.model.User;
import com.celcioblond.pms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs once at application startup.
 *
 * If no ADMIN user exists in the database, it creates one using the
 * admin.* properties (fed by the ADMIN_* environment variables).
 *
 * After the first admin is created, this bean does nothing on subsequent
 * startups — it will NOT overwrite an existing admin.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    static final int MIN_PASSWORD_LENGTH = 8;
    static final int MAX_PASSWORD_LENGTH = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.name}")
    private String adminName;

    @Override
    public void run(String... args) {
        if (!userRepository.findByRole(Role.ADMIN).isEmpty()) {
            log.info("Admin user already exists — skipping seed.");
            return;
        }

        // Same bounds as every other account (UserCreate); BCrypt ignores bytes beyond 72
        if (adminPassword == null || adminPassword.length() < MIN_PASSWORD_LENGTH
                || adminPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalStateException("ADMIN_PASSWORD must be between " + MIN_PASSWORD_LENGTH
                    + " and " + MAX_PASSWORD_LENGTH + " characters.");
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setName(adminName);
        admin.setRole(Role.ADMIN);
        admin.setPosition("Administrator");
        admin.setDepartment("Management");

        userRepository.save(admin);

        log.info("Bootstrap admin '{}' <{}> created.", adminUsername, adminEmail);
    }
}
