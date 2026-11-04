package gg.auroramc.aurora.api.reward.integration;

import gg.auroramc.aurora.api.AuroraLogger;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.reward.NumberReward;
import gg.auroramc.aurora.api.util.NamespacedId;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.api.stat.StatMap;
import io.lumine.mythic.lib.api.stat.modifier.StatModifier;
import io.lumine.mythic.lib.player.modifier.ModifierType;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Adds to a MythicLib (MMOCore, MMOItems) stat. Each plugin passes its own key, so the modifiers it gives are kept
 * apart from other plugins' (and its corrector can find them).
 */
@Getter
public class MMOStatReward extends NumberReward {
    private final String key;
    private final Plugin plugin;
    private final AuroraLogger logger;
    private String stat;
    private ModifierType modifierType;
    private boolean valid = true;

    /**
     * @param key    the namespace of the modifiers, like "aurora_levels"
     * @param plugin the plugin giving it, for scheduling
     */
    public MMOStatReward(String key, Plugin plugin, AuroraLogger logger) {
        this.key = key;
        this.plugin = plugin;
        this.logger = logger;
    }

    @Override
    public void execute(Player player, long level, List<Placeholder<?>> placeholders) {
        if (!valid) return;

        MMOPlayerData playerData = MMOPlayerData.get(player);
        StatMap statMap = playerData.getStatMap();
        String modifierKey = NamespacedId.of(key, stat).toString();
        StatModifier currentModifier = getCurrentModifier(modifierKey, statMap);

        double value = getValue(placeholders);

        if (currentModifier != null) {
            value += currentModifier.getValue();
            currentModifier.unregister(playerData);
        }

        StatModifier modifier = new StatModifier(modifierKey, stat, value, modifierType);

        modifier.register(playerData);

        // On the player's own thread (on Folia only that one may touch them)
        player.getScheduler().runDelayed(plugin, (task) -> statMap.getInstance(stat).update(), null, 3);
    }

    @Override
    public void init(ConfigurationSection args) {
        super.init(args);

        this.stat = args.getString("stat");

        if (this.stat == null) {
            this.valid = false;
            logger.warning("Stat is not defined in MMOStatReward");
        }

        this.modifierType = ModifierType.valueOf(args.getString("modifier", "FLAT"));
    }

    @Nullable
    public StatModifier getCurrentModifier(String key, StatMap statMap) {
        for (StatModifier modifier : statMap.getInstance(stat).getModifiers()) {
            if (modifier.getKey().equals(key)) {
                return modifier;
            }
        }
        return null;
    }
}
