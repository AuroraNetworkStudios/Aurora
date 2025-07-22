package gg.auroramc.aurora.api.user;

import gg.auroramc.aurora.api.util.NamespacedId;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class UserLocalizationHolder extends UserDataHolder {
    private final AtomicReference<Locale> localization = new AtomicReference<>(Locale.ROOT);

    public Locale getLocalization() {
        return localization.get();
    }

    public void setLocalization(Locale localization) {
        this.localization.set(localization);
        dirty.set(true);
    }

    @Override
    public NamespacedId getId() {
        return NamespacedId.fromDefault("localization");
    }

    @Override
    public void serializeInto(ConfigurationSection data) {
        var locale = localization.get();
        if (locale != Locale.ROOT) {
            data.set("language", locale.toString().replace('_', '-'));
        } else {
            data.set("language", null);
        }
    }

    @Override
    public void initFrom(@Nullable ConfigurationSection data) {
        if (data == null) return;
        if (data.contains("language")) {
            setLocalization(Locale.forLanguageTag(data.getString("language")));
        }
    }
}
