package com.spendwisebackend.spendwisebackend.config.JWT;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.spendwisebackend.spendwisebackend.user.CustomUserDetails;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JWTService {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_FULL_NAME = "fullName";
    public static final String CLAIM_IS_ACTIVE = "isActive";

    @Value("${security.jwt.secret-key}")
    private String secretkey;
    @Value("${app.jwt.expiration-in-ms}")
    private String jwtExpirationInMs;

    @Value("${app.jwt.remember-me-expiration-in-ms}")
    private String jwtRememberMeExpirationInMs;
    private final TokenBlacklistService blacklist;

    public String generateToken(CustomUserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put(CLAIM_USER_ID, userDetails.getId().toString());
        extraClaims.put(CLAIM_ROLE, userDetails.getRole());
        extraClaims.put(CLAIM_FULL_NAME, userDetails.getName());
        extraClaims.put(CLAIM_IS_ACTIVE, userDetails.isEnabled());

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
                .signWith(getKey())
                .compact();
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretkey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUserName(String token) {

        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUserName(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    public UUID extractUserId(String token) {
        String userId = extractClaim(token, claims -> claims.get(CLAIM_USER_ID, String.class));
        return (userId != null) ? UUID.fromString(userId) : null;
    }


    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_ROLE, String.class));
    }

    public String extractFullName(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_FULL_NAME, String.class));
    }

    public Boolean extractIsActive(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_IS_ACTIVE, Boolean.class));
    }

    public boolean isTokenValid(String token) {
        return !isTokenExpired(token);
    }

    public void addTokenBlacklist(String token) {
        blacklist.addToBlacklist(token, 100000);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}