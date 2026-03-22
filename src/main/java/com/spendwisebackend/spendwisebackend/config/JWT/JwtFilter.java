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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JWTService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractTokenFromCookies(request);


            if (token != null) {
                if (tokenBlacklistService.isBlacklisted(token)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }


                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    
                    if (jwtService.isTokenValid(token)) {

                        UUID userId = jwtService.extractUserId(token);
                        String username = jwtService.extractUserName(token);
                        String role = jwtService.extractRole(token);
                        String name = jwtService.extractFullName(token);
                        Boolean isActive = jwtService.extractIsActive(token);

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
                        }
                    }
                }
            }
        } catch (Exception e) {
          
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
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