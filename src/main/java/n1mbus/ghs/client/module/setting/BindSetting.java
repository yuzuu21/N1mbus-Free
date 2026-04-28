package n1mbus.ghs.client.module.setting;

import org.lwjgl.glfw.GLFW;

public class BindSetting extends Setting<Integer> {
    public BindSetting(String name, Integer defaultValue) {
        super(name, defaultValue);
    }

    public String getKeyName() {
        if (getValue() == 0) return "None";
        String name = GLFW.glfwGetKeyName(getValue(), 0);
        if (name == null) {
            return switch (getValue()) {
                case GLFW.GLFW_KEY_INSERT -> "Insert";
                case GLFW.GLFW_KEY_LEFT_SHIFT -> "LShift";
                case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RShift";
                case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCtrl";
                case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCtrl";
                case GLFW.GLFW_KEY_ESCAPE -> "Esc";
                default -> "Key " + getValue();
            };
        }
        return name.toUpperCase();
    }
}
