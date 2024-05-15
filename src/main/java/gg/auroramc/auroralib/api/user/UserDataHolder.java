package gg.auroramc.auroralib.api.user;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class UserDataHolder implements DataHolder {
    protected final AtomicReference<AuroraUser> user = new AtomicReference<>();
    protected final AtomicBoolean dirty = new AtomicBoolean(false);

    public void setUser(AuroraUser user) {
        this.user.set(user);
    }

    public AuroraUser getUser() {
        return user.get();
    }

    @Nullable
    public Player getPlayer() {
        return user.get().getPlayer();
    }

    @NotNull
    public OfflinePlayer getOfflinePlayer() {
        return user.get().getOfflinePlayer();
    }

    @Override
    public boolean isDirty() {
        return dirty.get();
    }
}
