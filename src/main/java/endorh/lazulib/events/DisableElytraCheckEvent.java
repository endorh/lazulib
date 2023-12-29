package endorh.lazulib.events;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Allows mods to conditionally disable the elytra movement check.<br>
 * Setting disable to true by using {@link DisableElytraCheckEvent#setDisable}
 * will cause the check to be ignored for this event.<br>
 * This event won't be fired if the check is already disabled unconditionally
 * by the game rules, or if the player is not elytra flying.
 */
public class DisableElytraCheckEvent extends Event {
	/**
	 * The player affected by the check
	 */
	public final ServerPlayer player;
	/**
	 * The movement packet that triggered the check
	 */
	public final ServerboundMovePlayerPacket packet;
	/**
	 * The difference between the speed of the player and its actual movement<br>
	 * This is what is actually checked.
	 */
	public final double excess;
	/**
	 * The amount of packets stacked for this check.<br>
	 * Multiplies the check amount.
	 */
	public final int stackedPackets;
	private boolean disable = false;
	
	public DisableElytraCheckEvent(
	  ServerPlayer player, ServerboundMovePlayerPacket packet, double excess, int stackedPackets
	) {
		this.player = player;
		this.packet = packet;
		this.excess = excess;
		this.stackedPackets = stackedPackets;
	}
	
	/**
	 * If disable is true when the event returns, the elytra
	 * movement check is skipped
	 * @return Current disable state
	 */
	public boolean getDisable() {
		return disable;
	}
	
	/**
	 * If disable is true when the event returns, the elytra movement
	 * check is skipped
	 * @param disable True to disable the check
	 */
	@SuppressWarnings("unused")
	public void setDisable(boolean disable) {
		this.disable = disable;
	}
}
