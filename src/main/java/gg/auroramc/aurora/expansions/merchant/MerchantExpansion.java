package gg.auroramc.aurora.expansions.merchant;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.expansions.AuroraExpansion;
import gg.auroramc.aurora.api.util.Version;
import lombok.Getter;

import java.util.List;

@Getter
public class MerchantExpansion implements AuroraExpansion {
    private Config config;

    @Override
    public void hook() {
        reload();
        var cm = Aurora.getInstance().getCommandManager().getPaperCommandManager();
        cm.getCommandReplacements().addReplacement("merchantAlias", a(config.getCommandAliases()));
        cm.getCommandCompletions().registerCompletion("merchants", c -> config.getMerchants().keySet());
        cm.registerCommand(new MerchantCommand(this));
    }

    @Override
    public void reload() {
        Config.saveDefault();
        config = new Config();
        config.load();
    }

    @Override
    public boolean canHook() {
        return Version.isAtLeastVersion(20, 4);
    }

    private String a(List<String> aliases) {
        return String.join("|", aliases);
    }
}
