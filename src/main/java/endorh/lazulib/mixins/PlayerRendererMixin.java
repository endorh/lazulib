package endorh.lazulib.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import endorh.lazulib.events.SetupRotationsRenderPlayerEvent;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects {@link SetupRotationsRenderPlayerEvent} on
 * {@link PlayerRenderer#setupRotations}
 */
@Mixin(PlayerRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class PlayerRendererMixin
  extends LivingEntityRenderer<AbstractClientPlayer,
  PlayerModel<AbstractClientPlayer>> {
	/**
	 * Dummy mixin constructor, required by the Java compiler to inherit from superclass.
	 * @param ctx ignored
	 * @param model ignored
	 * @param shadowRadius ignored
	 * @throws IllegalAccessException always
	 */
   protected PlayerRendererMixin(Context ctx, PlayerModel<AbstractClientPlayer> model, float shadowRadius)
	  throws IllegalAccessException {
		super(ctx, model, shadowRadius);
		throw new IllegalAccessException("Mixin dummy constructor shouldn't be called!");
	}
	
	/**
	 * Inject {@link SetupRotationsRenderPlayerEvent} on
	 * {@link PlayerRenderer#setupRotations}. The event is cancellable.
	 * If cancelled, the {@code applyRotations} method call will be
	 * skipped. The super method call can be invoked through the
	 * provided lambda.
	 * @param player Player being rendered
	 * @param mStack Transformation matrix stack
	 * @param ageInTicks Age of the player in ticks, containing partialTicks
	 * @param rotationYaw Smoothed yaw between this tick and the next for this frame
	 * @param partialTicks Interpolation progress between this tick and the next for this frame
	 * @param callbackInfo Mixin {@link CallbackInfo}
	 */
	@Inject(method = "setupRotations*", at = @At("HEAD"), cancellable = true)
	protected void _lazulib_applyRotations(
	  AbstractClientPlayer player, PoseStack mStack,
	  float ageInTicks, float rotationYaw, float partialTicks,
	  CallbackInfo callbackInfo
	) {
		SetupRotationsRenderPlayerEvent event = new SetupRotationsRenderPlayerEvent(
			(PlayerRenderer) (LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) this,
			player, mStack, ageInTicks, rotationYaw, partialTicks,
			vec -> super.setupRotations(
				player, mStack, vec.x(), vec.y(), vec.z()));
		MinecraftForge.EVENT_BUS.post(event);
		if (event.isCanceled()) callbackInfo.cancel();
	}
}
