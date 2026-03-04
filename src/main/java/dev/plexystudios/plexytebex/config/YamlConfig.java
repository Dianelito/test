package dev.plexystudios.plexytebex.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Lightweight dot-notation YAML reader.
 * Wraps SnakeYAML (bundled in Velocity) — no extra deps needed.
 */
public class YamlConfig {

    private final Path path; // null for in-memory sections
    private Map<String, Object> data = new LinkedHashMap<>();

    public YamlConfig(Path path) {
        this.path = path;
        reload();
    }

    private YamlConfig(Map<String, Object> data) {
        this.path = null;
        this.data = data;
    }

    public void reload() {
        if (path == null) return;
        try (InputStream in = Files.newInputStream(path)) {
            Object loaded = new Yaml().load(in);
            if (loaded instanceof Map) {
                //noinspection unchecked
                data = (Map<String, Object>) loaded;
            } else {
                data = new LinkedHashMap<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Object get(String path) {
        String[] parts = path.split("\\.");
        Object cur = data;
        for (String p : parts) {
            if (cur instanceof Map) cur = ((Map<?, ?>) cur).get(p);
            else return null;
        }
        return cur;
    }

    public String getString(String path, String def) {
        Object v = get(path);
        return v != null ? v.toString() : def;
    }

    public boolean getBoolean(String path, boolean def) {
        Object v = get(path);
        if (v instanceof Boolean) return (Boolean) v;
        return def;
    }

    public int getInt(String path, int def) {
        Object v = get(path);
        if (v instanceof Number) return ((Number) v).intValue();
        return def;
    }

    public double getDouble(String path, double def) {
        Object v = get(path);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return def;
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String path) {
        Object v = get(path);
        if (!(v instanceof List)) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (Object o : (List<?>) v) out.add(o != null ? o.toString() : "");
        return out;
    }

    @SuppressWarnings("unchecked")
    public List<Map<?, ?>> getMapList(String path) {
        Object v = get(path);
        if (!(v instanceof List)) return Collections.emptyList();
        List<Map<?, ?>> out = new ArrayList<>();
        for (Object o : (List<?>) v) { if (o instanceof Map) out.add((Map<?, ?>) o); }
        return out;
    }

    @SuppressWarnings("unchecked")
    public YamlConfig section(String path) {
        Object v = get(path);
        if (v instanceof Map) return new YamlConfig((Map<String, Object>) v);
        return null;
    }

    public Set<String> keys() { return data.keySet(); }
}
