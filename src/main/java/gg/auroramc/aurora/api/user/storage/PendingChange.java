package gg.auroramc.aurora.api.user.storage;

/**
 * A stored {@link gg.auroramc.aurora.api.user.change.UserChange} that wasn't applied yet.
 *
 * @param data the change serialized as yaml
 */
public record PendingChange(long id, String type, String data) {
}
