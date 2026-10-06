package org.kinal.fastdelivery.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.kinal.fastdelivery.dto.auth.*;
import org.kinal.fastdelivery.service.AuthService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService auth;

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest r) {
    return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(r));
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest r) {
    return auth.login(r);
  }
}
