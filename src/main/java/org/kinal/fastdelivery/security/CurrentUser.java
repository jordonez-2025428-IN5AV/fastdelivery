package org.kinal.fastdelivery.security;

import lombok.RequiredArgsConstructor;
import org.kinal.fastdelivery.entity.Usuario;
import org.kinal.fastdelivery.repository.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUser {
  private final UsuarioRepository usuarios;

  public Usuario get() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null || !a.isAuthenticated())
      throw new AuthenticationCredentialsNotFoundException("Autenticación requerida");
    return usuarios
        .findByEmail(a.getName())
        .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Usuario no encontrado"));
  }
}
