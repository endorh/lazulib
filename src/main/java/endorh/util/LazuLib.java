package endorh.util;

import endorh.util.command.LazuLibArgumentTypes;
import endorh.util.recipe.NBTInheritingShapedRecipe;
import endorh.util.recipe.NBTInheritingShapedRecipe.Serializer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

@Mod(LazuLib.MOD_ID)
@EventBusSubscriber(bus = Bus.MOD, modid = LazuLib.MOD_ID)
@Internal public class LazuLib {
	public static final String MOD_ID = "lazulib";
	
	public LazuLib() {
		LazuLibArgumentTypes.registerArgumentTypes();
	}
	
	@SubscribeEvent
	protected static void registerRecipes(RegisterEvent event) {
		event.register(ForgeRegistries.RECIPE_SERIALIZERS.getRegistryKey(), helper -> {
			helper.register(Serializer.NAME, NBTInheritingShapedRecipe.SERIALIZER);
		});
	}
}
