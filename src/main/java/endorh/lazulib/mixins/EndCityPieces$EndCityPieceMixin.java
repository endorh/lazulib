package endorh.lazulib.mixins;

import endorh.lazulib.events.GenerateEndShipItemFrameEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.structures.EndCityPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;
import java.util.function.Function;

import static net.minecraftforge.eventbus.api.Event.Result.DENY;

/**
 * Injects {@link GenerateEndShipItemFrameEvent} on
 * {@link EndCityPieces.EndCityPiece#handleDataMarker}.
 */
@Mixin(EndCityPieces.EndCityPiece.class)
public abstract class EndCityPieces$EndCityPieceMixin extends TemplateStructurePiece {
	/**
	 * Dummy mixin constructor, required by the Java compiler to inherit from superclass.
	 * @param type ignored
	 * @param tag ignored
	 * @param manager ignored
	 * @param settingsSupplier ignored
	 * @throws IllegalAccessException always
	 */
   protected EndCityPieces$EndCityPieceMixin(
      StructurePieceType type, CompoundTag tag, StructureTemplateManager manager,
      Function<ResourceLocation, StructurePlaceSettings> settingsSupplier
   ) throws IllegalAccessException {
		super(type, tag, manager, settingsSupplier);
		throw new IllegalAccessException("Mixin dummy constructor shouldn't be called!");
	}
	
	/**
	 * Inject {@link GenerateEndShipItemFrameEvent} on
	 * {@link EndCityPieces.EndCityPiece#handleDataMarker}.<br>
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
	 * @param marker Data marker
	 * @param pos Block position of the marker
	 * @param levelAccessor Accessor for the server level where the structure is loaded
	 * @param rand Generation {@link Random} object
	 * @param sbb Structure bounding box
	 * @param callbackInfo Mixin {@link CallbackInfo}
	 */
	@Inject(method = "handleDataMarker", at = @At("HEAD"), cancellable = true)
	protected void _lazulib_handleDataMarker(
	  String marker, BlockPos pos, ServerLevelAccessor levelAccessor,
	  RandomSource rand, BoundingBox sbb, CallbackInfo callbackInfo
	) {
		if (marker.startsWith("Elytra")) {
			ItemFrame itemFrame = new ItemFrame(
			  levelAccessor.getLevel(), pos, placeSettings.getRotation().rotate(Direction.SOUTH));
			ItemStack elytraStack = new ItemStack(Items.ELYTRA);
			
			GenerateEndShipItemFrameEvent event = new GenerateEndShipItemFrameEvent(
			  levelAccessor, pos, rand, sbb, itemFrame, elytraStack);
			MinecraftForge.EVENT_BUS.post(event);
			
			if (event.isCanceled()) {
				callbackInfo.cancel();
			} else if (event.getResult() == DENY) {
				callbackInfo.cancel();
				itemFrame.setItem(event.getElytraStack(), false);
				levelAccessor.addFreshEntity(itemFrame);
			}
		}
	}
}
