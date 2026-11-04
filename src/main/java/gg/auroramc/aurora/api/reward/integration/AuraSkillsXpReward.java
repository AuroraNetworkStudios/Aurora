package gg.auroramc.aurora.api.reward.integration;

import com.google.common.collect.Lists;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.skill.Skill;
import gg.auroramc.aurora.api.AuroraLogger;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.reward.NumberReward;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Gives AuraSkills skill XP. {skill_level} in the amount is the player's level in that skill.
 */
public class AuraSkillsXpReward extends NumberReward {
    private final AuroraLogger logger;
    private Skill skill;

    public AuraSkillsXpReward(AuroraLogger logger) {
        this.logger = logger;
    }

    @Override
    public void execute(Player player, long level, List<Placeholder<?>> placeholders) {
        if (skill == null) return;

        var user = AuraSkillsApi.get().getUser(player.getUniqueId());

        List<Placeholder<?>> compiledPlaceholders = Lists.asList(
                Placeholder.of("{skill_level}", user.getSkillLevel(skill)),
                placeholders.toArray(new Placeholder[0])
        );
        double value = getValue(compiledPlaceholders);

        user.addSkillXp(skill, value);
    }

    @Override
    public void init(ConfigurationSection args) {
        super.init(args);

        skill = AuraSkillsApi.get().getGlobalRegistry()
                .getSkill(NamespacedId.fromDefault(args.getString("skill", "invalid_skill")));

        if (skill == null) {
            logger.warning("Invalid skill in AuraSkillsXpReward: " + args.getString("skill"));
        }
    }

    @Override
    public String getDisplay(Player player, List<Placeholder<?>> placeholders) {
        if (skill == null) return super.getDisplay(player, placeholders);
        var user = AuraSkillsApi.get().getUser(player.getUniqueId());

        var display = super.getDisplay(player, Lists.asList(
                Placeholder.of("{skill_level}", user.getSkillLevel(skill)),
                placeholders.toArray(new Placeholder[0])
        ));

        return Placeholder.execute(display,
                Placeholder.of("{skill}", skill.getDisplayName(user.getLocale())));
    }
}
