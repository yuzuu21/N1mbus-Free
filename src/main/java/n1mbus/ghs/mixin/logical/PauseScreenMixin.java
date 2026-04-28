package n1mbus.ghs.mixin.logical;

import n1mbus.ghs.client.gui.clickgui.ClickGuiScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class PauseScreenMixin {
    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        long delta = System.currentTimeMillis() - ClickGuiScreen.lastCloseTime;
        if (delta < 1000) {
            // System.out.println("[NimbusGhs] BRUTE FORCE: Killing PauseScreen (delta: " + delta + "ms)");
            // Immediately close the pause screen if it was triggered by our GUI closing
            PauseScreen screen = (PauseScreen) (Object) this;
            screen.onClose();
        }
    }
}
