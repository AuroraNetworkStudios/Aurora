package gg.auroramc.aurora.api.user;

import gg.auroramc.aurora.Aurora;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.bukkit.configuration.ConfigurationSection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * A {@link ServerNotifier} that uses Redis pub/sub. Unlike the proxy, it works when nobody is online on this server,
 * and messages don't wait for a server tick.
 */
class RedisNotifier implements ServerNotifier {
    private static final String CHANNEL = "aurora:user-changes";
    // To skip our own messages
    private final String serverId = UUID.randomUUID().toString();
    private final JedisPool pool;
    private final Consumer<UUID> onChanges;
    private final Runnable onAnswers;
    // Publishing waits for Redis, so it's done here instead of on the threads changing user data
    private final ExecutorService publisher = Executors.newSingleThreadExecutor(r -> new Thread(r, "Aurora Redis publisher"));
    private final Thread listener = new Thread(this::listen, "Aurora Redis listener");
    private volatile JedisPubSub subscription;
    private volatile boolean connected = false;
    private volatile boolean closed = false;

    RedisNotifier(ConfigurationSection config, Consumer<UUID> onChanges, Runnable onAnswers) {
        this.onChanges = onChanges;
        this.onAnswers = onAnswers;
        var clientConfig = DefaultJedisClientConfig.builder()
                .user(emptyToNull(config.getString("username")))
                .password(emptyToNull(config.getString("password")))
                .ssl(config.getBoolean("ssl", false))
                .timeoutMillis(2000)
                .build();
        var hostAndPort = new HostAndPort(config.getString("host", "127.0.0.1"), config.getInt("port", 6379));
        this.pool = new JedisPool(new GenericObjectPoolConfig<Jedis>(), hostAndPort, clientConfig);
        listener.setDaemon(true);
        listener.start();
    }

    @Override
    public void notifyChanges(UUID uuid) {
        publish("changes " + uuid);
    }

    @Override
    public void notifyAnswers() {
        publish("answers");
    }

    private void publish(String message) {
        // While Redis is down, the checks pick everything up anyway
        if (!connected) return;
        publisher.execute(() -> {
            try (var jedis = pool.getResource()) {
                jedis.publish(CHANNEL, serverId + " " + message);
            } catch (Exception e) {
                Aurora.logger().debug("Failed to publish to Redis: " + e.getMessage());
            }
        });
    }

    private void listen() {
        var firstTry = true;
        while (!closed) {
            try (var jedis = pool.getResource()) {
                subscription = new JedisPubSub() {
                    @Override
                    public void onSubscribe(String channel, int subscribedChannels) {
                        connected = true;
                        Aurora.logger().info("Connected to Redis, other servers are told about user changes through it");
                    }

                    @Override
                    public void onMessage(String channel, String message) {
                        handle(message);
                    }
                };
                // Blocks until we unsubscribe or the connection is lost
                jedis.subscribe(subscription, CHANNEL);
            } catch (Exception e) {
                if (closed) return;
                if (connected || firstTry) Aurora.logger().warning("Can't reach Redis, trying again every 5 seconds: " + e.getMessage());
                else Aurora.logger().debug("Failed to connect to Redis: " + e.getMessage());
            }
            firstTry = false;
            connected = false;
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void handle(String message) {
        var parts = message.split(" ");
        if (parts.length < 2 || parts[0].equals(serverId)) return;
        try {
            if (parts[1].equals("changes") && parts.length == 3) {
                onChanges.accept(UUID.fromString(parts[2]));
            } else if (parts[1].equals("answers")) {
                onAnswers.run();
            }
        } catch (Exception e) {
            Aurora.logger().debug("Invalid Redis message: " + message);
        }
    }

    @Override
    public void close() {
        closed = true;
        var current = subscription;
        if (current != null && current.isSubscribed()) current.unsubscribe();
        listener.interrupt();
        publisher.shutdown();
        pool.close();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
