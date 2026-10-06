package org.kinal.fastdelivery.dto.auth;

import jakarta.validation.constraints.*;
import org.kinal.fastdelivery.enums.*;

public record LoginRequest(
    @NotBlank @Email @Size(max = 160) String email, @NotBlank @Size(max = 72) String password) {}
