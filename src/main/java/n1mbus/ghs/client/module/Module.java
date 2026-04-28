package n1mbus.ghs.client.module;

import java.util.ArrayList;
import java.util.List;
import n1mbus.ghs.client.module.setting.BindSetting;
import n1mbus.ghs.client.module.setting.Setting;

public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private final List<Setting<?>> settings = new ArrayList<>();

    // Every module gets a bind setting automatically
    public final BindSetting bind = new BindSetting("Bind", 0);

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
        // Bind is always the last setting, added automatically
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) onEnable();
        else onDisable();
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void onEnable() {}
    public void onDisable() {}

    public int getKey() { return bind.getValue(); }
    public void setKey(int key) { bind.setValue(key); }

    protected <T extends Setting<?>> T addSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    public void onTick() {}

    public List<Setting<?>> getSettings() {
        // Return all settings + the bind at the end
        List<Setting<?>> all = new ArrayList<>(settings);
        all.add(bind);
        return all;
    }

    /** Get only the user-configured settings (no bind). */
    public List<Setting<?>> getUserSettings() {
        return settings;
    }
}
