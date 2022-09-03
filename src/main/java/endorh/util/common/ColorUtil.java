package endorh.util.common;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.DyeColor;

import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.lang.Math.*;
import static net.minecraft.util.Mth.clamp;

/**
 * Color conversions
 */
public class ColorUtil {
	/**
	 * Measures color distance within the HSB space, giving double
	 * the weight to hue.
	 * @param a First color
	 * @param b Second color
	 * @return Color distance
	 */
	public static float hsbDistance(Color a, Color b) {
		float[] a_hsb =  Color.RGBtoHSB(a.getRed(), a.getGreen(), a.getBlue(), null);
		float[] b_hsb =  Color.RGBtoHSB(b.getRed(), b.getGreen(), b.getBlue(), null);
		float[] diff = {
		  min((a_hsb[0] - b_hsb[0] + 1F) % 1F, (b_hsb[0] - a_hsb[0] + 1F) % 1F),
		  abs(a_hsb[1] - b_hsb[1]),
		  abs(a_hsb[2] - b_hsb[2])
		};
		return diff[0] * diff[0] * 2F + diff[1] * diff[1] + diff[2] * diff[2];
	}
	
	/**
	 * Returns the closest {@link DyeColor} to a given color,
	 * based on {@link ColorUtil#hsbDistance}
	 */
	public static Optional<DyeColor> closestDyeColor(Color color) {
		return Arrays.stream(DyeColor.values()).min(
		  Comparator.comparingDouble(dye -> hsbDistance(color, new Color(dye.getTextColor()))));
	}
	
	/**
	 * Returns the closest {@link ChatFormatting} color to a given color,
	 * based on {@link ColorUtil#hsbDistance}
	 */
	public static Optional<ChatFormatting> closestTextColor(Color color) {
		//noinspection ConstantConditions
		return Arrays.stream(ChatFormatting.values())
		  .filter(ChatFormatting::isColor).min(
		    Comparator.comparingDouble(fmt -> hsbDistance(color, new Color(fmt.getColor()))));
	}
	
	public static int mix(List<Integer> colors) {
		return mix(colors.stream().mapToInt(t -> t).toArray());
	}
	
	public static int mix(int... colors) {
		return mix(Arrays.stream(colors).mapToObj(Color::new).toArray(Color[]::new)).getRGB();
	}
	
	public static Color mix(Color... colors) {
		final int n = colors.length;
		if (n == 0)
			return Color.black;
		float r = 0F, g = 0F, b = 0F, a = 0F;
		for (Color c : colors) {
			r += c.getRed() / 255F;
			g += c.getGreen() / 255F;
			b += c.getBlue() / 255F;
			a += c.getAlpha() / 255F;
		}
		return new Color(r / n, g / n, b / n, a / n);
	}
	
	public static Color multiply(Color color, Color mask) {
		return multiply(color, mask.getRed() / 255F, mask.getGreen() / 255F,
		                mask.getBlue() / 255F, mask.getAlpha() / 255F);
	}
	
	public static Color multiply(Color color, float mask) {
		return multiply(color, mask, mask, mask, 1F);
	}
	
	public static Color multiply(Color color, float r, float g, float b) {
		return multiply(color, r, g, b, 1F);
	}
	
	public static Color multiply(Color color, float r, float g, float b, float a) {
		return new Color(
		  clamp(round(color.getRed() * r), 0, 255),
		  clamp(round(color.getGreen() * g), 0, 255),
		  clamp(round(color.getBlue() * b), 0, 255),
		  clamp(round(color.getAlpha() * a), 0, 255));
	}
	
	/**
	 * Completely arbitrary mapping from {@link DyeColor}s to
	 * {@link ChatFormatting} colors
	 *
	 * @param color Dye color
	 * @return Closest text color, by my criteria
	 */
	@SuppressWarnings("DuplicateBranchesInSwitch")
	public static ChatFormatting textColorFromDye(DyeColor color) {
		return switch (color) {
			case WHITE -> ChatFormatting.WHITE;
			case ORANGE -> ChatFormatting.GOLD;
			case MAGENTA -> ChatFormatting.LIGHT_PURPLE;
			case LIGHT_BLUE -> ChatFormatting.AQUA;
			case YELLOW -> ChatFormatting.YELLOW;
			case LIME -> ChatFormatting.GREEN;
			case PINK -> ChatFormatting.LIGHT_PURPLE;
			case GRAY -> ChatFormatting.DARK_GRAY;
			case LIGHT_GRAY -> ChatFormatting.GRAY;
			case CYAN -> ChatFormatting.DARK_AQUA;
			case PURPLE -> ChatFormatting.DARK_PURPLE;
			case BLUE -> ChatFormatting.BLUE;
			case BROWN -> ChatFormatting.DARK_RED;
			case GREEN -> ChatFormatting.DARK_GREEN;
			case RED -> ChatFormatting.RED;
			case BLACK -> ChatFormatting.BLACK;
		};
	}
	
	/**
	 * Filter out black and use dark gray instead
	 */
	public static ChatFormatting discardBlack(ChatFormatting fmt) {
		if (fmt == ChatFormatting.BLACK)
			return ChatFormatting.DARK_GRAY;
		return fmt;
	}
	
	/**
	 * Color linear interpolation, within the RGB space
	 * @param t Interpolation progress, within 0~1
	 * @param origin Initial color
	 * @param target Target color
	 * @return Linearly interpolated color
	 * @see ColorUtil#hsbLerp
	 */
	public static Color lerp(float t, Color origin, Color target) {
		float r = 1F - t;
		return new Color(
		  origin.getRed() * r + target.getRed() * t,
		  origin.getGreen() * r + target.getGreen() * t,
		  origin.getBlue() * r + target.getBlue() * t
		);
	}
	
	private static final float[] originHSB = new float[3];
	private static final float[] targetHSB = new float[3];
	
	/**
	 * Color linear interpolation, within the HSB space
	 * @param t Interpolation progress
	 * @param origin Initial color
	 * @param target Target color
	 * @return Linearly interpolated color
	 * @see ColorUtil#lerp
	 */
	public static Color hsbLerp(float t, Color origin, Color target) {
		float r = 1F - t;
		Color.RGBtoHSB(origin.getRed(), origin.getGreen(), origin.getBlue(), originHSB);
		Color.RGBtoHSB(target.getRed(), target.getGreen(), target.getBlue(), targetHSB);
		
		
		if (originHSB[0] - targetHSB[0] > 180F)
			targetHSB[0] += 360F;
		if (targetHSB[0] - originHSB[0] > 180F)
			originHSB[0] += 360F;
		
		return Color.getHSBColor(
		  originHSB[0] * r + targetHSB[0] * t,
		  originHSB[1] * r + targetHSB[1] * t,
		  originHSB[2] * r + targetHSB[2] * t);
	}
	
	/**
	 * Color linear interpolation within the HSB space.<br>
	 * Optimized version without object allocations, useful for particle rendering.
	 * @param t Interpolation progress
	 * @param origin Initial color (HSB float array)
	 * @param target Target color (HSB float array)
	 * @param dest Destination color (HSB float array)
	 */
	public static void hsbLerp(float t, float[] origin, float[] target, float[] dest) {
		float r = 1F - t;
		float diff = origin[0] - target[0];
		float targetHue = diff > 180F? target[0] + 360F : target[0];
		float originHue = diff < -180F? origin[0] + 360F : origin[0];
		dest[0] = originHue * r + targetHue * t;
		dest[1] = origin[1] * r + target[1] * t;
		dest[2] = origin[2] * r + target[2] * t;
	}
	
	/**
	 * Color linear interpolation within the HSB space.<br>
	 * Optimized version without object allocations, useful for particle rendering.
	 * @param t Interpolation progress
	 * @param origin Initial color (HSB float array)
	 * @param target Target color (HSB float array)
	 * @param dest Destination color (RGB float array)
	 */
	public static void hsbLerpToRgb(float t, float[] origin, float[] target, float[] dest) {
		hsbLerp(t, origin, target, dest);
		HSBtoRGB(dest, dest);
	}
	
	/**
	 * HSB to RGB conversion in float array format.<br>
	 * The destination array can be the input array.
	 */
	public static void HSBtoRGB(float[] hsb, float[] rgb) {
		if (hsb[1] == 0) {
			rgb[0] = rgb[1] = rgb[2] = hsb[2];
		} else {
			float hue = (hsb[0] - (float) floor(hsb[0])) * 6.0f;
			float f = hue - (float) floor(hue);
			float p = hsb[2] * (1F - hsb[1]);
			float q = hsb[2] * (1F - hsb[1] * f);
			float t = hsb[2] * (1F - (hsb[1] * (1F - f)));
			switch ((int) hue) {
				case 0 -> {
					rgb[0] = hsb[2];
					rgb[1] = t;
					rgb[2] = p;
				}
				case 1 -> {
					rgb[0] = q;
					rgb[1] = hsb[2];
					rgb[2] = p;
				}
				case 2 -> {
					rgb[0] = p;
					rgb[1] = hsb[2];
					rgb[2] = t;
				}
				case 3 -> {
					rgb[0] = p;
					rgb[1] = q;
					rgb[2] = hsb[2];
				}
				case 4 -> {
					rgb[0] = t;
					rgb[1] = p;
					rgb[2] = hsb[2];
				}
				case 5 -> {
					rgb[0] = hsb[2];
					rgb[1] = p;
					rgb[2] = q;
				}
			}
		}
	}
	
	/**
	 * Equivalent to the old {@code DyeColor.getColorValue}
	 */
	public static int getTextureDiffuseColor(DyeColor color) {
		final float[] colors = color.getTextureDiffuseColors();
		return 0xFF000000
		       | ((int) (colors[0] * 255F + 0.5F) & 0xFF) << 24
		       | ((int) (colors[1] * 255F + 0.5F) & 0xFF) << 16
		       | (int) (colors[2] * 255F + 0.5F) & 0xFF;
	}
}
