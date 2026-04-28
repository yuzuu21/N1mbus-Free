package n1mbus.ghs.client.module.modules.visual;

import n1mbus.ghs.client.module.Category;
import n1mbus.ghs.client.module.Module;
import net.minecraft.client.Minecraft;

public class FullBright extends Module {
    private double originalGamma;

    public FullBright() {
        super("FullBright", "Makes everything bright.", Category.VISUALS);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            // Apply long-duration night vision effect
            mc.player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 1000, 0, false, false));
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
        }
    }
}
