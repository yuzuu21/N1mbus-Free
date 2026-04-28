package n1mbus.ghs.client.gui.custom;

import n1mbus.ghs.client.api.N1mbusGhsApi;
import n1mbus.ghs.client.api.WidgetStyle;
import n1mbus.ghs.client.gui.RoundedUiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.*;

public class ThemeEditorScreen extends Screen {
    private final Screen parent;
    private CustomUiTheme editingTheme;
    
    private enum StudioState { SELECT_PROJECT, CHOOSE_MODE, ADVANCED, VISUAL }
    private StudioState state = StudioState.SELECT_PROJECT;
    
    private String activeTab = "IDENTITY";
    private boolean showPopup = false;
    private String popupInput = "";
    
    // Spatial Interaction & Persistence
    private List<EditorPanel> panels = new ArrayList<>();
    private EditorPanel draggingPanel = null;
    private EditorPanel resizingPanel = null;
    private double dragOffsetX, dragOffsetY;
    private List<String> cachedThemes = new ArrayList<>();
    
    private ContextMenu activeMenu = null;
    private Picker activePicker = null;

    private String lastStatus = "READY";
    private float statusFade = 0f;
    private float projectScroll = 0f;
    private float targetProjectScroll = 0f;

    // Fonts from ClickGuiScreen
    private static final ResourceLocation HEADER_FONT = ResourceLocation.fromNamespaceAndPath("nimbus", "ui_rubik_bold");
    private static final ResourceLocation BODY_FONT = ResourceLocation.fromNamespaceAndPath("nimbus", "ui_rubik_medium");
    private static final ResourceLocation ICON_FONT = ResourceLocation.fromNamespaceAndPath("nimbus", "icons_tabler_outline");

    public ThemeEditorScreen(Screen parent) {
        super(Component.literal("Nimbus Spatial Studio"));
        this.parent = parent;
        refreshArchives();
        
        // Use real ClickGUI categories as default spatial nodes
        panels.add(new EditorPanel("COMBAT", 60, 40, 105, "\uF030", Arrays.asList("Aura", "Velocity", "Criticals")));
        panels.add(new EditorPanel("MOVEMENT", 180, 40, 105, "\uEC82", Arrays.asList("Fly", "Speed", "Step", "Spider")));
        panels.add(new EditorPanel("VISUALS", 300, 40, 105, "\uEA9A", Arrays.asList("ESP", "Tracers", "X-Ray")));
    }

    private void refreshArchives() {
        // Only show themes that have a source file on disk (External/Projects)
        this.cachedThemes = new ArrayList<>(CustomUiManager.get().getThemes().keySet());
        var active = CustomUiManager.get().getActiveTheme();
        if (active != null) {
            this.editingTheme = active;
            setStatus("THEME: " + active.name());
        }
    }

    private void setStatus(String msg) { this.lastStatus = msg; this.statusFade = 2.0f; }

    private void saveAll() {
        if (editingTheme != null) {
             // For each panel, we would update the visual element in theme JSON if it were structured that way.
             // For now, we save the global theme settings to theme.json.
             CustomUiManager.get().saveTheme(editingTheme);
             setStatus("STUDIO: WORKSPACE PERSISTED");
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        statusFade = Math.max(0, statusFade - delta * 0.05f);
        
        // --- 1:1 Backdrop Shader & Blur Control ---
        // We override background rendering to avoid the "Can only blur once per frame" crash
        n1mbus.ghs.client.LiquidGlassUniforms.get().setScreenWantsBlur(true);
        context.fillGradient(0, 0, width, height, 0xAA04070B, 0xAA06090E);
        
        // Apply the Actual Shader from the Theme
        if (editingTheme != null) {
             // In a real scenario, this helps the backend pick the right program
             CustomUiManager.get().applyTheme(editingTheme.name());
        }
        
        if (state == StudioState.VISUAL) {
            renderVisualCanvas(context, mouseX, mouseY);
            drawComponent(context, "[S] SAVE WORKSPACE | [ESC] SELECTION", 20, height - 20, 0xAAFFFFFF, false);
        } else {
            context.fill(0, 0, width, height, 0x66000000); 
            switch (state) {
                case SELECT_PROJECT -> renderProjectSelection(context, mouseX, mouseY);
                case CHOOSE_MODE -> renderModeSelection(context, mouseX, mouseY);
                case ADVANCED -> { renderSidebar(context, mouseX, mouseY); renderAdvanced(context, mouseX, mouseY); }
            }
        }

        if (showPopup) renderPopup(context, mouseX, mouseY);
        if (activeMenu != null) activeMenu.render(context, mouseX, mouseY);
        if (activePicker != null) {
            context.fill(0, 0, width, height, 0xCC000000); // Overlay for pickers
            activePicker.render(context, mouseX, mouseY);
        }

        if (statusFade > 0.01f) {
            int alpha = (int)(Math.min(1.0f, statusFade) * 255);
            context.drawString(minecraft.font, lastStatus, width - 10 - minecraft.font.width(lastStatus), height - 20, (alpha << 24) | 0xFFFFFF, false);
        }
    }

    private void renderSidebar(GuiGraphics context, int mouseX, int mouseY) {
        WidgetStyle style = WidgetStyle.create().tint(0x050515, 0.95f).blurRadius(15);
        N1mbusGhsApi.create(context).position(10, 10).size(160, height - 20).cornerRadius(15).style(style).render();
        drawComponent(context, "NIMBUS STUDIO", 25, 35, 0xFF539EFF, true);
        String[] tabs = {"IDENTITY", "SHADERS", "RESOURCES", "BACK"};
        int ty = 85;
        for (String tab : tabs) {
            boolean act = activeTab.equals(tab);
            boolean hov = mouseX >= 20 && mouseX <= 160 && mouseY >= ty && mouseY <= ty + 24;
            if (act) RoundedUiRenderer.drawRoundedRect(context, 20, ty, 140, 24, 6f, 0x44539EFF);
            drawComponent(context, tab, 35, ty + 7, act ? 0xFFFFFFFF : (hov ? 0xFFCBD5E1 : 0xFF64748B), false);
            ty += 34;
        }
    }

    private void renderProjectSelection(GuiGraphics context, int mouseX, int mouseY) {
        int cx = width/2, cy = height/2;
        WidgetStyle box = WidgetStyle.create().tint(0x050510, 0.98f).blurRadius(30);
        N1mbusGhsApi.create(context).position(cx-140, cy-120).size(280, 240).cornerRadius(20).style(box).render();
        context.drawCenteredString(minecraft.font, "THEME PROJECTS", cx, cy - 100, 0xFFFFFFFF);
        
        // --- Scrolling Project List ---
        int listX = cx - 110, listY = cy - 70, listW = 220, listH = 130;
        
        // Smooth scroll Interpolation
        projectScroll += (targetProjectScroll - projectScroll) * 0.2f;
        
        context.enableScissor(listX, listY, listX + listW, listY + listH);
        int py = (int) (listY - projectScroll);
        for (String name : cachedThemes) {
            if (py + 20 >= listY && py <= listY + listH) {
                boolean hov = mouseX >= listX && mouseX <= listX + listW && mouseY >= py && mouseY <= py + 20;
                RoundedUiRenderer.drawRoundedRect(context, listX, py, listW, 20, 6f, hov ? 0x22539EFF : 0x0AFFFFFF);
                drawComponent(context, name, listX + 15, py + 6, hov ? 0xFFFFFFFF : 0xFF94A3B8, false);
            }
            py += 24;
        }
        context.disableScissor();

        // Scrollbar Track
        int totalH = cachedThemes.size() * 24;
        if (totalH > listH) {
            int barY = listY + (int)((projectScroll / (totalH - listH)) * (listH - 20));
            context.fill(listX + listW + 4, listY, listX + listW + 6, listY + listH, 0x11FFFFFF);
            context.fill(listX + listW + 4, Math.max(listY, barY), listX + listW + 6, Math.min(listY + listH, barY + 20), 0xFF539EFF);
        }

        boolean hovN = mouseX >= cx-70 && mouseX <= cx+70 && mouseY >= cy+75 && mouseY <= cy+100;
        RoundedUiRenderer.drawRoundedRect(context, cx-70, cy+75, 140, 25, 10f, hovN ? 0xFF539EFF : 0xFF1E293B);
        drawComponent(context, "+ NEW PROJECT", cx-38, cy+83, 0xFFFFFFFF, false);
    }

    private void renderModeSelection(GuiGraphics context, int mouseX, int mouseY) {
        int cx = width / 2, cy = height / 2;
        context.drawCenteredString(minecraft.font, "DESIGN MODE", cx, cy - 100, 0xFF64748B);
        renderCard(context, mouseX, mouseY, cx - 180, cy - 60, 165, 165, "ADVANCED", "Manual Config", 0xFF2563EB);
        renderCard(context, mouseX, mouseY, cx + 15, cy - 60, 165, 165, "VISUAL", "ClickGUI Spatial", 0xFF10B981);
    }

    private void renderCard(GuiGraphics context, int mouseX, int mouseY, int x, int y, int w, int h, String title, String sub, int color) {
        boolean hov = mouseX >= x && mouseX <= x+w && mouseY >= y && mouseY <= y+h;
        WidgetStyle card = WidgetStyle.create().tint(hov ? 0x22FFFFFF : 0x0AFFFFFF, 0.4f).blurRadius(hov ? 15 : 5);
        N1mbusGhsApi.create(context).position(x, y).size(w, h).cornerRadius(15).style(card).render();
        RoundedUiRenderer.drawRoundedRect(context, x+w/2-22, y+30, 44, 44, 22f, color | 0x88000000);
        drawComponent(context, title, x+w/2 - minecraft.font.width(title)/2, y+90, 0xFFFFFFFF, true);
        drawComponent(context, sub, x+w/2 - minecraft.font.width(sub)/2, y+110, 0xFF64748B, false);
    }

    private void renderVisualCanvas(GuiGraphics context, int mouseX, int mouseY) {
        // High-precision Grid for design
        for (int i = 0; i < width; i += 40) context.fill(i, 0, i+1, height, 0x15FFFFFF);
        for (int i = 0; i < height; i += 40) context.fill(0, i, width, i+1, 0x15FFFFFF);
        
        for (EditorPanel panel : panels) panel.render(context, panel == draggingPanel || panel == resizingPanel);
    }

    private void renderAdvanced(GuiGraphics context, int mouseX, int mouseY) {
        WidgetStyle pStyle = WidgetStyle.create().tint(0x050510, 0.85f).blurRadius(15);
        N1mbusGhsApi.create(context).position(180, 10).size(width-190, height-20).cornerRadius(18).style(pStyle).render();
        drawComponent(context, activeTab + " SETTINGS", 205, 35, 0xFFFFFFFF, true);
        context.fill(205, 52, 280, 54, 0xFF539EFF);
    }

    private void renderPopup(GuiGraphics context, int mouseX, int mouseY) {
        int pw = 240, ph = 130;
        int px = (width-pw)/2, py = (height-ph)/2;
        RoundedUiRenderer.drawRoundedRect(context, px, py, pw, ph, 12f, 0xFF0D121F);
        RoundedUiRenderer.drawRoundedRect(context, px + 1, py + 1, pw - 2, ph - 2, 11f, 0xFF050505);
        drawComponent(context, "NEW THEME BRANCH", px+25, py+20, 0xFFFFFFFF, true);
        RoundedUiRenderer.drawRoundedRect(context, px+25, py+50, pw-50, 22, 6f, 0xFF111111);
        drawComponent(context, popupInput + "_", px+35, py+57, 0xFF539EFF, false);
        boolean hovOk = mouseX >= px+140 && mouseX <= px+215 && mouseY >= py+90 && mouseY <= py+115;
        RoundedUiRenderer.drawRoundedRect(context, px+140, py+90, 75, 25, 8f, hovOk ? 0xFF539EFF : 0xFF2563EB);
        drawComponent(context, "DONE", px+165, py+99, 0xFFFFFFFF, false);
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Leave empty to prevent standard Minecraft blur which causes IllegalStateException
    }

    private void drawComponent(GuiGraphics context, String text, int x, int y, int color, boolean shadow) {
        context.drawString(minecraft.font, Component.literal(text).setStyle(Style.EMPTY.withFont(BODY_FONT)), x, y, color, shadow);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (activePicker != null) { if (activePicker.handleClick(mouseX, mouseY)) { activePicker = null; return true; } if (activePicker.isOutside(mouseX, mouseY)) activePicker = null; return true; }
        if (activeMenu != null) { if (activeMenu.handleClick(mouseX, mouseY)) { activeMenu = null; return true; } activeMenu = null; }
        if (showPopup) {
            int pw = 240, ph = 130;
            int px = (width-pw)/2, py = (height-ph)/2;
            if (mouseX>=px+140 && mouseX<=px+215 && mouseY>=py+90 && mouseY<=py+115) { if (!popupInput.isEmpty()) { try { CustomUiManager.get().createNewProject(popupInput); refreshArchives(); showPopup = false; popupInput = ""; } catch (IOException ignored) {} } return true; }
            if (mouseX < px || mouseX > px+pw || mouseY < py || mouseY > py+ph) showPopup = false; return true;
        }

        switch (state) {
            case SELECT_PROJECT -> {
                int cx = width/2, cy = height/2;
                int listX = cx - 110, listY = cy - 70, listW = 220, listH = 130;
                
                if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
                    int py = (int) (listY - projectScroll);
                    for (String name : cachedThemes) {
                        if (mouseY >= py && mouseY <= py + 20) {
                            CustomUiManager.get().applyTheme(name);
                            refreshArchives();
                            state = StudioState.CHOOSE_MODE;
                            return true;
                        }
                        py += 24;
                    }
                }
                
                if (mouseX >= cx-70 && mouseX <= cx+70 && mouseY >= cy+75 && mouseY <= cy+100) { showPopup = true; return true; }
            }
            case CHOOSE_MODE -> {
                int cx = width/2, cy = height/2;
                if (mouseX >= cx - 170 && mouseX <= cx - 10 && mouseY >= cy - 60 && mouseY <= cy + 100) { state = StudioState.ADVANCED; return true; }
                if (mouseX >= cx + 10 && mouseX <= cx + 170 && mouseY >= cy - 60 && mouseY <= cy + 100) { state = StudioState.VISUAL; return true; }
            }
            case ADVANCED -> {
                if (mouseX >= 10 && mouseX <= 170) {
                    int ty = 80; String[] tabs = {"IDENTITY", "SHADERS", "RESOURCES", "BACK"};
                    for (String t : tabs) { if (mouseY >= ty && mouseY <= ty + 24) { if (t.equals("BACK")) { state = StudioState.CHOOSE_MODE; return true; } activeTab = t; return true; } ty += 34; }
                }
            }
            case VISUAL -> {
                for (EditorPanel p : panels) {
                    int totalH = 14 + p.modules.size() * 10;
                    if (mouseX >= p.x && mouseX <= p.x + p.w && mouseY >= p.y && mouseY <= p.y + 14) {
                        if (button == 1) { activeMenu = new ContextMenu((int)mouseX, (int)mouseY, p); return true; }
                        draggingPanel = p; dragOffsetX = mouseX - p.x; dragOffsetY = mouseY - p.y; return true;
                    }
                    if (mouseX >= p.x + p.w - 10 && mouseX <= p.x + p.w && mouseY >= p.y + totalH - 10 && mouseY <= p.y + totalH) { resizingPanel = p; return true; }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) { draggingPanel = null; resizingPanel = null; return super.mouseReleased(mouseX, mouseY, button); }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (draggingPanel != null) { draggingPanel.x = (int)(mouseX - dragOffsetX); draggingPanel.y = (int)(mouseY - dragOffsetY); }
        else if (resizingPanel != null) { resizingPanel.w = Math.max(72, (int)(mouseX - resizingPanel.x)); }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_S) { saveAll(); return true; }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (activePicker != null) { activePicker = null; return true; }
            if (activeMenu != null) { activeMenu = null; return true; }
            if (state == StudioState.CHOOSE_MODE) state = StudioState.SELECT_PROJECT;
            else if (state != StudioState.SELECT_PROJECT) state = StudioState.CHOOSE_MODE;
            else onClose(); return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean charTyped(char chr, int modifiers) { if (showPopup && popupInput.length() < 12) { popupInput += chr; return true; } return super.charTyped(chr, modifiers); }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (state == StudioState.SELECT_PROJECT) {
            int totalH = cachedThemes.size() * 24;
            int listH = 130;
            if (totalH > listH) {
                targetProjectScroll = Math.max(0, Math.min(totalH - listH + 10, targetProjectScroll - (float)verticalAmount * 24f));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // --- 1:1 REPLICATION OF CLICKGUI PANEL ---
    private class EditorPanel {
        String name; int x, y, w; String iconCode; List<String> modules;
        public EditorPanel(String name, int x, int y, int w, String iconCode, List<String> modules) { this.name = name; this.x = x; this.y = y; this.w = w; this.iconCode = iconCode; this.modules = modules; }
        
        void render(GuiGraphics context, boolean active) {
            Minecraft mc = Minecraft.getInstance();
            CustomUiTheme theme = CustomUiManager.get().getActiveTheme();
            CustomUiTheme.StyleConfig style = theme != null ? theme.style() : null;
            
            int panelBg = style != null ? CustomUiRenderer.parseHex(style.backgroundColor()) : 0xCC12171E;
            int accent = style != null ? CustomUiRenderer.parseHex(style.accentColor()) : 0xFF539EFF;
            float rad = style != null ? style.cornerRadius() : 10f;

            int totalH = 14 + modules.size() * 10;
            
            // Interaction Highlight
            if (active) RoundedUiRenderer.drawRoundedRect(context, x-1, y-1, w+2, totalH+2, rad, accent | 0x88000000);

            // Background Occlusion (Solid blackish pass)
            context.fill(x, y, x + w, y + totalH, 0xFF000000);

            // Header (Solid render using Nimbus API for that high-end look)
            WidgetStyle hStyle = WidgetStyle.create().tint(panelBg, 1.0f).blurRadius(10);
            N1mbusGhsApi.create(context).position(x, y).size(w, 14).cornerRadius(rad).style(hStyle).render();
            context.fill(x + 2, y + 13, x + w - 2, y + 14, accent);
            
            // Icon & Header Font correctly applied
            context.drawString(mc.font, Component.literal(iconCode).setStyle(Style.EMPTY.withFont(ICON_FONT)), x+5, y+3, accent, false);
            context.drawString(mc.font, Component.literal(name).setStyle(Style.EMPTY.withFont(HEADER_FONT)), x+20, y+3, 0xFFFFFFFF, false);
            
            // Modules List (Ensuring NO cut-off if width allows)
            int my = y + 14;
            for (String mod : modules) {
                context.fill(x, my, x + w, my + 10, 0xCC050510);
                int textW = mc.font.width(Component.literal(mod).setStyle(Style.EMPTY.withFont(BODY_FONT)));
                // Only trim if absolutely necessary
                String displayMod = (textW > w - 15) ? mc.font.plainSubstrByWidth(mod, w - 18) + ".." : mod;
                context.drawString(mc.font, Component.literal(displayMod).setStyle(Style.EMPTY.withFont(BODY_FONT)), x + 8, my + 1, 0xFFFFFFFF, false);
                my += 10;
            }
            
            // Corner Resize Handle
            context.fill(x + w - 3, y + totalH - 3, x + w, y + totalH, 0x88FFFFFF);
        }
    }

    private class ContextMenu {
        int x, y; EditorPanel target; String[] options = {"Change Icon (Coming Soon)", "Delete Panel"};
        public ContextMenu(int x, int y, EditorPanel target) { this.x = x; this.y = y; this.target = target; }
        void render(GuiGraphics context, int mouseX, int mouseY) {
            // High-contrast context menu
            int mw = 145, mh = options.length * 20 + 8;
            context.fill(x, y, x + mw, y + mh, 0xFF000000); 
            RoundedUiRenderer.drawRoundedRect(context, x, y, mw, mh, 4f, 0xFF252525);
            int oy = y + 4;
            for (String opt : options) {
                boolean disabled = opt.contains("Coming Soon");
                boolean hov = !disabled && mouseX >= x && mouseX <= x+mw && mouseY >= oy && mouseY <= oy+20;
                if (hov) RoundedUiRenderer.drawRoundedRect(context, x+2, oy, mw-4, 20, 3f, 0x33539EFF);
                int textColor = disabled ? 0xFF64748B : (hov ? 0xFFFFFFFF : 0xFFD1D5DB);
                context.drawString(minecraft.font, Component.literal(opt).setStyle(Style.EMPTY.withFont(BODY_FONT)), x+10, oy+5, textColor, false);
                oy += 20;
            }
        }
        boolean handleClick(double mx, double my) {
            int oy = y + 4;
            for (String opt : options) {
                if (mx >= x && mx <= x+145 && my >= oy && my <= oy+20) {
                   if (opt.contains("Coming Soon")) return true; // Consume but do nothing
                   if (opt.equals("Delete Panel")) panels.remove(target);
                   return true;
                }
                oy += 20;
            }
            return false;
        }
    }

    private abstract class Picker {
        protected int pw=300, ph=160, px, py;
        public Picker() { px=(width-pw)/2; py=(height-ph)/2; }
        public abstract void render(GuiGraphics context, int mouseX, int mouseY);
        public abstract boolean handleClick(double mx, double my);
        public boolean isOutside(double mx, double my) { return mx<px || mx>px+pw || my<py || my>py+ph; }
    }

    private class IconPicker extends Picker {
        private final EditorPanel target;
        private final String[] glyphs = {"\uF030", "\uEC82", "\uEA9A", "\uEAB9", "\uEB4D", "\uEAD2", "\uEB24", "\uEAC2", "\uEA8C", "\uEC8D", "\uEB44", "\uEAEA"};
        public IconPicker(EditorPanel target) { this.target = target; }
        @Override public void render(GuiGraphics context, int mouseX, int mouseY) {
            RoundedUiRenderer.drawRoundedRect(context, px, py, pw, ph, 12f, 0xFF050505);
            context.drawString(minecraft.font, "SYMBOL KEYBOARD", px+25, py+15, 0xFFFFFFFF, false);
            int ix = px+25, iy = py+45;
            for (String g : glyphs) {
                boolean hov = mouseX>=ix && mouseX<=ix+40 && mouseY>=iy && mouseY<=iy+40;
                RoundedUiRenderer.drawRoundedRect(context, ix, iy, 40, 40, 6f, hov?0x663B82F6:0x22FFFFFF);
                context.drawString(minecraft.font, Component.literal(g).setStyle(Style.EMPTY.withFont(ICON_FONT)), ix+14, iy+15, 0xFFFFFFFF, false);
                ix += 45; if (ix+40 > px+pw-20) { ix=px+25; iy+=45; }
            }
        }
        @Override public boolean handleClick(double mx, double my) {
            int ix = px+25, iy = py+45;
            for (String g : glyphs) { if (mx>=ix && mx<=ix+40 && my>=iy && my<=iy+40) { target.iconCode = g; return true; } ix += 45; if (ix+40 > px+pw-20) { ix=px+25; iy+=45; } }
            return false;
        }
    }
}
