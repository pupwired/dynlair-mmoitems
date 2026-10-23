package net.crimsoningot.dynlair.mmoitems;

import io.lumine.mythic.lib.api.item.ItemTag;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.stat.data.BooleanData;
import net.Indyuce.mmoitems.stat.type.BooleanStat;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Marker stat: {@code dynlair: true} under an item's {@code base:} section opts it in
 * to documentation. It is a regular {@link BooleanStat} (so it is editable in the
 * in-game item editor, which keeps the default BooleanStat toggle UI) with its side
 * effects on generated items switched off: no NBT tag and no lore line.
 */
public final class DynlairStat extends BooleanStat {

    public static final String ID = "DYNLAIR";
    public static final DynlairStat INSTANCE = new DynlairStat();

    private DynlairStat() {
        super(ID,
                Material.WRITABLE_BOOK,
                "Include in Dynlair",
                new String[]{"Include this item in generated documentation.",
                        "Left click to toggle."},
                new String[0]); // empty list = compatible with every item type
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull BooleanData data) {
        // Intentionally empty: generated items must not carry this stat.
    }

    @NotNull
    @Override
    public ArrayList<ItemTag> getAppliedNBT(@NotNull BooleanData data) {
        return new ArrayList<>();
    }
}
