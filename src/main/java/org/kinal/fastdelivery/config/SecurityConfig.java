package org.kinal.fastdelivery.config;

import lombok.RequiredArgsConstructor;
import org.kinal.fastdelivery.security.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final CustomUserDetailsService users;
  private final JwtAuthenticationFilter jwt;
  private final SecurityErrorWriter errors;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(PasswordEncoder encoder) {
    var provider = new DaoAuthenticationProvider(users);
    provider.setPasswordEncoder(encoder);
    return new ProviderManager(provider);
  }

  @Bean
  public FilterRegistrationBean<JwtAuthenticationFilter> jwtRegistration() {
    var registration = new FilterRegistrationBean<>(jwt);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(c -> c.disable())
        .formLogin(c -> c.disable())
        .httpBasic(c -> c.disable())
        .logout(c -> c.disable())
        .authorizeHttpRequests(
            a -> a.requestMatchers("/api/v1/auth/**").permitAll().anyRequest().authenticated())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (r, s, x) ->
                            errors.write(r, s, HttpStatus.UNAUTHORIZED, "Autenticación requerida"))
                    .accessDeniedHandler(
                        (r, s, x) ->
                            errors.write(
                                r,
                                s,
                                HttpStatus.FORBIDDEN,
                                "No tienes permiso para realizar esta operación")))
        .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
