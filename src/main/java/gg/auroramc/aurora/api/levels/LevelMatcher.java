package gg.auroramc.aurora.api.levels;

import gg.auroramc.aurora.api.reward.Reward;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface LevelMatcher {
    boolean matches(int level);

    Map<String, Reward> resolveRewards(int level);

    default List<Reward> computeRewards(int level) {
        return new ArrayList<>(resolveRewards(level).values());
    }
}
