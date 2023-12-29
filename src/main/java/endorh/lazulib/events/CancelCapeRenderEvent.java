package endorh.lazulib.events;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Generates before a player's cape is rendered<br>
 * Cancelling this event will prevent the cape from rendering
 */
@Cancelable
public class CancelCapeRenderEvent extends Event {
	/**
	 * Player being rendered
	 */
	public final AbstractClientPlayer player;
	
	public CancelCapeRenderEvent(AbstractClientPlayer player) {
		this.player = player;
	}
}
