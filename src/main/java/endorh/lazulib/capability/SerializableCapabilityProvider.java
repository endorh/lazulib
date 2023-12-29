package endorh.lazulib.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Basic serializable capability provider for multiple capabilities extending
 * {@link ISerializableCapability}<br>
 * Capabilities are de/serialized using their {@link ISerializableCapability#deserializeCapability}/
 * {@link ISerializableCapability#serializeCapability}
 * methods.
 */
public class SerializableCapabilityProvider implements ICapabilitySerializable<CompoundTag> {
	public static final String DEFAULT_SIDE_TAG = "self";
	private final Map<Capability<? extends ISerializableCapability>,
	  Map<Direction, NonNullSupplier<? extends ISerializableCapability>>> capabilityMap;
	
	public static Builder create() {
		return new Builder();
	}
	
	private SerializableCapabilityProvider(
	  Map<Capability<? extends ISerializableCapability>, Map<Direction, NonNullSupplier<?
	    extends ISerializableCapability>>> capabilityMap
	) {
		this.capabilityMap = capabilityMap;
	}
	
	@NotNull @Override
	public <C> LazyOptional<C> getCapability(@NotNull Capability<C> cap, @Nullable Direction side) {
		final Map<Direction, NonNullSupplier<? extends ISerializableCapability>> sub =
		  capabilityMap.get(cap);
		if (sub != null) {
			final NonNullSupplier<? extends ISerializableCapability> sup = sub.get(side);
			if (sup != null) return LazyOptional.of(sup).cast();
		}
		return LazyOptional.empty();
	}
	
	@Override public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		capabilityMap.forEach((c, m) -> {
			CompoundTag capTag = new CompoundTag();
			m.forEach((d, s) -> capTag.put(
			  d != null? d.getSerializedName() : DEFAULT_SIDE_TAG, s.get().serializeCapability()));
			tag.put(c.getName(), capTag);
		});
		return tag;
	}
	
	@Override public void deserializeNBT(CompoundTag tag) {
		capabilityMap.forEach((c, m) -> {
			final CompoundTag capTag = tag.getCompound(c.getName());
			m.forEach((d, s) -> s.get().deserializeCapability(
			  capTag.getCompound(d != null? d.getSerializedName() : DEFAULT_SIDE_TAG)));
		});
	}
	
	public static class Builder {
		private Builder() {}
		
		private final Map<Capability<? extends ISerializableCapability>,
		  Map<Direction, NonNullSupplier<? extends ISerializableCapability>>>
		  capabilityMap = new HashMap<>();
		
		/**
		 * Add a capability to this provider.
		 */
		public <T extends ISerializableCapability> Builder with(
		  Capability<T> capability, @NotNull T instance
		) {
			return with(capability, null, (NonNullSupplier<T>) () -> instance);
		}
		
		/**
		 * Add a capability to this provider.
		 */
		public <T extends ISerializableCapability> Builder with(
		  Capability<T> capability, @Nullable Direction side, @NotNull T instance
		) {
			return with(capability, side, (NonNullSupplier<T>) () -> instance);
		}
		
		/**
		 * Add a capability to this provider.
		 */
		public <T extends ISerializableCapability> Builder with(
		  Capability<T> capability, NonNullSupplier<T> supplier
		) {
			with(capability, null, supplier);
			return this;
		}
		
		/**
		 * Add a capability to this provider.
		 */
		public <T extends ISerializableCapability> Builder with(
		  Capability<T> capability, @Nullable Direction side, NonNullSupplier<T> supplier
		) {
			capabilityMap.computeIfAbsent(capability, c -> new HashMap<>()).put(side, supplier);
			return this;
		}
		
		/**
		 * Build the provider.
		 */
		public SerializableCapabilityProvider build() {
			return new SerializableCapabilityProvider(capabilityMap);
		}
	}
}
