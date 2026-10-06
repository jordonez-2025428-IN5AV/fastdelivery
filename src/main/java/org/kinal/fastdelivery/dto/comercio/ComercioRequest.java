package org.kinal.fastdelivery.dto.comercio;

import jakarta.validation.constraints.*;
import org.kinal.fastdelivery.enums.*;

public record ComercioRequest(
    @NotBlank @Size(max = 120) String nombre,
    @NotNull CategoriaComercio categoria,
    @NotBlank @Size(max = 255) String direccion,
    @NotNull Boolean abierto) {}
