package gg.auroramc.aurora.api.user;

import gg.auroramc.aurora.api.util.NamespacedId;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserMetaHolder extends UserDataHolder {
    private final Map<String, Object> meta = new ConcurrentHashMap<>();

    @Override
    public NamespacedId getId() {
        return NamespacedId.fromDefault("meta");
    }

    @Override
    public void serializeInto(ConfigurationSection data) {
        for (var key : data.getKeys(false)) {
            data.set(key, null);
        }
        for (var entry : meta.entrySet()) {
            data.set(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void initFrom(@Nullable ConfigurationSection data) {
        if (data == null) return;
        for (var key : data.getKeys(false)) {
            meta.put(key, data.get(key));
        }
    }

    public Object getMeta(String key) {
        return meta.get(key);
    }

    public Double getMeta(String key, double def) {
        var m = meta.get(key);
        return m == null ? def : (double) m;
    }

    public String getMeta(String key, String def) {
        var m = meta.get(key);
        return m == null ? def : m.toString();
    }

    public boolean setMeta(String key, double value) {
        if (!getUser().isLoaded()) return false;
        meta.put(key, value);
        dirty.set(true);
        return true;
    }

    public boolean setMeta(String key, String value) {
        if (!getUser().isLoaded()) return false;
        meta.put(key, value);
        dirty.set(true);
        return true;
    }

    public boolean removeMeta(String key) {
        if (!getUser().isLoaded()) return false;
        meta.remove(key);
        dirty.set(true);
        return true;
    }

    public boolean incrementMeta(String key, Double value) {
        if (!getUser().isLoaded()) return false;
        meta.put(key, getMeta(key, 0) + value);
        dirty.set(true);
        return true;
    }

    public boolean decrementMeta(String key, Double value) {
        return decrementMeta(key, value, false);
    }

    public boolean decrementMeta(String key, Double value, boolean allowNegative) {
        if (!getUser().isLoaded()) return false;
        if (allowNegative) {
            meta.put(key, getMeta(key, 0) - value);
        } else {
            double newValue = getMeta(key, 0) - value;
            if (newValue < 0) newValue = 0;
            meta.put(key, newValue);
        }
        dirty.set(true);
        return true;
    }

    public List<String> getMetaKeys() {
        return List.copyOf(meta.keySet());
    }
}
