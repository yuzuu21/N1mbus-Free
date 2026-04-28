package n1mbus.ghs.client.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DropManager {
    public interface DropListener {
        void onFilesDropped(List<File> files);
    }

    private static final List<DropListener> listeners = new ArrayList<>();

    public static void register(DropListener listener) {
        listeners.add(listener);
    }

    public static void unregister(DropListener listener) {
        listeners.remove(listener);
    }

    public static void init(long windowHandle) {
        GLFW.glfwSetDropCallback(windowHandle, (window, count, names) -> {
            List<File> files = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String path = GLFWDropCallback.getName(names, i);
                files.add(new File(path));
            }
            
            Minecraft.getInstance().execute(() -> {
                for (DropListener listener : listeners) {
                    listener.onFilesDropped(files);
                }
                
                // Also notify active screen if it implements listener
                Screen current = Minecraft.getInstance().screen;
                if (current instanceof DropListener dl) {
                    dl.onFilesDropped(files);
                }
            });
        });
    }
}

