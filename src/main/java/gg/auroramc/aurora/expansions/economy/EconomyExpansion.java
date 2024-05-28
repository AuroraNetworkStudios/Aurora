package gg.auroramc.aurora.expansions.economy;

import com.Zrips.CMI.CMI;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.api.expansions.AuroraExpansion;
import gg.auroramc.aurora.expansions.economy.providers.CMIEconomy;
import gg.auroramc.aurora.expansions.economy.providers.EssentialsEconomy;
import gg.auroramc.aurora.expansions.economy.providers.VaultEconomy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyExpansion implements AuroraExpansion {
    private final Map<String, AuroraEconomy> economies = new ConcurrentHashMap<>();
    private String defaultEconomy;

    @Override
    public void hook() {
        if(DependencyManager.hasDep(Dep.VAULT)) {
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
    }

    @Override
    public boolean canHook() {
        return DependencyManager.hasAnyDep(Dep.ESSENTIALS, Dep.CMI, Dep.VAULT);
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

    public void addEconomy(String economyPlugin, AuroraEconomy economy) {
        economies.put(economyPlugin, economy);
    }

    public void addEconomy(String economyPlugin, AuroraEconomy economy, boolean isDefault) {
        economies.put(economyPlugin, economy);
        if(isDefault) {
            defaultEconomy = economyPlugin;
        }
    }
}
