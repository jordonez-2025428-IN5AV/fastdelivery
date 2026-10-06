package org.kinal.fastdelivery.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.kinal.fastdelivery.enums.*;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
public class Producto {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "comercio_id", nullable = false)
  private Comercio comercio;

  @Column(nullable = false, length = 120)
  private String nombre;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal precio;

  @Column(nullable = false)
  private Integer stock;

  @Column(nullable = false)
  private boolean disponible;
}
