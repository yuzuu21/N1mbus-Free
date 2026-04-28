package n1mbus.ghs.mixin.logical;

import com.mojang.blaze3d.systems.RenderSystem;
import n1mbus.ghs.util.render.RenderUtil;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSystem.class, remap = false)
public class RenderSystemMixin {

    @Inject(method = "setProjectionMatrix", at = @At("HEAD"), require = 0)
    private static void onSetProjectionMatrix(Matrix4f matrix, CallbackInfo ci) {
        RenderUtil.setProjection(matrix);
    }

    @Inject(method = "setModelViewMatrix", at = @At("HEAD"), require = 0)
    private static void onSetModelViewMatrix(Matrix4f matrix, CallbackInfo ci) {
        RenderUtil.setModelView(matrix);
    }
}
