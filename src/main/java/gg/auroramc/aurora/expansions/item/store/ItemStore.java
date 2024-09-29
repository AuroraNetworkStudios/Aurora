package gg.auroramc.aurora.expansions.item.store;

import com.google.common.collect.Maps;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.util.InventorySerializer;
import lombok.SneakyThrows;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.Map;

public class ItemStore {
    private final File file;
    private final YamlConfiguration data;

    private Map<String, ItemStack> items = Maps.newConcurrentMap();

    @SneakyThrows
    public ItemStore(String filename) {
        file = new File(Aurora.getInstance().getDataFolder(), filename);
        if (!file.exists()) {
            file.mkdirs();
            file.createNewFile();
        }
        data = YamlConfiguration.loadConfiguration(file);

        for(String key : data.getKeys(false)) {
            items.put(key, InventorySerializer.readItemStackFromBase64(data.getString(key)));
        }
    }

    public void addItem(String id, ItemStack item) {
        items.put(id, item.clone());
        data.set(id, InventorySerializer.writeItemStackToBase64(item));
    }

    public ItemStack getItem(String id) {
        var item = items.get(id);
        if (item != null) {
            return item.clone();
        } else {
            return null;
        }
    }

    public void removeItem(String id) {
        items.remove(id);
        data.set(id, null);
    }

    @SneakyThrows
    public void saveItems() {
        data.save(file);
    }
}
