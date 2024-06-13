package gg.auroramc.aurora.api.reward;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.command.CommandDispatcher;
import gg.auroramc.aurora.api.expression.BooleanExpression;
import gg.auroramc.aurora.api.message.Placeholder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandReward extends AbstractReward {
    private String command;
    private String correctionExpression;

    @Override
    public void execute(Player player, long level, List<Placeholder<?>> formulaPlaceholders) {
        if (command == null) return;
        CommandDispatcher.dispatch(player, command, formulaPlaceholders);
    }

    @Override
    public void init(ConfigurationSection args) {
        super.init(args);
        command = args.getString("command", null);
        correctionExpression = args.getString("correction-condition");

        if (command == null) {
            Aurora.logger().warning("CommandReward has no command key");
        }
    }

    public boolean shouldBeCorrected(Player player, long level) {
        if (correctionExpression == null || command == null) return false;
        return BooleanExpression.eval(player, correctionExpression, Placeholder.of("{level}", level));
    }
}
