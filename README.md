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
| `messages/MessageUcoLabClient.java` | Consulta `MessageReader` con la identidad de servicio. Guarda las respuestas exitosas en caché y propaga los errores sin respaldo |
| `messages/MessageCatalog.java` | Códigos permitidos (`MSG_PRODSAVED`, `MSG_STOCKLOW`, …) y sustitución de variables, sin textos locales |
| `controller/MessageClientExceptionHandler.java` | Conserva el estado HTTP y el cuerpo del error remoto, sin reenviar cabeceras de autenticación; los fallos de transporte devuelven 502 o 504 |
| `GET /api/mensajes/estado` | Consulta los mensajes; si alguno falla devuelve el error, no una respuesta exitosa parcial (botón **Comprobar conexión**) |
| `POST /api/mensajes/limpiar-cache` | Vacía la caché para volver a pedir los textos |

Las consultas de `/api/mensajes` y las operaciones distintas de crear un producto devuelven el texto
listo para mostrar: `{ code, title, content, source, error }`, con `source: "api"` (MessageUcoLab).
No hay respaldo local: los errores se devuelven como errores HTTP.

Al crear un producto con **Guardar**, `POST /api/productos` consulta `MSG_PRODSAVED` mediante
`MessageReader.getDetails` a través de `MessageUcoLabClient.getFreshDetails`, sin reutilizar la caché local de Inventario. El backend sustituye
`{nombre}` por el nombre del producto tanto en el título como en el contenido y el frontend muestra
la respuesta. Se conserva el contrato `MessageDetails` incorporado en `develop`: incluye idioma,
clasificación y fechas, además de código, título y contenido; no añade el campo `source` de `ResolvedMessage`.
Si la consulta falla, devuelve el estado HTTP y el cuerpo del error remoto, sin sustituirlos
por un mensaje exitoso. La caché de
los demás mensajes no cambia. Esta consulta no invalida la caché del servidor MessageUcoLab.

Los errores de mensajes no revierten las operaciones de inventario: el guardado y los movimientos
se realizan antes de consultar su mensaje. No reintentes una operación automáticamente por un error de texto.

## Ejecutar

Inventario usa la versión oficial `0.1.2` de `messageucolab-spring-boot-starter`, publicada en
GitHub Packages. No requiere instalar el cliente experimental ni un SNAPSHOT local de MessageUcoLab.

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

Para descargar las dependencias privadas de GitHub Packages, Maven usa `.mvn/settings.xml` con
`GITHUB_PACKAGES_USER` y `GITHUB_PACKAGES_TOKEN` (permiso `read:packages`). No guardes sus valores
en el repositorio. En el entorno local compartido se pueden inyectar con Doppler, usando JDK 25:

```powershell
doppler run --project messageucolab --config dev -- .\mvnw.cmd spring-boot:run "-Dmaven.test.skip=true"
```

La API queda en `http://localhost:8081/api`. MessageUcoLab se busca en `http://localhost:8000` (Kong);
si está en otra dirección, define `MESSAGEUCOLAB_URL`.

## Avisos de mensajes incorporados desde develop

Se conserva la integración de Pulsar 3.2.2 y el canal SSE que llegaron en `origin/develop`,
independientes del cliente oficial HTTP `0.1.2`. No se incorpora el cliente experimental de eventos.

- `PULSAR_URL` configura la dirección del broker (por defecto `pulsar://localhost:6800`).
- `MESSAGEUCOLAB_ENVIRONMENT_ID` identifica el ambiente cuyo topic se escucha; debe corresponder al
  de la identidad de servicio. Solo se usa para los avisos: las consultas HTTP siguen deduciendo el
  ambiente de la identidad. Si el valor configurado queda vacío, el listener no se suscribe.
- `MESSAGEUCOLAB_EVENTS_TOPIC_PREFIX` configura el prefijo del topic; el nombre final es
  `<prefijo>-<environment-id>`.
- `MESSAGEUCOLAB_SUBSCRIPTION_NAME` identifica la suscripción, que es `Exclusive`: cada instancia
  simultánea necesita un nombre distinto.
- `GET /api/avisos` emite eventos SSE `aviso` con `{ title, content }` y `latido` cada diez segundos.
  No guarda historial. El consumidor de ese canal debe estar implementado en la aplicación que lo usa;
  esta integración no modifica el frontend.

Los avisos vuelven a consultar el título vigente mediante `MessageReader`, pero no invalidan la caché
de `MessageUcoLabClient`: para esa caché se conservan el TTL y `/api/mensajes/limpiar-cache`.
No se modifican los permisos ni el despliegue del componente de seguridad.

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
| GET | `/api/avisos` | Canal SSE de avisos de mensajes y latidos |
