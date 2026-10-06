package org.kinal.fastdelivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kinal.fastdelivery.entity.*;
import org.kinal.fastdelivery.enums.*;
import org.kinal.fastdelivery.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FastDeliveryIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired PasswordEncoder encoder;
  @Autowired UsuarioRepository usuarios;
  @Autowired ComercioRepository comercios;
  @Autowired ProductoRepository productos;
  @Autowired PedidoRepository pedidos;
  @Autowired DetallePedidoRepository detalles;
  private String admin, cliente, repartidor, otro, otroRepartidor;
  private Long comercioId, productoId, segundoId, otroRepartidorId;

  // Sin @Transactional en los tests: los Services deben confirmar o revertir sus propias
  // transacciones.
  @BeforeEach
  void setup() throws Exception {
    detalles.deleteAllInBatch();
    pedidos.deleteAllInBatch();
    productos.deleteAllInBatch();
    comercios.deleteAllInBatch();
    usuarios.deleteAllInBatch();
    usuario("admin@test.com", Rol.ADMIN);
    usuario("cliente@test.com", Rol.CLIENTE);
    usuario("repartidor@test.com", Rol.REPARTIDOR);
    usuario("otro@test.com", Rol.CLIENTE);
    otroRepartidorId = usuario("repartidor2@test.com", Rol.REPARTIDOR).getId();
    var c = new Comercio();
    c.setNombre("Comercio test");
    c.setDireccion("Guatemala");
    c.setCategoria(CategoriaComercio.RESTAURANTE);
    c.setAbierto(true);
    comercioId = comercios.save(c).getId();
    productoId = producto(c, "Hamburguesa", "30.00", 10).getId();
    segundoId = producto(c, "Bebida", "25.00", 5).getId();
    admin = login("admin@test.com");
    cliente = login("cliente@test.com");
    repartidor = login("repartidor@test.com");
    otro = login("otro@test.com");
    otroRepartidor = login("repartidor2@test.com");
  }

  private Usuario usuario(String email, Rol rol) {
    var u = new Usuario();
    u.setNombre("Test");
    u.setDireccion("Zona 1");
    u.setTelefono("55555555");
    u.setEmail(email);
    u.setPassword(encoder.encode("123456"));
    u.setRol(rol);
    return usuarios.save(u);
  }

  private Producto producto(Comercio c, String nombre, String precio, int stock) {
    var p = new Producto();
    p.setComercio(c);
    p.setNombre(nombre);
    p.setPrecio(new BigDecimal(precio));
    p.setStock(stock);
    p.setDisponible(true);
    return productos.save(p);
  }

  private String login(String email) throws Exception {
    return node(mvc.perform(
                post("/api/v1/auth/login")
                    .contentType("application/json")
                    .content(json.writeValueAsString(Map.of("email", email, "password", "123456"))))
            .andExpect(status().isOk())
            .andReturn())
        .get("token")
        .asText();
  }

  private JsonNode node(MvcResult result) throws Exception {
    return json.readTree(result.getResponse().getContentAsString());
  }

  private String items(int cantidad) {
    return "{\"items\":[{\"productoId\":" + productoId + ",\"cantidad\":" + cantidad + "}]}";
  }

  private JsonNode crear() throws Exception {
    return node(
        mvc.perform(
                post("/api/v1/pedidos")
                    .header("Authorization", "Bearer " + cliente)
                    .contentType("application/json")
                    .content(items(2)))
            .andExpect(status().isCreated())
            .andReturn());
  }

  private ResultActions estado(long id, String nuevo, String token) throws Exception {
    return mvc.perform(
        patch("/api/v1/pedidos/" + id + "/estado")
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .content("{\"estado\":\"" + nuevo + "\"}"));
  }

  private ResultActions cancelar(long id, String token) throws Exception {
    return mvc.perform(
        patch("/api/v1/pedidos/" + id + "/cancelar").header("Authorization", "Bearer " + token));
  }

  private String comercioRequest() {
    return "{\"nombre\":\"Nuevo\",\"categoria\":\"FARMACIA\",\"direccion\":\"Zona"
               + " 2\",\"abierto\":true}";
  }

  @Test
  void registroCodificaPasswordYAsignaCliente() throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType("application/json")
                .content(
                    "{\"nombre\":\"Jeremy\",\"direccion\":\"Guatemala\",\"telefono\":\"55555555\",\"email\":\"nuevo@test.com\",\"password\":\"123456\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.rol").value("CLIENTE"))
        .andExpect(jsonPath("$.token").isNotEmpty());
    var u = usuarios.findByEmail("nuevo@test.com").orElseThrow();
    assertThat(u.getPassword()).isNotEqualTo("123456");
    assertThat(encoder.matches("123456", u.getPassword())).isTrue();
  }

  @Test
  void registroNoAceptaRol() throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType("application/json")
                .content(
                    "{\"nombre\":\"Jeremy\",\"direccion\":\"Guatemala\",\"telefono\":\"55555555\",\"email\":\"nuevo@test.com\",\"password\":\"123456\",\"rol\":\"ADMIN\"}"))
        .andExpect(status().isBadRequest());
    assertThat(usuarios.existsByEmail("nuevo@test.com")).isFalse();
  }

  @Test
  void emailDuplicadoNormalizado() throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType("application/json")
                .content(
                    "{\"nombre\":\"Test\",\"direccion\":\"GT\",\"telefono\":\"5555\",\"email\":\"CLIENTE@test.com\",\"password\":\"123456\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void loginCorrecto() {
    assertThat(cliente).hasSizeGreaterThan(50);
  }

  @Test
  void loginIncorrecto() throws Exception {
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"cliente@test.com\",\"password\":\"incorrecta\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void endpointSinJwt() throws Exception {
    mvc.perform(get("/api/v1/comercios")).andExpect(status().isUnauthorized());
  }

  @Test
  void jwtMalformado() throws Exception {
    mvc.perform(get("/api/v1/comercios").header("Authorization", "Bearer invalido"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void jwtExpirado() throws Exception {
    var key =
        Keys.hmacShaKeyFor(
            "fastdelivery-pruebas-clave-fija-de-32-bytes-minimo".getBytes(StandardCharsets.UTF_8));
    String token =
        Jwts.builder()
            .subject("cliente@test.com")
            .claim("email", "cliente@test.com")
            .claim("rol", "CLIENTE")
            .issuedAt(Date.from(Instant.now().minusSeconds(60)))
            .expiration(Date.from(Instant.now().minusSeconds(30)))
            .signWith(key)
            .compact();
    mvc.perform(get("/api/v1/comercios").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void clienteNoCreaComercio() throws Exception {
    mvc.perform(
            post("/api/v1/comercios")
                .header("Authorization", "Bearer " + cliente)
                .contentType("application/json")
                .content(comercioRequest()))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCreaComercio() throws Exception {
    mvc.perform(
            post("/api/v1/comercios")
                .header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content(comercioRequest()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.categoria").value("FARMACIA"));
  }

  @Test
  void adminCreaProducto() throws Exception {
    mvc.perform(
            post("/api/v1/comercios/" + comercioId + "/productos")
                .header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content(
                    "{\"nombre\":\"Nuevo\",\"precio\":12.50,\"stock\":20,\"disponible\":true}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.precio").value(12.5));
  }

  @Test
  void comercioInexistente() throws Exception {
    mvc.perform(
            get("/api/v1/comercios/99999999/productos")
                .header("Authorization", "Bearer " + cliente))
        .andExpect(status().isNotFound());
  }

  @Test
  void filtroCategoriaYCierre() throws Exception {
    mvc.perform(
            get("/api/v1/comercios?categoria=FARMACIA")
                .header("Authorization", "Bearer " + cliente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    var c = comercios.findById(comercioId).orElseThrow();
    c.setAbierto(false);
    comercios.save(c);
    mvc.perform(get("/api/v1/comercios").header("Authorization", "Bearer " + cliente))
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(
            post("/api/v1/pedidos")
                .header("Authorization", "Bearer " + cliente)
                .contentType("application/json")
                .content(items(1)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void creaMultiproductoCalculaSubtotalesEnvioTotalYStock() throws Exception {
    String body =
        "{\"items\":[{\"productoId\":"
            + productoId
            + ",\"cantidad\":2},{\"productoId\":"
            + segundoId
            + ",\"cantidad\":1}]}";
    var n =
        node(
            mvc.perform(
                    post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + cliente)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn());
    assertThat(n.get("costoEnvio").decimalValue()).isEqualByComparingTo("20.00");
    assertThat(n.get("montoTotal").decimalValue()).isEqualByComparingTo("105.00");
    assertThat(n.get("detalles").get(0).get("subtotal").decimalValue())
        .isEqualByComparingTo("60.00");
    assertThat(n.get("detalles").get(1).get("subtotal").decimalValue())
        .isEqualByComparingTo("25.00");
    assertThat(productos.findById(productoId).orElseThrow().getStock()).isEqualTo(8);
    assertThat(productos.findById(segundoId).orElseThrow().getStock()).isEqualTo(4);
  }

  @Test
  void precioHistoricoNoCambia() throws Exception {
    long id = crear().get("id").asLong();
    var p = productos.findById(productoId).orElseThrow();
    p.setPrecio(new BigDecimal("99.00"));
    productos.save(p);
    mvc.perform(get("/api/v1/pedidos/" + id).header("Authorization", "Bearer " + cliente))
        .andExpect(jsonPath("$.detalles[0].precioUnitario").value(30.0));
  }

  @Test
  void productoSinStock() throws Exception {
    mvc.perform(
            post("/api/v1/pedidos")
                .header("Authorization", "Bearer " + cliente)
                .contentType("application/json")
                .content(items(11)))
        .andExpect(status().isConflict());
    assertThat(pedidos.count()).isZero();
  }

  @Test
  void rollbackCompletoCuandoSegundoProductoFalla() throws Exception {
    var p = productos.findById(segundoId).orElseThrow();
    p.setStock(0);
    productos.save(p);
    String body =
        "{\"items\":[{\"productoId\":"
            + productoId
            + ",\"cantidad\":2},{\"productoId\":"
            + segundoId
            + ",\"cantidad\":1}]}";
    mvc.perform(
            post("/api/v1/pedidos")
                .header("Authorization", "Bearer " + cliente)
                .contentType("application/json")
                .content(body))
        .andExpect(status().isConflict());
    assertThat(productos.findById(productoId).orElseThrow().getStock()).isEqualTo(10);
    assertThat(pedidos.count()).isZero();
    assertThat(detalles.count()).isZero();
  }

  @Test
  void productoInexistenteRevierteStock() throws Exception {
    mvc.perform(
            post("/api/v1/pedidos")
                .header("Authorization", "Bearer " + cliente)
                .contentType("application/json")
                .content(
                    "{\"items\":[{\"productoId\":"
                        + productoId
                        + ",\"cantidad\":2},{\"productoId\":99999999,\"cantidad\":1}]}"))
        .andExpect(status().isNotFound());
    assertThat(productos.findById(productoId).orElseThrow().getStock()).isEqualTo(10);
  }

  @Test
  void duplicadosAgrupanCantidad() throws Exception {
    String body =
        "{\"items\":[{\"productoId\":"
            + productoId
            + ",\"cantidad\":2},{\"productoId\":"
            + productoId
            + ",\"cantidad\":3}]}";
    mvc.perform(
            post("/api/v1/pedidos")
                .header("Authorization", "Bearer " + cliente)
                .contentType("application/json")
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.detalles.length()").value(1))
        .andExpect(jsonPath("$.detalles[0].cantidad").value(5));
    assertThat(productos.findById(productoId).orElseThrow().getStock()).isEqualTo(5);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void cantidadesInvalidas(int cantidad) throws Exception {
    mvc.perform(
            post("/api/v1/pedidos")
                .header("Authorization", "Bearer " + cliente)
                .contentType("application/json")
                .content(items(cantidad)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void itemsVacio() throws Exception {
    mvc.perform(
            post("/api/v1/pedidos")
                .header("Authorization", "Bearer " + cliente)
                .contentType("application/json")
                .content("{\"items\":[]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void clienteNoImponePrecio() throws Exception {
    mvc.perform(
            post("/api/v1/pedidos")
                .header("Authorization", "Bearer " + cliente)
                .contentType("application/json")
                .content(
                    "{\"items\":[{\"productoId\":"
                        + productoId
                        + ",\"cantidad\":1,\"precio\":0.01}]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void misPedidosSoloPropios() throws Exception {
    crear();
    mvc.perform(get("/api/v1/pedidos/mis-pedidos").header("Authorization", "Bearer " + cliente))
        .andExpect(jsonPath("$.length()").value(1));
    mvc.perform(get("/api/v1/pedidos/mis-pedidos").header("Authorization", "Bearer " + otro))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void noConsultaPedidoAjeno() throws Exception {
    long id = crear().get("id").asLong();
    mvc.perform(get("/api/v1/pedidos/" + id).header("Authorization", "Bearer " + otro))
        .andExpect(status().isForbidden());
  }

  @Test
  void cancelarPendienteRestauraStockYNoPermiteDobleCancelacion() throws Exception {
    long id = crear().get("id").asLong();
    cancelar(id, cliente)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("CANCELADO"));
    assertThat(productos.findById(productoId).orElseThrow().getStock()).isEqualTo(10);
    cancelar(id, cliente).andExpect(status().isConflict());
    assertThat(productos.findById(productoId).orElseThrow().getStock()).isEqualTo(10);
  }

  @Test
  void clienteNoCancelaAjeno() throws Exception {
    long id = crear().get("id").asLong();
    cancelar(id, otro).andExpect(status().isForbidden());
    assertThat(productos.findById(productoId).orElseThrow().getStock()).isEqualTo(8);
  }

  @Test
  void adminPuedeCancelarPendiente() throws Exception {
    cancelar(crear().get("id").asLong(), admin).andExpect(status().isOk());
  }

  @Test
  void transicionesValidasYNoCancelaPreparacionCaminoEntregado() throws Exception {
    long id = crear().get("id").asLong();
    estado(id, "EN_PREPARACION", repartidor).andExpect(status().isOk());
    cancelar(id, cliente).andExpect(status().isConflict());
    estado(id, "EN_CAMINO", repartidor).andExpect(status().isOk());
    cancelar(id, admin).andExpect(status().isConflict());
    estado(id, "ENTREGADO", repartidor).andExpect(status().isOk());
    cancelar(id, cliente).andExpect(status().isConflict());
    estado(id, "EN_CAMINO", repartidor).andExpect(status().isConflict());
  }

  @ParameterizedTest
  @ValueSource(strings = {"ENTREGADO", "EN_CAMINO", "CANCELADO", "PENDIENTE"})
  void transicionInvalida(String nuevo) throws Exception {
    long id = crear().get("id").asLong();
    estado(id, nuevo, admin).andExpect(status().isConflict());
    assertThat(pedidos.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
  }

  @Test
  void canceladoEsTerminal() throws Exception {
    long id = crear().get("id").asLong();
    cancelar(id, cliente).andExpect(status().isOk());
    estado(id, "EN_PREPARACION", admin).andExpect(status().isConflict());
  }

  @Test
  void preparacionNoSaltaAEntregado() throws Exception {
    long id = crear().get("id").asLong();
    estado(id, "EN_PREPARACION", admin).andExpect(status().isOk());
    estado(id, "ENTREGADO", admin).andExpect(status().isConflict());
  }

  @Test
  void repartidorNoTomaPedidoAjenoYDisponiblesFiltra() throws Exception {
    long id = crear().get("id").asLong();
    estado(id, "EN_PREPARACION", repartidor).andExpect(status().isOk());
    estado(id, "EN_CAMINO", otroRepartidor).andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/pedidos/disponibles").header("Authorization", "Bearer " + otroRepartidor))
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/v1/pedidos/disponibles").header("Authorization", "Bearer " + repartidor))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void adminAsignaYCompletaFlujo() throws Exception {
    long id = crear().get("id").asLong();
    mvc.perform(
            patch("/api/v1/pedidos/" + id + "/repartidor")
                .header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content("{\"repartidorId\":" + otroRepartidorId + "}"))
        .andExpect(status().isOk());
    estado(id, "EN_PREPARACION", admin).andExpect(status().isOk());
    estado(id, "EN_CAMINO", admin).andExpect(status().isOk());
    estado(id, "ENTREGADO", admin).andExpect(status().isOk());
    mvc.perform(get("/api/v1/pedidos/disponibles").header("Authorization", "Bearer " + admin))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void permisosPedido() throws Exception {
    long id = crear().get("id").asLong();
    estado(id, "EN_PREPARACION", cliente).andExpect(status().isForbidden());
    cancelar(id, repartidor).andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/pedidos/disponibles").header("Authorization", "Bearer " + cliente))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/pedidos")
                .header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content(items(1)))
        .andExpect(status().isForbidden());
  }

  @Test
  void concurrenciaNoSobrevende() throws Exception {
    var p = productos.findById(productoId).orElseThrow();
    p.setStock(1);
    productos.save(p);
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Integer> call =
          () -> {
            start.await();
            return mvc.perform(
                    post("/api/v1/pedidos")
                        .header("Authorization", "Bearer " + cliente)
                        .contentType("application/json")
                        .content(items(1)))
                .andReturn()
                .getResponse()
                .getStatus();
          };
      var a = pool.submit(call);
      var b = pool.submit(call);
      start.countDown();
      assertThat(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(201, 409);
    }
    assertThat(productos.findById(productoId).orElseThrow().getStock()).isZero();
    assertThat(pedidos.count()).isEqualTo(1);
  }

  @Test
  void concurrenciaCancelacionRestauraUnaVez() throws Exception {
    long id = crear().get("id").asLong();
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Integer> call =
          () -> {
            start.await();
            return cancelar(id, cliente).andReturn().getResponse().getStatus();
          };
      var a = pool.submit(call);
      var b = pool.submit(call);
      start.countDown();
      assertThat(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(200, 409);
    }
    assertThat(productos.findById(productoId).orElseThrow().getStock()).isEqualTo(10);
  }
}
