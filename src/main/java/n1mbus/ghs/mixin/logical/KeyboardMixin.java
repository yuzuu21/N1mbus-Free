package n1mbus.ghs.mixin.logical;

import n1mbus.ghs.client.gui.clickgui.ClickGuiScreen;
import n1mbus.ghs.client.module.ModuleManager;
import n1mbus.ghs.client.module.Module;
import n1mbus.ghs.client.module.modules.misc.ClickGuiModule;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS || minecraft.screen != null) return;

        // ClickGUI uses the module's inherited bind
        ClickGuiModule guiMod = ModuleManager.INSTANCE.getModule(ClickGuiModule.class);
        int guiKey = (guiMod != null && guiMod.getKey() != 0) ? guiMod.getKey() : GLFW.GLFW_KEY_INSERT;

        if (key == guiKey) {
            minecraft.setScreen(new ClickGuiScreen());
            ci.cancel();
            return;
        }

        // Handle all other module toggles via their inherited bind
        for (Module m : ModuleManager.INSTANCE.getModules()) {
            if (m instanceof ClickGuiModule) continue; // Already handled above
            if (m.getKey() == key && key != 0) {
                m.toggle();
            }
        }
        
        // ESC shield to prevent accidental pause menu after closing GUI
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            long delta = System.currentTimeMillis() - ClickGuiScreen.lastCloseTime;
            if (delta < 400) {
                ci.cancel();
            }
        }
    }
}
