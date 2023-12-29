package endorh.lazulib.events;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.Event.HasResult;

/**
 * Generated when an End Ship structure is creating its elytra item frame.<br>
 *
 * The event is cancellable. If cancelled, the default behaviour will
 * be prevented, but no item frame will be added to the world.<br>
 *
 * The event has a result. If the result is other than
 * {@code DENY} ({@code DEFAULT}, {@code ALLOW}) and the event was not
 * cancelled, the default behaviour takes place.<br>
 *
 * If the result is {@code DENY}, and the event was not cancelled, the
 * default behaviour is prevented, and an item frame with the item stack
 * set by the event is added.<br>
 *
 * To simply replace the item that will be displayed in the item frame,
 * just call {@link GenerateEndShipItemFrameEvent#setElytraStack} with
 * the item you want to appear in the item frame and
 * {@link Event#setResult}(DENY) to replace the elytra.<br>
 */
@SuppressWarnings("unused")
@Cancelable @HasResult
public class GenerateEndShipItemFrameEvent extends Event {
	/** Accessor for the server level where the structure is loaded */
	public final ServerLevelAccessor serverLevelAccessor;
	/** Server level where the structure is loaded */
	public final ServerLevel world;
	/** Block position of the item frame */
	public final BlockPos position;
	/** Structure generation random source */
	public final RandomSource random;
	/** Structure bounding box */
	public final BoundingBox sbb;
	/**
	 * Item frame that will be added if the event is not cancelled and
	 * its result is {@code DENY}
	 */
	private final ItemFrame itemFrame;
	/**
	 * Item stack that will be added to the item frame if the
	 * event is not cancelled and its result is {@code DENY}
	 */
	private ItemStack elytraStack;
	
	public GenerateEndShipItemFrameEvent(
	  ServerLevelAccessor levelAccessor, BlockPos pos, RandomSource rand, BoundingBox sbb,
	  ItemFrame itemFrame, ItemStack elytraStack
	) {
		super();
		this.serverLevelAccessor = levelAccessor;
		this.world = levelAccessor.getLevel();
		this.position = pos;
		this.random = rand;
		this.sbb = sbb;
		this.itemFrame = itemFrame;
		this.elytraStack = elytraStack;
	}
	
	/**
	 * @return The item frame that will be added if the event is
	 * not cancelled and results in {@code DENY}
	 */
	public ItemFrame getItemFrame() {
		return itemFrame;
	}
	
	/**
	 * @return The item stack that will be added to the item frame if
	 * the event is not cancelled and results in {@code DENY}
	 */
	public ItemStack getElytraStack() {
		return elytraStack;
	}
	
	/**
	 * Set the item stack that will be added to the item frame if the
	 * event is not cancelled and results in {@code DENY}
	 * @param stack Item stack to display in the item frame.
	 *              Can be different from an elytra.
	 */
	public void setElytraStack(ItemStack stack) {
		this.elytraStack = stack;
	}
}
