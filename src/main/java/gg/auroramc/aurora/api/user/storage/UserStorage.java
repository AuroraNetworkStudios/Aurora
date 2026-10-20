package gg.auroramc.aurora.api.user.storage;

import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.api.user.UserDataHolder;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public interface UserStorage {
    void loadUser(UUID uuid, Set<Class<? extends UserDataHolder>> dataHolders, Consumer<AuroraUser> handler);
    AuroraUser loadUser(UUID uuid, Set<Class<? extends UserDataHolder>> dataHolders);
    boolean saveUser(AuroraUser user, SaveReason reason);
    int bulkSaveUsers(List<AuroraUser> users, SaveReason reason);
    void purgeUser(UUID uuid);
    void dispose();

    // Changes for players whose data isn't loaded on the server that made the change
    boolean addChange(UUID uuid, String type, String data);
    List<PendingChange> getChanges(UUID uuid);
    boolean removeChanges(UUID uuid, List<PendingChange> changes);
    Set<UUID> getUsersWithChanges();

    // The same lock a server holds while a player is online there,
    // so only one server can change a player's data at a time
    boolean lockUser(UUID uuid);
    void unlockUser(UUID uuid);
}
