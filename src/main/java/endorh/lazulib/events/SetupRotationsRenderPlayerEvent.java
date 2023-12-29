package endorh.lazulib.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import org.joml.Vector3f;

import java.util.function.Consumer;

/**
 * Generates on every call to {@link PlayerRenderer#setupRotations},
 * when the rotations applied to the {@link PlayerModel} are performed.
 * The event is cancellable. If cancelled, the default rotation
 * behaviour won't take place.
 * It's possible to run the super method by using the {@code callSuper}
 * {@linkplain Consumer} provided with the event.
 */
@Cancelable
public class SetupRotationsRenderPlayerEvent extends Event {
	/**
	 * Player being rendered
	 */
	public final AbstractClientPlayer player;
	/**
	 * Source of the event
	 */
	public final PlayerRenderer renderer;
	/**
	 * Transformation matrix stack
	 */
	public final PoseStack matrixStack;
	/**
	 * Age of player in ticks, containing the partial fraction.
	 * Is equal to {@code player.ticksExisted + partialTicks}
	 */
	public final float ageInTicks;
	/**
	 * Smoothed yaw for this frame, interpolated between
	 * {@code player.prevRotationYaw} and {@code player.rotationYaw}
	 * by {@code partialTicks}
	 */
	public final float rotationYaw;
	/**
	 * Interpolation progress between this tick and the next (0~1)
	 */
	public final float partialTicks;
	/**
	 * Consumer that may be used to call the super method,
	 * {@link LivingEntityRenderer#render}. The required
	 * {@linkplain Vector3f} should contain the 3 float arguments
	 * of the call.
	 */
	public final Consumer<Vector3f> callSuper;
	
	public SetupRotationsRenderPlayerEvent(
	  PlayerRenderer renderer, AbstractClientPlayer player,
	  PoseStack mStack, float ageInTicks, float rotationYaw,
	  float partialTicks, Consumer<Vector3f> callSuper
	) {
		super();
		this.player = player;
		this.renderer = renderer;
		this.matrixStack = mStack;
		this.ageInTicks = ageInTicks;
		this.rotationYaw = rotationYaw;
		this.partialTicks = partialTicks;
		this.callSuper = callSuper;
	}
}
