package gg.auroramc.aurora.expansions.item.resolvers;

import com.willfp.eco.core.items.Items;
import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public class EcoItemsResolver implements ItemResolver {
    private final NamespacedKey ecoitemsKey = new NamespacedKey("ecoitems", "item");
    private final NamespacedKey ecoarmorKey = new NamespacedKey("ecoarmor", "set");
    // 0 if it isn't advanced
    private final NamespacedKey ecoarmorAdvancedKey = new NamespacedKey("ecoarmor", "advanced");
    private final NamespacedKey talismansKey = new NamespacedKey("talismans", "talisman");
    private final NamespacedKey ecopetsKey = new NamespacedKey("ecopets", "pet_egg");
    private final NamespacedKey reforgesKey = new NamespacedKey("reforges", "reforge_stone");
    private final NamespacedKey ecoscrollsKey = new NamespacedKey("ecoscrolls", "scroll");
    private final NamespacedKey ecocratesKey = new NamespacedKey("ecocrates", "key");
    // TODO
    //private final NamespacedKey stattrackersKey = new NamespacedKey("stattrackers", "tracker");

    @Override
    public boolean matches(ItemStack item) {
        return isEcoItem(item);
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        return resolveEcoItemId(item);
    }

    @Override
    public ItemStack resolveItem(String id, @Nullable Player player) {
        return Items.lookup(id).getItem();
    }

    private boolean isEcoItem(ItemStack item) {
        // Check every single pdc key
        // This is so dumb
        return item.getPersistentDataContainer().has(ecoitemsKey) ||
                item.getPersistentDataContainer().has(ecoarmorKey) ||
                item.getPersistentDataContainer().has(talismansKey) ||
                item.getPersistentDataContainer().has(ecopetsKey) ||
                item.getPersistentDataContainer().has(reforgesKey) ||
                item.getPersistentDataContainer().has(ecoscrollsKey) ||
                item.getPersistentDataContainer().has(ecocratesKey);
    }

    private TypeId resolveEcoItemId(ItemStack item) {
        // Get the key for the matching pdc key
        var pdc = item.getPersistentDataContainer();
        var type = PersistentDataType.STRING;

        if (pdc.has(ecoitemsKey)) {
            return new TypeId("eco", "ecoitems:" + pdc.get(ecoitemsKey, type));
        } else if (pdc.has(ecoarmorKey)) {
            // Lord have mercy, I can't figure out the <slot>, so for now we use the dumb method and pray to god
            var customItem = Items.getCustomItem(item);
            if (customItem != null) {
                return new TypeId("eco", customItem.getKey().toString());
            }
            //if(pdc.has(ecoarmorAdvancedKey) && pdc.get(ecoarmorAdvancedKey, PersistentDataType.INTEGER) != 0) {
            //    return new TypeId("eco", "ecoarmor:set_" + pdc.get(ecoarmorKey, type) + "_slot_" + "_advanced");
            //}
            //return new TypeId("eco", "ecoarmor:set_" + pdc.get(ecoarmorKey, type) + "_slot");
        } else if (pdc.has(talismansKey)) {
            return new TypeId("eco", "talismans:" + pdc.get(talismansKey, type));
        } else if (pdc.has(ecopetsKey)) {
            return new TypeId("eco", "ecopets:" + pdc.get(ecopetsKey, type) + "_spawn_egg");
        } else if (pdc.has(reforgesKey)) {
            return new TypeId("eco", "reforges:stone_" + pdc.get(reforgesKey, type));
        } else if (pdc.has(ecoscrollsKey)) {
            return new TypeId("eco", "ecoscrolls:scroll_" + pdc.get(ecoscrollsKey, type));
        } else if (pdc.has(ecocratesKey)) {
            return new TypeId("eco", "ecocrates:" + pdc.get(ecocratesKey, type) + "_key");
        }
        return null;
    }
}
