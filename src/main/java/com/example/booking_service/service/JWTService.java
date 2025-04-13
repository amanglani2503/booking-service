package com.example.booking_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JWTService {

    private static final Logger logger = LoggerFactory.getLogger(JWTService.class);

    @Value("${jwt.secret}")
    private String secretKey;

    public String extractRole(String token) {
        logger.debug("Extracting role from JWT");
        try {
            String role = extractAllClaims(token).get("role", String.class);
            logger.info("Role extracted: {}", role);
            return role;
        } catch (Exception e) {
            logger.error("Failed to extract role from token", e);
            throw e;
        }
    }

    private Claims extractAllClaims(String token) {
        logger.debug("Extracting all claims from token");
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            logger.debug("Claims successfully extracted");
            return claims;
        } catch (Exception e) {
            logger.error("Invalid JWT token", e);
            throw e;
        }
    }

    private SecretKey getKey() {
        logger.debug("Decoding and building secret key");
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        logger.debug("Extracting username from token");
        try {
            String username = extractClaim(token, Claims::getSubject);
            logger.info("Username extracted: {}", username);
            return username;
        } catch (Exception e) {
            logger.error("Failed to extract username from token", e);
            throw e;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        logger.debug("Validating token for user: {}", userDetails.getUsername());
        try {
            final String userName = extractUsername(token);
            boolean valid = userName.equals(userDetails.getUsername()) && !isTokenExpired(token);
            logger.info("Token validation result for user {}: {}", userDetails.getUsername(), valid);
            return valid;
        } catch (Exception e) {
            logger.error("Token validation failed", e);
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        boolean expired = expiration.before(new Date());
        logger.debug("Token expiration date: {}, is expired: {}", expiration, expired);
        return expired;
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
