package endorh.lazulib.animation;

import endorh.lazulib.animation.Easing.EasingFunction;

import static java.lang.Math.abs;

/**
 * Basic toggle animator.<br>
 * It's target can be changed before finishing the animation,
 * for natural animation cancellations.<br>
 * Can also map it's output range to a different interval.
 */
public class ToggleAnimator {
	protected float progress;
	protected float target;
	protected float lastProgress;
	protected long lastChange = 0L;
	protected long length;
	protected float min = 0F;
	protected float max = 1F;
	protected EasingFunction easing = Easing::linear;
	
	public static ToggleAnimator of(EasingFunction interpolation) {
		return new ToggleAnimator(interpolation);
	}
	
	public static ToggleAnimator of(long lengthMs, EasingFunction interpolation) {
		return new ToggleAnimator(lengthMs, interpolation);
	}
	
	public static ToggleAnimator linear() {
		return of(Easing::linear);
	}
	
	public static ToggleAnimator linear(long lengthMs) {
		return of(lengthMs, Easing::linear);
	}
	
	public static ToggleAnimator quadOut() {
		return of(Easing::quadOut);
	}
	
	public static ToggleAnimator quadOut(long lengthMs) {
		return of(lengthMs, Easing::quadOut);
	}
	
	public static ToggleAnimator bezier(float x1, float y1, float x2, float y2) {
		return of(Easing.cubicBezier(x1, y1, x2, y2));
	}
	
	public ToggleAnimator() {this(250L);}
	public ToggleAnimator(long length) {this(0F, length);}
	public ToggleAnimator(float progress, long length) {
		target = lastProgress = this.progress = progress;
		this.length = length;
	}
	public ToggleAnimator(EasingFunction interpolation) {
		this();
		this.easing = interpolation;
	}
	public ToggleAnimator(long length, EasingFunction interpolation) {
		this(length);
		this.easing = interpolation;
	}
	
	public float getRawProgress() {
		long time = System.currentTimeMillis();
		float len = length * abs(target - lastProgress);
		if (time - lastChange < len) {
			final float t = (time - lastChange) / len;
			return progress = lastProgress * (1 - t) + target * t;
		} else return progress = target;
	}
	
	public float getUnitProgress() {
		return easing.apply(getRawProgress());
	}
	
	public float getProgress() {
		return mapRange(getUnitProgress());
	}
	
	public float mapRange(float in) {
		return min + in * (max - min);
	}
	
	public void setTargetRaw(boolean onOff) {
		setTargetRaw(onOff? 1F : 0F);
	}
	
	public void setTargetRaw(float target) {
		lastProgress = getRawProgress();
		this.target = target;
		lastChange = System.currentTimeMillis();
	}
	
	public void setTarget(boolean onOff) {
		setTarget(onOff? 1F : 0F);
	}
	
	public void setTarget(float target) {
		lastProgress = getUnitProgress();
		this.target = target;
		lastChange = System.currentTimeMillis();
	}
	
	public void toggle() {
		setTargetRaw(target <= 0.5);
	}
	
	public void resetTarget() {
		resetTarget(true);
	}
	
	public void resetTarget(boolean onOff) {
		this.target = onOff? 1F : 0F;
		lastProgress = 1F - target;
		lastChange = System.currentTimeMillis();
	}
	
	public void resetTarget(float target) {
		this.target = target;
		lastProgress = 0F;
		lastChange = System.currentTimeMillis();
	}
	
	public void stopAndSet(float target) {
		this.target = target;
		lastProgress = 1F;
		lastChange = System.currentTimeMillis() - length;
	}
	
	public void stopAndSet(boolean target) {
		stopAndSet(target? 1F : 0F);
	}
	
	public boolean isInProgress() {
		return System.currentTimeMillis() - lastChange < length * abs(target - lastProgress);
	}
	
	public long getLastChange() {
		return lastChange;
	}
	
	public float getTarget() {
		return target;
	}
	
	public float getRangeMin() {
		return min;
	}
	
	public void setRangeMin(float min) {
		this.min = min;
	}
	
	public float getRangeMax() {
		return max;
	}
	
	public void setRangeMax(float max) {
		this.max = max;
	}
	
	public void setRange(float min, float max) {
		this.min = min;
		this.max = max;
	}
	
	public long getLength() {
		return length;
	}
	
	public void setLength(long length) {
		this.length = length;
	}
	
	public EasingFunction getEasing() {
		return easing;
	}
	
	public void setEasing(EasingFunction easing) {
		this.easing = easing;
	}
}
