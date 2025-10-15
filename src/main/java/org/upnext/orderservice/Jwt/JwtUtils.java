package org.upnext.orderservice.Jwt;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.upnext.sharedlibrary.Dtos.UserDto;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtUtils {
    @Value("${jwt.secret.key}")
    private String secretKey;

    public UserDto getUserFromToken(String token) {
        UserDto user = new UserDto();
        Claims claims = extractAllClaims(token);
        user.setId(Long.parseLong(claims.getSubject()));
        user.setName(claims.get("name", String.class));
        user.setEmail(claims.get("email", String.class));
        user.setRole(claims.get("role", List.class));
        user.setPhoneNumber(claims.get("phoneNumber", String.class));
        user.setAddress(claims.get("address", String.class));
        return user;
    }
    public String extractId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private  Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token).getBody();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }


    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String getJwtFromHeader(HttpServletRequest request) {
        return request.getHeader("X-User");
    }
    public boolean isValidToken(String token) {
        extractAllClaims(token);
        return !isTokenExpired(token);
    }
}
