package org.kinal.fastdelivery.dto.pedido;

import jakarta.validation.constraints.*;
import org.kinal.fastdelivery.enums.*;

public record AsignarRepartidorRequest(@NotNull @Positive Long repartidorId) {}
