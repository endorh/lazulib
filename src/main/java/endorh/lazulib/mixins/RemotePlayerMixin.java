package endorh.lazulib.mixins;

import com.mojang.authlib.GameProfile;
import endorh.lazulib.events.PlayerTravelEvent.RemotePlayerTravelEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects {@link RemotePlayerTravelEvent} on
 * {@link RemotePlayer#aiStep()}
 */
@Mixin(RemotePlayer.class)
@OnlyIn(Dist.CLIENT)
public class RemotePlayerMixin extends AbstractClientPlayer {
	/**
	 * Dummy mixin constructor, required by the Java compiler to inherit from superclass.
	 * @param level ignored
	 * @param profile ignored
    * @throws IllegalAccessException always
	 */
	protected RemotePlayerMixin(
	  ClientLevel level, GameProfile profile
	) throws IllegalAccessException {
		super(level, profile);
		throw new IllegalAccessException("Mixin dummy constructor shouldn't be called");
	}
	
	/**
	 * Inject {@link RemotePlayerTravelEvent} on
	 * {@link RemotePlayer#aiStep()}.
	 * @param callbackInfo Mixin {@link CallbackInfo}
	 */
	@Inject(
	  method = "aiStep",
	  at = @At(
	    value = "INVOKE_STRING", args = "ldc=push",
	    target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V"))
	protected void _lazulib_livingTick(CallbackInfo callbackInfo) {
		MinecraftForge.EVENT_BUS.post(new RemotePlayerTravelEvent(this));
	}
}
