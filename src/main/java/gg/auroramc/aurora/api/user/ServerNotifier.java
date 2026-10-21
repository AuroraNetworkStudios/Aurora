package gg.auroramc.aurora.api.user;

import java.util.UUID;

/**
 * Tells the other servers that a player has new pending changes, so the server they are online on applies them
 * right away instead of with its next check within a second. And that requests were answered, so the servers
 * waiting for answers don't have to wait for their next check either.
 * <p>
 * It's only a wake-up call, the changes and answers themselves are in the storage. If a message gets lost,
 * the checks still pick them up.
 */
interface ServerNotifier {
    void notifyChanges(UUID uuid);

    void notifyAnswers();

    default void close() {
    }
}
