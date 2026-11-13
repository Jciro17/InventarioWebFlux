# Inventario UCO — API

API REST de gestión de inventario en **Spring Boot WebFlux** (Java 25) integrada con MessageUcoLab.
Es la aplicación de ejemplo de la guía de integración de MessageUcoLab. La pantalla está en
[InventarioWebFlux-Frontend](https://github.com/Jciro17/InventarioWebFlux-Frontend).

| Rama | Contenido |
|---|---|
| `develop` | Inventario integrado con MessageUcoLab y funcionando |
| `test/guide-integration` | Inventario **sin** integrar: los textos están escritos en el código. Sirve para seguir la guía desde el paso 5 |

## Qué tiene la integración

| Pieza | Para qué sirve |
|---|---|
| `messages/MessageUcoLabClient.java` | Pide cada mensaje a `GET /messageucolab/v1/messages/consumer` con el token de la identidad de servicio. Guarda en caché y, si MessageUcoLab falla, usa el texto de respaldo; nunca lanza una excepción |
| `messages/MessageCatalog.java` | Textos de respaldo de cada código (`MSG_PRODSAVED`, `MSG_STOCKLOW`, …) |
| `GET /api/mensajes/{code}` | Entrega el texto de un mensaje ya resuelto: el front de Inventario lo pide aquí y no llama a MessageUcoLab |
| `GET /api/mensajes/estado` | Dice si cada mensaje viene de MessageUcoLab o del respaldo (botón **Comprobar conexión** de la pantalla) |
| `POST /api/mensajes/limpiar-cache` | Vacía la caché para volver a pedir los textos |

Cada respuesta de la API trae el texto listo para mostrar: `{ code, title, content, source, error }`,
donde `source` es `api` (MessageUcoLab) o `fallback` (respaldo).

## Requisitos

- **Java 25** y **Maven** (`mvn`) en el PATH.
- Tu `settings.xml` de Maven con un `<server>` para cada uno de estos dos `<id>` y un token de GitHub con el
  permiso `read:packages`: `github-messageucolab` y `github-seguridad`. Con ellos Maven descarga el cliente de
  MessageUcoLab y el starter de seguridad.
- MessageUcoLab y el componente de seguridad levantados.

## Configurar tus secretos

Inventario consume MessageUcoLab con una **identidad de servicio**. MessageUcoLab deduce la aplicación y el
ambiente del token, así que **no se configuran identificadores de aplicación ni de ambiente**.

1. En la consola de seguridad crea la identidad (**Identidades de servicio → Crear identidad**) y copia su
   Client ID y su secreto.
2. Pide el permiso de consumo (**Solicitar acceso**, rol `MESSAGE_CONSUMER`) y que lo aprueben.
3. Pide al propietario de la aplicación que asocie tu Client ID a un ambiente en MessageUcoLab
   (**Identidades de servicio**).
4. En esta carpeta, copia `.env.example` con el nombre `.env`:

   ```bat
   copy .env.example .env
   ```

5. Abre `.env` y reemplaza los valores vacíos:

   | Variable | Qué poner |
   |---|---|
   | `MESSAGEUCOLAB_CLIENT_ID` | El Client ID del paso 1 (empieza por `svc-`) |
   | `MESSAGEUCOLAB_CLIENT_SECRET` | El secreto del paso 1 |
   | `MESSAGEUCOLAB_URL` | La dirección de MessageUcoLab; por defecto `http://localhost:8000` (Kong) |

El archivo `.env` está en `.gitignore`: **no se sube a Git**. Si prefieres guardar los secretos en Doppler,
en variables del sistema u otro gestor, define esas mismas variables y salta el `.env`.

## Ejecutar

```bat
run.bat
```

`run.bat` carga el `.env`, comprueba que el Client ID y el secreto no estén vacíos y arranca la API. Queda en
`http://localhost:8081/api`.

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/productos` | Lista los productos |
| POST | `/api/productos` | Crea un producto |
| PUT | `/api/productos/{id}` | Actualiza un producto |
| DELETE | `/api/productos/{id}` | Elimina un producto |
| POST | `/api/productos/{id}/entrada?cantidad=N` | Registra una entrada |
| POST | `/api/productos/{id}/salida?cantidad=N` | Registra una salida |
| GET | `/api/mensajes/{code}` | Texto resuelto de un mensaje |
| GET | `/api/mensajes/estado` | Origen de cada mensaje |
| POST | `/api/mensajes/limpiar-cache` | Vacía la caché de mensajes |
