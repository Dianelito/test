package dev.plexystudios.plexytebex.core;

import dev.plexystudios.plexytebex.announcement.AnnouncementService;
import dev.plexystudios.plexytebex.announcement.SaleBossBarService;
import dev.plexystudios.plexytebex.command.PlexyCommand;
import dev.plexystudios.plexytebex.config.ConfigManager;
import dev.plexystudios.plexytebex.i18n.LangManager;
import dev.plexystudios.plexytebex.stats.StatsManager;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id          = "plexytebex",
        name        = "PlexyTebex",
        version     = "2.0.0",
        description = "Advanced Tebex broadcast plugin for Velocity — PlexyStudios",
        url         = "https://github.com/ItzThiago401/PlexyTebex",
        authors     = {"ItzThiago401"}
)
public class PlexyTebex {

    private final ProxyServer server;
    private final Logger      logger;
    private final Path        dataDirectory;

    private ConfigManager       configManager;
    private LangManager         langManager;
    private StatsManager        statsManager;
    private AnnouncementService announcementService;
    private SaleBossBarService  saleBossBarService;

    @Inject
    public PlexyTebex(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server        = server;
        this.logger        = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent e) {
        // 1. Config
        configManager = new ConfigManager(this);
        configManager.load();

        // 2. Language
        langManager = new LangManager(this);
        langManager.load();

        // 3. Stats
        statsManager = new StatsManager(this);
        statsManager.load();

        // 4. Announcements
        announcementService = new AnnouncementService(this);
        announcementService.startAutoScheduler();

        // 5. Sale BossBar (persistent across servers)
        saleBossBarService = new SaleBossBarService(this);

        // 5. Commands
        server.getCommandManager().register(
                server.getCommandManager()
                        .metaBuilder("plexytebex")
                        .aliases("ptebex", "pt")
                        .plugin(this)
                        .build(),
                new PlexyCommand(this)
        );

        printBanner();
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent e) {
        if (announcementService != null) announcementService.stopAutoScheduler();
        if (saleBossBarService  != null) saleBossBarService.disable();
        if (statsManager        != null) statsManager.save();
        logger.info("[PlexyTebex] Disabled. Goodbye!");
    }

    // ── Banner ────────────────────────────────────────────────────────────────

    private void printBanner() {
        logger.info("------------------------------------------");
        logger.info("  PlexyTebex v2.0.0 by ItzThiago401");
        logger.info("  PlexyStudios");
        logger.info("  Language : {}", langManager.getCurrentCode().toUpperCase());
        logger.info("  Status   : Enabled");
        logger.info("------------------------------------------");
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public ProxyServer         getServer()              { return server; }
    public Logger              getLogger()              { return logger; }
    public Path                getDataDirectory()       { return dataDirectory; }
    public ConfigManager       getConfigManager()       { return configManager; }
    public LangManager         getLang()                { return langManager; }
    public StatsManager        getStats()               { return statsManager; }
    public AnnouncementService getAnnouncementService() { return announcementService; }
    public SaleBossBarService  getSaleBossBar()         { return saleBossBarService; }
}
