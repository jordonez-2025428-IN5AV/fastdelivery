package org.kinal.fastdelivery.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.kinal.fastdelivery.dto.pedido.*;
import org.kinal.fastdelivery.entity.*;
import org.kinal.fastdelivery.enums.*;
import org.kinal.fastdelivery.exception.*;
import org.kinal.fastdelivery.repository.*;
import org.kinal.fastdelivery.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PedidoService {
  private static final BigDecimal COSTO_ENVIO = new BigDecimal("20.00");
  private static final List<EstadoPedido> ACTIVOS =
      List.of(EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION, EstadoPedido.EN_CAMINO);
  private final PedidoRepository pedidos;
  private final ProductoRepository productos;
  private final UsuarioRepository usuarios;
  private final CurrentUser current;

  @Transactional
  public PedidoResponse crearPedido(CrearPedidoRequest r) {
    var cliente = current.get();
    if (cliente.getRol() != Rol.CLIENTE)
      throw new UnauthorizedOperationException("Se requiere CLIENTE");
    // Agrupar duplicados y bloquear siempre en orden para evitar sobreventa y deadlocks.
    var cantidades = new TreeMap<Long, Integer>();
    for (var item : r.items()) {
      try {
        cantidades.merge(item.productoId(), item.cantidad(), Math::addExact);
      } catch (ArithmeticException e) {
        throw new BadRequestException("Cantidad demasiado grande");
      }
    }
    var pedido = new Pedido();
    pedido.setCliente(cliente);
    pedido.setFechaPedido(LocalDateTime.now());
    pedido.setCostoEnvio(COSTO_ENVIO);
    pedido.setEstado(EstadoPedido.PENDIENTE);
    BigDecimal subtotal = BigDecimal.ZERO.setScale(2);
    for (var item : cantidades.entrySet()) {
      var producto =
          productos
              .findByIdForUpdate(item.getKey())
              .orElseThrow(
                  () -> new ResourceNotFoundException("Producto no encontrado: " + item.getKey()));
      if (!producto.isDisponible() || !producto.getComercio().isAbierto())
        throw new BadRequestException("Producto o comercio no disponible: " + producto.getNombre());
      if (producto.getStock() < item.getValue())
        throw new InsufficientStockException("Stock insuficiente para " + producto.getNombre());
      var detalle = new DetallePedido();
      detalle.setPedido(pedido);
      detalle.setProducto(producto);
      detalle.setCantidad(item.getValue());
      detalle.setPrecioUnitario(producto.getPrecio());
      detalle.setSubtotal(
          producto.getPrecio().multiply(BigDecimal.valueOf(item.getValue())).setScale(2));
      pedido.getDetalles().add(detalle);
      subtotal = subtotal.add(detalle.getSubtotal());
      producto.setStock(producto.getStock() - item.getValue());
    }
    pedido.setMontoTotal(subtotal.add(COSTO_ENVIO));
    return DtoMapper.pedido(pedidos.saveAndFlush(pedido));
  }

  @Transactional(readOnly = true)
  public List<PedidoResponse> misPedidos() {
    return pedidos.findByClienteIdOrderByFechaPedidoDesc(current.get().getId()).stream()
        .map(DtoMapper::pedido)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PedidoResponse> disponibles() {
    var u = current.get();
    return pedidos.findDisponibles(ACTIVOS, u.getRol() == Rol.ADMIN, u.getId()).stream()
        .map(DtoMapper::pedido)
        .toList();
  }

  @Transactional(readOnly = true)
  public PedidoResponse consultar(Long id) {
    var p =
        pedidos
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
    var u = current.get();
    if (u.getRol() == Rol.CLIENTE && !p.getCliente().getId().equals(u.getId()))
      throw new UnauthorizedOperationException("Pedido ajeno");
    if (u.getRol() == Rol.REPARTIDOR
        && (p.getRepartidor() == null || !p.getRepartidor().getId().equals(u.getId())))
      throw new UnauthorizedOperationException("Pedido no asignado");
    return DtoMapper.pedido(p);
  }

  @Transactional
  public PedidoResponse actualizarEstado(Long id, ActualizarEstadoRequest r) {
    var u = current.get();
    var p = bloquear(id);
    EstadoPedido siguiente =
        switch (p.getEstado()) {
          case PENDIENTE -> EstadoPedido.EN_PREPARACION;
          case EN_PREPARACION -> EstadoPedido.EN_CAMINO;
          case EN_CAMINO -> EstadoPedido.ENTREGADO;
          default -> null;
        };
    if (siguiente == null || r.estado() != siguiente)
      throw new InvalidStatusException(
          "Transición inválida: " + p.getEstado() + " -> " + r.estado());
    if (u.getRol() == Rol.REPARTIDOR) {
      if (p.getRepartidor() != null && !p.getRepartidor().getId().equals(u.getId()))
        throw new UnauthorizedOperationException("Pedido asignado a otro repartidor");
      if (p.getRepartidor() == null) p.setRepartidor(u);
    } else if (u.getRol() != Rol.ADMIN)
      throw new UnauthorizedOperationException("Se requiere ADMIN o REPARTIDOR");
    if (r.estado() == EstadoPedido.EN_CAMINO && p.getRepartidor() == null)
      throw new BadRequestException("Asigna un repartidor antes de iniciar el envío");
    p.setEstado(r.estado());
    return DtoMapper.pedido(p);
  }

  @Transactional
  public PedidoResponse asignar(Long id, AsignarRepartidorRequest r) {
    if (current.get().getRol() != Rol.ADMIN)
      throw new UnauthorizedOperationException("Se requiere ADMIN");
    var p = bloquear(id);
    if (p.getEstado() != EstadoPedido.PENDIENTE && p.getEstado() != EstadoPedido.EN_PREPARACION)
      throw new InvalidStatusException("Solo se asigna antes de iniciar el envío");
    var u =
        usuarios
            .findById(r.repartidorId())
            .orElseThrow(() -> new ResourceNotFoundException("Repartidor no encontrado"));
    if (u.getRol() != Rol.REPARTIDOR)
      throw new BadRequestException("El usuario debe ser REPARTIDOR");
    p.setRepartidor(u);
    return DtoMapper.pedido(p);
  }

  @Transactional
  public PedidoResponse cancelarPedido(Long id) {
    var p = bloquear(id);
    var u = current.get();
    if (u.getRol() != Rol.ADMIN
        && (u.getRol() != Rol.CLIENTE || !p.getCliente().getId().equals(u.getId())))
      throw new UnauthorizedOperationException("No puedes cancelar este pedido");
    if (p.getEstado() != EstadoPedido.PENDIENTE)
      throw new InvalidStatusException("Solo se puede cancelar un pedido PENDIENTE");
    var detalles = new ArrayList<>(p.getDetalles());
    detalles.sort(Comparator.comparing(d -> d.getProducto().getId()));
    for (var d : detalles) {
      var producto =
          productos
              .findByIdForUpdate(d.getProducto().getId())
              .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
      try {
        producto.setStock(Math.addExact(producto.getStock(), d.getCantidad()));
      } catch (ArithmeticException e) {
        throw new BadRequestException("Stock fuera de rango");
      }
    }
    p.setEstado(EstadoPedido.CANCELADO);
    return DtoMapper.pedido(p);
  }

  private Pedido bloquear(Long id) {
    return pedidos
        .findByIdForUpdate(id)
        .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
  }
}
