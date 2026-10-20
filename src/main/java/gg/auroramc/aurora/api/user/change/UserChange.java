package gg.auroramc.aurora.api.user.change;

import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.api.util.NamespacedId;
import org.bukkit.configuration.ConfigurationSection;

/**
 * A change to a player's data, that works even if they are offline or online on another server.
 * See {@link gg.auroramc.aurora.api.user.UserManager#applyChange(java.util.UUID, UserChange)}.
 * <p>
 * It can be stored in the database like a data holder, so its type must be registered with
 * {@link gg.auroramc.aurora.api.user.UserManager#registerChangeType} to be able to read it back.
 */
public interface UserChange {
    NamespacedId getType();

    void serializeInto(ConfigurationSection data);

    void apply(AuroraUser user);
}
