package org.kinal.fastdelivery.security;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.kinal.fastdelivery.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
  private final UsuarioRepository usuarios;

  @Override
  public UserDetails loadUserByUsername(String email) {
    var u =
        usuarios
            .findByEmail(email.trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    return User.withUsername(u.getEmail())
        .password(u.getPassword())
        .roles(u.getRol().name())
        .build();
  }
}
