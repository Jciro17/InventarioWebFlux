package co.edu.uco.inventario.adaptador;

import co.edu.uco.messageucolab.client.api.MessageReader;
import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageListener;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageChangeListener")
class MessageChangeListenerTest {

    @Mock
    private PulsarClient pulsarClient;

    @Mock
    private MessageReader messageReader;

    @Mock
    private Consumer<String> consumer;

    @Mock
    private Message<String> pulsarMessage;

    private ConsumerBuilder<String> consumerBuilder;
    private AvisosEnTiempoReal avisos;
    private MessageChangeListener listener;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        consumerBuilder = mock(ConsumerBuilder.class, RETURNS_SELF);
        avisos = new AvisosEnTiempoReal();
    }

    @AfterEach
    void closeListener() {
        if (listener != null) {
            listener.close();
        }
    }

    @Test
    @DisplayName("sin ambiente no crea un consumidor Pulsar")
    void constructor_shouldSkipSubscriptionWithoutEnvironment() {
        listener = new MessageChangeListener(pulsarClient, messageReader, avisos,
                "", "persistent://test/events", "test-subscription");

        verify(pulsarClient, never()).newConsumer(Schema.STRING);
    }

    @Test
    @DisplayName("al crear un mensaje consulta el título, publica aviso y confirma el evento")
    void listener_shouldPublishCreatedMessageTitleAndAcknowledge() {
        MessageDetails details = details("Bienvenida");
        when(messageReader.getDetails("MSG_NEW")).thenReturn(Mono.just(details));
        when(pulsarMessage.getValue()).thenReturn("{\"eventType\":\"MessageCreated\",\"messageCode\":\"MSG_NEW\"}");
        when(consumer.acknowledgeAsync(pulsarMessage)).thenReturn(CompletableFuture.completedFuture(null));
        MessageListener<String> callback = subscribeAndCapture();

        StepVerifier.create(avisos.flujo())
                .then(() -> callback.received(consumer, pulsarMessage))
                .assertNext(aviso -> assertAll(
                        () -> assertEquals("Mensaje creado", aviso.title()),
                        () -> assertEquals("MSG_NEW: Bienvenida", aviso.content())
                ))
                .thenCancel()
                .verify();

        verify(messageReader).getDetails("MSG_NEW");
        verify(consumer).acknowledgeAsync(pulsarMessage);
        verify(consumerBuilder).topic("persistent://test/events-env-1");
    }

    @Test
    @DisplayName("publica notificación genérica si el mensaje actualizado ya no está disponible")
    void listener_shouldPublishFallbackWhenUpdatedMessageCannotBeRead() {
        when(messageReader.getDetails("MSG_GONE")).thenReturn(Mono.error(
                WebClientResponseException.create(404, "Not Found", null, null, StandardCharsets.UTF_8)));
        when(pulsarMessage.getValue()).thenReturn("{\"eventType\":\"MessageUpdated\",\"messageCode\":\"MSG_GONE\"}");
        when(consumer.acknowledgeAsync(pulsarMessage)).thenReturn(CompletableFuture.completedFuture(null));
        MessageListener<String> callback = subscribeAndCapture();

        StepVerifier.create(avisos.flujo())
                .then(() -> callback.received(consumer, pulsarMessage))
                .assertNext(aviso -> assertAll(
                        () -> assertEquals("Mensaje actualizado", aviso.title()),
                        () -> assertEquals("El mensaje MSG_GONE cambió en MessageUcoLab.", aviso.content())
                ))
                .thenCancel()
                .verify();

        verify(consumer).acknowledgeAsync(pulsarMessage);
    }

    @Test
    @DisplayName("ignora un evento ilegible, pero confirma la recepción y procesa el siguiente")
    void listener_shouldAcknowledgeMalformedEventAndContinueWithNextMessage() {
        when(pulsarMessage.getValue()).thenReturn("not-json",
                "{\"eventType\":\"MessageDeleted\",\"messageCode\":\"MSG_OLD\"}");
        when(consumer.acknowledgeAsync(pulsarMessage)).thenReturn(CompletableFuture.completedFuture(null));
        MessageListener<String> callback = subscribeAndCapture();

        StepVerifier.create(avisos.flujo())
                .then(() -> callback.received(consumer, pulsarMessage))
                .then(() -> callback.received(consumer, pulsarMessage))
                .assertNext(aviso -> assertAll(
                        () -> assertEquals("Mensaje eliminado", aviso.title()),
                        () -> assertEquals("El mensaje MSG_OLD se eliminó en MessageUcoLab.", aviso.content())
                ))
                .thenCancel()
                .verify();

        verify(consumer, times(2)).acknowledgeAsync(pulsarMessage);
    }

    @SuppressWarnings("unchecked")
    private MessageListener<String> subscribeAndCapture() {
        when(pulsarClient.newConsumer(Schema.STRING)).thenReturn(consumerBuilder);
        when(consumerBuilder.subscribeAsync()).thenReturn(CompletableFuture.completedFuture(consumer));
        when(consumer.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
        listener = new MessageChangeListener(pulsarClient, messageReader, avisos,
                "env-1", "persistent://test/events", "test-subscription");
        ArgumentCaptor<MessageListener<String>> callback = (ArgumentCaptor) ArgumentCaptor.forClass(MessageListener.class);
        verify(consumerBuilder).messageListener(callback.capture());
        verify(consumerBuilder).subscriptionName("test-subscription");
        return callback.getValue();
    }

    private MessageDetails details(String title) {
        Instant timestamp = Instant.parse("2026-03-04T05:06:07Z");
        return new MessageDetails("MSG_NEW", "es", "es", title, "Contenido", "INFO", "test", "test",
                timestamp, timestamp);
    }
}
