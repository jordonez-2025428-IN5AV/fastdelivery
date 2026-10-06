# Validación de la entrega

Verificado el 6 de octubre de 2026 con OpenJDK 21.0.12, Maven 3.9.9 y PostgreSQL 16.15.

- `mvn clean test`: **BUILD SUCCESS**, 40 pruebas, 0 fallos, 0 errores, 0 omitidas.
- `mvn -DskipTests package`: **BUILD SUCCESS**, JAR ejecutable generado.
- Inicio del JAR y login real de ADMIN, REPARTIDOR y CLIENTE: correcto.
- `test-fastorder.sh` contra la API real: todas las comprobaciones PASS.
- Reinicio con la misma base: conserva exactamente 3 usuarios iniciales, 1 Fast Burger y 3 productos iniciales, sin duplicados.
- Java 21 configurado en Maven; Spring Boot 3.5.6 y jjwt 0.12.6.
- DTOs separados; cinco entidades principales y sus repositorios.
- Bloqueos pesimistas, rollback de pedidos multiproducto, restauración de inventario y pruebas de concurrencia verificadas.
- No hay frontend, Docker, H2, pseudocódigo ni tareas pendientes en el código.

Las pruebas usan una base PostgreSQL desechable separada. La entrega incluye fuentes y configuración; las dependencias se descargan con Maven. Para ejecutarla en tu PC debes disponer de JDK 21 y PostgreSQL y configurar las credenciales según README.md.
