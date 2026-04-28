package n1mbus.ghs.mixin.accessor;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundSetEntityMotionPacket.class)
public interface EntityVelocityPacketAccessor {
    @Accessor("id")
    int getId();

    @Accessor("xa")
    int getXa();
    @Accessor("ya")
    int getYa();
    @Accessor("za")
    int getZa();

    @Accessor("xa")
    void setXa(int xa);
    @Accessor("ya")
    void setYa(int ya);
    @Accessor("za")
    void setZa(int za);
}
