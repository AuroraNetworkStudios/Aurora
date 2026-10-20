package gg.auroramc.aurora.api.user.change;

import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.api.util.NamespacedId;
import org.bukkit.configuration.ConfigurationSection;

public class StashClearChange implements UserChange {
    public static final NamespacedId TYPE = NamespacedId.fromDefault("stash_clear");

    @Override
    public NamespacedId getType() {
        return TYPE;
    }

    @Override
    public void serializeInto(ConfigurationSection data) {
    }

    @Override
    public boolean apply(AuroraUser user) {
        return user.getStashData().clear();
    }
}
