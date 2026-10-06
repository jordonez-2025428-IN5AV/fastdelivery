package org.kinal.fastdelivery.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.kinal.fastdelivery.enums.*;

@Entity
@Table(name = "detalles_pedido")
@Getter
@Setter
@NoArgsConstructor
public class DetallePedido {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pedido_id", nullable = false)
  private Pedido pedido;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "producto_id", nullable = false)
  private Producto producto;

  @Column(nullable = false)
  private Integer cantidad;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal precioUnitario;

  @Column(nullable = false, precision = 16, scale = 2)
  private BigDecimal subtotal;
}
