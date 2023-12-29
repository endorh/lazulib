package endorh.lazulib.events;

import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Generates on every livingTick(), when the player motion should be updated.
 * The field travelVector contains the travelVector for that tick.
 * You may want to read the implementations for the travel method
 * in the {@linkplain LivingEntity} and {@linkplain Player} classes
 *
 * @see LivingEntity#travel(Vec3)
 * @see Player#travel(Vec3)
 */
@Cancelable
public class PlayerTravelEvent extends Event {
	/**
	 * Player being ticked
	 */
	public final Player player;
	/**
	 * Travel vector for the event:
	 * {@linkplain Vec3}(moveStrafing, moveVertical, moveForward)
	 */
	public final Vec3 travelVector;
	
	public PlayerTravelEvent(Player player, Vec3 travelVector) {
		this.player = player;
		this.travelVector = travelVector;
	}
	
	/**
	 * Generates on every livingTick() of {@link RemotePlayer}s,
	 * which do not call their {@link Player#travel} method
	 * @see PlayerTravelEvent
	 */
	public static class RemotePlayerTravelEvent extends Event {
		/**
		 * Player being ticked
		 */
		public final Player player;
		
		public RemotePlayerTravelEvent(Player player) {
			this.player = player;
		}
	}
}
