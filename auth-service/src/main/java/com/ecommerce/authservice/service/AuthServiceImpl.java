package com.ecommerce.authservice.service;

import com.ecommerce.authservice.dto.AuthRequest;
import com.ecommerce.authservice.dto.AuthResponse;
import com.ecommerce.authservice.dto.RegisterRequest;
import com.ecommerce.authservice.entity.Role;
import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.exception.CustomException;
import com.ecommerce.authservice.repository.RoleRepository;
import com.ecommerce.authservice.repository.UserRepository;
import com.ecommerce.authservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Override
    public AuthResponse login(AuthRequest authRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String accessToken = tokenProvider.generateToken(authentication, false);
            String refreshToken = tokenProvider.generateToken(authentication, true);

            // Get user details from the repository directly to avoid potential recursion
            User user = userRepository.findByUsername(authRequest.getUsername())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "User not found"));

            // Get roles as strings
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            // Build and return the response
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .roles(roles)
                    .build();
        } catch (Exception e) {
            log.error("Login failed", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        // Check if username is already taken
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Username is already taken");
        }

        // Check if email is already in use
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Email is already in use");
        }

        // Create new user
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());

        // Set user roles
        Set<Role> roles = new HashSet<>();

        if (registerRequest.getRoles() == null || registerRequest.getRoles().isEmpty()) {
            // Default role is USER
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
            roles.add(userRole);
        } else {
            registerRequest.getRoles().forEach(roleName -> {
                String role = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
                Role userRole = roleRepository.findByName(role)
                        .orElseGet(() -> roleRepository.save(new Role(role)));
                roles.add(userRole);
            });
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        // Create authentication token and generate JWT
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                savedUser.getUsername(),
                null,
                roles.stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList())
        );

        String accessToken = tokenProvider.generateToken(authentication, false);
        String refreshToken = tokenProvider.generateToken(authentication, true);

        List<String> roleNames = roles.stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .roles(roleNames)
                .build();
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "User not found"));

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                authorities
        );

        String newAccessToken = tokenProvider.generateToken(authentication, false);
        String newRefreshToken = tokenProvider.generateToken(authentication, true);

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }
}
