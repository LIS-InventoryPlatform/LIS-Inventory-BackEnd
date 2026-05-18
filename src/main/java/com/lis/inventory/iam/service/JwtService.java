package com.lis.inventory.iam.service;

import com.lis.inventory.iam.entity.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    /**
     * Genera el JWT propio de la aplicación con rol y permisos del usuario.
     * Payload:
     *   sub         → email
     *   name        → nombre completo
     *   role        → nombre del rol asignado
     *   permissions → lista de nombres de permisos del rol
     */
    public String generateToken(AppUser user) {
        List<String> permissions = (user.getRole() != null)
                ? user.getRole().getPermissions().stream()
                        .map(p -> p.getName())
                        .sorted()
                        .toList()
                : List.of();

        String roleName = (user.getRole() != null) ? user.getRole().getName() : null;

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("name", user.getFullName())
                .claim("role", roleName)
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
