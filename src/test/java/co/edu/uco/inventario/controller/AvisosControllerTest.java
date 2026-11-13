package co.edu.uco.inventario.controller;

import co.edu.uco.inventario.adaptador.AvisosEnTiempoReal;
import co.edu.uco.inventario.model.Aviso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("AvisosController")
class AvisosControllerTest {

    @Test
    @DisplayName("abre el canal con comentario y convierte avisos al evento SSE esperado")
    void avisos_shouldStartWithCommentAndPublishAvisoEvent() {
        AvisosEnTiempoReal stream = new AvisosEnTiempoReal();
        AvisosController controller = new AvisosController(stream);
        Aviso aviso = new Aviso("Mensaje actualizado", "M-3: Título");

        StepVerifier.create(controller.avisos())
                .assertNext(event -> assertEquals("conectado", event.comment()))
                .then(() -> stream.publicar(aviso))
                .assertNext(event -> assertAllEvent(event, "aviso", aviso))
                .thenCancel()
                .verify();
    }

    @Test
    @DisplayName("emite un evento SSE de latido cada diez segundos")
    void avisos_shouldEmitHeartbeatAfterTenSeconds() {
        AvisosController controller = new AvisosController(new AvisosEnTiempoReal());

        StepVerifier.withVirtualTime(controller::avisos)
                .assertNext(event -> assertEquals("conectado", event.comment()))
                .thenAwait(Duration.ofSeconds(10))
                .assertNext(event -> {
                    assertEquals("latido", event.event());
                    assertEquals("ok", event.data());
                })
                .thenCancel()
                .verify();
    }

    private static void assertAllEvent(ServerSentEvent<Object> event, String eventName, Aviso expected) {
        assertAll(
                () -> assertEquals(eventName, event.event()),
                () -> assertEquals(expected, event.data())
        );
    }
}
