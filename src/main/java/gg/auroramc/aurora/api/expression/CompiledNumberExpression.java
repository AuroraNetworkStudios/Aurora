package gg.auroramc.aurora.api.expression;

import gg.auroramc.aurora.api.message.Placeholder;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompiledNumberExpression {
    private final Expression expression;
    private final Map<String, Double> cache = new HashMap<>();

    public CompiledNumberExpression(String expression, List<String> placeholders) {
        this.expression = new ExpressionBuilder(expression).variables(placeholders.stream()
                .map((pl) -> pl.replace("{", "").replace("}", "")).toArray(String[]::new)).build();
    }

    public static CompiledNumberExpression expression(String expression, List<String> placeholders) {
        return new CompiledNumberExpression(expression, placeholders);
    }

    public double evaluate(List<Placeholder<?>> variables) {
        for (var variable : variables) {
            if (variable.getValue() instanceof Number num) {
                if (variable.getKey().startsWith("{")) {
                    expression.setVariable(variable.getKey().replace("{", "").replace("}", ""), num.doubleValue());
                } else {
                    expression.setVariable(variable.getKey(), num.doubleValue());
                }

            }
        }
        return expression.evaluate();
    }

    public double cachedEvaluate(List<Placeholder<?>> variables) {
        String cacheKey = generateCacheKey(variables);

        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        var result = evaluate(variables);
        cache.put(cacheKey, result);
        return result;
    }

    private String generateCacheKey(List<Placeholder<?>> variables) {
        StringBuilder keyBuilder = new StringBuilder();
        for (var variable : variables) {
            if (variable.getValue() instanceof Number) {
                keyBuilder.append(variable.getKey())
                        .append("=")
                        .append(variable.getValue())
                        .append(";");
            }
        }
        return keyBuilder.toString();
    }
}
