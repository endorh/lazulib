package endorh.util.animation;


import java.util.Objects;
import java.util.function.Function;

import static java.lang.Math.pow;
import static net.minecraft.util.math.MathHelper.*;

/**
 * Handy easing functions, in simple precision<br>
 * Includes Robert Penner's easing functions, as well as a cubic bezier easing,
 * inspired in the Mozilla FireFox implementation for CSS easing functions.
 */
public class Easing {
	private static final float PI = (float) Math.PI;
	
	/**
	 * Linear interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t}
	 */
	public static float linear(float t) {
		return t;
	}
	
	/**
	 * Ease in sine interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code 1 - cos(tπ/2)}
	 */
	public static float sineIn(float t) {
		return 1 - cos(t * PI / 2);
	}
	
	/**
	 * Ease out sine interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code sin(tπ/2)}
	 */
	public static float sineOut(float t) {
		return sin(t * PI / 2);
	}
	
	/**
	 * Ease in/out sine interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code (1 - cos(tπ)) / 2}
	 */
	public static float sineInOut(float t) {
		return (1 - cos(PI * t)) / 2;
	}
	
	/**
	 * Ease in quadratic interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t²}
	 */
	public static float quadIn(float t) {
		return t*t;
	}
	
	/**
	 * Ease out quadratic interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return 1 - (1 - t)²
	 */
	public static float quadOut(float t) {
		return (2 - t) * t;
	}
	
	/**
	 * Ease in/out quadratic interpolation
	 * @param t Progress ∈ [0,1]
	 * @return {@code t ≤ ½ ⟼ 2t²}<br>
	 *         {@code t ≥ ½ ⟼ 1 - 2(1 - t)²}
	 */
	public static float quadInOut(float t) {
		return t<=0.5F? 2*t*t : t*(t*-2 + 4) - 1;
	}
	
	/**
	 * Ease in cubic interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t³}
	 */
	public static float cubicIn(float t) {
		return t*t*t;
	}
	
	/**
	 * Ease out cubic interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code 1 - (1 - t)³}
	 */
	public static float cubicOut(float t) {
		return t*(t*(t - 3) + 3);
	}
	
	/**
	 * Ease in/out cubic interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t ≤ ½ ⟼ 4t³}<br>
	 *         {@code t ≥ ½ ⟼ 1 - 4(1 - t)³}
	 */
	public static float cubicInOut(float t) {
		return t <= 0.5F? 4*t*t*t : t*(t*(t*4 - 12) + 12) - 3;
	}
	
	/**
	 * Ease in quartic interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t⁴}
	 */
	public static float quartIn(float t) {
		return t*t*t*t;
	}
	
	/**
	 * Ease out quartic interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code 1 - (1 - t)⁴}
	 */
	public static float quartOut(float t) {
		return t*(t*(t*(-t + 4) - 6) + 4);
	}
	
	/**
	 * Ease in/out quartic interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t ≤ ½ ⟼ 8t⁴}<br>
	 *         {@code t ≥ ½ ⟼ 1 - 8(1 - t)⁴}
	 */
	public static float quartInOut(float t) {
		return t <= 0.5F? 8*t*t*t*t : t*(t*(t*(t*-8 + 32) - 48) + 32) - 7;
	}
	
	/**
	 * Ease in quintic interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t⁵}
	 */
	public static float quintIn(float t) {
		return t*t*t*t*t;
	}
	
	/**
	 * Ease out quintic interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code 1 - (1 - t)⁵}
	 */
	public static float quintOut(float t) {
		return t*(t*(t*(t*(t - 5) + 10) - 10) + 5);
	}
	
	/**
	 * Ease in/out quintic interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t ≤ ½ ⟼ 16t⁵}<br>
	 *         {@code t ≥ ½ ⟼ 1 - 16(1 - t)⁵}
	 */
	public static float quintInOut(float t) {
		return t <= 0.5F? 16*t*t*t*t*t : t*(t*(t*(t*(t*16 - 80) + 160) - 160) + 80) - 15;
	}
	
	/**
	 * Ease in exponential interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t = 0 ⟼ 0}<br>
	 *         {@code t ≠ 0 ⟼ 2⁽¹⁰ᵗ ⁻ ¹⁰⁾}
	 */
	public static float expoIn(float t) {
		return t <= 0? 0 : (float) pow(2, 10 * (t - 1));
	}
	
	/**
	 * Ease out exponential interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t = 1 ⟼ 1}<br>
	 *         {@code t ≠ 1 ⟼ 1 - 2⁽⁻¹⁰ᵗ⁾}
	 */
	public static float expoOut(float t) {
		return t >= 1? 1 : 1 - (float) pow(2, -10 * t);
	}
	
	/**
	 * Ease in/out exponential interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t = 0 ⟼ 0}<br>
	 *         {@code t = 1 ⟼ 1}<br>
	 *         {@code t ≤ ½ ⟼ 2⁽²⁰ᵗ ⁻ ¹¹⁾}<br>
	 *         {@code t ≥ ½ ⟼ 1 - 2⁽⁹ ⁻ ²⁰ᵗ⁾}
	 */
	public static float expoInOut(float t) {
		return t <= 0? 0 :
		       t >= 1? 1 :
		       t <= 0.5F
		       ? (float) pow(2, 20 * t - 11)
		       : 1 - (float) pow(2, -20 * t + 9);
	}
	
	/**
	 * Ease in circular interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code 1 - √(1 - t²)}
	 */
	public static float circIn(float t) {
		return 1 - sqrt(1 - t*t);
	}
	
	/**
	 * Ease out circular interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code √(1 - (1 - t)²)}
	 */
	public static float circOut(float t) {
		return sqrt(2*t - t*t);
	}
	
	/**
	 * Ease in/out circular interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t ≤ ½ ⟼ (1 - √(1 - 4t²)) / 2}<br>
	 *         {@code t ≥ ½ ⟼ (1 + √(1 - 4(1 - t)²)) / 2}
	 */
	public static float circInOut(float t) {
		return t <= 0.5F? (1 - sqrt(1 - 4*t*t)) / 2 : (1 + sqrt(t*(t*-4 + 8) - 3)) / 2;
	}
	
	private static final float backEaseC1 = 1.70158F;
	private static final float backEaseC2 = backEaseC1 * 1.525F;
	/**
	 * Ease in back interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t³(1 + c) - t²c}<br>
	 *         {@code c ≔ 1.70158}
	 */
	public static float backIn(float t) {
		return t*t*(t*(backEaseC1 + 1) - backEaseC1);
	}
	
	/**
	 * Ease out back interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code 1 - (1 - t)³(1 + c) + (1 - t)²c}<br>
	 *         {@code c ≔ 1.70158}
	 */
	public static float backOut(float t) {
		return 1 - (backEaseC1 + 1) * (t * (t * (-t + 3) - 3) + 1) + backEaseC1 * (t * (t - 2) + 1);
	}
	
	/**
	 * Ease in/out back interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t ≤ ½ ⟼ 2t²(2t(c₂ + 1) - c₂)}<br>
	 *         {@code t ≥ ½ ⟼ 1 - 2(1 - t)²(2(1 - t)(c₂ + 1) + c₂)))}<br>
	 *         {@code c₁ ≔ 1.70158}<br>
	 *         {@code c₂ = 1.525c₁}
	 */
	public static float backInOut(float t) {
		return t <= 0.5F
		       ? 2*t*t*(2*t*(backEaseC2 + 1) - backEaseC2)
		       : 1 - 2*(t*(t - 2) + 1)*(2*(1 - t)*(backEaseC2 + 1) + backEaseC2);
	}
	
	/**
	 * Ease in elastic interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t = 0 ⟼ 0}<br>
	 * 		  {@code t = 1 ⟼ 1}<br>
	 * 		  {@code 2⁽¹⁰ᵗ ⁻ ¹⁰⁾sin((20t - 21.5)π / 3}
	 */
	public static float elasticIn(float t) {
		return t <= 0? 0 :
		       t >= 1? 1 :
		       (float) pow(2, 10 * (t - 1)) * sin((t*20 - 21.5F) * PI / 3);
	}
	
	/**
	 * Ease out elastic interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t = 0 ⟼ 0}<br>
	 * 		  {@code t = 1 ⟼ 1}<br>
	 * 		  {@code 1 - 2⁽⁻¹⁰ᵗ⁾sin((1.5 - 20t)π / 3}
	 */
	public static float elasticOut(float t) {
		return t <= 0? 0 :
		       t >= 1? 1 :
		       1 - (float) pow(2, -10*t) * sin((1.5F - t*20) * PI / 3);
	}
	
	/**
	 * Ease in/out elastic interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t = 0 ⟼ 0}<br>
	 * 		  {@code t = 1 ⟼ 1}<br>
	 * 		  {@code t ≤ ½ ⟼ -2⁽²⁰ᵗ ⁻ ¹¹⁾ sin((80t - 44.5)π / 9}<br>
	 * 		  {@code t ≥ ½ ⟼ 2⁽⁹ ⁻ ²⁰ᵗ⁾ sin((80t - 44.5)π / 9)}
	 */
	public static float elasticInOut(float t) {
		return t <= 0? 0 :
		       t >= 1? 1 :
		       t <= 0.5F
		       ? (float) pow(2, 20*t - 11) * sin((44.5F - t*80) * PI / 9)
		       : (float) pow(2,  9 - 20*t) * sin((t*80 - 44.5F) * PI / 9);
	}
	
	/**
	 * Ease in bounce interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t = 0 ⟼ 0}<br>
	 * 		  {@code t = 1 ⟼ 1}<br>
	 * 		  {@code 1 - }{@link #bounceOut}{@code (1 - t)}
	 */
	public static float bounceIn(float t) {
		return t <= 0? 0 : t >= 1? 1 : 1 - bounceOut(1 - t);
	}
	
	private static final float bounceEaseC1 = 7.5625F;
	private static final float bounceEaseC2 = 2.75F;
	/**
	 * Ease out bounce interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t = 0 ⟼ 0}<br>
	 * 		  {@code t = 1 ⟼ 1}<br>
	 * 		  {@code t ≤ 1/c₂ ⟼ c₁t²}<br>
	 * 		  {@code t ≤ 2/c₂ ⟼ c₁(t - 1.5/c₂)² + 0.75}<br>
	 * 		  {@code t ≤ 2.5/c₂ ⟼ c₁(t - 2.25/c₂)² + 0.9375}<br>
	 * 		  {@code t ≥ 2.75/c₂ ⟼ c₁(t - 2.625/c₂)² + 0.984375}<br>
	 * 		  {@code c₁ = 7.5625}<br>
	 * 		  {@code c₂ = 2.75}
	 */
	public static float bounceOut(float t) {
		return t <= 0? 0 :
		       t >= 1? 1 :
		       t <= 1 / bounceEaseC2
		       ? bounceEaseC1 * t*t
		       : t <= 2 / bounceEaseC2
		         ? bounceEaseC1 * (t -= 1.5F / bounceEaseC2) * t + 0.75F
		         : t <= 2.5F / bounceEaseC2
		           ? bounceEaseC1 * (t -= 2.25F / bounceEaseC2) * t + 0.9375F
		           : bounceEaseC1 * (t -= 2.625F / bounceEaseC2) * t + 0.984375F;
	}
	
	/**
	 * Ease in/out bounce interpolation
	 * @param t Progress ∈ [0, 1]
	 * @return {@code t = 0 ⟼ 0}<br>
	 * 		  {@code t = 1 ⟼ 1}<br>
	 * 		  {@code t ≤ ½ ⟼ (1 - }{@link #bounceOut}{@code (1 - 2*t)) / 2}<br>
	 * 		  {@code t ≥ ½ ⟼ (1 + }{@link #bounceOut}{@code (2*t - 1)) / 2}
	 */
	public static float bounceInOut(float t) {
		return t <= 0? 0 :
		       t >= 1? 1 :
		       t <= 0.5F
		       ? (1 - bounceOut(1 - 2*t)) / 2
		       : (1 + bounceOut(2*t - 1)) / 2;
	}
	
	/**
	 * Cubic bezier easing interpolation
	 * @param x1 X coordinate of the first control point
	 * @param y1 Y coordinate of the first control point
	 * @param x2 X coordinate of the second control point
	 * @param y2 Y coordinate of the second control point
	 */
	public static CubicBezier cubicBezier(float x1, float y1, float x2, float y2) {
		return new CubicBezier(x1, y1, x2, y2);
	}
	
	@FunctionalInterface public interface EasingFunction extends Function<Float, Float> {
		static EasingFunction clamped(EasingFunction f) {
			return t -> clamp(f.apply(t), 0, 1);
		}
		float map(float t);
		@Override default Float apply(Float t) {
			return map(t);
		}
		default EasingFunction clamped() {
			return clamped(this);
		}
	}
	
	/**
	 * Cubic bezier easing interpolation function.<br>
	 * Works the same as {@code cubic-bezier} in CSS.<br><br>
	 *
	 * Uses a sample table to find {@code t} values faster with either
	 * Newton-Raphson approximation or binary search depending on the slope.
	 */
	public static class CubicBezier implements EasingFunction {
		// Should be enough for simple precision
		private static final int NEWTON_ITERATIONS = 2;
		private static final float NEWTON_MIN_SLOPE = 0.02F;
		private static final float SUBDIVISION_PRECISION = 1E-6F;
		private static final int SUBDIVISION_MAX_ITERATIONS = 8;
		
		private static final int SAMPLE_TABLE_SIZE = 11;
		private static final float SAMPLE_STEP_SIZE = 1F / (SAMPLE_TABLE_SIZE - 1);
		
		private final float x1, y1, x2, y2;
		private final float[] sampleValues;
		
		private CubicBezier(float x1, float y1, float x2, float y2) {
			if (x1 < 0 || x1 > 1 || x2 < 0 || x2 > 1) throw new IllegalArgumentException(
			  "Bezier easing function control point x coordinates must be in [0, 1] range");
			
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			
			if (x1 != y1 || x2 != y2) {
				sampleValues = new float[SAMPLE_TABLE_SIZE];
				calcSampleValues();
			} else sampleValues = null; // Short-circuit linear bezier
		}
		
		/**
		 * Fills the {@link #sampleValues} array
		 */
		private void calcSampleValues() {
			for (int i = 0; i < SAMPLE_TABLE_SIZE; i++)
				sampleValues[i] = calcBezier(i * SAMPLE_STEP_SIZE, x1, x2);
		}
		
		/**
		 * Get the parameter of degree 3 for the 1D cubic bezier polynomial.
		 * @param p1 control point 1
		 * @param p2 control point 2
		 * @return {@code 1 - 3p2 + 3p1}
		 */
		private float A(float p1, float p2) {
			return 1 - 3 * p2 + 3 * p1;
		}
		
		/**
		 * Get the parameter of degree 2 for the 1D cubic bezier polynomial.
		 * @param p1 control point 1
		 * @param p2 control point 2
		 * @return {@code 3p2 - 6p1}
		 */
		private float B(float p1, float p2) {
			return 3 * p2 - 6 * p1;
		}
		
		/**
		 * Get the parameter of degree 1 for the 1D cubic bezier polynomial.
		 * @param p1 control point 1
		 * @return {@code 3p1}
		 */
		private float C(float p1) {
			return 3 * p1;
		}
		
		/**
		 * Returns x(t) given t, x1 and x2, or y(t) given t, y1 and y2
		 * @param t Bezier parameter
		 * @param p1 x or y coordinate of control point 1
		 * @param p2 x or y coordinate of control point 2
		 * @return x(t) or y(t)
		 */
		private float calcBezier(float t, float p1, float p2) {
			return t*(t*(t*A(p1, p2) + B(p1, p2)) + C(p1));
		}
		
		/**
		 * Returns dx/dt given t, x1, and x2, or dy/dt given t, y1 and y2
		 * @param t Bezier parameter
		 * @param p1 x or y coordinate of control point 1
		 * @param p2 x or y coordinate of control point 2
		 * @return dx/dt or dy/dt
		 */
		private float getSlope(float t, float p1, float p2) {
			return t*(t*3*A(p1, p2) + 2*B(p1, p2)) + C(p1);
		}
		
		/**
		 * Finds a {@code t} value for which {@code calcBezier(t) = x}.
		 * @param x value to solve for
		 * @return Approximate {@code t} value for which {@code calcBezier(t) = x}
		 */
		private float getTForX(float x) {
			// Find sample step where the x is
			int currentIndex = 1;
			int lastIndex = SAMPLE_TABLE_SIZE - 1;
			while (currentIndex < lastIndex && sampleValues[currentIndex] <= x) currentIndex++;
			float intervalStart = --currentIndex * SAMPLE_STEP_SIZE;
			
			// Linear guess
			float current = sampleValues[currentIndex];
			float next = sampleValues[currentIndex + 1];
			float dist = (x - current) / (next - current);
			float tGuess = intervalStart + dist * SAMPLE_STEP_SIZE;
			
			// Check the slope to choose a strategy.
			// If the slope is too small, Newton-Raphson won't converge.
			float initialSlope = getSlope(tGuess, x1, x2);
			if (initialSlope >= NEWTON_MIN_SLOPE) {
				return newtonRaphsonIterate(x, tGuess);
			} else if (initialSlope == 0) {
				return tGuess;
			} else return binarySubdivide(x, intervalStart, intervalStart + SAMPLE_STEP_SIZE);
		}
		
		/**
		 * Finds a {@code t} value for which {@code calcBezier(t) = x}
		 * using the Newton-Raphson method.
		 * @param x value to solve for
		 * @param tGuess Initial guess for t
		 * @return Approximate t value for which {@code calcBezier(t) = x}
		 */
		private float newtonRaphsonIterate(float x, float tGuess) {
			// Find a root for f(t) = calcBezier(t) - x
			float currentX, currentSlope;
			for (int i = 0; i < NEWTON_ITERATIONS; i++) {
				currentX = calcBezier(tGuess, x1, x2) - x;
				currentSlope = getSlope(tGuess, x1, x2);
				if (currentSlope == 0) return tGuess;
				tGuess -= currentX / currentSlope;
			}
			return tGuess;
		}
		
		/**
		 * Finds a {@code t} value for which {@code calcBezier(t) = x}
		 * using binary search.
		 * @param x value to solve for
		 * @param l Initial lower bound for t
		 * @param r Initial upper bound for t
		 * @return Approximate t value for which {@code calcBezier(t) = x}
		 */
		private float binarySubdivide(float x, float l, float r) {
			float currentX;
			float currentT;
			int i = 0;
			do {
				currentT = l + (r - l) / 2;
				currentX = calcBezier(currentT, x1, x2) - x;
				if (currentX > 0) {
					r = currentT;
				} else l = currentT;
			} while (abs(currentX) > SUBDIVISION_PRECISION && ++i < SUBDIVISION_MAX_ITERATIONS);
			return currentT;
		}
		
		@Override public float map(float x) {
			if (x1 == y1 && x2 == y2) return x; // Short-circuit linear bezier
			return calcBezier(getTForX(x), y1, y2);
		}
		
		public float getX1() {
			return x1;
		}
		public float getY1() {
			return y1;
		}
		public float getX2() {
			return x2;
		}
		public float getY2() {
			return y2;
		}
		
		@Override public String toString() {
			return "cubicBezier(" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + ")";
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			CubicBezier that = (CubicBezier) o;
			return Float.compare(that.x1, x1) == 0 && Float.compare(that.y1, y1) == 0 &&
			       Float.compare(that.x2, x2) == 0 && Float.compare(that.y2, y2) == 0;
		}
		
		@Override public int hashCode() {
			return Objects.hash(x1, y1, x2, y2);
		}
	}
}
