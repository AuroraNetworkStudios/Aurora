package gg.auroramc.auroralib.api.config.premade;

import lombok.Getter;
import lombok.Setter;

public class PotionConfig {
    @Getter
    @Setter
    private String type = "WATER";

    @Getter
    @Setter
    private Boolean extended = false;

    @Getter
    @Setter
    private Boolean upgraded = false;

    public PotionConfig() {}

    public PotionConfig(PotionConfig other) {
        this.type = other.type;
        this.extended = other.extended;
        this.upgraded = other.upgraded;
    }
}
