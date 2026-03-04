package dev.plexystudios.plexytebex.config;

import dev.plexystudios.plexytebex.core.PlexyTebex;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private final PlexyTebex plugin;
    private YamlConfig config;

    public ConfigManager(PlexyTebex plugin) {
        this.plugin = plugin;
    }

    public void load() {
        try {
            Path dir = plugin.getDataDirectory();
            if (!Files.exists(dir)) Files.createDirectories(dir);
            Path file = dir.resolve("config.yml");
            copyDefault(file, "/config.yml");
            config = new YamlConfig(file);
        } catch (IOException e) {
            plugin.getLogger().error("[PlexyTebex] Failed to load config.yml", e);
        }
    }

    public void reload() {
        if (config != null) config.reload();
    }

    public YamlConfig get() { return config; }

    public void copyDefault(Path target, String resource) throws IOException {
        if (Files.exists(target)) return;
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            if (in != null) Files.copy(in, target);
        }
    }
}
