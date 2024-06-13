package gg.auroramc.aurora.api.reward;

import com.google.common.collect.Maps;
import gg.auroramc.aurora.api.util.NamespacedId;
import org.bukkit.entity.Player;

import java.util.Map;

public class RewardAutoCorrector {
    private final Map<NamespacedId, RewardCorrector> correctors = Maps.newConcurrentMap();

    public void registerCorrector(NamespacedId id, RewardCorrector corrector) {
        correctors.put(id, corrector);
    }

    public void correctRewards(Player player) {
        for (var entry : correctors.entrySet()) {
            entry.getValue().correctRewards(player);
        }
    }
}
