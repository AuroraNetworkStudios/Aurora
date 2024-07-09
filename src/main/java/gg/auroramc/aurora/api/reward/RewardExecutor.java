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
    public static CompletableFuture<Void> execute(List<Reward> rewards, Player player, long level, List<Placeholder<?>> placeholders) {
        List<Reward> sync = new ArrayList<>();
        List<Reward> async = new ArrayList<>();

        for (Reward reward : rewards) {
            if (reward.getThreadSafety() == ThreadSafety.SYNC_ONLY) {
                sync.add(reward);
            } else {
                async.add(reward);
            }
        }

        CompletableFuture<Void> syncFuture = new CompletableFuture<>();
        CompletableFuture<Void> asyncFuture = new CompletableFuture<>();

        if (!sync.isEmpty()) {
            Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(), (task) -> {
                for (Reward reward : sync) {
                    reward.execute(player, level, placeholders);
                }
                syncFuture.complete(null);
            });
        } else {
            syncFuture.complete(null);
        }

        if (!async.isEmpty()) {
            Bukkit.getAsyncScheduler().runNow(Aurora.getInstance(), (task) -> {
                for (Reward reward : async) {
                    reward.execute(player, level, placeholders);
                }
                asyncFuture.complete(null);
            });
        } else {
            asyncFuture.complete(null);
        }

        return CompletableFuture.allOf(syncFuture, asyncFuture);
    }
}
