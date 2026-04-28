package n1mbus.ghs.client.module.modules.movement;

import n1mbus.ghs.client.module.Category;
import n1mbus.ghs.client.module.Module;
import n1mbus.ghs.client.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Step — Instantly walk up blocks without jumping.
 * Uses the vanilla stepHeight attribute in 1.21.8.
 */
public class Step extends Module {
    private final NumberSetting height = new NumberSetting("Height", 1.0, 0.5, 2.5);

    public Step() {
        super("Step", "Walk up blocks without jumping.", Category.MOVEMENT);
        addSetting(height);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        try {
            AttributeInstance attr = mc.player.getAttribute(Attributes.STEP_HEIGHT);
            if (attr != null) {
                attr.setBaseValue(height.getValue());
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        try {
            AttributeInstance attr = mc.player.getAttribute(Attributes.STEP_HEIGHT);
            if (attr != null) {
                attr.setBaseValue(0.6);
            }
        } catch (Exception ignored) {}
    }
}
