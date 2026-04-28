package n1mbus.ghs.client.gui.custom;
import net.minecraft.client.renderer.texture.DynamicTexture;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MarketplaceManager {
    private static final Logger LOGGER = LogManager.getLogger("NimbusMarketplace");
    private static final Gson GSON = new Gson();
    
    public record MarketplaceItem(
        String id,
        String name,
        String author,
        String description,
        String type, // SHADER, FONT, THEME
        String previewUrl,
        String downloadUrl
    ) {}

    private final Map<String, ResourceLocation> cachedPreviews = new HashMap<>();
    private final List<MarketplaceItem> trendingItems = new ArrayList<>();

    public CompletableFuture<List<MarketplaceItem>> fetchTrending() {
        return CompletableFuture.supplyAsync(() -> {
            // Simulated fetch - In a real scenario, this hits a remote API
            // For now, we provide high-quality defaults from a predefined manifest
            trendingItems.clear();
            trendingItems.add(new MarketplaceItem("nebula_v2", "Stellar Nebula V2", "Nimbus", "Enhanced cosmic animation", "SHADER", "https://i.imgur.com/example1.png", ""));
            trendingItems.add(new MarketplaceItem("glass_caustics", "Caustics Glass", "Mikan", "Real-time water reflection", "SHADER", "https://i.imgur.com/example2.png", ""));
            trendingItems.add(new MarketplaceItem("rubik_font", "Rubik UI", "Google", "Modern sans-serif font", "FONT", "https://i.imgur.com/example3.png", ""));
            return trendingItems;
        });
    }

    public void loadPreview(MarketplaceItem item) {
        if (cachedPreviews.containsKey(item.id())) return;

        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(item.previewUrl());
                try (InputStream is = url.openStream()) {
                    NativeImage img = NativeImage.read(is);
                    Minecraft.getInstance().execute(() -> {
                        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("nimbus", "marketplace/preview/" + item.id());
                        DynamicTexture tex = new DynamicTexture(() -> "nimbus_mp_" + item.id(), img);
                        Minecraft.getInstance().getTextureManager().register(id, tex);
                        cachedPreviews.put(item.id(), id);
                    });
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load marketplace preview for " + item.id(), e);
            }
        });
    }

    public ResourceLocation getPreview(String id) {
        return cachedPreviews.get(id);
    }
}
