package gg.auroramc.aurora.internal.config;

import lombok.Getter;

@Getter
public class BlockTrackerConfig {
    private Boolean enabled = true;
    private String storageType = "file";
}
