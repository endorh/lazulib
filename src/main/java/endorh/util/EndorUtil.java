package endorh.util;

import endorh.util.command.QualifiedNameArgumentType;
import endorh.util.recipe.NBTInheritingShapedRecipe;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod(EndorUtil.MOD_ID)
@EventBusSubscriber(bus = Bus.MOD, modid = EndorUtil.MOD_ID)
public class EndorUtil {
	public static final String MOD_ID = "endorutil";
	public EndorUtil() {
		registerArgumentTypes();
	}
	
	protected static void registerArgumentTypes() {
		ArgumentTypes.register(
		  MOD_ID + ":qualified_name", QualifiedNameArgumentType.class,
		  new QualifiedNameArgumentType.Serializer());
	}
	
	@SubscribeEvent
	protected static void registerRecipes(RegistryEvent.Register<RecipeSerializer<?>> event) {
		event.getRegistry().registerAll(NBTInheritingShapedRecipe.SERIALIZER);
	}
}
