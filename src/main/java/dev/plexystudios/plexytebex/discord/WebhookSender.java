package dev.plexystudios.plexytebex.discord;

import dev.plexystudios.plexytebex.config.YamlConfig;
import dev.plexystudios.plexytebex.util.ColorUtil;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class WebhookSender {

    private final YamlConfig cfg;
    private final Map<String, String> args;

    public WebhookSender(YamlConfig cfg, Map<String, String> args) {
        this.cfg  = cfg;
        this.args = args;
    }

    public void send() throws Exception {
        String url = cfg != null ? cfg.getString("url", "") : "";
        if (url.isEmpty() || url.equalsIgnoreCase("INSERT-WEBHOOK-URL")) return;

        byte[] body = buildPayload().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", "PlexyTebex/2.0");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) { os.write(body); }
        try { conn.getInputStream().close(); } catch (Exception ignored) {}
    }

    private String buildPayload() {
        StringBuilder j = new StringBuilder("{\"embeds\":[{");

        String color = cfg.getString("color", "#268EF4");
        if (color.startsWith("#")) j.append("\"color\":").append(Integer.parseInt(color.substring(1), 16)).append(",");

        j.append("\"title\":\"").append(esc(apply(cfg.getString("title", "")))).append("\",");

        List<String> desc = cfg.getStringList("description");
        if (!desc.isEmpty()) j.append("\"description\":\"").append(esc(apply(String.join("\\n", desc)))).append("\",");

        List<Map<?, ?>> fields = cfg.getMapList("fields");
        if (!fields.isEmpty()) {
            j.append("\"fields\":[");
            for (Map<?, ?> f : fields) {
                j.append("{\"name\":\"").append(esc(apply(s(f.get("name"))))).append("\",")
                 .append("\"value\":\"").append(esc(apply(s(f.get("value"))))).append("\",")
                 .append("\"inline\":").append(Boolean.parseBoolean(s(f.get("inline")))).append("},");
            }
            trim(j); j.append("],");
        }

        String footer = apply(cfg.getString("footer", ""));
        if (!footer.isEmpty()) j.append("\"footer\":{\"text\":\"").append(esc(footer)).append("\"},");

        String thumb = apply(cfg.getString("thumbnail", ""));
        if (!thumb.isEmpty()) j.append("\"thumbnail\":{\"url\":\"").append(esc(thumb)).append("\"},");

        String img = apply(cfg.getString("image", ""));
        if (!img.isEmpty()) j.append("\"image\":{\"url\":\"").append(esc(img)).append("\"},");

        trim(j); j.append("}]}");
        return j.toString();
    }

    private String apply(String t) {
        if (t == null) return "";
        return ColorUtil.stripColors(ColorUtil.replacePlaceholders(t, args));
    }

    private static String s(Object o) { return o == null ? "" : o.toString(); }
    private static void   trim(StringBuilder sb) { if (sb.length() > 0 && sb.charAt(sb.length()-1)==',') sb.deleteCharAt(sb.length()-1); }
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","");
    }
}
