package n1mbus.ghs.mixin.logical;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import n1mbus.ghs.client.module.ModuleManager;
import n1mbus.ghs.client.module.modules.combat.KillAura;
import n1mbus.ghs.client.module.modules.combat.Velocity;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    private float preYaw, prePitch;
    private boolean spoofing = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        n1mbus.ghs.client.module.ModuleManager.INSTANCE.getModules().stream()
            .filter(n1mbus.ghs.client.module.Module::isEnabled)
            .forEach(n1mbus.ghs.client.module.Module::onTick);

        LocalPlayer player = (LocalPlayer) (Object) this;
        Velocity velocity = ModuleManager.INSTANCE.getModule(Velocity.class);
        if (velocity != null) {
            velocity.onPlayerTick(player);
        }
    }

    private boolean spoofingFall = false;

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void onSendPositionPre(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        KillAura aura = ModuleManager.INSTANCE.getModule(KillAura.class);
        n1mbus.ghs.client.module.modules.misc.NoFall nofall = ModuleManager.INSTANCE.getModule(n1mbus.ghs.client.module.modules.misc.NoFall.class);
        
        if (nofall != null && nofall.isEnabled() && nofall.isSpoofingOnGround()) {
            this.spoofingFall = true;
            nofall.setPreOnGround(player.onGround());
            player.setOnGround(true);
        } else {
            this.spoofingFall = false;
        }

        if (aura != null && aura.isEnabled() && aura.isSpoofingRotation()) {
            this.spoofing = true;
            this.preYaw = player.getYRot();
            this.prePitch = player.getXRot();
            
            float[] rots = aura.getSpoofedRotations();
            player.setYRot(rots[0]);
            player.setXRot(rots[1]);
        } else {
            this.spoofing = false;
        }
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void onSendPositionPost(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (this.spoofingFall) {
            n1mbus.ghs.client.module.modules.misc.NoFall nofall = ModuleManager.INSTANCE.getModule(n1mbus.ghs.client.module.modules.misc.NoFall.class);
            if (nofall != null) {
                player.setOnGround(nofall.getPreOnGround());
            }
            this.spoofingFall = false;
        }

        if (this.spoofing) {
            player.setYRot(this.preYaw);
            player.setXRot(this.prePitch);
            this.spoofing = false;
            
            // Process attack after rotation is sent
            KillAura aura = ModuleManager.INSTANCE.getModule(KillAura.class);
            if (aura != null) {
                aura.onRotationSent();
            }
        }
    }
}
