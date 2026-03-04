package dev.plexystudios.plexytebex.command;

import dev.plexystudios.plexytebex.announcement.AnnouncementService;
import dev.plexystudios.plexytebex.announcement.SaleBossBarService;
import dev.plexystudios.plexytebex.broadcast.BroadcastService;
import dev.plexystudios.plexytebex.config.YamlConfig;
import dev.plexystudios.plexytebex.core.PlexyTebex;
import dev.plexystudios.plexytebex.i18n.LangManager;
import dev.plexystudios.plexytebex.stats.StatsManager;
import dev.plexystudios.plexytebex.util.ArgParser;
import dev.plexystudios.plexytebex.util.ColorUtil;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import java.util.*;

/**
 * /plexytebex (aliases: /ptebex /pt)
 *
 * Sub-commands:
 *   help, reload, test, top, announce <id>, args, lang
 *   (default) key(value) … → purchase broadcast
 */
public class PlexyCommand implements SimpleCommand {

    private final PlexyTebex plugin;

    public PlexyCommand(PlexyTebex plugin) { this.plugin = plugin; }

    @Override
    public void execute(Invocation inv) {
        CommandSource src  = inv.source();
        String[] raw       = inv.arguments();
        LangManager lang   = plugin.getLang();
        YamlConfig cfg     = plugin.getConfigManager().get();

        String sub = raw.length > 0 ? raw[0].toLowerCase() : "";

        switch (sub) {

            // ── help ──────────────────────────────────────────────────────
            case "help" -> {
                if (!perm(src, "plexytebex.help")) { deny(src, lang); return; }
                lang.getList("help", Map.of("cmd", "plexytebex"))
                        .forEach(l -> src.sendMessage(ColorUtil.parse(l)));
            }

            // ── reload ────────────────────────────────────────────────────
            case "reload" -> {
                if (!perm(src, "plexytebex.reload")) { deny(src, lang); return; }
                plugin.getConfigManager().reload();
                plugin.getLang().reload();
                plugin.getAnnouncementService().stopAutoScheduler();
                plugin.getAnnouncementService().startAutoScheduler();
                msg(src, lang.get("reload"));
            }

            // ── lang ──────────────────────────────────────────────────────
            case "lang" -> {
                if (!perm(src, "plexytebex.help")) { deny(src, lang); return; }
                msg(src, lang.get("current-lang", Map.of("lang", lang.getCurrentCode())));
            }

            // ── args ──────────────────────────────────────────────────────
            case "args" -> {
                if (!perm(src, "plexytebex.args")) { deny(src, lang); return; }
                List<String> phs = cfg.getStringList("placeholders");
                lang.getList("args.header").forEach(l -> src.sendMessage(ColorUtil.parse(l)));
                if (phs.isEmpty()) {
                    msg(src, lang.get("args.empty"));
                } else {
                    for (String ph : phs)
                        msg(src, lang.get("args.format", Map.of("arg", ph)));
                }
                lang.getList("args.footer").forEach(l -> src.sendMessage(ColorUtil.parse(l)));
            }

            // ── top ───────────────────────────────────────────────────────
            case "top" -> {
                if (!perm(src, "plexytebex.top")) { deny(src, lang); return; }
                int limit = cfg.getInt("top.limit", 10);
                var entries = plugin.getStats().getTop(limit);
                lang.getList("top.header").forEach(l -> src.sendMessage(ColorUtil.parse(l)));
                if (entries.isEmpty()) {
                    msg(src, lang.get("top.empty"));
                } else {
                    for (int i = 0; i < entries.size(); i++) {
                        var e = entries.get(i);
                        msg(src, lang.get("top.entry", Map.of(
                                "pos",   String.valueOf(i + 1),
                                "player", e.getKey(),
                                "count",  String.valueOf(e.getValue())
                        )));
                    }
                }
                lang.getList("top.footer").forEach(l -> src.sendMessage(ColorUtil.parse(l)));
            }

            // ── announce <id> ─────────────────────────────────────────────
            case "announce" -> {
                if (!perm(src, "plexytebex.announce")) { deny(src, lang); return; }
                if (raw.length < 2) {
                    msg(src, lang.get("invalid-format")); return;
                }
                String id = raw[1].toLowerCase();
                boolean sent = plugin.getAnnouncementService().trigger(id);
                if (sent) {
                    msg(src, lang.get("announce-sent", Map.of("id", id)));
                } else {
                    msg(src, lang.get("unknown-announce", Map.of("id", id)));
                }
            }

            // ── sale on/off ───────────────────────────────────────────────
            case "sale" -> {
                if (!perm(src, "plexytebex.sale")) { deny(src, lang); return; }
                if (raw.length < 2) {
                    msg(src, lang.get("sale-usage")); return;
                }
                String action = raw[1].toLowerCase();
                SaleBossBarService sale = plugin.getSaleBossBar();

                if (action.equals("off")) {
                    if (!sale.isActive()) {
                        msg(src, lang.get("sale-not-active")); return;
                    }
                    sale.disable();
                    msg(src, lang.get("sale-disabled"));

                } else if (action.equals("on")) {
                    // /plexytebex sale on [text] [color]
                    // Default text and color from config
                    String text  = cfg.getString("sale-bossbar.default-text",
                            "<yellow><bold> SALE ACTIVE — Check the shop! </bold></yellow>");
                    String color = cfg.getString("sale-bossbar.default-color", "YELLOW");

                    // Override text if provided after "on"
                    if (raw.length >= 3) {
                        // Join everything after "on" as the text (allows spaces)
                        StringBuilder sb = new StringBuilder();
                        for (int i = 2; i < raw.length; i++) {
                            if (i > 2) sb.append(" ");
                            sb.append(raw[i]);
                        }
                        text = sb.toString();
                    }

                    sale.enable(text, color);
                    msg(src, lang.get("sale-enabled"));

                } else {
                    msg(src, lang.get("sale-usage"));
                }
            }
            case "test" -> {
                if (!perm(src, "plexytebex.test")) { deny(src, lang); return; }
                String player  = cfg.getString("test.default-player",  "TestPlayer");
                String pkg     = cfg.getString("test.default-package", "VIP GOLD");
                String price   = cfg.getString("test.default-price",   "$9.99");

                // Override with args if provided: /plexytebex test player(X) ...
                if (raw.length > 1) {
                    Map<String, String> overrides = ArgParser.parse(Arrays.copyOfRange(raw, 1, raw.length));
                    player = overrides.getOrDefault("player", player);
                    pkg    = overrides.getOrDefault("package", pkg);
                    price  = overrides.getOrDefault("price", price);
                }

                Map<String, String> args = Map.of("player", player, "package", pkg, "price", price);
                final Map<String, String> finalArgs = args;

                plugin.getServer().getScheduler()
                        .buildTask(plugin, () -> new BroadcastService(plugin).broadcast(finalArgs))
                        .schedule();

                msg(src, lang.get("test-sent", Map.of("player", player, "package", pkg, "price", price)));
            }

            // ── broadcast trigger (console or admin player) ───────────────
            default -> {
                if (src instanceof Player && !src.hasPermission("plexytebex.admin")) {
                    msg(src, lang.get("no-permission")); return;
                }
                if (raw.length == 0) { showUsage(src, lang, cfg); return; }
                if (ArgParser.hasBrokenParens(raw)) {
                    msg(src, lang.get("broken-parens")); return;
                }
                if (!ArgParser.hasTokens(raw)) { showUsage(src, lang, cfg); return; }

                Map<String, String> parsed = ArgParser.parse(raw);
                if (parsed.isEmpty()) {
                    msg(src, lang.get("invalid-format")); return;
                }

                // Validate required placeholders
                for (String req : cfg.getStringList("placeholders")) {
                    if (!parsed.containsKey(req.toLowerCase())) {
                        msg(src, lang.get("missing-arg", Map.of("arg", req))); return;
                    }
                }

                // Record purchase for stats
                plugin.getStats().recordPurchase(parsed.getOrDefault("player", ""));

                final Map<String, String> finalParsed = parsed;
                plugin.getServer().getScheduler()
                        .buildTask(plugin, () -> new BroadcastService(plugin).broadcast(finalParsed))
                        .schedule();

                StringBuilder argList = new StringBuilder();
                parsed.forEach((k, v) -> argList.append("<").append(k).append("> "));
                msg(src, lang.get("console.available-args", Map.of("args", argList.toString().trim())));
            }
        }
    }

    // ── Tab complete ──────────────────────────────────────────────────────────

    @Override
    public List<String> suggest(Invocation inv) {
        String[] args  = inv.arguments();
        CommandSource src = inv.source();
        YamlConfig cfg = plugin.getConfigManager().get();
        List<String> out = new ArrayList<>();

        String cur = args.length > 0 ? args[args.length - 1].toLowerCase() : "";

        if (args.length <= 1) {
            for (String s : List.of("help","reload","test","top","announce","args","lang","sale")) {
                if (s.startsWith(cur)) out.add(s);
            }
            if (!(src instanceof Player)) {
                for (String ph : cfg.getStringList("placeholders")) {
                    String tok = ph + "()";
                    if (tok.startsWith(cur)) out.add(tok);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("announce")) {
            YamlConfig list = cfg.section("announcements.list");
            if (list != null) list.keys().stream().filter(k -> k.startsWith(cur)).forEach(out::add);
        }
        return out;
    }

    @Override
    public boolean hasPermission(Invocation inv) {
        if (!(inv.source() instanceof Player)) return true;
        return inv.source().hasPermission("plexytebex.admin")
                || inv.source().hasPermission("plexytebex.use");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean perm(CommandSource s, String specific) {
        // Console always has permission
        if (!(s instanceof Player)) return true;
        // plexytebex.admin grants access to everything
        if (s.hasPermission("plexytebex.admin")) return true;
        // Otherwise check the specific permission
        return s.hasPermission(specific);
    }
    private void deny(CommandSource s, LangManager l) { msg(s, l.get("no-permission")); }
    private void msg(CommandSource s, String raw)      { s.sendMessage(ColorUtil.parse(raw)); }

    private void showUsage(CommandSource s, LangManager lang, YamlConfig cfg) {
        List<String> phs = cfg.getStringList("placeholders");
        String usage = phs.isEmpty() ? "help | reload | test | top | announce <id>"
                : phs.stream().map(p -> p + "(value)").reduce((a, b) -> a + " " + b).orElse("");
        lang.getList("console.usage", Map.of("cmd", "plexytebex", "usage", usage))
                .forEach(l -> s.sendMessage(ColorUtil.parse(l)));
    }
}
