package endorh.lazulib.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map.Entry;

import static endorh.lazulib.nbt.JsonToNBTUtil.getTagFromJson;
import static org.junit.jupiter.api.Assertions.*;

class NBTPathTest {
	@Test void traverse() {
		test("{a: 0, b: 4, c: 5, d: 2}", 4);
		test("{a: 0, b: 4, c: {c: 5, d: 2}}", 4);
		test("{a: 0, b: {ba: 4, bb: {b: 2, d: 4}, bd: {b: 4, d: 2}}, c: {c: 5, d: {c: {d: 4}}}}", 8);
	}
	
	private void test(String nbt, int length) {
		System.out.println(nbt);
		assertEquals(length, testTraverse(getTagFromJson(nbt)));
	}
	
	protected static int testTraverse(CompoundTag nbt) {
		int i = 0;
		for (Entry<NBTPath, Tag> e : NBTPath.traverse(nbt)) {
			System.out.println("  " + e.getKey().toString() + ": " + e.getValue().toString());
			i++;
		}
		return i;
	}
}