package org.kinal.fastdelivery.dto.comercio;

import jakarta.validation.constraints.*;
import org.kinal.fastdelivery.enums.*;

public record ComercioResponse(
    Long id, String nombre, CategoriaComercio categoria, String direccion, boolean abierto) {}
