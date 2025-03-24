package gg.auroramc.aurora.expansions.item.resolvers;

import com.willfp.eco.core.items.Items;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.item.ItemResolver;
import gg.auroramc.aurora.api.item.TypeId;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class EcoItemsResolver implements ItemResolver {
    private final NamespacedKey ecoitemsKey = new NamespacedKey("ecoitems", "item");
    private final NamespacedKey ecoarmorKey = new NamespacedKey("ecoarmor", "set");
    // 0 if it isn't advanced
    private final NamespacedKey ecoarmorAdvancedKey = new NamespacedKey("ecoarmor", "advanced");
    private final NamespacedKey ecoarmorShardKey = new NamespacedKey("ecoarmor", "advancement-shard");
    private final NamespacedKey ecoarmorUpgradeCrystalKey = new NamespacedKey("ecoarmor", "upgrade_crystal");
    private final NamespacedKey talismansKey = new NamespacedKey("talismans", "talisman");
    private final NamespacedKey ecopetsKey = new NamespacedKey("ecopets", "pet_egg");
    private final NamespacedKey reforgesKey = new NamespacedKey("reforges", "reforge_stone");
    private final NamespacedKey ecoscrollsKey = new NamespacedKey("ecoscrolls", "scroll");
    private final NamespacedKey ecocratesKey = new NamespacedKey("ecocrates", "key");
    private final NamespacedKey stattrackersKey = new NamespacedKey("stattrackers", "stat_tracker");

    @Override
    public boolean matches(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        return isEcoItem(item.getItemMeta());
    }

    @Override
    public TypeId resolveId(ItemStack item) {
        return resolveEcoItemId(item, item.getItemMeta());
    }

    @Override
    public TypeId oneStepMatch(ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        if (isEcoItem(meta)) {
            return resolveEcoItemId(item, meta);
        }
        return null;
    }

    @Override
    public ItemStack resolveItem(String id, @Nullable Player player) {
        return Items.lookup(id).getItem();
    }

    private boolean isEcoItem(ItemMeta item) {
        // Check every single pdc key
        // This is so dumb
        var pdc = item.getPersistentDataContainer();

        return pdc.has(ecoitemsKey) ||
                pdc.has(ecoarmorKey) ||
                pdc.has(talismansKey) ||
                pdc.has(ecopetsKey) ||
                pdc.has(reforgesKey) ||
                pdc.has(ecoscrollsKey) ||
                pdc.has(ecocratesKey) ||
                pdc.has(ecoarmorShardKey) ||
                pdc.has(ecoarmorUpgradeCrystalKey) ||
                pdc.has(stattrackersKey);
    }

    private TypeId resolveEcoItemId(ItemStack item, ItemMeta meta) {
        // Get the key for the matching pdc key
        var pdc = meta.getPersistentDataContainer();
        var type = PersistentDataType.STRING;

        if (pdc.has(ecoitemsKey)) {
            return new TypeId("eco", "ecoitems:" + pdc.get(ecoitemsKey, type));
        } else if (pdc.has(ecoarmorKey)) {
            var slot = parseArmorSlot(item);
            if (pdc.has(ecoarmorAdvancedKey) && pdc.get(ecoarmorAdvancedKey, PersistentDataType.INTEGER) != 0) {
                return new TypeId("eco", "ecoarmor:set_" + pdc.get(ecoarmorKey, type) + "_" + slot + "_advanced");
            }
            return new TypeId("eco", "ecoarmor:set_" + pdc.get(ecoarmorKey, type) + "_" + slot);
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
        } else if (pdc.has(ecoarmorShardKey)) {
            return new TypeId("eco", "ecoarmor:shard_" + pdc.get(ecoarmorShardKey, type));
        } else if (pdc.has(ecoarmorUpgradeCrystalKey)) {
            return new TypeId("eco", "ecoarmor:upgrade_crystal_" + pdc.get(ecoarmorUpgradeCrystalKey, type));
        } else if (pdc.has(stattrackersKey)) {
            return new TypeId("eco", "stattrackers:" + pdc.get(stattrackersKey, type));
        }
        return null;
    }

    private String parseArmorSlot(ItemStack item) {
        return ArmorSlot.getSlot(item).name().toLowerCase(Locale.getDefault());
    }


    public enum ArmorSlot {
        HELMET(EquipmentSlot.HEAD),
        CHESTPLATE(EquipmentSlot.CHEST),
        ELYTRA(EquipmentSlot.CHEST),
        LEGGINGS(EquipmentSlot.LEGS),
        BOOTS(EquipmentSlot.FEET);

        private final EquipmentSlot slot;

        ArmorSlot(EquipmentSlot slot) {
            this.slot = slot;
        }

        public EquipmentSlot getSlot() {
            return slot;
        }

        public static ArmorSlot getSlot(ItemStack itemStack) {
            if (itemStack == null) {
                Aurora.logger().severe("Failed to parse armor slot for null item, returning null.");
                return null;
            }
            String materialName = itemStack.getType().name();
            return getSlot(materialName);
        }

        public static ArmorSlot getSlot(String name) {
            if (name.contains("HELMET") || name.contains("HEAD") || name.contains("SKULL") || name.contains("PUMPKIN")) {
                return HELMET;
            } else if (name.contains("CHESTPLATE")) {
                return CHESTPLATE;
            } else if (name.contains("ELYTRA")) {
                return ELYTRA;
            } else if (name.contains("LEGGINGS")) {
                return LEGGINGS;
            } else if (name.contains("BOOTS")) {
                return BOOTS;
            } else {
                Aurora.logger().severe("Failed to parse armor slot for item: " + name + " returning HELMET instead.");
                return HELMET;
            }
        }
    }
}
