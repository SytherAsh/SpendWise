package com.spendwisebackend.spendwisebackend.config.JWT;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.spendwisebackend.spendwisebackend.user.CustomUserDetails;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private final JWTService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (token != null) {
                if (tokenBlacklistService.isBlacklisted(token)) {
                    log.warn("Attempted access with blacklisted token");
                    // Instead of returning SC_UNAUTHORIZED, we just don't set the authentication.
                    // This allows requests to public endpoints to proceed.
                } else if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    if (jwtService.isTokenValid(token)) {
                        Claims claims = jwtService.extractAllClaims(token);

                        String userIdStr = claims.get(JWTService.CLAIM_USER_ID, String.class);
                        UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;
                        String username = claims.getSubject();
                        String role = claims.get(JWTService.CLAIM_ROLE, String.class);
                        String name = claims.get(JWTService.CLAIM_FULL_NAME, String.class);
                        Boolean isActive = claims.get(JWTService.CLAIM_IS_ACTIVE, Boolean.class);

                        if (username != null && role != null) {
                            String formattedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                            
                            List<SimpleGrantedAuthority> authorities = Collections
                                    .singletonList(new SimpleGrantedAuthority(formattedRole));

                            CustomUserDetails userDetails = new CustomUserDetails(
                                    userId, username, "", isActive != null && isActive, role, name, username);

                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, authorities);

                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                            log.debug("User {} authenticated via JWT", username);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Authentication failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // 1. Try Authorization Header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. Fallback to Cookies
        return extractTokenFromCookies(request);
    }

    private String extractTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}