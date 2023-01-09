package endorh.util.text;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.*;
import net.minecraft.util.text.ITextProperties.IStyledTextAcceptor;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Writing {@code new TranslationTextComponent()} or
 * {@code new StringTextComponent()} everywhere is not readable when nested
 * a couple of times
 */
public class TextUtil {
	protected static final Pattern NEW_LINE = Pattern.compile("\\R");
	
	/**
	 * Wrap with {@link StringTextComponent}
	 */
	public static StringTextComponent stc(Object x) {
		return stc(String.valueOf(x));
	}
	/**
	 * Wrap with {@link StringTextComponent}
	 */
	public static StringTextComponent stc(String str, Object... args) {
		return stc(String.format(str, args));
	}
	/**
	 * Wrap with {@link StringTextComponent}
	 */
	public static StringTextComponent stc(String str) {
		return new StringTextComponent(str);
	}
	/**
	 * Shorthand for {@link TranslationTextComponent}
	 */
	public static TranslationTextComponent ttc(String key, Object... args) {
		return new TranslationTextComponent(key, args);
	}
	
	/**
	 * Optional translation<br>
	 * @return The {@link TranslationTextComponent} or empty if the key is not translated
	 */
	public static Optional<TranslationTextComponent> optTtc(String key, Object... args) {
		return I18n.hasKey(key) ? Optional.of(new TranslationTextComponent(key, args)) : Optional.empty();
	}
	
	/**
	 * Separate a string text component on each line break<br>
	 */
	public static FormattableTextComponentList splitStc(String str, Object... args) {
		return splitStc(String.format(str, args));
	}
	
	/**
	 * Separate a string text component on each line break<br>
	 */
	public static FormattableTextComponentList splitStc(String str) {
		return new FormattableTextComponentList(
		  Arrays.stream(NEW_LINE.split(str)).map(StringTextComponent::new)
		    .toArray(IFormattableTextComponent[]::new));
	}
	
	/**
	 * Separate a translation text component on each line break<br>
	 * Line breaks added by format arguments aren't considered<br>
	 * <b>This must only be called on the client side</b>, since
	 * translations are not present on the server side<br>
	 * If the translation does not exist, returns a list with the key untranslated.
	 * To obtain an empty list instead, use {@link TextUtil#optSplitTtc}
	 */
	public static FormattableTextComponentList splitTtc(String key, Object... args) {
		return splitTtcImpl(key, false, args);
	}
	
	/**
	 * Separate a translation text component on each line break<br>
	 * Line breaks added by format arguments aren't considered<br>
	 * <b>This must only be called on the client side</b>, since
	 * translations are not present on the server side<br>
	 * If the translation does not exist, returns an empty list.
	 * For the default behaviour use {@link TextUtil#splitTtc}
	 */
	public static FormattableTextComponentList optSplitTtc(String key, Object... args) {
		return splitTtcImpl(key, true, args);
	}
	
	protected static final Pattern FS_PATTERN = Pattern.compile(
	  "%(?:(?<index>\\d+)\\$)?(?<flags>[-#+ 0,(<]*)?(?<width>\\d+)?(?<precision>\\.\\d+)?(?<t>[tT])?(?<conversion>[a-zA-Z%])");
	@OnlyIn(Dist.CLIENT)
	protected static FormattableTextComponentList splitTtcImpl(String key, boolean optional, Object... args) {
		if (I18n.hasKey(key)) {
			// We add the explicit indexes, so relative indexes preserve meaning after splitting
			final String f = addExplicitFormatIndexes(LanguageMap.getInstance().func_230503_a_(key));
			final String[] lines = NEW_LINE.split(f);
			final FormattableTextComponentList components = new FormattableTextComponentList();
			for (String line : lines) {
				final Matcher m = FS_PATTERN.matcher(line);
				final IFormattableTextComponent built = new StringTextComponent("");
				int cursor = 0;
				while (m.find()) { // Replace format arguments manually to append ITextComponents
					if (m.group("conversion").equals("%")) {
						built.appendString("%");
						continue;
					}
					final int s = m.start();
					if (s > cursor)
						built.appendString(line.substring(cursor, s));
					// Since we've called addExplicitFormatIndexes,
					//   the following line must not throw NumberFormatException
					final int i = Integer.parseInt(m.group("index")) - 1;
					if (i < args.length) {
						// Format options are ignored when the argument is an ITextComponent
						if (args[i] instanceof ITextComponent)
							built.append((ITextComponent) args[i]);
						else built.appendString(String.format(m.group(), args));
					} // else ignore error
					cursor = m.end();
				}
				if (line.length() > cursor)
					built.appendString(line.substring(cursor));
				components.add(built);
			}
			return components;
		} else {
			FormattableTextComponentList components = new FormattableTextComponentList();
			if (!optional)
				components.add(new StringTextComponent(key));
			return components;
		}
	}
	
	// For some reason using a lookahead for the last character class
	//   makes the pattern fail if at the start of the sample
	//   I think it may be a bug in the JDK
	protected static final Pattern FS_INDEX_PATTERN = Pattern.compile(
	  "(?<pre>(?<!%)(?:%%)*+%)(?:(?<d>\\d+)\\$)?(?<flags>[-#+ 0,(<]*)(?<pos>[a-zA-Z])");
	
	/**
	 * Add explicit format indexes to a format string.<br>
	 * This keeps the arguments order even when splitting the format String.
	 */
	public static String addExplicitFormatIndexes(String fmt) {
		final Matcher m = FS_INDEX_PATTERN.matcher(fmt);
		final StringBuffer sb = new StringBuffer();
		int last_gen = -1; // Keep track of the last generated index for implicit indexes
		int last = -1; // Keep track of the last index for relative indexes
		while (m.find()) { // Loop through all arguments
			final String g = m.group("d");
			final String f = m.group("flags");
			String rep = g + f;
			if (f.contains("<")) { // Relative index
				if (last >= 0)
					rep = (last + 1) + "\\$" + f.replace("<", "");
			} else if (g == null || g.isEmpty()) { // Implicit index
				last_gen++;
				last = last_gen;
				rep = (last_gen + 1) + "\\$" + f;
			} else { // Explicit index
				last = Integer.parseInt(g) - 1;
				rep += "\\$";
			}
			m.appendReplacement(sb, "${pre}" + rep + "${pos}");
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	// Substrings
	
	/**
	 * Extract a formatted substring from an {@link ITextComponent}.<br>
	 * Must be called on the client side only, if the component may contain translations.
	 * @param component {@link ITextComponent} to extract from
	 * @param start     Inclusive index to start from
	 * @throws StringIndexOutOfBoundsException if start is out of bounds
	 * @return Formatted component corresponding to the text that would be
	 * returned by a call to substring on its contents.
	 */
	@OnlyIn(Dist.CLIENT)
	public static IFormattableTextComponent subText(ITextComponent component, int start) {
		int length = component.getString().length();
		checkBounds(start, length);
		SubTextVisitor visitor = new SubTextVisitor(start, Integer.MAX_VALUE);
		component.getComponentWithStyle(visitor, Style.EMPTY);
		return visitor.getResult();
	}
	
	/**
	 * Extract a formatted substring from an {@link ITextComponent}.<br>
	 * Should be called on the client side only, if the component may contain translations.
	 * @param component {@link ITextComponent} to extract from
	 * @param start Inclusive index to start from
	 * @param end Exclusive index to end at
	 * @throws StringIndexOutOfBoundsException if start or end are out of bounds
	 * @return Formatted component corresponding to the text that would be
	 *         returned by a call to substring on its contents.
	 */
	@OnlyIn(Dist.CLIENT)
	public static IFormattableTextComponent subText(ITextComponent component, int start, int end) {
		int length = component.getString().length();
		checkBounds(start, length);
		checkBounds(end, length);
		SubTextVisitor visitor = new SubTextVisitor(start, end);
		component.getComponentWithStyle(visitor, Style.EMPTY);
		return visitor.getResult();
	}
	
	private static void checkBounds(int index, int length) {
		if (index < 0 || index > length) throw new StringIndexOutOfBoundsException(index);
	}
	
	@OnlyIn(Dist.CLIENT)
	private static class SubTextVisitor implements IStyledTextAcceptor<Boolean> {
		private final int start;
		private final int end;
		private IFormattableTextComponent result = null;
		private int length = 0;
		
		private SubTextVisitor(int start, int end) {
			this.start = start;
			this.end = end;
		}
		
		private void appendFragment(String fragment, Style style) {
			if (result == null) {
				result = new StringTextComponent(fragment).setStyle(style);
			} else result.append(new StringTextComponent(fragment).setStyle(style));
		}
		
		@Override public @NotNull Optional<Boolean> accept(
		  @NotNull Style style, @NotNull String text
		) {
			int l = text.length();
			if (length + l > end) {
				appendFragment(text.substring(max(0, start - length), end - length), style);
				return Optional.of(true);
			} else if (length + l >= start) {
				appendFragment(text.substring(max(0, start - length)), style);
			}
			length += l;
			return Optional.empty();
		}
		
		public IFormattableTextComponent getResult() {
			return result != null? result : StringTextComponent.EMPTY.copyRaw();
		}
	}
	
	/**
	 * Apply a style to a component from a given index.<br>
	 * @param component Component to style
	 * @param style     Style to apply
	 * @param start     Inclusive index to start from
	 * @return Component with the style applied
	 * @throws StringIndexOutOfBoundsException if start is out of bounds
	 */
	@OnlyIn(Dist.CLIENT)
	public static IFormattableTextComponent applyStyle(
	  IFormattableTextComponent component, Style style, int start
	) {
		int length = component.getString().length();
		checkBounds(start, length);
		ApplyStyleVisitor visitor = new ApplyStyleVisitor(style, start, Integer.MAX_VALUE);
		component.getComponentWithStyle(visitor, Style.EMPTY);
		return visitor.getResult();
	}
	
	/**
	 * Apply a style to a specific range of a component.<br>
	 * @param component Component to style
	 * @param style     Style to apply
	 * @param start     Inclusive index of styled range
	 * @param end       Exclusive index of styled range
	 * @return A new component with the style applied to the specified range
	 * @throws StringIndexOutOfBoundsException if start or end are out of bounds
	 */
	@OnlyIn(Dist.CLIENT)
	public static IFormattableTextComponent applyStyle(
	  IFormattableTextComponent component, Style style, int start, int end
	) {
		int length = component.getString().length();
		checkBounds(start, length);
		checkBounds(end, length);
		if (start == end) return component;
		ApplyStyleVisitor visitor = new ApplyStyleVisitor(style, start, end);
		component.getComponentWithStyle(visitor, Style.EMPTY);
		return visitor.getResult();
	}
	
	@OnlyIn(Dist.CLIENT)
	private static final class ApplyStyleVisitor implements IStyledTextAcceptor<Boolean> {
		private final Style style;
		private final int start;
		private final int end;
		private IFormattableTextComponent result = null;
		private int length = 0;
		
		private ApplyStyleVisitor(Style style, int start, int end) {
			this.style = style;
			this.start = start;
			this.end = end;
		}
		
		@Override public @NotNull Optional<Boolean> accept(@NotNull Style style, @NotNull String text) {
			int l = text.length();
			if (l + length <= start || length >= end) {
				appendFragment(text, style);
			} else {
				int relStart = max(0, start - length);
				int relEnd = min(l, end - length);
				if (relStart > 0)
					appendFragment(text.substring(0, relStart), style);
				if (relEnd > relStart)
					appendFragment(text.substring(relStart, relEnd), this.style.mergeStyle(style));
				if (relEnd < l)
					appendFragment(text.substring(relEnd), style);
			}
			length += l;
			return Optional.empty();
		}
		
		public IFormattableTextComponent getResult() {
			return result != null? result : StringTextComponent.EMPTY.copyRaw();
		}
		
		private void appendFragment(String fragment, Style style) {
			if (result == null) {
				result = new StringTextComponent(fragment).setStyle(style);
			} else result.append(new StringTextComponent(fragment).setStyle(style));
		}
	}
	
	// Link builders
	
	public static IFormattableTextComponent makeLink(
	  String text, String url
	) { return makeLink(stc(text), url); }
	
	public static IFormattableTextComponent makeLink(
	  String text, String url, TextFormatting format
	) { return makeLink(stc(text), url, format); }
	
	public static IFormattableTextComponent makeLink(
	  ITextComponent text, String url
	) { return makeLink(text, url, TextFormatting.DARK_AQUA); }
	
	public static IFormattableTextComponent makeLink(
	  ITextComponent text, String url, TextFormatting format
	) {
		return text.deepCopy().modifyStyle(
		  style -> style.setFormatting(format)
		    .setHoverEvent(new HoverEvent(
		      HoverEvent.Action.SHOW_TEXT, ttc("chat.link.open")))
		    .setClickEvent(new ClickEvent(
		      ClickEvent.Action.OPEN_URL, url)));
	}
	
	public static IFormattableTextComponent makeCopyLink(
	  String text, String url
	) { return makeCopyLink(stc(text), url); }
	
	public static IFormattableTextComponent makeCopyLink(
	  String text, String url, TextFormatting format
	) { return makeCopyLink(stc(text), url, format); }
	
	public static IFormattableTextComponent makeCopyLink(
	  ITextComponent text, String url
	) { return makeCopyLink(text, url, TextFormatting.DARK_AQUA); }
	
	public static IFormattableTextComponent makeCopyLink(
	  ITextComponent text, String url, TextFormatting format
	) {
		return text.deepCopy().modifyStyle(
		  style -> style.setFormatting(format)
			 .setHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_TEXT, ttc("chat.copy.click")))
			 .setClickEvent(new ClickEvent(
				ClickEvent.Action.COPY_TO_CLIPBOARD, url)));
	}
	
	public static IFormattableTextComponent makeFileLink(
	  String text, String path
	) { return makeFileLink(stc(text), path); }
	
	public static IFormattableTextComponent makeFileLink(
	  String text, String path, TextFormatting format
	) { return makeFileLink(stc(text), path, format); }
	
	public static IFormattableTextComponent makeFileLink(
	  ITextComponent text, String path
	) { return makeFileLink(text, path, TextFormatting.DARK_AQUA); }
	
	public static IFormattableTextComponent makeFileLink(
	  ITextComponent text, String path, TextFormatting format
	) {
		return text.deepCopy().modifyStyle(
		  style -> style.setFormatting(format)
			 .setHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_TEXT, stc(path)))
			 .setClickEvent(new ClickEvent(
				ClickEvent.Action.OPEN_FILE, path)));
	}
	
	public static IFormattableTextComponent makeCommandLink(
	  String text, String command
	) { return makeCommandLink(stc(text), command); }
	
	public static IFormattableTextComponent makeCommandLink(
	  String text, String command, TextFormatting format
	) { return makeCommandLink(stc(text), command, format); }
	
	public static IFormattableTextComponent makeCommandLink(
	  ITextComponent text, String command
	) { return makeCommandLink(text, command, TextFormatting.DARK_AQUA); }
	
	public static IFormattableTextComponent makeCommandLink(
	  ITextComponent text, String command, TextFormatting format
	) {
		return text.deepCopy().modifyStyle(
		  style -> style.setFormatting(format)
			 .setHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_TEXT, ttc("chat.copy")))
			 .setClickEvent(new ClickEvent(
				ClickEvent.Action.SUGGEST_COMMAND, command)));
	}
}
