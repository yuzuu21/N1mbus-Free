package n1mbus.ghs.client;

import com.nimbus.sync.NimbusFree;
import n1mbus.ghs.client.render.Esp3DWorldRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class N1mbusGhsClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("NimbusGhs");
    public static Minecraft minecraft;
    private static KeyMapping clickGuiKey;

    @Override
    public void onInitializeClient() {
        System.out.println(">>> [NimbusGhs] MOD INITIALIZING FOR 1.21.8 <<<");
        minecraft = Minecraft.getInstance();

        try {
            // Use standard 1.21.8 constructor
            clickGuiKey = new KeyMapping(
                "key.nimbus.clickgui", 
                InputConstants.Type.KEYSYM, 
                GLFW.GLFW_KEY_INSERT, 
                "category.nimbus.general"
            );
            LOGGER.info("KeyMapping registered (1.21.8 signature).");
        } catch (Throwable t) {
            LOGGER.error("KeyMapping registration failed: {}", t.getMessage());
        }
        
        try {
            NimbusFree.init();
            LOGGER.info("NimbusNative JNI Bridge initialized.");
        } catch (Throwable t) {
            LOGGER.error("JNI Initialization failed: {}", t.getMessage());
        }

        try {
            System.out.println("[NimbusGhs] Initializing 3D ESP renderer...");
            Esp3DWorldRenderer.init();
            System.out.println("[NimbusGhs] 3D ESP renderer initialized.");
        } catch (Throwable t) {
            System.out.println("[NimbusGhs] 3D ESP renderer init failed: " + t);
            t.printStackTrace();
        }
    }

    public static KeyMapping getClickGuiKey() {
        return clickGuiKey;
    }
}
