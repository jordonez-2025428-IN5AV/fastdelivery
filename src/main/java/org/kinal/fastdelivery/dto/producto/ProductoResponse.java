package org.kinal.fastdelivery.dto.producto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.kinal.fastdelivery.enums.*;

public record ProductoResponse(
    Long id,
    Long comercioId,
    String nombre,
    BigDecimal precio,
    Integer stock,
    boolean disponible) {}
