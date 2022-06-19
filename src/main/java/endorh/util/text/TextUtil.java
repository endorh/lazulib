package endorh.util.text;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.FormattedText.StyledContentConsumer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import org.jetbrains.annotations.NotNull;

/**
 * Aliases for the old annoying {@code new MutableComponent()} and
 * {@code new MutableComponent()} and other utilities.
 */
public class TextUtil {
	protected static final Pattern NEW_LINE = Pattern.compile("\\R");
	
	/**
	 * Wrap with {@link Component#literal(String)}
	 */
	public static MutableComponent stc(Object x) {
		return stc(String.valueOf(x));
	}
	/**
	 * Wrap with {@link Component#literal(String)}
	 */
	public static MutableComponent stc(String str, Object... args) {
		return stc(String.format(str, args));
	}
	/**
	 * Wrap with {@link Component#literal(String)}
	 */
	public static MutableComponent stc(String str) {
		return Component.literal(str);
	}
	/**
	 * Shorthand for {@link Component#translatable(String)}
	 */
	public static MutableComponent ttc(String key, Object... args) {
		return Component.translatable(key, args);
	}
	
	/**
	 * Optional translation<br>
	 * @return The {@link Component#translatable(String)} or empty if the key is not translated
	 */
	public static Optional<MutableComponent> optTtc(String key, Object... args) {
		return I18n.exists(key) ? Optional.of(Component.translatable(key, args)) : Optional.empty();
	}
	
	/**
	 * Separate a string text component on each line break<br>
	 */
	public static MutableComponentList splitStc(String str, Object... args) {
		return splitStc(String.format(str, args));
	}
	
	/**
	 * Separate a string text component on each line break<br>
	 */
	public static MutableComponentList splitStc(String str) {
		return new MutableComponentList(
		  Arrays.stream(NEW_LINE.split(str)).map(Component::literal)
		    .toArray(MutableComponent[]::new));
	}
	
	/**
	 * Separate a translation text component on each line break<br>
	 * Line breaks added by format arguments aren't considered<br>
	 * <b>This must only be called on the client side</b>, since
	 * translations are not present on the server side<br>
	 * If the translation does not exist, returns a list with the key untranslated.
	 * To obtain an empty list instead, use {@link TextUtil#optSplitTtc}
	 */
	public static MutableComponentList splitTtc(String key, Object... args) {
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
	public static MutableComponentList optSplitTtc(String key, Object... args) {
		return splitTtcImpl(key, true, args);
	}
	
	protected static final Pattern FS_PATTERN = Pattern.compile(
	  "%(?:(?<index>\\d+)\\$)?(?<flags>[-#+ 0,(<]*)?(?<width>\\d+)?(?<precision>\\.\\d+)?(?<t>[tT])?(?<conversion>[a-zA-Z%])");
	@OnlyIn(Dist.CLIENT)
	protected static MutableComponentList splitTtcImpl(String key, boolean optional, Object... args) {
		if (I18n.exists(key)) {
			// We add the explicit indexes, so relative indexes preserve meaning after splitting
			final String f = addExplicitFormatIndexes(Language.getInstance().getOrDefault(key));
			final String[] lines = NEW_LINE.split(f);
			final MutableComponentList components = new MutableComponentList();
			for (String line : lines) {
				final Matcher m = FS_PATTERN.matcher(line);
				final MutableComponent built = Component.literal("");
				int cursor = 0;
				while (m.find()) { // Replace format arguments manually to append Components
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
						// Format options are ignored when the argument is an Component
						if (args[i] instanceof Component)
							built.append((Component) args[i]);
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
			MutableComponentList components = new MutableComponentList();
			if (!optional)
				components.add(Component.literal(key));
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
		final StringBuilder sb = new StringBuilder();
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
	 * Extract a formatted substring from an {@link Component}.<br>
	 * Must be called on the client side only, if the component may contain translations.
	 * @param text Component to slice.
	 * @param start Start index of the substring.
	 *              Negative values are corrected counting from the end.
	 */
	public static MutableComponent subText(Component text, int start) {
		return subText(text, start, text.getString().length());
	}
	
	/**
	 * Extract a formatted substring from an {@link Component}.<br>
	 * Should be called on the client side only, if the component may contain translations.
	 * @param text Component to slice
	 * @param start Start index of the substring.
	 *              Negative values are corrected counting from the end.
	 * @param end End index of the substring.
	 *            Negative values are corrected counting from the end.
	 *            Defaults to the end of the component.
	 */
	public static MutableComponent subText(Component text, int start, int end) {
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
		if (end <= start) return Component.empty();
		final int st = start, en = end;
		final MutableComponent res = Component.empty();
		AtomicInteger length = new AtomicInteger(0);
		text.visit((style, str) -> {
			int l = str.length(), o = length.getAndAdd(l);
			if (st <= o) {
				res.append(Component.literal(str.substring(0, Math.min(l, en - o))).setStyle(style));
			} else if (st < o + l) {
				res.append(Component.literal(str.substring(st - o, Math.min(l, en - o))).setStyle(style));
			}
			return en <= o + l? Optional.of(res) : Optional.empty();
		}, Style.EMPTY);
		return res;
	}
	
	private static StringIndexOutOfBoundsException iob(int index, int length) {
		return new StringIndexOutOfBoundsException("Index: " + index + ", Length: " + length);
	}
	
	// Link builders
	
	public static MutableComponent makeLink(
	  String text, String url
	) { return makeLink(stc(text), url); }
	
	public static MutableComponent makeLink(
	  String text, String url, ChatFormatting format
	) { return makeLink(stc(text), url, format); }
	
	public static MutableComponent makeLink(
	  Component text, String url
	) { return makeLink(text, url, ChatFormatting.DARK_AQUA); }
	
	public static MutableComponent makeLink(
	  Component text, String url, ChatFormatting format
	) {
		return text.plainCopy().withStyle(
		  style -> style.withColor(format)
		    .withHoverEvent(new HoverEvent(
		      HoverEvent.Action.SHOW_TEXT, ttc("chat.link.open")))
		    .withClickEvent(new ClickEvent(
		      ClickEvent.Action.OPEN_URL, url)));
	}
	
	public static MutableComponent makeCopyLink(
	  String text, String url
	) { return makeCopyLink(stc(text), url); }
	
	public static MutableComponent makeCopyLink(
	  String text, String url, ChatFormatting format
	) { return makeCopyLink(stc(text), url, format); }
	
	public static MutableComponent makeCopyLink(
	  Component text, String url
	) { return makeCopyLink(text, url, ChatFormatting.DARK_AQUA); }
	
	public static MutableComponent makeCopyLink(
	  Component text, String url, ChatFormatting format
	) {
		return text.plainCopy().withStyle(
		  style -> style.withColor(format)
			 .withHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_TEXT, ttc("chat.copy.click")))
			 .withClickEvent(new ClickEvent(
				ClickEvent.Action.COPY_TO_CLIPBOARD, url)));
	}
	
	public static MutableComponent makeFileLink(
	  String text, String path
	) { return makeFileLink(stc(text), path); }
	
	public static MutableComponent makeFileLink(
	  String text, String path, ChatFormatting format
	) { return makeFileLink(stc(text), path, format); }
	
	public static MutableComponent makeFileLink(
	  Component text, String path
	) { return makeFileLink(text, path, ChatFormatting.DARK_AQUA); }
	
	public static MutableComponent makeFileLink(
	  Component text, String path, ChatFormatting format
	) {
		return text.plainCopy().withStyle(
		  style -> style.withColor(format)
			 .withHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_TEXT, stc(path)))
			 .withClickEvent(new ClickEvent(
				ClickEvent.Action.OPEN_FILE, path)));
	}
	
	public static MutableComponent makeCommandLink(
	  String text, String command
	) { return makeCommandLink(stc(text), command); }
	
	public static MutableComponent makeCommandLink(
	  String text, String command, ChatFormatting format
	) { return makeCommandLink(stc(text), command, format); }
	
	public static MutableComponent makeCommandLink(
	  Component text, String command
	) { return makeCommandLink(text, command, ChatFormatting.DARK_AQUA); }
	
	public static MutableComponent makeCommandLink(
	  Component text, String command, ChatFormatting format
	) {
		return text.plainCopy().withStyle(
		  style -> style.withColor(format)
			 .withHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_TEXT, ttc("chat.copy")))
			 .withClickEvent(new ClickEvent(
				ClickEvent.Action.SUGGEST_COMMAND, command)));
	}
}
