package gg.auroramc.aurora.api.config.premade;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class IntervalMatcherConfig {
    private Integer interval;
    private Integer priority;
    private String inheritFrom;
    private ConfigurationSection rewards;
}
