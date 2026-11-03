package gg.auroramc.aurora.api.reward;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.util.ThreadSafety;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RewardExecutor {
    /**
     * Runs the rewards for the player. The ones that have to run on the main thread run on the player's own thread
     * (their region's thread on Folia), the others off it. One reward failing doesn't stop the rest.
     *
     * @return completes when every reward ran, also if some of them failed
     */
    public static CompletableFuture<Void> execute(List<Reward> rewards, Player player, long level, List<Placeholder<?>> placeholders) {
        List<Reward> sync = new ArrayList<>();
        List<Reward> async = new ArrayList<>();

        for (Reward reward : rewards) {
            if (threadSafety(reward) == ThreadSafety.SYNC_ONLY) {
                sync.add(reward);
            } else {
                async.add(reward);
            }
        }

        CompletableFuture<Void> syncFuture = new CompletableFuture<>();
        CompletableFuture<Void> asyncFuture = new CompletableFuture<>();

        if (!sync.isEmpty()) {
            Runnable runSync = () -> {
                runAll(sync, player, level, placeholders);
                syncFuture.complete(null);
            };
            // If they left before it runs, the rewards still run (a command reward works for an offline player too),
            // on the global thread like before
            Runnable whenLeft = () -> Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(), (task) -> runSync.run());
            var scheduled = player.getScheduler().run(Aurora.getInstance(), (task) -> runSync.run(), whenLeft);
            // Null if they had left already, then neither runs
            if (scheduled == null) whenLeft.run();
        } else {
            syncFuture.complete(null);
        }

        if (!async.isEmpty()) {
            Bukkit.getAsyncScheduler().runNow(Aurora.getInstance(), (task) -> {
                runAll(async, player, level, placeholders);
                asyncFuture.complete(null);
            });
        } else {
            asyncFuture.complete(null);
        }

        return CompletableFuture.allOf(syncFuture, asyncFuture);
    }

    private static ThreadSafety threadSafety(Reward reward) {
        try {
            return reward.getThreadSafety();
        } catch (Exception e) {
            // Run it where it's always safe
            Aurora.logger().warning("Failed to check where the reward " + reward.getClass().getSimpleName() + " can run, running it on the player's thread: " + e);
            return ThreadSafety.SYNC_ONLY;
        }
    }

    private static void runAll(List<Reward> rewards, Player player, long level, List<Placeholder<?>> placeholders) {
        for (Reward reward : rewards) {
            try {
                reward.execute(player, level, placeholders);
            } catch (Throwable e) {
                Aurora.logger().severe("Failed to give the reward " + reward.getClass().getSimpleName() + " to " + player.getName() + ": " + e);
                e.printStackTrace();
            }
        }
    }
}
