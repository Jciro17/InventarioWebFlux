package co.edu.uco.inventario.controller;

import co.edu.uco.inventario.model.Aviso;
import co.edu.uco.inventario.model.Producto;
import co.edu.uco.inventario.service.InventarioService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping("/productos")
    public Flux<Producto> listar() {
        return inventarioService.listar();
    }

    @PostMapping("/productos")
    public Mono<Aviso> crear(@RequestBody Producto producto) {
        return inventarioService.guardar(producto);
    }

    @PutMapping("/productos/{id}")
    public Mono<Aviso> actualizar(@PathVariable String id, @RequestBody Producto datos) {
        return inventarioService.actualizar(id, datos);
    }

    @DeleteMapping("/productos/{id}")
    public Mono<Aviso> eliminar(@PathVariable String id) {
        return inventarioService.eliminar(id);
    }

    @PostMapping("/productos/{id}/salida")
    public Mono<Aviso> salida(@PathVariable String id, @RequestParam(defaultValue = "1") int cantidad) {
        return inventarioService.salida(id, cantidad);
    }

    @PostMapping("/productos/{id}/entrada")
    public Mono<Aviso> entrada(@PathVariable String id, @RequestParam(defaultValue = "1") int cantidad) {
        return inventarioService.entrada(id, cantidad);
    }
}
