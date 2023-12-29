package endorh.lazulib.mixins;

import endorh.lazulib.events.RegisterTextureAtlasEvent;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModLoader;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Injects {@link RegisterTextureAtlasEvent} after the static initialization
 * of {@link ModelManager} to inject custom texture atlas entries.
 */
@Mixin(ModelManager.class)
public class ModelManagerMixin {
   /**
    * Mutable accessor to the {@code static final} {@link ModelManager#VANILLA_ATLASES} map.
    */
   @Shadow(aliases="VANILLA_ATLASES")
   @Mutable @Final private static Map<ResourceLocation, ResourceLocation> VANILLA_ATLASES;

   /**
    * Fires {@link RegisterTextureAtlasEvent} to collect texture atlas entries to add to
    * {@link #VANILLA_ATLASES}.<br>
    * <br>
    * Atlas entries are added in the order they are registered, so event handlers registered
    * with lower priority may overwrite texture atlases added by others with higher priority.
    * @param ci mixin callback info
    */
   @Inject(method="<clinit>", at=@At("TAIL"))
   private static void _lazulib_injectTextureAtlases(CallbackInfo ci) {
      VANILLA_ATLASES = new HashMap<>(VANILLA_ATLASES);
      RegisterTextureAtlasEvent.Impl event = new RegisterTextureAtlasEvent.Impl();
      ModLoader.get().postEvent(event);
      for (Pair<ResourceLocation, ResourceLocation> pair : event.registeredAtlases)
         VANILLA_ATLASES.put(pair.getLeft(), pair.getRight());
   }
}
