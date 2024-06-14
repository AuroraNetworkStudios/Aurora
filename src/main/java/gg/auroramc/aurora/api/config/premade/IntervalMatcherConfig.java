package gg.auroramc.aurora.api.config.premade;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

@Getter
public class IntervalMatcherConfig {
    private Integer interval;
    private Integer priority;
    private List<String> inheritsFrom;
    private ConfigurationSection rewards;
}
