package n1mbus.ghs.client.module.modules.visual;

import n1mbus.ghs.client.module.Category;
import n1mbus.ghs.client.module.Module;
import n1mbus.ghs.client.module.setting.BooleanSetting;
import n1mbus.ghs.client.module.setting.ColorSetting;
import n1mbus.ghs.client.module.setting.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class ESP extends Module {
    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting animals = new BooleanSetting("Animals", false);
    private final BooleanSetting monsters = new BooleanSetting("Monsters", false);
    private final ModeSetting type = new ModeSetting("Type", "Glow", java.util.Arrays.asList("Glow", "Box", "3D Box"));
    private final ModeSetting colorMode = new ModeSetting("Mode", "Static", java.util.Arrays.asList("Static", "Team", "Rainbow"));
    private final ColorSetting color = new ColorSetting("Color", 0x539EFF);

    public ESP() {
        super("ESP", "Highlights entities through walls.", Category.VISUALS);
        addSetting(players);
        addSetting(animals);
        addSetting(monsters);
        addSetting(type);
        addSetting(colorMode);
        addSetting(color);
        setKey(org.lwjgl.glfw.GLFW.GLFW_KEY_Y);
    }

    public String getType() {
        return type.getValue();
    }

    public int getEspColor(Entity entity) {
        String mode = colorMode.getValue();
        
        if (mode.equals("Rainbow")) {
            float hue = (System.currentTimeMillis() % 4000) / 4000f;
            return java.awt.Color.HSBtoRGB(hue, 0.6f, 1f);
        }
        
        if (mode.equals("Team") && entity instanceof Player) {
            Player p = (Player) entity;
            if (p.getTeam() != null && p.getTeam().getColor().getColor() != null) {
                return p.getTeam().getColor().getColor();
            }
        }
        
        return color.getValue();
    }

    public boolean shouldRender(Entity entity) {
        if (!isEnabled()) return false;
        if (entity == Minecraft.getInstance().player) return false;
        if (entity instanceof Player) return players.getValue();
        if (entity instanceof net.minecraft.world.entity.monster.Monster) return monsters.getValue();
        return animals.getValue();
    }
}
