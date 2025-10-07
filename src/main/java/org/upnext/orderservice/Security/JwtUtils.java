package org.upnext.orderservice.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class JwtUtils {

    private static final String SECRET = "my_super_secret_key_1234567890_abcdefghij";

    private final Key key;
    public JwtUtils() {
        this.key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
    public Claims extractClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (SignatureException e) {
            throw new RuntimeException("Invalid JWT signature", e);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

}
