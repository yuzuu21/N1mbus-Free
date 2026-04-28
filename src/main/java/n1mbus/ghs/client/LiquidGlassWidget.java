package n1mbus.ghs.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import n1mbus.ghs.client.api.N1mbusGhsApi;
import n1mbus.ghs.client.api.WidgetStyle;

public class LiquidGlassWidget extends AbstractWidget {
    private float cornerRadiusPx;
    private boolean moveable;
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    public WidgetStyle style = new WidgetStyle();

    @Override protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {}

    public LiquidGlassWidget(int x, int y, int width, int height, WidgetStyle style) {
        super(x, y, width, height, Component.empty());
        this.cornerRadiusPx = 0.5f * Math.min(width, height);
        if (style != null) this.style = style;
    }

    public LiquidGlassWidget setCornerRadiusPx(float radiusPx) {
        this.cornerRadiusPx = Math.max(0f, radiusPx);
        return this;
    }

    public LiquidGlassWidget setMoveable(boolean moveable) {
        this.moveable = moveable;
        return this;
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        N1mbusGhsApi.create(context).fromWidget(this).cornerRadius(cornerRadiusPx).style(this.style).render();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.moveable) return super.mouseClicked(mouseX, mouseY, button);
        if (button == 0 && mouseX >= this.getX() && mouseX < this.getX() + this.width && mouseY >= this.getY() && mouseY < this.getY() + this.getHeight()) {
            this.dragging = true;
            this.dragOffsetX = (int) (mouseX - this.getX());
            this.dragOffsetY = (int) (mouseY - this.getY());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.dragging && button == 0) {
            int newX = (int) (mouseX - this.dragOffsetX);
            int newY = (int) (mouseY - this.dragOffsetY);
            this.setX(newX);
            this.setY(newY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.dragging && button == 0) {
            this.dragging = false;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

}