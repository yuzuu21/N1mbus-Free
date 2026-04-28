package n1mbus.ghs.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import n1mbus.ghs.client.gui.custom.CustomUiManager;
import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;

public final class LiquidGlassPipelines {
    private static RenderPipeline WIDGET_PIPELINE;
    private static ResourceLocation WIDGET_SHDR_ID;
    private static RenderPipeline BACKGROUND_PIPELINE;
    private static ResourceLocation BACKGROUND_SHDR_ID;
    private static com.mojang.blaze3d.buffers.GpuBuffer QUAD_BUFFER;
    private static com.mojang.blaze3d.textures.GpuTexture backgroundCopy;
    private static com.mojang.blaze3d.textures.GpuTextureView backgroundCopyView;

    private LiquidGlassPipelines() {}

    public static synchronized RenderPipeline getWidgetPipeline() {
        ResourceLocation targetShader = CustomUiManager.get().getWidgetShaderId();
        if (WIDGET_PIPELINE == null || !targetShader.equals(WIDGET_SHDR_ID)) {
            WIDGET_SHDR_ID = targetShader;
            WIDGET_PIPELINE = createPipeline(targetShader);
        }
        return WIDGET_PIPELINE;
    }

    public static synchronized RenderPipeline getBackgroundPipeline() {
        ResourceLocation targetShader = CustomUiManager.get().getBackgroundShaderId();
        if (BACKGROUND_PIPELINE == null || !targetShader.equals(BACKGROUND_SHDR_ID)) {
            BACKGROUND_SHDR_ID = targetShader;
            BACKGROUND_PIPELINE = createPipeline(targetShader);
        }
        return BACKGROUND_PIPELINE;
    }

    private static RenderPipeline createPipeline(ResourceLocation targetShader) {
        RenderPipeline.Builder b = RenderPipeline.builder()
                .withLocation(ResourceLocation.fromNamespaceAndPath("nimbus", "pipeline/ui_" + targetShader.getPath().replace('/', '_')))
                .withVertexShader(ResourceLocation.fromNamespaceAndPath("nimbus", "core/blit_fullscreen"))
                .withFragmentShader(targetShader)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                .withUniform("CustomUniforms", UniformType.UNIFORM_BUFFER)
                .withUniform("WidgetInfo", UniformType.UNIFORM_BUFFER)
                .withUniform("BgConfig", UniformType.UNIFORM_BUFFER)
                .withSampler("Sampler0")
                .withSampler("Sampler1")
                .withSampler("Sampler2")
                .withSampler("Sampler3")
                .withSampler("Sampler4")
                .withSampler("Sampler5")
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS);

        RenderPipeline pipeline = b.build();
        
        RenderSystem.getDevice().precompilePipeline(pipeline, (id, type) -> {
            String pathStr = id.getPath();
            String extension = (pathStr.contains("core/") || pathStr.endsWith(".vsh")) ? ".vsh" : ".fsh";
            String shaderPath = "shaders/" + pathStr + (pathStr.endsWith(".vsh") || pathStr.endsWith(".fsh") ? "" : extension);

            // JAR Stream (Standard Fallback)
            try (var is = LiquidGlassPipelines.class.getResourceAsStream("/assets/nimbus/" + shaderPath)) {
                if (is != null) {
                    byte[] bytes = is.readAllBytes();
                    if (bytes.length > 0) {
                        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
            } catch (Exception ignored) {}

            // EMERGENCY FALLBACK
            if (extension.equals(".vsh")) {
                return "#version 150\nin vec3 Position; out vec2 texCoord; void main() { texCoord = Position.xy; gl_Position = vec4(Position.xy * 2.0 - 1.0, 0.0, 1.0); }";
            } else {
                return "#version 150\nout vec4 fragColor; void main() { fragColor = vec4(1.0, 0.0, 1.0, 1.0); }";
            }
        });
        return pipeline;
    }

    public static void forceReload() {
        WIDGET_PIPELINE = null;
        BACKGROUND_PIPELINE = null;
        if (backgroundCopy != null) {
            if (backgroundCopyView != null) backgroundCopyView.close();
            backgroundCopy.close();
            backgroundCopy = null;
            backgroundCopyView = null;
        }
    }

    public static com.mojang.blaze3d.textures.GpuTextureView getBackgroundCopyView(int w, int h) {
        var device = com.mojang.blaze3d.systems.RenderSystem.getDevice();
        if (backgroundCopy == null || backgroundCopy.getWidth(0) != w || backgroundCopy.getHeight(0) != h) {
            if (backgroundCopy != null) {
                if (backgroundCopyView != null) backgroundCopyView.close();
                backgroundCopy.close();
            }
            backgroundCopy = device.createTexture("nimbus backgroundCopy", 12, com.mojang.blaze3d.textures.TextureFormat.RGBA8, w, h, 1, 1);
            backgroundCopyView = device.createTextureView(backgroundCopy);
        }
        return backgroundCopyView;
    }

    public static com.mojang.blaze3d.textures.GpuTexture getBackgroundCopyTex() {
        return backgroundCopy;
    }

    public static com.mojang.blaze3d.buffers.GpuBuffer getQuadBuffer() {
        if (QUAD_BUFFER == null) {
            QUAD_BUFFER = RenderSystem.getDevice().createBuffer(() -> "nimbus QuadBuffer", 128, 48);
            try (var map = RenderSystem.getDevice().createCommandEncoder().mapBuffer(QUAD_BUFFER, false, true)) {
                java.nio.ByteBuffer data = map.data();
                data.putFloat(0.0f); data.putFloat(0.0f); data.putFloat(0.0f); // BL
                data.putFloat(0.0f); data.putFloat(1.0f); data.putFloat(0.0f); // TL
                data.putFloat(1.0f); data.putFloat(1.0f); data.putFloat(0.0f); // TR
                data.putFloat(1.0f); data.putFloat(0.0f); data.putFloat(0.0f); // BR
            }
        }
        return QUAD_BUFFER;
    }
}