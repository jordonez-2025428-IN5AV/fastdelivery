package org.kinal.fastdelivery.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.kinal.fastdelivery.entity.Pedido;
import org.kinal.fastdelivery.enums.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
  List<Pedido> findByClienteIdOrderByFechaPedidoDesc(Long clienteId);

  List<Pedido> findByEstado(EstadoPedido estado);

  @Query(
      "select p from Pedido p where p.estado in :estados and (p.repartidor is null or :admin = true"
          + " or p.repartidor.id = :repartidorId) order by p.fechaPedido")
  List<Pedido> findDisponibles(
      @Param("estados") List<EstadoPedido> estados,
      @Param("admin") boolean admin,
      @Param("repartidorId") Long repartidorId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Pedido p where p.id = :id")
  Optional<Pedido> findByIdForUpdate(@Param("id") Long id);
}
