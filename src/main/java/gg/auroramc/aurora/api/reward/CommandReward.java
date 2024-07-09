package gg.auroramc.aurora.api.reward;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.command.CommandDispatcher;
import gg.auroramc.aurora.api.expression.BooleanExpression;
import gg.auroramc.aurora.api.message.Placeholder;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandReward extends AbstractReward {
    @Getter
    private List<String> commands;
    private String correctionExpression;

    @Override
    public void execute(Player player, long level, List<Placeholder<?>> formulaPlaceholders) {
        if (commands == null) return;
        for (String command : commands) {
            CommandDispatcher.dispatch(player, command, formulaPlaceholders);
        }
    }

    @Override
    public void init(ConfigurationSection args) {
        super.init(args);
        if(args.isString("command") && args.getString("command") != null) {
            commands = List.of(args.getString("command"));
        } else if(args.isList("command")) {
            commands = args.getStringList("command");
        } else {
            commands = List.of();
            Aurora.logger().warning("CommandReward doesn't have the command key");
        }

        correctionExpression = args.getString("correction-condition");
    }

    public boolean shouldBeCorrected(Player player, long level) {
        if (correctionExpression == null || commands == null) return false;
        return BooleanExpression.eval(player, correctionExpression, Placeholder.of("{level}", level));
    }
}
