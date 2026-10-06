package org.kinal.fastdelivery.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.kinal.fastdelivery.enums.*;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cliente_id", nullable = false)
  private Usuario cliente;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "repartidor_id")
  private Usuario repartidor;

  @Column(nullable = false)
  private LocalDateTime fechaPedido;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal costoEnvio;

  @Column(nullable = false, precision = 16, scale = 2)
  private BigDecimal montoTotal;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private EstadoPedido estado;

  @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<DetallePedido> detalles = new ArrayList<>();
}
