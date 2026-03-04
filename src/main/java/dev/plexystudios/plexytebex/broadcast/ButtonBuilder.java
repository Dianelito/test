package dev.plexystudios.plexytebex.broadcast;

import dev.plexystudios.plexytebex.config.YamlConfig;
import dev.plexystudios.plexytebex.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class ButtonBuilder {

    private final YamlConfig config;

    public ButtonBuilder(YamlConfig config) { this.config = config; }

    public Component build() {
        int padding = config.getInt("buttons.padding", 0);
        Component row = Component.text(" ".repeat(Math.max(0, padding)));

        YamlConfig list = config.section("buttons.list");
        if (list == null) return row;

        boolean first = true;
        for (String key : list.keys()) {
            YamlConfig btn = list.section(key);
            if (btn == null) continue;

            Component comp = ColorUtil.parse(btn.getString("text", ""));

            String hover = btn.getString("hover", "");
            if (!hover.isEmpty())
                comp = comp.hoverEvent(HoverEvent.showText(ColorUtil.parse(hover)));

            String url = btn.getString("open_url", "");
            if (!url.isEmpty())
                comp = comp.clickEvent(ClickEvent.openUrl(url));

            if (!first) row = row.append(Component.text(" "));
            row = row.append(comp);
            first = false;
        }
        return row;
    }
}
