package gg.auroramc.aurora.api.expression;

import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.message.Text;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.entity.Player;

import java.util.List;

public class NumberExpression {

    private final ExpressionBuilder builder;

    public NumberExpression(String expression, String... variables) {
        this.builder = new ExpressionBuilder(expression).variables(variables);
    }

    public NumberExpression(String expression, List<String> variables) {
        this.builder = new ExpressionBuilder(expression).variables(variables.toArray(String[]::new));
    }

    public NumberExpression(String expression) {
        this.builder = new ExpressionBuilder(expression);
    }


    public double evaluate(Placeholder<?>... variables) {
        var expr = builder.build();
        for (var variable : variables) {
            if(variable.getValue() instanceof Number num) {
                expr.setVariable(variable.getKey(), num.doubleValue());
            }
        }
        return expr.evaluate();
    }

    public double evaluate() {
        return builder.build().evaluate();
    }

    public double evaluate(List<Placeholder<?>> variables) {
        var expr = builder.build();
        for (var variable : variables) {
            if(variable.getValue() instanceof Number num) {
                expr.setVariable(variable.getKey(), num.doubleValue());
            }
        }
        return expr.evaluate();
    }

    public static double eval(String expression, Placeholder<?>... variables) {
        return new NumberExpression(Text.fillPlaceholders(expression, variables)).evaluate();
    }

    public static double eval(String expression, List<Placeholder<?>> variables) {
        return new NumberExpression(Text.fillPlaceholders(expression, variables)).evaluate();
    }

    public static double eval(Player player, String expression, Placeholder<?>... variables) {
        return new NumberExpression(Text.fillPlaceholders(player, expression, variables)).evaluate();
    }

    public static double eval(Player player, String expression, List<Placeholder<?>> variables) {
        return new NumberExpression(Text.fillPlaceholders(player, expression, variables)).evaluate();
    }

    public static double eval(String expression) {
        return new NumberExpression(expression).evaluate();
    }
}
