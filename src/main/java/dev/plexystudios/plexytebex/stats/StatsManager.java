package dev.plexystudios.plexytebex.stats;

import dev.plexystudios.plexytebex.core.PlexyTebex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Simple JSON-based stats storage for purchase counts per player.
 * Uses hand-written JSON to avoid extra dependencies.
 *
 * File format:
 * {
 *   "month": "2025-01",
 *   "counts": {
 *     "Steve": 5,
 *     "Alex": 3
 *   }
 * }
 */
public class StatsManager {

    private final PlexyTebex plugin;
    private final Path statsFile;

    private String currentMonth = "";
    private final Map<String, Integer> counts = new LinkedHashMap<>();

    public StatsManager(PlexyTebex plugin) {
        this.plugin    = plugin;
        this.statsFile = plugin.getDataDirectory()
                .resolve(plugin.getConfigManager().get().getString("general.stats-file", "stats.json"));
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void load() {
        if (!Files.exists(statsFile)) { currentMonth = thisMonth(); return; }
        try {
            String json = Files.readString(statsFile, StandardCharsets.UTF_8);
            String month = extractString(json, "month");
            currentMonth = (month != null) ? month : thisMonth();

            // Auto-reset if configured and month changed
            String resetPolicy = plugin.getConfigManager().get().getString("top.reset", "MONTHLY");
            if ("MONTHLY".equalsIgnoreCase(resetPolicy) && !currentMonth.equals(thisMonth())) {
                counts.clear();
                currentMonth = thisMonth();
                save();
                return;
            }

            // Parse counts object
            Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\\d+)");
            boolean inCounts = false;
            for (String line : json.split("\n")) {
                if (line.contains("\"counts\"")) { inCounts = true; continue; }
                if (inCounts) {
                    Matcher m = p.matcher(line);
                    if (m.find()) counts.put(m.group(1), Integer.parseInt(m.group(2)));
                    if (line.contains("}")) break;
                }
            }
        } catch (IOException e) {
            plugin.getLogger().error("[PlexyTebex] Failed to load stats.json", e);
        }
    }

    public void save() {
        try {
            StringBuilder sb = new StringBuilder("{\n");
            sb.append("  \"month\": \"").append(currentMonth).append("\",\n");
            sb.append("  \"counts\": {\n");
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
            for (int i = 0; i < entries.size(); i++) {
                sb.append("    \"").append(entries.get(i).getKey())
                  .append("\": ").append(entries.get(i).getValue());
                if (i < entries.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  }\n}");
            Files.writeString(statsFile, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().error("[PlexyTebex] Failed to save stats.json", e);
        }
    }

    // ── API ───────────────────────────────────────────────────────────────────

    /** Record a purchase for a player. */
    public void recordPurchase(String player) {
        if (player == null || player.isBlank()) return;
        // Auto-reset on new month
        if (!thisMonth().equals(currentMonth)) {
            String policy = plugin.getConfigManager().get().getString("top.reset", "MONTHLY");
            if ("MONTHLY".equalsIgnoreCase(policy)) {
                counts.clear();
                currentMonth = thisMonth();
            }
        }
        counts.merge(player, 1, Integer::sum);
        save();
    }

    /** Returns top N players sorted by purchase count (descending). */
    public List<Map.Entry<String, Integer>> getTop(int limit) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String thisMonth() {
        java.time.YearMonth ym = java.time.YearMonth.now();
        return ym.getYear() + "-" + String.format("%02d", ym.getMonthValue());
    }

    private String extractString(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
