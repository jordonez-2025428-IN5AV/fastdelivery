package org.kinal.fastdelivery.repository;

import java.util.Optional;
import org.kinal.fastdelivery.entity.Usuario;
import org.kinal.fastdelivery.enums.*;
import org.springframework.data.jpa.repository.*;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
  Optional<Usuario> findByEmail(String email);

  boolean existsByEmail(String email);
}
