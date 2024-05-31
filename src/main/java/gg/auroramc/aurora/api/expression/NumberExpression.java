package gg.auroramc.aurora.api.expression;

import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.message.Text;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.entity.Player;

import java.util.List;

public class NumberExpression {

    private final Expression expression;

    public NumberExpression(String expression, String... variables) {
        this.expression = new ExpressionBuilder(expression).variables(variables).build();
    }

    public NumberExpression(String expression, List<String> variables) {
        this.expression = new ExpressionBuilder(expression).variables(variables.toArray(String[]::new)).build();
    }

    public NumberExpression(String expression) {
        this.expression = new ExpressionBuilder(expression).build();
    }


    public double evaluate(Placeholder<?>... variables) {
        for (var variable : variables) {
            if(variable.getValue() instanceof Number num) {
                expression.setVariable(variable.getKey(), num.doubleValue());
            }
        }
        return expression.evaluate();
    }

    public double evaluate() {
        return expression.evaluate();
    }

    public double evaluate(List<Placeholder<?>> variables) {
        for (var variable : variables) {
            if(variable.getValue() instanceof Number num) {
                expression.setVariable(variable.getKey(), num.doubleValue());
            }
        }
        return expression.evaluate();
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
