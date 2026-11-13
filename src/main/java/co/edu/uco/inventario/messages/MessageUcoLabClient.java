package co.edu.uco.inventario.messages;

import co.edu.uco.messageucolab.client.api.MessageReader;
import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MessageUcoLabClient {

    private static final Logger log = LoggerFactory.getLogger(MessageUcoLabClient.class);

    private static final long CACHE_TTL_MS = 5 * 60 * 1_000L;

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
            cache.put(code, new CacheEntry(msg, CACHE_TTL_MS));
            return applyVars(msg, vars);
        });
    }

    public Mono<ResolvedMessage> get(String code) {
        return get(code, Map.of());
    }

    /** Consulta MessageReader sin caché local y sustituye las variables de la respuesta. */
    public Mono<ResolvedMessage> getFresh(String code, Map<String, String> vars) {
        return fetch(code).map(message -> applyVars(message, vars));
    }

    /** Conserva el detalle del SDK sin usar la caché local, con las variables resueltas. */
    public Mono<MessageDetails> getFreshDetails(String code, Map<String, String> vars) {
        return readDetails(code).map(details -> applyVars(details, vars));
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
        return readDetails(code)
                .map(details -> new ResolvedMessage(code, details.title(), details.content(), "api"));
    }

    private Mono<MessageDetails> readDetails(String code) {
        return messageReader.getDetails(code)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, code)))
                .doOnError(error -> log.warn("[MessageUcoLab] {}", code, error));
    }

    private MessageDetails applyVars(MessageDetails details, Map<String, String> vars) {
        if (vars == null || vars.isEmpty()) return details;
        return new MessageDetails(
                details.code(),
                details.sourceLanguage(),
                details.targetLanguage(),
                MessageCatalog.format(details.title(), vars),
                MessageCatalog.format(details.content(), vars),
                details.type(),
                details.category(),
                details.functionality(),
                details.createdAt(),
                details.updatedAt()
        );
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
