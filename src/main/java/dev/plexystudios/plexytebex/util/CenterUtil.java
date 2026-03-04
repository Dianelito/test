package dev.plexystudios.plexytebex.util;

public final class CenterUtil {

    private static final int CENTER_PX   = 154;
    private static final int SPACE_WIDTH = 4;

    private CenterUtil() {}

    public static String center(String line) {
        if (line == null || line.trim().isEmpty()) return line;
        line = line.replaceAll("(?i)</?center>", "").trim();
        String visible = ColorUtil.stripColors(line).replaceAll("<[^>]+>", "");
        int pixels = measurePixels(visible);
        int spaces = Math.max(0, (CENTER_PX - pixels / 2) / SPACE_WIDTH);
        return " ".repeat(spaces) + line;
    }

    private static int measurePixels(String text) {
        int total = 0;
        boolean bold = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                bold = Character.toLowerCase(text.charAt(i + 1)) == 'l';
                i++; continue;
            }
            int w = charWidth(c);
            total += bold ? Math.min(w + 1, 6) : w;
            total++;
        }
        return total;
    }

    private static int charWidth(char c) {
        return switch (c) {
            case 'i', '!', '|', '\'', ';', ':' -> 1;
            case 'l'                             -> 2;
            case '`', 't', 'r', ' '             -> 3;
            case 'f', 'k', '"', '*', '(', ')', '[', ']', '{', '}' -> 4;
            default                              -> 5;
        };
    }
}
