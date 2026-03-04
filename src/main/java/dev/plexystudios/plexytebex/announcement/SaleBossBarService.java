package dev.plexystudios.plexytebex.announcement;

import dev.plexystudios.plexytebex.core.PlexyTebex;
import dev.plexystudios.plexytebex.util.ColorUtil;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

import java.util.Collection;

/**
 * Manages a single persistent BossBar shown to ALL players across the entire network.
 *
 * Because Velocity is a proxy, BossBars are per-connection. When a player switches
 * backend servers, the BossBar disappears. We fix this by:
 *  1. Storing the active BossBar in memory.
 *  2. Listening to ServerConnectedEvent and re-showing it to every player that connects.
 *  3. Listening to DisconnectEvent and hiding it cleanly.
 *
 * Activated with:  /plexytebex sale on  [text] [color]
 * Deactivated with: /plexytebex sale off
 */
public class SaleBossBarService {

    private final PlexyTebex plugin;

    // The single active BossBar (null = no sale active)
    private BossBar activeBossBar = null;
    private boolean listenerRegistered = false;

    public SaleBossBarService(PlexyTebex plugin) {
        this.plugin = plugin;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Activates the sale BossBar with the given text and color.
     * Shows it to all currently online players immediately.
     */
    public void enable(String text, String colorName) {
        // If one is already active, remove it first
        disable();

        Component component = ColorUtil.parse(text);
        BossBar.Color color = parseColor(colorName);

        activeBossBar = BossBar.bossBar(component, 1.0f, color, BossBar.Overlay.PROGRESS);

        // Show to all currently online players
        for (Player p : plugin.getServer().getAllPlayers()) {
            p.showBossBar(activeBossBar);
        }

        // Register listener if not already done
        if (!listenerRegistered) {
            plugin.getServer().getEventManager().register(plugin, this);
            listenerRegistered = true;
        }

        plugin.getLogger().info("[PlexyTebex] Sale BossBar enabled.");
    }

    /**
     * Deactivates the sale BossBar and hides it from all players.
     */
    public void disable() {
        if (activeBossBar == null) return;

        for (Player p : plugin.getServer().getAllPlayers()) {
            p.hideBossBar(activeBossBar);
        }

        activeBossBar = null;
        plugin.getLogger().info("[PlexyTebex] Sale BossBar disabled.");
    }

    public boolean isActive() {
        return activeBossBar != null;
    }

    // ── Velocity Events ───────────────────────────────────────────────────────

    /**
     * Re-show the BossBar whenever a player connects to any backend server.
     * This is what makes it "persistent" across server switches.
     */
    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        if (activeBossBar == null) return;
        event.getPlayer().showBossBar(activeBossBar);
    }

    /**
     * Clean up when a player disconnects from the proxy entirely.
     */
    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (activeBossBar == null) return;
        event.getPlayer().hideBossBar(activeBossBar);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private BossBar.Color parseColor(String name) {
        if (name == null) return BossBar.Color.YELLOW;
        try { return BossBar.Color.valueOf(name.toUpperCase()); }
        catch (Exception e) { return BossBar.Color.YELLOW; }
    }
}
