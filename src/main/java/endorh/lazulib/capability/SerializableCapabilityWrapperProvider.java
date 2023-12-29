package endorh.lazulib.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Basic serializable capability provider for a single capability extending
 * {@link ISerializableCapability}<br>
 * Capability is de/serialized using its
 * {@link ISerializableCapability#deserializeCapability}/{@link
 * ISerializableCapability#serializeCapability}
 * methods.
 *
 * @param <C> Capability type
 */
public class SerializableCapabilityWrapperProvider<C extends ISerializableCapability> implements ICapabilitySerializable<CompoundTag> {
	private final Capability<C> capability;
	private final Direction side;
	private final C instance;
	
	public SerializableCapabilityWrapperProvider(final Capability<C> capability, final C instance) {
		this(capability, null, instance);
	}
	
	public SerializableCapabilityWrapperProvider(
	  final Capability<C> capability, @Nullable final Direction side, final C instance
	) {
		this.capability = capability;
		this.side = side;
		this.instance = instance;
	}
	
	@NotNull @Override
	public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		return cap == capability && side == this.side? LazyOptional.of(() -> instance).cast()
		                                             : LazyOptional.empty();
	}
	
	@Override public CompoundTag serializeNBT() {
		return instance.serializeCapability();
	}
	
	@Override public void deserializeNBT(CompoundTag nbt) {
		instance.deserializeCapability(nbt);
	}
}