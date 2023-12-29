package endorh.lazulib.math;

import endorh.lazulib.math.MathParser.ExpressionParser.ParseException;
import endorh.lazulib.math.MathParser.ExpressionParser.ParseException.FunctionParseException;
import endorh.lazulib.math.MathParser.ExpressionParser.ParseException.NameParseException;
import endorh.lazulib.math.MathParser.ExpressionParser.ParseException.ScriptParseException;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static java.lang.Double.compare;
import static java.lang.Math.*;

/**
 * Parse math expressions with custom variables and functions.<br>
 * For usage examples, see the tests, or Aerobatic Elytra's Ability
 * Upgrade system which uses this expression parser to read arbitrary
 * expressions from recipe JSONs.<br>
 * <br>
 * Names of both, values and functions, must start with a letter, or a
 * character with code strictly greater than 127 that is not used by
 * any operator. The rest of the name's characters can be letters,
 * numbers, underscores '_', or characters with code strictly greater
 * than 127 that are not used by any operator.<br>
 * Names that do not match this pattern can be escaped between
 * backticks '`', similar to Kotlin names. It is not possible to
 * use backticks in names.<br>
 * <br>
 * Notes on the chosen operator hierarchy:
 * <ul>
 *    <li>
 *       The vertical bar '|' is parsed as absolute value parenthesis '|...|'
 *       unless surrounded by whitespace '... | ...', in which case it would be
 *       parsed as bit-wise or.<br>
 *       The broken bar '¦' is also understood as bit-wise or, and the 'abs(...)'
 *       function may be used instead of vertical bar parenthesis
 *    </li>
 *    <li>
 *       The xor symbol '⊻' is parsed as logical xor, while the
 *       direct sum symbol '⊕' is parsed as bit-wise xor, differing from Julia
 *    </li>
 *    <li>
 *       The exponent operator '^' is used for exponentiation
 *       instead of bit-wise xor differing from Java, and it's right associative<br>
 *       An immediately following hyphen is also allowed as unary
 *       minus for the exponent, even though the unary minus
 *       has less precedence than exponentiation
 *    </li>
 *    <li>
 *       The root symbol '√' is treated as a prefix unary operator and
 *       receives an optional prefix superscript argument which modifies
 *       its degree.<br>
 *       The cubic and quartic root symbols, '∛', '∜', do not receive
 *       prefix superscript arguments, but are parsed correctly.
 *    </li>
 *    <li>
 *       Unicode comparison symbols, {@code '≤', '≥', '≠', '≨' and '≩'}, are
 *       parsed as well as their classical ANSI sequences
 *       {@code '<=', '>=', '!=', '<', '>'}, and their concatenation
 *       {@code ('0 < x < 1')} is understood.<br>
 *       The approximation symbol '≈' compares two numbers numerically, and
 *       optionally receives a suffix subscript argument indicating how
 *       many decimal places should be compared.
 *    </li>
 *    <li>
 *       Floor '⌊...⌋' and ceiling '⌈...⌉' parenthesis are understood,
 *       and combining them like '⌊...⌉' will apply rounding instead
 *    </li>
 *    <li>
 *       Both, standard ternary operator '?', ':' and Python's 'if', 'else' ternary
 *       operator are understood. Both are right associative.<br>
 *       Other accepted word operators are 'or', 'xor', and 'and'. Word operators
 *       require word boundaries on both sides to be parsed.
 *    </li>
 *    <li>
 *       The division symbol '÷' and Python's '//' operator perform
 *       floor division<br>
 *       The percent symbol '%' shares meaning with Java and returns
 *       the remainder, as 'rem(a, b)'.<br>
 *       The modulus may be obtained through the 'mod(a, b)' function
 *    </li>
 *    <li>
 *       The Elvis operator '?:' corrects non-finite values (Infinity and NaN)
 *    </li>
 *    <li>
 *       The rest of the operators share meaning with Java/Python
 *    </li>
 * </ul>
 * The names 'π', 'e', 'NaN' and '∞' are parsed by default.<br>
 * Suffix superscripts following factors are parsed as exponents.<br><br>
 * You may as well define your own operator hierarchy, default namespace and function namespace.
 * See the static initializer for an example.<br>
 */
@SuppressWarnings("unused")
public class MathParser {
	
	public static final Map<String, Double> UNICODE_MATH_NAMES;
	public static final Map<Entry<String, Integer>, ExpressionFunc<Double>>
	  UNICODE_MATH_FUNCTIONS;
	public static final OperatorHierarchy<Double> UNICODE_MATH_OPERATOR_HIERARCHY;
	
	private static final Random random = new Random();
	
	static {
		// Standard operators
		OperatorHierarchy.Builder<Double> o = new OperatorHierarchy.Builder<>();
		o.addGroup(new RightAssociativeTernaryOperatorParser<>())
		  .add("? :", (a, b, c) -> () -> b(a.eval()) ? b.eval() : c.eval())
		  .add("\bif\b \belse\b", (a, b, c) -> () -> b(b.eval()) ? a.eval() : c.eval());
		o.addGroup(new LeftAssociativeBinaryOperatorParser<>())
		  .add("||", (a, b) -> () -> d(b(a.eval()) || b(b.eval())), "∨", "\bor\b");
		o.addGroup(new LeftAssociativeBinaryOperatorParser<>())
		  .add("⊻", (a, b) -> () -> d(b(a.eval()) != b(b.eval())), "\bxor\b");
		o.addGroup(new LeftAssociativeBinaryOperatorParser<>())
		  .add("&&", (a, b) -> () -> d(b(a.eval()) && b(b.eval())), "∧", "\band\b");
		o.addGroup(new PrefixUnaryOperatorParser<>())
		  .add("!", x -> () -> d(!b(x.eval())), "¬");
		o.addGroup(new LeftJoinedBinaryOperatorParser<>(
		  (a, b) -> () -> d(b(a.eval()) && b(b.eval()))))
		  // Double#equals distinguishes between +0D and -0D, as opposed to the == operator
		  .add("==", (a, b) -> () -> d(a.eval().doubleValue() == b.eval().doubleValue()), "=")
		  .add("≠", (a, b) -> () -> d(a.eval().doubleValue() != b.eval().doubleValue()), "!=")
		  .add("≤", (a, b) -> () -> d(a.eval() <= b.eval()), "<=")
		  .add("≥", (a, b) -> () -> d(a.eval() >= b.eval()), ">=")
		  .add("<", (a, b) -> () -> d(a.eval() < b.eval()), "≨")
		  .add(">", (a, b) -> () -> d(a.eval() > b.eval()), "≩")
		  .dec("≈", (lu, ru, ld, rd) -> {
			  if (!allNaN(lu, ru, ld))
				  return null;
			  if (anyNaN(rd))
				  return (a, b) -> () -> d(compare(a.eval(), b.eval()) == 0);
			  final double pow = pow(10D, rd);
			  return (a, b) -> () -> d(compare(
				 round(a.eval() * pow) / pow,
				 round(b.eval() * pow) / pow) == 0);
		  });
		o.addGroup(new LeftAssociativeBinaryOperatorParser<>())
		  .add("?:", (a, b) -> () -> Double.isFinite(a.eval()) ? a.eval() : b.eval());
		o.addGroup(new LeftAssociativeBinaryOperatorParser<>())
		  // | requires whitespace around, to distinguish from |...|
		  .add("\t|\t", (a, b) -> () -> d(l(a.eval()) | l(b.eval())), "¦");
		o.addGroup(new LeftAssociativeBinaryOperatorParser<>())
		  .add("⊕", (a, b) -> () -> d(l(a.eval()) ^ l(b.eval())));
		o.addGroup(new LeftAssociativeBinaryOperatorParser<>())
		  .add("&", (a, b) -> () -> d(l(a.eval()) & l(b.eval())));
		o.addGroup(new LeftAssociativeBinaryOperatorParser<>())
		  .add("<<", (a, b) -> () -> d(l(a.eval()) << l(b.eval())))
		  .add(">>>", (a, b) -> () -> d(l(a.eval()) >>> l(b.eval())))
		  .add(">>", (a, b) -> () -> d(l(a.eval()) >> l(b.eval())));
		o.addGroup(new LeftAssociativeBinaryOperatorParser<>())
		  .add("+", (a, b) -> () -> a.eval() + b.eval())
		  .add("-", (a, b) -> () -> a.eval() - b.eval());
		o.addGroup(new LeftAssociativeBinaryOperatorParser<>())
		  .add("*", (a, b) -> () -> a.eval() * b.eval(), "⋅", "·")
		  .add("÷", (a, b) -> () -> (a.eval() - a.eval() % b.eval()) / b.eval(), "//")
		  .add("/", (a, b) -> () -> a.eval() / b.eval())
		  .add("%", (a, b) -> () -> a.eval() % b.eval());
		o.addGroup(new PrefixUnaryOperatorParser<>())
		  .add("+", x -> x)
		  .add("-", x -> () -> -x.eval())
		  .add("~", x -> () -> d(~l(x.eval())));
		o.addGroup(new PrefixUnaryOperatorParser<>())
		  .dec("√", (lu, ru, ld, rd) ->
			 !allNaN(ru, ld, rd)
			 ? null
			 : anyNaN(lu)
			   ? x -> () -> sqrt(x.eval())           // Default is sqrt
			   : x -> () -> pow(x.eval(), 1D / lu))  // n-root
		  .add("∛", x -> () -> cbrt(x.eval()))
		  .add("∜", x -> () -> pow(x.eval(), 0.25D));
		o.addGroup(new SurroundingUnaryOperatorParser<>())
		  .add("( )", x -> x)
		  .add("| |", x -> () -> abs(x.eval()))
		  .add("⌊ ⌋", x -> () -> floor(x.eval()))
		  .add("⌈ ⌉", x -> () -> ceil(x.eval()))
		  .add("⌊ ⌉", x -> () -> d(round(x.eval())));
		o.addGroup(new RightAssociativeBinaryOperatorParser<>())
		  .add("^-", (a, b) -> () -> pow(a.eval(), -b.eval()))
		  .add("^", (a, b) -> () -> pow(a.eval(), b.eval()));
		o.setDecoratorFactory( // Handles superscripts and subscripts on factors
		  (lu, ru, ld, rd) ->
		    allNaN(lu, ru, ld, rd)
		    ? x -> x  // Nothing
		    : !allNaN(lu, ld, rd)
		      ? null     // Unsupported
		      : x -> () -> pow(x.eval(), ru)   // Exponentiation
		);
		UNICODE_MATH_OPERATOR_HIERARCHY = o.build();
		
		// Standard Variables
		UNICODE_MATH_NAMES = new ExpressionNamespaceBuilder<Double>()
		  .add("π", PI)
		  .add("e", E)
		  .add("NaN", Double.NaN)
		  .add("∞", Double.POSITIVE_INFINITY)
		  .build();
		
		// Standard functions
		// Using Math::abs,min,max directly is ambiguous, so...
		// noinspection Convert2MethodRef, SpellCheckingInspection
		UNICODE_MATH_FUNCTIONS = new ExpressionFuncMapBuilder<Double>()
		  // Roots
		  .add("sqrt", Math::sqrt)
		  .add("cbrt", Math::cbrt)
		  // Trigonometric
		  .add("sin", Math::sin)
		  .add("cos", Math::cos)
		  .add("tan", Math::tan)
		  .add("sec", x -> 1D / cos(x))
		  .add("csc", x -> 1D / sin(x))
		  .add("cot", x -> 1D / tan(x))
		  .add("asin", Math::asin)
		  .add("acos", Math::acos)
		  .add("atan", Math::atan)
		  .add("asec", x -> acos(1D / x))
		  .add("acsc", x -> asin(1D / x))
		  .add("acot", x -> atan(1D / x))
		  .add("atan", Math::atan2)
		  .add("atan2", Math::atan2)
		  // Degrees / Radians
		  .add("rad", Math::toRadians)
		  .add("deg", Math::toDegrees)
		  // Trigonometric in degrees
		  .add("sind", x -> sin(toRadians(x)))
		  .add("cosd", x -> cos(toRadians(x)))
		  .add("tand", x -> tan(toRadians(x)))
		  .add("secd", x -> 1D / cos(toRadians(x)))
		  .add("cscd", x -> 1D / sin(toRadians(x)))
		  .add("cotd", x -> 1D / tan(toRadians(x)))
		  .add("asind", x -> toDegrees(asin(x)))
		  .add("acosd", x -> toDegrees(acos(x)))
		  .add("atand", x -> toDegrees(atan(x)))
		  .add("asecd", x -> toDegrees(acos(1D / x)))
		  .add("acscd", x -> toDegrees(asin(1D / x)))
		  .add("acotd", x -> toDegrees(atan(1D / x)))
		  .add("atand", (y, x) -> toDegrees(atan2(y, x)))
		  // Hyperbolic
		  .add("sinh", Math::sinh)
		  .add("cosh", Math::cosh)
		  .add("tanh", Math::tanh)
		  .add("sech", x -> 1D / cosh(x))
		  .add("csch", x -> 1D / sinh(x))
		  .add("tanh", x -> 1D / tanh(x))
		  .add("asinh", x -> log(x + sqrt(x * x + 1)))
		  .add("acosh", x -> log(x + sqrt(x * x - 1)))
		  .add("atanh", x -> log((1 + x) / (1 - x)) / 2)
		  .add("acoth", x -> log((x + 1) / (x - 1)) / 2)
		  .add("asech", x -> log((1 + sqrt(1 - x * x)) / x))
		  .add("acsch", x -> log((1 + sqrt(x * x + 1)) / x))
		  // Exponentiation
		  .add("exp", Math::exp)
		  .add("ln", Math::log)
		  .add("log", Math::log)
		  .add("log2", x -> log(x) / log(2))
		  .add("log10", Math::log10)
		  .add("log1p", Math::log1p)
		  // Modulus
		  .add("mod", (a, b) -> a < 0D? (a % b) + b : a % b)
		  .add("rem", (a, b) -> a % b)
		  // Rectification
		  .add("abs", x -> abs(x))
		  .add("max", (a, b) -> max(a, b))
		  .add("min", (a, b) -> min(a, b))
		  .add("clamp", (x, min, max) -> min(max, max(min, x)))
		  .add("lerp", (t, min, max) -> t * max + (1 - t) * min)
		  .add("clampLerp", (t, min, max) -> t <= 0 ? min : (t >= 1 ? max : t * max + (1 - t) * min))
		  .add("sign", x -> signum(x))
		  .add("copySign", (x, y) -> copySign(x, y))
		  .add("flipSign", (x, y) -> copySign(x, x * y))
		  .add("relu", x -> max(0, x))
		  .add("sigmoid", x -> 1D / (1D + exp(-x)))
		  .add("rect", x -> abs(x) > 0.5D ? 0D : abs(x) == 0.5D ? 0.5D : 1D)
		  // Rounding
		  .add("round", x -> d(Math.round(x)))
		  .add("floor", Math::floor)
		  .add("ceil", Math::ceil)
		  // Sinc
		  .add("sinc", x -> x == 0 ? 1D : sin(PI * x) / (PI * x))
		  .add("cosc", x -> x == 0 ? 0D : cos(PI * x) / x - sin(PI * x) / (PI * x * x))
		  // NaN / Infinite
		  .add("isNaN", x -> d(Double.isNaN(x)))
		  .add("isFinite", x -> d(Double.isFinite(x)))
		  .add("isInfinite", x -> d(Double.isInfinite(x)))
		  // Random
		  .add("rand", Math::random)
		  .add("rand", d -> random.nextDouble() * d)
		  .add("rand", (a, b) -> a + random.nextDouble() * (b - a))
		  .add("randGaussian", () -> random.nextGaussian())
		  .add("randGaussian", s -> random.nextGaussian() * s)
		  .add("randGaussian", (m, s) -> m + random.nextGaussian() * s)
		  .add("randInt", i -> d(random.nextInt(i(i))))
		  .add("randInt", (a, b) -> a + random.nextInt(i(b - a)))
		  // Interpolation
		  .add("quadInOut",
		       t -> t <= 0D ? 0D : (t <= 0.5D ? 2D * t * t : (t < 1D ? -1D + (4D - 2D * t) * t : 1D)))
		  .add("quadIn", t -> t <= 0D ? 0D : (t < 1D ? t * t : 1D))
		  .add("quadOut", t -> t <= 0D ? 0D : (t < 1D ? 2 * t - t * t : 1D))
		  .build();
	}
	
	private static <K, V> Map<K, V> nullMap(Set<K> set) {
		final HashMap<K, V> map = new HashMap<>();
		for (K k : set)
			map.put(k, null);
		return map;
	}
	
	/**
	 * A parsed expression that can be evaluated.<br>
	 * If the expression requires a namespace or function namespace at
	 * evaluation type and it is not present, an {@link EvalException} will be thrown
	 *
	 * @param <T> Expression return type
	 */
	public static class ParsedExpression<T> {
		private final String expression;
		private final FixedNamespaceSet<T> namespaceSet;
		private final FixedNamespace<Entry<String, Integer>, ExpressionFunc<T>> functions;
		private final ExpressionNode<T> parsed;
		private final ExpressionParser<T> parser;
		
		public ParsedExpression(
		  String expression,
		  FixedNamespaceSet<T> namespaceSet,
		  FixedNamespace<Entry<String, Integer>, ExpressionFunc<T>> functions,
		  ExpressionNode<T> parsed,
		  ExpressionParser<T> parser
		) {
			this.expression = expression;
			this.namespaceSet = namespaceSet;
			this.functions = functions;
			this.parsed = parsed;
			this.parser = parser;
		}
		
		/**
		 * Evaluate the expression
		 */
		public T eval() {
			return parsed.eval();
		}
		
		/**
		 * Evaluate the expression using the given namespace<br>
		 * The names in the namespace are parsed for separators. To provide
		 * the namespace set as a map of maps, see {@link ParsedExpression#evalX(Map)}<br>
		 * You may also provide a function namespace
		 * @return The evaluated result
		 * @see ParsedExpression#evalX(Map)
		 */
		public T eval(Map<String, T> namespace) {
			this.namespaceSet.setFrom(namespace);
			final T r = parsed.eval();
			this.namespaceSet.reset();
			return r;
		}
		
		/**
		 * Evaluate the expression using the given namespace and functions<br>
		 * The names in the namespace are parsed for separators. To provide
		 * the namespace set as a map of maps, see {@link ParsedExpression#evalX(Map, Collection)}
		 * @return The evaluated result
		 * @see ParsedExpression#evalX(Map, Collection)
		 */
		public T eval(
		  Map<String, T> namespace,
		  Collection<? extends Entry<String, ? extends ExpressionFunc<T>>> functions
		) {
			this.namespaceSet.setFrom(namespace);
			for (Entry<String, ? extends ExpressionFunc<T>> e : functions) {
				final ExpressionFunc<T> func = e.getValue();
				this.functions.set(Pair.of(e.getKey(), func.getNumberOfArguments()), func);
			}
			final T r = parsed.eval();
			this.namespaceSet.reset();
			this.functions.reset();
			return r;
		}
		
		/**
		 * Evaluate the expression using the given namespace and functions<br>
		 * The names in the namespace are parsed for separators. To provide
		 * the namespace set as a map of maps, see {@link ParsedExpression#evalX(Map, Map)}
		 *
		 * @return The evaluated result
		 * @see ParsedExpression#evalX(Map, Map)
		 */
		public T eval(
		  Map<String, T> namespace,
		  Map<Entry<String, Integer>, ExpressionFunc<T>> functions
		) {
			this.namespaceSet.setFrom(namespace);
			this.functions.setAll(functions);
			final T r = parsed.eval();
			this.namespaceSet.reset();
			this.functions.reset();
			return r;
		}
		
		/**
		 * Evaluate the expression using the given namespace<br>
		 * The names in the namespace are parsed for separators. To provide
		 * the namespace set as a map of maps, see {@link ParsedExpression#eval(Map)}
		 * @return The evaluated result
		 * @see ParsedExpression#eval(Map)
		 */
		public T evalX(Map<String, Map<String, T>> namespace) {
			this.namespaceSet.setAll(namespace);
			final T r = parsed.eval();
			this.namespaceSet.reset();
			this.functions.reset();
			return r;
		}
		
		/**
		 * Evaluate the expression using the given namespace and functions<br>
		 * To provide the namespace set as a flat map of composed names, see
		 * {@link ParsedExpression#eval(Map, Collection)}
		 * @return The evaluated result
		 * @see ParsedExpression#eval(Map, Collection)
		 */
		public T evalX(
		  Map<String, Map<String, T>> namespace,
		  Collection<? extends Entry<String, ? extends ExpressionFunc<T>>> functions
		) {
			this.namespaceSet.setAll(namespace);
			for (Entry<String, ? extends ExpressionFunc<T>> e : functions) {
				final ExpressionFunc<T> func = e.getValue();
				this.functions.set(Pair.of(e.getKey(), func.getNumberOfArguments()), func);
			}
			final T r = parsed.eval();
			this.namespaceSet.reset();
			this.functions.reset();
			return r;
		}
		
		/**
		 * Evaluate the expression using the given namespace and functions<br>
		 * To provide the namespace set as a flat map of composed names, see
		 * {@link ParsedExpression#eval(Map, Map)}
		 * @return The evaluated result
		 * @see ParsedExpression#eval(Map, Map)
		 */
		public T evalX(
		  Map<String, Map<String, T>> namespace,
		  Map<Entry<String, Integer>, ExpressionFunc<T>> functions
		) {
			this.namespaceSet.setAll(namespace);
			this.functions.setAll(functions);
			final T r = parsed.eval();
			this.namespaceSet.reset();
			this.functions.reset();
			return r;
		}
		
		/**
		 * Evaluate the expression using the given namespace<br>
		 * You may also provide a function namespace, or provide the namespace as a map
		 * @return The evaluated result
		 */
		public T eval(FixedNamespaceSet<T> namespaceSet) {
			this.namespaceSet.setAll(namespaceSet);
			final T r = parsed.eval();
			this.namespaceSet.reset();
			return r;
		}
		
		/**
		 * Evaluate the expression using the given functions<br>
		 * You may also provide a namespace, or provide the functions as a map
		 * @return The evaluated result
		 */
		public T eval(FixedNamespace<Entry<String, Integer>, ExpressionFunc<T>> functions) {
			this.functions.setAll(functions);
			final T r = parsed.eval();
			this.functions.reset();
			return r;
		}
		
		/**
		 * Evaluate the expression using the given namespace and functions<br>
		 * You may also provide the namespace or functions as maps
		 * @return The evaluated result
		 */
		public T eval(
		  FixedNamespaceSet<T> namespaceSet,
		  FixedNamespace<Entry<String, Integer>, ExpressionFunc<T>> functions
		) {
			this.namespaceSet.setAll(namespaceSet);
			this.functions.setAll(functions);
			final T r = parsed.eval();
			this.namespaceSet.reset();
			this.functions.reset();
			return r;
		}
		
		@Override public String toString() { return expression; }
		/**
		 * Guarantees that the expression can be rebuilt from its
		 * string form (using the same namespaces)
		 *
		 * @return The original expression used to parse this
		 */
		public String getExpression() { return expression; }
		public ExpressionParser<T> getParser() { return parser; }
		
		public FixedNamespaceSet<T> getNamespaceSet() { return namespaceSet; }
		public FixedNamespace<Entry<String, Integer>, ExpressionFunc<T>> getFunctions() { return functions; }
		
		public static class EvalException extends RuntimeException {
			public EvalException(String message) { super(message); }
		}
	}
	
	/**
	 * Does the parsing for {@link ExpressionParser} to build a
	 * {@link ParsedExpression}
	 */
	public static abstract class InternalExpressionParser<T> {
		protected static final Set<Character> scriptCharacters;
		
		static {
			Set<Character> c = new HashSet<>();
			c.add('⁰'); // U+2070
			c.add('¹'); // U+00B9
			c.add('²'); // U+00B2
			c.add('³'); // U+00B3
			for (int i = 4; i <= 0xB; i++)
				c.add((char) (0x2070 + i));
			for (int i = 0; i <= 0xB; i++)
				c.add((char) (0x2080 + i));
			scriptCharacters = c;
		}
		
		public final String expression;
		public final FixedNamespaceSet<T> namespaceSet;
		public final FixedNamespace<Entry<String, Integer>, ExpressionFunc<T>> functions;
		public final OperatorHierarchy<T> operators;
		public final ExpressionParser<T> parser;
		
		protected int pos = -1, depth = -1;
		protected char ch = 0;
		
		/**
		 * To use default operators, functions or variables, see
		 * {@link DoubleExpressionParser}
		 */
		public InternalExpressionParser(
		  String expression,
		  FixedNamespaceSet<T> namespaceSet,
		  FixedNamespace<Entry<String, Integer>, ExpressionFunc<T>> functions,
		  OperatorHierarchy<T> operators,
		  ExpressionParser<T> parser
		) {
			this.expression = expression;
			this.operators = operators;
			this.namespaceSet = namespaceSet.copy();
			this.functions = functions.copy();
			this.parser = parser;
		}
		
		public ParsedExpression<T> parse() throws ParseException {
			ExpressionNode<T> parsed = parseRoot();
			return new ParsedExpression<>(expression, namespaceSet, functions, parsed, parser);
		}
		
		protected void nextChar() {
			ch = (++pos < expression.length()) ? expression.charAt(pos) : 0;
		}
		
		protected void setCursor(int p) {
			pos = p;
			ch = pos < expression.length() ? expression.charAt(pos) : 0;
		}
		
		protected boolean peek(char charToPeek) {
			if (pos + 1 < expression.length())
				return expression.charAt(pos + 1) == charToPeek;
			return false;
		}
		
		protected boolean eat(char charToEat) {
			eatWhitespace();
			if (ch == charToEat) {
				nextChar();
				return true;
			}
			return false;
		}
		
		protected void eatWhitespace() {
			while (Character.isWhitespace(ch)) nextChar();
		}
		
		protected boolean isWord(char ch) {
			return !Character.isWhitespace(ch) && !operators.use(ch) && !scriptCharacters.contains(ch);
		}
		
		protected boolean eatString(String str) {
			eatWhitespace();
			int n = str.length();
			if ('\b' == str.charAt(0)) {
				if (pos > 0 && isWord(expression.charAt(pos - 1)))
					return false;
				n--;
				str = str.substring(1);
			} else if ('\t' == str.charAt(0)) {
				if (pos == 0 || !Character.isWhitespace(expression.charAt(pos - 1)))
					return false;
				n--;
				str = str.substring(1);
			}
			char end_char = 0;
			if ('\b' == str.charAt(n - 1))
				end_char = '\b';
			else if ('\t' == str.charAt(n - 1))
				end_char = '\t';
			if (end_char != 0) {
				str = str.substring(0, n - 1);
				n--;
			}
			if (pos + n <= expression.length()
			    && str.equals(expression.substring(pos, pos + n))) {
				setCursor(pos + n);
				return switch (end_char) {
					case '\b' -> !isWord(ch);
					case '\t' -> Character.isWhitespace(ch);
					default -> true;
				};
			}
			return false;
		}
		
		protected String displayName(String op_sequence) {
			return op_sequence.replace("\b", "");
		}
		
		protected String eatNumber() {
			eatWhitespace();
			if (ch >= '0' && ch <= '9' || ch == '.') {
				int start = pos;
				while (ch >= '0' && ch <= '9') nextChar();
				if (ch == '.') nextChar();
				while (ch >= '0' && ch <= '9') nextChar();
				if (ch == 'e' || ch == 'E') {
					nextChar();
					if (ch == '+' || ch == '-')
						nextChar();
					while (ch >= '0' && ch <= '9') nextChar();
				}
				return expression.substring(start, pos);
			}
			return null;
		}
		
		protected String eatName() {
			if (eat('`')) {
				final int start = pos;
				while (ch != '`')
					nextChar();
				nextChar();
				return expression.substring(start, pos-1);
			}
			if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch > 127 && isWord(ch)) {
				final int start = pos;
				while (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9' ||
				       ch == '_' || ch > 127 && isWord(ch))
					nextChar();
				return expression.substring(start, pos);
			}
			return null;
		}
		
		protected String eatSuperscript() {
			eatWhitespace();
			StringBuilder e = new StringBuilder();
			if (ch == '⁺') { // U+207A
				e.append('+');
				nextChar();
			} else if (ch == '⁻') { // U+207B
				e.append('-');
				nextChar();
			}
			parse:
			{
				while (true) {
					switch (ch) {
						case '⁰': e.append('0'); break; // U+2070
						case '¹': e.append('1'); break; // U+00B9
						case '²': e.append('2'); break; // U+00B2
						case '³': e.append('3'); break; // U+00B3
						default:
							if (ch >= 0x2074 && ch <= 0x2079) {
								e.append((int) ch - 0x2070);
								break;
							} else break parse;
					}
					nextChar();
				}
			}
			return e.length() > 0 ? e.toString() : null;
		}
		
		protected String eatSubscript() {
			eatWhitespace();
			StringBuilder e = new StringBuilder();
			if (ch == '₊') { // U+208A
				e.append('+');
				nextChar();
			} else if (ch == '₋') { // U+208B
				e.append('-');
				nextChar();
			}
			while (ch >= 0x2080 && ch <= 0x2089) {
				e.append((int) ch - 0x2080);
				nextChar();
			}
			return e.length() > 0 ? e.toString() : null;
		}
		
		protected double[] eatScript() {
			eatWhitespace();
			double[] tuple = new double[]{Double.NaN, Double.NaN};
			int start = pos;
			String sub = eatSubscript();
			if (sub != null) {
				tuple[1] = parseDouble(sub, start);
				start = pos;
				String sup = eatSuperscript();
				if (sup != null) {
					tuple[0] = parseDouble(sup, start);
				}
			} else {
				String sup = eatSuperscript();
				if (sup != null) {
					tuple[0] = parseDouble(sup, start);
					start = pos;
					sub = eatSubscript();
					if (sub != null) {
						tuple[1] = parseDouble(sub, start);
					}
				}
			}
			return tuple;
		}
		
		/**
		 * Called by operator parsers before applying an operator
		 */
		protected <O extends Operator<T>> O decorate(O operator) {
			return operator;
		}
		
		/**
		 * Decorate a node with using the decorator factory operator
		 * and the prefix script array
		 */
		protected ExpressionNode<T> decorate(ExpressionNode<T> node, double[] pre) {
			if (operators.decoratorFactory != null)
				return decorate(operators.decoratorFactory.decorate(this, pre)).apply(node);
			else if (!allNaN(pre))
				throw scriptParseException(pre);
			return node;
		}
		
		/**
		 * Parses the expression string into an {@link ExpressionNode}
		 *
		 * @return Parsed expression
		 */
		protected ExpressionNode<T> parseRoot() {
			try {
				nextChar();
				depth = -1;
				ExpressionNode<T> x = next();
				if (pos < expression.length())
					throw parseException("Unexpected: " + ch);
				return x;
			} catch (StackOverflowError e) {
				throw new ParseException(
				  "Stack overflow, the expression is too complex to be parsed", expression, pos);
			}
		}
		
		/**
		 * Parse first operator group
		 *
		 * @return Parsed expression
		 */
		protected ExpressionNode<T> first() {
			int prev = depth;
			depth = -1;
			ExpressionNode<T> x = next();
			depth = prev;
			return x;
		}
		
		/**
		 * Parse next operator group
		 *
		 * @return Parsed expression
		 */
		protected ExpressionNode<T> next() {
			if (++depth < operators.size()) {
				ExpressionNode<T> x = operators.get(depth).getKey().init(this)
				  .parse(operators.get(depth).getValue());
				depth--;
				return x;
			}
			ExpressionNode<T> x = parseFactor();
			depth--;
			return x;
		}
		
		/**
		 * Parses the tightest elements of the grammar, after all operators
		 *
		 * @return Parsed expression
		 */
		public ExpressionNode<T> parseFactor() {
			ExpressionNode<T> x = parseNode();
			if (x != null)
				return x;
			double[] pre = eatScript();
			int start = pos;
			String name = eatName();
			if (name != null) {
				if (eat('(')) {
					final List<ExpressionNode<T>> in = new ArrayList<>();
					if (!eat(')')) {
						in.add(first());
						while (!eat(')')) {
							if (!eat(','))
								throw parseException(
								  "Expecting comma or end parenthesis in function call, got: '" + ch + "'");
							in.add(first());
						}
					}
					int n = in.size();
					final Pair<String, Integer> signature = Pair.of(name, n);
					//noinspection unchecked
					final ExpressionNode<T>[] arr = (ExpressionNode<T>[]) new ExpressionNode[n];
					in.toArray(arr);
					if (!functions.contains(signature))
						throw functionParseException(name, n, start);
					x = decorate(() -> callFunction(signature, arr), pre);
				} else if (namespaceSet.containsName(name)) {
					x = decorate(() -> getName(name), pre);
				} else throw nameParseException(name, start);
			} else
				throw parseException("Unexpected: '" + ch + "'", pos);
			return x;
		}
		
		public abstract ExpressionNode<T> parseNode();
		
		protected T getName(String name) {
			if (namespaceSet.containsName(name)) {
				final T value = namespaceSet.get(name);
				if (value != null)
					return value;
			}
			throw new ParsedExpression.EvalException(
			  "Name '" + name + "' was defined at parse time, but not present at runtime");
		}
		
		protected T callFunction(Pair<String, Integer> signature, ExpressionNode<T>[] params) {
			//noinspection unchecked
			T[] args = Arrays.stream(params).map(ExpressionNode::eval)
			  .toArray(i -> (T[]) new Object[i]);
			return functions.get(signature).eval(args);
		}
		
		/**
		 * Fill exception arguments in automatically
		 */
		public ParseException parseException(String msg) {
			return new ParseException(msg, expression, pos);
		}
		
		public ParseException parseException(String msg, int pos) {
			return new ParseException(msg, expression, pos);
		}
		
		public NameParseException nameParseException(String name) {
			return nameParseException(name, pos);
		}
		
		public NameParseException nameParseException(String name, int pos) {
			return new NameParseException(name, expression, pos);
		}
		
		public FunctionParseException functionParseException(String name, int parameterCount) {
			return functionParseException(name, parameterCount, pos);
		}
		
		public FunctionParseException functionParseException(
		  String name, int parameterCount, int pos
		) {
			return new FunctionParseException(name, parameterCount, expression, pos);
		}
		
		public ScriptParseException scriptParseException(double[] pre, double[] pos) {
			if (allNaN(pre, pos))
				return new ScriptParseException(
				  "Expected subscript or superscript", expression, this.pos);
			return new ScriptParseException(
			  "Unexpected subscript or superscript", expression, this.pos);
		}
		
		public ParseException scriptParseException(double[] pre) {
			return scriptParseException(pre, new double[]{Double.NaN, Double.NaN});
		}
		
		protected Double parseDouble(String literal, int pos) {
			try {
				return Double.parseDouble(literal);
			} catch (NumberFormatException e) {
				throw parseException("Malformed number literal: '" + literal + "'", pos);
			}
		}
		
	}
	
	/**
	 * Abstract expression parser<br>
	 * Used to hold defaults for an {@link InternalExpressionParser} constructor
	 *
	 * @param <T> Expression type
	 */
	public static abstract class ExpressionParser<T> {
		public abstract ParsedExpression<T> parse(String expression);
		public @Nullable ParsedExpression<T> tryParse(String expression) {
			try {
				return parse(expression);
			} catch (ParseException e) {
				return null;
			}
		}
		
		/**
		 * Generic parse exception
		 */
		public static class ParseException extends RuntimeException {
			protected ParseException(String message, String expression, int pos) {
				super(addContextInfo(message, expression, pos));
			}
			
			private static String addContextInfo(String message, String expr, int pos) {
				if (expr.length() <= 40) { // Show full expression
					return message + "\n\tparsing expression: '" + expr + "'" +
					       "\n\t" + spaces("parsing expression: '") + spaces(pos) + "^" +
					       "\n\tat pos: " + pos;
				} else { // Show relevant fragment
					return message + "\n\tparsing expression: '" + (pos > 20 ? "..." : "") +
					       expr.substring(Math.max(0, pos - 20), Math.min(expr.length(), pos + 20)) +
					       (pos + 20 < expr.length() ? "..." : "") + "'" +
					       "\n\t" + spaces("parsing expression: '") + spaces(pos > 20 ? 3 + 20 : pos) +
					       "^" +
					       "\n\tat pos: " + pos;
				}
			}
			
			protected static String spaces(@SuppressWarnings("SameParameterValue") String str) {
				return spaces(str.length());
			}
			
			protected static String spaces(int n) {
				return " ".repeat(Math.max(0, n));
			}
			
			/**
			 * Parsed malformed scripts
			 */
			public static class ScriptParseException extends ParseException {
				protected ScriptParseException(String message, String expression, int pos) {
					super(message, expression, pos);
				}
			}
			
			/**
			 * Parsed unknown name
			 */
			public static class NameParseException extends ParseException {
				public final String name;
				protected NameParseException(String name, String expression, int pos) {
					super("Unknown name: \"" + name + "\"", expression, pos);
					this.name = name;
				}
			}
			
			/**
			 * Parsed unknown function
			 */
			public static class FunctionParseException extends ParseException {
				public final String functionName;
				public final int parameterCount;
				protected FunctionParseException(
				  String name, int parameterCount, String expression, int pos
				) {
					super("Unknown function: \"" + name + "\" with " + parameterCount + " parameters",
					      expression, pos);
					functionName = name;
					this.parameterCount = parameterCount;
				}
			}
		}
	}
	
	/**
	 * A single node in the Abstract Syntax Tree of a parsed
	 * {@link InternalExpressionParser}
	 */
	@FunctionalInterface
	public interface ExpressionNode<T> {
		T eval();
	}
	
	/**
	 * Special {@link ExpressionNode} with a fixed value
	 */
	public static class ComputedExpressionNode<T> implements ExpressionNode<T> {
		public final T value;
		public ComputedExpressionNode(T value) {
			this.value = value;
		}
		@Override public T eval() {
			return value;
		}
	}
	
	/**
	 * Double expression recursive descent parser.<br><br>
	 * <p>
	 * A variable and function namespace may be defined at parse time,
	 * and populated at evaluation time.<br>
	 * An {@link OperatorHierarchy} must be provided at parse time.<br>
	 * Parsing takes place on construction.<br><br>
	 * <p>
	 * Evaluation is not thread-safe.<br><br>
	 * <p>
	 * To use a standard set of operators, names and functions, see
	 * {@link UnicodeMathDoubleExpressionParser}.<br><br>
	 * <p>
	 * May throw {@link ParseException} when parsing.
	 *
	 * @see ParsedExpression
	 */
	public static class DoubleExpressionParser extends ExpressionParser<Double> {
		public FixedNamespaceSet<Double> namespaceSet = null;
		public FixedNamespace<Entry<String, Integer>, ExpressionFunc<Double>> functions = null;
		public OperatorHierarchy<Double> operators = null;
		
		public DoubleExpressionParser() {}
		
		public DoubleExpressionParser(
		  @Nullable FixedNamespaceSet<Double> namespaceSet,
		  @Nullable FixedNamespace<Entry<String, Integer>, ExpressionFunc<Double>> functions,
		  OperatorHierarchy<Double> operators
		) {
			this.namespaceSet = namespaceSet != null? namespaceSet : new FixedNamespaceSet<>();
			this.functions = functions != null? functions : new FixedNamespace<>();
			this.operators = operators;
		}
		
		@Override
		public ParsedExpression<Double> parse(String expression) {
			if (operators == null)
				operators = new OperatorHierarchy<>(new ArrayList<>(), null);
			return new InternalDoubleExpressionParser(
			  expression, namespaceSet, functions, operators, this
			).parse();
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			DoubleExpressionParser that = (DoubleExpressionParser) o;
			return Objects.equals(namespaceSet, that.namespaceSet) &&
			       Objects.equals(functions, that.functions) &&
			       Objects.equals(operators, that.operators);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(namespaceSet, functions, operators);
		}
	}
	
	/**
	 * {@link DoubleExpressionParser} using default math operators,
	 * variables and functions.<br><br>
	 * <p>
	 * Provided variable and function namespaces are merged with the
	 * standard ones, overwriting the defaults if conflicting.<br><br>
	 * <p>
	 * The operator hierarchy can be overridden completely, since there's no
	 * natural way to merge two of them. However, you may build your own
	 * from the default by passing the default to a new builder.
	 */
	public static class UnicodeMathDoubleExpressionParser extends DoubleExpressionParser {
		
		public UnicodeMathDoubleExpressionParser(String... names) {
			this(Arrays.stream(names).collect(Collectors.toSet()), null, null);
		}
		
		public UnicodeMathDoubleExpressionParser() {
			this((Map<String, Double>) null, null, null);
		}
		
		public UnicodeMathDoubleExpressionParser(
		  Set<String> namespace
		) { this(namespace, null, null); }
		
		public UnicodeMathDoubleExpressionParser(
		  Set<String> namespace,
		  @Nullable Map<Entry<String, Integer>, ExpressionFunc<Double>> functions
		) { this(namespace, functions, null); }
		
		public UnicodeMathDoubleExpressionParser(
		  Set<String> namespace,
		  @Nullable Map<Entry<String, Integer>, ExpressionFunc<Double>> functions,
		  @Nullable OperatorHierarchy<Double> operators
		) {
			this(nullMap(namespace), functions, operators);
		}
		
		public UnicodeMathDoubleExpressionParser(
		  @Nullable Map<String, Double> namespace
		) { this(namespace, null, null); }
		
		public UnicodeMathDoubleExpressionParser(
		  @Nullable Map<String, Double> namespace,
		  @Nullable Map<Entry<String, Integer>, ExpressionFunc<Double>> functions
		) { this(namespace, functions, null); }
		
		public UnicodeMathDoubleExpressionParser(
		  @Nullable Map<String, Double> namespace,
		  @Nullable Map<Entry<String, Integer>, ExpressionFunc<Double>> functions,
		  @Nullable OperatorHierarchy<Double> operators
		) {
			this(
			  namespace != null? FixedNamespaceSet.of(namespace) : null,
			  functions != null? new FixedNamespace<>(functions) : null, operators
			);
		}
		
		public UnicodeMathDoubleExpressionParser(
		  @Nullable FixedNamespaceSet<Double> namespaceSet
		) { this(namespaceSet, null, null); }
		
		public UnicodeMathDoubleExpressionParser(
         @Nullable FixedNamespaceSet<Double> namespaceSet,
         @Nullable FixedNamespace<Entry<String, Integer>, ExpressionFunc<Double>> functions
		) { this(namespaceSet, functions, null); }
		
		public UnicodeMathDoubleExpressionParser(
		  @Nullable FixedNamespaceSet<Double> namespaceSet,
		  @Nullable FixedNamespace<Entry<String, Integer>, ExpressionFunc<Double>> functions,
		  @Nullable OperatorHierarchy<Double> operators
		) {
			final String defaultNamespace = namespaceSet != null? namespaceSet.defaultNamespace : "";
			final Map<String, Map<String, Double>> mathNames = new HashMap<>();
			mathNames.put(defaultNamespace, UNICODE_MATH_NAMES);
			this.namespaceSet = namespaceSet != null? namespaceSet.copyWith(mathNames) :
			                    new FixedNamespaceSet<>(mathNames);
			this.functions = functions != null? functions.copyWith(UNICODE_MATH_FUNCTIONS) :
			                 new FixedNamespace<>(UNICODE_MATH_FUNCTIONS);
			this.operators = operators != null ? operators : UNICODE_MATH_OPERATOR_HIERARCHY;
		}
	}
	
	/**
	 * Does the parsing for {@link DoubleExpressionParser} to build a
	 * {@link ParsedExpression}{@code <Double>}
	 */
	public static class InternalDoubleExpressionParser extends InternalExpressionParser<Double> {
		/**
		 * To use default operators, functions or variables, see
		 * {@link DoubleExpressionParser}
		 */
		public InternalDoubleExpressionParser(
		  String expression,
		  FixedNamespaceSet<Double> namespace,
		  FixedNamespace<Entry<String, Integer>, ExpressionFunc<Double>> functions,
		  OperatorHierarchy<Double> operators,
		  ExpressionParser<Double> parser
		) { super(expression, namespace, functions, operators, parser); }
		
		public ExpressionNode<Double> parseNode() {
			ExpressionNode<Double> x;
			int p = pos;
			double[] pre = eatScript();
			int start = pos;
			String digit = eatNumber();
			if (digit != null) {
				final double x_val = parseDouble(digit, start);
				return decorate(() -> x_val, pre);
			} else {
				setCursor(p);
				return null;
			}
		}
	}
	
	// Casting functions
	private static boolean b(double d) {
		// Not NaN nor 0
		return !Double.isNaN(d) && round(d) != 0;
	}
	
	private static double d(boolean b) {
		// How is not this a standard?
		return b ? 1D : 0D;
	}
	
	private static double d(long l) {
		// Java compiler can be really smart sometimes
		return l;
	}
	
	private static long l(double d) {
		// No rounding
		return (long) d;
	}
	
	private static int i(double d) {
		// No rounding
		return (int) d;
	}
	
	public static boolean anyNaN(double[]... values) {
		for (double[] list : values) {
			for (double d : list) {
				if (Double.isNaN(d))
					return true;
			}
		}
		return false;
	}
	
	public static boolean allNaN(double[]... values) {
		for (double[] list : values) {
			for (double d : list) {
				if (!Double.isNaN(d))
					return false;
			}
		}
		return true;
	}
	
	public static boolean anyNaN(double... values) {
		for (double d : values) {
			if (Double.isNaN(d))
				return true;
		}
		return false;
	}
	
	public static boolean allNaN(double... values) {
		for (double d : values) {
			if (!Double.isNaN(d))
				return false;
		}
		return true;
	}
	
	@FunctionalInterface
	public interface Operator<T> {
		@SuppressWarnings("unchecked")
		ExpressionNode<T> apply(ExpressionNode<T>... inputs);
	}
	
	public interface UnaryOperator<T> extends Operator<T> {
		@SuppressWarnings("unchecked")
		@Override default ExpressionNode<T> apply(ExpressionNode<T>... inputs) { return apply(inputs[0]); }
		ExpressionNode<T> apply(ExpressionNode<T> a);
	}
	
	public interface BinaryOperator<T> extends Operator<T> {
		@SuppressWarnings("unchecked")
		@Override default ExpressionNode<T> apply(ExpressionNode<T>... inputs) { return apply(inputs[0], inputs[1]); }
		ExpressionNode<T> apply(ExpressionNode<T> a, ExpressionNode<T> b);
	}
	
	public interface TernaryOperator<T> extends Operator<T> {
		@SuppressWarnings("unchecked")
		@Override default ExpressionNode<T> apply(ExpressionNode<T>... inputs) { return apply(inputs[0], inputs[1], inputs[2]); }
		ExpressionNode<T> apply(ExpressionNode<T> a, ExpressionNode<T> b, ExpressionNode<T> c);
	}
	
	public interface OperatorFactory<T, O extends Operator<T>> extends Operator<T> {
		O get(double... args);
		@SuppressWarnings("unchecked")
		@Override default ExpressionNode<T> apply(ExpressionNode<T>... inputs) {
			throw new IllegalAccessError("Operator factory cannot be used as operator");
		}
	}
	
	public interface DecoratedOperator<T, O extends Operator<T>> extends OperatorFactory<T, O> {
		@Override default O get(double... args) { return get(args[0], args[1], args[2], args[3]); }
		
		O get(double a, double b, double c, double d);
		
		default O get(double[] pre, double[] pos) {
			if (pos == null)
				return get(pre[0], Double.NaN, pre[1], Double.NaN);
			return get(pre[0], pos[0], pre[1], pos[1]);
		}
		
		default O decorate(InternalExpressionParser<T> e, double[] pre) {
			int p = e.pos;
			double[] pos = e.eatScript();
			O op = get(pre, pos);
			if (op == null) {
				op = get(pre, null);
				e.setCursor(p);
			}
			if (op == null)
				throw e.scriptParseException(pre, pos);
			return op;
		}
	}
	
	public static abstract class OperatorParser<T, O extends Operator<T>> {
		protected InternalExpressionParser<T> e;
		
		public OperatorParser() {}
		public OperatorParser<T, O> init(InternalExpressionParser<T> e) {
			this.e = e;
			return this;
		}
		
		/**
		 * Parse an expression node from the expression using the given
		 * ordered map of operators<br>
		 * Implementations should call {@link InternalExpressionParser#decorate(Operator)}
		 * before applying any operator, or instead use the
		 * {@link OperatorParser#apply(Operator, ExpressionNode[])} and
		 * {@link OperatorParser#decorate(Operator, double[])} utility methods
		 */
		public abstract ExpressionNode<T> parse(LinkedHashMap<String, Operator<T>> operators);
		
		/**
		 * Utility for implementations<br>
		 * Assumes op is a DecoratedOperator, and returns its
		 * decorated result
		 * @param op A DecoratedOperator
		 * @return The decorated operator to be used
		 * @throws ClassCastException if op is not a {@link DecoratedOperator}
		 */
		protected final Operator<T> decorate(Operator<T> op, double[] pre) {
			return e.decorate((DecoratedOperator<T, ?>) op).decorate(e, pre);
		}
		
		/**
		 * Utility for implementations<br>
		 * Lets the expression parser decorate the operator before applying it.
		 */
		@SafeVarargs protected final ExpressionNode<T> apply(Operator<T> op, ExpressionNode<T>... inputs) {
			return e.decorate(op).apply(inputs);
		}
	}
	
	public static abstract class UnaryOperatorParser<T, O extends UnaryOperator<T>>
	  extends OperatorParser<T, O> {}
	
	public static class PrefixUnaryOperatorParser<T>
	  extends UnaryOperatorParser<T, UnaryOperator<T>> {
		
		@Override
		public ExpressionNode<T> parse(LinkedHashMap<String, Operator<T>> operators) {
			int p = e.pos;
			double[] pre = e.eatScript();
			for (Entry<String, Operator<T>> pair : operators.entrySet()) {
				Operator<T> op = pair.getValue();
				if (e.eatString(pair.getKey())) {
					if (op instanceof DecoratedOperator)
						op = decorate(op, pre);
					if (!(op instanceof UnaryOperator))
						throw new IllegalArgumentException(
						  "Used non-unary operator on prefix unary operator builder'");
					return apply(op, e.next());
				}
			}
			e.setCursor(p);
			return e.next();
		}
	}
	
	public static class PostfixUnaryOperatorParser<T>
	  extends UnaryOperatorParser<T, UnaryOperator<T>> {
		
		@Override public ExpressionNode<T> parse(LinkedHashMap<String, Operator<T>> operators) {
			ExpressionNode<T> x = e.next();
			int p = e.pos;
			double[] pre = e.eatScript();
			for (Entry<String, Operator<T>> pair : operators.entrySet()) {
				Operator<T> op = pair.getValue();
				if (e.eatString(pair.getKey())) {
					if (op instanceof DecoratedOperator)
						op = decorate(op, pre);
					if (!(op instanceof UnaryOperator))
						throw new IllegalArgumentException(
						  "Used non-unary operator on postfix unary operator builder'");
					return apply(op, x);
				}
			}
			e.setCursor(p);
			return x;
		}
	}
	
	public static class SurroundingUnaryOperatorParser<T>
	  extends UnaryOperatorParser<T, UnaryOperator<T>> {
		
		@Override public ExpressionNode<T> parse(LinkedHashMap<String, Operator<T>> operators) {
			int start = e.pos;
			double[] pre = e.eatScript();
			for (Entry<String, Operator<T>> pair : operators.entrySet()) {
				String[] split = pair.getKey().split(" ");
				if (e.eatString(split[0])) {
					ExpressionNode<T> x = e.first();
					Operator<T> op;
					if (!e.eatString(split[1])) {
						match:
						{
							for (Entry<String, Operator<T>> entry : operators.entrySet()) {
								final String[] sp = entry.getKey().split(" ");
								if (split[0].equals(sp[0]) && e.eatString(sp[1])) {
									pair = entry;
									break match;
								}
							}
							throw e.parseException(
							  "Missing closing right pair for surrounding unary operator: '"
							  + split[0] + "', expected '" + split[1] + "'");
						}
					}
					op = pair.getValue();
					if (op instanceof DecoratedOperator)
						return apply(decorate(op, pre), x);
					if (!(op instanceof UnaryOperator))
						throw new IllegalArgumentException(
						  "Tried to use non-unary operator with SurroundingUnaryOperatorParser");
					return e.decorate(apply(op, x), pre);
				}
			}
			e.setCursor(start);
			return e.next();
		}
	}
	
	public static abstract class BinaryOperatorParser<T, O extends BinaryOperator<T>>
	  extends OperatorParser<T, O> {}
	
	/**
	 * Joins operators with left associativity
	 *
	 * @see RightAssociativeBinaryOperatorParser
	 */
	public static class LeftAssociativeBinaryOperatorParser<T>
	  extends BinaryOperatorParser<T, BinaryOperator<T>> {
		
		@Override public ExpressionNode<T> parse(LinkedHashMap<String, Operator<T>> operators) {
			ExpressionNode<T> x = e.next();
			while (true) {
				find:
				{
					int p = e.pos;
					double[] pre = e.eatScript();
					for (Entry<String, Operator<T>> pair : operators.entrySet()) {
						Operator<T> op = pair.getValue();
						if (e.eatString(pair.getKey())) {
							if (op instanceof DecoratedOperator) {
								op = decorate(op, pre);
							} else if (!allNaN(pre))
								throw e.scriptParseException(pre);
							if (!(op instanceof BinaryOperator))
								throw new IllegalArgumentException(
								  "Used non-binary operator with binary operator builder");
							ExpressionNode<T> a = x, b = e.next();
							x = apply(op, a, b);
							break find;
						}
					}
					e.setCursor(p);
					return x;
				}
			}
		}
	}
	
	/**
	 * Joins operators with right associativity
	 *
	 * @see LeftAssociativeBinaryOperatorParser
	 */
	public static class RightAssociativeBinaryOperatorParser<T>
	  extends BinaryOperatorParser<T, BinaryOperator<T>> {
		
		@Override public ExpressionNode<T> parse(LinkedHashMap<String, Operator<T>> operators) {
			ExpressionNode<T> x;
			Stack<ExpressionNode<T>> stack = new Stack<>();
			Stack<BinaryOperator<T>> ops = new Stack<>();
			stack.add(e.next());
			while (true) {
				find:
				{
					int p = e.pos;
					double[] pre = e.eatScript();
					for (Entry<String, Operator<T>> pair : operators.entrySet()) {
						Operator<T> op = pair.getValue();
						if (e.eatString(pair.getKey())) {
							if (op instanceof DecoratedOperator) {
								op = decorate(op, pre);
							} else if (!allNaN(pre))
								throw e.scriptParseException(pre);
							if (!(op instanceof BinaryOperator))
								throw new IllegalArgumentException(
								  "Used non-binary operator with binary operator builder");
							ops.add((BinaryOperator<T>) op);
							stack.add(e.next());
							break find;
						}
					}
					e.setCursor(p);
					if (stack.size() == 1)
						return stack.pop();
					x = stack.pop();
					while (!stack.empty()) {
						assert !ops.empty();
						x = apply(ops.pop(), stack.pop(), x);
					}
					assert ops.empty();
					return x;
				}
			}
		}
	}
	
	/**
	 * Joins operators of the same group using the provided joiner operator,
	 * from left to right, instead of associating them
	 */
	public static class LeftJoinedBinaryOperatorParser<T>
	  extends BinaryOperatorParser<T, BinaryOperator<T>> {
		final protected BinaryOperator<T> joiner;
		
		public LeftJoinedBinaryOperatorParser(BinaryOperator<T> joiner) {
			this.joiner = joiner;
		}
		
		@Override
		public ExpressionNode<T> parse(LinkedHashMap<String, Operator<T>> operators) {
			ExpressionNode<T> x = null, a = e.next(), b, c;
			while (true) {
				find:
				{
					int p = e.pos;
					double[] pre = e.eatScript();
					for (Entry<String, Operator<T>> pair : operators.entrySet()) {
						Operator<T> op = pair.getValue();
						if (e.eatString(pair.getKey())) {
							if (op instanceof DecoratedOperator) {
								op = decorate(op, pre);
							} else if (!allNaN(pre))
								throw e.scriptParseException(pre);
							if (!(op instanceof BinaryOperator))
								throw new IllegalArgumentException(
								  "Used non-binary operator with comparator (binary) operator builder");
							b = e.next();
							c = apply(op, a, b);
							a = b;
							x = x != null ? apply(joiner, x, c) : c;
							break find;
						}
					}
					e.setCursor(p);
					return x != null ? x : a;
				}
			}
		}
	}
	
	public static abstract class TernaryOperatorParser<T> extends OperatorParser<T,
	  TernaryOperator<T>> {}
	
	/**
	 * Supports {@link DecoratedOperator}s on the left operator part
	 */
	public static class RightAssociativeTernaryOperatorParser<T> extends TernaryOperatorParser<T> {
		@Override
		public ExpressionNode<T> parse(LinkedHashMap<String, Operator<T>> operators) {
			ExpressionNode<T> x = e.next();
			while (true) {
				find:
				{
					int p = e.pos;
					double[] pre = e.eatScript();
					for (Entry<String, Operator<T>> pair : operators.entrySet()) {
						String[] split = pair.getKey().split(" ");
						if (split.length != 2)
							throw new IllegalArgumentException(
							  "Ternary operator with " + split.length + " parts, must be 2");
						Operator<T> op = pair.getValue();
						if (split[0].equals(split[1]))
							throw new IllegalArgumentException(
							  "Ternary operator pairs must be different, used '" + e.displayName(split[0])
							  + "' for both");
						if (e.eatString(split[0])) {
							if (op instanceof DecoratedOperator)
								op = decorate(op, pre);
							if (!(op instanceof TernaryOperator))
								throw new IllegalArgumentException(
								  "Used non-ternary operator with ternary operator builder");
							ExpressionNode<T> a = x, b = parse(operators);
							if (!e.eatString(split[1]))
								throw e.parseException(
								  "Missing right pair for ternary operator: '" + e.displayName(split[0])
								  + "', expected: '" + e.displayName(split[1]) + "'");
							ExpressionNode<T> c = parse(operators);
							x = apply(op, a, b, c);
							break find;
						}
					}
					e.setCursor(p);
					return x;
				}
			}
		}
	}
	
	/**
	 * Function suitable to be called from parsed {@link InternalDoubleExpressionParser}s
	 *
	 * @see Func0
	 * @see Func1
	 * @see Func2
	 * @see Func3
	 * @see Func4
	 * @see Func5
	 */
	public interface ExpressionFunc<T> {
		int getNumberOfArguments();
		
		@SuppressWarnings("unchecked")
		T eval(T... inputs);
	}
	
	/**
	 * {@link ExpressionFunc} with 0 parameters
	 */
	public interface Func0<T> extends ExpressionFunc<T> {
		@Override
		default int getNumberOfArguments() { return 0; }
		
		@SuppressWarnings("unchecked")
		@Override
		default T eval(T... inputs) { return eval(); }
		
		T eval();
	}
	
	/**
	 * {@link ExpressionFunc} with 1 parameter
	 */
	public interface Func1<T> extends ExpressionFunc<T> {
		@Override
		default int getNumberOfArguments() { return 1; }
		
		@SuppressWarnings("unchecked")
		@Override
		default T eval(T... inputs) { return eval(inputs[0]); }
		
		T eval(T arg);
	}
	
	/**
	 * {@link ExpressionFunc} with 2 parameters
	 */
	public interface Func2<T> extends ExpressionFunc<T> {
		@Override
		default int getNumberOfArguments() { return 2; }
		
		@SuppressWarnings("unchecked")
		@Override
		default T eval(T... inputs) { return eval(inputs[0], inputs[1]); }
		
		T eval(T arg1, T arg2);
	}
	
	/**
	 * {@link ExpressionFunc} with 3 parameters
	 */
	public interface Func3<T> extends ExpressionFunc<T> {
		@Override
		default int getNumberOfArguments() { return 3; }
		
		@SuppressWarnings("unchecked")
		@Override
		default T eval(T... inputs) { return eval(inputs[0], inputs[1], inputs[2]); }
		
		T eval(T arg1, T arg2, T arg3);
	}
	
	/**
	 * {@link ExpressionFunc} with 4 parameters
	 */
	public interface Func4<T> extends ExpressionFunc<T> {
		@Override
		default int getNumberOfArguments() { return 4; }
		
		@SuppressWarnings("unchecked")
		@Override
		default T eval(T... inputs) { return eval(inputs[0], inputs[1], inputs[2], inputs[3]); }
		
		T eval(T arg1, T arg2, T arg3, T arg4);
	}
	
	/**
	 * {@link ExpressionFunc} with 5 parameters
	 */
	public interface Func5<T> extends ExpressionFunc<T> {
		@Override
		default int getNumberOfArguments() { return 5; }
		
		@SuppressWarnings("unchecked")
		@Override
		default T eval(T... inputs) {
			return eval(inputs[0], inputs[1], inputs[2], inputs[3], inputs[4]);
		}
		
		T eval(T arg1, T arg2, T arg3, T arg4, T arg5);
	}
	
	/**
	 * Fixed key set map<br>
	 * Throws {@link KeyNotDefinedException} when trying to access a
	 * non defined key
	 * @param <V> Value type
	 */
	public static class FixedNamespace<K, V> {
		private final Map<K, V> values;
		private final Map<K, V> initial;
		
		public FixedNamespace() {
			this(new HashMap<>());
		}
		
		public FixedNamespace(@Nonnull Map<K, V> namespace) {
			this.initial = new HashMap<>(namespace);
			this.values = new HashMap<>(namespace);
		}
		
		public FixedNamespace(@Nonnull FixedNamespace<K, V> source) {
			this.values = new HashMap<>(source.values);
			this.initial = new HashMap<>(source.values);
		}
		
		/**
		 * Set values from map<br>
		 * Keys not defined in this namespace will throw a {@link KeyNotDefinedException}
		 * @see FixedNamespace#setAll(Map)
		 * @see FixedNamespace#putAll(FixedNamespace)
		 */
		public void putAll(Map<K, V> source) {
			for (K key : source.keySet()) {
				if (!initial.containsKey(key))
					throw new KeyNotDefinedException(key);
				values.put(key, source.get(key));
			}
		}
		
		/**
		 * Set values from another namespace<br>
		 * Keys not defined in this namespace will throw a {@link KeyNotDefinedException}
		 * @see FixedNamespace#setAll(FixedNamespace)
		 * @see FixedNamespace#putAll(Map)
		 */
		public void putAll(FixedNamespace<K, V> source) {
			for (K key : source.initial.keySet()) {
				if (!initial.containsKey(key))
					throw new KeyNotDefinedException(key);
				values.put(key, source.get(key));
			}
		}
		
		/**
		 * Set values from map<br>
		 * Keys not defined in the namespace will be ignored
		 * @see FixedNamespace#putAll(Map)
		 * @see FixedNamespace#setAll(FixedNamespace)
		 */
		public void setAll(Map<K, V> source) {
			for (K key : this.initial.keySet()) {
				if (source.containsKey(key))
					values.put(key, source.get(key));
			}
		}
		
		/**
		 * Set values from another namespace<br>
		 * Keys not defined in this namespace will be ignored
		 * @see FixedNamespace#putAll(FixedNamespace)
		 * @see FixedNamespace#setAll(Map)
		 */
		public void setAll(FixedNamespace<K, V> source) {
			for (K key : this.initial.keySet()) {
				if (source.contains(key))
					this.values.put(key, source.get(key));
			}
		}
		
		public void set(K key, V value) {
			if (!initial.containsKey(key))
				throw new KeyNotDefinedException(key);
			values.put(key, value);
		}
		
		public V get(K key) {
			if (!initial.containsKey(key))
				throw new KeyNotDefinedException(key);
			return values.get(key);
		}
		
		public boolean contains(K key) {
			return initial.containsKey(key);
		}
		
		public boolean containsValue(V value) {
			return values.containsValue(value);
		}
		
		/**
		 * Reset to initial values
		 */
		public void reset() {
			values.putAll(initial);
		}
		
		public Set<K> keySet() {
			return initial.keySet();
		}
		
		public Collection<V> values() {
			return values.values();
		}
		
		public FixedNamespace<K, V> copy() {
			return new FixedNamespace<>(this);
		}
		
		/**
		 * Create a copy with extra initial values<br>
		 * Already existing values have priority
		 */
		public FixedNamespace<K, V> copyWith(Map<K, V> extra) {
			final Map<K, V> initial = new HashMap<>(extra);
			initial.putAll(this.initial);
			final FixedNamespace<K, V> namespace = new FixedNamespace<>(initial);
			namespace.setAll(values);
			return namespace;
		}
	}
	
	/**
	 * Fixed map of maps, each with fixed key sets.<br>
	 * Trying to access a non defined map or key will result in
	 * a {@link NameNotDefinedException} thrown<br>
	 *
	 * To create a FixedNamespaceSet with a single namespace from
	 * a simple {@code Map<String, V>}, use the static {@link FixedNamespaceSet#of} methods
	 * @param <V> Value type
	 */
	public static class FixedNamespaceSet<V> {
		private final Map<String, Map<String, V>> values;
		private final Map<String, Map<String, V>> initial;
		private final Map<String, Pair<String, String>> shortcuts;
		public final String separator;
		public final String defaultNamespace;
		
		public static <V> FixedNamespaceSet<V> of(
		  @Nonnull Set<String> namespace
		) { return of(nullMap(namespace)); }
		
		public static <V> FixedNamespaceSet<V> of (
		  @Nonnull Set<String> namespace, @Nonnull String defaultNamespace
		) { return of(nullMap(namespace), defaultNamespace); }
		
		public static <V> FixedNamespaceSet<V> of(
		  @Nonnull Set<String> namespace, @Nonnull String defaultNamespace, @Nonnull String separator
		) { return of(nullMap(namespace), defaultNamespace, separator); }
		
		public static <V> FixedNamespaceSet<V> of(
         @Nonnull Map<String, V> namespace
		) { return of(namespace, "", ":"); }
		
		public static <V> FixedNamespaceSet<V> of(
         @Nonnull Map<String, V> namespace, @Nonnull String defaultNamespace
		) {return of(namespace, defaultNamespace, ":");}
		
		/**
		 * Names in the namespace are parsed for separators
		 */
		public static <V> FixedNamespaceSet<V> of(
		  @Nonnull Map<String, V> namespace, @Nonnull String defaultNamespace, @Nonnull String separator
		) {
			final Map<String, Map<String, V>> set = new HashMap<>();
			String ns, nm;
			for (String name : namespace.keySet()) {
				if (name.contains(separator)) {
					final int i = name.indexOf(separator);
					ns = name.substring(0, i);
					nm = name.substring(i + separator.length());
				} else {
					ns = defaultNamespace;
					nm = name;
				}
				set.computeIfAbsent(ns, s -> new HashMap<>()).put(nm, namespace.get(name));
			}
			return new FixedNamespaceSet<>(set, defaultNamespace, separator);
		}
		
		public FixedNamespaceSet() {
			this(new HashMap<>(), "", ":");
		}
		
		public FixedNamespaceSet(Map<String, Map<String, V>> namespaces) {
			this(namespaces, "", ":");
		}
		
		public FixedNamespaceSet(Map<String, Map<String, V>> namespaces, String defaultNamespace) {
			this(namespaces, defaultNamespace, ":");
		}
		
		public FixedNamespaceSet(Map<String, Map<String, V>> namespaces, String defaultNamespace, String separator) {
			values = new HashMap<>();
			initial = new HashMap<>();
			for (String namespace : namespaces.keySet()) {
				values.put(namespace, new HashMap<>(namespaces.get(namespace)));
				initial.put(namespace, new HashMap<>(namespaces.get(namespace)));
			}
			this.separator = separator;
			this.defaultNamespace = defaultNamespace;
			
			// Compute shortcuts
			final Map<String, Integer> nameCounts = new HashMap<>();
			final Map<String, String> last = new HashMap<>();
			for (String namespace : initial.keySet()) {
				for (String name : initial.get(namespace).keySet()) {
					nameCounts.put(name, nameCounts.getOrDefault(name, 0) + 1);
					if (!name.equals(defaultNamespace))
						last.put(name, namespace);
				}
			}
			shortcuts = new HashMap<>();
			for (Entry<String, Integer> entry : nameCounts.entrySet()) {
				final String key = entry.getKey();
				if (entry.getValue() == 1 && last.containsKey(key))
					shortcuts.put(key, Pair.of(last.get(key), key));
			}
		}
		
		public FixedNamespaceSet(
         FixedNamespaceSet<V> source
		) { this(source, source.defaultNamespace, source.separator); }
		
		public FixedNamespaceSet(
         FixedNamespaceSet<V> source, String defaultNamespace
		) { this(source, defaultNamespace, source.separator); }
		
		public FixedNamespaceSet(
		  FixedNamespaceSet<V> source, String defaultNamespace, String separator
		) {
			this(source.initial, defaultNamespace, separator);
			this.values.putAll(source.values);
		}
		
		/**
		 * Put values from a map of maps<br>
		 * Values not defined in this namespace will throw a {@link NameNotDefinedException}
		 * @see FixedNamespaceSet#setAll(Map)
		 * @see FixedNamespaceSet#putAll(FixedNamespaceSet)
		 * @see FixedNamespaceSet#putFrom(Map)
		 */
		public void putAll(Map<String, Map<String, V>> source) {
			for (String namespace : source.keySet()) {
				final Map<String, V> space = getNamespace(namespace);
				for (Entry<String, V> entry : source.get(namespace).entrySet()) {
					if (!space.containsKey(entry.getKey()))
						throw new NameNotDefinedException(namespace + separator + entry.getKey());
					space.put(entry.getKey(), entry.getValue());
				}
			}
		}
		
		/**
		 * Put values from a map<br>
		 * Names in the map are parsed for separators<br>
		 * Values not defined in this namespace will throw a {@link NameNotDefinedException}
		 * @see FixedNamespaceSet#putAll(Map)
		 * @see FixedNamespaceSet#putAll(FixedNamespaceSet)
		 * @see FixedNamespaceSet#setFrom(Map)
		 */
		public void putFrom(Map<String, V> source) {
			for (String name : source.keySet()) {
				final Pair<String, String> split = split(name);
				if (!containsName(split.getLeft(), split.getRight()))
					throw new NameNotDefinedException(name);
				set(split.getLeft(), split.getRight(), source.get(name));
			}
		}
		
		/**
		 * Put values from another namespace set<br>
		 * Names not defined in this namespace will throw a {@link NameNotDefinedException}
		 * @see FixedNamespaceSet#putAll(Map)
		 * @see FixedNamespaceSet#putFrom(Map)
		 * @see FixedNamespaceSet#setAll(FixedNamespaceSet)
		 */
		public void putAll(FixedNamespaceSet<V> source) {
			for (String namespace : source.initial.keySet()) {
				final Map<String, V> space = getNamespace(namespace);
				final Map<String, V> src = source.values.get(namespace);
				for (String name : src.keySet()) {
					if (!space.containsKey(name))
						throw new NameNotDefinedException(namespace + separator + name);
					space.put(name, src.get(name));
				}
			}
		}
		
		/**
		 * Set values from a map of maps<br>
		 * Values not defined in this namespace set will be ignored
		 * @see FixedNamespaceSet#setAll(FixedNamespaceSet)
		 * @see FixedNamespaceSet#putAll(Map)
		 * @see FixedNamespaceSet#setFrom(Map)
		 */
		public void setAll(Map<String, Map<String, V>> source) {
			for (String namespace : values.keySet()) {
				if (source.containsKey(namespace)) {
					final Map<String, V> dest = values.get(namespace);
					final Map<String, V> space = source.get(namespace);
					for (String name : dest.keySet()) {
						if (space.containsKey(name))
							dest.put(name, space.get(name));
					}
				}
			}
		}
		
		/**
		 * Set values from a map<br>
		 * Values not defined in this namespace set will be ignored<br>
		 * The names in the map are parsed for separators
		 * @see FixedNamespaceSet#setAll(Map)
		 * @see FixedNamespaceSet#setAll(FixedNamespaceSet)
		 * @see FixedNamespaceSet#putFrom(Map)
		 */
		public void setFrom(Map<String, V> source) {
			for (String name : source.keySet()) {
				final Pair<String, String> split = split(name);
				if (containsName(split.getLeft(), split.getRight())) {
					set(split.getLeft(), split.getRight(), source.get(name));
				}
			}
		}
		
		/**
		 * Set values from another namespace set<br>
		 * Values not defined in this namespace set will be ignored
		 * @see FixedNamespaceSet#setAll(Map)
		 * @see FixedNamespaceSet#setFrom(Map)
		 * @see FixedNamespaceSet#putAll(FixedNamespaceSet)
		 */
		public void setAll(FixedNamespaceSet<V> source) {
			for (String namespace : values.keySet()) {
				if (source.values.containsKey(namespace)) {
					final Map<String, V> dest = values.get(namespace);
					final Map<String, V> space = source.values.get(namespace);
					for (String name : dest.keySet()) {
						if (space.containsKey(name))
							dest.put(name, space.get(name));
					}
				}
			}
		}
		
		public void set(String namespace, String name, V value) {
			final Map<String, V> space = getNamespace(namespace);
			if (!space.containsKey(name))
				throw new NameNotDefinedException(name);
			space.put(name, value);
		}
		
		public void set(String name, V value) {
			final Pair<String, String> split = split(name);
			set(split.getLeft(), split.getRight(), value);
		}
		
		public V get(String namespace, String name) {
			final Map<String, V> space = getNamespace(namespace);
			if (!space.containsKey(name))
				throw new NameNotDefinedException(name);
			return space.get(name);
		}
		
		public V get(String name) {
			final Pair<String, String> split = split(name);
			return get(split.getLeft(), split.getRight());
		}
		
		public boolean containsNamespace(String namespace) {
			return values.containsKey(namespace);
		}
		
		public boolean containsName(String namespace, String name) {
			return containsNamespace(namespace) && getNamespace(namespace).containsKey(name);
		}
		
		public boolean containsName(String name) {
			final Pair<String, String> split = split(name);
			return containsName(split.getLeft(), split.getRight());
		}
		
		public boolean containsValue(V value) {
			for (Map<String, V> space : values.values()) {
				if (space.containsValue(value))
					return true;
			}
			return false;
		}
		
		public boolean containsValue(String namespace, V value) {
			return getNamespace(namespace).containsValue(value);
		}
		
		/**
		 * Splits a name according to the {@link FixedNamespaceSet#separator}
		 * and {@link FixedNamespaceSet#defaultNamespace}
		 * @return Pair.of(namespace, name)
		 */
		public Pair<String, String> split(String name) {
			if (name.contains(separator)) {
				final int i = name.indexOf(separator);
				return Pair.of(name.substring(0, i), name.substring(i + separator.length()));
			} else if (shortcuts.containsKey(name))
				return shortcuts.get(name);
			else return Pair.of(defaultNamespace, name);
		}
		
		public Set<String> namespaceSet() {
			return values.keySet();
		}
		
		public Set<String> nameSet(String namespace) {
			return getNamespace(namespace).keySet();
		}
		
		public Collection<V> values(String namespace) {
			return getNamespace(namespace).values();
		}
		
		public Collection<V> values() {
			Collection<V> c = new ArrayList<>();
			for (String namespace : values.keySet())
				c.addAll(values.get(namespace).values());
			return c;
		}
		
		/**
		 * Resets all values from a namespace to the initial ones
		 */
		public void reset(String namespace) {
			getNamespace(namespace).putAll(initial.get(namespace));
		}
		
		/**
		 * Resets all values to the initial ones
		 */
		public void reset() {
			for (String namespace : initial.keySet())
				values.get(namespace).putAll(initial.get(namespace));
		}
		
		public FixedNamespaceSet<V> copy() {
			return new FixedNamespaceSet<>(this);
		}
		
		/**
		 * Create a copy with extra initial values<br>
		 * Already existing values have priority
		 */
		public FixedNamespaceSet<V> copyWith(Map<String, Map<String, V>> extra) {
			final Map<String, Map<String, V>> initial = new HashMap<>();
			for (String namespace : extra.keySet())
				initial.put(namespace, new HashMap<>(extra.get(namespace)));
			for (String namespace : this.initial.keySet())
				initial.put(namespace, new HashMap<>(this.initial.get(namespace)));
			final FixedNamespaceSet<V> set = new FixedNamespaceSet<>(initial, defaultNamespace,
			                                                         separator
			);
			set.setAll(values);
			return set;
		}
		
		public Map<String, Pair<String, String>> getShortcuts() {
			return Collections.unmodifiableMap(shortcuts);
		}
		
		private Map<String, V> getNamespace(String namespace) {
			if (!values.containsKey(namespace))
				throw new NameNotDefinedException(namespace);
			return values.get(namespace);
		}
	}
	
	public static class KeyNotDefinedException extends RuntimeException {
		public KeyNotDefinedException(Object key) {
			super("The key \"" + key + "\" is not defined");
		}
	}
	
	public static class NameNotDefinedException extends RuntimeException {
		public NameNotDefinedException(String name) {
			super("The name \"" + name + "\" is not defined");
		}
	}
	
	/**
	 * Ordered list of operator groups, each associated with an
	 * {@link OperatorParser} which handles the way they're read
	 * from an expression and associated within the group.<br>
	 * <p>
	 * Supports indexed access, and not much more.
	 */
	public static class OperatorHierarchy<T> {
		private final List<Entry<OperatorParser<T, ?>,
		  LinkedHashMap<String, Operator<T>>>> list;
		private final DecoratedOperator<T, ? extends UnaryOperator<T>> decoratorFactory;
		private final Set<Character> operatorCharacters;
		
		public OperatorHierarchy(
		  List<Entry<OperatorParser<T, ?>, LinkedHashMap<String, Operator<T>>>> operators,
		  DecoratedOperator<T, ? extends UnaryOperator<T>> decoratorFactory
		) {
			this.list = operators;
			this.decoratorFactory = decoratorFactory;
			this.operatorCharacters = new HashSet<>();
			for (Entry<OperatorParser<T, ?>, LinkedHashMap<String, Operator<T>>> entry : operators) {
				for (String str : entry.getValue().keySet()) {
					str = str.replaceAll("\\s", "");
					for (int i = 0; i < str.length(); i++)
						operatorCharacters.add(str.charAt(i));
				}
			}
		}
		
		public Entry<OperatorParser<T, ?>, LinkedHashMap<String, Operator<T>>>
		get(int i) {
			return list.get(i);
		}
		
		public int size() {
			return list.size();
		}
		
		public DecoratedOperator<T, ? extends UnaryOperator<T>> getDecoratorFactory() {
			return decoratorFactory;
		}
		
		public boolean use(char ch) {
			return operatorCharacters.contains(ch);
		}
		
		public static class Builder<T> {
			private final List<Entry<OperatorParser<T, ?>, LinkedHashMap<String, Operator<T>>>> list;
			private DecoratedOperator<T, UnaryOperator<T>> decoratorFactory = null;
			
			private int caret = -1;
			
			public Builder() { this.list = new ArrayList<>(); }
			
			public DecoratedOperator<T, UnaryOperator<T>> getDecoratorFactory() {
				return decoratorFactory;
			}
			
			public void setDecoratorFactory(DecoratedOperator<T, UnaryOperator<T>> decorator) {
				this.decoratorFactory = decorator;
			}
			
			public <O extends Operator<T>> GroupBuilder<T, O> addGroup(
			  OperatorParser<T, O> parserFactory
			) {
				Entry<OperatorParser<T, O>, LinkedHashMap<String, Operator<T>>> p = Pair.of(
				  parserFactory, new LinkedHashMap<>());
				//noinspection unchecked
				list.add(
				  (Entry<OperatorParser<T, ?>, LinkedHashMap<String, Operator<T>>>)
				    (Entry<?, ?>) p);
				caret = list.size() - 1;
				return new GroupBuilder<>(p);
			}
			
			public <O extends Operator<T>> GroupBuilder<T, O> insertGroup(
			  int i, OperatorParser<T, O> parserFactory, Class<O> cls
			) {
				Entry<OperatorParser<T, O>, LinkedHashMap<String, Operator<T>>> p = Pair.of(
				  parserFactory, new LinkedHashMap<>());
				//noinspection unchecked
				list.add(
				  i, (Entry<OperatorParser<T, ?>, LinkedHashMap<String, Operator<T>>>) (Entry<?, ?>) p);
				caret = i;
				return new GroupBuilder<>(p);
			}
			
			public OperatorHierarchy<T> build() {
				return new OperatorHierarchy<>(list, decoratorFactory);
			}
			
			public static class GroupBuilder<T, O extends Operator<T>> {
				private final Entry<OperatorParser<T, O>, LinkedHashMap<String, Operator<T>>> entry;
				
				public GroupBuilder(
				  Entry<OperatorParser<T, O>, LinkedHashMap<String, Operator<T>>> entry
				) { this.entry = entry; }
				
				public GroupBuilder<T, O> add(String name, O op, String... aliases) {
					final LinkedHashMap<String, Operator<T>> map = entry.getValue();
					map.put(name, op);
					for (String alias : aliases)
						map.put(alias, op);
					return this;
				}
				
				/**
				 * Convenience
				 */
				public GroupBuilder<T, O> dec(
				  String name, DecoratedOperator<T, O> op, String... aliases
				) {
					final LinkedHashMap<String, Operator<T>> map = entry.getValue();
					map.put(name, op);
					for (String alias : aliases)
						map.put(alias, op);
					return this;
				}
			}
		}
		
		public List<Entry<OperatorParser<T, ?>, LinkedHashMap<String, Operator<T>>>> copyList() {
			return new ArrayList<>(this.list);
		}
	}
	
	/**
	 * Because generating maps with an inferred second key is a pain
	 */
	public static class ExpressionFuncMapBuilder<T> {
		private Map<Entry<String, Integer>, ExpressionFunc<T>> map;
		private Map<Entry<String, Integer>, ExpressionFunc<T>> lastMap = null;
		
		public ExpressionFuncMapBuilder() {
			map = new HashMap<>();
		}
		
		public ExpressionFuncMapBuilder<T> add(String name, ExpressionFunc<T> func) {
			if (map == null)
				map = new HashMap<>(lastMap);
			map.put(Pair.of(name, func.getNumberOfArguments()), func);
			return this;
		}
		
		// Ease of casting
		public ExpressionFuncMapBuilder<T> add(String name, Func0<T> func) {
			return add(name, (ExpressionFunc<T>) func);
		}
		
		public ExpressionFuncMapBuilder<T> add(String name, Func1<T> func) {
			return add(name, (ExpressionFunc<T>) func);
		}
		
		public ExpressionFuncMapBuilder<T> add(String name, Func2<T> func) {
			return add(name, (ExpressionFunc<T>) func);
		}
		
		public ExpressionFuncMapBuilder<T> add(String name, Func3<T> func) {
			return add(name, (ExpressionFunc<T>) func);
		}
		
		public ExpressionFuncMapBuilder<T> add(String name, Func4<T> func) {
			return add(name, (ExpressionFunc<T>) func);
		}
		
		public ExpressionFuncMapBuilder<T> add(String name, Func5<T> func) {
			return add(name, (ExpressionFunc<T>) func);
		}
		
		public Map<Entry<String, Integer>, ExpressionFunc<T>> build() {
			lastMap = map;
			map = null;
			return lastMap;
		}
	}
	
	/**
	 * Pure vanity, using {@link Map#put} is inelegant
	 */
	public static class ExpressionNamespaceBuilder<T> {
		private Map<String, T> map = new HashMap<>();
		
		public ExpressionNamespaceBuilder<T> add(String name, T value) {
			if (map == null)
				throw new IllegalStateException("Cannot reuse built namespace builder");
			map.put(name, value);
			return this;
		}
		
		public Map<String, T> build() {
			Map<String, T> ret = map;
			map = null;
			return ret;
		}
	}
}
