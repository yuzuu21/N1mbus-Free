package com.nimbus.sync;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * v3.0: High-performance bridge for HTML-based UI.
 * Now sends high-level model data instead of low-level draw commands.
 */
public class NimbusUiBridge {
    private static JsonArray currentData = new JsonArray();

    public static void begin() {
        currentData = new JsonArray();
    }

    public static void addPanel(String title, int x, int y, JsonArray modules, JsonObject state) {
        JsonObject panel = new JsonObject();
        panel.addProperty("title", title);
        panel.addProperty("x", x);
        panel.addProperty("y", y);
        panel.add("modules", modules);
        if (state != null) {
            panel.add("state", state);
        }
        currentData.add(panel);
    }

    public static JsonObject createModule(String name, boolean enabled, JsonArray settings) {
        JsonObject m = new JsonObject();
        m.addProperty("name", name);
        m.addProperty("enabled", enabled);
        if (settings != null) {
            m.add("settings", settings);
        }
        return m;
    }

    public static void send() {
        // Add window size metadata
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc != null && mc.getWindow() != null) {
            JsonObject meta = new JsonObject();
            meta.addProperty("_type", "windowSize");
            meta.addProperty("width", mc.getWindow().getWidth());
            meta.addProperty("height", mc.getWindow().getHeight());
            meta.addProperty("guiScale", mc.getWindow().getGuiScale());
            currentData.add(meta);
        }
        String json = currentData.toString();
        NimbusNative.submitUiCommands(json);
    }
}
