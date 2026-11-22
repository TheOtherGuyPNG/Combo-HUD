package combo.mixin;
import combo.Combo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.multiplayer.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;

@Mixin(MultiPlayerGameMode.class)
class SwingMixin {
    @Inject(method="attack",at=@At("HEAD"))
    private void comboAttack(Player attacker, Entity target, CallbackInfo ci){Combo.attack(attacker,target);}
}
@Mixin(ClientPacketListener.class)
class DamageMixin {
    @Inject(method="handleEntityEvent",at=@At("TAIL"))
    private void comboEntityEvent(net.minecraft.network.protocol.game.ClientboundEntityEventPacket packet,CallbackInfo ci){Combo.entityEvent(packet);}
    @Inject(method="handleDamageEvent",at=@At("TAIL"))
    private void comboDamage(ClientboundDamageEventPacket packet,CallbackInfo ci){Combo.damage(packet);}
}


