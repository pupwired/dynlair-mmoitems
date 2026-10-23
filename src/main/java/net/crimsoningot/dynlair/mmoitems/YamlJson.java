package net.crimsoningot.dynlair.mmoitems;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;

/** Lossless-ish conversion of loaded YAML values to JSON. */
final class YamlJson {

    private YamlJson() {
    }

    static JsonElement toJson(Object value) {
        if (value == null) return JsonNull.INSTANCE;

        if (value instanceof ConfigurationSection section) {
            JsonObject obj = new JsonObject();
            for (String key : section.getKeys(false))
                obj.add(key, toJson(section.get(key)));
            return obj;
        }
        if (value instanceof Map<?, ?> map) {
            JsonObject obj = new JsonObject();
            for (Map.Entry<?, ?> entry : map.entrySet())
                obj.add(String.valueOf(entry.getKey()), toJson(entry.getValue()));
            return obj;
        }
        if (value instanceof Iterable<?> iterable) {
            JsonArray array = new JsonArray();
            for (Object element : iterable) array.add(toJson(element));
            return array;
        }
        if (value instanceof Boolean b) return new JsonPrimitive(b);
        if (value instanceof Number n) return new JsonPrimitive(n);
        return new JsonPrimitive(value.toString());
    }

    /** Coarse shape of a JSON value, for renderers that don't know a stat. */
    static String kind(JsonElement element) {
        if (element == null || element.isJsonNull()) return "unknown";
        if (element.isJsonArray()) return "array";
        if (element.isJsonObject()) return "object";
        JsonPrimitive p = element.getAsJsonPrimitive();
        if (p.isBoolean()) return "boolean";
        if (p.isNumber()) return "number";
        return "string";
    }
}
