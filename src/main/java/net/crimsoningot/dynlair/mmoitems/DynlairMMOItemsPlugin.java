package net.crimsoningot.dynlair.mmoitems;

import net.crimsoningot.dynlair.core.api.DynlairApi;
import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.plugin.java.JavaPlugin;

public final class DynlairMMOItemsPlugin extends JavaPlugin {

    @Override
    public void onLoad() {
        // Must happen here, not in onEnable(): MMOItems parses its item files in its own
        // onEnable(), and an unregistered `dynlair:` key would be logged and dropped.
        // `depend: [MMOItems]` guarantees MMOItems' onLoad() already ran.
        MMOItems.plugin.getStats().register(DynlairStat.INSTANCE);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        DynlairApi api = getServer().getServicesManager().load(DynlairApi.class);
        if (api == null) {
            getLogger().severe("DynlairCore did not provide its API; disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        api.registerSource(new MMOItemsSource(this));
    }

    @Override
    public void onDisable() {
        DynlairApi api = getServer().getServicesManager().load(DynlairApi.class);
        if (api != null) api.unregisterSource("mmoitems");
    }
}
