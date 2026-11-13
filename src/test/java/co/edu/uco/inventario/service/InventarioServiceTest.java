package co.edu.uco.inventario.service;

import co.edu.uco.inventario.messages.MessageUcoLabClient;
import co.edu.uco.inventario.model.Producto;
import co.edu.uco.messageucolab.client.api.exception.MessageClientTransportException;
import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventarioService")
class InventarioServiceTest {

    private static final Instant CREATED = Instant.parse("2026-02-03T04:05:06Z");

    @Mock
    private MessageUcoLabClient messageClient;

    private InventarioService service;

    @BeforeEach
    void setUp() {
        service = new InventarioService(messageClient);
    }

    @Test
    @DisplayName("guardar almacena el producto y devuelve el detalle oficial con variables resueltas")
    void guardar_shouldStoreProductAndReturnMessageDetails() {
        Producto product = new Producto("Lápiz", "LAP-001", 4, 1);
        MessageDetails expected = details("Producto guardado: Lápiz", "Se guardó LAP-001");
        when(messageClient.getFreshDetails("MSG_PRODSAVED", Map.of("nombre", "Lápiz")))
                .thenReturn(Mono.just(expected));

        MessageDetails actual = service.guardar(product).block();
        long productCount = service.listar().count().block();

        assertAll(
                () -> assertEquals(expected, actual),
                () -> assertEquals(5, productCount),
                () -> assertEquals(product.getId(), service.buscar(product.getId()).block().getId()),
                () -> assertEquals("Producto guardado: Lápiz", actual.title()),
                () -> assertEquals("Se guardó LAP-001", actual.content()),
                () -> assertEquals("es", actual.sourceLanguage()),
                () -> assertEquals("SUCCESS", actual.type()),
                () -> assertEquals(CREATED, actual.createdAt())
        );
        verify(messageClient).getFreshDetails("MSG_PRODSAVED", Map.of("nombre", "Lápiz"));
    }

    @Test
    @DisplayName("guardar inválido consulta MSG_INVALID y no añade el producto")
    void guardar_shouldResolveInvalidMessageWithoutStoringProduct() {
        Producto invalid = new Producto(" ", "LAP-002", 1, 0);
        MessageDetails invalidMessage = details("Datos inválidos", "Revisa el producto");
        when(messageClient.getFreshDetails("MSG_INVALID", Map.of())).thenReturn(Mono.just(invalidMessage));

        MessageDetails result = service.guardar(invalid).block();
        long productCount = service.listar().count().block();

        assertAll(
                () -> assertEquals(invalidMessage, result),
                () -> assertEquals(4, productCount),
                () -> assertFalse(service.buscar(invalid.getId()).hasElement().block())
        );
        verify(messageClient).getFreshDetails("MSG_INVALID", Map.of());
    }

    @Test
    @DisplayName("guardar no oculta error de mensaje ni elimina el producto ya añadido")
    void guardar_shouldPropagateMessageFailureAndKeepStoredProduct() {
        Producto product = new Producto("Cuaderno", "CUA-003", 2, 1);
        MessageClientTransportException failure = new MessageClientTransportException(
                new IOException("message service unavailable"));
        when(messageClient.getFreshDetails("MSG_PRODSAVED", Map.of("nombre", "Cuaderno")))
                .thenReturn(Mono.error(failure));

        StepVerifier.create(service.guardar(product)).expectErrorSatisfies(actual -> assertSame(failure, actual))
                .verify();

        assertEquals(product.getId(), service.buscar(product.getId()).block().getId());
    }

    @Test
    @DisplayName("operaciones de actualización, borrado y stock conservan 404 para IDs inexistentes")
    void operations_shouldReturnNotFoundForUnknownProductIds() {
        ResponseStatusException updateError = assertThrows(ResponseStatusException.class,
                () -> service.actualizar("missing", new Producto("Nuevo", "SKU", 1, 0)).block());
        ResponseStatusException deleteError = assertThrows(ResponseStatusException.class,
                () -> service.eliminar("missing").block());
        ResponseStatusException outputError = assertThrows(ResponseStatusException.class,
                () -> service.salida("missing", 1).block());
        ResponseStatusException inputError = assertThrows(ResponseStatusException.class,
                () -> service.entrada("missing", 1).block());

        assertAll(
                () -> assertEquals(HttpStatus.NOT_FOUND, updateError.getStatusCode()),
                () -> assertEquals(HttpStatus.NOT_FOUND, deleteError.getStatusCode()),
                () -> assertEquals(HttpStatus.NOT_FOUND, outputError.getStatusCode()),
                () -> assertEquals(HttpStatus.NOT_FOUND, inputError.getStatusCode())
        );
    }

    private MessageDetails details(String title, String content) {
        return new MessageDetails("MSG_PRODSAVED", "es", "es", title, content, "SUCCESS", "inventory",
                "products", CREATED, CREATED.plusSeconds(60));
    }
}
