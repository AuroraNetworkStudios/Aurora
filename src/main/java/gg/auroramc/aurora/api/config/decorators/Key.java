package gg.auroramc.aurora.api.config.decorators;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The YAML key of a config field, instead of the field's name in kebab-case. Renaming the field doesn't change the
 * key then.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Key {
    String value();
}
