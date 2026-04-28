package n1mbus.ghs.mixin.logical;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import n1mbus.ghs.client.module.ModuleManager;
import n1mbus.ghs.client.module.modules.visual.ESP;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        ESP esp = (ESP) ModuleManager.INSTANCE.getModules().stream()
                .filter(m -> m.getName().equals("ESP"))
                .findFirst().orElse(null);
        
        if (esp != null && esp.isEnabled() && esp.getType().equals("Glow")) {
            if (esp.shouldRender((Entity) (Object) this)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColor(CallbackInfoReturnable<Integer> cir) {
        ESP esp = (ESP) ModuleManager.INSTANCE.getModules().stream()
                .filter(m -> m.getName().equals("ESP"))
                .findFirst().orElse(null);
        
        if (esp != null && esp.isEnabled() && esp.getType().equals("Glow")) {
            if (esp.shouldRender((Entity) (Object) this)) {
                cir.setReturnValue(esp.getEspColor((Entity) (Object) this));
            }
        }
    }
}
