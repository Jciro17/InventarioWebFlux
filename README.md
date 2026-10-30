# Inventario UCO — API

API REST de gestión de inventario en Spring Boot WebFlux (Java 17). La pantalla está en
[InventarioWebFlux-Frontend](https://github.com/Jciro17/InventarioWebFlux-Frontend).

## Ejecutar

```
run.bat
```

La API queda en `http://localhost:8081/api`:

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/productos` | Lista los productos |
| POST | `/api/productos` | Crea un producto |
| PUT | `/api/productos/{id}` | Actualiza un producto |
| DELETE | `/api/productos/{id}` | Elimina un producto |
| POST | `/api/productos/{id}/entrada?cantidad=N` | Registra una entrada |
| POST | `/api/productos/{id}/salida?cantidad=N` | Registra una salida |
