package gg.auroramc.auroralib.message;

public class Text {

    public static String build(String text, Placeholder... placeholders) {
        return Chat.translateColorCodes(Placeholder.execute(text, placeholders));
    }

}
