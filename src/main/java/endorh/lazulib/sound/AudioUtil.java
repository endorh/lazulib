package endorh.lazulib.sound;

import net.minecraft.util.Mth;

/**
 * Remembering how to implement these everytime is annoying
 */
public class AudioUtil {
	private static final float PI = (float) Math.PI;
	
	/**
	 * Get sqrt cosine crossfade factors
	 * @param p progress ∈ [0, 1]
	 * @return [pre, pos] crossfaded at p
	 */
	public static float[] crossFade(float p) {
		float[] factors = new float[2];
		float cos = Mth.cos(PI * p);
		factors[0] = Mth.sqrt(0.5F + 0.5F * cos);
		factors[1] = Mth.sqrt(0.5F - 0.5F * cos);
		return factors;
	}
	
	/**
	 * Exponential fade out
	 * @param p progress ∈ [0, 1]
	 * @return (1E-3)^(3(1-p)), clamped to [0, 1]
	 */
	public static float fadeOutExp(float p) {
		return p >= 1F ? 0F : p <= 0F? 1F : 1E-3F * (float) Math.pow(10D, 3F - 3F * p);
	}
	
	/**
	 * Exponential fade in
	 * @param p progress ∈ [0, 1]
	 * @return (1E-3)^(3p), clamped to [0, 1]
	 */
	public static float fadeInExp(float p) {
		return p <= 0F ? 0F : p >= 1F? 1F : 1E-3F * (float) Math.pow(10D, 3F * p);
	}
}
