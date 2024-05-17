package gg.auroramc.auroralib.expansions.placeholder;

import gg.auroramc.auroralib.api.dependency.Dep;
import gg.auroramc.auroralib.api.dependency.DependencyManager;
import gg.auroramc.auroralib.api.expansions.AuroraExpansion;
import gg.auroramc.auroralib.api.placeholder.PlaceholderHandlerRegistry;

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
