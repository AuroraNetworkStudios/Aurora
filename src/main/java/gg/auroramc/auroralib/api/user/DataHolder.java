package gg.auroramc.auroralib.api.user;

import org.bukkit.configuration.ConfigurationSection;

public interface DataHolder {
    String getId();
    void serializeInto(ConfigurationSection data);
    void initFrom(ConfigurationSection data);
}
