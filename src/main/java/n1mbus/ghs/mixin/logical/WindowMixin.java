package n1mbus.ghs.mixin.logical;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class WindowMixin {
    @Shadow private long window;
    @Shadow private boolean fullscreen;
    @Shadow private int windowedX;
    @Shadow private int windowedY;
    @Shadow private int windowedWidth;
    @Shadow private int windowedHeight;
    
    @Inject(method = "updateFullscreen", at = @At("HEAD"), cancellable = true)
    private void forceBorderlessFullscreen(CallbackInfo ci) {
        if (this.fullscreen) {
            // Get primary monitor resolution
            long primaryMonitor = GLFW.glfwGetPrimaryMonitor();
            if (primaryMonitor != 0L) {
                org.lwjgl.glfw.GLFWVidMode mode = GLFW.glfwGetVideoMode(primaryMonitor);
                if (mode != null) {
                    int[] xpos = new int[1];
                    int[] ypos = new int[1];
                    GLFW.glfwGetMonitorPos(primaryMonitor, xpos, ypos);
                    // Force borderless by setting height to screen+1 to bypass Windows OS fullscreen optimization.
                    // This allows native UI overlays to render correctly over the game window.
                    GLFW.glfwSetWindowAttrib(this.window, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
                    GLFW.glfwSetWindowMonitor(this.window, 0L, xpos[0], ypos[0], mode.width(), mode.height() + 1, GLFW.GLFW_DONT_CARE);
                }
            }
        } else {
            // ウィンドウモードに戻す
            GLFW.glfwSetWindowAttrib(this.window, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
            GLFW.glfwSetWindowPos(this.window, this.windowedX, this.windowedY);
            GLFW.glfwSetWindowSize(this.window, this.windowedWidth, this.windowedHeight);
        }
        ci.cancel();
    }
}
