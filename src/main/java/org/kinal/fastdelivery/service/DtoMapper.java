package org.kinal.fastdelivery.service;

import org.kinal.fastdelivery.dto.comercio.ComercioResponse;
import org.kinal.fastdelivery.dto.pedido.*;
import org.kinal.fastdelivery.dto.producto.ProductoResponse;
import org.kinal.fastdelivery.entity.*;

public final class DtoMapper {
  private DtoMapper() {
    throw new AssertionError("Clase utilitaria");
  }

  public static ComercioResponse comercio(Comercio c) {
    return new ComercioResponse(
        c.getId(), c.getNombre(), c.getCategoria(), c.getDireccion(), c.isAbierto());
  }

  public static ProductoResponse producto(Producto p) {
    return new ProductoResponse(
        p.getId(),
        p.getComercio().getId(),
        p.getNombre(),
        p.getPrecio(),
        p.getStock(),
        p.isDisponible());
  }

  public static PedidoResponse pedido(Pedido p) {
    var detalles =
        p.getDetalles().stream()
            .map(
                d ->
                    new DetallePedidoResponse(
                        d.getId(),
                        d.getProducto().getId(),
                        d.getProducto().getNombre(),
                        d.getCantidad(),
                        d.getPrecioUnitario(),
                        d.getSubtotal()))
            .toList();
    return new PedidoResponse(
        p.getId(),
        p.getCliente().getId(),
        p.getCliente().getNombre(),
        p.getCliente().getDireccion(),
        p.getCliente().getTelefono(),
        p.getRepartidor() == null ? null : p.getRepartidor().getId(),
        p.getFechaPedido(),
        p.getCostoEnvio(),
        p.getMontoTotal(),
        p.getEstado(),
        detalles);
  }
}
