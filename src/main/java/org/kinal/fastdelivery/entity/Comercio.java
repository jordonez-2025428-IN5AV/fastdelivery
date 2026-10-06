package org.kinal.fastdelivery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.kinal.fastdelivery.enums.*;

@Entity
@Table(name = "comercios")
@Getter
@Setter
@NoArgsConstructor
public class Comercio {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String nombre;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private CategoriaComercio categoria;

  @Column(nullable = false, length = 255)
  private String direccion;

  @Column(nullable = false)
  private boolean abierto;
}
