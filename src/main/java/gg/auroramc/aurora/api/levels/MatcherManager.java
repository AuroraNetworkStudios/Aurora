package gg.auroramc.aurora.api.levels;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import gg.auroramc.aurora.api.config.premade.ConcreteMatcherConfig;
import gg.auroramc.aurora.api.config.premade.IntervalMatcherConfig;
import gg.auroramc.aurora.api.reward.Reward;
import gg.auroramc.aurora.api.reward.RewardFactory;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

@Getter
public class MatcherManager {
    private final List<IntervalMatcher> intervalMatchers = Lists.newCopyOnWriteArrayList();
    private final Map<Integer, ConcreteMatcher> customMatchers = Maps.newConcurrentMap();
    private final RewardFactory rewardFactory;

    public MatcherManager(RewardFactory rewardFactory) {
        this.rewardFactory = rewardFactory;
    }

    public void reload(Map<String, IntervalMatcherConfig> matchers, Map<String, ConcreteMatcherConfig> customMatchers) {
        this.intervalMatchers.clear();
        this.customMatchers.clear();

        Map<String, IntervalMatcher> map = new LinkedHashMap<>(matchers.entrySet().size());
        Map<String, ConcreteMatcher> cMap = new LinkedHashMap<>(matchers.entrySet().size());

        for (Map.Entry<String, IntervalMatcherConfig> matcher : matchers.entrySet()) {
            map.put(matcher.getKey(), parseMatcher(matcher.getKey(), matcher.getValue()));
        }

        for (var customMatcher : customMatchers.entrySet()) {
            var val = customMatcher.getValue();
            var key = customMatcher.getKey();
            cMap.put(key, new ConcreteMatcher(key, val, createRewards(val.getRewards())));
        }

        for (var matcher : map.values()) {
            if (matcher.getConfig().getInheritFrom() != null) {
                var parent = map.get(matcher.getConfig().getInheritFrom());
                if (parent != null) {
                    matcher.setParent(parent);
                }
            }
        }

        for (var matcher : cMap.values()) {
            if (matcher.getConfig().getInheritFrom() != null) {
                LevelMatcher parent = map.get(matcher.getConfig().getInheritFrom());
                if (parent == null) {
                    parent = cMap.get(matcher.getConfig().getInheritFrom());
                }
                if (parent != null) {
                    matcher.setParent(parent);
                }
            }
            this.customMatchers.put(matcher.getConfig().getLevel(), matcher);
        }

        this.intervalMatchers.addAll(map.values().stream().sorted((a, b) -> Integer.compare(b.getConfig().getPriority(), a.getConfig().getPriority())).toList());
    }

    private IntervalMatcher parseMatcher(String key, IntervalMatcherConfig matcher) {
        return new IntervalMatcher(key, matcher, createRewards(matcher.getRewards()));
    }

    private Map<String, Reward> createRewards(ConfigurationSection rewards) {
        var map = Maps.<String, Reward>newLinkedHashMap();
        if (rewards == null) return map;

        for (var key : rewards.getKeys(false)) {
            var config = rewards.getConfigurationSection(key);
            if (config == null) continue;
            rewardFactory.createReward(config).ifPresent(reward -> map.put(key, reward));
        }

        return map;
    }

    /**
     * Get the best matcher for the given level
     *
     * @param level level to get the matcher for
     * @return the best matcher for the given level
     */
    public LevelMatcher getBestMatcher(int level) {
        if (customMatchers.containsKey(level)) {
            return customMatchers.get(level);
        }

        for (var matcher : intervalMatchers) {
            if (matcher.matches(level)) {
                return matcher;
            }
        }

        return null;
    }
}
