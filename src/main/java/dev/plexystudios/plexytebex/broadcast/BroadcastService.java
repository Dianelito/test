package dev.plexystudios.plexytebex.broadcast;

import dev.plexystudios.plexytebex.config.YamlConfig;
import dev.plexystudios.plexytebex.core.PlexyTebex;
import dev.plexystudios.plexytebex.discord.WebhookSender;
import dev.plexystudios.plexytebex.util.ColorUtil;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.bossbar.BossBar;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Executes a full purchase broadcast:
 * chat lines → actionbar (animated) → bossbar → title → sound → discord webhook.
 *
 * Fireworks note: Velocity is a proxy and has no access to world/entity APIs.
 * The fireworks feature sends a plugin message to the backend server where
 * the player is connected, which must handle it (e.g. via a companion Bukkit plugin).
 * The channel used is: plexytebex:fireworks  payload: player UUID as UTF-8 string.
 */
public class BroadcastService {

    private final PlexyTebex plugin;

    public BroadcastService(PlexyTebex plugin) { this.plugin = plugin; }

    public void broadcast(Map<String, String> args) {
        YamlConfig cfg = plugin.getConfigManager().get();
        Collection<Player> all = plugin.getServer().getAllPlayers();

        sendChat(cfg, all, args);
        sendActionBar(cfg, all, args);
        sendBossBar(cfg, all, args, "broadcast.bossbar");
        sendTitle(cfg, all, args);
        sendSound(cfg, all, "broadcast.effects.sound");
        sendDiscord(cfg, args);

        // Fireworks — notify backend for the buyer
        if (cfg.getBoolean("broadcast.effects.fireworks.enabled", false)) {
            String playerName = args.getOrDefault("player", "");
            plugin.getServer().getPlayer(playerName).ifPresent(p -> {
                int count = cfg.getInt("broadcast.effects.fireworks.count", 3);
                p.getCurrentServer().ifPresent(s -> {
                    // FIX: MinecraftChannelIdentifier implements ChannelIdentifier (required by Velocity API)
                    MinecraftChannelIdentifier channel =
                            MinecraftChannelIdentifier.from("plexytebex:fireworks");
                    byte[] payload = (playerName + ":" + count)
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    s.sendPluginMessage(channel, payload);
                });
            });
        }
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    private void sendChat(YamlConfig cfg, Collection<Player> players, Map<String, String> args) {
        ButtonBuilder buttons = new ButtonBuilder(cfg);
        // Read broadcast lines from lang file so language is respected
        List<String> lines = plugin.getLang().getList("broadcast.message", args);
        if (lines.isEmpty()) {
            // fallback to config if lang key missing
            lines = cfg.getStringList("broadcast.message");
        }
        for (String raw : lines) {
            Component line = raw.trim().equalsIgnoreCase("%buttons%")
                    ? buttons.build()
                    : LineParser.parse(raw, args);
            players.forEach(p -> p.sendMessage(line));
        }
    }

    // ── ActionBar ─────────────────────────────────────────────────────────────

    private void sendActionBar(YamlConfig cfg, Collection<Player> players, Map<String, String> args) {
        YamlConfig ab = cfg.section("broadcast.actionbar");
        if (ab == null || !ab.getBoolean("enabled", false)) return;

        List<String> frames = ab.getStringList("frames");
        if (frames.isEmpty()) return;

        int durationSec = ab.getInt("duration", 8);
        long totalTicks  = durationSec * 20L;

        // Schedule one task per frame-cycle (every 20 ticks = 1 sec, alternating frames)
        for (int tick = 0; tick < totalTicks; tick += 20) {
            final int frameTick = tick;
            plugin.getServer().getScheduler()
                    .buildTask(plugin, () -> {
                        int idx = (frameTick / 20) % frames.size();
                        Component bar = ColorUtil.parseWith(frames.get(idx), args);
                        players.forEach(p -> p.sendActionBar(bar));
                    })
                    .delay(tick * 50L, TimeUnit.MILLISECONDS)
                    .schedule();
        }
    }

    // ── BossBar ───────────────────────────────────────────────────────────────

    public void sendBossBar(YamlConfig cfg, Collection<Player> players,
                            Map<String, String> args, String sectionPath) {
        YamlConfig bb = cfg.section(sectionPath);
        if (bb == null || !bb.getBoolean("enabled", false)) return;

        String text  = ColorUtil.replacePlaceholders(bb.getString("text", ""), args);
        String color = bb.getString("color", "PURPLE").toUpperCase();
        String style = bb.getString("style", "SOLID").toUpperCase();
        int    dur   = bb.getInt("duration", 10);

        BossBar bar = BossBar.bossBar(
                ColorUtil.parse(text),
                1.0f,
                parseBossBarColor(color),
                parseBossBarOverlay(style)
        );

        players.forEach(p -> p.showBossBar(bar));

        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> players.forEach(p -> p.hideBossBar(bar)))
                .delay(dur, TimeUnit.SECONDS)
                .schedule();
    }

    // ── Title ─────────────────────────────────────────────────────────────────

    private void sendTitle(YamlConfig cfg, Collection<Player> players, Map<String, String> args) {
        YamlConfig t = cfg.section("broadcast.effects.title");
        if (t == null || !t.getBoolean("enabled", false)) return;

        // Read title text from lang file so language is respected
        String topRaw    = plugin.getLang().get("broadcast.title-top",    args);
        String bottomRaw = plugin.getLang().get("broadcast.title-bottom", args);
        // fallback to config if lang key missing
        if (topRaw.contains("Missing lang key")) topRaw    = t.getString("top",    "");
        if (bottomRaw.contains("Missing lang key")) bottomRaw = t.getString("bottom", "");

        Component top    = ColorUtil.parseWith(topRaw, args);
        Component bottom = ColorUtil.parseWith(bottomRaw, args);
        int fadeIn  = t.getInt("fade-in",  10);
        int stay    = t.getInt("stay",     60);
        int fadeOut = t.getInt("fade-out", 20);

        Title title = Title.title(top, bottom, Title.Times.times(
                Duration.ofMillis(fadeIn  * 50L),
                Duration.ofMillis(stay    * 50L),
                Duration.ofMillis(fadeOut * 50L)
        ));
        players.forEach(p -> p.showTitle(title));
    }

    // ── Sound ─────────────────────────────────────────────────────────────────

    public void sendSound(YamlConfig cfg, Collection<Player> players, String sectionPath) {
        YamlConfig s = cfg.section(sectionPath);
        if (s == null || !s.getBoolean("enabled", false)) return;

        String id  = s.getString("id", "minecraft:entity.player.levelup");
        float vol  = (float) s.getDouble("volume", 1.0);
        float pitch = (float) s.getDouble("pitch",  1.0);

        String key = id.contains(":") ? id.toLowerCase()
                : "minecraft:" + id.toLowerCase().replace('_', '.');
        try {
            Sound sound = Sound.sound(Key.key(key), Sound.Source.PLAYER, vol, pitch);
            players.forEach(p -> p.playSound(sound));
        } catch (Exception e) {
            plugin.getLogger().warn("[PlexyTebex] Invalid sound id: {}", id);
        }
    }

    // ── Discord ───────────────────────────────────────────────────────────────

    private void sendDiscord(YamlConfig cfg, Map<String, String> args) {
        if (!cfg.getBoolean("discord.enabled", false)) return;
        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> {
                    try { new WebhookSender(cfg.section("discord"), args).send(); }
                    catch (Exception e) { plugin.getLogger().error("[PlexyTebex] Webhook error", e); }
                }).schedule();
    }

    // ── Enum parsers ──────────────────────────────────────────────────────────

    private BossBar.Color parseBossBarColor(String s) {
        try { return BossBar.Color.valueOf(s); }
        catch (Exception e) { return BossBar.Color.PURPLE; }
    }

    private BossBar.Overlay parseBossBarOverlay(String s) {
        try { return BossBar.Overlay.valueOf(s); }
        catch (Exception e) { return BossBar.Overlay.PROGRESS; }
    }
}
