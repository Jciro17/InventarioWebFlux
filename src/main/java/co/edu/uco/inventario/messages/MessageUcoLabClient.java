package co.edu.uco.inventario.messages;

import co.edu.uco.inventario.config.MessageUcoLabProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cliente reactivo para MessageUcoLab.
 *
 *   - Llama a la ruta pública GET /messageucolab/v1/public/messages/MSG_XXXX
 *   - Envía los encabezados X-Application-Id y X-Environment-Id; no necesita sesión
 *   - Caché en memoria 5 minutos
 *   - Fallback automático al catálogo local si el servicio falla
 *   - Nunca lanza excepción al consumidor
 */
@Service
public class MessageUcoLabClient {

    private static final Logger log = LoggerFactory.getLogger(MessageUcoLabClient.class);

    private static final Duration TIMEOUT     = Duration.ofSeconds(4);
    private static final long     CACHE_TTL_MS = 5 * 60 * 1_000L;
    private static final long     MISS_TTL_MS  = 30 * 1_000L;

    private final WebClient               webClient;
    private final MessageUcoLabProperties props;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public MessageUcoLabClient(WebClient messageUcoLabWebClient,
                               MessageUcoLabProperties props) {
        this.webClient = messageUcoLabWebClient;
        this.props     = props;
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
        return webClient.get()
                .uri("/messageucolab/v1/public/messages/{code}", code)
                .header("X-Application-Id", props.getApplicationId())
                .header("X-Environment-Id", props.getEnvironmentId())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(TIMEOUT)
                .map(json -> new ResolvedMessage(
                        code,
                        json.path("title").asText(code),
                        json.path("content").asText(code),
                        "api"))
                .onErrorResume(ex -> {
                    String errorMsg = ex instanceof WebClientResponseException wcex
                            ? "HTTP " + wcex.getStatusCode().value()
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
