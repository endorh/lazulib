package endorh.util;

import endorh.util.command.EndorUtilArgumentTypes;
import endorh.util.recipe.NBTInheritingShapedRecipe;
import endorh.util.recipe.NBTInheritingShapedRecipe.Serializer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod(EndorUtil.MOD_ID)
@EventBusSubscriber(bus = Bus.MOD, modid = EndorUtil.MOD_ID)
public class EndorUtil {
	public static final String MOD_ID = "endorutil";
	
	public EndorUtil() {
		EndorUtilArgumentTypes.registerArgumentTypes();
	}
	
	@SubscribeEvent
	protected static void registerRecipes(RegisterEvent event) {
		event.register(ForgeRegistries.RECIPE_SERIALIZERS.getRegistryKey(), helper -> {
			helper.register(Serializer.NAME, NBTInheritingShapedRecipe.SERIALIZER);
		});
	}
}
