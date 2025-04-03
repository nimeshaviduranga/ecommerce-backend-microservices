package com.ecommerce.paymentservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && validateToken(jwt)) {
                Claims claims = extractClaims(jwt);

                String username = claims.getSubject();
                log.debug("JWT Token username: {}", username);

                // Extract roles from the claims
                String rolesString = claims.get("roles", String.class);
                if (rolesString == null || rolesString.trim().isEmpty()) {
                    rolesString = "ROLE_USER"; // Default role
                }
                log.debug("JWT Token roles: {}", rolesString);

                List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesString.split(","))
                        .filter(role -> !role.trim().isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // Create user principal with ID from token
                UserPrincipal userPrincipal = new UserPrincipal();
                userPrincipal.setUsername(username);

                // Extract user ID if available
                if (claims.get("userId") != null) {
                    try {
                        userPrincipal.setId(Long.parseLong(claims.get("userId").toString()));
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse userId from token: {}", e.getMessage());
                    }
                }

                // Default to user ID 1 if not found in token (for testing only)
                if (userPrincipal.getId() == null) {
                    userPrincipal.setId(1L);
                    log.debug("Using default user ID: 1");
                }

                // Extract auth ID if available
                if (claims.get("authId") != null) {
                    userPrincipal.setAuthId(claims.get("authId").toString());
                }

                // Store the raw JWT token as credentials for service-to-service calls
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userPrincipal, jwt, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims extractClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}