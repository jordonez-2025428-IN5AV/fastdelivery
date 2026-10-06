package org.kinal.fastdelivery.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  private final CustomUserDetailsService users;
  private final SecurityErrorWriter errors;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest r) {
    return r.getServletPath().startsWith("/api/v1/auth/");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain chain)
      throws ServletException, IOException {
    String header = r.getHeader("Authorization");
    if (header != null) {
      try {
        if (!header.startsWith("Bearer ") || header.length() == 7)
          throw new IllegalArgumentException("Bearer inválido");
        var claims = jwt.parse(header.substring(7));
        var user = users.loadUserByUsername(claims.getSubject());
        if (!user.getUsername().equals(claims.get("email", String.class))
            || user.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_" + claims.get("rol", String.class))))
          throw new IllegalArgumentException("Claims inválidos");
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(r));
        SecurityContextHolder.getContext().setAuthentication(auth);
      } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
        SecurityContextHolder.clearContext();
        errors.write(r, s, HttpStatus.UNAUTHORIZED, "Token inválido o expirado");
        return;
      }
    }
    chain.doFilter(r, s);
  }
}
