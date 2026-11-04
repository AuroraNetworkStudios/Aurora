package bench;

import gg.auroramc.aurora.api.events.user.AuroraUserLoadedEvent;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.reward.Reward;
import gg.auroramc.aurora.api.reward.RewardExecutor;
import gg.auroramc.aurora.api.util.ThreadSafety;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.function.Consumer;

// Checks that Aurora calls into plugins on the thread that owns the player (on Folia only that one may touch them)
public class BenchThreads implements Listener {
    private final Consumer<String> log;

    public BenchThreads(Consumer<String> log) {
        this.log = log;
    }

    @EventHandler
    public void onLoaded(AuroraUserLoadedEvent event) {
        var player = Bukkit.getPlayer(event.getUser().getUniqueId());
        if (player == null) return;
        log.accept("loadedevent name=" + player.getName() + " owned=" + Bukkit.isOwnedByCurrentRegion(player));
    }

    // A reward that throws, then one that reports its thread: the second must still run, and the future complete
    public void runRewards(Player player) {
        Reward throwing = reward(p -> {
            throw new IllegalStateException("bench reward failure");
        });
        Reward reporting = reward(p -> log.accept("reward name=" + p.getName() + " owned=" + Bukkit.isOwnedByCurrentRegion(p)));
        Bukkit.getAsyncScheduler().runNow(Bukkit.getPluginManager().getPlugin("AuroraBench"), t ->
                RewardExecutor.execute(List.of(throwing, reporting), player, 1, List.of())
                        .whenComplete((v, e) -> log.accept("rewards done name=" + player.getName() + " error=" + e)));
    }

    private static Reward reward(Consumer<Player> action) {
        return new Reward() {
            @Override
            public void execute(Player player, long level, List<Placeholder<?>> placeholders) {
                action.accept(player);
            }

            @Override
            public void init(ConfigurationSection args) {
            }

            @Override
            public String getDisplay(Player player, List<Placeholder<?>> placeholders) {
                return "";
            }

            @Override
            public ThreadSafety getThreadSafety() {
                return ThreadSafety.SYNC_ONLY;
            }
        };
    }
}
