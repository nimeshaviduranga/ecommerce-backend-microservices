package com.ecommerce.authservice.config;

import com.ecommerce.authservice.entity.Role;
import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.repository.RoleRepository;
import com.ecommerce.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("dev") // Only run in development environment

/**
 *Database Initialization
 */
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (roleRepository.count() == 0) {
                log.info("Initializing roles...");
                roleRepository.save(new Role("ROLE_USER"));
                roleRepository.save(new Role("ROLE_ADMIN"));
                log.info("Roles initialized");
            }

            if (userRepository.count() == 0) {
                log.info("Initializing sample users...");

                // Create admin user
                User adminUser = new User();
                adminUser.setUsername("admin");
                adminUser.setEmail("admin@example.com");
                adminUser.setPassword(passwordEncoder.encode("admin123"));
                adminUser.setFirstName("Admin");
                adminUser.setLastName("User");

                Set<Role> adminRoles = new HashSet<>();
                roleRepository.findByName("ROLE_ADMIN").ifPresent(adminRoles::add);
                roleRepository.findByName("ROLE_USER").ifPresent(adminRoles::add);
                adminUser.setRoles(adminRoles);

                userRepository.save(adminUser);

                // Create regular user
                User regularUser = new User();
                regularUser.setUsername("user");
                regularUser.setEmail("user@example.com");
                regularUser.setPassword(passwordEncoder.encode("user123"));
                regularUser.setFirstName("Regular");
                regularUser.setLastName("User");

                Set<Role> userRoles = new HashSet<>();
                roleRepository.findByName("ROLE_USER").ifPresent(userRoles::add);
                regularUser.setRoles(userRoles);

                userRepository.save(regularUser);

                log.info("Sample users initialized");
            }
        };
    }
}

