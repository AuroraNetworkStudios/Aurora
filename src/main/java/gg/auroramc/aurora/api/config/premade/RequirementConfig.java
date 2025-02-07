package gg.auroramc.aurora.api.config.premade;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequirementConfig {
    private String requirement;
    private List<String> denyActions;
}
