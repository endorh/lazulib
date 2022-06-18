package endorh.util.text;

import com.google.common.collect.Lists;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		final IFormattableTextComponent stc = stc("red").mergeStyle(TextFormatting.RED)
		  .append(stc("green").mergeStyle(TextFormatting.GREEN))
		  .append(stc("blue").mergeStyle(TextFormatting.BLUE));
		final List<Triple<Integer, Integer, IFormattableTextComponent>> testSet =
		  Lists.newArrayList(
			 Triple.of(0, 2, stc("re").mergeStyle(TextFormatting.RED)),
			 Triple.of(0, 3, stc("red").mergeStyle(TextFormatting.RED)),
			 Triple.of(0, 4, stc("red").mergeStyle(TextFormatting.RED)
				.append(stc("g").mergeStyle(TextFormatting.GREEN))),
			 Triple.of(1, 5, stc("ed").mergeStyle(TextFormatting.RED)
				.append(stc("gr").mergeStyle(TextFormatting.GREEN))),
			 Triple.of(1, 10, stc("ed").mergeStyle(TextFormatting.RED)
				.append(stc("green").mergeStyle(TextFormatting.GREEN))
				.append(stc("bl").mergeStyle(TextFormatting.BLUE))),
			 Triple.of(6, 10, stc("en").mergeStyle(TextFormatting.GREEN)
				.append(stc("bl").mergeStyle(TextFormatting.BLUE))),
			 Triple.of(6, 12, stc("en").mergeStyle(TextFormatting.GREEN)
				.append(stc("blue").mergeStyle(TextFormatting.BLUE))),
			 Triple.of(6, 8, stc("en").mergeStyle(TextFormatting.GREEN)),
			 Triple.of(3, 5, stc("gr").mergeStyle(TextFormatting.GREEN)),
			 Triple.of(0, 12, stc),
			 Triple.of(8, 12, stc("blue").mergeStyle(TextFormatting.BLUE)),
			 Triple.of(3, 8, stc("green").mergeStyle(TextFormatting.GREEN))
		  );
		for (Triple<Integer, Integer, IFormattableTextComponent> t : testSet) {
			final int start = t.getLeft();
			final int end = t.getMiddle();
			final IFormattableTextComponent exp = t.getRight();
			System.out.printf("Case [%2d~%2d): %s%n", start, end, repr(exp));
			final IFormattableTextComponent res = TextUtil.subText(stc, start, end);
			assertTrue(equivalent(res, exp), () -> "Expected: " + repr(exp) + "\nActual:   " + repr(res));
			assertEquals(end - start, res.getString().length());
		}
	}
	
	public String repr(ITextComponent t) {
		@Language("RegExp") String repl = "\\w+=null|font=[^{}\\s]+";
		return nonEmptyComponents(t).stream().map(tt ->
		  String.format("{\"%s\": %s}", tt.getUnformattedComponentText(),
		                tt.getStyle().toString().replaceAll("(?:, )?(?:" + repl + ")|(?:" + repl + ")(?:, )?", ""))
		).collect(Collectors.joining("~"));
	}
	
	public boolean equivalent(ITextComponent a, ITextComponent b) {
		if (!a.getString().equals(b.getString())) return false;
		final Iterator<ITextComponent> ai = nonEmptyComponents(a).iterator();
		final Iterator<ITextComponent> bi = nonEmptyComponents(b).iterator();
		while (ai.hasNext()) {
			ITextComponent as = ai.next();
			ITextComponent bs = bi.next();
			if (!as.getUnformattedComponentText().equals(bs.getUnformattedComponentText()))
				return false;
			if (!as.getStyle().equals(bs.getStyle()))
				return false;
		}
		return true;
	}
	
	public List<ITextComponent> nonEmptyComponents(ITextComponent t) {
		return Stream.concat(Stream.of(t), t.getSiblings().stream())
		  .filter(tt -> !tt.getUnformattedComponentText().isEmpty())
		  .collect(Collectors.toList());
	}
}