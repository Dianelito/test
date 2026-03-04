package dev.plexystudios.plexytebex.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ArgParser {

    private static final Pattern TOKEN = Pattern.compile("(\\w+)\\(([^)]*)\\)");

    private ArgParser() {}

    public static Map<String, String> parse(String[] args) {
        Map<String, String> out = new LinkedHashMap<>();
        Matcher m = TOKEN.matcher(String.join(" ", args));
        while (m.find()) out.put(m.group(1).toLowerCase(), m.group(2));
        return out;
    }

    public static boolean hasTokens(String[] args) {
        String j = String.join(" ", args);
        return j.contains("(") && j.contains(")");
    }

    public static boolean hasBrokenParens(String[] args) {
        String j = String.join(" ", args);
        return j.contains("(") ^ j.contains(")");
    }
}
