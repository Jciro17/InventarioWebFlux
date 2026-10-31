package co.edu.uco.inventario.service;

import co.edu.uco.inventario.model.Aviso;
import co.edu.uco.inventario.model.Producto;
import co.edu.uco.messageucolab.client.api.MessageReader;
import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class InventarioService {

    private final List<Producto> productos = new CopyOnWriteArrayList<>();
    private final MessageReader messageReader;

    public InventarioService(MessageReader messageReader) {
        this.messageReader = messageReader;
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
        if (esInvalido(producto)){
            return messageReader.getDetails("MSG_PRUEBA");
        }
        productos.add(producto);
        return  messageReader.getDetails("MSG_PRUEBA");
    }

    public Mono<Aviso> actualizar(String id, Producto datos) {
        if (esInvalido(datos)) return Mono.just(datosIncompletos());
        Optional<Producto> found = productos.stream().filter(p -> p.getId().equals(id)).findFirst();
        if (found.isEmpty()) return Mono.just(noEncontrado());
        Producto p = found.get();
        p.setNombre(datos.getNombre());
        p.setSku(datos.getSku());
        p.setCantidad(datos.getCantidad());
        p.setStockMinimo(datos.getStockMinimo());
        return Mono.just(actualizado(p));
    }

    public Mono<Aviso> eliminar(String id) {
        Optional<Producto> found = productos.stream().filter(p -> p.getId().equals(id)).findFirst();
        if (found.isEmpty()) return Mono.just(noEncontrado());
        Producto p = found.get();
        productos.removeIf(x -> x.getId().equals(id));
        return Mono.just(new Aviso("Producto eliminado",
            "El producto " + p.getNombre() + " se retiró del inventario."));
    }

    public Mono<Aviso> salida(String id, int cantidad) {
        Optional<Producto> found = productos.stream().filter(p -> p.getId().equals(id)).findFirst();
        if (found.isEmpty()) return Mono.just(noEncontrado());
        Producto p = found.get();
        if (p.getCantidad() < cantidad) {
            return Mono.just(new Aviso("Sin existencias suficientes",
                "No hay unidades de " + p.getNombre() + " para registrar la salida."));
        }
        p.setCantidad(p.getCantidad() - cantidad);
        if (p.isStockBajo()) {
            return Mono.just(new Aviso("Stock por debajo del mínimo",
                "Quedan " + p.getCantidad() + " unidades de " + p.getNombre() + ". Considera reabastecer."));
        }
        return Mono.just(actualizado(p));
    }

    public Mono<Aviso> entrada(String id, int cantidad) {
        Optional<Producto> found = productos.stream().filter(p -> p.getId().equals(id)).findFirst();
        if (found.isEmpty()) return Mono.just(noEncontrado());
        Producto p = found.get();
        p.setCantidad(p.getCantidad() + cantidad);
        return Mono.just(actualizado(p));
    }

    private static Aviso actualizado(Producto p) {
        return new Aviso("Producto actualizado", "Los datos de " + p.getNombre() + " se actualizaron correctamente.");
    }

    private static Aviso datosIncompletos() {
        return new Aviso("Datos del producto incompletos",
            "Revisa nombre, SKU, cantidad y stock mínimo antes de guardar.");
    }

    private static Aviso noEncontrado() {
        return new Aviso("Producto no encontrado", "El producto indicado no existe.");
    }

    private boolean esInvalido(Producto p) {
        return p == null
            || p.getNombre() == null || p.getNombre().isBlank()
            || p.getSku()    == null || p.getSku().isBlank()
            || p.getCantidad() < 0
            || p.getStockMinimo() < 0;
    }
}
