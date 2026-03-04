package dev.plexystudios.plexytebex.i18n;

import dev.plexystudios.plexytebex.config.YamlConfig;
import dev.plexystudios.plexytebex.core.PlexyTebex;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Loads the active language file and provides translated messages.
 * The {prefix} token in any message is replaced with the lang file's prefix.
 * Supported: en, es, pt, fr, de.
 */
public class LangManager {

    private static final String[] SUPPORTED = {"en", "es", "pt", "fr", "de"};

    private final PlexyTebex plugin;
    private YamlConfig lang;
    private String current = "en";
    private String prefix  = "";

    public LangManager(PlexyTebex plugin) {
        this.plugin = plugin;
    }

    public void load() {
        current = resolveCode(plugin.getConfigManager().get().getString("general.language", "en"));
        Path langDir = plugin.getDataDirectory().resolve("lang");

        try {
            if (!java.nio.file.Files.exists(langDir))
                java.nio.file.Files.createDirectories(langDir);

            for (String code : SUPPORTED) {
                Path target = langDir.resolve(code + ".yml");
                plugin.getConfigManager().copyDefault(target, "/lang/" + code + ".yml");
            }

            lang   = new YamlConfig(langDir.resolve(current + ".yml"));
            // Full colored prefix for in-game display
            prefix = lang.getString("prefix", "&9&lPlexyTebex &8» &b");
        } catch (IOException e) {
            plugin.getLogger().error("[PlexyTebex] Failed to load language: " + current, e);
        }
    }

    public void reload() { load(); }

    // ── Message retrieval ─────────────────────────────────────────────────────

    /** Single string, with {prefix} and {key→value} substituted. */
    public String get(String key, Map<String, String> vars) {
        String raw = lang != null ? lang.getString("messages." + key, null) : null;
        if (raw == null) raw = "&#FF5555[PlexyTebex] Missing lang key: " + key;
        return applyVars(raw.replace("{prefix}", prefix), vars);
    }

    public String get(String key) { return get(key, Map.of()); }

    /** Multi-line list, each line has {prefix} and vars applied. */
    public List<String> getList(String key, Map<String, String> vars) {
        List<String> lines = lang != null ? lang.getStringList("messages." + key) : List.of();
        return lines.stream()
                .map(l -> applyVars(l.replace("{prefix}", prefix), vars))
                .toList();
    }

    public List<String> getList(String key) { return getList(key, Map.of()); }

    public String getCurrentCode() { return current; }
    public String getPrefix()      { return prefix; }

    /**
     * Returns a plain-text version of a message suitable for console logger output.
     * Strips all MiniMessage tags and color codes.
     */
    public String getPlain(String key, Map<String, String> vars) {
        return dev.plexystudios.plexytebex.util.ColorUtil.plainForConsole(get(key, vars));
    }

    public String getPlain(String key) { return getPlain(key, Map.of()); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String applyVars(String text, Map<String, String> vars) {
        if (vars == null) return text;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            text = text.replace("{" + e.getKey() + "}", e.getValue());
        }
        return text;
    }

    private String resolveCode(String raw) {
        if (raw == null) return "en";
        String lower = raw.trim().toLowerCase();
        for (String code : SUPPORTED) if (code.equals(lower)) return code;
        plugin.getLogger().warn("[PlexyTebex] Unknown language '{}', defaulting to 'en'.", raw);
        return "en";
    }
}
