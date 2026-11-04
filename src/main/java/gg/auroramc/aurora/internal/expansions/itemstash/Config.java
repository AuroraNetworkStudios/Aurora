package gg.auroramc.aurora.internal.expansions.itemstash;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.config.ConfigFile;
import gg.auroramc.aurora.api.config.premade.ItemConfig;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class Config {
    private Boolean notifyOnJoin = true;
    private List<String> commandAliases = List.of("stash", "itemstash");
    private List<Integer> stashArea;
    private MenuConfig menu;

    @Getter
    public static final class MenuConfig {
        private String title;
        private FillerConfig filler;
        private ItemsConfig items;
        private Map<String, ItemConfig> customItems;
    }

    @Getter
    public static final class ItemsConfig {
        private ItemConfig prevPage;
        private ItemConfig currentPage;
        private ItemConfig nextPage;
        private ItemConfig collectAll;
    }

    @Getter
    public static final class FillerConfig {
        public Boolean enabled = true;
        public ItemConfig item;
    }

    // Kept across reloads, so a file broken while the server runs keeps the config from before
    private static ConfigFile<Config> file;

    /**
     * Loads itemstash.yml, or reloads it if it was loaded before.
     */
    public static synchronized Config load() {
        if (file == null) {
            file = ConfigFile.builder(Aurora.getInstance(), "itemstash.yml", Config.class).build();
            return file.get();
        }
        return file.reload();
    }
}
