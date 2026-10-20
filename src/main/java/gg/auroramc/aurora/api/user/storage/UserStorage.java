package gg.auroramc.aurora.api.user.storage;

import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.api.user.UserDataHolder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    // Returns the id of the change, or -1 if it couldn't be stored
    long addChange(UUID uuid, String type, String data, boolean awaiting);
    List<PendingChange> getChanges(UUID uuid);
    Set<UUID> getUsersWithChanges();
    // Removes the applied changes, and stores the answers for the servers waiting for them
    boolean completeChanges(UUID uuid, List<PendingChange> applied, Map<Long, Boolean> answers);
    // Returns and removes the answers that are there already
    Map<Long, Boolean> takeAnswers(Collection<Long> ids);
    // Removes the change if it wasn't applied yet
    boolean cancelChange(long id);

    // The same lock a server holds while a player is online there,
    // so only one server can change a player's data at a time
    boolean lockUser(UUID uuid);
    void unlockUser(UUID uuid);
}
