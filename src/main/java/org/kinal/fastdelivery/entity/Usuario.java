package org.kinal.fastdelivery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.kinal.fastdelivery.enums.*;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String nombre;

  @Column(nullable = false, length = 255)
  private String direccion;

  @Column(nullable = false, length = 30)
  private String telefono;

  @Column(nullable = false, unique = true, length = 160)
  private String email;

  @Column(nullable = false, length = 100)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Rol rol;
}
