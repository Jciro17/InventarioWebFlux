package co.edu.uco.inventario.messages;

import co.edu.uco.messageucolab.client.api.MessageReader;
import co.edu.uco.messageucolab.client.api.exception.MessageClientResponseException;
import co.edu.uco.messageucolab.client.api.exception.MessageClientTransportException;
import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageUcoLabClient")
class MessageUcoLabClientTest {

    private static final String CODE = "MSG_TEST";

    @Mock
    private MessageReader messageReader;

    private MessageUcoLabClient client;

    @BeforeEach
    void setUp() {
        client = new MessageUcoLabClient(messageReader);
    }

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("consulta y reutiliza el resultado en caché aplicando variables por llamada")
        void get_shouldCacheUnformattedMessageAndApplyVariablesPerCall() {
            when(messageReader.getDetails(CODE)).thenReturn(Mono.just(details("Hola {name}", "Contenido {name}")));

            ResolvedMessage first = client.get(CODE, Map.of("name", "Ana")).block();
            ResolvedMessage second = client.get(CODE, Map.of("name", "Luis")).block();

            assertAll(
                    () -> assertEquals("Hola Ana", first.getTitle()),
                    () -> assertEquals("Contenido Ana", first.getContent()),
                    () -> assertEquals("Hola Luis", second.getTitle()),
                    () -> assertEquals("Contenido Luis", second.getContent()),
                    () -> assertEquals("api", second.getSource())
            );
            verify(messageReader).getDetails(CODE);
        }

        @Test
        @DisplayName("acepta variables nulas como una consulta sin sustituciones")
        void get_shouldReturnOriginalTextWhenVariablesAreNull() {
            when(messageReader.getDetails(CODE)).thenReturn(Mono.just(details("Hola {name}", "Texto")));

            ResolvedMessage result = client.get(CODE, null).block();

            assertEquals("Hola {name}", result.getTitle());
            assertEquals("Texto", result.getContent());
        }

        @Test
        @DisplayName("no usa respuesta de respaldo si el lector falla")
        void get_shouldPropagateReaderErrorWithoutFallback() {
            ResponseStatusException failure = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
            when(messageReader.getDetails(CODE)).thenReturn(Mono.error(failure));

            ResponseStatusException actual = assertThrows(ResponseStatusException.class,
                    () -> client.get(CODE).block());

            assertSame(failure, actual);
        }

        @Test
        @DisplayName("propaga errores HTTP del cliente sin respuesta de respaldo")
        void get_shouldPropagateHttpErrorWithoutFallback() {
            MessageClientResponseException failure = new MessageClientResponseException(503,
                    new IllegalStateException("upstream failure"));
            when(messageReader.getDetails(CODE)).thenReturn(Mono.error(failure));

            MessageClientResponseException actual = assertThrows(MessageClientResponseException.class,
                    () -> client.get(CODE).block());

            assertSame(failure, actual);
        }

        @Test
        @DisplayName("propaga errores de transporte del cliente sin respuesta de respaldo")
        void get_shouldPropagateTransportErrorWithoutFallback() {
            MessageClientTransportException failure = new MessageClientTransportException(
                    new java.io.IOException("transport failure"));
            when(messageReader.getDetails(CODE)).thenReturn(Mono.error(failure));

            MessageClientTransportException actual = assertThrows(MessageClientTransportException.class,
                    () -> client.get(CODE).block());

            assertSame(failure, actual);
        }

        @Test
        @DisplayName("convierte una respuesta vacía en error de gateway")
        void get_shouldFailWhenReaderCompletesEmpty() {
            when(messageReader.getDetails(CODE)).thenReturn(Mono.empty());

            ResponseStatusException actual = assertThrows(ResponseStatusException.class,
                    () -> client.get(CODE).block());

            assertEquals(HttpStatus.BAD_GATEWAY, actual.getStatusCode());
        }
    }

    @Nested
    @DisplayName("getFresh y clearCache")
    class FreshAndClear {

        @Test
        @DisplayName("getFresh no contamina la caché usada por get")
        void getFresh_shouldNotPopulateCache() {
            doReturn(Mono.just(details("Respuesta {name}", "Texto")),
                    Mono.just(details("Respuesta {name}", "Texto actualizado")))
                    .when(messageReader).getDetails(CODE);

            ResolvedMessage fresh = client.getFresh(CODE, Map.of("name", "Ana")).block();
            ResolvedMessage cachedPath = client.get(CODE).block();

            assertAll(
                    () -> assertEquals("Respuesta Ana", fresh.getTitle()),
                    () -> assertEquals("Texto actualizado", cachedPath.getContent())
            );
            verify(messageReader, times(2)).getDetails(CODE);
        }

        @Test
        @DisplayName("clearCache fuerza una nueva consulta")
        void clearCache_shouldCauseNextGetToFetchAgain() {
            doReturn(Mono.just(details("Primera", "Uno")), Mono.just(details("Segunda", "Dos")))
                    .when(messageReader).getDetails(CODE);

            ResolvedMessage first = client.get(CODE).block();
            client.clearCache();
            ResolvedMessage second = client.get(CODE).block();

            assertAll(
                    () -> assertEquals("Primera", first.getTitle()),
                    () -> assertEquals("Segunda", second.getTitle())
            );
            verify(messageReader, times(2)).getDetails(CODE);
        }
    }

    @Nested
    @DisplayName("getFreshDetails")
    class FreshDetails {

        @Test
        @DisplayName("sustituye variables y conserva todos los metadatos del detalle")
        void getFreshDetails_shouldFormatTextAndPreserveMetadata() {
            MessageDetails original = details("Guardado {nombre}", "Producto {nombre}");
            when(messageReader.getDetails(CODE)).thenReturn(Mono.just(original));

            MessageDetails result = client.getFreshDetails(CODE, Map.of("nombre", "Lápiz")).block();

            assertAll(
                    () -> assertNotSame(original, result),
                    () -> assertEquals(CODE, result.code()),
                    () -> assertEquals("es", result.sourceLanguage()),
                    () -> assertEquals("es", result.targetLanguage()),
                    () -> assertEquals("Guardado Lápiz", result.title()),
                    () -> assertEquals("Producto Lápiz", result.content()),
                    () -> assertEquals("INFO", result.type()),
                    () -> assertEquals("test", result.category()),
                    () -> assertEquals("test", result.functionality()),
                    () -> assertEquals(original.createdAt(), result.createdAt()),
                    () -> assertEquals(original.updatedAt(), result.updatedAt()),
                    () -> assertEquals("Guardado {nombre}", original.title()),
                    () -> assertEquals("Producto {nombre}", original.content())
            );
        }

        @Test
        @DisplayName("no utiliza ni modifica la caché local")
        void getFreshDetails_shouldNotReadOrPopulateResolvedMessageCache() {
            doReturn(Mono.just(details("Fresco {nombre}", "Detalle")),
                    Mono.just(details("Cacheable {nombre}", "Cache")))
                    .when(messageReader).getDetails(CODE);

            MessageDetails fresh = client.getFreshDetails(CODE, Map.of("nombre", "Ana")).block();
            ResolvedMessage cachedPath = client.get(CODE).block();

            assertAll(
                    () -> assertEquals("Fresco Ana", fresh.title()),
                    () -> assertEquals("Cacheable {nombre}", cachedPath.getTitle())
            );
            verify(messageReader, times(2)).getDetails(CODE);
        }

        @Test
        @DisplayName("devuelve el detalle original cuando las variables son nulas o vacías")
        void getFreshDetails_shouldReturnOriginalForNullOrEmptyVariables() {
            MessageDetails original = details("Título {nombre}", "Contenido {nombre}");
            doReturn(Mono.just(original), Mono.just(original)).when(messageReader).getDetails(CODE);

            MessageDetails nullVars = client.getFreshDetails(CODE, null).block();
            MessageDetails emptyVars = client.getFreshDetails(CODE, Map.of()).block();

            assertAll(
                    () -> assertSame(original, nullVars),
                    () -> assertSame(original, emptyVars),
                    () -> assertEquals("Título {nombre}", original.title())
            );
            verify(messageReader, times(2)).getDetails(CODE);
        }

        @Test
        @DisplayName("propaga errores HTTP y de transporte sin respuestas alternativas")
        void getFreshDetails_shouldPropagateHttpAndTransportErrors() {
            MessageClientResponseException httpFailure = new MessageClientResponseException(503,
                    new IllegalStateException("upstream failure"));
            MessageClientTransportException transportFailure = new MessageClientTransportException(
                    new java.io.IOException("transport failure"));
            doReturn(Mono.error(httpFailure), Mono.error(transportFailure))
                    .when(messageReader).getDetails(CODE);

            MessageClientResponseException httpActual = assertThrows(MessageClientResponseException.class,
                    () -> client.getFreshDetails(CODE, Map.of()).block());
            MessageClientTransportException transportActual = assertThrows(MessageClientTransportException.class,
                    () -> client.getFreshDetails(CODE, Map.of()).block());

            assertAll(
                    () -> assertSame(httpFailure, httpActual),
                    () -> assertSame(transportFailure, transportActual)
            );
        }

        @Test
        @DisplayName("traduce una respuesta vacía a error de gateway")
        void getFreshDetails_shouldReturnBadGatewayWhenReaderIsEmpty() {
            when(messageReader.getDetails(CODE)).thenReturn(Mono.empty());

            ResponseStatusException actual = assertThrows(ResponseStatusException.class,
                    () -> client.getFreshDetails(CODE, Map.of()).block());

            assertEquals(HttpStatus.BAD_GATEWAY, actual.getStatusCode());
        }
    }

    private MessageDetails details(String title, String content) {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        return new MessageDetails(CODE, "es", "es", title, content, "INFO", "test", "test",
                timestamp, timestamp);
    }
}
