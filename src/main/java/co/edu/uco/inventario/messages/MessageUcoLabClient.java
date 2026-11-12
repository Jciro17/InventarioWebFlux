package co.edu.uco.inventario.messages;

import co.edu.uco.messageucolab.client.api.MessageReader;
import co.edu.uco.messageucolab.client.api.exception.MessageClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mensajes de Inventario resueltos con MessageUcoLab.
 *
 *   - Consulta con el {@link MessageReader} del cliente de MessageUcoLab, que llama a
 *     GET /messageucolab/v1/messages/consumer con el token de la identidad de servicio de Inventario
 *   - Caché en memoria 5 minutos
 *   - Respaldo automático al catálogo local si MessageUcoLab falla o no autoriza la identidad
 *   - Nunca lanza excepción al consumidor
 */
@Service
public class MessageUcoLabClient {

    private static final Logger log = LoggerFactory.getLogger(MessageUcoLabClient.class);

    private static final Duration TIMEOUT      = Duration.ofSeconds(4);
    private static final long     CACHE_TTL_MS = 5 * 60 * 1_000L;
    private static final long     MISS_TTL_MS  = 30 * 1_000L;

    private final MessageReader messageReader;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public MessageUcoLabClient(MessageReader messageReader) {
        this.messageReader = messageReader;
    }

    public Mono<ResolvedMessage> get(String code, Map<String, String> vars) {
        CacheEntry cached = cache.get(code);
        if (cached != null && !cached.isExpired()) {
            return Mono.just(applyVars(cached.message, vars));
        }
        return fetch(code).map(msg -> {
            long ttl = "api".equals(msg.getSource()) ? CACHE_TTL_MS : MISS_TTL_MS;
            cache.put(code, new CacheEntry(msg, ttl));
            return applyVars(msg, vars);
        });
    }

    public Mono<ResolvedMessage> get(String code) {
        return get(code, Map.of());
    }

    /** Consulta sin caché, para comprobar la conexión. */
    public Mono<ResolvedMessage> check(String code) {
        return fetch(code);
    }

    public void clearCache() {
        cache.clear();
        log.debug("[MessageUcoLab] cache cleared");
    }

    private Mono<ResolvedMessage> fetch(String code) {
        return messageReader.getDetails(code)
                .timeout(TIMEOUT)
                .map(details -> new ResolvedMessage(code, details.title(), details.content(), "api"))
                .switchIfEmpty(Mono.fromSupplier(() -> fallback(code, "Sin respuesta")))
                .onErrorResume(ex -> {
                    String errorMsg = ex instanceof MessageClientResponseException response
                            ? "HTTP " + response.getStatusCode()
                            : ex.getClass().getSimpleName() + ": " + ex.getMessage();
                    log.warn("[MessageUcoLab] {} -> fallback ({})", code, errorMsg);
                    return Mono.just(fallback(code, errorMsg));
                });
    }

    private ResolvedMessage fallback(String code, String errorMsg) {
        MessageCatalog.Fallback fb = MessageCatalog.FALLBACKS.getOrDefault(
            code, new MessageCatalog.Fallback(code, code)
        );
        return ResolvedMessage.fallback(code, fb.title(), fb.content(), errorMsg);
    }

    private ResolvedMessage applyVars(ResolvedMessage msg, Map<String, String> vars) {
        if (vars == null || vars.isEmpty()) return msg;
        ResolvedMessage result = new ResolvedMessage(
            msg.getCode(),
            MessageCatalog.format(msg.getTitle(), vars),
            MessageCatalog.format(msg.getContent(), vars),
            msg.getSource()
        );
        result.setError(msg.getError());
        return result;
    }

    private static final class CacheEntry {
        final ResolvedMessage message;
        final long expiryMs;
        CacheEntry(ResolvedMessage message, long ttlMs) {
            this.message  = message;
            this.expiryMs = System.currentTimeMillis() + ttlMs;
        }
        boolean isExpired() { return System.currentTimeMillis() > expiryMs; }
    }
}
