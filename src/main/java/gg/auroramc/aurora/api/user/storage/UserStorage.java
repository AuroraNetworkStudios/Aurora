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

    /**
     * Serializes what has to be saved of the user right now, to be written by {@link PendingSave#write()} later, so
     * no lock of theirs is held while it's written. The default does all of it when writing.
     */
    default PendingSave prepareSave(AuroraUser user, SaveReason reason) {
        return () -> saveUser(user, reason);
    }
    int bulkSaveUsers(List<AuroraUser> users, SaveReason reason);
    void purgeUser(UUID uuid);
    void dispose();

    @FunctionalInterface
    interface PendingSave {
        boolean write();
    }
}
