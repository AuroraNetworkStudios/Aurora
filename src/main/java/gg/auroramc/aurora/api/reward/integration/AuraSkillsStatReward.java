package gg.auroramc.aurora.api.reward.integration;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.stat.Stat;
import dev.aurelium.auraskills.api.stat.StatModifier;
import gg.auroramc.aurora.api.AuroraLogger;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.reward.NumberReward;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Adds to an AuraSkills stat. Each plugin passes its own modifier prefix, so the modifiers it gives are kept apart from
 * other plugins' (and its corrector can find them).
 */
public class AuraSkillsStatReward extends NumberReward {
    @Getter
    private final String prefix;
    private final AuroraLogger logger;
    @Getter
    private Stat stat;

    /**
     * @param prefix put before the stat's id for the modifier's name, like "AURORA_LEVELS_"
     */
    public AuraSkillsStatReward(String prefix, AuroraLogger logger) {
        this.prefix = prefix;
        this.logger = logger;
    }

    @Override
    public void execute(Player player, long level, List<Placeholder<?>> placeholders) {
        if (stat == null) return;

        var modId = prefix + stat.getId().toString();
        double value = getValue(placeholders);

        var user = AuraSkillsApi.get().getUser(player.getUniqueId());

        var currentMod = user.getStatModifier(modId);
        if (currentMod != null) {
            value = currentMod.value() + value;
        }

        user.addStatModifier(new StatModifier(modId, stat, value));
    }

    @Override
    public void init(ConfigurationSection args) {
        super.init(args);

        stat = AuraSkillsApi.get().getGlobalRegistry()
                .getStat(NamespacedId.fromDefault(args.getString("stat", "invalid_stat")));

        if (stat == null) {
            logger.warning("Invalid stat in AuraSkillsStatReward: " + args.getString("stat"));
        }
    }

    @Override
    public String getDisplay(Player player, List<Placeholder<?>> placeholders) {
        var display = super.getDisplay(player, placeholders);
        if (stat == null) return display;
        var user = AuraSkillsApi.get().getUser(player.getUniqueId());

        return Placeholder.execute(display,
                Placeholder.of("{symbol}", stat.getSymbol(user.getLocale())),
                Placeholder.of("{stat}", stat.getDisplayName(user.getLocale())));
    }
}
