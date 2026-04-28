package n1mbus.ghs.client.module.modules.misc;

import n1mbus.ghs.client.module.Category;
import n1mbus.ghs.client.module.Module;
import n1mbus.ghs.client.module.setting.ModeSetting;
import net.minecraft.client.Minecraft;

import java.util.Arrays;

/**
 * NoFall — Prevents fall damage safely.
 *   "Spoof" — Sets onGround=true directly in the vanilla packet via Mixin (safest).
 *   "Vanilla" — Client-side only fallDistance reset (works only on weak servers).
 */
public class NoFall extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "Spoof", Arrays.asList("Spoof", "Vanilla"));
    
    private boolean spoofingOnGround = false;
    private boolean preOnGround = false;

    public NoFall() {
        super("NoFall", "Prevents fall damage without spamming packets.", Category.MISC);
        addSetting(mode);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        switch (mode.getValue()) {
            case "Spoof" -> {
                // If we are falling fast enough to take damage, tell LocalPlayerMixin to spoof the next packet
                spoofingOnGround = (mc.player.fallDistance > 2.0f);
            }
            case "Vanilla" -> {
                spoofingOnGround = false;
                mc.player.fallDistance = 0;
            }
        }
    }
    
    @Override
    public void onDisable() {
        spoofingOnGround = false;
    }

    // Mixin Hooks
    public boolean isSpoofingOnGround() { return spoofingOnGround; }
    public void setPreOnGround(boolean val) { this.preOnGround = val; }
    public boolean getPreOnGround() { return preOnGround; }
}
