package org.kinal.fastdelivery.dto.auth;

import jakarta.validation.constraints.*;
import org.kinal.fastdelivery.enums.*;

public record AuthResponse(String token, String type, String email, Rol rol) {}
