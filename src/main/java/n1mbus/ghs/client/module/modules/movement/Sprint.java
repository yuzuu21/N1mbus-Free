package n1mbus.ghs.client.module.modules.movement;

import n1mbus.ghs.client.module.Category;
import n1mbus.ghs.client.module.Module;
import net.minecraft.client.Minecraft;

/**
 * Sprint — Forces sprint at all times.
 * Simple but essential for PvP (combo distance) and general movement.
 */
public class Sprint extends Module {
    public Sprint() {
        super("Sprint", "Always sprint.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isDeadOrDying()) return;

        if (mc.player.input.hasForwardImpulse() && !mc.player.isUsingItem()
            && mc.player.getFoodData().getFoodLevel() > 6) {
            mc.player.setSprinting(true);
        }
    }
}
