package dev.plexystudios.plexytebex.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Full colour parsing pipeline for PlexyTebex:
 *  1. &#RRGGBB  →  §x§R§R§G§G§B§B  (Spigot-style hex, works on modern Paper/Velocity)
 *  2. &-codes   →  §-codes           (legacy)
 *  3. MiniMessage tags               (<gradient:…>, <color:…>, <bold>, etc.)
 *
 * Adventure / MiniMessage is available natively on Velocity 3+.
 */
public final class ColorUtil {

    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder()
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private ColorUtil() {}

    // ── Parse ─────────────────────────────────────────────────────────────────

    /**
     * Returns a plain string safe for console output (no color codes).
     * Used by logger calls so the console doesn't show garbled characters.
     */
    public static String plainForConsole(String text) {
        if (text == null) return "";
        // Strip &#RRGGBB hex
        text = text.replaceAll("(?i)&#[A-F0-9]{6}", "");
        // Strip §x hex sequences
        text = text.replaceAll("§x(§[0-9a-fA-F]){6}", "");
        // Strip legacy & and § codes
        text = text.replaceAll("(?i)[&§][0-9A-FK-ORX]", "");
        // Strip MiniMessage tags
        text = text.replaceAll("<[^>]+>", "");
        return text.trim();
    }

    /** Full pipeline: &#hex → §x, then MiniMessage or legacy fallback. */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        text = expandHex(text);
        if (text.contains("<")) {
            try { return MM.deserialize(stripSection(text)); } catch (Exception ignored) {}
        }
        return LEGACY.deserialize(text);
    }

    /** Replace {key} placeholders then parse. */
    public static Component parseWith(String text, Map<String, String> args) {
        return parse(replacePlaceholders(text, args));
    }

    // ── Plain-string helpers ──────────────────────────────────────────────────

    public static String replacePlaceholders(String text, Map<String, String> args) {
        if (args == null || text == null) return text;
        for (Map.Entry<String, String> e : args.entrySet())
            text = text.replace("{" + e.getKey() + "}", e.getValue());
        return text;
    }

    /** Strips all colour codes and MiniMessage tags — for Discord / plain output. */
    public static String stripColors(String text) {
        if (text == null) return "";
        text = text.replaceAll("(?i)&#[A-F0-9]{6}", "");
        text = text.replaceAll("§x(§[0-9a-fA-F]){6}", "");
        text = text.replaceAll("(?i)[&§][0-9A-FK-ORX]", "");
        text = text.replaceAll("<[^>]+>", "");
        return text;
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static String expandHex(String text) {
        Matcher m = HEX.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String h = m.group(1);
            String rep = "§x§" + h.charAt(0) + "§" + h.charAt(1)
                       + "§" + h.charAt(2) + "§" + h.charAt(3)
                       + "§" + h.charAt(4) + "§" + h.charAt(5);
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String stripSection(String text) {
        text = text.replaceAll("§x(§[0-9a-fA-F]){6}", "");
        text = text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
        return text;
    }
}
