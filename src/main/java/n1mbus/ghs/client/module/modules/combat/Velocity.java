package n1mbus.ghs.client.module.modules.combat;

import n1mbus.ghs.client.module.Category;
import n1mbus.ghs.client.module.Module;
import n1mbus.ghs.client.module.setting.ModeSetting;
import n1mbus.ghs.client.module.setting.NumberSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;

/**
 * Velocity modifier module.
 */
public class Velocity extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "Reduce", java.util.Arrays.asList("Cancel", "Reduce", "Jump"));
    private final NumberSetting horizontal = new NumberSetting("Horizontal", 60.0, 0.0, 100.0);
    private final NumberSetting vertical = new NumberSetting("Vertical", 100.0, 0.0, 100.0);
    private int pendingTicks = 0;

    public Velocity() {
        super("Velocity", "Modifies incoming knockback.", Category.COMBAT);
        addSetting(mode);
        addSetting(horizontal);
        addSetting(vertical);
    }

    public double[] getMultipliers() {
        return new double[]{ horizontal.getValue() / 100.0, vertical.getValue() / 100.0 };
    }

    public String getMode() {
        return mode.getValue();
    }

    public void flagIncomingVelocity() {
        // Correct over next ticks after packet processing.
        pendingTicks = 2;
    }

    public void onPlayerTick(LocalPlayer player) {
        if (!isEnabled() || pendingTicks <= 0) return;
        pendingTicks--;

        String m = mode.getValue();
        if ("Cancel".equals(m)) {
            player.setDeltaMovement(0.0, Math.min(player.getDeltaMovement().y, 0.08), 0.0);
            player.hurtMarked = true;
            return;
        }

        double[] mult = getMultipliers();
        double h = mult[0];
        double v = mult[1];

        double x = player.getDeltaMovement().x * h;
        double y = player.getDeltaMovement().y * v;
        double z = player.getDeltaMovement().z * h;

        if ("Jump".equals(m) && player.onGround()) {
            player.jumpFromGround();
            y = player.getDeltaMovement().y;
        }

        player.setDeltaMovement(x, y, z);
        player.hurtMarked = true;
    }

    public boolean handleVelocity(double[] motion) {
        if (!isEnabled()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        switch (mode.getValue()) {
            case "Cancel":
                return true;
            case "Reduce":
                double[] mults = getMultipliers();
                double h = mults[0];
                double v = mults[1];
                
                double curX = mc.player.getDeltaMovement().x;
                double curY = mc.player.getDeltaMovement().y;
                double curZ = mc.player.getDeltaMovement().z;
                
                double deltaX = (motion[0] - curX) * h;
                double deltaY = (motion[1] - curY) * v;
                double deltaZ = (motion[2] - curZ) * h;
                
                motion[0] = curX + deltaX;
                motion[1] = curY + deltaY;
                motion[2] = curZ + deltaZ;
                return false;
            case "Jump":
                if (mc.player.onGround()) {
                    mc.player.jumpFromGround();
                }
                return false;
        }
        return false;
    }
}
