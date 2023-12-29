package endorh.lazulib.mixins;

import endorh.lazulib.events.PlayerTurnEvent;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects {@link PlayerTurnEvent} on {@link Player#turn}.<br>
 * The mixin is applied to {@linkplain Entity} instead of
 * {@linkplain Player} because the method is inherited from
 * {@linkplain Entity} in {@linkplain Player}
 */
@Mixin(Entity.class)
public abstract class EntityMixin extends CapabilityProvider<Entity> {
	/**
	 * Dummy mixin constructor, required by the Java compiler to inherit from superclass.
	 * @param baseClass ignored
	 * @throws IllegalAccessException always
	 */
	protected EntityMixin(Class<Entity> baseClass) throws IllegalAccessException {
		super(baseClass);
		throw new IllegalAccessException("Mixin dummy constructor shouldn't be called!");
	}
	
	/**
	 * Inject {@link PlayerTurnEvent} on {@link Player#turn}.<br>
	 *
	 * The event is cancellable. If cancelled, the rotateTowards method
	 * call will be skipped.<br>
	 *
	 * Applied here rather than on the Player class because
	 * the method is inherited.<br>
	 *
	 * @param yaw Unscaled relative yaw rotation
	 * @param pitch Unscaled relative pitch rotation
	 * @param callbackInfo Mixin {@linkplain CallbackInfo}
	 *
	 * @see Player#turn(double, double)
	 * @see MouseHandler#turnPlayer()
	 */
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	public void _lazulib_rotateTowards(double yaw, double pitch, CallbackInfo callbackInfo) {
		// noinspection ConstantConditions
		if ((CapabilityProvider<Entity>)this instanceof Player) {
			PlayerTurnEvent event = new PlayerTurnEvent(
			  (Player)(CapabilityProvider<Entity>)this, yaw, pitch);
			MinecraftForge.EVENT_BUS.post(event);
			if (event.isCanceled()) callbackInfo.cancel();
		}
	}
}
