package endorh.lazulib.math;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import endorh.lazulib.math.MathParser.*;
import endorh.lazulib.math.MathParser.ExpressionParser.ParseException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static java.lang.Math.*;
import static org.junit.jupiter.api.Assertions.*;

class DoubleExpressionTest {
	DoubleExpressionParser p = new UnicodeMathDoubleExpressionParser();
	DoubleExpressionParser p_x = new UnicodeMathDoubleExpressionParser("x");
	DoubleExpressionParser p_x_y = new UnicodeMathDoubleExpressionParser("x", "y");
	
	private static void assertThrowsAndPrintStackTrace(
	  @SuppressWarnings("SameParameterValue") Class<? extends Exception> cls, Executable r
	) {
		Throwable t = assertThrows(cls, r);
		System.err.println("Successful test exception:");
		t.printStackTrace();
		System.err.println();
	}
	
	@Test void basicArithmetic() throws ParseException {
		assertEquals(4, p.parse("2+2").eval());
	}
	
	@Test void whitespace() throws ParseException {
		assertEquals(4, p.parse(" 2 + 2 ").eval());
	}
	
	@Test void basicComparison() throws ParseException {
		assertEquals(1D, p.parse("0 == -0").eval());
	}
	
	@Test void unicodeOperator() {
		ParsedExpression<Double> e = p_x_y.parse("x⊻y");
		for (double x = -100; x < 100; x+=1D) {
			for (double y = -100; y < 100; y+=1D) {
				assertEquals(((round(x) == 0) == (round(y) != 0)) ? 1D : 0D, e.eval(ImmutableMap.of(
				  "x", x, "y", y
				)));
			}
		}
	}
	
	@Test void tryParse() {
		ParsedExpression<Double> e = p_x.tryParse("x+");
		assertNull(e);
		e = p_x.tryParse("x + x");
		assertNotNull(e);
		assertEquals(2D + 2D, e.eval(ImmutableMap.of("x", 2D)));
	}
	
	@Test void textOperator() {
		ParsedExpression<Double> e = p_x_y.parse("x xor\ty");
		Map<String, Double> namespace = new HashMap<>();
		for (double x = -100; x < 100; x+=1D) {
			for (double y = -100; y < 100; y+=1D) {
				namespace.put("x", x);
				namespace.put("y", y);
				assertEquals(((round(x) == 0) == (round(y) != 0)) ? 1D : 0D, e.eval(namespace));
			}
		}
		
		e = p_x_y.parse("x if y % 3 == 0 else -x");
		for (double x = -100; x < 100; x+=1D) {
			for (double y = -100; y < 100; y+=1D) {
				namespace.put("x", x);
				namespace.put("y", y);
				assertEquals(y % 3 == 0? x : -x, e.eval(namespace));
			}
		}
	}
	
	@Test void unicodeNames() {
		assertEquals(2 * Math.PI, p.parse("π * 2").eval());
	}
	
	@Test void operatorAssociativity() throws ParseException {
		assertEquals(2,    p.parse("4 - 1 - 1").eval());       // Left
		assertEquals(256,  p.parse("2^2^3").eval());           // Right
		assertEquals(0.4D, p.parse("20/10/5").eval());         // Left
		assertEquals(0,    p.parse("20//10//5").eval());       // Left
		assertEquals(2,    p.parse("22 % 5 % 3").eval());      // Left
		assertEquals(0,    p.parse("1? 0 : 1? 2 : 3").eval()); // Right
	}
	
	@Test void operatorPrecedence() throws ParseException {
		ParsedExpression<Double> e = p.parse("2^-1 + 3^2 * 5 - 7 // 2 + 5 % 2");
		assertEquals(0.5D + pow(3, 2) * 5 - floor(7 / 2D) + 5 % 2, e.eval());
		e = p.parse("√⌊5 / 2⌋ + ∛3");
		assertEquals(sqrt(2D) + Math.pow(3D, 1D / 3D), e.eval());
		e = p.parse("2 + (3 | 7) - 2 + |-5 | 3| + ⌊7 / 3⌋");
		assertEquals(2 + (3 | 7) - 2 + abs(-5 | 3) + floor(7D / 3D), e.eval());
		e = p.parse(" 2 - 2? 17 : 3 / ∞ ? 54 : 24⁴²");
		assertEquals(pow(24, 42), e.eval());
		assertEquals(2 + pow(2, -pow(2, 3)), p.parse("2 + 2^-2³").eval());
	}
	
	@Test void exponents() {
		ParsedExpression<Double> e = p.parse("2²⁴ - 3⁻³");
		assertEquals(Math.pow(2, 24) - Math.pow(3, -3), e.eval());
		e = p.parse("(2 + 2)² * ⁴√3");
		assertEquals(pow(4, 2) * pow(3, 1D/4D), e.eval());
		e = p.parse("|sin(4)|⁵");
		assertEquals(pow(abs(sin(4)), 5), e.eval());
		assertThrowsAndPrintStackTrace(ParseException.class, () ->
		  p.parse("2²⁴₂"));
		assertEquals(1D, p.parse("π² ≈₃ 9.87").eval());
	}
	
	@Test void defaultVariables() throws ParseException {
		ParsedExpression<Double> e = p.parse("e");
		assertEquals(Math.E, e.eval());
		e = p.parse("π");
		assertEquals(Math.PI, e.eval());
		e = p.parse("e ^ 2 * π + 2");
		assertEquals(Math.pow(Math.E, 2) * Math.PI + 2, e.eval());
		e = p.parse("1 / 0 = ∞");
		assertEquals(1D, e.eval());
		e = p.parse("sqrt(NaN)");
		assertEquals(Double.NaN, e.eval());
	}
	
	@Test void defaultFunctions() throws ParseException {
		ParsedExpression<Double> e = p.parse("sin(2)");
		assertEquals(Math.sin(2D), e.eval());
		e = p.parse("cos(3 * π / 2) + ln(2 * e)");
		assertEquals(Math.cos(Math.PI * 3D / 2) + Math.log(Math.E * 2), e.eval());
	}
	
	@Test void richOperators() {
		ParsedExpression<Double> e = p_x.parse(
		  "⌊5 - |-3|⌋ + |5 - x| * √(2 + 3)");
		Map<String, Double> vars = new HashMap<>();
		for (double x = -100; x < 100; x += 0.5D) {
			vars.put("x", x);
			assertEquals(floor(5 - abs(-3)) + Math.abs(5 - x) * sqrt(2 + 3), e.eval(vars));
		}
		e = p_x.parse("0 < x <= 2 > 1 > 0 > -1");
		for (double x = -2; x < 4; x += 0.125D) {
			vars.put("x", x);
			assertEquals((0 < x && x <= 2)? 1D : 0D, e.eval(vars));
		}
		e = p_x.parse("0 < x < 2 > 1 < 0 > -1");
		for (double x = -2; x < 4; x += 0.1D) {
			vars.put("x", x);
			assertEquals(0D, e.eval(vars));
		}
		e = p_x_y.parse("x ≈ y");
		for (double x = -1; x < 1; x += 0.0025) {
			for (double y = -1; y < 1; y += 0.0025) {
				vars.put("x", x);
				vars.put("y", y);
				assertEquals(Double.compare(x, y) == 0? 1D : 0D, e.eval(vars));
			}
		}
		e = p_x_y.parse("x ≈₂ y");
		for (double x = -1; x < 1; x += 0.0025) {
			for (double y = -1; y < 1; y += 0.0025) {
				vars.put("x", x);
				vars.put("y", y);
				assertEquals(Double.compare(round(x * 100) / 100D, round(y * 100) / 100D) == 0? 1D : 0D,
				             e.eval(vars));
			}
		}
		e = p_x_y.parse("-50 <= x ≈₋₁ y < 50");
		for (double x = -100; x < 100; x += 0.5) {
			for (double y = -100; y < 100; y += 0.5) {
				vars.put("x", x);
				vars.put("y", y);
				assertEquals((Double.compare(round(x / 10) * 10, round(y / 10) * 10) == 0
				              && x >= -50 && y < 50)? 1D : 0D,
				             e.eval(vars));
			}
		}
		e = p.parse("³√(1 + 1) + ⁷√3 + √|-4|");
		assertEquals(pow(1 + 1, 1/3D) + pow(3, 1D/7D) + sqrt(abs(-4)), e.eval());
	}
	
	@Test void inputVariables() throws ParseException {
		ParsedExpression<Double> e = p_x.parse("x + 2");
		Map<String, Double> values = new HashMap<>();
		for (double x = -20; x < 20; x += 1.5) {
			values.put("x", x);
			assertEquals(x + 2, e.eval(values));
		}
		e = p_x_y.parse("x * sin(y) + 2 * sqrt(x)");
		for (double x = -100; x < 100; x += 0.1D) {
			for (double y = -100; y < 100; y += 0.1D) {
				values.put("x", x);
				values.put("y", y);
				assertEquals(x * sin(y) + 2 * sqrt(x), e.eval(values));
			}
		}
	}
	
	@Test void customFunctions() throws ParseException {
		ExpressionFuncMapBuilder<Double> f = new ExpressionFuncMapBuilder<>();
		f.add("squanch", x -> Math.log(x * Math.exp(x)));
		Map<Entry<String, Integer>, ExpressionFunc<Double>> funcMap = f.build();
		DoubleExpressionParser par = new UnicodeMathDoubleExpressionParser(
		  ImmutableSet.of("x"), funcMap
		);
		ParsedExpression<Double> e = par.parse("x + squanch(x)");
		for (double x = -100; x < 100; x += 0.1D) {
			assertEquals(x + Math.log(x * Math.exp(x)), e.eval(ImmutableMap.of("x", x)));
		}
	}
	
	@Test void overriddenFunction() throws ParseException {
		ExpressionFuncMapBuilder<Double> f = new ExpressionFuncMapBuilder<>();
		f.add("squanch",  x  -> Math.log10(Math.cosh(x)));
		f.add("squanchy", (x, y) -> Math.atan2(y, x));
		final Map<Entry<String, Integer>, ExpressionFunc<Double>> funcMap = f.build();
		
		DoubleExpressionParser par = new UnicodeMathDoubleExpressionParser(
		  ImmutableSet.of("x", "y"), funcMap);
		
		ParsedExpression<Double> e = par.parse("squanch(x) + squanchy(y, x) * log2(5)");
		
		double eps = 1E-12;
		for (double x = -100; x < 100; x += 0.5D) {
			for (double y = -100; y < 100; y += 0.5D) {
				final double a = atan2(x, y) * log(5) / log(2);
				assertEquals(
				  -abs(Math.sin(x)) + a,
				  e.eval(ImmutableMap.of("x", x, "y", y), ImmutableList.of(
				    Pair.of("squanch", (Func1<Double>) t -> -abs(Math.sin(t)))
				  )), eps);
				assertEquals(
				  Math.log10(Math.cosh(x)) + a,
				  e.eval(ImmutableMap.of("x", x, "y", y)), eps);
			}
			double y = Double.NaN;
			assertEquals(
			  -abs(Math.sin(x)) + Math.atan2(x, y) * Math.log(5) / Math.log(2),
			  e.eval(ImmutableMap.of("x", x, "y", y), ImmutableList.of(
			    Pair.of("squanch", (Func1<Double>) t -> -abs(Math.sin(t)))
			  )), eps);
		}
		double x = Double.NaN;
		for (double y = -100; y < 100; y += 0.5D) {
			assertEquals(
			  -abs(Math.sin(x)) + Math.atan2(x, y) * Math.log(5) / Math.log(2),
			  e.eval(ImmutableMap.of("x", x, "y", y), ImmutableList.of(
			    Pair.of("squanch", (Func1<Double>) t -> -abs(Math.sin(t)))
			  )), eps);
		}
		double y = Double.NaN;
		assertEquals(
		  -abs(Math.sin(x)) + Math.atan2(x, y) * Math.log(5) / Math.log(2),
		  e.eval(ImmutableMap.of("x", x, "y", y), ImmutableList.of(
		    Pair.of("squanch", (Func1<Double>) t -> -abs(Math.sin(t)))
		  )), eps);
	}
	
	@Test void parseExceptions() throws ParseException {
		assertThrowsAndPrintStackTrace(ParseException.class, () ->
		  p_x.parse("x + sen(x)"));
		assertThrowsAndPrintStackTrace(ParseException.class, () ->
		  p_x.parse(
		    "x + sin(x) - clamp(x, 2.0, 5) + cos(3) * 3 - exp(x^2) * x % 3 + 3..2 + asin(0.5) - 3"));
		assertThrowsAndPrintStackTrace(ParseException.class, () ->
		  p.parse("π + sin(3) + 3 2"));
		assertThrowsAndPrintStackTrace(ParseException.class, () ->
		  p.parse("2 + x"));
	}
	
	@Test void multipleNamespaces() {
		final UnicodeMathDoubleExpressionParser p = new UnicodeMathDoubleExpressionParser(
		  new FixedNamespaceSet<>(
			 ImmutableMap.of(
			   "ort", ImmutableMap.of("x", 0D, "y", 0D, "z", 0D),
			   "sph", ImmutableMap.of("ρ", 0D, "θ", 0D, "φ", 0D),
			   "dup", ImmutableMap.of("x", 0D, "y", 0D, "z", 0D)
			 ), "ort"), null, null);
		final ParsedExpression<Double> e = p.parse("`dup:x` + ρ * φ - x");
		final Map<String, Double> values = new HashMap<>();
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				for (int k = 0; k < 10; k++) {
					for (int l = 0; l < 10; l++) {
						values.put("ort:x", (double) i);
						values.put("sph:ρ", (double) j);
						values.put("φ", (double) k);
						values.put("dup:x", (double) l);
						assertEquals(l + j * k - i, e.eval(values));
					}
				}
			}
		}
	}
}