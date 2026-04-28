package n1mbus.ghs.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;

public final class RoundedPipelines {
    private static RenderPipeline ROUNDED_GUI;

    private RoundedPipelines() {}

    public static synchronized RenderPipeline getGuiPipeline() {
        if (ROUNDED_GUI == null) {
            RenderPipeline.Builder b = RenderPipeline.builder()
                .withLocation(ResourceLocation.fromNamespaceAndPath("nimbus", "pipeline/rounded_gui"))
                .withVertexShader(ResourceLocation.fromNamespaceAndPath("nimbus", "core/blit_fullscreen"))
                .withFragmentShader(ResourceLocation.fromNamespaceAndPath("nimbus", "program/rounded_gui"))
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                .withUniform("RoundedInfo", UniformType.UNIFORM_BUFFER)
                .withSampler("Sampler0")
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS);

            ROUNDED_GUI = b.build();
            
            RenderSystem.getDevice().precompilePipeline(ROUNDED_GUI, (id, type) -> {
                String pathStr = id.getPath();
                String extension = (pathStr.contains("core/") || pathStr.endsWith(".vsh")) ? ".vsh" : ".fsh";
                String shaderPath = "shaders/" + pathStr + (pathStr.endsWith(".vsh") || pathStr.endsWith(".fsh") ? "" : extension);
                ResourceLocation shaderId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), shaderPath);
                String fabricResourcePath = "assets/" + id.getNamespace() + "/" + shaderPath;
                
                try {
                    // Method 1: Minecraft ResourceManager
                    Minecraft mc = Minecraft.getInstance();
                    if (mc != null && mc.getResourceManager() != null) {
                        var resource = mc.getResourceManager().getResource(shaderId);
                        if (resource.isPresent()) {
                            try (var is = resource.get().open()) {
                                System.out.println("[NimbusGhs] Loaded rounded shader via ResourceManager: " + shaderId);
                                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                            }
                        }
                    }

                    // Method 2: Fabric Loader Fallback
                    var container = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(id.getNamespace());
                    if (container.isPresent()) {
                         var path = container.get().findPath(fabricResourcePath).orElse(null);
                         if (path != null) {
                             System.out.println("[NimbusGhs] Loaded rounded shader via FabricLoader: " + fabricResourcePath);
                             return new String(java.nio.file.Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
                         }
                    }
                } catch (Exception e) {
                    System.err.println("[NimbusGhs] Error reading rounded shader " + id + ": " + e.getMessage());
                }
                System.err.println("[NimbusGhs] FAILED to load rounded shader: " + id + " (Tried ID: " + shaderId + ")");
                return "";
            });
        }
        return ROUNDED_GUI;
    }
}
