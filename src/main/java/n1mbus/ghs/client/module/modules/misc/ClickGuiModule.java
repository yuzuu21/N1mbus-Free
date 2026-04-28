package n1mbus.ghs.client.module.modules.misc;

import n1mbus.ghs.client.module.Category;
import n1mbus.ghs.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class ClickGuiModule extends Module {
    public ClickGuiModule() {
        super("ClickGUI", "Opens the configuration GUI.", Category.MISC);
        // Default bind is INSERT — set via the inherited bind field
        setKey(GLFW.GLFW_KEY_INSERT);
    }

    @Override
    public void onEnable() {
        // Immediately disable since KeyboardMixin handles opening.
        setEnabled(false);
    }
}
