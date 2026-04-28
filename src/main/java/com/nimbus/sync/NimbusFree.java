package com.nimbus.sync;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.DeltaTracker;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;

/**
 * v2.2.31: Lightweight stats sync + HWND relay.
 * Minimal JNI interaction to avoid interfering with native window logic.
 */
public class NimbusFree {
    private static long lastHwnd = 0;
    private static long lastRectLogMs = 0;

    public static void init() {
        System.out.println("[NimbusGhs] Initializing native UI engine...");
        try {
            if (NimbusNative.isLoaded()) {
                NimbusNative.init();
                System.out.println("[NimbusGhs] Native engine v2.2.31 bound successfully.");
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker delta) {
        syncNow();
    }

    // Always sync route independent of draw callbacks (called from tick)
    public static void tick() {
        syncNow();
    }

    private static void syncNow() {
        if (!NimbusNative.isLoaded()) return;

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) return;

        long windowHandle = client.getWindow().getWindow();
        long hwnd = GLFWNativeWin32.glfwGetWin32Window(windowHandle);
        if (hwnd != 0 && hwnd != lastHwnd) {
            NimbusNative.setTargetHWND(hwnd);
            lastHwnd = hwnd;
            System.out.println("[NimbusGhs] Target HWND bound: " + hwnd);
        }
        int[] x = new int[1];
        int[] y = new int[1];
        int[] fbW = new int[1];
        int[] fbH = new int[1];
        int[] winW = new int[1];
        int[] winH = new int[1];
        GLFW.glfwGetWindowPos(windowHandle, x, y);
        GLFW.glfwGetFramebufferSize(windowHandle, fbW, fbH);
        GLFW.glfwGetWindowSize(windowHandle, winW, winH);

        int finalW = fbW[0];
        int finalH = fbH[0];
        if (finalW <= 1 || finalH <= 1) {
            finalW = winW[0];
            finalH = winH[0];
        }
        if (finalW <= 1 || finalH <= 1) {
            finalW = client.getWindow().getWidth();
            finalH = client.getWindow().getHeight();
        }
        finalW = Math.max(1, finalW);
        finalH = Math.max(1, finalH);

        NimbusNative.setTargetRect(x[0], y[0], finalW, finalH);
        long now = System.currentTimeMillis();
        if (now - lastRectLogMs > 2500) {
            lastRectLogMs = now;
            System.out.println(
                "[NimbusGhs] Rect sync x=" + x[0] + " y=" + y[0]
                    + " fb=" + fbW[0] + "x" + fbH[0]
                    + " win=" + winW[0] + "x" + winH[0]
                    + " final=" + finalW + "x" + finalH
            );
        }

        // Sync player stats to Rust
        LocalPlayer player = client.player;
        if (player != null && client.level != null) {
            boolean isPlaying = (client.screen == null);
            NimbusNative.updateStats(
                player.getHealth(),
                player.getMaxHealth(),
                (float) player.getFoodData().getFoodLevel(),
                player.getArmorValue(),
                isPlaying
            );
        } else {
            NimbusNative.updateStats(20, 20, 20, 0, false);
        }
    }
}
