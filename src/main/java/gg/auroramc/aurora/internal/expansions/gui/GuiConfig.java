package gg.auroramc.aurora.internal.expansions.gui;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.config.ConfigFile;
import gg.auroramc.aurora.api.config.premade.ItemConfig;
import gg.auroramc.aurora.api.config.premade.RequirementConfig;
import lombok.Getter;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class GuiConfig {
    private List<String> registerCommands;
    private String title;
    private Integer rows = 6;
    private boolean refresh = false;
    private Integer refreshInterval = 1;
    private List<RequirementConfig> openRequirements;
    private List<String> openActions;
    private List<String> closeActions;
    private ItemConfig filler;
    private Map<String, ItemConfig> items = new HashMap<>();

    // Kept across reloads, so a file broken while the server runs keeps the menu from before
    private static final Map<String, ConfigFile<GuiConfig>> files = new ConcurrentHashMap<>();

    /**
     * Loads the menu file, or reloads it if it was loaded before.
     */
    public static GuiConfig load(File file) {
        var relative = Aurora.getInstance().getDataFolder().toPath().relativize(file.toPath()).toString();
        var existing = files.get(relative);
        if (existing != null) return existing.reload();
        var configFile = files.computeIfAbsent(relative, k -> ConfigFile.builder(Aurora.getInstance(), relative, GuiConfig.class).bundled(null).build());
        return configFile.get();
    }
}
