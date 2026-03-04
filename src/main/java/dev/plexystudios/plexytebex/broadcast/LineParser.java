package dev.plexystudios.plexytebex.broadcast;

import dev.plexystudios.plexytebex.util.CenterUtil;
import dev.plexystudios.plexytebex.util.ColorUtil;
import net.kyori.adventure.text.Component;

import java.util.Map;

public final class LineParser {

    private LineParser() {}

    public static Component parse(String line, Map<String, String> args) {
        if (line == null || line.equalsIgnoreCase("<empty>")) return Component.empty();

        line = ColorUtil.replacePlaceholders(line, args);

        String trimmed = line.trim();
        if (trimmed.toLowerCase().startsWith("<center>")) {
            String inner = trimmed.replaceAll("(?i)</?center>", "").trim();
            line = CenterUtil.center(inner);
        }

        return ColorUtil.parse(line);
    }
}
