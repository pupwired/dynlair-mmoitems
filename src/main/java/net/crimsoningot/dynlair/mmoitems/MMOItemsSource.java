package net.crimsoningot.dynlair.mmoitems;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.crimsoningot.dynlair.core.api.DocSource;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import net.Indyuce.mmoitems.stat.data.random.RandomBooleanData;
import net.Indyuce.mmoitems.stat.data.random.RandomStatData;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Exports the MMOItems stat registry, item types and (tagged) item templates. */
final class MMOItemsSource implements DocSource {

    private final DynlairMMOItemsPlugin plugin;

    MMOItemsSource(DynlairMMOItemsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getId() {
        return "mmoitems";
    }

    @Override
    public @NotNull String getDisplayName() {
        return "MMOItems";
    }

    @Override
    public @NotNull JsonObject export() {
        boolean all = "all".equalsIgnoreCase(plugin.getConfig().getString("tagging.mode", "opt-in"));

        JsonObject data = new JsonObject();
        data.addProperty("tagging_mode", all ? "all" : "opt-in");
        data.add("stat_registry", statRegistry());
        data.add("types", types());
        data.add("items", items(all));
        return data;
    }

    // ------------------------------------------------------------------ stats

    private JsonArray statRegistry() {
        List<ItemStat<?, ?>> stats = new ArrayList<>(MMOItems.plugin.getStats().getAll());
        stats.sort(Comparator.comparing(ItemStat::getId));

        JsonArray array = new JsonArray();
        for (ItemStat<?, ?> stat : stats) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", stat.getId());
            obj.addProperty("name", strip(stat.getName()));
            obj.addProperty("config_path", stat.getPath());
            obj.addProperty("nbt_path", stat.getNBTPath());

            JsonArray description = new JsonArray();
            for (String line : stat.getLore()) description.add(strip(line));
            obj.add("description", description);

            if (stat.getCategory() == null) obj.add("category", JsonNull.INSTANCE);
            else obj.addProperty("category", stat.getCategory().getId());

            JsonArray aliases = new JsonArray();
            String[] aliasList = stat.getAliases();
            if (aliasList != null) for (String alias : aliasList) aliases.add(alias);
            obj.add("aliases", aliases);

            JsonArray compatible = new JsonArray();
            for (String type : stat.getCompatibleTypes()) compatible.add(type);
            obj.add("compatible_types", compatible); // empty = all types

            obj.addProperty("display_material", stat.getDisplayMaterial().name());
            obj.addProperty("enabled", stat.isEnabled());
            obj.addProperty("stat_class", stat.getClass().getSimpleName());
            obj.addProperty("builtin", stat.getClass().getName().startsWith("net.Indyuce.mmoitems."));
            array.add(obj);
        }
        return array;
    }

    // ------------------------------------------------------------------ types

    private JsonArray types() {
        JsonArray array = new JsonArray();
        for (Type type : sortedTypes()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", type.getId());
            obj.addProperty("name", strip(type.getName()));
            obj.addProperty("displayed", type.isDisplayed());
            obj.addProperty("weapon", type.isWeapon());
            obj.addProperty("modifier_source", String.valueOf(type.getModifierSource()));

            @SuppressWarnings("deprecation")
            Type parent = type.getParent();
            if (parent == null) obj.add("parent", JsonNull.INSTANCE);
            else obj.addProperty("parent", parent.getId());

            ItemStack icon = type.getItem();
            if (icon == null) obj.add("display_material", JsonNull.INSTANCE);
            else obj.addProperty("display_material", icon.getType().name());

            JsonArray available = new JsonArray();
            List<String> ids = new ArrayList<>();
            for (ItemStat<?, ?> stat : type.getAvailableStats()) ids.add(stat.getId());
            ids.sort(Comparator.naturalOrder());
            ids.forEach(available::add);
            obj.add("available_stats", available);

            array.add(obj);
        }
        return array;
    }

    // ------------------------------------------------------------------ items

    private JsonArray items(boolean includeUntagged) {
        JsonArray array = new JsonArray();

        for (Type type : sortedTypes()) {
            List<MMOItemTemplate> templates = new ArrayList<>(MMOItems.plugin.getTemplates().getTemplates(type));
            templates.sort(Comparator.comparing(MMOItemTemplate::getId));

            ConfigurationSection root = null; // this type's YAML, read lazily (one disk read per type)
            for (MMOItemTemplate template : templates) {
                if (!includeUntagged && !isTagged(template)) continue;

                if (root == null) root = type.getConfigFile().getConfig();
                array.add(item(type, template, root.getConfigurationSection(template.getId())));
            }
        }
        return array;
    }

    private static boolean isTagged(MMOItemTemplate template) {
        RandomStatData<?> data = template.getBaseItemData().get(DynlairStat.INSTANCE);
        // Only a plain `true` counts; a numeric value would be a per-generation chance.
        return data instanceof RandomBooleanData bool && bool.getChance() >= 1.0;
    }

    private JsonObject item(Type type, MMOItemTemplate template, ConfigurationSection raw) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", type.getId());
        obj.addProperty("id", template.getId());
        obj.addProperty("revision_id", template.getRevisionId());

        JsonArray options = new JsonArray();
        for (MMOItemTemplate.TemplateOption option : MMOItemTemplate.TemplateOption.values())
            if (template.hasOption(option)) options.add(option.name());
        obj.add("options", options);

        JsonArray modifiers = new JsonArray();
        template.getModifiers().keySet().stream().sorted().forEach(modifiers::add);
        obj.add("modifier_ids", modifiers);

        // Structured stat list: registry metadata + the raw YAML value of each stat.
        ConfigurationSection base = raw == null ? null : raw.getConfigurationSection("base");
        List<ItemStat<?, ?>> stats = new ArrayList<>();
        for (ItemStat<?, ?> stat : template.getBaseItemData().keySet())
            if (stat != DynlairStat.INSTANCE) stats.add(stat);
        stats.sort(Comparator.comparing(ItemStat::getId));

        JsonArray statArray = new JsonArray();
        for (ItemStat<?, ?> stat : stats) {
            JsonElement value = YamlJson.toJson(base == null ? null : base.get(stat.getPath()));
            JsonObject s = new JsonObject();
            s.addProperty("id", stat.getId());
            s.addProperty("name", strip(stat.getName()));
            s.addProperty("config_path", stat.getPath());
            s.addProperty("kind", YamlJson.kind(value)); // JSON shape, not a stat-specific type
            s.add("value", value);
            statArray.add(s);
        }
        obj.add("stats", statArray);

        // Everything else (modifiers, options, anything not modeled above), untouched.
        obj.add("raw_yaml", YamlJson.toJson(raw));
        return obj;
    }

    // ---------------------------------------------------------------- helpers

    private static List<Type> sortedTypes() {
        List<Type> types = new ArrayList<>(MMOItems.plugin.getTypes().getAll());
        types.sort(Comparator.comparing(Type::getId));
        return types;
    }

    private static String strip(String text) {
        return text == null ? "" : ChatColor.stripColor(text);
    }
}
