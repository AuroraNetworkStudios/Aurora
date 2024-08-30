package gg.auroramc.aurora.expansions.gui;

import gg.auroramc.aurora.api.config.AuroraConfig;
import gg.auroramc.aurora.api.config.premade.ItemConfig;
import gg.auroramc.aurora.api.config.premade.RequirementConfig;
import lombok.Getter;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class GuiConfig extends AuroraConfig {
    private String registerCommand;
    private String title;
    private Integer rows = 6;
    private boolean update = false;
    private List<RequirementConfig> openRequirements;
    private List<String> openActions;
    private List<String> closeActions;
    private ItemConfig filler;
    private Map<String, ItemConfig> items = new HashMap<>();

    public GuiConfig(File file) {
        super(file);
    }
}
