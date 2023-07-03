package gg.auroramc.auroralib.config.decorators;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Should only be used on custom Lists
 * Like List\<MyObject\> since its designed that way.
 * If you try loading anything else in it will give error
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface IClassList {  }