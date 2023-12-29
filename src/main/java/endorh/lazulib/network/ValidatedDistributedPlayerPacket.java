package endorh.lazulib.network;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * Extra abstraction layer for {@link DistributedPlayerPacket} which
 * provides convenience methods to invalidate packets based on their
 * values and bounce them to the sender instead of relaying the packet.
 */
public abstract class ValidatedDistributedPlayerPacket extends DistributedPlayerPacket {
	
	private boolean invalid = false;
	private boolean propagate = true;
	
	/**
	 * @return True if the packet has been marked as invalid by using
	 * {@link ValidatedDistributedPlayerPacket#invalidate}
	 */
	protected boolean isInvalid() {
		return invalid;
	}
	
	/**
	 * Invalidate this packet.
	 */
	protected void invalidate() {
		invalid = true;
	}
	
	/**
	 * Cancel the invalidated status
	 */
	protected void unInvalidate() {
		invalid = false;
	}
	
	/**
	 * Clamps value between min and max, and if was out of range,
	 * invalidates this packet.<br>
	 * If called on clients, the value will be returned as passed,
	 * without clamping nor validation
	 */
	protected float validateClamp(float value, float min, float max) {
		if (!onServer)
			return value;
		if (value < min) {
			value = min;
			invalid = true;
		} else if (value > max) {
			value = max;
			invalid = true;
		}
		return value;
	}
	
	/**
	 * Invalidates the packet if the values are not close enough,
	 * and clamps a to the allowed value from b
	 *
	 * @param a First value
	 * @param b Second value
	 * @param delta Maximum distance, inclusive
	 * @return a
	 */
	protected float validateClose(float a, float b, float delta) {
		if (!onServer)
			return a;
		if (Math.abs(a - b) > delta) {
			a = a<b? b - delta : b + delta;
			invalid = true;
		}
		return a;
	}
	
	/**
	 * If propagate is set to false, the packet will not be relayed
	 * to other clients. It is true by default.
	 */
	protected void setPropagate(boolean propagate) {
		this.propagate = propagate;
	}
	
	/**
	 * If propagate is set to false, the packet will not be relayed to
	 * other clients. It is true by default.
	 */
	protected boolean getPropagate() {
		return this.propagate;
	}
	
	/**
	 * Calls {@link DistributedPlayerPacket#onServer} and, if the packet
	 * is invalidated after the call, cancels packet relay and bounces the
	 * packet to the sender.
	 */
	@Override public final boolean onServerCancellable(Player sender, Context ctx) {
		onServer(sender, ctx);
		if (isInvalid()) {
			sendBack();
			return propagate;
		}
		return true;
	}
}
