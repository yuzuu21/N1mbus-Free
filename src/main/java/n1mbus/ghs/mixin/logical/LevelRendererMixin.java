package n1mbus.ghs.mixin.logical;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void onRenderLevelHead(
        @Coerce Object deltaTracker,
        @Coerce Object fog,
        boolean renderBlockOutline,
        @Coerce Object camera,
        org.joml.Matrix4f projectionMatrix,
        org.joml.Matrix4f modelViewMatrix,
        @Coerce Object bufferSlice,
        @Coerce Object fogColor,
        boolean isFoggy,
        CallbackInfo ci
    ) {
        n1mbus.ghs.util.render.RenderUtil.setWorldMatrices(modelViewMatrix, projectionMatrix);
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void onRenderLevelReturn(
        @Coerce Object deltaTracker,
        @Coerce Object fog,
        boolean renderBlockOutline,
        @Coerce Object camera,
        org.joml.Matrix4f projectionMatrix,
        org.joml.Matrix4f modelViewMatrix,
        @Coerce Object bufferSlice,
        @Coerce Object fogColor,
        boolean isFoggy,
        CallbackInfo ci
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!(camera instanceof net.minecraft.client.Camera cam)) return;

        // Fallback path to keep 3D ESP visible even if render event route is unavailable.
        float partialTicks = 1.0f;
        if (deltaTracker instanceof net.minecraft.client.DeltaTracker dt) {
            partialTicks = dt.getGameTimeDeltaPartialTick(true);
        }
        com.mojang.blaze3d.vertex.PoseStack matrices = new com.mojang.blaze3d.vertex.PoseStack();
        matrices.mulPose(new org.joml.Quaternionf(cam.rotation()).conjugate());
        n1mbus.ghs.client.render.Esp3DWorldRenderer.render(matrices, cam, partialTicks);
    }
}
