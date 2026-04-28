package n1mbus.ghs.mixin.logical;

import n1mbus.ghs.client.accessor.GuiGraphicsAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.spongepowered.asm.mixin.Mixin;
import java.util.Deque;
import java.lang.reflect.Field;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin implements GuiGraphicsAccessor {

    @Override
    public ScreenRectangle n1mbusghs$peekScissor() {
        try {
            // Search for stack by type rather than field name
            for (Field field : GuiGraphics.class.getDeclaredFields()) {
                // In 1.21.x, scissor stack is an internal ScissorStack class or a Deque
                if (field.getType().getSimpleName().equals("ScissorStack") || Deque.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object obj = field.get(this);
                    if (obj instanceof Deque) {
                        return ((Deque<ScreenRectangle>) obj).peekLast();
                    } else if (obj != null) {
                        // Check for inner stack field in ScissorStack
                        for (Field innerField : obj.getClass().getDeclaredFields()) {
                            if (Deque.class.isAssignableFrom(innerField.getType())) {
                                innerField.setAccessible(true);
                                Deque<ScreenRectangle> stack = (Deque<ScreenRectangle>) innerField.get(obj);
                                return stack.peekLast();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently handle failures for safe rendering
        }
        return null;
    }
}
