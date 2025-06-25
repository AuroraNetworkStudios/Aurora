package gg.auroramc.aurora.hooks;

import dev.aurelium.auraskills.api.AuraSkillsProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class AuraSkillsHook {
    public static boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("AuraSkills");
    }

    public static void pauseActionBar(Player player) {
        AuraSkillsProvider.getInstance().getUser(player.getUniqueId()).pauseActionBar(2500, TimeUnit.MILLISECONDS);
    }
}
