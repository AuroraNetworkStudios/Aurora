package gg.auroramc.aurora.internal.expansions;

public interface AuroraExpansion {
    void hook();
    boolean canHook();

    default void reload() {}
}
