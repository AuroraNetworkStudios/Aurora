package gg.auroramc.aurora.api.user.storage;

import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.api.user.UserDataHolder;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public interface UserStorage {
    void loadUser(UUID uuid, Set<Class<? extends UserDataHolder>> dataHolders, Consumer<AuroraUser> handler);
    AuroraUser loadUser(UUID uuid, Set<Class<? extends UserDataHolder>> dataHolders);
    boolean saveUser(AuroraUser user, SaveReason reason);
    void purgeUser(UUID uuid);
}
