package gg.auroramc.aurora.api.levels;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.reward.Reward;
import lombok.Getter;

import java.util.*;

public abstract class LevelMatcher {
    @Getter
    protected final String key;
    @Getter
    protected final Map<String, Reward> rewards;
    protected List<LevelMatcher> parents;

    public LevelMatcher(String key, Map<String, Reward> rewards) {
        this.rewards = rewards;
        this.key = key;
    }

    public List<Reward> computeRewards(int level) {
        return new ArrayList<>(resolveRewards(level).values());
    }

    public Map<String, Reward> resolveRewards(int level) {
        return resolveRewards(level, new LinkedHashSet<>());
    }

    private Map<String, Reward> resolveRewards(int level, Set<LevelMatcher> visited) {
        if (parents == null && matches(level)) return rewards;

        if (!visited.add(this)) {
            Aurora.logger().warning("Detected infinite loop in level matcher inheritance chain. Stopping at " + new ArrayList<>(visited).get(visited.size() - 1).getKey() + " to prevent stack overflow. [" +
                    String.join(" -> ", visited.stream().map(LevelMatcher::getKey).toList()) + " -> " + this.getKey() + "]");
            return Collections.emptyMap();
        }

        Map<String, Reward> collectedRewards = new LinkedHashMap<>();

        if (parents != null) {
            for (LevelMatcher parent : parents) {
                collectedRewards.putAll(parent.resolveRewards(level, visited));
            }
        }

        if (matches(level)) {
            collectedRewards.putAll(rewards);
        }

        visited.remove(this);

        return collectedRewards;
    }

    public void addParent(LevelMatcher parent) {
        if (this.parents == null) {
            this.parents = new ArrayList<>();
        }
        this.parents.add(parent);
    }

    public abstract boolean matches(int level);
}
