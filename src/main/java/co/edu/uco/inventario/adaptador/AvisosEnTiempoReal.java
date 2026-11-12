package co.edu.uco.inventario.adaptador;

import co.edu.uco.inventario.model.Aviso;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Puente entre el listener de Pulsar y las pantallas conectadas: cada aviso publicado llega a
 * todas las pantallas que tengan abierto el canal. No guarda historial: quien se conecta después
 * no recibe avisos anteriores.
 */
@Component
public class AvisosEnTiempoReal {

    // directBestEffort: sin pantallas conectadas el aviso se descarta, sin acumular memoria.
    private final Sinks.Many<Aviso> sink = Sinks.many().multicast().directBestEffort();

    // Se llama desde hilos del cliente de Pulsar; Sinks exige emisiones serializadas.
    public synchronized void publicar(Aviso aviso) {
        sink.tryEmitNext(aviso);
    }

    public Flux<Aviso> flujo() {
        return sink.asFlux();
    }
}
