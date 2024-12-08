package gg.auroramc.aurora.expansions.item.store;

import com.google.common.collect.Maps;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.util.InventorySerializer;
import lombok.SneakyThrows;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.io.File;
import java.util.Map;

public class ItemStore {
    private final File file;
    private final YamlConfiguration data;

    private final Map<String, ItemStack> items = Maps.newConcurrentMap();
    private final Map<Integer, String> hashToIdMap = Maps.newHashMap();

    @SneakyThrows
    public ItemStore(String filename) {

        file = new File(Aurora.getInstance().getDataFolder(), filename);
        if (!file.exists()) {
            file.createNewFile();
        }
        data = YamlConfiguration.loadConfiguration(file);

        for (String key : data.getKeys(false)) {
            ItemStack item = InventorySerializer.readItemStackFromBase64(data.getString(key));
            items.put(key, item);
            hashToIdMap.put(generateHash(item), key);
        }
    }

    public void addItem(String id, ItemStack item) {
        items.put(id, item.clone());
        data.set(id, InventorySerializer.writeItemStackToBase64(item));
        hashToIdMap.put(generateHash(item), id);
    }

    public ItemStack getItem(String id) {
        var item = items.get(id);
        return item != null ? item.clone() : null;
    }

    public String getIdFromItem(ItemStack item) {
        return hashToIdMap.get(generateHash(item));
    }

    public void removeItem(String id) {
        ItemStack item = items.remove(id);
        hashToIdMap.remove(generateHash(item));
        data.set(id, null);
    }

    @SneakyThrows
    public void saveItems() {
        data.save(file);
    }

    private int generateHash(ItemStack item) {
        if (item.getAmount() == 1) {
            return item.hashCode();
        }
        // Cloning is extremely cheap in 1.21+
        var clonedItem = item.clone();
        clonedItem.setAmount(1);
        return clonedItem.hashCode();
    }
}