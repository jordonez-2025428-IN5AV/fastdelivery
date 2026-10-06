package org.kinal.fastdelivery.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.kinal.fastdelivery.dto.comercio.*;
import org.kinal.fastdelivery.entity.Comercio;
import org.kinal.fastdelivery.enums.CategoriaComercio;
import org.kinal.fastdelivery.exception.ResourceNotFoundException;
import org.kinal.fastdelivery.repository.ComercioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ComercioService {
  private final ComercioRepository comercios;

  @Transactional(readOnly = true)
  public List<ComercioResponse> listar(CategoriaComercio categoria) {
    return (categoria == null
            ? comercios.findByAbiertoTrueOrderByIdAsc()
            : comercios.findByAbiertoTrueAndCategoriaOrderByIdAsc(categoria))
        .stream().map(DtoMapper::comercio).toList();
  }

  @Transactional(readOnly = true)
  public List<ComercioResponse> todos() {
    return comercios.findAll().stream().map(DtoMapper::comercio).toList();
  }

  @Transactional
  public ComercioResponse crear(ComercioRequest r) {
    var c = new Comercio();
    aplicar(c, r);
    return DtoMapper.comercio(comercios.save(c));
  }

  @Transactional
  public ComercioResponse actualizar(Long id, ComercioRequest r) {
    var c =
        comercios
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Comercio no encontrado"));
    aplicar(c, r);
    return DtoMapper.comercio(c);
  }

  private void aplicar(Comercio c, ComercioRequest r) {
    c.setNombre(r.nombre().trim());
    c.setCategoria(r.categoria());
    c.setDireccion(r.direccion().trim());
    c.setAbierto(r.abierto());
  }
}
