package endorh.util.capability;

import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Basic serializable capability provider.<br>
 * Ignores the side parameter<br>
 * Capability is de/serialized using its readNBT/writeNBT methods.
 * @param <T> Capability type
 */
public class CapabilityProviderSerializable<T>
  implements ICapabilitySerializable<INBT> {
	private final Capability<T> capability;
	private final Direction side;
	private final T instance;
	
	public CapabilityProviderSerializable(final Capability<T> cap) {
		this(cap, null, cap.getDefaultInstance());
	}
	
	public CapabilityProviderSerializable(final Capability<T> cap, @Nullable final Direction side) {
		this(cap, side, cap.getDefaultInstance());
	}
	
	public CapabilityProviderSerializable(
	  final Capability<T> cap, @Nullable final Direction side, final T inst
	) {
		this.capability = cap;
		this.side = side;
		this.instance = inst;
	}
	
	@NotNull @Override
	public <C> LazyOptional<C> getCapability(@NotNull Capability<C> cap, @Nullable Direction side) {
		return cap == capability ? LazyOptional.of(() -> instance).cast() : LazyOptional.empty();
	}
	
	@Override public INBT serializeNBT() {
		return capability.writeNBT(instance, side);
	}
	
	@Override public void deserializeNBT(INBT nbt) {
		capability.readNBT(instance, side, nbt);
	}
}
