package gg.auroramc.aurora.api.user.storage;

/**
 * A stored {@link gg.auroramc.aurora.api.user.change.UserChange} that wasn't applied yet.
 *
 * @param data     the change serialized as yaml
 * @param awaiting whether a server waits for the answer, see {@link gg.auroramc.aurora.api.user.UserManager#requestChange}
 */
public record PendingChange(long id, String type, String data, boolean awaiting) {
}
