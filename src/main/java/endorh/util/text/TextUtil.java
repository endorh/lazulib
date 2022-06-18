package endorh.util.text;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		return I18n.exists(key) ? Optional.of(new TranslationTextComponent(key, args)) : Optional.empty();
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
		if (I18n.exists(key)) {
			// We add the explicit indexes, so relative indexes preserve meaning after splitting
			final String f = addExplicitFormatIndexes(LanguageMap.getInstance().getOrDefault(key));
			final String[] lines = NEW_LINE.split(f);
			final FormattableTextComponentList components = new FormattableTextComponentList();
			for (String line : lines) {
				final Matcher m = FS_PATTERN.matcher(line);
				final IFormattableTextComponent built = new StringTextComponent("");
				int cursor = 0;
				while (m.find()) { // Replace format arguments manually to append ITextComponents
					if (m.group("conversion").equals("%")) {
						built.append("%");
						continue;
					}
					final int s = m.start();
					if (s > cursor)
						built.append(line.substring(cursor, s));
					// Since we've called addExplicitFormatIndexes,
					//   the following line must not throw NumberFormatException
					final int i = Integer.parseInt(m.group("index")) - 1;
					if (i < args.length) {
						// Format options are ignored when the argument is an ITextComponent
						if (args[i] instanceof ITextComponent)
							built.append((ITextComponent) args[i]);
						else built.append(String.format(m.group(), args));
					} // else ignore error
					cursor = m.end();
				}
				if (line.length() > cursor)
					built.append(line.substring(cursor));
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
	 * @param text Component to slice.
	 * @param start Start index of the substring.
	 *              Negative values are corrected counting from the end.
	 */
	public static IFormattableTextComponent subText(ITextComponent text, int start) {
		return subText(text, start, text.getString().length());
	}
	
	/**
	 * Extract a formatted substring from an {@link ITextComponent}.<br>
	 * Should be called on the client side only, if the component may contain translations.
	 * @param text Component to slice
	 * @param start Start index of the substring.
	 *              Negative values are corrected counting from the end.
	 * @param end End index of the substring.
	 *            Negative values are corrected counting from the end.
	 *            Defaults to the end of the component.
	 */
	public static IFormattableTextComponent subText(ITextComponent text, int start, int end) {
		final int n = text.getString().length();
		if (start > n) throw iob(start, n);
		if (start < 0) {
			if (n + start < 0) throw iob(start, n);
			start = n + start;
		}
		if (end > n) throw iob(end, n);
		if (end < 0) {
			if (n + end < 0) throw iob(end, n);
			end = n + end;
		}
		if (end <= start) return new StringTextComponent("");
		boolean started = false;
		final List<ITextComponent> siblings = text.getSiblings();
		IFormattableTextComponent res = new StringTextComponent("");
		String str = text.getContents();
		if (start < str.length()) {
			started = true;
			res = res.append(new StringTextComponent(
			  str.substring(start, Math.min(str.length(), end))).setStyle(text.getStyle()));
			if (end < str.length()) return res;
		}
		int o = str.length();
		for (ITextComponent sibling : siblings) {
			str = sibling.getContents();
			if (started || start - o < str.length()) {
				res = res.append(new StringTextComponent(
				  str.substring(started? 0 : start - o, Math.min(str.length(), end - o))
				).setStyle(sibling.getStyle()));
				started = true;
				if (end - o < str.length()) return res;
			}
			o += str.length();
		}
		return res;
	}
	
	private static StringIndexOutOfBoundsException iob(int index, int length) {
		return new StringIndexOutOfBoundsException("Index: " + index + ", Length: " + length);
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
		return text.plainCopy().withStyle(
		  style -> style.withColor(format)
		    .withHoverEvent(new HoverEvent(
		      HoverEvent.Action.SHOW_TEXT, ttc("chat.link.open")))
		    .withClickEvent(new ClickEvent(
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
		return text.plainCopy().withStyle(
		  style -> style.withColor(format)
			 .withHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_TEXT, ttc("chat.copy.click")))
			 .withClickEvent(new ClickEvent(
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
		return text.plainCopy().withStyle(
		  style -> style.withColor(format)
			 .withHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_TEXT, stc(path)))
			 .withClickEvent(new ClickEvent(
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
		return text.plainCopy().withStyle(
		  style -> style.withColor(format)
			 .withHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_TEXT, ttc("chat.copy")))
			 .withClickEvent(new ClickEvent(
				ClickEvent.Action.SUGGEST_COMMAND, command)));
	}
}
