package org.kinal.fastdelivery.dto.pedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.kinal.fastdelivery.enums.*;

public record CrearPedidoRequest(
    @NotEmpty @Size(max = 100) List<@NotNull @Valid PedidoItemRequest> items) {}
