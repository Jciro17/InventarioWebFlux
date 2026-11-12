package co.edu.uco.inventario.controller;

import co.edu.uco.inventario.adaptador.AvisosEnTiempoReal;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

/** Canal por el que la pantalla se entera de los cambios de mensajes de MessageUcoLab. */
@RestController
@RequestMapping("/api")
public class AvisosController {

    private static final Duration INTERVALO_LATIDO = Duration.ofSeconds(10);

    private final AvisosEnTiempoReal avisos;

    public AvisosController(AvisosEnTiempoReal avisos) {
        this.avisos = avisos;
    }

    @GetMapping(value = "/avisos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> avisos() {
        Flux<ServerSentEvent<Object>> cambios = avisos.flujo()
                .map(aviso -> ServerSentEvent.<Object>builder(aviso).event("aviso").build());
        // Latido como evento real (no como comentario): la pantalla lo usa para saber que el canal sigue
        // vivo. Un proxy puede dejar abierta la conexión del navegador aunque el backend haya caído, y el
        // navegador no se entera; sin latidos nunca reconectaría.
        Flux<ServerSentEvent<Object>> latido = Flux.interval(INTERVALO_LATIDO)
                .map(tick -> ServerSentEvent.<Object>builder("ok").event("latido").build());
        // Comentario inicial: abre el canal de inmediato (si no, las cabeceras no salen hasta el primer
        // latido y el navegador lo ve "pendiente").
        return Flux.merge(cambios, latido)
                .startWith(ServerSentEvent.<Object>builder().comment("conectado").build());
    }
}
