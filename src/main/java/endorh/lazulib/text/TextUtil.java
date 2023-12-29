package endorh.lazulib.text;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.FormattedText.StyledContentConsumer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.locale.Language;

import static java.lang.Math.max;
import static java.lang.Math.min;

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
	 * Extract a formatted substring from an {@link Component}.<br>
	 * Must be called on the client side only, if the component may contain translations.
	 * @param component {@link Component} to extract from
	 * @param start     Inclusive index to start from
	 * @throws StringIndexOutOfBoundsException if start is out of bounds
	 * @return Formatted component corresponding to the text that would be
	 * returned by a call to substring on its contents.
	 */
	@OnlyIn(Dist.CLIENT)
	public static MutableComponent subText(Component component, int start) {
		int length = component.getString().length();
		checkBounds(start, length);
		SubTextVisitor visitor = new SubTextVisitor(start, Integer.MAX_VALUE);
		component.visit(visitor, Style.EMPTY);
		return visitor.getResult();
	}
	
	/**
	 * Extract a formatted substring from an {@link Component}.<br>
	 * Should be called on the client side only, if the component may contain translations.
	 * @param component {@link Component} to extract from
	 * @param start Inclusive index to start from
	 * @param end Exclusive index to end at
	 * @throws StringIndexOutOfBoundsException if start or end are out of bounds
	 * @return Formatted component corresponding to the text that would be
	 *         returned by a call to substring on its contents.
	 */
	@OnlyIn(Dist.CLIENT)
	public static MutableComponent subText(Component component, int start, int end) {
		int length = component.getString().length();
		checkBounds(start, length);
		checkBounds(end, length);
		SubTextVisitor visitor = new SubTextVisitor(start, end);
		component.visit(visitor, Style.EMPTY);
		return visitor.getResult();
	}
	
	private static void checkBounds(int index, int length) {
		if (index < 0 || index > length) throw new StringIndexOutOfBoundsException(index);
	}
	
	@OnlyIn(Dist.CLIENT)
	private static class SubTextVisitor implements StyledContentConsumer<Boolean> {
		private final int start;
		private final int end;
		private MutableComponent result = null;
		private int length = 0;
		
		private SubTextVisitor(int start, int end) {
			this.start = start;
			this.end = end;
		}
		
		private void appendFragment(String fragment, Style style) {
			if (result == null) {
				result = Component.literal(fragment).setStyle(style);
			} else result.append(Component.literal(fragment).setStyle(style));
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
		
		public MutableComponent getResult() {
			return result != null? result : Component.empty();
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
	public static MutableComponent applyStyle(
	  Component component, Style style, int start
	) {
		int length = component.getString().length();
		checkBounds(start, length);
		ApplyStyleVisitor visitor = new ApplyStyleVisitor(style, start, Integer.MAX_VALUE);
		component.visit(visitor, Style.EMPTY);
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
	public static MutableComponent applyStyle(
	  Component component, Style style, int start, int end
	) {
		int length = component.getString().length();
		checkBounds(start, length);
		checkBounds(end, length);
		if (start == end) return component.copy();
		ApplyStyleVisitor visitor = new ApplyStyleVisitor(style, start, end);
		component.visit(visitor, Style.EMPTY);
		return visitor.getResult();
	}
	
	@OnlyIn(Dist.CLIENT)
	private static final class ApplyStyleVisitor implements StyledContentConsumer<Boolean> {
		private final Style style;
		private final int start;
		private final int end;
		private MutableComponent result = null;
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
					appendFragment(text.substring(relStart, relEnd), this.style.applyTo(style));
				if (relEnd < l)
					appendFragment(text.substring(relEnd), style);
			}
			length += l;
			return Optional.empty();
		}
		
		public MutableComponent getResult() {
			return result != null? result : Component.empty();
		}
		
		private void appendFragment(String fragment, Style style) {
			if (result == null) {
				result = Component.literal(fragment).setStyle(style);
			} else result.append(Component.literal(fragment).setStyle(style));
		}
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
		return text.copy().withStyle(
		  style -> style.applyFormat(format)
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
		return text.copy().withStyle(
		  style -> style.applyFormat(format)
			 .withHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_TEXT, ttc("chat.copy")))
			 .withClickEvent(new ClickEvent(
				ClickEvent.Action.SUGGEST_COMMAND, command)));
	}
}
