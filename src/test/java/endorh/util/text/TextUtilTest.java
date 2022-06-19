package endorh.util.text;

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

import static endorh.util.text.TextUtil.stc;
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
		final MutableComponent stc = stc("red").withStyle(ChatFormatting.RED)
		  .append(stc("green").withStyle(ChatFormatting.GREEN))
		  .append(stc("blue").withStyle(ChatFormatting.BLUE));
		final List<Triple<Integer, Integer, MutableComponent>> testSet =
		  Lists.newArrayList(
			 Triple.of(0, 2, stc("re").withStyle(ChatFormatting.RED)),
			 Triple.of(0, 3, stc("red").withStyle(ChatFormatting.RED)),
			 Triple.of(0, 4, stc("red").withStyle(ChatFormatting.RED)
				.append(stc("g").withStyle(ChatFormatting.GREEN))),
			 Triple.of(1, 5, stc("ed").withStyle(ChatFormatting.RED)
				.append(stc("gr").withStyle(ChatFormatting.GREEN))),
			 Triple.of(1, 10, stc("ed").withStyle(ChatFormatting.RED)
				.append(stc("green").withStyle(ChatFormatting.GREEN))
				.append(stc("bl").withStyle(ChatFormatting.BLUE))),
			 Triple.of(6, 10, stc("en").withStyle(ChatFormatting.GREEN)
				.append(stc("bl").withStyle(ChatFormatting.BLUE))),
			 Triple.of(6, 12, stc("en").withStyle(ChatFormatting.GREEN)
				.append(stc("blue").withStyle(ChatFormatting.BLUE))),
			 Triple.of(6, 8, stc("en").withStyle(ChatFormatting.GREEN)),
			 Triple.of(3, 5, stc("gr").withStyle(ChatFormatting.GREEN)),
			 Triple.of(0, 12, stc),
			 Triple.of(8, 12, stc("blue").withStyle(ChatFormatting.BLUE)),
			 Triple.of(3, 8, stc("green").withStyle(ChatFormatting.GREEN))
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
	
	public String repr(Component t) {
		@Language("RegExp") String repl = "\\w+=null|font=[^{}\\s]+";
		return nonEmptyComponents(t).stream().map(tt ->
		  String.format("{\"%s\": %s}", tt.getString(),
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