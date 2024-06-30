package gg.auroramc.aurora.api.levels;

import gg.auroramc.aurora.api.config.premade.IntervalMatcherConfig;
import gg.auroramc.aurora.api.reward.Reward;
import lombok.Getter;

import java.util.Map;

@Getter
public class IntervalMatcher extends LevelMatcher {
    private final IntervalMatcherConfig config;

    public IntervalMatcher(String key, IntervalMatcherConfig config, Map<String, Reward> rewards) {
        super(key, rewards);
        this.config = config;
    }

    public boolean matches(int level) {
        if (level < config.getStart() || level > config.getStop()) return false;
        return (level - config.getStart()) % config.getInterval() == 0;
    }
}
