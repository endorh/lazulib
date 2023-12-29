package endorh.lazulib.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.IModBusEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Fired after the static initialization of the
 * {@link net.minecraft.client.resources.model.ModelManager} class,
 * to allow injecting custom texture atlas entries in the {@code private static final}
 * {@link net.minecraft.client.resources.model.ModelManager#VANILLA_ATLASES} map.<br>
 * <br>
 * This event is fired on the {@link Bus#MOD MOD event bus}, since it occurs early in
 * the construction of the {@link net.minecraft.client.Minecraft} instance.<br>
 * <br>
 * To register a texture atlas sheet, use the {@link #add} method.
 * Textures registered in this event are added to the map in order, so event handlers with
 * lower priority can (perhaps counter-intuitively) override texture atlases of event handlers
 * with higher priority (and also vanilla atlases).
 */
public abstract class RegisterTextureAtlasEvent extends Event implements IModBusEvent {
   /**
    * Register a texture atlas.
    * @param sheet Resource location of the sheet within which the atlas texture is stitched.
    *              The convention for these locations is {@code namespace:textures/atlas/<atlas_name>.png}.
    * @param atlas Relative resource location of the atlas specification JSON, under the {@code banners} root directory.
    *              The convention is to use the atlas name directly (without extension) (with optional namespace).
    *              For example, a location of {@code namespace:<atlas_name>} refers to the spec within a
    *              {@code namespace/banners/<atlas_name>.json}.
    */
   public abstract void add(ResourceLocation sheet, ResourceLocation atlas);

   @ApiStatus.Internal public static class Impl extends RegisterTextureAtlasEvent {
      public List<Pair<ResourceLocation, ResourceLocation>> registeredAtlases = new ArrayList<>();

      @Override
      public void add(ResourceLocation sheet, ResourceLocation atlas) {
         registeredAtlases.add(Pair.of(sheet, atlas));
      }
   }
}
