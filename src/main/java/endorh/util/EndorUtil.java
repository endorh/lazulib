package endorh.util;

import endorh.util.command.QualifiedNameArgumentType;
import endorh.util.recipe.NBTInheritingShapedRecipe;
import endorh.util.recipe.NBTInheritingShapedRecipe.Serializer;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod(EndorUtil.MOD_ID)
@EventBusSubscriber(bus = Bus.MOD, modid = EndorUtil.MOD_ID)
public class EndorUtil {
	public static final String MOD_ID = "endorutil";
	private static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES = DeferredRegister.create(Registry.COMMAND_ARGUMENT_TYPE_REGISTRY, MOD_ID);
	public static final RegistryObject<ArgumentTypeInfo<?, ?>> QUALIFIED_NAME_ARGUMENT_TYPE =
	  ARGUMENT_TYPES.register("qualified_name", () -> ArgumentTypeInfos.registerByClass(
		 QualifiedNameArgumentType.class, new QualifiedNameArgumentType.Info()));
	
	public EndorUtil() {
		MinecraftForge.EVENT_BUS.register(ARGUMENT_TYPES);
	}
	
	@SubscribeEvent
	protected static void registerRecipes(RegisterEvent event) {
		event.register(ForgeRegistries.RECIPE_SERIALIZERS.getRegistryKey(), helper -> {
			helper.register(Serializer.NAME, NBTInheritingShapedRecipe.SERIALIZER);
		});
	}
}
