package org.kinal.fastdelivery.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.kinal.fastdelivery.dto.comercio.*;
import org.kinal.fastdelivery.dto.producto.*;
import org.kinal.fastdelivery.enums.CategoriaComercio;
import org.kinal.fastdelivery.service.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comercios")
@RequiredArgsConstructor
public class ComercioController {
  private final ComercioService comercios;
  private final ProductoService productos;

  @GetMapping
  public List<ComercioResponse> listar(
      @RequestParam(required = false) CategoriaComercio categoria) {
    return comercios.listar(categoria);
  }

  @GetMapping("/administracion")
  @PreAuthorize("hasRole('ADMIN')")
  public List<ComercioResponse> todos() {
    return comercios.todos();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ComercioResponse> crear(@Valid @RequestBody ComercioRequest r) {
    return ResponseEntity.status(HttpStatus.CREATED).body(comercios.crear(r));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ComercioResponse actualizar(@PathVariable Long id, @Valid @RequestBody ComercioRequest r) {
    return comercios.actualizar(id, r);
  }

  @GetMapping("/{id}/productos")
  public List<ProductoResponse> productos(@PathVariable Long id) {
    return productos.listar(id);
  }

  @PostMapping("/{id}/productos")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProductoResponse> crearProducto(
      @PathVariable Long id, @Valid @RequestBody ProductoRequest r) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productos.crear(id, r));
  }

  @PutMapping("/{id}/productos/{productoId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ProductoResponse actualizarProducto(
      @PathVariable Long id, @PathVariable Long productoId, @Valid @RequestBody ProductoRequest r) {
    return productos.actualizar(id, productoId, r);
  }
}
