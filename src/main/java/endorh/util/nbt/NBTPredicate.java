package endorh.util.nbt;

import com.florianingerl.util.regex.Matcher;
import com.florianingerl.util.regex.Pattern;
import com.florianingerl.util.regex.PatternSyntaxException;
import com.github.curiousoddman.rgxgen.RgxGen;
import com.github.curiousoddman.rgxgen.config.RgxGenOption;
import com.github.curiousoddman.rgxgen.config.RgxGenProperties;
import com.github.curiousoddman.rgxgen.parsing.dflt.RgxGenParseException;
import com.google.common.collect.ImmutableList;
import endorh.util.network.PacketBufferUtil;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Util;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static endorh.util.text.TextUtil.stc;
import static java.lang.Math.round;

/**
 * NBT predicates describe restrictions on NBT data<br>
 * All NBT predicates have a {@link NBTCompoundPredicate} as their root, which has the form
 * of a compound NBT, with paths and predicates comma-separated within braces:
 * <code>{n: 0, s: ""}</code>.<br>
 * NBT predicates use {@link NBTPath}s to map predicates to values. Sometimes, a predicate may
 * match even when the corresponding NBT value doesn't exist, but its default value matches the
 * predicate (for example, 0 for numeric predicates, or "" for string predicates)<br>
 * <br>
 * Within a compound, any number of predicates may be specified, potentially applying to the same
 * path.<br>
 * All predicates inside a compound predicate must match for the whole to match.
 * Predicates in a compound with their path preceded by an exclamation mark "!" are inverted.<br>
 * Between a path and its predicate a colon '{@code :}' is tolerated, but is not required.
 * It can be clarifying to use it when the NBT predicate doesn't start with an operator symbol.<br>
 * <br>
 * Supported types of predicates inside compounds:
 * <ul>
 *    <li>{@link NBTNumericPredicate}:
 *       Supports the operators {@code =}, {@code >}, {@code <}, {@code <=}, {@code >=}, {@code !=}
 *       or a range notation between parenthesis (exclusive) or square brackets (inclusive),
 *       separated by a tilde '{@code ~}'.<br>
 *       A type may be specified at the end with a single letter among '{@code BSILFD}',
 * 	   case-insensitive.
 * 	   If no type is specified, any numeric NBT type is allowed.<br>
 * 	   <i>Examples</i>:
 * 	   <ul>
 * 	      <li><code>{n: -2}</code> Exact match</li>
 * 	      <li><code>{n: [0~10]}</code> an inclusive range</li>
 * 	      <li><code>{n: [ -1.5 ~ +1.5 )}</code> a range inclusive only on the left.
 * 	          Whitespace is tolerated between components.</li>
 * 	      <li><code>{n: (0 ~ 1)f}</code> non inclusive range specifying the type (float)</li>
 * 	      <li><code>{n &gt;= 2i}</code> Usage of the 'greater than or equals to' (>=) operator,
 * 	      requiring an Int type</li>
 * 	   </ul>
 *    </li>
 *    <li>{@link NBTStringPredicate}:
 *       Can be either an exact string match, or a Java regex pattern.<br>
 * 	   To specify a regex instead of an exact match, use a tilde '~' before the opening quotes.
 * 	   <i>Examples</i>:
 * 	   <ul>
 * 	      <li><code>{str: "Lorem"}</code> An exact string match</li>
 * 	      <li><code>{str ~ "ipsum(\\s+dolor)?"}</code> A regex match. As in Java, the backslash
 * 	      must be escaped. This can be particularly bothersome if the predicate is specified
 * 	      within a JSON or Java literal, since that would require another level of escape
 * 	      (and escaped quotes).</li>
 * 	   </ul>
 * 	   Note that the Java regular expression dialect does not support recursion.
 *    </li>
 *    <li>{@link NBTListPredicate}:
 *       Compare a list NBT to a specified list in a specific comparison mode<br>
 * 	   Consists of a list of other NBT predicates (including compound or list predicates
 * 	   if necessary), comma-separated within square brackets, preceded by an optional comparison
 * 	   mode operator<br>
 * 	   <i>Supported comparison modes are</i>:
 * 	   <ul>
 * 	      <li><b>Exact match</b> ({@code =}) (default): All predicates must match in their
 * 	      position</li>
 * 	      <li><b>Contain any</b> ({@code ~}): At least one predicate must match any item</li>
 * 	      <li><b>Contain all</b> ({@code >}): All predicates must match each a different item</li>
 * 	      <li><b>Subset</b> ({@code <}): All items must match each a different predicate</li>
 * 	      <li><b>Starts with</b> ({@code <<}): All predicates must match in their positions at the
 * 	      start of the list, but the list may be longer than the predicate</li>
 * 	      <li><b>Ends with</b> ({@code >>}): All predicates must match in their positions at
 * 	      the end
 * 	      of the list, but the list may be longer than the predicate</li>
 * 	      <li><b>Contains sequence</b> ({@code ><}): All predicates must match in order somewhere
 * 	      in the list, and the list may be longer</li>
 * 	   </ul>
 * 	   <i>Examples</i>:
 * 	   <ul>
 * 	      <li><code>[0l, [1~3], >=4]</code> List of numeric predicates.
 * 	      The first predicate specifies {@code Long} as its type, so only {@code LongArrayNBT}s or
 * 	      {@code ListNBT}s of {@code LongNBT}s can match. There's currently no way to force one
 * 	      specific of the two</li>
 * 	      <li><code><<[<["a", "b"], <["c", "d"], <["e", "f"]]</code> List of list predicates,
 * 	      showcasing the use of different comparison mode operators</li>
 * 	   </ul>
 * 	   Keep in mind that NBT lists only support a single type shared by all its elements, so
 * 	   List predicates as well only support a single type of predicates within the list.
 *    </li>
 *    <li>Other {@link NBTCompoundPredicate}s:
 *       Sub-compounds can be specified as predicates. This is convenient to group common paths,
 *       or to negate a whole compound.
 *       <i>Examples</i>:
 *       <ul>
 *          <li><code>{Fireworks: {Flight >= 3}}</code>
 *          which is actually equivalent to <code>{Fireworks.Flight >= 3}</code></li>
 *          <li><code>{Very.Large.Path: {Shared >= 3, Paths ~ "can be shortened"}}</code>
 *          Extracting the common path of two predicates</li>
 *          <li><code>{!Negated.Group: {Predicate: [-1~1], !DoubleNegation: [-1~1]}}</code>
 *          Negate a group of predicates, which may also negate again themselves</li>
 *       </ul>
 *    </li>
 * </ul>
 */
public abstract class NBTPredicate implements Predicate<ItemStack> {
	
	/**
	 * Default style used to display predicates
	 */
	public static Style defaultStyle = new Style();
	
	/**
	 * Style class containing formats for each styleable token<br>
	 * Used by {@link NBTPredicate#getDisplay(Style)}
	 */
	public static class Style {
		public TextFormatting operatorStyle = TextFormatting.GOLD;
		public TextFormatting numberStyle = TextFormatting.DARK_AQUA;
		public TextFormatting stringStyle = TextFormatting.DARK_GREEN;
		public TextFormatting quoteStyle = TextFormatting.GOLD;
		public NBTPath.Style pathStyle = new NBTPath.Style();
	}
	
	/**
	 * Parse an {@link NBTPredicate} from its string form.
	 */
	public static Optional<NBTPredicate> parse(String str) {
		return NBTCompoundPredicate.parse(str);
	}
	
	protected static String nest(String exclude) {
		return "(?<nest>(?:[^()\\[\\]{}\"'" + exclude + "]++|[\\[({](?'nest')[\\])}]" +
		       "|\"(?:[^\"\\\\]++|\\\\.)*+\"|'(?:[^'\\\\]++|\\\\.)*+')*+)";
	}
	protected static final String nest = nest("");
	protected static Optional<NBTPredicate> parseNode(String str) {
		return Optional.ofNullable(
		  NBTCompoundPredicate.parse(str)
		    .orElseGet(() -> NBTNumericPredicate.parse(str)
		      .orElseGet(() -> NBTStringPredicate.parse(str)
		        .orElseGet(() -> NBTListPredicate.parse(str)
		          .orElse(null)))));
	}
	
	/**
	 * Actual implementation of the root {@link NBTPredicate} compound type.
	 * @see NBTPredicate
	 */
	public static class NBTCompoundPredicate extends NBTPredicate {
		protected final List<Pair<NBTPath, NBTPredicate>> tagMap;
		protected final List<Pair<NBTPath, NBTPredicate>> excludeTagMap;
		
		protected static final Pattern pattern = Pattern.compile(
		  "^\\{(?<p>" + nest + ")\\}$");
		protected static final Pattern elemPattern = Pattern.compile(
		  "\\s*+(?<pre>!?)" +
		  "(?<path>(?:(?:\\w++|\"(?:[^\"\\\\]++|\\\\.)*+\")\\s*+" +
		  "(?:\\[\\s*+[+-]?\\d++\\s*+\\]\\s*+)*+)" +
		  "(?:\\.(?:(?:\\w++|\"(?:[^\"\\\\]++|\\\\.)*+\")(?:\\[[+-]?\\d++\\])*+))*+)" +
		  "\\s*+:?+\\s*+(?(DEFINE)" + nest + ")" +
		  "(?<value>(?:[^()\\[\\]{}\"',]++|[\\[({](?'nest')[\\])}]|\"(?:\\\\.|[^\"]++)*+\"|'(?:\\\\" +
		  ".|[^']++)*+')++)");
		
		protected NBTCompoundPredicate(
		  List<Pair<NBTPath, NBTPredicate>> tagMap,
		  List<Pair<NBTPath, NBTPredicate>> excludeTagMap
		) {
			this.tagMap = tagMap;
			this.excludeTagMap = excludeTagMap;
		}
		
		public static Optional<NBTPredicate> parse(String str) {
			final Matcher m = pattern.matcher(str.trim());
			if (m.matches()) {
				final String p = m.group("p");
				final Matcher e = elemPattern.matcher(p);
				final List<Pair<NBTPath, NBTPredicate>> tagMap = new ArrayList<>();
				final List<Pair<NBTPath, NBTPredicate>> excludeTagMap = new ArrayList<>();
				while (e.find()) {
					final String pre = e.group("pre");
					try {
						final NBTPath path = new NBTPath(e.group("path"));
						final String v = e.group("value");
						Optional<NBTPredicate> opt = NBTPredicate.parseNode(v);
						if (!opt.isPresent())
							return Optional.empty();
						NBTPredicate value = opt.get();
						if (pre.equals("!"))
							excludeTagMap.add(Pair.of(path, value));
						else tagMap.add(Pair.of(path, value));
					} catch (IllegalArgumentException ex) {
						throw new NBTPredicateParseException(
						  str, "Invalid NBT path: \"" + e.group("path") + "\"", ex);
					}
				}
				return Optional.of(new NBTCompoundPredicate(tagMap, excludeTagMap));
			}
			return Optional.empty();
		}
		
		@Override public boolean test(@Nullable INBT nbt) {
			if (nbt == null)
				nbt = new CompoundNBT();
			if (!(nbt instanceof CompoundNBT))
				return false;
			CompoundNBT com = (CompoundNBT) nbt;
			for (Map.Entry<NBTPath, NBTPredicate> entry : tagMap) {
				if (!entry.getValue().test(entry.getKey().apply(com)))
					return false;
			}
			for (Map.Entry<NBTPath, NBTPredicate> entry : excludeTagMap) {
				if (entry.getValue().test(entry.getKey().apply(com)))
					return false;
			}
			return true;
		}
		
		@Override public Optional<INBT> generateValid() {
			final CompoundNBT nbt = new CompoundNBT();
			for (Map.Entry<NBTPath, NBTPredicate> entry : tagMap) {
				final Optional<INBT> opt = entry.getValue().generateValid();
				if (!opt.isPresent())
					return Optional.empty();
				if (!entry.getKey().makePath(nbt, opt.get()))
					return Optional.empty();
			}
			for (Map.Entry<NBTPath, NBTPredicate> entry : excludeTagMap) {
				if (entry.getValue().test(entry.getKey().apply(nbt)))
					return Optional.empty();
			}
			return Optional.of(nbt);
		}
		
		@Override public boolean isUnique() {
			return false;
		}
		
		@Override public String toString() {
			final String sep = tagMap.isEmpty() || excludeTagMap.isEmpty()? "" : ", ";
			return "{" + tagMap.stream().map(
			  p -> p.getKey().toString() + ": " + p.getValue().toString()
			).collect(Collectors.joining(", ")) + sep + excludeTagMap.stream().map(
			  p -> "!" + p.getKey().toString() + ": " + p.getValue().toString()
			).collect(Collectors.joining(", ")) + "}";
		}
		
		@Override public IFormattableTextComponent getDisplay(Style style) {
			IFormattableTextComponent tc = stc("{").withStyle(style.operatorStyle);
			for (int i = 0, s = tagMap.size() - 1; i < s; i++) {
				Pair<NBTPath, NBTPredicate> e = tagMap.get(i);
				tc = tc.append(e.getKey().getDisplay(style.pathStyle)).append(
				  stc(": ").withStyle(style.operatorStyle)
				).append(e.getValue().getDisplay(style))
				  .append(stc(", ").withStyle(style.operatorStyle));
			}
			if (!tagMap.isEmpty()) {
				Pair<NBTPath, NBTPredicate> last = tagMap.get(tagMap.size() - 1);
				tc = tc.append(last.getKey().getDisplay(style.pathStyle)).append(
				  stc(": ").withStyle(style.operatorStyle)
				).append(last.getValue().getDisplay(style));
				if (!excludeTagMap.isEmpty())
					tc = tc.append(stc(", ").withStyle(style.operatorStyle));
			}
			for (int i = 0, s = excludeTagMap.size() - 1; i < s; i++) {
				Pair<NBTPath, NBTPredicate> e = excludeTagMap.get(i);
				tc = tc.append(stc("!").withStyle(style.operatorStyle)).append(
				  e.getKey().getDisplay(style.pathStyle)
				).append(stc(": ").withStyle(style.operatorStyle))
				  .append(e.getValue().getDisplay(style))
				  .append(stc(", ").withStyle(style.operatorStyle));
			}
			if (!excludeTagMap.isEmpty()) {
				Pair<NBTPath, NBTPredicate> last = excludeTagMap.get(excludeTagMap.size() - 1);
				tc = tc.append(last.getKey().getDisplay(style.pathStyle)).append(
				  stc(": ").withStyle(style.operatorStyle)
				).append(last.getValue().getDisplay(style));
			}
			return tc.append(stc("}").withStyle(style.operatorStyle));
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			NBTCompoundPredicate that = (NBTCompoundPredicate) o;
			return Objects.equals(tagMap, that.tagMap) &&
			       Objects.equals(excludeTagMap, that.excludeTagMap);
		}
		
		@Override public int hashCode() {
			return Objects.hash(tagMap, excludeTagMap);
		}
	}
	
	/**
	 * List predicate asserting that a certain list NBT is similar to a
	 * specified list in a specific comparison mode<br>
	 * Consists of a list of other {@link NBTPredicate}s (including compound or list predicates
	 * if necessary), comma-separated within square brackets, preceded by an optional comparison
	 * mode operator<br>
	 * Supported comparison modes are:
	 * <ul>
	 *    <li><b>Exact match</b> ({@code =}) (default): All predicates must match in their position</li>
	 *    <li><b>Contain any</b> ({@code ~}): At least one predicate must match any item</li>
	 *    <li><b>Contain all</b> ({@code >}): All predicates must match each a different item</li>
	 *    <li><b>Subset</b> ({@code <}): All items must match each a different predicate</li>
	 *    <li><b>Starts with</b> ({@code <<}): All predicates must match in their positions at the
	 *    start of the list, but the list may be longer than the predicate</li>
	 *    <li><b>Ends with</b> ({@code >>}): All predicates must match in their positions at the end
	 *    of the list, but the list may be longer than the predicate</li>
	 *    <li><b>Contains sequence</b> ({@code ><}): All predicates must match in order somewhere
	 *    in the list, and the list may be longer</li>
	 * </ul>
	 * Examples:
	 * <ul>
	 *    <li><code>[0l, [1~3], >=4]</code> List of numeric predicates.
	 *    The first predicate specifies {@code Long} as its type, so only {@code LongArrayNBT}s or
	 *    {@code ListNBT}s of {@code LongNBT}s can match. There's currently no way to force one
	 *    specific of the two</li>
	 *    <li><code><<[<["a", "b"], <["c", "d"], <["e", "f"]]</code> List of list predicates,
	 *    showcasing the use of different comparison mode operators</li>
	 * </ul>
	 * Keep in mind that NBT lists only support a single type shared by all its elements, so
	 * List predicates as well only support a single type of predicates within the list.
	 */
	public static class NBTListPredicate extends NBTPredicate {
		public List<NBTPredicate> list;
		public ComparisonMode mode;
		
		protected NBTListPredicate(List<NBTPredicate> list, ComparisonMode mode) {
			this.list = list;
			this.mode = mode;
		}
		
		protected static final Pattern pattern = Pattern.compile(
		  "^(?<m>[=~<>]?|<<|>>|><)\\[(?<p>" + nest + ")]$");
		protected static final Pattern elemPattern = Pattern.compile(
		  "(?(DEFINE)" + nest + ")(?<p>(?:[^()\\[\\]{}\"',]++|[\\[({](?'nest')[\\])}]" +
		  "|\"(?:\\\\.|[^\"]++)*+\"|'(?:\\\\.|[^']++)*+')++)");
		public static Optional<NBTPredicate> parse(String str) {
			final Matcher m = pattern.matcher(str.trim());
			if (m.matches()) {
				final String p = m.group("p");
				final String mode_s = m.group("m");
				final ComparisonMode mode = ComparisonMode.fromSymbol(mode_s);
				if (mode == null)
					throw new NBTPredicateParseException(
					  str, "Unknown list comparison mode: \"" + mode_s + "\"");
				final Matcher e = elemPattern.matcher(p);
				List<NBTPredicate> list = new ArrayList<>();
				Class<? extends NBTPredicate> cls = null;
				String numeric_type = null;
				while (e.find()) {
					final String el = e.group("p");
					final Optional<NBTPredicate> opt = NBTPredicate.parseNode(el);
					if (!opt.isPresent())
						return Optional.empty();
					final NBTPredicate pr = opt.get();
					if (cls == null)
						cls = pr.getClass();
					else if (cls != pr.getClass())
						throw new NBTPredicateParseException(str, "Different NBT types in array");
					if (pr instanceof NBTNumericPredicate) {
						String t = ((NBTNumericPredicate) pr).type;
						if (numeric_type == null)
							numeric_type = t;
						else if (!numeric_type.equals(t))
							throw new NBTPredicateParseException(str, "Different NBT types in array");
					}
					list.add(pr);
				}
				return Optional.of(new NBTListPredicate(list, mode));
			}
			return Optional.empty();
		}
		
		@Override public boolean test(@Nullable INBT nbt) {
			if (nbt == null)
				nbt = new ListNBT();
			if (nbt instanceof LongArrayNBT) {
				final long[] value = ((LongArrayNBT) nbt).getAsLongArray();
				nbt = new ListNBT();
				for (long v : value)
					((ListNBT) nbt).add(LongNBT.valueOf(v));
			} else if (nbt instanceof IntArrayNBT) {
				final int[] value = ((IntArrayNBT) nbt).getAsIntArray();
				nbt = new ListNBT();
				for (int v : value)
					((ListNBT) nbt).add(IntNBT.valueOf(v));
			} else if (nbt instanceof ByteArrayNBT) {
				final byte[] value = ((ByteArrayNBT) nbt).getAsByteArray();
				nbt = new ListNBT();
				for (byte v : value)
					((ListNBT) nbt).add(ByteNBT.valueOf(v));
			}
			if (!(nbt instanceof ListNBT))
				return false;
			ListNBT value = (ListNBT) nbt.copy();
			switch (mode) {
				case EXACT_MATCH:
					if (value.size() != list.size())
						return false;
					for (int i = 0; i < value.size(); i++) {
						if (!list.get(i).test(value.get(i)))
							return false;
					}
					return true;
				case CONTAIN_ANY:
					if (list.isEmpty())
						return value.isEmpty();
					for (INBT item : value) {
						for (NBTPredicate nbtPredicate : list) {
							if (nbtPredicate.test(item))
								return true;
						}
					}
					return false;
				case CONTAIN_ALL:
					return checkMatch(list, value, NBTPredicate::test, (p, v) -> p.isUnique());
				case SUBSET:
					return checkMatch(value, list, (v, p) -> p.test(v), (v, p) -> p.isUnique());
				case STARTS_WITH:
					if (value.size() < list.size())
						return false;
					for (int i = 0, listSize = list.size(); i < listSize; i++) {
						if (!list.get(i).test(value.get(i)))
							return false;
					}
					return true;
				case ENDS_WITH:
					if (value.size() < list.size())
						return false;
					for (int i = 0, listSize = list.size(), valueSize = value.size(); i < listSize; i++) {
						if (!list.get(i).test(value.get(valueSize - listSize + i)))
							return false;
					}
					return true;
				case CONTAINS_SEQUENCE:
					if (value.size() < list.size())
						return false;
					//noinspection TextLabelInSwitchStatement
					check:for (int i = 0, s = value.size() - list.size(); i <= s; i++) {
						for (int j = 0, listSize = list.size(); j < listSize; j++) {
							if (!list.get(j).test(value.get(i + j)))
								continue check;
						}
						return true;
					}
					return false;
				default: throw new IllegalStateException("Comparison mode cannot be null");
			}
		}
		
		@Override public Optional<INBT> generateValid() {
			List<INBT> res = new ArrayList<>();
			switch (mode) {
				case EXACT_MATCH:
				case CONTAIN_ALL:
				case STARTS_WITH:
				case ENDS_WITH:
				case CONTAINS_SEQUENCE:
					for (NBTPredicate p : list) {
						final Optional<INBT> opt = p.generateValid();
						if (!opt.isPresent())
							return Optional.empty();
						res.add(opt.get());
					}
					break;
				case CONTAIN_ANY:
					if (!list.isEmpty()) {
						for (NBTPredicate p : list) {
							final Optional<INBT> opt = p.generateValid();
							if (opt.isPresent()) {
								res.add(opt.get());
								break;
							}
						}
						if (res.isEmpty())
							return Optional.empty();
					}
				case SUBSET: break;
				default: throw new IllegalStateException("Comparison mode cannot be null");
			}
			final ListNBT nbt = new ListNBT();
			try {
				nbt.addAll(res);
			} catch (UnsupportedOperationException e) {
				return Optional.empty();
			}
			return Optional.of(nbt);
		}
		
		/**
		 * Check match until aList is exhausted.<br>
		 */
		protected <A, B> boolean checkMatch(
		  List<A> aList, List<B> bList, BiPredicate<A, B> matcher, BiPredicate<A, B> unique
		) {
			if (aList.isEmpty())
				return true;
			for (A a : aList) {
				for (B b : bList) {
					if (matcher.test(a, b)) {
						if (aList.size() == 1)
							return true;
						else {
							List<A> al = new ArrayList<>(aList);
							al.remove(a);
							List<B> bl = new ArrayList<>(bList);
							bl.remove(b);
							if (checkMatch(al, bl, matcher, unique))
								return true;
							if (unique.test(a, b))
								return false;
						}
					}
				}
			}
			return false;
		}
		
		public enum ComparisonMode {
			EXACT_MATCH("", "="),
			CONTAIN_ANY("~"),
			CONTAIN_ALL(">"),
			SUBSET("<"),
			STARTS_WITH("<<"),
			ENDS_WITH(">>"),
			CONTAINS_SEQUENCE("><");
			
			private final String alias;
			private final String secondAlias;
			
			ComparisonMode(String alias) {
				this(alias, null);
			}
			ComparisonMode(String alias, String secondAlias) {
				this.alias = alias;
				this.secondAlias = secondAlias;
			}
			
			public static @Nullable ComparisonMode fromSymbol(String symbol) {
				return Arrays.stream(values()).filter(
				  v -> v.alias.equals(symbol) || v.secondAlias != null && v.secondAlias.equals(symbol)
				).findFirst().orElse(null);
			}
		}
		
		@Override public boolean isUnique() {
			return mode == ComparisonMode.EXACT_MATCH && list.stream().allMatch(NBTPredicate::isUnique);
		}
		
		@Override public String toString() {
			return "[" + list.stream().map(NBTPredicate::toString).collect(Collectors.joining(", ")) + "]";
		}
		
		@Override public IFormattableTextComponent getDisplay(Style style) {
			IFormattableTextComponent tc = stc(mode.alias + "[").withStyle(style.operatorStyle);
			for (int i = 0, listSize = list.size() - 1; i < listSize; i++) {
				NBTPredicate pr = list.get(i);
				tc = tc.append(pr.getDisplay(style)).append(stc(", ").withStyle(style.operatorStyle));
			}
			return tc.append(list.get(list.size() - 1).getDisplay(style))
			  .append(stc("]").withStyle(style.operatorStyle));
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			NBTListPredicate that = (NBTListPredicate) o;
			return list.equals(that.list) && mode == that.mode;
		}
		
		@Override public int hashCode() {
			return Objects.hash(list, mode);
		}
	}
	
	/**
	 * Accepts a numeric predicate consisting in either a numeric constant
	 * or a range surrounded in regular or square brackets, separating
	 * the optional bounds with a tilde '~'<br>
	 * A type may be specified at the end with a single letter among 'BSILFD',
	 * case-insensitive<br>
	 * Examples:
	 * <ul>
	 *    <li>{@code -2} a number literal</li>
	 *    <li>{@code [0~10]} an inclusive range</li>
	 *    <li>{@code [ -1.5 ~ +1.5 )} a range inclusive only on the left.
	 *        Whitespace is tolerated between components.</li>
	 *    <li>{@code (0 ~ 1)f} non inclusive range specifying the type (float)</li>
	 * </ul>
	 */
	public static class NBTNumericPredicate extends NBTPredicate {
		public final double min;
		public final double max;
		public final boolean includeMin;
		public final boolean includeMax;
		public final String type;
		protected final Function<Double, NumberNBT> converter;
		@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalAssignedToNull"})
		protected Optional<NumberNBT> valid = null;
		
		protected NBTNumericPredicate(
		  double min, double max, boolean includeMin, boolean includeMax, @Nullable String type
		) {
			if (type != null) {
				type = type.toUpperCase();
				if (!typeMap.containsKey(type))
					throw new IllegalArgumentException("Unknown numeric NBT type: \"" + type + "\"");
			}
			this.min = min;
			this.max = max;
			this.includeMin = includeMin;
			this.includeMax = includeMax;
			this.type = type;
			this.converter = type != null? typeMap.get(type) : null;
		}
		
		protected static final java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
		  "(?:(?<pre>[\\[(])\\s*+(?<min>[+-]?+(?:\\d++\\.)?+\\d++)?\\s*+~" +
		  "\\s*+(?<max>[+-]?+(?:\\d++\\.)?+\\d++)?+\\s*+(?<pos>[])])" +
		  "|(?<com>[><]=?+)?+\\s*+(?<value>[+-]?+(?:\\d++\\.)?+\\d++))(?<type>[BSILFD])?+",
		  java.util.regex.Pattern.CASE_INSENSITIVE);
		protected static final Map<String, Function<Double, NumberNBT>> typeMap =
		  Util.make(new HashMap<>(), m -> {
		  	m.put("B", d -> ByteNBT.valueOf(d.byteValue()));
			m.put("S", d -> ShortNBT.valueOf(d.shortValue()));
			m.put("I", d -> IntNBT.valueOf(d.intValue()));
			m.put("L", d -> LongNBT.valueOf(d.longValue()));
			m.put("F", d -> FloatNBT.valueOf(d.floatValue()));
			m.put("D", DoubleNBT::valueOf);
		});
		
		public static Optional<NBTPredicate> parse(String str) {
			final java.util.regex.Matcher m = pattern.matcher(str.trim());
			if (m.matches()) {
				final String v = m.group("value");
				final String t = m.group("type");
				if (t != null && !typeMap.containsKey(t.toUpperCase()))
					throw new NBTPredicateParseException(str, "Unknown numeric NBT type: \"" + t + "\"");
				if (v != null) {
					double d = Double.parseDouble(v);
					final String com = m.group("com");
					if (com != null) switch (com) {
						case "<": return Optional.of(new NBTNumericPredicate(Double.NEGATIVE_INFINITY, d, true, false, t));
						case ">": return Optional.of(new NBTNumericPredicate(d, Double.POSITIVE_INFINITY, false, true, t));
						case "<=": return Optional.of(new NBTNumericPredicate(Double.NEGATIVE_INFINITY, d, true, true, t));
						case ">=": return Optional.of(new NBTNumericPredicate(d, Double.POSITIVE_INFINITY, true, true, t));
					}
					return Optional.of(new NBTNumericPredicate(d, d, true, true, t));
				}
				final String min_s = m.group("min");
				final String max_s = m.group("max");
				final double min = min_s != null? Double.parseDouble(min_s) : Double.NEGATIVE_INFINITY;
				final double max = max_s != null? Double.parseDouble(max_s) : Double.POSITIVE_INFINITY;
				final boolean includeMin = m.group("pre").equals("[");
				final boolean includeMax = m.group("pos").equals("]");
				return Optional.of(new NBTNumericPredicate(min, max, includeMin, includeMax, t));
			}
			return Optional.empty();
		}
		
		@Override public boolean test(@Nullable INBT nbt) {
			if (nbt == null)
				nbt = DoubleNBT.ZERO;
			if (!(nbt instanceof NumberNBT))
				return false;
			double value = ((NumberNBT) nbt).getAsDouble();
			return (includeMin? min <= value : min < value)
			       && (includeMax? value <= max : value < max);
		}
		
		protected static final List<Double> stepAttempts = ImmutableList.of(
		  0.5, 1D, 2D, 5D, 10D, 0.2D, 0.1D, 0.01D);
		
		@Override public Optional<INBT> generateValid() {
			//noinspection OptionalAssignedToNull
			if (valid == null)
				valid = genValid();
			//noinspection unchecked
			return (Optional<INBT>) (Optional<?>) valid;
		}
		
		protected Optional<NumberNBT> genValid() {
			NumberNBT nbt;
			if (min == max) {
				if (includeMin && includeMax) {
					nbt = wrap(min);
					if (test(nbt))
						return Optional.of(nbt);
				}
				return Optional.empty();
			}
			if (min >= 0) {
				if (includeMin)
					return Optional.of(wrap(min));
				for (double step : stepAttempts) {
					if (min + step < max) {
						nbt = wrap(min + step);
						if (test(nbt))
							return Optional.of(nbt);
					}
				}
				nbt = wrap((min + max) / 2);
				if (test(nbt))
					return Optional.of(nbt);
				if (includeMax) {
					nbt = wrap(max);
					if (test(nbt))
						return Optional.of(nbt);
				}
				return Optional.empty();
			}
			if (max <= 0) {
				if (includeMax)
					return Optional.of(wrap(max));
				for (double step : stepAttempts) {
					if (max - step > min) {
						nbt = wrap(max - step);
						if (test(nbt))
							return Optional.of(nbt);
					}
				}
				nbt = wrap((min + max) / 2);
				if (test(nbt))
					return Optional.of(nbt);
				if (includeMin) {
					nbt = wrap(min);
					if (test(nbt))
						return Optional.of(nbt);
				}
				return Optional.empty();
			}
			else return Optional.of(wrap(0));
		}
		
		protected NumberNBT wrap(double value) {
			if (converter != null) {
				return converter.apply(value);
			} else if (round(value) == value) {
				if (Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE)
					return IntNBT.valueOf((int) value);
				else return LongNBT.valueOf((long) value);
			} else return DoubleNBT.valueOf(value);
		}
		
		@Override public boolean isUnique() {
			return min == max;
		}
		
		@Override public String toString() {
			final String t = type != null ? type : "";
			if (min == max && includeMin && includeMax)
				return min + t;
			if (min == Double.NEGATIVE_INFINITY && max != Double.POSITIVE_INFINITY && includeMin)
				return (includeMax? "<=" : "<") + " " + max + t;
			if (max == Double.POSITIVE_INFINITY && min != Double.NEGATIVE_INFINITY && includeMax)
				return (includeMin? ">=" : ">") + " " + min + t;
			return (includeMin ? "[" : "(") + min + "~" + max + (includeMax ? "]" : ")") + t;
		}
		
		@Override public IFormattableTextComponent getDisplay(Style style) {
			IFormattableTextComponent type = this.type != null? stc(this.type).withStyle(style.operatorStyle) : stc("");
			if (min == max && includeMin && includeMax)
				return stc(min).withStyle(style.numberStyle).append(type);
			if (min == Double.NEGATIVE_INFINITY && max != Double.POSITIVE_INFINITY && includeMin)
				return stc(includeMax? "<= " : "< ").withStyle(style.operatorStyle)
				  .append(stc(max).withStyle(style.numberStyle)).append(type);
			if (max == Double.POSITIVE_INFINITY && min != Double.NEGATIVE_INFINITY && includeMax)
				return stc(includeMin? ">= " : "> ").withStyle(style.operatorStyle)
				  .append(stc(min).withStyle(style.numberStyle)).append(type);
			return stc(includeMin ? "[" : "(").withStyle(style.operatorStyle)
			  .append(stc(min).withStyle(style.numberStyle))
			  .append(stc("~").withStyle(style.operatorStyle))
			  .append(stc(max).withStyle(style.numberStyle))
			  .append(stc(includeMax ? "]" : ")").withStyle(style.operatorStyle))
			  .append(type);
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			NBTNumericPredicate that = (NBTNumericPredicate) o;
			return Double.compare(that.min, min) == 0 &&
			       Double.compare(that.max, max) == 0 && includeMin == that.includeMin &&
			       includeMax == that.includeMax && Objects.equals(type, that.type);
		}
		
		@Override public int hashCode() {
			return Objects.hash(min, max, includeMin, includeMax, type);
		}
	}
	
	/**
	 * String {@link NBTPredicate} consisting in either an exact string match,
	 * or a Java regex pattern.<br>
	 * To specify a regex instead of an exact match, use a tilde '~' before the opening quotes.
	 * Examples:
	 * <ul>
	 *    <li>{@code "Lorem"} An exact string match</li>
	 *    <li>{@code ~"ipsum(\\s+dolor)?"} A regex match. As in Java, the backslash must be
	 *    escaped. This can be particularly bothersome if the predicate is specified within a
	 *    JSON or Java literal, since that would require another level of escape
	 *    (and escaped quotes).</li>
	 * </ul>
	 * Note that the Java regular expression dialect does not support recursion.
	 */
	public static class NBTStringPredicate extends NBTPredicate {
		public final String value;
		public final java.util.regex.Pattern pattern;
		
		public NBTStringPredicate(String value) {
			this.value = value;
			pattern = null;
		}
		
		public NBTStringPredicate(java.util.regex.Pattern pattern) {
			value = null;
			this.pattern = pattern;
		}
		
		private static final java.util.regex.Pattern parsePattern = java.util.regex.Pattern.compile(
		  "(?<pre>~)?\\s*\"(?<value>(?:\\\\.|[^\"\\\\]++)*+)\"");
		public static Optional<NBTPredicate> parse(String str) {
			final java.util.regex.Matcher m = parsePattern.matcher(str.trim());
			if (m.matches()) {
				final String pre = m.group("pre");
				final String value = StringEscapeUtils.unescapeJava(m.group("value"));
				if (pre != null) {
					try {
						return Optional.of(new NBTStringPredicate(java.util.regex.Pattern.compile(value)));
					} catch (PatternSyntaxException e) {
						throw new NBTPredicateParseException(str, "Error parsing regex", e);
					}
				} else return Optional.of(new NBTStringPredicate(value));
			}
			return Optional.empty();
		}
		
		@Override public boolean test(@Nullable INBT nbt) {
			if (nbt == null)
				nbt = StringNBT.valueOf("");
			if (!(nbt instanceof StringNBT))
				return false;
			if (value != null)
				return value.equals(nbt.getAsString());
			else if (pattern != null)
				return pattern.matcher(nbt.getAsString()).matches();
			return true;
		}
		
		protected static final RgxGenProperties rgxGenProperties = new RgxGenProperties();
		static {
			RgxGenOption.INFINITE_PATTERN_REPETITION.setInProperties(rgxGenProperties, 16);
		}
		@Override public Optional<INBT> generateValid() {
			if (value != null)
				return Optional.of(StringNBT.valueOf(value));
			else if (pattern != null) {
				try {
					RgxGen rgxGen = new RgxGen(pattern.pattern());
					rgxGen.setProperties(rgxGenProperties);
					return Optional.of(StringNBT.valueOf(rgxGen.generate()));
				} catch (RgxGenParseException e) {
					return Optional.empty();
				}
			}
			return Optional.empty();
		}
		
		@Override public boolean isUnique() {
			return value != null;
		}
		
		@Override public String toString() {
			if (value != null)
				return "\"" + StringEscapeUtils.escapeJava(value) + "\"";
			else if (pattern != null)
				return "~\"" + StringEscapeUtils.escapeJava(pattern.pattern()) + "\"";
			else return "";
		}
		
		@Override public IFormattableTextComponent getDisplay(Style style) {
			if (value != null)
				return stc("\"").append(stc(StringEscapeUtils.escapeJava(value)).withStyle(style.stringStyle))
				  .append("\"").withStyle(style.quoteStyle);
			else if (pattern != null)
				return stc("~\"").append(stc(StringEscapeUtils.escapeJava(pattern.pattern())).withStyle(style.stringStyle))
				  .append("\"").withStyle(style.quoteStyle);
			else return stc("");
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			NBTStringPredicate that = (NBTStringPredicate) o;
			return Objects.equals(value, that.value) &&
			       Objects.equals(pattern, that.pattern);
		}
		
		@Override public int hashCode() {
			return Objects.hash(value, pattern);
		}
	}
	
	/**
	 * Attempt to generate an NBT value that would match this predicate
	 */
	public Optional<INBT> generateValid() {
		return Optional.empty();
	}
	
	/**
	 * Test the predicate
	 */
	public abstract boolean test(@Nullable INBT nbt);
	
	/**
	 * True if this predicate only allows a single value
	 */
	public abstract boolean isUnique();
	
	/**
	 * Test the predicate with the item NBT of an item stack
	 */
	public boolean test(ItemStack stack) {
		final CompoundNBT tag = stack.getTag();
		return test(tag != null? tag : new CompoundNBT());
	}
	
	/**
	 * Test the predicate with the NBT of an entity
	 */
	public boolean test(Entity entity) {
		return test(entity.getPersistentData());
	}
	
	/**
	 * Thrown when parsing an invalid NBT predicate string
	 */
	public static class NBTPredicateParseException extends RuntimeException {
		public final String parsedString;
		public NBTPredicateParseException(String str, String msg, Exception cause) {
			super(msg + ":\n\t" + str, cause);
			parsedString = str;
		}
		public NBTPredicateParseException(String str, String msg) {
			super(msg + ":\n\t" + str);
			parsedString = str;
		}
	}
	
	/**
	 * Serialize to packet
	 */
	public void write(PacketBuffer buf) {
		buf.writeUtf(toString());
	}
	
	/**
	 * Deserialize from packet
	 */
	public static NBTPredicate read(PacketBuffer buf) {
		return NBTCompoundPredicate.parse(PacketBufferUtil.readString(buf)).orElse(null);
	}
	
	/**
	 * Serialize to string form. The result should be parsable back.
	 */
	@Override public abstract String toString();
	
	/**
	 * Pretty formatted text with {@link Style}
	 */
	public abstract IFormattableTextComponent getDisplay(Style style);
	
	/**
	 * Pretty formatted text with {@link NBTPredicate#defaultStyle}
	 */
	public IFormattableTextComponent getDisplay() {
		return getDisplay(defaultStyle);
	}
}
