package co.edu.uco.inventario.service;

import co.edu.uco.inventario.messages.MessageUcoLabClient;
import co.edu.uco.inventario.messages.ResolvedMessage;
import co.edu.uco.inventario.model.Producto;
import co.edu.uco.messageucolab.client.api.MessageReader;
import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class InventarioService {

    private final MessageUcoLabClient msgClient;
    private final List<Producto> productos = new CopyOnWriteArrayList<>();
    private final MessageReader mesaReader;

    public InventarioService(MessageUcoLabClient msgClient, MessageReader mesaReader) {
        this.msgClient = msgClient;
        this.mesaReader = mesaReader;
        productos.add(new Producto("Mouse inalámbrico",  "MOUSE-001", 12, 5));
        productos.add(new Producto("Teclado mecánico",   "TECLADO-002", 3, 5));
        productos.add(new Producto("Monitor 24\"",        "MONITOR-003", 0, 2));
        productos.add(new Producto("Cable HDMI 2m",       "HDMI-004",  20, 8));
    }

    public Flux<Producto> listar() {
        return Flux.fromIterable(productos);
    }

    public Mono<Producto> buscar(String id) {
        return Mono.justOrEmpty(
            productos.stream().filter(p -> p.getId().equals(id)).findFirst()
        );
    }

    public Mono<MessageDetails> guardar(Producto producto) {
        if (esInvalido(producto)) return mesaReader.getDetails("MSG_INVALID");
        productos.add(producto);
        return mesaReader.getDetails("MSG_PRODSAVED");
    }

    public Mono<ResolvedMessage> actualizar(String id, Producto datos) {
        if (esInvalido(datos)) return msgClient.get("MSG_INVALID");
        Optional<Producto> found = productos.stream().filter(p -> p.getId().equals(id)).findFirst();
        if (found.isEmpty()) return Mono.just(new ResolvedMessage(id, "No encontrado",
                "El producto con id " + id + " no existe.", "fallback"));
        Producto p = found.get();
        p.setNombre(datos.getNombre());
        p.setSku(datos.getSku());
        p.setCantidad(datos.getCantidad());
        p.setStockMinimo(datos.getStockMinimo());
        return msgClient.get("MSG_PRODUPD", Map.of("nombre", p.getNombre()));
    }

    public Mono<ResolvedMessage> eliminar(String id) {
        Optional<Producto> found = productos.stream().filter(p -> p.getId().equals(id)).findFirst();
        if (found.isEmpty()) return Mono.just(new ResolvedMessage(id, "No encontrado",
                "El producto con id " + id + " no existe.", "fallback"));
        Producto p = found.get();
        productos.removeIf(x -> x.getId().equals(id));
        return msgClient.get("MSG_PRODDEL", Map.of("nombre", p.getNombre()));
    }

    public Mono<ResolvedMessage> salida(String id, int cantidad) {
        Optional<Producto> found = productos.stream().filter(p -> p.getId().equals(id)).findFirst();
        if (found.isEmpty()) return Mono.just(new ResolvedMessage(id, "No encontrado",
                "El producto no existe.", "fallback"));
        Producto p = found.get();
        if (p.getCantidad() < cantidad) {
            return msgClient.get("MSG_NOSTOCK", Map.of("nombre", p.getNombre()));
        }
        p.setCantidad(p.getCantidad() - cantidad);
        if (p.isStockBajo()) {
            return msgClient.get("MSG_STOCKLOW",
                Map.of("nombre", p.getNombre(), "cantidad", String.valueOf(p.getCantidad())));
        }
        return msgClient.get("MSG_PRODUPD", Map.of("nombre", p.getNombre()));
    }

    public Mono<ResolvedMessage> entrada(String id, int cantidad) {
        Optional<Producto> found = productos.stream().filter(p -> p.getId().equals(id)).findFirst();
        if (found.isEmpty()) return Mono.just(new ResolvedMessage(id, "No encontrado",
                "El producto no existe.", "fallback"));
        Producto p = found.get();
        p.setCantidad(p.getCantidad() + cantidad);
        return msgClient.get("MSG_PRODUPD", Map.of("nombre", p.getNombre()));
    }

    private boolean esInvalido(Producto p) {
        return p == null
            || p.getNombre() == null || p.getNombre().isBlank()
            || p.getSku()    == null || p.getSku().isBlank()
            || p.getCantidad() < 0
            || p.getStockMinimo() < 0;
    }
}
