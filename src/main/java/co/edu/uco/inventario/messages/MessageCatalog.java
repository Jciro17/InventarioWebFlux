package co.edu.uco.inventario.messages;

import java.util.Map;
import java.util.Set;

public final class MessageCatalog {
    private MessageCatalog() {}

    public static final Set<String> CODES = Set.of(
        "MSG_PRODSAVED", "MSG_PRODUPD", "MSG_PRODDEL", "MSG_INVALID",
        "MSG_STOCKLOW", "MSG_NOSTOCK", "MSG_STOCKOK", "MSG_AGOTADO", "MSG_STOCKZERO",
        "MSG_ENTRADA_OK", "MSG_SALIDA_OK",
        "MSG_CARGA_OK", "MSG_CARGA_PARCIAL", "MSG_CARGA_VACIA", "MSG_CARGA_ERROR",
        "MSG_CARGA_SKU_DUP", "MSG_CARGA_SKU_EXIST", "MSG_CARGA_FILA_INVALIDA",
        "MSG_SKU_DUP", "MSG_NOTFOUND", "MSG_SEARCH_EMPTY",
        "MSG_VAL_NOMBRE", "MSG_VAL_SKU", "MSG_VAL_CANTIDAD", "MSG_VAL_STOCKMIN",
        "MSG_SERVICIO_NO_DISP", "MSG_SESION_EXPIRADA"
    );

    public static String format(String text, Map<String, String> vars) {
        if (text == null || vars == null || vars.isEmpty()) return text;
        String result = text;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", e.getValue());
        }
        return result;
    }
}
