package gg.auroramc.aurora.api.config.premade;


import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SkullConfig {
    private String base64;
    private String url;

    public SkullConfig(SkullConfig other) {
        if (other == null) return;
        this.base64 = other.base64;
        this.url = other.url;
    }
}
