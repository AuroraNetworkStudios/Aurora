package gg.auroramc.aurora.api.config.premade;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

@Getter
public class IntervalMatcherConfig {
    private Integer interval = 1;
    private Integer start = 0;
    private Integer stop = Integer.MAX_VALUE;
    private Integer priority = 0;
    private List<String> inheritsFrom;
    private ConfigurationSection rewards;
    private Map<String, ItemConfig> item;
}
