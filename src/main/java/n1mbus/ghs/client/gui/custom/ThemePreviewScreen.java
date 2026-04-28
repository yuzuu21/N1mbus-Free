package n1mbus.ghs.client.gui.custom;

import n1mbus.ghs.client.api.N1mbusGhsApi;
import n1mbus.ghs.client.api.WidgetStyle;
import n1mbus.ghs.client.gui.RoundedUiRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public class ThemePreviewScreen extends Screen {
    private final Screen editor;
    private final CustomUiTheme previewTheme;
    private final CustomUiTheme originalTheme;
    private float anim = 0f;

    public ThemePreviewScreen(Screen editor, CustomUiTheme theme) {
        super(Component.literal("Nimbus Theme Preview"));
        this.editor = editor;
        this.previewTheme = theme;
        this.originalTheme = CustomUiManager.get().getActiveTheme();
    }

    @Override
    protected void init() {
        // Temporarily apply the theme globally so shaders work on EVERYTHING
        CustomUiManager.get().loadThemeAssets(previewTheme);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        anim = Math.min(1.0f, anim + delta * 0.05f);
        
        // This screen allows the user to see the background blur and global shaders
        // No Studio UI here - pure immersion
        
        // Render a mockup of the actual ClickGUI or a test panel using the theme
        renderMockup(context, mouseX, mouseY);
        
        // Close hint with animation
        int alpha = (int)(Math.abs(Math.sin(System.currentTimeMillis() / 600.0)) * 150 + 105);
        context.drawCenteredString(minecraft.font, "[ESC] TO EXIT PREVIEW MODE", width / 2, height - 30, (alpha << 24) | 0xFFFFFF);
        
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderMockup(GuiGraphics context, int mouseX, int mouseY) {
        var style = previewTheme.style();
        int accent = (int) Long.parseUnsignedLong(style.accentColor().replace("#", ""), 16);
        WidgetStyle demoStyle = WidgetStyle.create().tint(accent, 0.65f).blurRadius((int)style.blurRadius());
        ResourceLocation fontId = ResourceLocation.fromNamespaceAndPath("nimbus", style.fontName().toLowerCase() + "_medium");
        if (style.fontName().equalsIgnoreCase("Rubik")) fontId = ResourceLocation.fromNamespaceAndPath("nimbus", "ui_rubik_medium");
        var themedFont = net.minecraft.network.chat.Style.EMPTY.withFont(fontId);

        // Center Panel
        int pw = 340, ph = 220;
        int px = (width - pw) / 2;
        int py = (height - ph) / 2;
        
        N1mbusGhsApi.create(context).position(px, py).size(pw, ph).cornerRadius(style.cornerRadius()).style(demoStyle).render();
        
        context.drawString(minecraft.font, Component.literal("THEME LIVE PREVIEW").setStyle(themedFont), px + 25, py + 25, 0xFFFFFFFF, true);
        RoundedUiRenderer.drawRoundedRect(context, px + 25, py + 48, pw - 50, 2, 1f, accent);
        
        String[] mods = {"Aura", "Velocity", "Flight", "Scaffold", "NoFall"};
        int my = py + 65;
        for (String mod : mods) {
             boolean hov = mouseX >= px + 25 && mouseX <= px + 120 && mouseY >= my && mouseY <= my + 15;
             context.drawString(minecraft.font, Component.literal("> " + mod).setStyle(themedFont), px + 30, my, hov ? accent : 0xFFE2E8F0, false);
             my += 22;
        }

        // Secondary Info Panel
        N1mbusGhsApi.create(context).position(px + pw + 15, py).size(120, 80).cornerRadius(style.cornerRadius()).style(demoStyle).render();
        context.drawString(minecraft.font, Component.literal("STATS").setStyle(themedFont), px + pw + 30, py + 20, 0xFFFFFFFF, true);
        context.drawString(minecraft.font, Component.literal("BPS: 21.4").setStyle(themedFont), px + pw + 30, py + 45, 0xFFCBD5E1, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        // Restore the original theme when returning to editor
        if (originalTheme != null) {
            CustomUiManager.get().loadThemeAssets(originalTheme);
        }
        minecraft.setScreen(editor);
    }
}
