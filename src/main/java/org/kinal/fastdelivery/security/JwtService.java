package org.kinal.fastdelivery.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.kinal.fastdelivery.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final javax.crypto.SecretKey key;
  private final long expiration;

  public JwtService(
      @Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expiration) {
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
      throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 bytes");
    if (expiration <= 0)
      throw new IllegalArgumentException("JWT_EXPIRATION debe ser positiva (milisegundos)");
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expiration = expiration;
  }

  public String generateToken(Usuario usuario) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(usuario.getEmail())
        .claim("email", usuario.getEmail())
        .claim("rol", usuario.getRol().name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusMillis(expiration)))
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
