package gg.auroramc.aurora.api.levels;

import gg.auroramc.aurora.api.config.premade.ConcreteMatcherConfig;
import gg.auroramc.aurora.api.reward.Reward;
import lombok.Getter;

import java.util.Map;

@Getter
public class ConcreteMatcher extends AbstractLevelMatcher {
    private final ConcreteMatcherConfig config;

    public ConcreteMatcher(String key, ConcreteMatcherConfig config, Map<String, Reward> rewards) {
        super(key, rewards);
        this.config = config;
    }

    public boolean matches(int level) {
        return this.config.getLevel() == level;
    }
}
