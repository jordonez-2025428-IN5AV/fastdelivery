# FastDelivery

API REST académica de pedidos y delivery. Proyecto Maven para **Java 21**, listo para abrir desde `pom.xml` en IntelliJ IDEA. Backend exclusivamente; PostgreSQL local, sin Docker.

## Tecnologías y estructura

Spring Boot 3.5.6, Spring Web, Spring Security, JWT (jjwt 0.12.6), Spring Data JPA/Hibernate, PostgreSQL, Lombok, Jakarta Validation, BCrypt, JUnit 5, MockMvc y Spring Security Test.

`controller → service → repository → PostgreSQL`. Entidades y DTOs separados. Seguridad en `config/` y `security/`; errores en `exception/`. Las respuestas no exponen passwords ni entidades JPA.

## Preparación e IntelliJ IDEA

Requisitos: JDK 21, PostgreSQL en ejecución, IntelliJ IDEA y Maven 3.6.3+ (puedes usar el integrado en IntelliJ).

1. Descomprime el ZIP y abre la carpeta `fastdelivery`, o elige **File → Open → pom.xml → Open as Project**.
2. En **File → Project Structure → Project SDK**, selecciona JDK 21. En **Settings → Build Tools → Maven**, configura también JDK 21 para importer y runner.
3. Espera la sincronización de dependencias Maven. Si es necesario, habilita annotation processing en **Settings → Build, Execution, Deployment → Compiler → Annotation Processors** para Lombok.
4. Crea la base en pgAdmin o psql, conectado a la base administrativa `postgres`:

```sql
CREATE DATABASE fastdelivery;
CREATE DATABASE fastdelivery_test;
```

5. Configura las variables de entorno en **Run → Edit Configurations → Environment variables** de `FastDeliveryApplication`.
6. Ejecuta `src/main/java/org/kinal/fastdelivery/FastDeliveryApplication.java` con el botón **Run**. La API escucha en `http://localhost:8080/api/v1`.

La raíz `/` no es una página web; usa los endpoints documentados. El perfil normal inicializa el esquema con Hibernate y ejecuta `data.sql` después. Los datos iniciales son idempotentes en ejecuciones secuenciales y no restauran el stock consumido al reiniciar.

### Variables

| Variable | Valor por defecto / significado |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/fastdelivery` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `postgres` |
| `JWT_SECRET` | Clave pública de desarrollo en application.properties; usa otra clave de al menos 32 bytes para tu entorno |
| `JWT_EXPIRATION` | `86400000` milisegundos (24 horas) |
| `PORT` | `8080` |
| `SHOW_SQL` | `false` |
| `SQL_INIT_MODE` | `always`; usa `never` para desactivar usuarios y productos de demostración |
| `TEST_DB_URL` | `jdbc:postgresql://localhost:5432/fastdelivery_test` |
| `TEST_DB_USERNAME` | `postgres` |
| `TEST_DB_PASSWORD` | `postgres` |

`.env.example` es una referencia; Spring Boot no lo lee automáticamente. Introduce sus valores en IntelliJ o en tu terminal. No se incluyen secretos de producción.

Para ejecutar desde PowerShell:

```powershell
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="tu_clave"
mvn spring-boot:run
```

Para terminal Bash:

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=tu_clave
mvn spring-boot:run
```

## Credenciales de desarrollo

Las tres cuentas usan **`123456`**. `data.sql` almacena únicamente el hash BCrypt.

| Email | Rol |
|---|---|
| admin@fastdelivery.com | ADMIN |
| repartidor@fastdelivery.com | REPARTIDOR |
| cliente@fastdelivery.com | CLIENTE |

ADMIN crea y actualiza comercios/productos, asigna repartidores, gestiona estados y cancela pedidos pendientes. REPARTIDOR consulta pedidos disponibles o propios y actualiza estados; no puede gestionar pedidos de otro repartidor. CLIENTE crea y consulta sus pedidos y cancela únicamente los propios pendientes. El registro público siempre crea CLIENTE y rechaza campos desconocidos, incluyendo `rol`.

## Autenticación

Registro (público):

```http
POST /api/v1/auth/register
Content-Type: application/json
```

```json
{"nombre":"Jeremy","direccion":"Guatemala","telefono":"55555555","email":"jeremy@example.com","password":"123456"}
```

Login (público):

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{"email":"cliente@fastdelivery.com","password":"123456"}
```

Respuesta:

```json
{"token":"JWT...","type":"Bearer","email":"cliente@fastdelivery.com","rol":"CLIENTE"}
```

Incluye `Authorization: Bearer <token>` en todas las demás peticiones. El JWT incluye subject/email, rol, emisión `iat` y expiración `exp`, y se verifica su firma y expiración. También se verifica el usuario y su rol actual contra la base. La API es stateless, sin sesiones ni CSRF. `javax.crypto.SecretKey` es una clase criptográfica de Java SE; todas las APIs web, JPA y validación usan Jakarta.

## Todos los endpoints

| Método | Ruta | Acceso | Resultado |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Público | Registro CLIENTE y JWT, 201 |
| POST | `/api/v1/auth/login` | Público | JWT, 200 |
| GET | `/api/v1/comercios?categoria=RESTAURANTE` | Autenticado | Comercios abiertos; categoría opcional, 200 |
| GET | `/api/v1/comercios/administracion` | ADMIN | Todos los comercios, incluidos cerrados, 200 |
| POST | `/api/v1/comercios` | ADMIN | Crear comercio, 201 |
| PUT | `/api/v1/comercios/{id}` | ADMIN | Actualizar comercio, 200 |
| GET | `/api/v1/comercios/{id}/productos` | Autenticado | Catálogo completo del comercio con disponibilidad y stock, 200 |
| POST | `/api/v1/comercios/{id}/productos` | ADMIN | Crear producto, 201 |
| PUT | `/api/v1/comercios/{id}/productos/{productoId}` | ADMIN | Actualizar producto/stock, 200 |
| POST | `/api/v1/pedidos` | CLIENTE | Crear pedido multiproducto, 201 |
| GET | `/api/v1/pedidos/mis-pedidos` | CLIENTE | Historial propio, 200 |
| GET | `/api/v1/pedidos/disponibles` | ADMIN, REPARTIDOR | Pedidos activos sin asignar o asignados al repartidor; ADMIN ve todos los activos, 200 |
| GET | `/api/v1/pedidos/{id}` | ADMIN, cliente dueño, repartidor asignado | Seguimiento, 200 |
| PATCH | `/api/v1/pedidos/{id}/estado` | ADMIN, REPARTIDOR | Siguiente estado válido, 200 |
| PATCH | `/api/v1/pedidos/{id}/cancelar` | ADMIN, cliente dueño | Cancelar PENDIENTE, 200 |
| PATCH | `/api/v1/pedidos/{id}/repartidor` | ADMIN | Asignar/reasignar antes de EN_CAMINO, 200 |

Los endpoints adicionales de actualización, seguimiento y asignación completan la gestión requerida. No se borran productos/comercios con historial: cambia `disponible` o `abierto` mediante PUT.

### Comercios y productos

POST/PUT comercio:

```json
{"nombre":"Fast Burger","categoria":"RESTAURANTE","direccion":"Zona 1, Guatemala","abierto":true}
```

Categorías: `RESTAURANTE`, `SUPERMERCADO`, `FARMACIA`.

POST/PUT producto:

```json
{"nombre":"Hamburguesa","precio":35.00,"stock":50,"disponible":true}
```

Precio positivo, hasta dos decimales; stock entero no negativo. PUT reemplaza los campos del recurso y requiere todos los campos del DTO.

### Pedidos

POST pedido (los IDs son ejemplos; consulta el catálogo para conocer los actuales):

```json
{"items":[{"productoId":1,"cantidad":2},{"productoId":2,"cantidad":1}]}
```

No admite `clienteId`, precio, subtotal, costoEnvio, montoTotal, rol ni estado inicial. El cliente proviene de SecurityContext; precios e inventario provienen de PostgreSQL. Se permiten productos de distintos comercios abiertos. IDs repetidos se agrupan en un detalle y se suma la cantidad.

Respuesta ilustrativa para precios Q30 y Q25:

```json
{
  "id":1,"clienteId":3,"clienteNombre":"Cliente",
  "direccionEntrega":"Zona 1, Guatemala","telefonoCliente":"55550003",
  "repartidorId":null,"fechaPedido":"2026-10-06T13:00:00",
  "costoEnvio":20.00,"montoTotal":105.00,"estado":"PENDIENTE",
  "detalles":[
    {"id":1,"productoId":1,"productoNombre":"Producto A","cantidad":2,"precioUnitario":30.00,"subtotal":60.00},
    {"id":2,"productoId":2,"productoNombre":"Producto B","cantidad":1,"precioUnitario":25.00,"subtotal":25.00}
  ]
}
```

Se almacena el precio unitario de compra. Todo el dinero usa BigDecimal, nunca double/float. El envío fijo es **Q20.00 por pedido**.

Estado:

```json
{"estado":"EN_PREPARACION"}
```

Flujo estricto: `PENDIENTE → EN_PREPARACION → EN_CAMINO → ENTREGADO`. Solo cancelar permite `PENDIENTE → CANCELADO`. ENTREGADO y CANCELADO son terminales. No se permite repetir estados ni saltarlos.

Al actualizar un pedido sin asignar, un REPARTIDOR queda asignado automáticamente. ADMIN puede preparar un pedido sin asignación; para pasar a EN_CAMINO debe asignar primero un repartidor mediante:

```json
{"repartidorId":2}
```

El ID debe corresponder a un usuario REPARTIDOR. La cancelación no lleva body. Solo PENDIENTE, incluyendo cuando el solicitante es ADMIN, y restaura stock una vez.

### Atomicidad y concurrencia

`crearPedido`, `cancelarPedido`, asignación y cambio de estado son transaccionales. Las excepciones de negocio extienden RuntimeException y provocan rollback. Un fallo en cualquier producto revierte el stock, pedido y detalles. Bloqueos pesimistas de productos en orden ascendente previenen sobreventa; bloqueo del pedido previene doble cancelación y cambios de estado simultáneos. Actualizar inventario administrativo utiliza el mismo bloqueo.

## Errores

Formato uniforme en MVC y Spring Security:

```json
{"timestamp":"2026-10-06T13:00:00","status":409,"error":"Conflict","message":"Stock insuficiente para Hamburguesa","path":"/api/v1/pedidos"}
```

400: validación, JSON/campos desconocidos, no disponible. 401: login/token inválido o ausente. 403: rol incorrecto o pedido ajeno. 404: recurso/ruta inexistente. 409: email duplicado, stock insuficiente, transición o cancelación inválida, conflicto de persistencia. 405: método no permitido. 500: error interno sin exponer detalles; se registra la causa en servidor.

## Pruebas automatizadas con PostgreSQL real

Crea `fastdelivery_test` y configura `TEST_DB_*` en la terminal o configuración Maven de IntelliJ. **Debe ser una base exclusiva y desechable**: el perfil test utiliza `create-drop` y limpia tablas entre casos. Nunca apuntes TEST_DB_URL a la base de la aplicación.

```powershell
$env:TEST_DB_PASSWORD="tu_clave"
mvn clean test
```

Pruebas con JUnit 5, Spring Boot Test y MockMvc: registro, BCrypt, email duplicado, JWT/login válido e inválido/expirado, autorización, comercios/productos, totales, envío, stock, rollback completo sin transacción envolvente del test, historial, ownership, cancelación/restauración, transiciones, asignación y concurrencia de compra y cancelación. No utiliza H2, mocks de persistencia ni Docker.

También puedes compilar el JAR con `mvn package`; luego `java -jar target/fastdelivery-1.0.0.jar`.

## Script curl

Con la API ejecutándose, Bash, curl y Python 3 (solo para interpretar JSON):

```bash
bash test-fastorder.sh
# Otra dirección:
BASE_URL=http://localhost:8081 bash test-fastorder.sh
```

En Windows úsalo desde Git Bash o WSL con `python3` disponible. Muestra `[PASS]`/`[FAIL]` y termina con código no cero ante un fallo. Crea un comercio/producto de prueba y dos pedidos por ejecución; sus datos quedan guardados para inspección. Prueba roles, consultas, compra/totales, entrega completa, operaciones inválidas y cancelación con restauración.

## Git: mantener main sin archivos

El ZIP no contiene `.git`. Si tu repositorio ya tiene main vacío y las ramas develop y jordonez-2025428, copia el contenido de esta carpeta dentro del repositorio, estando en tu rama personal:

```bash
git switch jordonez-2025428
# Copiar aquí pom.xml, src/, README.md y los demás archivos del proyecto.
git add .
git commit -m "Implementar API FastDelivery con Java 21 y JWT"
git push -u origin jordonez-2025428
```

No hagas este commit sobre main si debe permanecer sin archivos. No se incluyen archivos exclusivos de otro IDE.

## Referencias técnicas

- Spring Boot 3.5: https://docs.spring.io/spring-boot/3.5/system-requirements.html
- JJWT: https://github.com/jwtk/jjwt/tree/0.12.6
