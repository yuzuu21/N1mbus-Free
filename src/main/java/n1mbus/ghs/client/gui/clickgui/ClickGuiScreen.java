package n1mbus.ghs.client.gui.clickgui;

import n1mbus.ghs.client.api.N1mbusGhsApi;
import n1mbus.ghs.client.api.WidgetStyle;
import n1mbus.ghs.client.module.Category;
import n1mbus.ghs.client.module.Module;
import n1mbus.ghs.client.module.ModuleManager;
import n1mbus.ghs.client.module.setting.BooleanSetting;
import n1mbus.ghs.client.module.setting.ModeSetting;
import n1mbus.ghs.client.module.setting.NumberSetting;
import n1mbus.ghs.client.module.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.glfw.GLFW;
import n1mbus.ghs.client.gui.RoundedUiRenderer;
import com.nimbus.sync.NimbusUiBridge;
import com.nimbus.sync.NimbusNative;
import com.google.gson.JsonArray;

public class ClickGuiScreen extends Screen {
    private static final int ACCENT = 0xFF539EFF;
    private static final int TEXT = 0xFFEFF4F8;
    private static final int TEXT_MUTED = 0xFF93A0B3;
    private static final ResourceLocation HEADER_FONT = ResourceLocation.fromNamespaceAndPath("nimbus", "ui_rubik_bold");
    private static final ResourceLocation BODY_FONT = ResourceLocation.fromNamespaceAndPath("nimbus", "ui_rubik_medium");
    private static final ResourceLocation ICON_FONT = ResourceLocation.fromNamespaceAndPath("nimbus", "icons_tabler_outline");

    private static final java.util.Map<Category, java.awt.Point> PANEL_POSITIONS = new java.util.HashMap<>();
    private static final java.util.Map<String, HtmlPanelState> HTML_PANEL_STATES = new java.util.HashMap<>();
    public static long lastCloseTime = 0;
    private final List<Panel> panels = new ArrayList<>();
    
    private final long openedAtNanos = System.nanoTime();
    private long lastFrameNanos = openedAtNanos;
    private boolean closing;
    private long closingAtNanos = -1L;

    private float sidebarAmount = 0.0f;
    private float sidebarScroll = 0.0f;
    private float targetSidebarScroll = 0.0f;
    private boolean sidebarHovered = false;
    private String selectedTheme = "Midnight Glass";
    private boolean showThemeCombo = false;
    private float handleGlow = 0.0f;
    private float themeListGlow = 0.0f;

    public ClickGuiScreen() {
        super(Component.literal("N1mbusGhs Reglass Edition"));
    }

    private boolean originalPauseOnLostFocus;
    public static boolean useHtmlMode = true;

    @Override
    protected void init() {
        super.init();
        rebuildPanels();
        
        if (useHtmlMode) {
            try {
                var res = minecraft.getResourceManager().getResource(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("nimbus", "gui/index.html"));
                if (res.isPresent()) {
                    try (var in = res.get().open()) {
                        String html = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        NimbusNative.setHtml(html);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            NimbusNative.setInteractive(true);
        }

        this.originalPauseOnLostFocus = minecraft.options.pauseOnLostFocus;
        minecraft.options.pauseOnLostFocus = false;
    }

    @Override
    public void tick() {
        super.tick();
        if (NimbusNative.checkCloseRequest()) {
            this.onClose();
        }
        
        if (useHtmlMode) {
            String msgs = NimbusNative.pollIpcMessages();
            if (msgs != null && !msgs.isEmpty()) {
                try {
                    JsonArray arr = com.google.gson.JsonParser.parseString(msgs).getAsJsonArray();
                    for (com.google.gson.JsonElement el : arr) {
                        com.google.gson.JsonObject obj = el.getAsJsonObject();
                        if (obj.has("action")) {
                            String action = obj.get("action").getAsString();
                            
                            if (action.equals("switchToNative")) {
                                useHtmlMode = false;
                                NimbusNative.setInteractive(false);
                                NimbusNative.setHtml("");
                                return;
                            }
                            if (action.equals("savePanelState")) {
                                String title = obj.has("title") ? obj.get("title").getAsString() : "";
                                if (!title.isEmpty()) {
                                    HtmlPanelState st = HTML_PANEL_STATES.computeIfAbsent(title, k -> new HtmlPanelState());
                                    if (obj.has("x")) st.x = obj.get("x").getAsInt();
                                    if (obj.has("y")) st.y = obj.get("y").getAsInt();
                                    if (obj.has("width")) st.width = obj.get("width").getAsString();
                                    if (obj.has("closed")) st.closed = obj.get("closed").getAsBoolean();
                                }
                                continue;
                            }
                            if (action.equals("saveModuleOpenState")) {
                                String title = obj.has("title") ? obj.get("title").getAsString() : "";
                                String moduleName = obj.has("module") ? obj.get("module").getAsString() : "";
                                if (!title.isEmpty() && !moduleName.isEmpty()) {
                                    HtmlPanelState st = HTML_PANEL_STATES.computeIfAbsent(title, k -> new HtmlPanelState());
                                    boolean open = obj.has("open") && obj.get("open").getAsBoolean();
                                    st.moduleOpen.put(moduleName, open);
                                }
                                continue;
                            }

                            if (!obj.has("module")) continue;
                            String modName = obj.get("module").getAsString();
                            Module m = findModule(modName);
                            if (m != null) {
                                switch (action) {
                                    case "toggle" -> m.toggle();
                                    case "setBool" -> {
                                        Setting<?> s = findSetting(m, obj.get("setting").getAsString());
                                        if (s instanceof BooleanSetting bs) bs.setValue(obj.get("value").getAsBoolean());
                                    }
                                    case "setNumber" -> {
                                        Setting<?> s = findSetting(m, obj.get("setting").getAsString());
                                        if (s instanceof NumberSetting ns) ns.setValue(obj.get("value").getAsDouble());
                                        else if (s instanceof n1mbus.ghs.client.module.setting.ColorSetting cs) cs.setValue(obj.get("value").getAsInt());
                                    }
                                    case "setMode" -> {
                                        Setting<?> s = findSetting(m, obj.get("setting").getAsString());
                                        if (s instanceof ModeSetting ms) ms.setValue(obj.get("value").getAsString());
                                    }
                                    case "setBind" -> {
                                        String settingName = obj.get("setting").getAsString();
                                        // Support both "Bind" (from module row) and "MainBind" (legacy)
                                        String lookupName = settingName.equals("MainBind") ? "Bind" : settingName;
                                        Setting<?> s = findSetting(m, lookupName);
                                        if (s instanceof n1mbus.ghs.client.module.setting.BindSetting bs) {
                                            int key = obj.get("value").getAsInt();
                                            bs.setValue(key);
                                        }
                                    }

                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Module findModule(String name) {
        String cleanName = name.replace(" ", "").toLowerCase();
        for (Category cat : Category.values()) {
            for (Module m : ModuleManager.INSTANCE.getModulesInCategory(cat)) {
                if (m.getName().replace(" ", "").toLowerCase().equals(cleanName)) return m;
            }
        }
        return null;
    }

    private Setting<?> findSetting(Module m, String name) {
        String cleanName = name.replace(" ", "").toLowerCase();
        for (Setting<?> s : m.getSettings()) {
            if (s.getName().replace(" ", "").toLowerCase().equals(cleanName)) return s;
        }
        return null;
    }

    private static final class HtmlPanelState {
        int x; int y; String width; boolean closed;
        final java.util.Map<String, Boolean> moduleOpen = new java.util.HashMap<>();
    }

    @Override
    public void resize(net.minecraft.client.Minecraft client, int width, int height) {
        super.resize(client, width, height);
    }

    @Override
    public void removed() {
        if (useHtmlMode) {
            NimbusUiBridge.begin();
            NimbusUiBridge.send();
            NimbusNative.setInteractive(false);
        }
        lastCloseTime = System.currentTimeMillis();
        minecraft.options.pauseOnLostFocus = this.originalPauseOnLostFocus;
        super.removed();
    }

    private void rebuildPanels() {
        panels.clear();
        int gap = 15; int panelHeight = 14; int panelWidth = 210;
        int x = 10; int y = 40;
        for (int i = 0; i < Category.values().length; i++) {
            Category category = Category.values()[i];
            java.awt.Point pos = PANEL_POSITIONS.get(category);
            int finalX = (pos != null) ? pos.x : x;
            int finalY = (pos != null) ? pos.y : y;
            panels.add(new Panel(category, i, finalX, finalY, panelWidth, panelHeight, true));
            x += panelWidth + gap;
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float elapsedSeconds = (now - openedAtNanos) / 1_000_000_000.0f;
        float closeElapsedSeconds = closing ? (now - closingAtNanos) / 1_000_000_000.0f : 0.0f;
        float dtSeconds = Math.min(0.05f, Math.max(0.0f, (now - lastFrameNanos) / 1_000_000_000.0f));
        lastFrameNanos = now;

        if (closing && closeElapsedSeconds >= 0.18f) {
            if (minecraft.screen == this) minecraft.setScreen(null);
            return;
        }

        if (useHtmlMode) {
            NimbusUiBridge.begin();
            int sidebarX = width - Math.round(sidebarWidth() * sidebarAmount);
            for (Panel panel : panels) {
                int xOffset = 0;
                if (sidebarAmount > 0.01f) {
                    float distToSidebar = (sidebarX + 5) - (panel.x + panel.width);
                    if (distToSidebar < 20) xOffset = Math.round((20 - distToSidebar) * sidebarAmount);
                }
                syncPanelToRust(panel, mouseX, mouseY, elapsedSeconds, 1.0f, xOffset);
            }
            NimbusUiBridge.send();
        } else {
            renderBackground(context, mouseX, mouseY, delta);
            for (Panel panel : panels) {
                panel.render(context, mouseX, mouseY, delta, elapsedSeconds, closeElapsedSeconds, dtSeconds, closing, panels.size(), 1.0f, 0);
            }
            boolean hov = mouseX >= width - 110 && mouseX <= width - 10 && mouseY >= 10 && mouseY <= 28;
            RoundedUiRenderer.drawRoundedRect(context, width - 110, 10, 100, 18, 4f, hov ? 0xFF539EFF : 0xFF1A2A42);
            context.drawString(font, bodyComponent("Switch to HTML"), width - 100, 15, 0xFFFFFFFF, false);
        }
        if (useHtmlMode) renderSidebar(context, mouseX, mouseY, dtSeconds);
    }

    private void syncPanelToRust(Panel panel, int mouseX, int mouseY, float elapsed, float alpha, int xOff) {
        JsonArray modules = new JsonArray();
        for (ModuleButton mb : panel.buttons) {
            JsonArray settingsArr = new JsonArray();
            for (Setting<?> s : mb.module.getSettings()) {
                com.google.gson.JsonObject sObj = new com.google.gson.JsonObject();
                sObj.addProperty("name", s.getName());
                if (s instanceof BooleanSetting bs) {
                    sObj.addProperty("type", "bool");
                    sObj.addProperty("value", bs.getValue());
                } else if (s instanceof NumberSetting ns) {
                    sObj.addProperty("type", "number");
                    sObj.addProperty("value", ns.getValue());
                    sObj.addProperty("min", ns.getMin());
                    sObj.addProperty("max", ns.getMax());
                } else if (s instanceof ModeSetting ms) {
                    sObj.addProperty("type", "mode");
                    sObj.addProperty("value", ms.getValue());
                    JsonArray opts = new JsonArray();
                    for (String mode : ms.getModes()) opts.add(mode);
                    sObj.add("options", opts);
                } else if (s instanceof n1mbus.ghs.client.module.setting.ColorSetting cs) {
                    sObj.addProperty("type", "color");
                    sObj.addProperty("value", cs.getValue());
                } else if (s instanceof n1mbus.ghs.client.module.setting.BindSetting bs) {
                    sObj.addProperty("type", "bind");
                    sObj.addProperty("value", bs.getValue());
                    sObj.addProperty("keyName", bs.getKeyName());
                }
                settingsArr.add(sObj);
            }
            modules.add(NimbusUiBridge.createModule(mb.module.getName(), mb.module.isEnabled(), settingsArr));
        }
        
        String title = displayName(panel.category);
        HtmlPanelState st = HTML_PANEL_STATES.get(title);
        int sendX = (st != null) ? st.x : (panel.x - xOff);
        int sendY = (st != null) ? st.y : panel.y;
        com.google.gson.JsonObject state = null;
        if (st != null) {
            state = new com.google.gson.JsonObject();
            if (st.width != null) state.addProperty("width", st.width);
            state.addProperty("closed", st.closed);
            if (!st.moduleOpen.isEmpty()) {
                com.google.gson.JsonObject mo = new com.google.gson.JsonObject();
                for (var e : st.moduleOpen.entrySet()) mo.addProperty(e.getKey(), e.getValue());
                state.add("moduleOpen", mo);
            }
        }
        NimbusUiBridge.addPanel(title, sendX, sendY, modules, state);
    }

    private void renderSidebar(GuiGraphics context, int mouseX, int mouseY, double dt) {
        int sidebarWidth = sidebarWidth();
        sidebarHovered = mouseX >= width - (sidebarAmount > 0.1f ? sidebarWidth : 20);
        sidebarAmount = smoothTo(sidebarAmount, sidebarHovered ? 1.0f : 0.0f, (float)dt, 0.1f);
        int x = width - Math.round(sidebarWidth * sidebarAmount);
        if (sidebarAmount <= 0.01f) return;
        RoundedUiRenderer.drawRoundedRect(context, x, 0, sidebarWidth, height, 0f, applyAlpha(0x0B1220, 0.88f * sidebarAmount));
    }

    private int sidebarWidth() { return 166; }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        float visibility = Math.min(easeOutCubic(clamp01((System.nanoTime() - openedAtNanos) / 220_000_000.0f)), closing ? 1.0f - easeInCubic(clamp01((System.nanoTime() - closingAtNanos) / 240_000_000.0f)) : 1.0f);
        context.fillGradient(0, 0, width, height, applyAlpha(0x4804070B, visibility), applyAlpha(0x3E06090E, visibility));
    }

    private static MutableComponent bodyComponent(String value) { return Component.literal(value).setStyle(Style.EMPTY.withFont(BODY_FONT)); }
    private static String displayName(Category category) {
        return switch (category) {
            case COMBAT -> "Combat";
            case MOVEMENT -> "Movement";
            case VISUALS -> "Visual";
            case WORLD -> "World";
            case MISC -> "Player";
        };
    }


    private static float clamp01(float v) { return Math.max(0, Math.min(1, v)); }
    private static float easeOutCubic(float t) { float f = 1.0f - t; return 1.0f - f * f * f; }
    private static float easeInCubic(float t) { return t * t * t; }
    private static float smoothTo(float c, float t, float dt, float tau) { float alpha = 1.0f - (float) Math.exp(-dt / tau); return c + (t - c) * alpha; }
    private static int applyAlpha(int c, float a) { int ai = Math.round(((c >>> 24) & 0xFF) * clamp01(a)); return (ai << 24) | (c & 0x00FFFFFF); }

    public void onClose() { if (!closing) { closing = true; closingAtNanos = System.nanoTime(); } }
    @Override public boolean keyPressed(int k, int s, int m) { if (k == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; } return super.keyPressed(k, s, m); }

    private class Panel {
        private final Category category;
        private final List<ModuleButton> buttons = new ArrayList<>();
        private int x, y, width, height;
        public Panel(Category cat, int index, int x, int y, int w, int h, boolean compact) {
            this.category = cat; this.x = x; this.y = y; this.width = w; this.height = h;
            for (Module m : ModuleManager.INSTANCE.getModulesInCategory(cat)) buttons.add(new ModuleButton(m, x, y, w, 14));
        }
        public void render(GuiGraphics c, int mx, int my, float d, float e, float ce, float dt, boolean cls, int count, float alpha, int off) {}
        public boolean mouseClicked(double mx, double my, int b) { return false; }
        public void mouseReleased(double mx, double my, int b) {}
        public boolean mouseDragged(double mx, double my, int b, double dx, double dy) { return false; }
    }

    private class ModuleButton {
        private final Module module;
        private int x, y, width, height, baseHeight;
        public ModuleButton(Module m, int x, int y, int w, int bh) { this.module = m; this.x = x; this.y = y; this.width = w; this.baseHeight = bh; }
        public boolean mouseDragged(double mx, double my, int b, double dx, double dy) { return false; }
    }
}
