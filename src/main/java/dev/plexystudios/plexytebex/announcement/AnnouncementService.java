package dev.plexystudios.plexytebex.announcement;

import dev.plexystudios.plexytebex.broadcast.BroadcastService;
import dev.plexystudios.plexytebex.broadcast.ButtonBuilder;
import dev.plexystudios.plexytebex.broadcast.LineParser;
import dev.plexystudios.plexytebex.config.YamlConfig;
import dev.plexystudios.plexytebex.core.PlexyTebex;
import dev.plexystudios.plexytebex.util.ColorUtil;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Handles discount/sale announcements.
 *
 * Announcements are defined under {@code announcements.list} in config.yml.
 * They can be triggered manually with  /plexytebex announce <id>
 * or automatically on the interval set at  announcements.auto-interval  (minutes).
 */
public class AnnouncementService {

    private final PlexyTebex plugin;
    private ScheduledTask autoTask;

    public AnnouncementService(PlexyTebex plugin) { this.plugin = plugin; }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void startAutoScheduler() {
        stopAutoScheduler();
        int intervalMin = plugin.getConfigManager().get().getInt("announcements.auto-interval", 0);
        if (intervalMin <= 0) return;

        autoTask = plugin.getServer().getScheduler()
                .buildTask(plugin, this::broadcastAll)
                .delay(intervalMin, TimeUnit.MINUTES)
                .repeat(intervalMin, TimeUnit.MINUTES)
                .schedule();

        plugin.getLogger().info("[PlexyTebex] Auto-announcements every {} min.", intervalMin);
    }

    public void stopAutoScheduler() {
        if (autoTask != null) { autoTask.cancel(); autoTask = null; }
    }

    private void broadcastAll() {
        YamlConfig list = plugin.getConfigManager().get().section("announcements.list");
        if (list == null) return;
        for (String id : list.keys()) {
            YamlConfig ann = list.section(id);
            if (ann != null && ann.getBoolean("enabled", false)) send(id, ann);
        }
    }

    // ── Manual trigger ────────────────────────────────────────────────────────

    /**
     * Returns true if the announcement was found and sent, false if not found.
     */
    public boolean trigger(String id) {
        YamlConfig list = plugin.getConfigManager().get().section("announcements.list");
        if (list == null) return false;
        YamlConfig ann = list.section(id);
        if (ann == null) return false;
        send(id, ann);
        return true;
    }

    // ── Internal sender ───────────────────────────────────────────────────────

    private void send(String id, YamlConfig ann) {
        Collection<Player> all = plugin.getServer().getAllPlayers();
        YamlConfig cfg = plugin.getConfigManager().get();
        BroadcastService bs = new BroadcastService(plugin);
        ButtonBuilder buttons = new ButtonBuilder(cfg);

        // Chat lines
        List<String> lines = ann.getStringList("message");
        for (String raw : lines) {
            var comp = raw.trim().equalsIgnoreCase("%buttons%")
                    ? buttons.build()
                    : LineParser.parse(raw, Map.of());
            all.forEach(p -> p.sendMessage(comp));
        }

        // BossBar
        YamlConfig bb = ann.section("bossbar");
        if (bb != null && bb.getBoolean("enabled", false)) {
            bs.sendBossBar(cfg, all, Map.of(), "announcements.list." + id + ".bossbar");
        }

        // Sound
        YamlConfig sound = ann.section("sound");
        if (sound != null && sound.getBoolean("enabled", false)) {
            bs.sendSound(cfg, all, "announcements.list." + id + ".sound");
        }

        plugin.getLogger().info("[PlexyTebex] Announcement '{}' broadcast.", id);
    }
}
