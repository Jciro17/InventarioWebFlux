package co.edu.uco.inventario.controller;

import co.edu.uco.inventario.messages.MessageUcoLabClient;
import co.edu.uco.inventario.model.Producto;
import co.edu.uco.inventario.service.InventarioService;
import co.edu.uco.messageucolab.client.api.exception.MessageClientResponseException;
import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventarioController")
class InventarioControllerTest {

    private static final Instant CREATED = Instant.parse("2026-02-03T04:05:06Z");

    @Mock
    private MessageUcoLabClient messageClient;

    private InventarioService service;
    private WebTestClient webClient;

    @BeforeEach
    void setUp() {
        service = new InventarioService(messageClient);
        InventarioController controller = new InventarioController(service, messageClient);
        webClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new MessageClientExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST productos devuelve el detalle SDK con texto formateado y metadatos")
    void crear_shouldReturnFormattedMessageDetailsFromSdk() {
        Producto product = new Producto("Regla", "REG-100", 3, 1);
        MessageDetails expected = details("Se guardó Regla", "Código REG-100");
        when(messageClient.getFreshDetails("MSG_PRODSAVED", Map.of("nombre", "Regla")))
                .thenReturn(Mono.just(expected));

        webClient.post().uri("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(product)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MessageDetails.class)
                .value(actual -> assertAll(
                        () -> assertEquals("MSG_PRODSAVED", actual.code()),
                        () -> assertEquals("es", actual.sourceLanguage()),
                        () -> assertEquals("Se guardó Regla", actual.title()),
                        () -> assertEquals("Código REG-100", actual.content()),
                        () -> assertEquals("SUCCESS", actual.type()),
                        () -> assertEquals("inventory", actual.category()),
                        () -> assertEquals(CREATED, actual.createdAt())
                ));

        verify(messageClient).getFreshDetails("MSG_PRODSAVED", Map.of("nombre", "Regla"));
    }

    @Test
    @DisplayName("POST productos propaga el error del catálogo y mantiene el producto almacenado")
    void crear_shouldPropagateMessageHttpErrorAfterStoringProduct() {
        Producto product = new Producto("Borrador", "BOR-101", 2, 0);
        when(messageClient.getFreshDetails("MSG_PRODSAVED", Map.of("nombre", "Borrador")))
                .thenReturn(Mono.error(new MessageClientResponseException(503,
                        new IllegalStateException("upstream unavailable"))));

        webClient.post().uri("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(product)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        assertEquals(product.getId(), service.buscar(product.getId()).block().getId());
    }

    private MessageDetails details(String title, String content) {
        return new MessageDetails("MSG_PRODSAVED", "es", "es", title, content, "SUCCESS", "inventory",
                "products", CREATED, CREATED.plusSeconds(60));
    }
}
