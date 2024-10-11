package gg.auroramc.aurora.api.dependency;

public enum Dep {
    WORLDGUARD("WorldGuard"),
    CITIZENS("Citizens"),
    GANGSPLUS("GangsPlus"),
    DUELS("Duels"),
    PAPI("PlaceholderAPI"),
    CMI("CMI"),
    VAULT("Vault"),
    ESSENTIALS("Essentials"),
    LANDS("Lands"),
    MYTHICMOBS("MythicMobs"),
    ORAXEN("Oraxen"),
    ELITEMOBS("EliteMobs"),
    AURELIUMSKILLS("AureliumSkills"),
    AURASKILLS("AuraSkills"),
    SUPERIORSKYBLOCK2("SuperiorSkyblock2"),
    TRADESYSTEM("TradeSystem"),
    SHOPKEEPERS("Shopkeepers"),
    CUSTOMFISHING("CustomFishing"),
    MMOITEMS("MMOItems"),
    CHESTSORT("ChestSort"),
    REGIONAPI("RegionAPI"),
    ECONOMYSHOPGUI("EconomyShopGUI"),
    PROTCOLLIB("ProtocolLib"),
    MULTIVERSECORE("Multiverse-Core"),
    WILDREGENERATION("WildRegeneration"),
    ECONOMYSHOPGUIPREMIUM("EconomyShopGUI-Premium"),
    EXECUTABLE_ITEMS("ExecutableItems"),
    HEAD_DATABASE("HeadDatabase"),
    PLAYER_POINTS("PlayerPoints"),
    COINS_ENGINE("CoinsEngine"),
    ECO("Eco");

    private String id;

    Dep(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
