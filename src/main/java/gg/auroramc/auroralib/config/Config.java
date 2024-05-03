package gg.auroramc.auroralib.config;

import gg.auroramc.auroralib.api.config.AuroraConfig;
import lombok.Getter;

import java.io.File;

@Getter
public class Config extends AuroraConfig {
    private boolean debug;

    public Config(File file) {
        super(file);
    }

}
