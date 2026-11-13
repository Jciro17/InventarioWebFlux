package co.edu.uco.inventario.adaptador;

import co.edu.uco.inventario.model.Aviso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("AvisosEnTiempoReal")
class AvisosEnTiempoRealTest {

    @Test
    @DisplayName("publicar emite el aviso a los suscriptores actuales")
    void publicar_shouldDeliverAvisoToCurrentSubscriber() {
        AvisosEnTiempoReal avisos = new AvisosEnTiempoReal();
        Aviso expected = new Aviso("Mensaje creado", "M-1: Bienvenida");

        StepVerifier.create(avisos.flujo())
                .then(() -> avisos.publicar(expected))
                .expectNext(expected)
                .thenCancel()
                .verify();
    }

    @Test
    @DisplayName("el flujo no reproduce un aviso publicado antes de suscribirse")
    void flujo_shouldNotReplayAvisoPublishedBeforeSubscription() {
        AvisosEnTiempoReal avisos = new AvisosEnTiempoReal();
        avisos.publicar(new Aviso("Mensaje previo", "No se conserva historial"));

        StepVerifier.create(avisos.flujo())
                .then(() -> avisos.publicar(new Aviso("Mensaje actual", "M-2: Actualizado")))
                .assertNext(actual -> assertEquals("Mensaje actual", actual.title()))
                .thenCancel()
                .verify();
    }
}
