package gg.auroramc.aurora.api.levels;

import gg.auroramc.aurora.api.reward.Reward;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public abstract class AbstractLevelMatcher implements LevelMatcher {
    protected final String key;
    protected LevelMatcher parent;
    protected final Map<String, Reward> rewards;

    public AbstractLevelMatcher(String key, Map<String, Reward> rewards) {
        this.rewards = rewards;
        this.key = key;
    }

    public Map<String, Reward> resolveRewards(int level) {
        if(parent == null && matches(level)) return rewards;

        // Map to collect rewards from all matching entities in the inheritance chain
        Map<String, Reward> collectedRewards = new LinkedHashMap<>();

        // Regardless of the current match, continue to traverse the parent chain
        if (parent != null) {
            collectedRewards.putAll(parent.resolveRewards(level));
        }

        // If the current level matches, add current rewards to the map
        if (matches(level)) {
            collectedRewards.putAll(rewards);
        }

        return collectedRewards;
    }
}
