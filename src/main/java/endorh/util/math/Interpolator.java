package endorh.util.math;

/**
 * Interpolation functions<br>
 */
public class Interpolator {
	
	/**
	 * @param t Progress ∈ [0, 1]
	 * @return t²
	 * @see Interpolator#clampQuadIn
	 */
	public static float quadIn(float t) {
		return t*t;
	}
	
	/**
	 * @param t Progress ∈ [0, 1]
	 * @return 1 - (1 - t)²
	 * @see Interpolator#clampQuadOut
	 */
	public static float quadOut(float t) {
		return 2*t - t*t;
	}
	
	/**
	 * @param t Progress ∈ [0,1]
	 * @return 2t²χ(-∞,½](t) + (1 - 2(1 - t)²)χ(½,+∞)(t)
	 * @see Interpolator#clampQuadInOut
	 */
	public static float quadInOut(float t) {
		return t<=0.5? 2*t*t : -1F + (4 - 2*t)*t;
	}
	
	/**
	 * @param t Progress ∈ |R
	 * @return t²χ(0,1)(t) + χ[1,+∞)(t)
	 * @see Interpolator#quadIn
	 */
	public static float clampQuadIn(float t) {
		return t<=0F? 0F : t<1F? t*t : 1F;
	}
	
	/**
	 * @param t Progress ∈ |R
	 * @return (1 - (1 - t)²)χ(0,1)(t) + χ[1,+∞)(t)
	 * @see Interpolator#quadOut
	 */
	public static float clampQuadOut(float t) {
		return t<=0F? 0F : t<1F? 2*t - t*t : 1F;
	}
	
	/**
	 * @param t Progress ∈ |R
	 * @return 2t²χ(0,½](t) + (1 - 2(1 - t)²)χ(½,1)(t) + χ[1,+∞)(t)
	 * @see Interpolator#quadInOut
	 */
	public static float clampQuadInOut(float t) {
		return t <= 0F? 0F : (t <= 0.5? 2*t*t : (t < 1F? -1F + (4 - 2*t)*t : 1F));
	}
	
	/**
	 * @param lowerBnd Lower bound
	 * @param upperBnd Upper bound
	 * @param slide Interpolation progress ∈ |R
	 * @return lowerBnd + (upperBnd-lowerBnd) (slideχ(lowerBnd, upperBnd)(t) + χ[upperBnd,+∞)(t))
	 */
	// Parameter order consistent with net.minecraft.util.math.MathHelper#clampedLerp(double, double, double)
	public static float clampedLerp(float lowerBnd, float upperBnd, float slide) {
		return slide < 0F ? lowerBnd : slide > 1F ? upperBnd : (upperBnd - lowerBnd) * slide + lowerBnd;
	}
}
