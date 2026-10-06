package org.kinal.fastdelivery.dto.error;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import org.kinal.fastdelivery.enums.*;

public record ErrorResponse(
    LocalDateTime timestamp, int status, String error, String message, String path) {}
