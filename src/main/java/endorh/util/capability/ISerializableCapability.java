package endorh.util.capability;

import net.minecraft.nbt.Tag;

public interface ISerializableCapability<T extends Tag> {
	T serializeCapability();
	
	void deserializeCapability(T nbt);
}
