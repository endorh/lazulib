package endorh.lazulib.mixins;

import endorh.lazulib.common.LogUtil;
import endorh.lazulib.events.DisableElytraCheckEvent;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.Key;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Redirects checks to {@link GameRules#RULE_DISABLE_ELYTRA_MOVEMENT_CHECK} to
 * {@link DisableElytraCheckEvent}, when the rules do not already disable
 * the check and a player is flying
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
	@Unique private static final Logger LOGGER = LogManager.getLogger();

	/**
	 * The player being processed
	 */
	@Shadow(aliases = "player") public ServerPlayer player;
	
	@Shadow(aliases = "firstGoodX") private double firstGoodX;
	@Shadow(aliases = "firstGoodY") private double firstGoodY;
	@Shadow(aliases = "firstGoodZ") private double firstGoodZ;
	
	@Shadow(aliases = "receivedMovePacketCount") private int receivedMovePacketCount;
	@Shadow(aliases = "knownMovePacketCount") private int knownMovePacketCount;
	
	/**
	 * Redirect the query for the game rule
	 * {@link GameRules#RULE_DISABLE_ELYTRA_MOVEMENT_CHECK} when applying the
	 * speed check at {@link ServerGamePacketListenerImpl#handleMovePlayer} to
	 * the event {@link DisableElytraCheckEvent}.<br>
	 *
	 * The redirection only takes place if the check is not already disabled
	 * unconditionally by the rules, and if the player is flying, since
	 * otherwise the redirect would serve no purpose.
	 *
	 * @param rules {@link GameRules} in effect
	 * @param key Key for the {@link GameRules#RULE_DISABLE_ELYTRA_MOVEMENT_CHECK} rule.
	 *            If it does not match, the mixin is considered as missed, and will
	 *            not interfere with the call, after logging a warning.
	 * @param packet The packet that triggered the check
	 * @return True if the check is disabled, no matter the reason.
	 */
	@Redirect(
	  method = "handleMovePlayer",
	  at = @At(
	    value = "INVOKE",
	    target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z",
	    ordinal = 0
	  )
	)
	public boolean _lazulib_shouldDisableElytraCheck(
	  GameRules rules, Key<BooleanValue> key, ServerboundMovePlayerPacket packet
	) {
		// If the key is not DISABLE_ELYTRA_MOVEMENT_CHECK, the mixin missed its target
		if (key != GameRules.RULE_DISABLE_ELYTRA_MOVEMENT_CHECK) {
			// Log a warning once
			LogUtil.warnOnce(LOGGER,
				"Mixin ServerGamePacketListenerImpl$shouldDisableElytraCheck missed its target. " +
				"Conditional elytra check disabling may be impossible for mods relying on this mixin.\n" +
				"You may report this bug in the \"LazuLib\" mod issue tracker");
			// Return the value without interference
			return rules.getBoolean(key);
		}
		// If the check is disabled by the rules, leave it like that
		if (rules.getBoolean(key))
			return true;
		// If the player is not flying this mixin can't avoid the check, so simply return
		if (!player.isFallFlying())
			return false;
		// Mimic the check
		double pX = packet.getX(player.getX());
		double pY = packet.getY(player.getY());
		double pZ = packet.getZ(player.getZ());
		double dX = pX - firstGoodX;
		double dY = pY - firstGoodY;
		double dZ = pZ - firstGoodZ;
		double playerMotion2 = player.getDeltaMovement().lengthSqr();
		double playerDelta2 = dX * dX + dY * dY + dZ * dZ;
		double excess = playerDelta2 - playerMotion2;
		int stackedPackets = receivedMovePacketCount - knownMovePacketCount;
		// Post the event
		DisableElytraCheckEvent event = new DisableElytraCheckEvent(
		  player, packet, excess, stackedPackets);
		MinecraftForge.EVENT_BUS.post(event);
		return event.getDisable();
	}
}
