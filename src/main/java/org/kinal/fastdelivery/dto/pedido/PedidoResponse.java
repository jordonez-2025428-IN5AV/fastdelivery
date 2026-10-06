package org.kinal.fastdelivery.dto.pedido;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.kinal.fastdelivery.enums.*;

public record PedidoResponse(
    Long id,
    Long clienteId,
    String clienteNombre,
    String direccionEntrega,
    String telefonoCliente,
    Long repartidorId,
    LocalDateTime fechaPedido,
    BigDecimal costoEnvio,
    BigDecimal montoTotal,
    EstadoPedido estado,
    List<DetallePedidoResponse> detalles) {}
