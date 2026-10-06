package org.kinal.fastdelivery.service;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.kinal.fastdelivery.dto.auth.*;
import org.kinal.fastdelivery.entity.Usuario;
import org.kinal.fastdelivery.enums.Rol;
import org.kinal.fastdelivery.exception.EmailAlreadyExistsException;
import org.kinal.fastdelivery.repository.UsuarioRepository;
import org.kinal.fastdelivery.security.JwtService;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final UsuarioRepository usuarios;
  private final PasswordEncoder encoder;
  private final AuthenticationManager auth;
  private final JwtService jwt;

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    if (usuarios.existsByEmail(email))
      throw new EmailAlreadyExistsException("El email ya está registrado");
    var u = new Usuario();
    u.setNombre(request.nombre().trim());
    u.setDireccion(request.direccion().trim());
    u.setTelefono(request.telefono().trim());
    u.setEmail(email);
    u.setPassword(encoder.encode(request.password()));
    u.setRol(Rol.CLIENTE);
    usuarios.saveAndFlush(u);
    return response(u);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    auth.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
    return response(
        usuarios
            .findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas")));
  }

  private AuthResponse response(Usuario u) {
    return new AuthResponse(jwt.generateToken(u), "Bearer", u.getEmail(), u.getRol());
  }
}
