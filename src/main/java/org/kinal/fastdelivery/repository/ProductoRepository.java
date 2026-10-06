package org.kinal.fastdelivery.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.kinal.fastdelivery.entity.Producto;
import org.kinal.fastdelivery.enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
  List<Producto> findByComercioIdOrderByIdAsc(Long comercioId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Producto p where p.id = :id")
  Optional<Producto> findByIdForUpdate(@Param("id") Long id);
}
