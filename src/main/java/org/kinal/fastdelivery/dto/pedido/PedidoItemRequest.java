package org.kinal.fastdelivery.dto.pedido;

import jakarta.validation.constraints.*;
import org.kinal.fastdelivery.enums.*;

public record PedidoItemRequest(
    @NotNull @Positive Long productoId, @NotNull @Positive Integer cantidad) {}
