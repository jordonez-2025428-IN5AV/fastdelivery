package org.kinal.fastdelivery.repository;

import java.util.List;
import org.kinal.fastdelivery.entity.Comercio;
import org.kinal.fastdelivery.enums.*;
import org.springframework.data.jpa.repository.*;

public interface ComercioRepository extends JpaRepository<Comercio, Long> {
  List<Comercio> findByAbiertoTrueOrderByIdAsc();

  List<Comercio> findByAbiertoTrueAndCategoriaOrderByIdAsc(CategoriaComercio categoria);
}
