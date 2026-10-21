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
 * A {@link ServerNotifier} that sends plugin messages through the proxy (BungeeCord or Velocity).
 * A message can't be sent if nobody is online on this server, since plugin messages go through a player's connection.
 */
class ProxyNotifier implements ServerNotifier, PluginMessageListener {
    private static final String CHANNEL = "BungeeCord";
    private static final String CHANGES = "AuroraUserChanges";
    private static final String ANSWERS = "AuroraChangeAnswers";
    private final Consumer<UUID> onChanges;
    private final Runnable onAnswers;

    ProxyNotifier(Consumer<UUID> onChanges, Runnable onAnswers) {
        this.onChanges = onChanges;
        this.onAnswers = onAnswers;
        Bukkit.getMessenger().registerOutgoingPluginChannel(Aurora.getInstance(), CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(Aurora.getInstance(), CHANNEL, this);
    }

    @Override
    public void notifyChanges(UUID uuid) {
        send(CHANGES, uuid.toString());
    }

    @Override
    public void notifyAnswers() {
        send(ANSWERS, "");
    }

    private void send(String subchannel, String message) {
        var player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (player == null) return;

        var payload = message.getBytes(StandardCharsets.UTF_8);
        var out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ONLINE"); // All servers with players on them, except this one
        out.writeUTF(subchannel);
        out.writeShort(payload.length);
        out.write(payload);
        player.sendPluginMessage(Aurora.getInstance(), CHANNEL, out.toByteArray());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) return;
        try {
            var in = ByteStreams.newDataInput(message);
            var subchannel = in.readUTF();
            if (!subchannel.equals(CHANGES) && !subchannel.equals(ANSWERS)) return;
            var payload = new byte[in.readShort()];
            in.readFully(payload);
            if (subchannel.equals(CHANGES)) {
                onChanges.accept(UUID.fromString(new String(payload, StandardCharsets.UTF_8)));
            } else {
                onAnswers.run();
            }
        } catch (Exception e) {
            Aurora.logger().debug("Invalid Aurora proxy message: " + e.getMessage());
        }
    }
}
