package org.kinal.fastdelivery.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.kinal.fastdelivery.dto.pedido.*;
import org.kinal.fastdelivery.service.PedidoService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PedidoController {
  private final PedidoService pedidos;

  @PostMapping
  @PreAuthorize("hasRole('CLIENTE')")
  public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody CrearPedidoRequest r) {
    return ResponseEntity.status(HttpStatus.CREATED).body(pedidos.crearPedido(r));
  }

  @GetMapping("/mis-pedidos")
  @PreAuthorize("hasRole('CLIENTE')")
  public List<PedidoResponse> propios() {
    return pedidos.misPedidos();
  }

  @GetMapping("/disponibles")
  @PreAuthorize("hasAnyRole('ADMIN','REPARTIDOR')")
  public List<PedidoResponse> disponibles() {
    return pedidos.disponibles();
  }

  @GetMapping("/{id}")
  public PedidoResponse consultar(@PathVariable Long id) {
    return pedidos.consultar(id);
  }

  @PatchMapping("/{id}/estado")
  @PreAuthorize("hasAnyRole('ADMIN','REPARTIDOR')")
  public PedidoResponse estado(
      @PathVariable Long id, @Valid @RequestBody ActualizarEstadoRequest r) {
    return pedidos.actualizarEstado(id, r);
  }

  @PatchMapping("/{id}/cancelar")
  @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
  public PedidoResponse cancelar(@PathVariable Long id) {
    return pedidos.cancelarPedido(id);
  }

  @PatchMapping("/{id}/repartidor")
  @PreAuthorize("hasRole('ADMIN')")
  public PedidoResponse asignar(
      @PathVariable Long id, @Valid @RequestBody AsignarRepartidorRequest r) {
    return pedidos.asignar(id, r);
  }
}
