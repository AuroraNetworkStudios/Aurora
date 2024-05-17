package gg.auroramc.aurora.api.user;

import gg.auroramc.aurora.api.util.NamespacedId;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

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

    public Long getMeta(String key, long def) {
        return (long) meta.getOrDefault(key, def);
    }

    public Double getMeta(String key, double def) {
        return (double) meta.getOrDefault(key, def);
    }

    public String getMeta(String key, String def) {
        if (def == null) return (String) meta.get(key);
        return (String) meta.getOrDefault(key, def);
    }

    public void setMeta(String key, long value) {
        meta.put(key, value);
        dirty.set(true);
    }

    public void setMeta(String key, double value) {
        meta.put(key, value);
        dirty.set(true);
    }

    public void setMeta(String key, String value) {
        meta.put(key, value);
        dirty.set(true);
    }

    public void incrementMeta(String key, Number value) {
        meta.put(key, getMeta(key, 0).doubleValue() + value.doubleValue());
        dirty.set(true);
    }

    public void decrementMeta(String key, Number value) {
        decrementMeta(key, value, false);
    }

    public void decrementMeta(String key, Number value, boolean allowNegative) {
        if (allowNegative) {
            meta.put(key, getMeta(key, 0).doubleValue() - value.doubleValue());
        } else {
            double newValue = getMeta(key, 0).doubleValue() - value.doubleValue();
            if (newValue < 0) newValue = 0;
            meta.put(key, newValue);
        }
        dirty.set(true);
    }
}
