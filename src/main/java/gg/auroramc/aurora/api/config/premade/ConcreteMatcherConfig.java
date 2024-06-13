package gg.auroramc.aurora.api.config.premade;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public class ConcreteMatcherConfig {
    private String inheritFrom;
    private ConfigurationSection rewards;
    private Integer level;
}
