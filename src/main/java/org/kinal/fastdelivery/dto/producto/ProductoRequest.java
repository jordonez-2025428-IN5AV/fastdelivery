package org.kinal.fastdelivery.dto.producto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.kinal.fastdelivery.enums.*;

public record ProductoRequest(
    @NotBlank @Size(max = 120) String nombre,
    @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal precio,
    @NotNull @PositiveOrZero Integer stock,
    @NotNull Boolean disponible) {}
