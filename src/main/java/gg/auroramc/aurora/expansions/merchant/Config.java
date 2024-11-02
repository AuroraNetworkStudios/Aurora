package gg.auroramc.aurora.expansions.merchant;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.config.AuroraConfig;
import lombok.Getter;

import java.io.File;
import java.util.List;
import java.util.Map;

@Getter
public class Config extends AuroraConfig {
    private List<String> commandAliases = List.of("merchants");
    private Map<String, MerchantConfig> merchants;
    private MessageConfig messages;

    public Config() {
        super(new File(Aurora.getInstance().getDataFolder(), "/merchants.yml"));
    }

    public static void saveDefault() {
        if (!new File(Aurora.getInstance().getDataFolder(), "/merchants.yml").exists()) {
            Aurora.getInstance().saveResource("merchants.yml", false);
        }
    }

    @Getter
    public static final class MerchantConfig {
        private Boolean enabled = true;
        private String name = "";
        private String permission;
        private List<MerchantOfferConfig> offers = List.of();
    }

    @Getter
    public static final class MerchantOfferConfig {
        private List<String> ingredients = List.of();
        private String result = "";
    }

    @Getter
    public static final class MessageConfig {
        private String noPermission = "&cYou don't have permission to open this merchant!";
        private String notFound = "&cMerchant not found!";
    }
}
