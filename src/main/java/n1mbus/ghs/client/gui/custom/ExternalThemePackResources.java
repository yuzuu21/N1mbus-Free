package n1mbus.ghs.client.gui.custom;

import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class ExternalThemePackResources implements PackResources {
    private final PackLocationInfo info;

    public ExternalThemePackResources() {
        this.info = new PackLocationInfo("nimbus_external", net.minecraft.network.chat.Component.literal("Nimbus External Themes"), net.minecraft.server.packs.repository.PackSource.DEFAULT, java.util.Optional.empty());
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... segments) {
        return null; 
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        if (type != PackType.CLIENT_RESOURCES || !id.getNamespace().equals("nimbus")) return null;
        
        CustomUiTheme active = CustomUiManager.get().getActiveTheme();
        if (active == null) return null;
        
        File source = CustomUiManager.get().getThemeSource(active.name());
        if (source == null) return null;

        String path = id.getPath(); // e.g., program/my_shader
        if (path.startsWith("program/")) {
            String shaderName = path.substring("program/".length());
            String internalPath = "assets/shader/" + shaderName + ".fsh";
            
            if (source.isDirectory()) {
                File fsh = source.toPath().resolve(internalPath).toFile();
                if (fsh.exists()) {
                    return () -> new FileInputStream(fsh);
                }
            } else if (source.getName().endsWith(".zip")) {
                return () -> {
                    byte[] bytes;
                    try (ZipFile zip = new ZipFile(source)) {
                        ZipEntry entry = zip.getEntry(internalPath);
                        if (entry == null) throw new FileNotFoundException(internalPath + " in ZIP");
                        try (InputStream is = zip.getInputStream(entry)) {
                            bytes = is.readAllBytes();
                        }
                    }
                    return new ByteArrayInputStream(bytes);
                };
            }
        }
        
        return null;
    }

    @Override
    public void listResources(PackType type, String namespace, String prefix, PackResources.ResourceOutput consumer) {
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? Set.of("nimbus") : Collections.emptySet();
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionType<T> reader) {
        return null;
    }

    @Override
    public PackLocationInfo location() {
        return info;
    }

    @Override
    public void close() {}
}
