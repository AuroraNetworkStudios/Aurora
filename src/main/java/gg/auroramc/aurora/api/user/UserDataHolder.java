package gg.auroramc.aurora.api.user;

import gg.auroramc.aurora.Aurora;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class UserDataHolder implements DataHolder {
    protected AtomicReference<UUID> uuid = new AtomicReference<>();
    protected final AtomicBoolean dirty = new AtomicBoolean(false);

    public void setUuid(UUID uuid) {
        this.uuid.set(uuid);
    }

    public UUID getUniqueId() {
        return uuid.get();
    }

    public AuroraUser getUser() {
        return Aurora.getUserManager().getUser(uuid.get());
    }

    @Nullable
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid.get());
    }

    @NotNull
    public OfflinePlayer getOfflinePlayer() {
        return Bukkit.getOfflinePlayer(uuid.get());
    }

    @Override
    public boolean isDirty() {
        return dirty.get();
    }

    public void setDirty(boolean dirty) {
        this.dirty.set(dirty);
    }
}
