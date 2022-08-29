package endorh.util.command;

import com.mojang.brigadier.arguments.ArgumentType;
import endorh.util.EndorUtil;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.function.Supplier;

@EventBusSubscriber(modid = EndorUtil.MOD_ID, bus = Bus.MOD)
@Internal public class EndorUtilArgumentTypes {
	private static final DeferredRegister<ArgumentTypeInfo<?, ?>> TYPE_INFOS =
	  DeferredRegister.create(Registry.COMMAND_ARGUMENT_TYPE_REGISTRY, EndorUtil.MOD_ID);
	
	public static void registerArgumentTypes() {
		reg("qualified_name", QualifiedNameArgumentType.class, QualifiedNameArgumentType.Info::new);
		TYPE_INFOS.register(FMLJavaModLoadingContext.get().getModEventBus());
	}
	
	private static <T extends ArgumentType<?>, TT extends ArgumentTypeInfo.Template<T>> void reg(
	  String name, Class<T> clazz, Supplier<ArgumentTypeInfo<T, TT>> info
	) {
		ArgumentTypeInfo<T, TT> i = info.get();
		ArgumentTypeInfos.registerByClass(clazz, i);
		TYPE_INFOS.register(name, () -> i);
	}
}
