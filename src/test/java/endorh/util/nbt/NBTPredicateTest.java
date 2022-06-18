package endorh.util.nbt;

import endorh.util.nbt.NBTPredicate;
import net.minecraft.nbt.INBT;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static endorh.util.nbt.JsonToNBTUtil.getTagFromJson;
import static endorh.util.nbt.NBTPredicate.parse;
import static org.junit.jupiter.api.Assertions.*;

class NBTPredicateTest {
	protected void println(Object x) {
		System.out.println(x);
	}
	
	@Test void test() {
		NBTPredicate p;
		Optional<INBT> opt;
		INBT valid;
		p = parse("{group: {str: \"test\", fraction: (0~1]}, number: 2, p >= 0, regex: ~\"\\\\w{1,2}\\\\d?\"}").get();
		assertTrue(p.test(getTagFromJson("{number: 2b, p: 2, group: {str: \"test\", fraction: 0.2f}, regex: \"bd2\"}")));
		assertTrue(p.test(getTagFromJson("{number: 2b, p: 4, group: {str: \"test\", fraction: 1f}, regex: \"123\"}")));
		assertFalse(p.test(getTagFromJson("{number: 2b, p: 2, group: {str: \"test\", fraction: 0f}, regex: \"bd2\"}")));
		assertFalse(p.test(getTagFromJson("{number: 1b, p: 2, group: {str: \"test\", fraction: 0.2f}, regex: \"bd2\"}")));
		assertFalse(p.test(getTagFromJson("{number: 2b, p: 2, group: {str: \"tes\", fraction: 0.2f}, regex: \"bd2\"}")));
		assertFalse(p.test(getTagFromJson("{number: 2b, p: 2, group: {str: \"test\", fraction: 1.1f}, regex: \"bd2\"}")));
		assertFalse(p.test(getTagFromJson("{number: 2b, p: 2, group: {str: \"test\", fraction: 1f}, regex: \"a 1\"}")));
		assertFalse(p.test(getTagFromJson("{number: 2b, p: -1, group: {str: \"test\", fraction: 0.2f}, regex: \"bd2\"}")));
		assertTrue(p.test(assertPresentAndLog(p.generateValid())));
	}
	
	@Test void list() {
		NBTPredicate p;
		p = parse("{list: [2, [0~1), 4]}").get();
		assertTrue(p.test(getTagFromJson("{list: [2, 0.2, 4]}")));
		assertFalse(p.test(getTagFromJson("{list: [2, 0.2, 3]}")));
		assertFalse(p.test(getTagFromJson("{list: [2, 0.2, 4, 2]}")));
		assertFalse(p.test(getTagFromJson("{list: [2, 2, 0.2, 4]}")));
		assertFalse(p.test(getTagFromJson("{list: [0.2, 4]}")));
		assertFalse(p.test(getTagFromJson("{list: [4]}")));
		p = parse("{list: ~[2, [0~1), 4]}").get();
		assertTrue(p.test(getTagFromJson("{list: [2]}")));
		assertTrue(p.test(getTagFromJson("{list: [0.2]}")));
		assertTrue(p.test(getTagFromJson("{list: [4]}")));
		assertTrue(p.test(getTagFromJson("{list: [4, 2]}")));
		assertFalse(p.test(getTagFromJson("{list: [3]}")));
		assertTrue(p.test(getTagFromJson("{list: [3, 0.2]}")));
		p = parse("{list: <[2, [3~5), [4~6]]}").get();
		assertTrue(p.test(getTagFromJson("{list: [2, 3, 4]}")));
		assertTrue(p.test(getTagFromJson("{list: [2, 4, 4]}")));
		assertTrue(p.test(getTagFromJson("{list: [2, 3]}")));
		assertTrue(p.test(getTagFromJson("{list: [4, 2]}")));
		assertTrue(p.test(getTagFromJson("{list: [4, 4]}")));
		assertFalse(p.test(getTagFromJson("{list: [2, 2]}")));
		assertFalse(p.test(getTagFromJson("{list: [1]}")));
		assertFalse(p.test(getTagFromJson("{list: [2, 4, 4, 4]}")));
		assertFalse(p.test(getTagFromJson("{list: [\"string\"]}")));
		p = parse("{list: >[2, [3~5), [4~6]]}").get();
		assertTrue(p.test(getTagFromJson("{list: [2, 3, 4]}")));
		assertTrue(p.test(getTagFromJson("{list: [2, 3, 4, 5]}")));
		assertTrue(p.test(getTagFromJson("{list: [2, 3, 4, -1]}")));
		assertTrue(p.test(getTagFromJson("{list: [4, 3, 2]}")));
		assertTrue(p.test(getTagFromJson("{list: [1, 4, 3, 2]}")));
		assertTrue(p.test(getTagFromJson("{list: [6, 3, 2]}")));
		assertFalse(p.test(getTagFromJson("{list: [2, 3]}")));
		assertFalse(p.test(getTagFromJson("{list: [1, 2, 4]}")));
		assertFalse(p.test(getTagFromJson("{list: [2, 6, 6]}")));
		p = parse("{list: <<[2, [3~5), [4~6]]}").get();
		assertTrue(p.test(getTagFromJson("{list: [2, 3, 4]}")));
		assertTrue(p.test(getTagFromJson("{list: [2, 3, 4, -1]}")));
		assertTrue(p.test(getTagFromJson("{list: [2, 3, 4, 0.2, 3]}")));
		assertTrue(p.test(getTagFromJson("{list: [2, 4, 4, 0.2, 3]}")));
		assertFalse(p.test(getTagFromJson("{list: [2, 3, 1, 1]}")));
		assertFalse(p.test(getTagFromJson("{list: [2, 1, 4, 1]}")));
		assertFalse(p.test(getTagFromJson("{list: [-1, 4, 4, 1]}")));
		p = parse("{list: >>[2, [3~5), [4~6]]}").get();
		assertTrue(p.test(getTagFromJson("{list: [2, 3, 4]}")));
		assertTrue(p.test(getTagFromJson("{list: [2, 2, 3, 4]}")));
		assertTrue(p.test(getTagFromJson("{list: [0.2, 0.5, 2, 3, 4]}")));
		assertTrue(p.test(getTagFromJson("{list: [1, 2, 4, 6]}")));
		assertFalse(p.test(getTagFromJson("{list: [-1, 2, 4]}")));
		assertFalse(p.test(getTagFromJson("{list: [-1, 2, 4, 7]}")));
		assertFalse(p.test(getTagFromJson("{list: [-1, 2, 2, 5]}")));
		assertFalse(p.test(getTagFromJson("{list: [-1, 0.2, 1, 4, 5]}")));
		p = parse("{list: ><[2, [3~5), [4~6]]}").get();
		assertTrue(p.test(getTagFromJson("{list: [2, 3, 4]}")));
		assertTrue(p.test(getTagFromJson("{list: [-1, 2, 3, 4, -1]}")));
		assertTrue(p.test(getTagFromJson("{list: [-1, 0.2, 2, 3, 4, -1]}")));
		assertTrue(p.test(getTagFromJson("{list: [-1, 0.2, -3, 2, 4, 4, -1, -3]}")));
		assertTrue(p.test(getTagFromJson("{list: [-1, 0.2, -3, 2, 4, 6, -3]}")));
		assertFalse(p.test(getTagFromJson("{list: [4, 3, 2]}")));
		assertFalse(p.test(getTagFromJson("{list: [2, 3]}")));
		assertFalse(p.test(getTagFromJson("{list: [2, 3, 3, 1]}")));
		assertFalse(p.test(getTagFromJson("{list: [1, -1, 1, -1]}")));
		assertFalse(p.test(getTagFromJson("{list: [1, -1, 2, 4, 1, 6, 0.2]}")));
	}
	
	@Test void generateValid() {
		checkGen(parse("{a: 2, b: [2~4], c: (-3~-1), d: [-4~4]}").get());
		checkGen(parse("{s: \"a\", r~\"\\\\d+\"}").get());
		checkGen(parse("{a: 0, c.m: 0, c.n: 0, c.c.n: 0}").get());
		checkGen(parse("{a: 0, c: {m: 0, n: 0, c: {n: 0}}}").get());
		checkGen(parse("{l: [{n: 0}]}").get());
		checkGen(parse("{l[0].n: 0}").get());
		checkGen(parse("{list: [2, [3~5), [4~6]]}").get());
		checkGen(parse("{list: ~[2, [3~5), [4~6]]}").get());
		checkGen(parse("{list: <[2, [3~5), [4~6]]}").get());
		checkGen(parse("{list: >[2, [3~5), [4~6]]}").get());
		checkGen(parse("{list: <<[2, [3~5), [4~6]]}").get());
		checkGen(parse("{list: >>[2, [3~5), [4~6]]}").get());
		checkGen(parse("{list: ><[2, [3~5), [4~6]]}").get());
	}
	
	protected void checkGen(NBTPredicate p) {
		assertTrue(p.test(assertPresentAndLog(p.generateValid())));
	}
	
	@Test void serialization() {
		checkSer(parse("{a: 2, b: [2~4], c: (-3~-1), d: [-4~4]}").get());
		checkSer(parse("{s: \"a\", r~\"\\\\d+\"}").get());
		checkSer(parse("{a: 0, c.m: 0, c.n: 0, c.c.n: 0}").get());
		checkSer(parse("{a: 0, c: {m: 0, n: 0, c: {n: 0}}}").get());
		checkSer(parse("{l: [{n: 0}]}").get());
		checkSer(parse("{l[0].n: 0}").get());
		checkSer(parse("{list: [2, [3~5), [4~6]]}").get());
		checkSer(parse("{list: ~[2, [3~5), [4~6]]}").get());
		checkSer(parse("{list: <[2, [3~5), [4~6]]}").get());
		checkSer(parse("{list: >[2, [3~5), [4~6]]}").get());
		checkSer(parse("{list: <<[2, [3~5), [4~6]]}").get());
		checkSer(parse("{list: >>[2, [3~5), [4~6]]}").get());
		checkSer(parse("{list: ><[2, [3~5), [4~6]]}").get());
	}
	
	protected void checkSer(NBTPredicate p) {
		println(p.toString());
		assertEquals(p.toString(), parse(p.toString()).get().toString());
	}
	
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	protected <T> T assertPresentAndLog(Optional<T> opt) {
		assertTrue(opt.isPresent());
		final T t = opt.get();
		println(t);
		return t;
	}
}