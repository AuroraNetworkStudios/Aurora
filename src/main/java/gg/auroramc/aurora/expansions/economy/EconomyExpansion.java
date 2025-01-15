package gg.auroramc.aurora.expansions.economy;

import com.Zrips.CMI.CMI;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.api.expansions.AuroraExpansion;
import gg.auroramc.aurora.expansions.economy.providers.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyExpansion implements AuroraExpansion {
    private final Map<String, AuroraEconomy> economies = new ConcurrentHashMap<>();
    private String defaultEconomy = null;

    @Override
    public void hook() {
        if (DependencyManager.hasDep(Dep.VAULT)) {
            economies.put(Dep.VAULT.getId(), new VaultEconomy());
            defaultEconomy = Dep.VAULT.getId();
        }

        if (DependencyManager.hasDep(Dep.ESSENTIALS) && DependencyManager.getEssentials().isEconomyEnabled()) {
            economies.put(Dep.ESSENTIALS.getId(), new EssentialsEconomy());
            defaultEconomy = Dep.ESSENTIALS.getId();
        } else if (DependencyManager.hasDep(Dep.CMI) && CMI.getInstance().getEconomyManager().isEnabled()) {
            economies.put(Dep.CMI.getId(), new CMIEconomy());
            defaultEconomy = Dep.CMI.getId();
        }

        if (DependencyManager.hasDep(Dep.ELITEMOBS)) {
            economies.put(Dep.ELITEMOBS.getId(), new EliteMobsEconomy());
            if (defaultEconomy == null) defaultEconomy = Dep.ELITEMOBS.getId();
        }

        if (DependencyManager.hasDep("EcoBits")) {
            economies.put("EcoBits", new EcoBitsEconomy());
            if (defaultEconomy == null) defaultEconomy = "EcoBits";
        }

        if (DependencyManager.hasDep(Dep.PLAYER_POINTS)) {
            economies.put(Dep.PLAYER_POINTS.getId(), new PlayerPointsEconomy());
            if (defaultEconomy == null) defaultEconomy = Dep.PLAYER_POINTS.getId();
        }

        if (DependencyManager.hasDep(Dep.COINS_ENGINE)) {
            economies.put(Dep.COINS_ENGINE.getId(), new CoinsEngineEconomy());
            if (defaultEconomy == null) defaultEconomy = Dep.COINS_ENGINE.getId();
        }

        if (DependencyManager.hasDep("RoyaleEconomy")) {
            economies.put("RoyaleEconomyBank", new RoyaleEconomyBank());
            economies.put("RoyaleEconomy", new RoyaleEco());
            if (defaultEconomy == null) defaultEconomy = "RoyaleEconomy";
        }

        if (!Aurora.getLibConfig().getDefaultEconomyProvider().equals("auto-detect")) {
            if (economies.containsKey(Aurora.getLibConfig().getDefaultEconomyProvider())) {
                defaultEconomy = Aurora.getLibConfig().getDefaultEconomyProvider();
            } else {
                Aurora.logger().severe("Invalid default economy provider in config: " + Aurora.getLibConfig().getDefaultEconomyProvider() + ", using " + defaultEconomy + " instead.");
            }
        }

        Aurora.logger().info("Loaded " + economies.size() + " economy providers. " + String.join(", ", economies.keySet()));
        Aurora.logger().info("Using " + defaultEconomy + " as the default economy provider.");
    }

    @Override
    public boolean canHook() {
        return
            DependencyManager.hasAnyDep(
                Dep.ESSENTIALS,
                Dep.CMI,
                Dep.VAULT,
                Dep.ELITEMOBS,
                Dep.PLAYER_POINTS,
                Dep.COINS_ENGINE
            ) || DependencyManager.hasAnyDep(
        "EcoBits",
                "RoyaleEconomy"
            );
    }

    public AuroraEconomy getEconomy(String economyPlugin) {
        return economies.get(economyPlugin);
    }

    public AuroraEconomy getEconomy(Dep economyPlugin) {
        return economies.get(economyPlugin.getId());
    }

    public AuroraEconomy getDefaultEconomy() {
        return economies.get(defaultEconomy);
    }

    public AuroraEconomy getOrDefaultEconomy(String economyPlugin) {
        return economies.getOrDefault(economyPlugin, getDefaultEconomy());
    }

    public void addEconomy(String economyPlugin, AuroraEconomy economy) {
        economies.put(economyPlugin, economy);
    }

    public void addEconomy(String economyPlugin, AuroraEconomy economy, boolean isDefault) {
        economies.put(economyPlugin, economy);
        if (isDefault) {
            defaultEconomy = economyPlugin;
        }
    }

    public String getDefaultEconomyId() {
        return defaultEconomy;
    }

    public Set<String> getEconomyIds() {
        return economies.keySet();
    }
}
