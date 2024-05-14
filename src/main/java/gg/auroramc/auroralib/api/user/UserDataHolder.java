package gg.auroramc.auroralib.api.user;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public abstract class UserDataHolder implements DataHolder {
    private AuroraUser user;


    @Nullable
    public Player getPlayer() {
        return user.getPlayer();
    }

    @NotNull
    public OfflinePlayer getOfflinePlayer() {
        return user.getOfflinePlayer();
    }
}
