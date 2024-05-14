package gg.auroramc.auroralib.api.dependency;

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
    ECONOMYSHOPGUIPREMIUM("EconomyShopGUI-Premium");

    private String id;

    Dep(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}