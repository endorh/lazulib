package endorh.lazulib.nbt;

import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.Objects;

public class NBTCompareHelper {
	public static boolean compareExcept(CompoundTag a, CompoundTag b, String... excludedPaths) {
		if (a == null) return b == null;
		else if (b == null) return false;
		final CompoundTag aCopy = a.copy();
		final CompoundTag bCopy = b.copy();
		for (String excludedPath : excludedPaths) {
			NBTPath p = new NBTPath(excludedPath);
			if (p.isRoot())
				return true;
			p.delete(aCopy);
			p.delete(bCopy);
		}
		return aCopy.equals(bCopy);
	}
	
	public static boolean compareOn(CompoundTag a, CompoundTag b, String... comparedPaths) {
		if (a == null) return b == null;
		else if (b == null) return false;
		return Arrays.stream(comparedPaths).map(NBTPath::new)
		  .allMatch(p -> Objects.equals(p.apply(a), p.apply(b)));
	}
}
