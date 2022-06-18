package endorh.util;

import endorh.util.command.QualifiedNameArgumentType;
import endorh.util.recipe.NBTInheritingShapedRecipe;
import net.minecraft.command.arguments.ArgumentTypes;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod(EndorUtil.MOD_ID)
@EventBusSubscriber(bus = Bus.MOD, modid = EndorUtil.MOD_ID)
public class EndorUtil {
	public static final String MOD_ID = "endor-util";
	public EndorUtil() {
		registerArgumentTypes();
	}
	
	protected static void registerArgumentTypes() {
		ArgumentTypes.register(
		  MOD_ID + ":qualified_name", QualifiedNameArgumentType.class,
		  new QualifiedNameArgumentType.Serializer());
	}
	
	@SubscribeEvent
	protected static void registerRecipes(RegistryEvent.Register<IRecipeSerializer<?>> event) {
		event.getRegistry().registerAll(NBTInheritingShapedRecipe.SERIALIZER);
	}
}
