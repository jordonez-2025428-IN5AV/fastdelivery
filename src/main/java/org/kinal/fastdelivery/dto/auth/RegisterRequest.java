package org.kinal.fastdelivery.dto.auth;

import jakarta.validation.constraints.*;
import org.kinal.fastdelivery.enums.*;

public record RegisterRequest(
    @NotBlank @Size(max = 120) String nombre,
    @NotBlank @Size(max = 255) String direccion,
    @NotBlank @Size(max = 30) String telefono,
    @NotBlank @Email @Size(max = 160) String email,
    @NotBlank @Size(min = 6, max = 72) String password) {}
