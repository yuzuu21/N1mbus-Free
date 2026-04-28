package n1mbus.ghs.client.module;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import n1mbus.ghs.client.module.modules.combat.*;
import n1mbus.ghs.client.module.modules.movement.*;
import n1mbus.ghs.client.module.modules.visual.*;
import n1mbus.ghs.client.module.modules.misc.*;

public class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        // Combat
        add(new KillAura());
        add(new Velocity());

        // Movement
        add(new Sprint());
        add(new Step());

        // Visuals
        add(new FullBright());
        add(new ESP());

        // Misc
        add(new ClickGuiModule());
        add(new NoFall());
    }

    private void add(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getModulesInCategory(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        return (T) modules.stream().filter(m -> m.getClass() == clazz).findFirst().orElse(null);
    }
}
