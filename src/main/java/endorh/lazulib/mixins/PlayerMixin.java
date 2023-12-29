package endorh.lazulib.mixins;

import endorh.lazulib.events.PlayerTravelEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects {@link PlayerTravelEvent} on {@link Player#travel}
 */
@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
	/**
	 * Dummy mixin constructor, required by the Java compiler to inherit from superclass.
	 * @param type ignored
	 * @param worldIn ignored
	 * @throws IllegalAccessException always
	 */
	protected PlayerMixin(EntityType<? extends LivingEntity> type, Level worldIn)
	  throws IllegalAccessException {
		super(type, worldIn);
		throw new IllegalAccessException("Mixin dummy constructor shouldn't be called!");
	}
	
	/**
	 * Inject {@link PlayerTravelEvent} on {@link Player#travel}
	 * The event is cancellable. When cancelled, the travel method is skipped.
	 * @param travelVector {@linkplain Vec3}(moveStrafing, moveVertical, moveForward)
	 * @param callbackInfo Mixin {@linkplain CallbackInfo}
	 *
	 * @see Player#travel(Vec3)
	 * @see LivingEntity#aiStep()
	 */
	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	public void _lazulib_travel(Vec3 travelVector, CallbackInfo callbackInfo) {
		//noinspection ConstantConditions
		PlayerTravelEvent event = new PlayerTravelEvent(
		  (Player)(LivingEntity)this, travelVector);
		MinecraftForge.EVENT_BUS.post(event);
		if (event.isCanceled()) callbackInfo.cancel();
	}
}
