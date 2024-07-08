package gg.auroramc.aurora.api.config.premade;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

@Getter
public class ConcreteMatcherConfig {
    private List<String> inheritsFrom;
    private ConfigurationSection rewards;
    private Integer level;
    private Map<String, ItemConfig> item;
}
