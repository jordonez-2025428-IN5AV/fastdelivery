package org.kinal.fastdelivery.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.kinal.fastdelivery.dto.producto.*;
import org.kinal.fastdelivery.entity.Producto;
import org.kinal.fastdelivery.exception.ResourceNotFoundException;
import org.kinal.fastdelivery.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductoService {
  private final ProductoRepository productos;
  private final ComercioRepository comercios;

  @Transactional(readOnly = true)
  public List<ProductoResponse> listar(Long comercioId) {
    if (!comercios.existsById(comercioId))
      throw new ResourceNotFoundException("Comercio no encontrado");
    return productos.findByComercioIdOrderByIdAsc(comercioId).stream()
        .map(DtoMapper::producto)
        .toList();
  }

  @Transactional
  public ProductoResponse crear(Long comercioId, ProductoRequest r) {
    var c =
        comercios
            .findById(comercioId)
            .orElseThrow(() -> new ResourceNotFoundException("Comercio no encontrado"));
    var p = new Producto();
    p.setComercio(c);
    aplicar(p, r);
    return DtoMapper.producto(productos.save(p));
  }

  @Transactional
  public ProductoResponse actualizar(Long comercioId, Long id, ProductoRequest r) {
    var p =
        productos
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    if (!p.getComercio().getId().equals(comercioId))
      throw new ResourceNotFoundException("Producto no pertenece al comercio");
    aplicar(p, r);
    return DtoMapper.producto(p);
  }

  private void aplicar(Producto p, ProductoRequest r) {
    p.setNombre(r.nombre().trim());
    p.setPrecio(r.precio().setScale(2));
    p.setStock(r.stock());
    p.setDisponible(r.disponible());
  }
}
