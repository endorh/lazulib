package endorh.util.math;

import endorh.util.math.MathParser.ExpressionParser.ParseException;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import static endorh.util.math.MathParser.*;
import static endorh.util.text.TextUtil.stc;
import static java.lang.String.format;

/**
 * Expression highlighter based on {@link MathParser.UnicodeMathDoubleExpressionParser}
 */
@SuppressWarnings("unused")
public class MathHighlighter {
	public static final Map<String, TextFormatting> UNICODE_MATH_COLORED_NAMESPACE;
	public static final Map<Entry<String, Integer>, TextFormatting> UNICODE_MATH_COLORED_FUNCTIONS;
	public static final OperatorHierarchy<IFormattableTextComponent>
	  UNICODE_MATH_SYNTAX_HIGHLIGHT_OPERATOR_HIERARCHY;
	
	static {
		UNICODE_MATH_SYNTAX_HIGHLIGHT_OPERATOR_HIERARCHY =
		  new SyntaxHighlightOperatorHierarchy.Builder<>(UNICODE_MATH_OPERATOR_HIERARCHY).build();
		UNICODE_MATH_COLORED_NAMESPACE = new HashMap<>();
		for (String name : UNICODE_MATH_NAMES.keySet())
			UNICODE_MATH_COLORED_NAMESPACE.put(name, TextFormatting.AQUA);
		UNICODE_MATH_COLORED_FUNCTIONS = new HashMap<>();
		for (Entry<String, Integer> signature : UNICODE_MATH_FUNCTIONS.keySet()) {
			UNICODE_MATH_COLORED_FUNCTIONS.put(signature, TextFormatting.DARK_AQUA);
		}
	}
	
	/**
	 * Simple theme class, containing the colors used by a highlighter
	 */
	public static class HighlighterColorTheme {
		public TextFormatting numberColor = TextFormatting.AQUA;
		public TextFormatting braceColor = TextFormatting.GOLD;
		public TextFormatting operatorColor = TextFormatting.GOLD;
		public TextFormatting functionColor = TextFormatting.WHITE;
		public TextFormatting scriptColor = TextFormatting.AQUA;
		
		public TextFormatting[] nameColors = new TextFormatting[] {
		  TextFormatting.DARK_GREEN,
		  TextFormatting.DARK_RED,
		  TextFormatting.BLUE,
		  TextFormatting.YELLOW,
		  TextFormatting.LIGHT_PURPLE,
		  TextFormatting.DARK_PURPLE,
		  TextFormatting.RED,
		  TextFormatting.GREEN
		};
		
		public HighlighterColorTheme() {}
		
		public TextFormatting getNameColor(String name) {
			int hash = name.hashCode() % nameColors.length;
			if (hash < 0)
				hash += nameColors.length;
			return nameColors[hash];
		}
		
		/**
		 * Copy non null colors from another theme
		 */
		public void set(HighlighterColorTheme source) {
			if (source.numberColor != null) numberColor = source.numberColor;
			if (source.braceColor != null) braceColor = source.braceColor;
			if (source.operatorColor != null) operatorColor = source.operatorColor;
			if (source.functionColor != null) functionColor = source.functionColor;
			if (source.scriptColor != null) scriptColor = source.scriptColor;
			if (source.nameColors != null) nameColors = source.nameColors.clone();
		}
		
		/**
		 * Return a deep copy of this theme
		 */
		public HighlighterColorTheme copy() {
			final HighlighterColorTheme theme = new HighlighterColorTheme();
			theme.numberColor = numberColor;
			theme.braceColor = braceColor;
			theme.operatorColor = operatorColor;
			theme.functionColor = functionColor;
			theme.scriptColor = scriptColor;
			theme.nameColors = nameColors.clone();
			return theme;
		}
	}
	
	public static class UnicodeMathSyntaxHighlightParser extends ExpressionParser<IFormattableTextComponent> {
		public FixedNamespaceSet<TextFormatting> namespaceSetColors;
		public FixedNamespace<Entry<String, Integer>, TextFormatting> functionColors;
		public OperatorHierarchy<IFormattableTextComponent> operators;
		public Map<String, IFormattableTextComponent> translations;
		public HighlighterColorTheme colorTheme;
		public boolean allowMissingNames;
		
		public TextFormatting getNameColor(String name) {
			if (namespaceSetColors.containsName(name)) {
				TextFormatting color = namespaceSetColors.get(name);
				return color != null? color : colorTheme.getNameColor(name);
			}
			return colorTheme.getNameColor(name);
		}
		
		public UnicodeMathSyntaxHighlightParser(String... names) {
			this(Arrays.stream(names).collect(Collectors.toSet()), null, null, null, null);
		}
		
		public UnicodeMathSyntaxHighlightParser() {
			this((Map<String, TextFormatting>) null, null, null, null, null);
		}
		
		public UnicodeMathSyntaxHighlightParser(
		  Set<String> namespace
		) { this(namespace, null, null, null, null); }
		
		public UnicodeMathSyntaxHighlightParser(
		  Set<String> namespace,
		  @Nullable Map<String, IFormattableTextComponent> translations
		) { this(namespace, translations, null, null, null); }
		
		public UnicodeMathSyntaxHighlightParser(
		  Set<String> namespace,
		  @Nullable Map<String, IFormattableTextComponent> translations,
		  @Nullable Map<Entry<String, Integer>, TextFormatting> functions
		) { this(namespace, translations, functions, null, null); }
		
		public UnicodeMathSyntaxHighlightParser(
		  Set<String> namespace,
		  @Nullable Map<String, IFormattableTextComponent> translations,
		  @Nullable Map<Entry<String, Integer>, TextFormatting> functionColors,
		  @Nullable OperatorHierarchy<IFormattableTextComponent> operators
		) { this(namespace, translations, functionColors, operators, null); }
		
		public UnicodeMathSyntaxHighlightParser(
		  Set<String> namespace,
		  @Nullable Map<String, IFormattableTextComponent> translations,
		  @Nullable Map<Entry<String, Integer>, TextFormatting> functionColors,
		  @Nullable OperatorHierarchy<IFormattableTextComponent> operators,
		  HighlighterColorTheme colorTheme
		) { this(nullMap(namespace), translations, functionColors, operators, colorTheme); }
		
		public UnicodeMathSyntaxHighlightParser(
		  Set<String> namespace,
		  @Nullable Map<String, IFormattableTextComponent> translations,
		  @Nullable Map<Entry<String, Integer>, TextFormatting> functionColors,
		  @Nullable OperatorHierarchy<IFormattableTextComponent> operators,
		  HighlighterColorTheme colorTheme, boolean allowMissingNames
		) { this(nullMap(namespace), translations, functionColors, operators, colorTheme, allowMissingNames); }
		
		private static Map<String, TextFormatting> nullMap(Set<String> namespace) {
			final Map<String, TextFormatting> map = new HashMap<>();
			for (String name : namespace)
				map.put(name, null);
			return map;
		}
		
		public UnicodeMathSyntaxHighlightParser(
		  @Nullable Map<String, TextFormatting> namespaceColors
		) { this(namespaceColors, null, null, null, null); }
		
		public UnicodeMathSyntaxHighlightParser(
		  @Nullable Map<String, TextFormatting> namespaceColors,
		  @Nullable Map<String, IFormattableTextComponent> translations
		) { this(namespaceColors, translations, null, null, null); }
		
		public UnicodeMathSyntaxHighlightParser(
		  @Nullable Map<String, TextFormatting> namespaceColors,
		  @Nullable Map<String, IFormattableTextComponent> translations,
		  @Nullable Map<Entry<String, Integer>, TextFormatting> functionColors
		) { this(namespaceColors, translations, functionColors, null, null); }
		
		public UnicodeMathSyntaxHighlightParser(
		  @Nullable Map<String, TextFormatting> namespaceColors,
		  @Nullable Map<String, IFormattableTextComponent> translations,
		  @Nullable Map<Entry<String, Integer>, TextFormatting> functionColors,
		  @Nullable OperatorHierarchy<IFormattableTextComponent> operators
		) { this(namespaceColors, translations, functionColors, operators, null); }
		
		public UnicodeMathSyntaxHighlightParser(
		  @Nullable Map<String, TextFormatting> namespaceColors,
		  @Nullable Map<String, IFormattableTextComponent> translations,
		  @Nullable Map<Entry<String, Integer>, TextFormatting> functionColors,
		  @Nullable OperatorHierarchy<IFormattableTextComponent> operators,
		  @Nullable HighlighterColorTheme colorTheme
		) { this(namespaceColors, translations, functionColors, operators, colorTheme, true); }
		
		public UnicodeMathSyntaxHighlightParser(
		  @Nullable Map<String, TextFormatting> namespaceColors,
		  @Nullable Map<String, IFormattableTextComponent> translations,
		  @Nullable Map<Entry<String, Integer>, TextFormatting> functionColors,
		  @Nullable OperatorHierarchy<IFormattableTextComponent> operators,
		  @Nullable HighlighterColorTheme colorTheme,
		  boolean allowMissingNames
		) {
			this(
			  namespaceColors != null? FixedNamespaceSet.of(namespaceColors) : null, translations,
			  functionColors != null? new FixedNamespace<>(functionColors) : null, operators,
			  colorTheme, allowMissingNames
			);
		}
		
		public UnicodeMathSyntaxHighlightParser(
		  @Nullable FixedNamespaceSet<TextFormatting> namespaceSetColors,
		  @Nullable Map<String, IFormattableTextComponent> translations,
		  @Nullable FixedNamespace<Entry<String, Integer>, TextFormatting> functionColors,
		  @Nullable OperatorHierarchy<IFormattableTextComponent> operators,
		  @Nullable HighlighterColorTheme colorTheme
		) {
			this(namespaceSetColors, translations, functionColors, operators, colorTheme, true);
		}
		
		public UnicodeMathSyntaxHighlightParser(
		  @Nullable FixedNamespaceSet<TextFormatting> namespaceSetColors,
		  @Nullable Map<String, IFormattableTextComponent> translations,
		  @Nullable FixedNamespace<Entry<String, Integer>, TextFormatting> functionColors,
		  @Nullable OperatorHierarchy<IFormattableTextComponent> operators,
		  @Nullable HighlighterColorTheme colorTheme,
		  boolean allowMissingNames
		) {
			final String defaultNamespace = namespaceSetColors != null ? namespaceSetColors.defaultNamespace : "";
			final Map<String, Map<String, TextFormatting>> mathNames = new HashMap<>();
			mathNames.put(defaultNamespace, UNICODE_MATH_COLORED_NAMESPACE);
			this.namespaceSetColors = namespaceSetColors != null
			                          ? namespaceSetColors.copyWith(mathNames)
			                          : new FixedNamespaceSet<>(mathNames);
			this.functionColors = functionColors != null
			                      ? functionColors.copyWith(UNICODE_MATH_COLORED_FUNCTIONS)
			                      : new FixedNamespace<>(UNICODE_MATH_COLORED_FUNCTIONS);
			this.operators = operators != null? operators : UNICODE_MATH_SYNTAX_HIGHLIGHT_OPERATOR_HIERARCHY;
			this.translations = translations != null? translations : new HashMap<>();
			this.colorTheme = colorTheme != null? colorTheme : new HighlighterColorTheme();
			this.allowMissingNames = allowMissingNames;
		}
		
		@Override public ParsedExpression<IFormattableTextComponent> parse(String expression) {
			return new InternalUnicodeMathSyntaxHighlightParser(
			  expression, namespaceSetColors, functionColors, operators,
			  this, translations, colorTheme, allowMissingNames
			).parse();
		}
	}
	
	public static class StyleableParsedExpression extends ParsedExpression<IFormattableTextComponent> {
		public final HighlighterColorTheme colorTheme;
		public StyleableParsedExpression(
		  String expression, FixedNamespaceSet<IFormattableTextComponent> namespaceSet,
		  FixedNamespace<Entry<String, Integer>, ExpressionFunc<IFormattableTextComponent>> functions,
		  ExpressionNode<IFormattableTextComponent> parsed,
		  ExpressionParser<IFormattableTextComponent> parser,
		  HighlighterColorTheme colorTheme
		) {
			super(expression, namespaceSet, functions, parsed, parser);
			this.colorTheme = colorTheme;
		}
	}
	
	public static class InternalUnicodeMathSyntaxHighlightParser
	  extends InternalExpressionParser<IFormattableTextComponent> {
		private final HighlighterColorTheme colorTheme;
		private final FixedNamespaceSet<TextFormatting> namespaceSetColors;
		private final FixedNamespace<Entry<String, Integer>, TextFormatting> functionColors;
		private final Map<String, IFormattableTextComponent> translations;
		private final boolean allowMissingNames;
		
		public InternalUnicodeMathSyntaxHighlightParser(
		  String expression,
		  FixedNamespaceSet<TextFormatting> namespaceSetColors,
		  FixedNamespace<Entry<String, Integer>, TextFormatting> functionColors,
		  OperatorHierarchy<IFormattableTextComponent> operators,
		  ExpressionParser<IFormattableTextComponent> parser,
		  @Nullable Map<String, IFormattableTextComponent> translations,
		  @Nullable HighlighterColorTheme colorTheme,
		  boolean allowMissingNames
		) {
			super(expression, new FixedNamespaceSet<>(), new FixedNamespace<>(), operators, parser);
			this.namespaceSetColors = namespaceSetColors;
			this.functionColors = functionColors;
			this.translations = translations != null? translations : new HashMap<>();
			this.colorTheme = colorTheme != null? colorTheme.copy() : new HighlighterColorTheme();
			this.allowMissingNames = allowMissingNames;
		}
		
		public TextFormatting getNameColor(String name) {
			if (namespaceSetColors.containsName(name)) {
				final TextFormatting color = namespaceSetColors.get(name);
				return color != null? color : colorTheme.getNameColor(name);
			}
			return colorTheme.getNameColor(name);
		}
		
		public TextFormatting getFunctionColor(Entry<String, Integer> signature) {
			if (functionColors.contains(signature)) {
				TextFormatting color = functionColors.get(signature);
				return color != null? color : colorTheme.functionColor;
			}
			return colorTheme.functionColor;
		}
		
		@Override public StyleableParsedExpression parse() throws ParseException {
			ExpressionNode<IFormattableTextComponent> parsed = parseRoot();
			return new StyleableParsedExpression(
			  expression, namespaceSet, functions, parsed, parser, colorTheme);
		}
		
		@Override protected <O extends Operator<IFormattableTextComponent>> O decorate(O operator) {
			if (operator instanceof ThemedOperator)
				((ThemedOperator<?>) operator).init(colorTheme);
			return super.decorate(operator);
		}
		
		@Override public ExpressionNode<IFormattableTextComponent> parseFactor() {
			ExpressionNode<IFormattableTextComponent> x = parseNode();
			if (x != null)
				return x;
			double[] pre = eatScript();
			int start = pos;
			String name = eatName();
			if (name != null) {
				if (eat('(')) {
					final List<ExpressionNode<IFormattableTextComponent>> argsList = new ArrayList<>();
					if (!eat(')')) {
						argsList.add(first());
						while (!eat(')')) {
							if (!eat(','))
								throw parseException(
								  "Expecting comma or end parenthesis in function call, got: '" + ch + "'");
							argsList.add(first());
						}
					}
					final int n = argsList.size();
					//noinspection unchecked
					ExpressionNode<IFormattableTextComponent>[] a =
					  (ExpressionNode<IFormattableTextComponent>[]) new ExpressionNode[n];
					final ExpressionNode<IFormattableTextComponent>[] args = argsList.toArray(a);
					final Pair<String, Integer> signature = Pair.of(name, n);
					if (!allowMissingNames && !functionColors.contains(signature))
						throw functionParseException(name, n, start);
					x = decorate(() -> makeCall(signature, args), pre);
				} else if (allowMissingNames || namespaceSetColors.containsName(name)) {
					x = decorate(() -> makeName(name), pre);
				} else throw nameParseException(name, start);
			} else
				throw parseException("Unexpected: '" + ch + "'", pos);
			return x;
		}
		
		protected IFormattableTextComponent makeName(String name) {
			return translations.containsKey(name)
			       ? translations.get(name).plainCopy().withStyle(getNameColor(name))
			       : stc(name).withStyle(getNameColor(name));
		}
		
		protected IFormattableTextComponent makeCall(
		  Pair<String, Integer> signature, ExpressionNode<IFormattableTextComponent>[] args
		) {
			IFormattableTextComponent r = stc(signature.getKey())
			  .withStyle(getFunctionColor(signature))
			  .append(stc("(").withStyle(colorTheme.braceColor));
			for (int i = 0; i < args.length - 1; i++)
				r = r.append(args[i].eval()).append(stc(", ").withStyle(colorTheme.operatorColor));
			return r.append(args[args.length - 1].eval()).append(stc(")").withStyle(colorTheme.braceColor));
		}
		
		@Override public ExpressionNode<IFormattableTextComponent> parseNode() {
			int p = pos;
			double[] pre = eatScript();
			int start = pos;
			String digit = eatNumber();
			if (digit != null) {
				parseDouble(digit, start); // Assert the double is parsable
				return decorate(() -> stc(digit).withStyle(colorTheme.numberColor), pre);
			} else {
				setCursor(p);
				return null;
			}
		}
	}
	
	/**
	 * Provides a way to convert an already existing {@link OperatorHierarchy} into one
	 * which highlights the expression
	 */
	public static class SyntaxHighlightOperatorHierarchy extends OperatorHierarchy<IFormattableTextComponent> {
		public SyntaxHighlightOperatorHierarchy(
		  List<Entry<OperatorParser<IFormattableTextComponent, ?>,
			 LinkedHashMap<String, Operator<IFormattableTextComponent>>>> operators,
		  DecoratedOperator<IFormattableTextComponent, ? extends UnaryOperator<IFormattableTextComponent>>
			 decoratorFactory
		) { super(operators, decoratorFactory); }
		
		/**
		 * Builds a {@link SyntaxHighlightOperatorHierarchy} from an
		 * already existing {@link OperatorHierarchy} by creating synthetic
		 * {@link IFormattableTextComponent} operators which simply
		 * concatenate the evaluation of their arguments with the
		 * operators text
		 * @param <T> The type of the original {@link OperatorHierarchy}
		 */
		public static class Builder<T> {
			public final OperatorHierarchy<T> hierarchy;
			public Builder(OperatorHierarchy<T> hierarchy) {
				this.hierarchy = hierarchy;
			}
			
			public SyntaxHighlightOperatorHierarchy build() {
				List<Entry<OperatorParser<IFormattableTextComponent, ?>,
				  LinkedHashMap<String, Operator<IFormattableTextComponent>>>> list = new ArrayList<>();
				for (Entry<OperatorParser<T, ?>, LinkedHashMap<String, Operator<T>>> group : hierarchy.copyList()) {
					final Pair<OperatorParser<IFormattableTextComponent, ?>, Function<String,
					  Operator<IFormattableTextComponent>>> translation = translate(group.getKey());
					LinkedHashMap<String, Operator<IFormattableTextComponent>> map = new LinkedHashMap<>();
					for (String key : group.getValue().keySet())
						map.put(key, translation.getRight().apply(key));
					list.add(Pair.of(translation.getLeft(), map));
				}
				return new SyntaxHighlightOperatorHierarchy(list, getDecorator());
			}
			
			/**
			 * Translate an {@link OperatorParser} to the proper type, and provide
			 * a translator function which, given the string which defines an
			 * operator returns an {@link IFormattableTextComponent} {@link Operator} for it<br>
			 *
			 * Subclasses may override this method to add support for more classes,
			 * reusing this implementation by calling super in the else branch
			 *
			 * @param parser The parser to translate
			 */
			protected Pair<OperatorParser<IFormattableTextComponent, ?>, Function<String,
			  Operator<IFormattableTextComponent>>> translate(OperatorParser<T, ?> parser) {
				if (parser instanceof UnaryOperatorParser) {
					if (parser instanceof PrefixUnaryOperatorParser) {
						return Pair.of(new PrefixUnaryOperatorParser<>(), PrefixUnarySyntaxHighlightOperator::new);
					} else if (parser instanceof PostfixUnaryOperatorParser) {
						return Pair.of(new PostfixUnaryOperatorParser<>(), PostfixUnarySyntaxHighlightOperator::new);
					} else if (parser instanceof SurroundingUnaryOperatorParser) {
						return Pair.of(new SurroundingUnaryOperatorParser<>(), SurroundingUnarySyntaxHighlightOperator::new);
					} else throw new IllegalArgumentException(
					  "Unsupported UnaryOperatorParser type: " + parser.getClass().getName());
				} else if (parser instanceof BinaryOperatorParser) {
					return Pair.of(new LeftAssociativeBinaryOperatorParser<>(), BinarySyntaxHighlightOperator::new);
				} else if (parser instanceof TernaryOperatorParser) {
					return Pair.of(new RightAssociativeBinaryOperatorParser<>(), TernarySyntaxHighlightOperator::new);
				} else throw new IllegalArgumentException(
				  "Unsupported OperatorParser type: " + parser.getClass().getName());
			}
			
			/**
			 * Create a decorator operator for the hierarchy
			 */
			protected DecoratedThemedOperator<IFormattableTextComponent,
			  UnaryThemedOperator<IFormattableTextComponent>>
			getDecorator() {
				return new DecoratedThemedOperator<IFormattableTextComponent, UnaryThemedOperator<IFormattableTextComponent>>() {
					@Override public UnaryThemedOperator<IFormattableTextComponent> get(
					  double lu, double ru, double ld, double rd
					) {
						return new UnaryThemedOperator<IFormattableTextComponent>() {
							@Override public ExpressionNode<IFormattableTextComponent> apply(
							  ExpressionNode<IFormattableTextComponent> x
							) {
								IFormattableTextComponent d = stc("");
								if (!Double.isNaN(lu))
									d = d.append(stc(superscript(lu)).withStyle(theme.scriptColor));
								if (!Double.isNaN(ld))
									d = d.append(stc(subscript(ld)).withStyle(theme.scriptColor));
								d = d.append(x.eval());
								if (!Double.isNaN(rd))
									d = d.append(stc(subscript(rd)).withStyle(theme.scriptColor));
								if (!Double.isNaN(ru))
									d = d.append(stc(superscript(ru)).withStyle(theme.scriptColor));
								final IFormattableTextComponent r = d;
								return () -> r;
							}
						};
					}
				};
			}
			
			protected String subscript(double d) {
				if (Double.isNaN(d))
					return "";
				String l = format("%d", (long)d);
				StringBuilder builder = new StringBuilder();
				final char off = (char)0x2080 - '0';
				l.chars().forEach(ch -> {
					if (ch == '-') {
						builder.append('₋');
					} else if (ch >= '0' && ch <= '9') {
						builder.append(off + (char)ch);
					} else throw new IllegalStateException("Invalid number literal: '" + l + "'");
				});
				return builder.toString();
			}
			
			protected String superscript(double d) {
				if (Double.isNaN(d))
					return "";
				String l = format("%d", (long)d);
				StringBuilder builder = new StringBuilder();
				final char off = (char)0x2070 - '0';
				l.chars().forEach(i -> {
					if (i == '-') {
						builder.append('⁻');
					} else if (i < '0' || i > '9') {
						throw new IllegalStateException("Invalid number literal: '" + l + "'");
					} else {
						char ch = (char)i;
						switch (ch) {
							case '1': builder.append('¹'); break;
							case '2': builder.append('²'); break;
							case '3': builder.append('³'); break;
							default: builder.append(off + ch);
						}
					}
				});
				return builder.toString();
			}
		}
	}
	
	/**
	 * Special operator which can be configured with a {@link HighlighterColorTheme}
	 */
	public static abstract class ThemedOperator<T> implements Operator<T> {
		public HighlighterColorTheme theme;
		public void init(HighlighterColorTheme theme) {
			this.theme = theme;
		}
		
		/**
		 * Cleans an operator key by replacing its control characters
		 * by printable characters
		 */
		protected static String clean(String key) {
			return key.replace('\b', ' ').replace('\t', ' ');
		}
	}
	
	public static abstract class UnaryThemedOperator<T> extends ThemedOperator<T> implements UnaryOperator<T> {}
	public static abstract class BinaryThemedOperator<T> extends ThemedOperator<T> implements BinaryOperator<T> {}
	public static abstract class TernaryThemedOperator<T> extends ThemedOperator<T> implements  TernaryOperator<T> {}
	public static abstract class DecoratedThemedOperator<T, O extends ThemedOperator<T>> implements DecoratedOperator<T, O> {
		public HighlighterColorTheme theme;
		public void init(HighlighterColorTheme theme) {
			this.theme = theme;
		}
	}
	
	public static class PrefixUnarySyntaxHighlightOperator extends UnaryThemedOperator<IFormattableTextComponent> {
		public final String o;
		public PrefixUnarySyntaxHighlightOperator(String o) {
			this.o = clean(o);
		}
		@Override public ExpressionNode<IFormattableTextComponent> apply(
		  ExpressionNode<IFormattableTextComponent> a
		) { return () -> stc(o).withStyle(theme.operatorColor).append(a.eval()); }
	}
	public static class PostfixUnarySyntaxHighlightOperator extends UnaryThemedOperator<IFormattableTextComponent> {
		public final String o;
		public PostfixUnarySyntaxHighlightOperator(String o) {
			this.o = clean(o);
		}
		@Override public ExpressionNode<IFormattableTextComponent> apply(
		  ExpressionNode<IFormattableTextComponent> a
		) { return () -> a.eval().append(stc(o).withStyle(theme.operatorColor)); }
	}
	public static class SurroundingUnarySyntaxHighlightOperator extends UnaryThemedOperator<IFormattableTextComponent> {
		public final String l;
		public final String r;
		public SurroundingUnarySyntaxHighlightOperator(String s) {
			final String[] sp = s.split(" ");
			this.l = clean(sp[0]);
			this.r = clean(sp[1]);
		}
		public SurroundingUnarySyntaxHighlightOperator(String l, String r) {
			this.l = clean(l);
			this.r = clean(r);
		}
		@Override public ExpressionNode<IFormattableTextComponent> apply(
		  ExpressionNode<IFormattableTextComponent> a
		) {
			return () -> stc(l).withStyle(theme.operatorColor).append(a.eval())
			  .append(stc(r).withStyle(theme.operatorColor));
		}
	}
	public static class BinarySyntaxHighlightOperator extends BinaryThemedOperator<IFormattableTextComponent> {
		public final String o;
		public BinarySyntaxHighlightOperator(String o) {
			this.o = clean(o);
		}
		@Override public ExpressionNode<IFormattableTextComponent> apply(
		  ExpressionNode<IFormattableTextComponent> a, ExpressionNode<IFormattableTextComponent> b
		) {
			return () -> a.eval().append(stc(o).withStyle(theme.operatorColor)).append(b.eval());
		}
	}
	public static class TernarySyntaxHighlightOperator extends TernaryThemedOperator<IFormattableTextComponent> {
		public final String l;
		public final String r;
		public TernarySyntaxHighlightOperator(String s) {
			final String[] sp = s.split(" ");
			this.l = clean(sp[0]);
			this.r = clean(sp[1]);
		}
		public TernarySyntaxHighlightOperator(String l, String r) {
			this.l = clean(l);
			this.r = clean(r);
		}
		@Override public ExpressionNode<IFormattableTextComponent> apply(
		  ExpressionNode<IFormattableTextComponent> a, ExpressionNode<IFormattableTextComponent> b,
		  ExpressionNode<IFormattableTextComponent> c
		) {
			return () -> a.eval().append(stc(l).withStyle(theme.operatorColor))
			  .append(b.eval()).append(stc(r).withStyle(theme.operatorColor))
			  .append(c.eval());
		}
	}
}
