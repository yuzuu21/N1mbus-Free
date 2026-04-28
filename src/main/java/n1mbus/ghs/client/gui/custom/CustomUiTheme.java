package n1mbus.ghs.client.gui.custom;

import java.util.List;
import java.util.Map;

public record CustomUiTheme(
    String name,
    String author,
    String description,
    String version,
    String shader, // Background shader
    StyleConfig style,
    List<ComponentConfig> layout,
    Map<String, String> assets, // Mapping of logical names to file paths (fonts, images, etc)
    Map<String, Object> metadata // Extra info like creation date, tags
) {
    public record StyleConfig(
        String accentColor,
        String backgroundColor,
        float blurRadius,
        float cornerRadius,
        String fontName, // Primary font
        String headerFont, // Header-specific font
        float globalBlurAlpha,
        PanelStyle panel,
        Map<String, Object> extra
    ) {
        public record PanelStyle(
            String backgroundColor,
            float cornerRadius,
            String outlineColor,
            float outlineWidth,
            String shader, // Per-panel shader
            float opacity // Visual opacity
        ) {}
    }

    public record ComponentConfig(
        String type,
        String id,
        int x,
        int y,
        int width,
        int height,
        Map<String, Object> properties
    ) {}
}
