package co.edu.uco.inventario.messages;

import java.util.Map;

public final class MessageCatalog {
    private MessageCatalog() {}

    public static final Map<String, Fallback> FALLBACKS = Map.ofEntries(
        // ── Operaciones básicas ──────────────────────────────────────────
        Map.entry("MSG_PRODSAVED", new Fallback("Operación completada",
            "Los cambios se guardaron correctamente.")),
        Map.entry("MSG_PRODUPD", new Fallback("Producto actualizado",
            "Los datos de {nombre} se actualizaron correctamente.")),
        Map.entry("MSG_PRODDEL", new Fallback("Producto eliminado",
            "El producto {nombre} se retiró del inventario.")),
        Map.entry("MSG_INVALID", new Fallback("Datos del producto incompletos",
            "Revisa nombre, SKU, cantidad y stock mínimo antes de guardar.")),

        // ── Stock ────────────────────────────────────────────────────────
        Map.entry("MSG_STOCKLOW", new Fallback("Stock por debajo del mínimo",
            "Quedan {cantidad} unidades de {nombre}. Considera reabastecer.")),
        Map.entry("MSG_NOSTOCK", new Fallback("Sin existencias suficientes",
            "No hay unidades de {nombre} para registrar la salida.")),
        Map.entry("MSG_STOCKOK", new Fallback("Stock normalizado",
            "El stock de {nombre} volvió al nivel mínimo requerido ({cantidad} unidades).")),
        Map.entry("MSG_AGOTADO", new Fallback("Producto agotado",
            "{nombre} está agotado.")),
        Map.entry("MSG_STOCKZERO", new Fallback("Producto agotado",
            "{nombre} quedó sin unidades. Genera una orden de compra.")),
        Map.entry("MSG_ENTRADA_OK", new Fallback("Entrada registrada",
            "Se agregaron {cantidad} unidades a {nombre}. Stock actual: {stock}.")),
        Map.entry("MSG_SALIDA_OK", new Fallback("Salida registrada",
            "Se descontaron {cantidad} unidades de {nombre}. Stock actual: {stock}.")),

        // ── Carga masiva / Excel ─────────────────────────────────────────
        Map.entry("MSG_CARGA_OK", new Fallback("Carga masiva completada",
            "Se importaron {total} productos correctamente desde el archivo.")),
        Map.entry("MSG_CARGA_PARCIAL", new Fallback("Carga completada con advertencias",
            "Se importaron {ok} de {total} productos. {errores} filas tenían errores y fueron omitidas.")),
        Map.entry("MSG_CARGA_VACIA", new Fallback("Archivo sin datos",
            "El archivo no contiene filas de productos válidas para importar.")),
        Map.entry("MSG_CARGA_ERROR", new Fallback("Error al procesar el archivo",
            "No se pudo leer el archivo. Verifica que sea un Excel válido (.xlsx) y vuelve a intentarlo.")),
        Map.entry("MSG_CARGA_SKU_DUP", new Fallback("SKU duplicado en el archivo",
            "El SKU {sku} aparece más de una vez en el archivo. Solo se registró la primera ocurrencia.")),
        Map.entry("MSG_CARGA_SKU_EXIST", new Fallback("SKU ya registrado",
            "El SKU {sku} ({nombre}) ya existe en el inventario. Se omitió la fila.")),
        Map.entry("MSG_CARGA_FILA_INVALIDA", new Fallback("Fila inválida omitida",
            "La fila {fila} no tiene todos los campos requeridos (nombre, SKU, cantidad, stock mínimo).")),

        // ── SKU / búsqueda ───────────────────────────────────────────────
        Map.entry("MSG_SKU_DUP", new Fallback("SKU ya registrado",
            "El código {sku} ya está en uso por otro producto. Usa un SKU diferente.")),
        Map.entry("MSG_NOTFOUND", new Fallback("Producto no encontrado",
            "No existe un producto con el identificador indicado.")),
        Map.entry("MSG_SEARCH_EMPTY", new Fallback("Sin resultados",
            "No se encontraron productos que coincidan con \"{termino}\".")),

        // ── Validaciones de campo ────────────────────────────────────────
        Map.entry("MSG_VAL_NOMBRE", new Fallback("Nombre requerido",
            "El nombre del producto no puede estar vacío.")),
        Map.entry("MSG_VAL_SKU", new Fallback("SKU requerido",
            "El SKU no puede estar vacío. Usa un código único para identificar el producto.")),
        Map.entry("MSG_VAL_CANTIDAD", new Fallback("Cantidad inválida",
            "La cantidad debe ser un número entero igual o mayor a cero.")),
        Map.entry("MSG_VAL_STOCKMIN", new Fallback("Stock mínimo inválido",
            "El stock mínimo debe ser un número entero igual o mayor a cero.")),

        // ── Sistema ──────────────────────────────────────────────────────
        Map.entry("MSG_SERVICIO_NO_DISP", new Fallback("Servicio de mensajes no disponible",
            "El servicio de mensajes no responde. Se están usando los mensajes locales.")),
        Map.entry("MSG_SESION_EXPIRADA", new Fallback("Sesión expirada",
            "Tu sesión ha expirado. Vuelve a iniciar sesión para continuar."))
    );

    public record Fallback(String title, String content) {}

    public static String format(String text, Map<String, String> vars) {
        if (text == null || vars == null || vars.isEmpty()) return text;
        String result = text;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", e.getValue());
        }
        return result;
    }
}
