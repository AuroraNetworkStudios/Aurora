package gg.auroramc.aurora.api.config.premade;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

@Getter
public class ConcreteMatcherConfig {
    private List<String> inheritsFrom;
    private ConfigurationSection rewards;
    private Integer level;
}
