package co.edu.uco.inventario.controller;

import co.edu.uco.inventario.messages.MessageUcoLabClient;
import co.edu.uco.inventario.messages.ResolvedMessage;
import co.edu.uco.inventario.model.Producto;
import co.edu.uco.inventario.service.InventarioService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InventarioController {

    private static final List<String> MSG_CODES = List.of(
        "MSG_PRODSAVED", "MSG_PRODUPD", "MSG_PRODDEL", "MSG_STOCKLOW", "MSG_NOSTOCK", "MSG_INVALID");

    private final InventarioService   inventarioService;
    private final MessageUcoLabClient msgClient;

    public InventarioController(InventarioService inventarioService, MessageUcoLabClient msgClient) {
        this.inventarioService = inventarioService;
        this.msgClient         = msgClient;
    }

    @GetMapping("/productos")
    public Flux<Producto> listar() {
        return inventarioService.listar();
    }

    @PostMapping("/productos")
    public Mono<ResolvedMessage> crear(@RequestBody Producto producto) {
        return inventarioService.guardar(producto);
    }

    @PutMapping("/productos/{id}")
    public Mono<ResolvedMessage> actualizar(@PathVariable String id,
                                            @RequestBody Producto datos) {
        return inventarioService.actualizar(id, datos);
    }

    @DeleteMapping("/productos/{id}")
    public Mono<ResolvedMessage> eliminar(@PathVariable String id) {
        return inventarioService.eliminar(id);
    }

    @PostMapping("/productos/{id}/salida")
    public Mono<ResolvedMessage> salida(@PathVariable String id,
                                        @RequestParam(defaultValue = "1") int cantidad) {
        return inventarioService.salida(id, cantidad);
    }

    @PostMapping("/productos/{id}/entrada")
    public Mono<ResolvedMessage> entrada(@PathVariable String id,
                                         @RequestParam(defaultValue = "1") int cantidad) {
        return inventarioService.entrada(id, cantidad);
    }

    @GetMapping("/mensajes/estado")
    public Flux<ResolvedMessage> estado() {
        return Flux.fromIterable(MSG_CODES).concatMap(msgClient::check);
    }

    @PostMapping("/mensajes/limpiar-cache")
    public Mono<Map<String, String>> limpiarCache() {
        msgClient.clearCache();
        return Mono.just(Map.of("status", "ok", "message", "Cache limpiada correctamente"));
    }
}
