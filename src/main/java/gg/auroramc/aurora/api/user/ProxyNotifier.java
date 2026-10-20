package gg.auroramc.aurora.api.user;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
import gg.auroramc.aurora.Aurora;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Tells the other servers through the proxy (BungeeCord or Velocity) that a player has new pending changes,
 * so the server they are online on applies them right away, instead of with its next check within a second.
 * <p>
 * It's only a wake-up call, the changes themselves are in the storage. A message can't be sent if nobody is
 * online on this server (plugin messages go through a player's connection), the checks still pick those up.
 */
class ProxyNotifier implements PluginMessageListener {
    private static final String CHANNEL = "BungeeCord";
    private static final String SUBCHANNEL = "AuroraUserChanges";
    private final Consumer<UUID> onNotified;

    ProxyNotifier(Consumer<UUID> onNotified) {
        this.onNotified = onNotified;
        Bukkit.getMessenger().registerOutgoingPluginChannel(Aurora.getInstance(), CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(Aurora.getInstance(), CHANNEL, this);
    }

    void notifyChanges(UUID uuid) {
        var player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (player == null) return;

        var payload = uuid.toString().getBytes(StandardCharsets.UTF_8);
        var out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ONLINE"); // All servers with players on them, except this one
        out.writeUTF(SUBCHANNEL);
        out.writeShort(payload.length);
        out.write(payload);
        player.sendPluginMessage(Aurora.getInstance(), CHANNEL, out.toByteArray());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) return;
        try {
            var in = ByteStreams.newDataInput(message);
            if (!in.readUTF().equals(SUBCHANNEL)) return;
            var payload = new byte[in.readShort()];
            in.readFully(payload);
            onNotified.accept(UUID.fromString(new String(payload, StandardCharsets.UTF_8)));
        } catch (Exception e) {
            Aurora.logger().debug("Invalid " + SUBCHANNEL + " message: " + e.getMessage());
        }
    }
}
