package org.kinal.fastdelivery.dto.pedido;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.kinal.fastdelivery.enums.*;

public record DetallePedidoResponse(
    Long id,
    Long productoId,
    String productoNombre,
    Integer cantidad,
    BigDecimal precioUnitario,
    BigDecimal subtotal) {}
