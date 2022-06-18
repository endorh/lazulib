package endorh.util.nbt;

import endorh.util.nbt.NBTPath;
import net.minecraft.nbt.CompoundNBT;

import java.util.Arrays;
import java.util.Objects;

public class NBTCompareHelper {
	public static boolean compareExcept(CompoundNBT a, CompoundNBT b, String... excludedPaths) {
		if (a == null) return b == null;
		else if (b == null) return false;
		final CompoundNBT aCopy = a.copy();
		final CompoundNBT bCopy = b.copy();
		for (String excludedPath : excludedPaths) {
			NBTPath p = new NBTPath(excludedPath);
			if (p.isRoot())
				return true;
			p.delete(aCopy);
			p.delete(bCopy);
		}
		return aCopy.equals(bCopy);
	}
	
	public static boolean compareOn(CompoundNBT a, CompoundNBT b, String... comparedPaths) {
		if (a == null) return b == null;
		else if (b == null) return false;
		return Arrays.stream(comparedPaths).map(NBTPath::new)
		  .allMatch(p -> Objects.equals(p.apply(a), p.apply(b)));
	}
}
