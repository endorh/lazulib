package endorh.lazulib.capability;

import net.minecraft.nbt.CompoundTag;

public interface ISerializableCapability {
	CompoundTag serializeCapability();
	
	void deserializeCapability(CompoundTag nbt);
}
