package endorh.util.common;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import org.jetbrains.annotations.NotNull;

public class ForgeUtil {
	public static final String TAG_FORGE_CAPS = "ForgeCaps";
	
	/**
	 * Convenience method for Object Holder initializers, to
	 * prevent the IDE from complaining about nullity and
	 * ConstantConditions everywhere
	 */
	@SuppressWarnings("ConstantConditions")
	public static<T> @NotNull T futureNotNull() {
		return null;
	}
	
	/**
	 * Extract the capabilities NBT from an ItemStack.<br>
	 * Useful to create items with cloned capabilities.
	 */
	public static CompoundNBT getSerializedCaps(ItemStack stack) {
		CompoundNBT stackNBT = stack.serializeNBT();
		return stackNBT.getCompound(TAG_FORGE_CAPS);
	}
}
