package com.example.Project_Management.config;

import com.example.Project_Management.model.User;
import com.example.Project_Management.repo.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs once at application startup.
 *
 * If no ADMIN user exists in the database, it creates one using credentials
 * from environment variables (or application.properties defaults).
 *
 * After the first admin is created, this bean does nothing on subsequent
 * startups — it will NOT overwrite an existing admin.
 *
 * Environment variables (set these in your .env or deployment config):
 *   ADMIN_USERNAME  — defaults to "admin"
 *   ADMIN_EMAIL     — defaults to "admin@company.com"
 *   ADMIN_PASSWORD  — defaults to "ChangeMe123!" (CHANGE THIS in production)
 *   ADMIN_NAME      — defaults to "System Administrator"
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepo userRepo;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.email:admin@company.com}")
    private String adminEmail;

    @Value("${admin.password:ChangeMe123!}")
    private String adminPassword;

    @Value("${admin.name:System Administrator}")
    private String adminName;

    @Override
    public void run(String... args) {
        // Check if any admin already exists — if so, skip completely
        if (!userRepo.findByRole("ADMIN").isEmpty()) {
            log.info("Admin user already exists — skipping seed.");
            return;
        }

        // No admin found — create the bootstrap admin
        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(encoder.encode(adminPassword));
        admin.setName(adminName);
        admin.setRole("ADMIN");
        admin.setPosition("Administrator");
        admin.setDepartment("Management");

        userRepo.save(admin);

        log.info("========================================");
        log.info("Bootstrap admin created successfully.");
        log.info("Username : {}", adminUsername);
        log.info("Email    : {}", adminEmail);
        log.info("IMPORTANT: Change the default password immediately!");
        log.info("========================================");
    }
}