package gg.auroramc.aurora.expansions.placeholder;

import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.api.expansions.AuroraExpansion;
import gg.auroramc.aurora.api.placeholder.PlaceholderHandlerRegistry;

public class PlaceholderExpansion implements AuroraExpansion {
    @Override
    public void hook() {
        new AuroraPapiExpansion().register();
        PlaceholderHandlerRegistry.addHandler(new MetaHandler());
    }

    @Override
    public boolean canHook() {
        return DependencyManager.hasDep(Dep.PAPI);
    }
}
