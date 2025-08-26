package gg.auroramc.aurora.api.dependency;

public enum Dep {
    ECONOMYSHOPGUIPREMIUM("EconomyShopGUI-Premium"),
    SUPERIORSKYBLOCK2("SuperiorSkyblock2"),
    EXECUTABLE_BLOCKS("ExecutableBlocks"),
    WILDREGENERATION("WildRegeneration"),
    EXECUTABLE_ITEMS("ExecutableItems"),
    MULTIVERSECORE("Multiverse-Core"),
    AURELIUMSKILLS("AureliumSkills"),
    ECONOMYSHOPGUI("EconomyShopGUI"),
    CUSTOMFISHING("CustomFishing"),
    EVEN_MORE_FISH("EvenMoreFish"),
    HEAD_DATABASE("HeadDatabase"),
    PLAYER_POINTS("PlayerPoints"),
    COINS_ENGINE("CoinsEngine"),
    TRADESYSTEM("TradeSystem"),
    SHOPKEEPERS("Shopkeepers"),
    PROTCOLLIB("ProtocolLib"),
    WORLDGUARD("WorldGuard"),
    AURASKILLS("AuraSkills"),
    MYTHICMOBS("MythicMobs"),
    ESSENTIALS("Essentials"),
    PAPI("PlaceholderAPI"),
    ELITEMOBS("EliteMobs"),
    GANGSPLUS("GangsPlus"),
    CHESTSORT("ChestSort"),
    REGIONAPI("RegionAPI"),
    CRACKSHOT("CrackShot"),
    ITEM_EDIT("ItemEdit"),
    MMOITEMS("MMOItems"),
    CITIZENS("Citizens"),
    ORAXEN("Oraxen"),
    DUELS("Duels"),
    VAULT("Vault"),
    LANDS("Lands"),
    NEXO("Nexo"),
    ECO("Eco"),
    CMI("CMI"),
    ;

    private String id;

    Dep(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
