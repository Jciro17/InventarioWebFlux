package co.edu.uco.inventario.adaptador;

import co.edu.uco.inventario.model.Aviso;
import co.edu.uco.messageucolab.client.api.MessageReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Escucha los avisos que MessageUcoLab publica cuando cambia un mensaje del ambiente que consume
 * este backend y los convierte en notificaciones para las pantallas conectadas.
 */
@Component
public class MessageChangeListener {

    private static final Logger log = LoggerFactory.getLogger(MessageChangeListener.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final MessageReader messageReader;
    private final AvisosEnTiempoReal avisos;
    private final CompletableFuture<Consumer<String>> consumer;

    public MessageChangeListener(
            PulsarClient client,
            MessageReader messageReader,
            AvisosEnTiempoReal avisos,
            @Value("${messageucolab.environment-id}") String environmentId,
            @Value("${messageucolab.events-topic-prefix:persistent://public/default/message-events}") String prefix,
            @Value("${messageucolab.subscription-name:inventario-webflux}") String subscriptionName) {

        this.messageReader = messageReader;
        this.avisos = avisos;
        String topic = prefix + "-" + environmentId;

        // subscribeAsync: si Pulsar no está disponible el backend arranca igual y se registra el error.
        this.consumer = client.newConsumer(Schema.STRING)
                .topic(topic)
                .subscriptionName(subscriptionName)
                .subscriptionType(SubscriptionType.Exclusive)
                .messageListener((c, msg) -> {
                    procesar(msg.getValue());
                    // Siempre se confirma: un aviso ilegible reintentado indefinidamente bloquearía la cola.
                    c.acknowledgeAsync(msg);
                })
                .subscribeAsync();

        this.consumer.whenComplete((created, error) -> {
            if (error != null) {
                log.error("No fue posible suscribirse al topic {}", topic, error);
            } else {
                log.info("Escuchando avisos de MessageUcoLab en el topic {}", topic);
            }
        });
    }

    private void procesar(String json) {
        try {
            JsonNode evento = mapper.readTree(json);
            String tipo = evento.path("eventType").asText("");
            String codigo = evento.path("messageCode").asText("");
            // Un texto que no es un aviso del contrato (sin eventType) no debe notificar nada.
            if (!evento.isObject() || tipo.isBlank()) {
                log.warn("Se ignora un aviso sin eventType: {}", json);
                return;
            }
            log.debug("Aviso recibido de MessageUcoLab: {} {}", tipo, codigo);
            construirAviso(tipo, codigo).subscribe(avisos::publicar);
        } catch (Exception e) {
            log.warn("Aviso de MessageUcoLab ilegible: {}", json, e);
        }
    }

    private Mono<Aviso> construirAviso(String tipo, String codigo) {
        if ("MessagesChanged".equals(tipo) || codigo.isBlank()) {
            return Mono.just(new Aviso("Mensajes actualizados",
                    "Cambió la disponibilidad de varios mensajes en MessageUcoLab."));
        }
        return switch (tipo) {
            case "MessageDeleted" -> Mono.just(new Aviso("Mensaje eliminado",
                    "El mensaje " + codigo + " se eliminó en MessageUcoLab."));
            case "MessageCreated" -> conTitulo("Mensaje creado", codigo);
            case "MessageUpdated" -> conTitulo("Mensaje actualizado", codigo);
            default -> Mono.just(new Aviso("Mensaje modificado",
                    "El mensaje " + codigo + " cambió en MessageUcoLab."));
        };
    }

    // Vuelve a consultar el mensaje para mostrar su título vigente. Si ya no está disponible para este
    // ambiente (por ejemplo, se le quitó la asociación), se avisa del cambio sin título.
    private Mono<Aviso> conTitulo(String titulo, String codigo) {
        return messageReader.getDetails(codigo)
                .map(detalle -> new Aviso(titulo, codigo + ": " + detalle.title()))
                .onErrorResume(error -> Mono.just(new Aviso(titulo,
                        "El mensaje " + codigo + " cambió en MessageUcoLab.")));
    }

    @PreDestroy
    void close() {
        consumer.thenCompose(Consumer::closeAsync).exceptionally(error -> null);
    }
}
