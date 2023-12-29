package endorh.lazulib.text;

import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static endorh.lazulib.text.TextUtil.stc;
import static net.minecraft.ChatFormatting.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextUtilTest {
	
	@Test void testAddExplicitFormatIndexes() {
		final List<Pair<String, String>> testSet = Lists.newArrayList(
		  Pair.of("I: %d %d %d", "I: %1$d %2$d %3$d"),
		  Pair.of("R: %d %<d %d", "R: %1$d %1$d %2$d"),
		  Pair.of("E: %d %3$d %d", "E: %1$d %3$d %2$d"));
		for (Pair<String, String> pair : testSet) {
			assertEquals(pair.getRight(), TextUtil.addExplicitFormatIndexes(pair.getLeft()));
			assertEquals( // Sanity check
			  String.format(pair.getLeft(), 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
			  String.format(pair.getRight(), 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
		}
	}
	
	@Test void testSubText() {
		final MutableComponent stc =
		  append(s("red", RED), s("green", GREEN), s("blue", BLUE));
		final List<Triple<Integer, Integer, MutableComponent>> testSet =
		  Lists.newArrayList(
			 Triple.of(0, 2, s("re", RED)),
			 Triple.of(0, 3, s("red", RED)),
			 Triple.of(0, 4, append(s("red", RED), s("g", GREEN))),
			 Triple.of(1, 5, append(s("ed", RED), s("gr", GREEN))),
			 Triple.of(1, 10, append(s("ed", RED), s("green", GREEN), s("bl", BLUE))),
			 Triple.of(6, 10, append(s("en", GREEN), s("bl", BLUE))),
			 Triple.of(6, 12, append(s("en", GREEN), s("blue", BLUE))),
			 Triple.of(6, 8, s("en", GREEN)),
			 Triple.of(3, 5, s("gr", GREEN)),
			 Triple.of(0, 12, stc),
			 Triple.of(8, 12, s("blue", BLUE)),
			 Triple.of(3, 8, s("green", GREEN))
		  );
		for (Triple<Integer, Integer, MutableComponent> t : testSet) {
			final int start = t.getLeft();
			final int end = t.getMiddle();
			final MutableComponent exp = t.getRight();
			System.out.printf("Case [%2d~%2d): %s%n", start, end, repr(exp));
			final MutableComponent res = TextUtil.subText(stc, start, end);
 			assertTrue(equivalent(res, exp), () -> "Expected: " + repr(exp) + "\nActual:   " + repr(res));
			assertEquals(end - start, res.getString().length());
		}
	}
	
	@Test void testApplyStyle() {
		final MutableComponent stc =
		  append(s("red", RED), s("green", GREEN), s("blue", BLUE));
		final List<Triple<ChatFormatting, Pair<Integer, Integer>, MutableComponent>> testSet =
		  Lists.newArrayList(
			 Triple.of(BOLD, Pair.of(0, 2), append(s("re", BOLD, RED), s("d", RED), s("green", GREEN), s("blue", BLUE))),
			 Triple.of(ITALIC, Pair.of(0, 3), append(s("red", ITALIC, RED), s("green", GREEN), s("blue", BLUE))),
			 Triple.of(BOLD, Pair.of(0, 4), append(s("red", BOLD, RED), s("g", BOLD, GREEN), s("reen", GREEN), s("blue", BLUE))),
			 Triple.of(BOLD, Pair.of(1, 5), append(s("r", RED), s("ed", BOLD, RED), s("gr", BOLD, GREEN), s("een", GREEN), s("blue", BLUE))),
			 Triple.of(BOLD, Pair.of(1, 10), append(s("r", RED), s("ed", BOLD, RED), s("green", BOLD, GREEN), s("bl", BOLD, BLUE), s("ue", BLUE))),
			 Triple.of(BOLD, Pair.of(6, 10), append(s("red", RED), s("gre", GREEN), s("en", BOLD, GREEN), s("bl", BOLD, BLUE), s("ue", BLUE))),
			 Triple.of(YELLOW, Pair.of(3, 8), append(s("red", RED), s("green", YELLOW), s("blue", BLUE))),
			 Triple.of(UNDERLINE, Pair.of(0, 12), append(s("red", UNDERLINE, RED), s("green", UNDERLINE, GREEN), s("blue", UNDERLINE, BLUE))));
		for (Triple<ChatFormatting, Pair<Integer, Integer>, MutableComponent> t : testSet) {
			final Style style = Style.EMPTY.applyFormat(t.getLeft());
			final Pair<Integer, Integer> range = t.getMiddle();
			final MutableComponent exp = t.getRight();
			System.out.printf("Case [%2d~%2d): %s%n", range.getLeft(), range.getRight(), repr(exp));
			final MutableComponent res = TextUtil.applyStyle(stc, style, range.getLeft(), range.getRight());
			assertTrue(equivalent(res, exp), () -> "Expected: " + repr(exp) + "\nActual:   " + repr(res));
			assertEquals(exp.getString().length(), res.getString().length());
		}
	}
	
	private MutableComponent append(MutableComponent first, Component... components) {
		for (Component c : components) first.append(c);
		return first;
	}
	
	private MutableComponent s(String s, ChatFormatting... formatting) {
		MutableComponent res = stc(s);
		for (ChatFormatting f : formatting) res.withStyle(f);
		return res;
	}
	
	public String repr(Component t) {
		@Language("RegExp") String repl = "\\w+=null|font=[^{}\\s]+";
		return nonEmptyComponents(t).stream().map(tt ->
		  String.format("{\"%s\": %s}", tt.getContents(),
		                tt.getStyle().toString().replaceAll("(?:, )?(?:" + repl + ")|(?:" + repl + ")(?:, )?", ""))
		).collect(Collectors.joining("~"));
	}
	
	public boolean equivalent(Component a, Component b) {
		if (!a.getString().equals(b.getString())) return false;
		final Iterator<Component> ai = nonEmptyComponents(a).iterator();
		final Iterator<Component> bi = nonEmptyComponents(b).iterator();
		while (ai.hasNext()) {
			Component as = ai.next();
			Component bs = bi.next();
			if (!as.getContents().equals(bs.getContents()))
				return false;
			if (!as.getStyle().equals(bs.getStyle()))
				return false;
		}
		return true;
	}
	
	public List<Component> nonEmptyComponents(Component t) {
		return t.toFlatList(Style.EMPTY);
	}
	
}