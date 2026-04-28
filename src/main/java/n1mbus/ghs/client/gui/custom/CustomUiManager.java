package n1mbus.ghs.client.gui.custom;
import net.minecraft.client.renderer.texture.DynamicTexture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.util.concurrent.CompletableFuture;
import java.net.URL;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import n1mbus.ghs.client.LiquidGlassPipelines;
import net.minecraft.server.packs.PackResources;
import net.minecraft.resources.ResourceLocation;
import java.nio.channels.FileChannel;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public final class CustomUiManager {
    private static final Logger LOGGER = LogManager.getLogger("NimbusCustomUI");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final CustomUiManager INSTANCE = new CustomUiManager();

    private final Path configDir;
    private final Path themesDir;
    private final Path externalShadersDir;
    private final List<CustomUiTheme> loadedThemes = new ArrayList<>();
    private final Map<String, CustomUiTheme> builtinThemes = new LinkedHashMap<>();
    private final Map<String, CustomUiTheme> externalThemes = new LinkedHashMap<>();
    private CustomUiTheme activeTheme = null;
    private ResourceLocation activeShaderId = ResourceLocation.fromNamespaceAndPath("nimbus", "program/liquid_glass_gui");
    private ResourceLocation backgroundShaderId = ResourceLocation.fromNamespaceAndPath("nimbus", "program/liquid_glass_gui");
    
    private boolean useExternalShader = false;
    private File activeExternalShaderFile = null;
    private long lastFileTime = 0;
    private long lastCheckTime = 0;
    private ExternalThemePackResources externalPack;

    private final Map<String, ResourceLocation> dynamicTextures = new HashMap<>();
    private final Map<String, File> themeSources = new HashMap<>(); // Track where each theme came from

    public static CustomUiManager get() { return INSTANCE; }
    
    public ResourceLocation getBackgroundShaderId() { return backgroundShaderId; }
    public ResourceLocation getWidgetShaderId() { return activeShaderId; }

    private CustomUiManager() {
        String appdata = System.getenv("APPDATA");
        if (appdata == null) appdata = System.getProperty("user.home");
        this.configDir = Path.of(appdata).resolve(".nimbus").resolve("Theme");
        this.themesDir = configDir; 
        this.externalShadersDir = configDir.resolve("assets").resolve("shaders");
        this.externalPack = new ExternalThemePackResources();
        try {
            Files.createDirectories(configDir);
            registerBuiltinThemes(); // Restore internal memory registration
            scanExternalThemes();
        } catch (IOException e) {
            LOGGER.error("Failed to create UI config directories", e);
        }
    }
    
    public PackResources getExternalPack() { return externalPack; }

    private void registerBuiltinThemes() {
        builtinThemes.clear();
        builtinThemes.put("Aurora Ice", createBuiltinTheme(
            "Aurora Ice", "Nimbus Team", "Cool cyan glass", "aurora",
            "#63B3FF", "#0F1824", 10f, 8f,
            Map.of("headerComponentColor", "#75C0FF", "bodyBackgroundColor", "#111B2A", "textEnabledColor", "#F8FCFF", "textDisabledColor", "#9DB1C5")
        ));
        builtinThemes.put("Midnight Glass", createBuiltinTheme(
            "Midnight Glass", "Nimbus Team", "Deep dark navy", "liquidglass",
            "#4C8DFF", "#0C111A", 9f, 7f,
            Map.of("headerComponentColor", "#68A7FF", "bodyBackgroundColor", "#101723", "textEnabledColor", "#EFF4FF", "textDisabledColor", "#93A0B3")
        ));
        builtinThemes.put("Emerald Mist", createBuiltinTheme(
            "Emerald Mist", "Nimbus Team", "Green accent glass", "aurora",
            "#40D7B0", "#0B1715", 10f, 8f,
            Map.of("headerComponentColor", "#57E2BE", "bodyBackgroundColor", "#10201D", "textEnabledColor", "#F1FFFA", "textDisabledColor", "#99B8AE")
        ));
        builtinThemes.put("Crimson Pulse", createBuiltinTheme(
            "Crimson Pulse", "Nimbus Team", "Warm red neon", "neon",
            "#FF5E7A", "#1A0E14", 8f, 8f,
            Map.of("headerComponentColor", "#FF7F97", "bodyBackgroundColor", "#25131C", "textEnabledColor", "#FFF2F5", "textDisabledColor", "#BDA1AB")
        ));
        builtinThemes.put("Violet Matrix", createBuiltinTheme(
            "Violet Matrix", "Nimbus Team", "Purple digital rain", "matrix",
            "#B58BFF", "#120E1D", 8f, 9f,
            Map.of("headerComponentColor", "#C6A6FF", "bodyBackgroundColor", "#1A1428", "textEnabledColor", "#F7F2FF", "textDisabledColor", "#A89ABF")
        ));
        builtinThemes.put("Amber Glow", createBuiltinTheme(
            "Amber Glow", "Nimbus Team", "Warm gold contrast", "liquidglass",
            "#FFB14A", "#1D150B", 9f, 8f,
            Map.of("headerComponentColor", "#FFC46E", "bodyBackgroundColor", "#2A1E10", "textEnabledColor", "#FFF7EC", "textDisabledColor", "#B8A58C")
        ));
        builtinThemes.put("Neon Noir", createBuiltinTheme(
            "Neon Noir", "Nimbus Team", "Cyber neon aesthetic", "neon",
            "#FF007F", "#0A0C10", 12f, 8f,
            Map.of("headerComponentColor", "#FF007F", "bodyBackgroundColor", "#11141A", "textEnabledColor", "#00D4FF", "textDisabledColor", "#4B5563")
        ));
        builtinThemes.put("Sakura Glass", createBuiltinTheme(
            "Sakura Glass", "Nimbus Team", "Atmospheric petal pink", "sakura",
            "#FFB7C5", "#1A0F11", 10f, 9f,
            Map.of("headerComponentColor", "#FFC8D4", "bodyBackgroundColor", "#241618", "textEnabledColor", "#FFF0F3", "textDisabledColor", "#B89EA3")
        ));
        builtinThemes.put("Neon Gallery", createBuiltinTheme(
            "Neon Gallery", "Mikan", "High-fidelity neon UI kit", "neon_gallery",
            "#00d2ff", "#0a0a0c", 13f, 10f,
            Map.of("headerComponentColor", "#00d2ff", "bodyBackgroundColor", "#0d0d12", "textEnabledColor", "#ffffff", "textDisabledColor", "#565666")
        ));
        builtinThemes.put("Pink Cyber", createBuiltinTheme(
            "Pink Cyber", "Mikan", "Aggressive pink glow", "neon_gallery",
            "#ff2d95", "#0a0a0c", 13f, 10f,
            Map.of("headerComponentColor", "#ff2d95", "bodyBackgroundColor", "#120d10", "textEnabledColor", "#ffffff", "textDisabledColor", "#66565b")
        ));
        builtinThemes.put("Cyber Hologram", createBuiltinTheme(
            "Cyber Hologram", "Nimbus Team", "Flickering project style", "hologram",
            "#00D4FF", "#050A10", 12f, 4f,
            Map.of("headerComponentColor", "#00D4FF", "bodyBackgroundColor", "#081018", "textEnabledColor", "#F0FFFF", "textDisabledColor", "#506070")
        ));
        builtinThemes.put("Mercury Liquid", createBuiltinTheme(
            "Mercury Liquid", "Nimbus Team", "Weighted silver metal", "mercury",
            "#A0A0A0", "#080808", 8f, 12f,
            Map.of("headerComponentColor", "#D0D0D0", "bodyBackgroundColor", "#0A0A0A", "textEnabledColor", "#FFFFFF", "textDisabledColor", "#606060")
        ));
    }

    private static CustomUiTheme createBuiltinTheme(
        String name,
        String author,
        String description,
        String shader,
        String accentColor,
        String backgroundColor,
        float blurRadius,
        float cornerRadius,
        Map<String, Object> extra
    ) {
        CustomUiTheme.StyleConfig style = new CustomUiTheme.StyleConfig(
            accentColor,
            backgroundColor,
            blurRadius,
            cornerRadius,
            "Rubik",
            "Rubik",
            1.0f,
            null,
            extra
        );
        return new CustomUiTheme(name, author, description, "1.0.0", shader, style, List.of(), new HashMap<>(), new HashMap<>());
    }

    private void scanExternalThemes() {
        externalThemes.clear();
        File[] files = themesDir.toFile().listFiles();
        if (files == null) return;
        
        for (File f : files) {
            String name = f.getName();
            // Strictly only directories or .zip files
            if (f.isDirectory()) {
                loadExternalThemeFromDir(f);
            } else if (name.toLowerCase().endsWith(".zip")) {
                loadExternalThemeFromZip(f);
            }
        }
    }

    private void loadExternalThemeFromDir(File dir) {
        File jsonFile = new File(dir, "theme.json");
        if (!jsonFile.exists()) return;

        try (FileReader reader = new FileReader(jsonFile)) {
            CustomUiTheme theme = GSON.fromJson(reader, CustomUiTheme.class);
            if (theme != null) {
                externalThemes.put(theme.name(), theme);
                themeSources.put(theme.name(), dir);
                LOGGER.info("Loaded external theme from folder: " + theme.name());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load theme from folder " + dir.getName(), e);
        }
    }

    private void loadExternalThemeFromZip(File zipFile) {
        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry entry = zip.getEntry("theme.json");
            if (entry == null) return;

            try (InputStream is = zip.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(is)) {
                CustomUiTheme theme = GSON.fromJson(reader, CustomUiTheme.class);
                if (theme != null) {
                    externalThemes.put(theme.name(), theme);
                    themeSources.put(theme.name(), zipFile);
                    LOGGER.info("Loaded external theme from ZIP: " + theme.name());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load theme from ZIP " + zipFile.getName(), e);
        }
    }

    public void reloadAvailableThemes() {
        loadedThemes.clear();
        externalThemes.clear();
        themeSources.clear();
        
        // Scan for loose JSON themes in config root
        File[] jsonFiles = configDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles != null) {
            for (File f : jsonFiles) {
                try (FileReader reader = new FileReader(f)) {
                    CustomUiTheme theme = GSON.fromJson(reader, CustomUiTheme.class);
                    if (theme != null) loadedThemes.add(theme);
                } catch (Exception e) {
                    LOGGER.error("Failed to load loose JSON theme from " + f.getName(), e);
                }
            }
        }

        // Scan for ZIP/Folder themes
        scanExternalThemes();
        
        LOGGER.info("Reloaded themes: " + getThemeNames().size() + " total.");
    }

    public void reloadAssets() {
        dynamicTextures.values().forEach(id -> Minecraft.getInstance().getTextureManager().release(id));
        dynamicTextures.clear();
        
        File assetsDir = configDir.resolve("assets").toFile();
        File[] files = assetsDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (files == null) return;

        for (File f : files) {
            try (FileInputStream fis = new FileInputStream(f)) {
                NativeImage img = NativeImage.read(fis);
                String name = f.getName();
                Supplier<String> label = () -> "nimbus_dynamic_" + name;
                DynamicTexture tex = new DynamicTexture(label, img);
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath("nimbus", "dynamic/" + name.toLowerCase().replace(".png", ""));
                Minecraft.getInstance().getTextureManager().register(id, tex);
                dynamicTextures.put(name, id);
                LOGGER.info("Registered dynamic texture: " + name);
            } catch (IOException e) {
                LOGGER.error("Failed to load texture " + f.getName(), e);
            }
        }
    }

    public File createNewProject(String name) throws IOException {
        Path projectPath = themesDir.resolve(name);
        Files.createDirectories(projectPath);
        Files.createDirectories(projectPath.resolve("assets").resolve("shader"));
        Files.createDirectories(projectPath.resolve("assets").resolve("font"));
        
        File themeJson = projectPath.resolve("theme.json").toFile();
        if (!themeJson.exists()) {
             CustomUiTheme template = new CustomUiTheme(name, "User", "New Theme", "1.0.0", "none", 
                 new CustomUiTheme.StyleConfig("#FF539EFF", "#00000000", 10f, 10f, "Rubik", "Rubik", 1.0f, null, new HashMap<>()),
                 new ArrayList<>(), new HashMap<>(), new HashMap<>());
             try (FileWriter writer = new FileWriter(themeJson)) {
                 GSON.toJson(template, writer);
             }
        }
        reloadAvailableThemes();
        return themeJson;
    }

    public void importAsset(String themeName, File sourceFile) throws IOException {
        CustomUiTheme theme = externalThemes.get(themeName);
        if (theme == null) return;
        File source = themeSources.get(themeName);
        if (source == null || !source.isDirectory()) return;

        String ext = sourceFile.getName().toLowerCase();
        Path targetPath;
        if (ext.endsWith(".fsh") || ext.endsWith(".vsh")) {
            targetPath = source.toPath().resolve("assets").resolve("shader").resolve(sourceFile.getName());
        } else if (ext.endsWith(".ttf") || ext.endsWith(".otf")) {
            targetPath = source.toPath().resolve("assets").resolve("font").resolve(sourceFile.getName());
        } else if (ext.endsWith(".png") || ext.endsWith(".jpg") || ext.endsWith(".jpeg") || ext.endsWith(".webp")) {
            targetPath = source.toPath().resolve("assets").resolve(sourceFile.getName().replaceAll("\\.\\w+$", ".png"));
            // Convert to PNG if not already
            if (!ext.endsWith(".png")) {
                 BufferedImage img = ImageIO.read(sourceFile);
                 if (img != null) {
                     ImageIO.write(img, "png", targetPath.toFile());
                     return;
                 }
            }
        } else {
            return; // Unsupported
        }

        Files.createDirectories(targetPath.getParent());
        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("Imported asset: " + sourceFile.getName() + " to " + themeName);
        reloadAvailableThemes();
    }

    public void applyTheme(String themeName) {
        CustomUiTheme builtin = builtinThemes.get(themeName);
        if (builtin != null) {
            this.activeTheme = builtin;
            updateShaderId(builtin.shader());
            LOGGER.info("Applied builtin theme: " + themeName);
            return;
        }
        
        CustomUiTheme external = externalThemes.get(themeName);
        if (external != null) {
            this.activeTheme = external;
            updateShaderId(external.shader());
            LOGGER.info("Applied external theme: " + themeName);
            return;
        }

        reloadAvailableThemes();
        for (CustomUiTheme theme : loadedThemes) {
            if (theme.name().equals(themeName)) {
                this.activeTheme = theme;
                updateShaderId(theme.shader());
                reloadAssets();
                LOGGER.info("Applied custom theme: " + themeName);
                return;
            }
        }
    }

    private void updateShaderId(String shaderName) {
        // Always use internal liquid glass for standard widgets to ensure theme colors work
        activeShaderId = ResourceLocation.fromNamespaceAndPath("nimbus", "program/liquid_glass_gui");
        backgroundShaderId = activeShaderId; // Default
        useExternalShader = false; 

        // Resolve background shader (Apply to background pass)
        if (shaderName != null && !shaderName.isBlank() && !shaderName.equals("none") && !shaderName.equals("liquidglass")) {
            backgroundShaderId = ResourceLocation.fromNamespaceAndPath("nimbus", "program/" + shaderName);
            LOGGER.info("Theme using background shader: " + shaderName);
        }

        // Standard widget shader mappings (for transparency/effect on panels themselves)
        if (shaderName == null || shaderName.isBlank() || shaderName.equals("liquidglass") || shaderName.equals("default")) {
            activeShaderId = ResourceLocation.fromNamespaceAndPath("nimbus", "program/liquid_glass_gui");
        } else if (shaderName.equals("aurora")) {
            activeShaderId = ResourceLocation.fromNamespaceAndPath("nimbus", "program/aurora_gui");
        } else if (shaderName.equals("neon")) {
            activeShaderId = ResourceLocation.fromNamespaceAndPath("nimbus", "program/neon_sign_gui");
        } else if (shaderName.equals("hologram")) {
            activeShaderId = ResourceLocation.fromNamespaceAndPath("nimbus", "program/hologram_gui");
        } else if (shaderName.equals("mercury")) {
            activeShaderId = ResourceLocation.fromNamespaceAndPath("nimbus", "program/mercury_gui");
        } else if (shaderName.equals("sakura")) {
            activeShaderId = ResourceLocation.fromNamespaceAndPath("nimbus", "program/sakura_gui");
        } else if (shaderName.equals("matrix")) {
            activeShaderId = ResourceLocation.fromNamespaceAndPath("nimbus", "program/matrix_gui");
        } else if (shaderName.equals("neon_gallery")) {
            activeShaderId = ResourceLocation.fromNamespaceAndPath("nimbus", "program/neon_gallery_gui");
        } else {
            activeShaderId = ResourceLocation.fromNamespaceAndPath("nimbus", "program/" + shaderName);
        }
    }

    public void updateHotReload() {
        if (!useExternalShader || activeExternalShaderFile == null || !activeExternalShaderFile.exists() || activeExternalShaderFile.isDirectory() && !activeExternalShaderFile.getName().endsWith(".fsh")) {
            // If it's a directory (source of theme), we need to check the specific shader file inside it
            if (activeExternalShaderFile != null && activeExternalShaderFile.isDirectory()) {
                 // The actual shader file was already resolved in updateShaderId
                 // Wait! activeExternalShaderFile should be the .fsh file if it's a folder-theme
            }
            if (activeExternalShaderFile == null || !activeExternalShaderFile.getName().endsWith(".fsh")) return;
        }
        
        long now = System.currentTimeMillis();
        if (now - lastCheckTime > 500) {
            lastCheckTime = now;
            long m = activeExternalShaderFile.lastModified();
            if (m > lastFileTime) {
                lastFileTime = m;
                LOGGER.info("Hot-reloading detected for: " + activeExternalShaderFile.getName());
                // Force global shader reload
                Minecraft.getInstance().execute(() -> {
                    LiquidGlassPipelines.forceReload();
                    System.out.println("[Nimbus] Hot-reloaded: " + activeExternalShaderFile.getName());
                });
            }
        }
    }

    public static void downloadFont(String project, String fontUrl, String fileName) {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(fontUrl);
                Path target = INSTANCE.themesDir.resolve("themes").resolve(project).resolve("assets").resolve("font").resolve(fileName);
                Files.createDirectories(target.getParent());
                try (InputStream is = url.openStream()) {
                    Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                    INSTANCE.reloadAvailableThemes();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to download font: " + fontUrl, e);
            }
        });
    }

    public List<String> getThemeNames() {
        List<String> names = new ArrayList<>();
        names.addAll(builtinThemes.keySet());
        names.addAll(externalThemes.keySet());
        names.addAll(loadedThemes.stream().map(CustomUiTheme::name).collect(Collectors.toList()));
        return names;
    }

    public ResourceLocation getTexture(String filename) { return dynamicTextures.get(filename); }
    public CustomUiTheme getActiveTheme() { return activeTheme; }
    public ResourceLocation getActiveShaderId() { return activeShaderId; }
    public File getThemeSource(String name) { return themeSources.get(name); }
    public Map<String, CustomUiTheme> getThemes() { return externalThemes; }

    public void setActiveTheme(String name) {
        CustomUiTheme theme = externalThemes.get(name);
        if (theme == null) theme = builtinThemes.get(name);
        if (theme != null) {
            this.activeTheme = theme;
            loadThemeAssets(theme);
        }
    }

    public void saveTheme(CustomUiTheme theme) {
        if (theme == null) return;
        File source = themeSources.get(theme.name());
        if (source == null || !source.isDirectory()) {
            // If it's a loose JSON file or ZIP, we might need a different path, 
            // but for Studio "Projects" it's always a directory.
            source = themesDir.resolve(theme.name()).toFile();
        }
        
        File jsonFile = new File(source, "theme.json");
        try (FileWriter writer = new FileWriter(jsonFile)) {
            GSON.toJson(theme, writer);
            LOGGER.info("Theme saved to disk: " + theme.name());
        } catch (IOException e) {
            LOGGER.error("Failed to save theme " + theme.name(), e);
        }
    }

    public void loadThemeAssets(CustomUiTheme theme) {
        if (theme == null) return;
        this.activeTheme = theme;
        updateShaderId(theme.shader());
        reloadAvailableThemes();
        reloadAssets();
    }

    public void registerExternalTexture(String name, File f) {
        if (!f.exists()) return;
        try (FileInputStream fis = new FileInputStream(f)) {
            NativeImage img = NativeImage.read(fis);
            if (img != null) {
                Supplier<String> label = () -> "nimbus_external_" + name;
                DynamicTexture tex = new DynamicTexture(label, img);
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath("nimbus", "dynamic/" + name.toLowerCase());
                Minecraft.getInstance().getTextureManager().register(id, tex);
                LOGGER.info("Registered external texture: " + name + " -> " + id);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to register external texture " + name, e);
        }
    }
}
