package n1mbus.ghs.scratch;

import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import java.lang.reflect.Field;

public class FieldScanner {
    public static void scan(GameRenderer gr) {
        for (Field f : GameRenderer.class.getDeclaredFields()) {
            if (f.getType() == Matrix4f.class) {
                f.setAccessible(true);
                try {
                    Matrix4f m = (Matrix4f) f.get(gr);
                    System.out.println("Field: " + f.getName() + " Value: " + m);
                } catch (Exception e) {}
            }
        }
    }
}
