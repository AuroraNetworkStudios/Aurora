package gg.auroramc.aurora.api.user.storage;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.user.AuroraUser;
import gg.auroramc.aurora.api.user.UserDataHolder;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class YamlStorage implements UserStorage {
    // Only this server uses these files, so changes are applied within a second (and on shutdown), no need to save them
    private final Map<UUID, List<PendingChange>> changes = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> answers = new ConcurrentHashMap<>();
    private final AtomicLong nextChangeId = new AtomicLong();

    public DirectoryStream<Path> getFileStream() throws IOException {
        var file = new File(Aurora.getInstance().getDataFolder() + "/userdata");
        return Files.newDirectoryStream(file.toPath(), "*.yml");
    }


    @Override
    public void loadUser(UUID uuid, Set<Class<? extends UserDataHolder>> dataHolders, Consumer<AuroraUser> handler) {
        final var start = System.nanoTime();
        var file = new File(Aurora.getInstance().getDataFolder() + "/userdata", uuid + ".yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                Aurora.logger().severe("Failed to create data file for player: " + uuid);
            }
        }
        var data = YamlConfiguration.loadConfiguration(file);
        var user = new AuroraUser(uuid);
        user.initData(data, dataHolders);
        final var end = System.nanoTime();
        Aurora.getUserManager().getLoadLatencyMeasure().addLatency(end - start);
        handler.accept(user);
    }

    @Override
    public AuroraUser loadUser(UUID uuid, Set<Class<? extends UserDataHolder>> dataHolders) {
        var file = new File(Aurora.getInstance().getDataFolder() + "/userdata", uuid + ".yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                Aurora.logger().severe("Failed to create data file for player: " + uuid);
            }
        }
        var data = YamlConfiguration.loadConfiguration(file);
        var user = new AuroraUser(uuid);
        user.initData(data, dataHolders);
        return user;
    }

    @Override
    public synchronized boolean saveUser(AuroraUser user, SaveReason reason) {
        var file = new File(Aurora.getInstance().getDataFolder() + "/userdata", user.getUniqueId() + ".yml");

        try {
            final var start = System.nanoTime();
            user.serializeData().save(file);
            final var end = System.nanoTime();
            Aurora.getUserManager().getSaveLatencyMeasure().addLatency(end - start);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    @Override
    public int bulkSaveUsers(List<AuroraUser> users, SaveReason reason) {
        int saved = 0;
        for (var user : users) {
            var success = saveUser(user, reason);
            if (success) saved++;
        }
        return saved;
    }

    @Override
    public void purgeUser(UUID uuid) {
        var file = new File(Aurora.getInstance().getDataFolder() + "/userdata", uuid + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public void dispose() {
        // nothing to dispose
    }

    @Override
    public long addChange(UUID uuid, String type, String data, boolean awaiting) {
        var id = nextChangeId.incrementAndGet();
        changes.computeIfAbsent(uuid, k -> new CopyOnWriteArrayList<>()).add(new PendingChange(id, type, data, awaiting));
        return id;
    }

    @Override
    public List<PendingChange> getChanges(UUID uuid) {
        return List.copyOf(changes.getOrDefault(uuid, List.of()));
    }

    @Override
    public Set<UUID> getUsersWithChanges() {
        return Set.copyOf(changes.keySet());
    }

    @Override
    public boolean completeChanges(UUID uuid, List<PendingChange> applied, Map<Long, Boolean> answers) {
        changes.computeIfPresent(uuid, (k, list) -> {
            list.removeIf(change -> applied.contains(change) || answers.containsKey(change.id()));
            return list.isEmpty() ? null : list;
        });
        this.answers.putAll(answers);
        return true;
    }

    @Override
    public Map<Long, Boolean> takeAnswers(Collection<Long> ids) {
        var taken = new HashMap<Long, Boolean>();
        for (var id : ids) {
            var answer = answers.remove(id);
            if (answer != null) taken.put(id, answer);
        }
        return taken;
    }

    @Override
    public boolean cancelChange(long id) {
        for (var entry : changes.entrySet()) {
            var change = entry.getValue().stream().filter(c -> c.id() == id).findFirst();
            if (change.isPresent()) return completeChanges(entry.getKey(), List.of(change.get()), Map.of());
        }
        return false;
    }

    @Override
    public boolean lockUser(UUID uuid) {
        // Only one server uses these files, the UserManager's player lock is enough
        return true;
    }

    @Override
    public void unlockUser(UUID uuid) {
    }
}
