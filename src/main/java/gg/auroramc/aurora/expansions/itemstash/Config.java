package gg.auroramc.aurora.expansions.itemstash;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.config.AuroraConfig;
import gg.auroramc.aurora.api.config.premade.ItemConfig;
import lombok.Getter;

import java.io.File;
import java.util.List;
import java.util.Map;

@Getter
public class Config extends AuroraConfig {
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

    public Config() {
        super(new File(Aurora.getInstance().getDataFolder(), "/itemstash.yml"));
    }

    public static void saveDefault() {
        Aurora.getInstance().saveResource("itemstash.yml", false);
    }
}
