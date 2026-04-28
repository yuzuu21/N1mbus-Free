package com.nimbus.sync;
import java.io.File;
import java.nio.file.StandardCopyOption;

public class NimbusNative {
    private static boolean loaded = false;
    static {
        try {
            File dllFile = new File("nimbusfree.dll");
            // v2.2.31: Always try to extract the latest DLL from JAR to ensure updates are applied.
            System.out.println("[NimbusGhs] Updating native engine from JAR...");
            try (java.io.InputStream is = NimbusNative.class.getResourceAsStream("/nimbusfree.dll")) {
                if (is != null) {
                    java.nio.file.Files.copy(is, dllFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    System.err.println("[NimbusGhs] Could not find nimbusfree.dll in JAR resources!");
                }
            } catch (java.io.IOException e) {
                // If the file is locked (game already running?), we might not be able to overwrite.
                System.err.println("[NimbusGhs] Could not overwrite DLL (possibly in use): " + e.getMessage());
            }

            if (dllFile.exists()) {
                System.load(dllFile.getAbsolutePath());
                loaded = true;
                System.out.println("[NimbusGhs] Native engine v2.2.31 loaded successfully.");
            }
        } catch (Throwable t) {
            System.err.println("[NimbusGhs] Native loading failed: " + t.getMessage());
            t.printStackTrace();
        }
    }
    public static boolean isLoaded() { return loaded; }
    public static native void init();
    public static native void updateStats(float hp, float maxHp, float food, int armor, boolean isPlaying);
    public static native void setTargetHWND(long hwnd);
    public static native void setTargetRect(int x, int y, int w, int h);
    
    public static native void submitUiCommands(String json);
    public static native void setInteractive(boolean interactive);
    public static native boolean checkCloseRequest();
    public static native void setHtml(String html);
    public static native String pollIpcMessages();


}
