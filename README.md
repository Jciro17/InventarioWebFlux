# Inventario UCO — API

API REST de gestión de inventario en **Spring Boot WebFlux** (Java 17) integrada con MessageUcoLab.
Es la aplicación de ejemplo de la guía de integración de MessageUcoLab. La pantalla está en
[InventarioWebFlux-Frontend](https://github.com/Jciro17/InventarioWebFlux-Frontend).

| Rama | Contenido |
|---|---|
| `develop` | Inventario integrado con MessageUcoLab y funcionando |
| `test/guide-integration` | Inventario **sin** integrar: los textos están escritos en el código. Sirve para seguir la guía desde el paso 5 |

## Qué tiene la integración

| Pieza | Para qué sirve |
|---|---|
| `messages/MessageUcoLabClient.java` | Pide cada mensaje a la ruta pública `GET /messageucolab/v1/public/messages/{código}` con los encabezados `X-Application-Id` y `X-Environment-Id`. Guarda en caché y, si MessageUcoLab falla, usa el texto de respaldo; nunca lanza una excepción |
| `messages/MessageCatalog.java` | Textos de respaldo de cada código (`MSG_PRODSAVED`, `MSG_STOCKLOW`, …) |
| `GET /api/mensajes/estado` | Dice si cada mensaje viene de MessageUcoLab o del respaldo (botón **Comprobar conexión** de la pantalla) |
| `POST /api/mensajes/limpiar-cache` | Vacía la caché para volver a pedir los textos |

Cada respuesta de la API trae el texto listo para mostrar: `{ code, title, content, source, error }`,
donde `source` es `api` (MessageUcoLab) o `fallback` (respaldo).

## Ejecutar

Inventario consume MessageUcoLab con su identidad de servicio. Crea la identidad en el componente de
seguridad, pide acceso a las rutas de consumo y pide al propietario en MessageUcoLab que asocie su
Client ID al ambiente (**Identidades de servicio**). MessageUcoLab deduce la aplicación y el ambiente del
token, así que no se configuran identificadores. El Client ID y el secreto llegan por variables de entorno,
nunca en un archivo versionado:

```bat
set MESSAGEUCOLAB_CLIENT_ID=<client id de la identidad>
set MESSAGEUCOLAB_CLIENT_SECRET=<secreto de la identidad>
run.bat
```

La API queda en `http://localhost:8081/api`. MessageUcoLab se busca en `http://localhost:8000` (Kong);
si está en otra dirección, define `MESSAGEUCOLAB_BACKEND`.

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/productos` | Lista los productos |
| POST | `/api/productos` | Crea un producto |
| PUT | `/api/productos/{id}` | Actualiza un producto |
| DELETE | `/api/productos/{id}` | Elimina un producto |
| POST | `/api/productos/{id}/entrada?cantidad=N` | Registra una entrada |
| POST | `/api/productos/{id}/salida?cantidad=N` | Registra una salida |
| GET | `/api/mensajes/estado` | Origen de cada mensaje |
| POST | `/api/mensajes/limpiar-cache` | Vacía la caché de mensajes |
