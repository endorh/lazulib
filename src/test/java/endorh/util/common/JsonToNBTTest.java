package endorh.util.common;

import com.google.gson.JsonSyntaxException;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static endorh.util.nbt.JsonToNBTUtil.getTagFromJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsonToNBTTest {
	private static void assertThrowsAndPrintStackTrace(
	  @SuppressWarnings("SameParameterValue") Class<? extends Exception> cls, Executable r
	) {
		Throwable t = assertThrows(cls, r);
		System.err.println("Successful test exception:");
		t.printStackTrace();
		System.err.println();
	}
	
	@Test void test() {
		final CompoundTag tag = getTagFromJson(
		  "{id: \"2b\", name: \"test\", t: \"2.0f\", n: {b: \"2d\", d: \"4b\"}," +
		  " a: [\"B\", 2, 4, 3], l: [{name: Steve}, {name: Alex}], num: 24, db: 24.42}");
		
		assertEquals(2F, tag.getFloat("t"));
		assertEquals("test", tag.getString("name"));
		assertEquals((byte)2, tag.getByte("id"));
		assertEquals((byte)2, tag.getByteArray("a")[0]);
		assertEquals((byte)4, tag.getByteArray("a")[1]);
		assertEquals((byte)3, tag.getByteArray("a")[2]);
		assertEquals("Alex", tag.getList("l", 10).getCompound(1).getString("name"));
		assertEquals("INT", tag.get("num").getType().getName());
		assertEquals("DOUBLE", tag.get("db").getType().getName());
		
		assertThrowsAndPrintStackTrace(JsonSyntaxException.class, () ->
		  System.err.println(getTagFromJson("{l: [\"B\", \"NaN\"]}"))
		);
		assertThrowsAndPrintStackTrace(JsonSyntaxException.class, () ->
		  System.err.println(getTagFromJson("{l: [{\"name\": \"test\"}, [\"array\"]]}"))
		);
	}
}
