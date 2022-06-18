package endorh.util.math;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static java.lang.Math.*;
import static net.minecraft.util.math.MathHelper.cos;
import static net.minecraft.util.math.MathHelper.sin;

/**
 * Mutable 3D float vector implementation, as an alternative to
 * Vector3f, with unified interface across server/client side.<br>
 * It does not necessarily support all methods that {@link Vector3f} offers.
 * It is also not thread-safe, since it's mutable.<br><br>
 * Some features:
 * <ul>
 *   <li>Standard vector operations</li>
 *   <li>Several rotation methods</li>
 *   <li>Conversion from/to {@link Vector3d}, {@link Vector3f} and {@link Vector3i}.</li>
 *   <li>Conversion from/to spherical coordinates</li>
 *   <li>De/serialization from/into {@link PacketBuffer}</li>
 *   <li>Various random vector generation methods</li>
 * </ul>
 * @see Vec3d
 */
@SuppressWarnings("unused")
public class Vec3f {
	public float x;
	public float y;
	public float z;
	
	/**
	 * Degrees to radians factor
	 */
	public static final float TO_RAD = (float)(Math.PI / 180D);
	/**
	 * Radians to degrees factor
	 */
	public static final float TO_DEGREES = (float)(180D / Math.PI);
	/**
	 * float PI
	 */
	public static final float PI = (float)Math.PI;
	/**
	 * Float PI/2
	 */
	public static final float PI_HALF = (float)(Math.PI / 2D);
	/**
	 * Random generator
	 */
	private static final Random random = new Random();
	
	/**
	 * Used by other constructors
	 */
	private Vec3f() {}
	/**
	 * Create a new vector
	 * @param x X component
	 * @param y Y component
	 * @param z Z component
	 */
	public Vec3f(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	/**
	 * Create a new vector
	 * @param x X component
	 * @param y Y component
	 * @param z Z component
	 */
	public Vec3f(double x, double y, double z) {
		this.x = (float)x;
		this.y = (float)y;
		this.z = (float)z;
	}
	/**
	 * Create copy vector
	 * @param vec Vector3 to copy
	 */
	public Vec3f(Vec3f vec) {
		x = vec.x;
		y = vec.y;
		z = vec.z;
	}
	
	/**
	 * Create copy vector
	 * @param vec Vector3i to copy
	 */
	public Vec3f(Vector3i vec) {
		x = vec.getX();
		y = vec.getY();
		z = vec.getZ();
	}
	
	/**
	 * Create copy vector
	 * @param vec Vector3i to copy
	 * @param center true if coordinates should be centered
	 */
	public Vec3f(Vector3i vec, boolean center) {
		if (center) {
			x = vec.getX() + 0.5F;
			y = vec.getY() + 0.5F;
			z = vec.getZ() + 0.5F;
		} else {
			x = vec.getX();
			y = vec.getY();
			z = vec.getZ();
		}
	}
	
	/**
	 * Create copy vector
	 * @param vec Vector3f to copy
	 */
	public Vec3f(Vector3f vec) {
		x = vec.x();
		y = vec.y();
		z = vec.z();
	}
	
	/**
	 * Create copy vector
	 * @param vec Vector3d to copy
	 */
	public Vec3f(Vector3d vec) {
		x = (float)vec.x;
		y = (float)vec.y;
		z = (float)vec.z;
	}
	
	/**
	 * Create copy vector
	 * @param vec {@link Vec3d} to copy
	 */
	public Vec3f(Vec3d vec) {
		x = (float)vec.x;
		y = (float)vec.y;
		z = (float)vec.z;
	}
	
	/**
	 * Create unitary vector from spherical coordinates, as used by Minecraft<br>
	 * The Y axis is used as height. The Z axis is the Yaw origin, and Yaw is
	 * measured clockwise as seen from positive Y, with the axis following the
	 * right hand rule, that is, X × Y = Z
	 * @param yaw Spherical yaw in degrees, from Z axis, clockwise
	 * @param pitch Spherical pitch in degrees, from XZ plane
	 */
	public Vec3f(float yaw, float pitch) {
		this(yaw, pitch, true);
	}
	
	/**
	 * Create unitary vector from spherical coordinates, as used by Minecraft<br>
	 * The Y axis is used as height. The Z axis is the Yaw origin, and Yaw is
	 * measured clockwise as seen from positive Y, with the axis following the
	 * right hand rule, that is, X × Y = Z
	 *
	 * @param yaw Spherical yaw, from Z axis, clockwise
	 * @param pitch Spherical pitch, from XZ plane
	 * @param degrees True if yaw and pitch are given in degrees, default
	 */
	public Vec3f(float yaw, float pitch, boolean degrees) {
		if (degrees) {
			pitch = pitch * TO_RAD;
			yaw = yaw * TO_RAD;
		}
		float yawCos = (float)Math.cos(yaw);
		float yawSin = (float)Math.sin(yaw);
		float pitCos = (float)Math.cos(pitch);
		float pitSin = (float)Math.sin(pitch);
		x = -yawSin * pitCos;
		y = -pitSin;
		z = yawCos * pitCos;
	}
	
	/**
	 * Create vector from float[]. The first three elements of the
	 * list will be stored as x, y and z respectively<br>
	 * Is the inverse of {@link Vec3f#asArray()}.
	 * @param array Array containing the coordinates, must have at least
	 *             three elements
	 */
	public Vec3f(float[] array) {
		try {
			x = array[0];
			y = array[1];
			z = array[2];
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException(
			  "Unable to read Vector3 from array, too few elements");
		}
	}
	
	/**
	 * Create vector from {@code List<Float>}. The first three elements of the list will
	 * be stored as x, y and z respectively<br>
	 * Is the inverse of {@link Vec3f#asList}
	 * @param list List containing the coordinates, must have at least three elements
	 */
	public Vec3f(List<Float> list) {
		try {
			x = list.get(0);
			y = list.get(1);
			z = list.get(2);
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException(
			  "Unable to read Vector3 from list, too few elements");
		}
	}
	
	/**
	 * Creates a random vector, with each coordinate ranging
	 * between 0~1. Uniformly distributed as provided by
	 * {@link Random#nextFloat}
	 */
	public static Vec3f random() {
		return new Vec3f(random.nextFloat(), random.nextFloat(), random.nextFloat());
	}
	
	/**
	 * Creates a random vector, with each coordinate within -range~range,
	 * uniformly distributed as provided by {@link Random#nextFloat}<br>
	 * Equivalent to calling {@code Vector3.random(-range, range)}
	 * @param range Absolute bound for each coordinate
	 */
	public static Vec3f random(float range) {
		Vec3f res = Vec3f.random();
		res.mul(2 * range);
		res.add(-range);
		return res;
	}
	
	/**
	 * Creates a random vector, with each coordinate within the
	 * given range. Uniformly distributed as provided by
	 * {@link Random#nextFloat}.
	 * @param min Lower bound
	 * @param max Upper bound
	 */
	public static Vec3f random(float min, float max) {
		Vec3f res = Vec3f.random();
		res.mul(max - min);
		res.add(min);
		return res;
	}
	
	/**
	 * Creates a random unitary vector, uniformly distributed across
	 * the sphere, by means of the Lambert cylindrical equal-area
	 * projection, as provided by {@link Random#nextFloat}.
	 */
	public static Vec3f randomUnitary() {
		float u = random.nextFloat() * 2 * PI;
		float v = -1F + random.nextFloat() * 2;
		// Short-circuited spherical coordinates conversion
		float h = (float)sqrt(1F - v * v);
		return new Vec3f(cos(u) * h, sin(u) * h, v);
	}
	
	
	/**
	 * Creates a random vector of norm &#8804; 1, uniformly distributed
	 * across the sphere in terms of volume, by means of the
	 * Lambert cylindrical equal-area projection, multiplied by a
	 * random radius between 0~1 quadratically distributed.
	 *
	 * The pseudo-random generation is provided by {@link Random#nextFloat()}
	 */
	public static Vec3f randomSpherical() {
		final float u = random.nextFloat() * 2F * PI;
		final float v = -1F + random.nextFloat() * 2F;
		// √X where X is uniformly distributed follows a quadratic distribution
		final float r = (float)sqrt(random.nextFloat());
		// Short-circuited spherical coordinates conversion
		final float h = (float)sqrt(1F - v * v);
		return new Vec3f(cos(u) * h * r, sin(u) * h * r, v * r);
	}
	
	/**
	 * Addition
	 * @param vec Vector to add
	 */
	public void add(Vec3f vec) {
		x += vec.x;
		y += vec.y;
		z += vec.z;
	}
	
	/**
	 * Scaled addition
	 * @param vec Vector to add, scaled by scale.
	 *            Unmodified by the call.
	 * @param scale Scale to apply to vec before addition
	 */
	public void add(Vec3f vec, float scale) {
		x += vec.x * scale;
		y += vec.y * scale;
		z += vec.z * scale;
	}
	
	/**
	 * Addition for all coordinates
	 * @param a Amount added to every component
	 */
	public void add(float a) {
		x += a;
		y += a;
		z += a;
	}
	
	/**
	 * Addition
	 * @param ax X component
	 * @param ay Y component
	 * @param az Z component
	 */
	public void add(float ax, float ay, float az) {
		x += ax;
		y += ay;
		z += az;
	}
	
	/**
	 * Scale per coordinate
	 * @param mx X factor
	 * @param my Y factor
	 * @param mz Z factor
	 */
	public void mul(float mx, float my, float mz) {
		x *= mx;
		y *= my;
		z *= mz;
	}
	
	/**
	 * Scale
	 * @param m Factor
	 */
	public void mul(float m) {
		x *= m;
		y *= m;
		z *= m;
	}
	
	/**
	 * Substract vector
	 * @param vec Vector to substract
	 */
	public void sub(Vec3f vec) {
		x -= vec.x;
		y -= vec.y;
		z -= vec.z;
	}
	
	/**
	 * Substract scaled vector
	 * @param vec Vector to substract, scaled by scale
	 * @param scale Scale applied to vec
	 */
	public void sub(Vec3f vec, float scale) {
		x -= vec.x * scale;
		y -= vec.y * scale;
		z -= vec.z * scale;
	}
	
	/**
	 * Shorthand for clamp(-range, range)<br>
	 * Clamps all coordinates within the given centered absolute range
	 * @see Vec3f#clamp(float, float)
	 */
	public void clamp(float range) {
		clamp(-range, range);
	}
	
	/**
	 * Clamps all coordinates
	 * @param mn Minimum value
	 * @param mx Maximum value
	 */
	public void clamp(float mn, float mx) {
		x = max(mn, min(mx, x));
		y = max(mn, min(mx, y));
		z = max(mn, min(mx, z));
	}
	
	/**
	 * Dot product
	 * @param vec Other Vector3F
	 * @return this·vec
	 */
	public float dot(Vec3f vec) {
		return this.x * vec.x + this.y * vec.y + this.z * vec.z;
	}
	
	/**
	 * Cross product<br>
	 * Sets this to this×vec
	 * @param vec Other Vector3F
	 */
	public void cross(Vec3f vec) {
		float x = this.x;
		float y = this.y;
		float z = this.z;
		this.x = y * vec.z - z * vec.y;
		this.y = z * vec.x - x * vec.z;
		this.z = x * vec.y - y * vec.x;
	}
	
	/**
	 * Reverse cross product<br>
	 * Sets this to vec×this
	 * @param vec Other vector
	 */
	public void crossReverse(Vec3f vec) {
		float x = this.x;
		float y = this.y;
		float z = this.z;
		this.x = vec.y * z - vec.z * y;
		this.y = vec.z * x - vec.x * z;
		this.z = vec.x * y - vec.y * x;
	}
	
	/**
	 * Compute norm
	 * @return ‖this‖²
	 */
	public float normSquared() {
		return x * x + y * y + z * z;
	}
	
	/**
	 * Compute norm
	 * @return ‖this‖
	 */
	public float norm() {
		return MathHelper.sqrt(x * x + y * y + z * z);
	}
	
	/**
	 * Compute the squared horizontal norm
	 * @return ‖this - (this·Y)Y‖²
	 */
	public float hNormSquared() {
		return x * x + z * z;
	}
	
	/**
	 * Compute the horizontal norm
	 * @return ‖this - (this·Y)Y‖
	 */
	public float hNorm() {
		return MathHelper.sqrt(x * x + z * z);
	}
	
	/**
	 * Compute distance to other vector
	 * @param other Other vector
	 * @return ‖this - other‖
	 */
	public float distance(Vec3f other) {
		return MathHelper.sqrt(distanceSquared(other));
	}
	
	/**
	 * Compute squared distance to other vector
	 * @param other Other vector
	 * @return ‖this - other‖²
	 */
	public float distanceSquared(Vec3f other) {
		float x_d = x - other.x;
		float y_d = y - other.y;
		float z_d = z - other.z;
		return x_d * x_d + y_d * y_d + z_d * z_d;
	}
	
	/**
	 * Scale as unitary. This must have sufficient norm.
	 */
	public void unitary() {
		float i = 1F / this.norm();
		x *= i;
		y *= i;
		z *= i;
	}
	
	/**
	 * Check if the norm of this vector is so small that
	 * operations which depend on the direction given by this
	 * vector become unstable, that is, computing the unitary
	 * vector is imprecise.
	 * @return true if the squared norm of this is smaller than 1E-8
	 * @see Vec3f#isZero(double)
	 * @see Vec3f#norm()
	 * @see Vec3f#normSquared()
	 */
	public boolean isZero() {
		return normSquared() < 1E-8F;
	}
	
	/**
	 * Check if the norm of this vector is smaller than a given epsilon
	 * @see Vec3f#isZero()
	 * @see Vec3f#norm()
	 * @see Vec3f#normSquared()
	 */
	public boolean isZero(double epsilon) {
		return normSquared() < epsilon * epsilon;
	}
	
	/**
	 * Transform to Vector3f
	 * @return new Vector3f
	 */
	public Vector3f toVector3f() {
		return new Vector3f(x, y, z);
	}
	
	/**
	 * Transform to Vector3d
	 * @return new Vector3d
	 */
	public Vector3d toVector3d() {
		return new Vector3d(x, y, z);
	}
	
	/**
	 * Transform to Vector3i, applies rounding
	 * @return new Vector3i
	 */
	public Vector3i toVector3i() {
		return new Vector3i(round(x), round(y), round(z));
	}
	
	/**
	 * Transform to {@link Vec3d}
	 * @return new Vec3d
	 */
	public Vec3d toVec3d() {
		return new Vec3d(x, y, z);
	}
	
	/**
	 * Packs a vector with each coordinate between 0~1 into
	 * a compressed int
	 * @return Compressed vector
	 * @see Vec3f#unpack
	 */
	public int pack() {
		return (round(x * 255F) & 0xFF) << 16 | (round(y * 255F) & 0xFF) << 8 | round(z * 255F) & 0xFF;
	}
	
	/**
	 * Unpacks vector from int
	 * @param packed Compressed vector
	 * @return Unpacked Vector3, with each coordinate within 0~1
	 * @see Vec3f#pack
	 */
	public static Vec3f unpack(int packed) {
		return new Vec3f(
		  (packed >> 16 & 255) / 255F,
		  (packed >> 8 & 255) / 255F,
		  (packed & 255) / 255F);
	}
	
	/**
	 * Serialize to buffer
	 * @param buf PacketBuffer
	 */
	public void write(PacketBuffer buf) {
		buf.writeFloat(x);
		buf.writeFloat(y);
		buf.writeFloat(z);
	}
	
	/**
	 * Read from PacketBuffer
	 * @param buf PacketBuffer
	 */
	public static Vec3f read(PacketBuffer buf) {
		return new Vec3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
	}
	
	/**
	 * Write into NBT
	 */
	public CompoundNBT toNBT() {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putFloat("x", x);
		nbt.putFloat("y", y);
		nbt.putFloat("z", z);
		return nbt;
	}
	
	/**
	 * Read from NBT
	 * @param nbt Compound NBT
	 */
	public static Vec3f fromNBT(CompoundNBT nbt) {
		return new Vec3f(
		  nbt.getFloat("x"), nbt.getFloat("y"), nbt.getFloat("z"));
	}
	
	/**
	 * Set values from NBT
	 * @param nbt Compound NBT
	 */
	public void readNBT(CompoundNBT nbt) {
		x = nbt.getFloat("x");
		y = nbt.getFloat("y");
		z = nbt.getFloat("z");
	}
	
	/**
	 * Linear interpolation
	 * @param vec Target vector
	 * @param t Progress (0~1)
	 */
	public void lerp(Vec3f vec, float t) {
		float r = 1.0F - t;
		x = x * r + vec.x * t;
		y = y * r + vec.y * t;
		z = z * r + vec.z * t;
	}
	
	/**
	 * Determine the average of a collection of vectors
	 * @param vectors Vector collection
	 * @return (∑vectors) / #vectors
	 */
	public static Vec3f average(Collection<Vec3f> vectors) {
		Vec3f res = ZERO.get();
		int n = 0;
		for (Vec3f vec : vectors) {
			res.add(vec);
			n++;
		}
		res.mul(1F/n);
		return res;
	}
	
	/**
	 * Serializes the vector as a float[]
	 * @return new float[]{x, y, z}
	 */
	public float[] asArray() {
		return new float[]{x, y, z};
	}
	
	/**
	 * Serializes the vector as a {@code List<Float>}
	 * @return List.of(x, y, z)
	 */
	public List<Float> asList() {
		List<Float> l = new ArrayList<>();
		l.add(x);
		l.add(y);
		l.add(z);
		return l;
	}
	
	/**
	 * Spherical coordinates getter<br>
	 * Assumes the vector is unitary
	 * @return The pitch of this vector, in degrees
	 */
	public float getPitch() {
		return - (float)asin(y) * TO_DEGREES;
	}
	
	/**
	 * Spherical coordinates getter
	 * The pitch is usually computed before<br>
	 * Assumes the vector is unitary
	 * @return The yaw of this vector in degrees, starting from Z clockwise
	 */
	public float getYaw() {
		return - (float) Math.atan2(x, z) * TO_DEGREES;
	}
	
	/**
	 * Copy vector
	 */
	public Vec3f copy() {
		return new Vec3f(this);
	}
	
	// Transformations
	
	/**
	 * General Quaternion transformation
	 * @param quaternion Transformation
	 */
	public void transform(Quaternion quaternion) {
		Quaternion q = new Quaternion(quaternion);
		q.mul(new Quaternion(x, y, z, 0F));
		Quaternion qc = new Quaternion(quaternion);
		qc.conj();
		q.mul(qc);
		x = q.i();
		y = q.j();
		z = q.k();
	}
	
	/**
	 * Obtain rotation quaternion using this as axis
	 * @param angle Angle of rotation, in radians
	 * @return Rotation quaternion
	 */
	public Quaternion rotation(float angle) {
		assert Float.compare(norm(), 1F) == 0F;
		final float s = sin(angle * 0.5F);
		return new Quaternion(x * s, y * s, z * s, cos(angle * 0.5F));
	}
	
	/**
	 * Obtain rotation quaternion using this as axis
	 * @param degrees Angle of rotation, in degrees
	 * @return Rotation quaternion
	 */
	public Quaternion rotationDegrees(float degrees) {
		return rotation(degrees * TO_RAD);
	}
	
	/**
	 * Rotate around axis vector counter-clockwise
	 * @param axis Axis vector, unitary
	 * @param angle Radians
	 */
	public void rotateAlongVec(Vec3f axis, float angle) {
		assert Float.compare(axis.norm(), 1F) == 0F;
		transform(axis.rotation(angle));
	}
	
	/**
	 * Rotates around an axis vector counter-clockwise
	 * @param axis Axis vector, unitary
	 * @param degrees Rotation
	 */
	public void rotateAlongVecDegrees(Vec3f axis, float degrees) {
		assert Float.compare(axis.norm(), 1F) == 0F;
		transform(axis.rotationDegrees(degrees));
	}
	
	/**
	 * Rotate around an orthogonal axis vector counter-clockwise
	 * @param axis Axis vector, unitary and orthogonal to toRotate
	 * @param angle Radians
	 */
	public void rotateAlongOrtVec(Vec3f axis, float angle) {
		assert Float.compare(axis.norm(), 1F) == 0F;
		Vec3f cross = axis.copy();
		cross.cross(this);
		cross.mul(sin(angle));
		this.mul(cos(angle));
		this.add(cross);
	}
	
	/**
	 * Rotate around an orthogonal axis vector counter-clockwise
	 * @param axis Axis vector, unitary and orthogonal to toRotate
	 * @param degrees Rotation
	 */
	public void rotateAlongOrtVecDegrees(Vec3f axis, float degrees) {
		rotateAlongOrtVec(axis, degrees * TO_RAD);
	}
	
	/**
	 * Measure the angle to another vector.
	 * The angle is within the range of 0~π.<br>
	 * If this and vec are known to be unitary, use
	 * {@link Vec3f#angleUnitary}
	 * @param vec Target vector
	 * @return The angle from this to vec in radians
	 */
	public float angle(Vec3f vec) {
		return (float) acos(MathHelper.clamp(dot(vec), -1F, 1F)) / (norm() * vec.norm());
	}
	
	/**
	 * Measure the angle to another vector.
	 * The angle is within the range of 0~180.<br>
	 * If this and vec are known to be unitary, use
	 * {@link Vec3f#angleUnitary}
	 * @param vec Target vector
	 * @return The angle from this to vec in degrees
	 */
	public float angleDegrees(Vec3f vec) {
		return angle(vec) * TO_DEGREES;
	}
	
	/**
	 * Measure the angle to another unitary vector.
	 * The angle is within the range of 0~π.
	 * This and vec must be unitary
	 * @param vec Target vector, unitary
	 * @return The angle from this to vec in radians
	 *
	 * @see Vec3f#angleUnitaryDegrees(Vec3f)
	 * @see Vec3f#angleUnitary(Vec3f, Vec3f)
	 */
	public float angleUnitary(Vec3f vec) {
		return (float) acos(MathHelper.clamp(dot(vec), -1F, 1F));
	}
	
	/**
	 * Measure the angle to another unitary vector.
	 * The angle is within the range of 0~180°.
	 * This and vec must be unitary
	 * @param vec Target vector, unitary
	 * @return The angle from this to vec in degrees
	 *
	 * @see Vec3f#angleUnitary(Vec3f)
	 * @see Vec3f#angleUnitaryDegrees(Vec3f, Vec3f)
	 */
	public float angleUnitaryDegrees(Vec3f vec) {
		return angleUnitary(vec) * TO_DEGREES;
	}
	
	/**
	 * Measure the angle to another unitary vector, within the plane
	 * described by another, orthogonal to both. The angle is measured
	 * following the right hand rule from the axis vector, from this
	 * to the other vector, and is within the range of 0~2π.
	 * This and vec must be unitary
	 * @param vec Target vector, unitary
	 * @param axis Orientation vector, orthogonal to this and vec
	 * @return The angle from this to vec in radians, counter-clockwise
	 * as seen from positive axis
	 *
	 * @see Vec3f#angleUnitaryDegrees(Vec3f, Vec3f)
	 * @see Vec3f#angleUnitary(Vec3f)
	 */
	public float angleUnitary(Vec3f vec, Vec3f axis) {
		float angle = (float) acos(MathHelper.clamp(this.dot(vec), -1F, 1F));
		Vec3f compare = axis.copy();
		compare.cross(this);
		return (vec.dot(compare) > 0)? angle : 2 * (float)Math.PI - angle;
	}
	
	/**
	 * Measure the angle to another unitary vector, within the plane
	 * described by another, orthogonal to both. The angle is measured
	 * following the right hand rule from the axis vector, from this
	 * to the other vector, and is within the range of 0~360°.
	 * This and vec must be unitary
	 * @param vec Target vector, unitary
	 * @param axis Orientation vector, orthogonal to this and vec
	 * @return The angle from this to vec in degrees, counter-clockwise
	 * as seen from positive axis
	 *
	 * @see Vec3f#angleUnitary(Vec3f, Vec3f)
	 * @see Vec3f#angleUnitaryDegrees(Vec3f)
	 */
	public float angleUnitaryDegrees(Vec3f vec, Vec3f axis) {
		return this.angleUnitary(vec, axis) * TO_DEGREES;
	}
	
	
	/**
	 * Measure the angle to another vector, within the plane described
	 * by another, not necessarily orthogonal to both.<br>
	 * The angle is measured following the right hand rule from the axis
	 * vector, from this to the other vector and is within the range of
	 * 0~2π. This and vec need not be unitary, but axis should be.<br>
	 * Both vectors are first projected to the plane where the angle is
	 * measured
	 * @param vec Target vector
	 * @param axis Orientation vector, unitary
	 * @return The angle in radians from this to vec as seen from
	 * the normal plane to axis
	 */
	public float angleProjected(Vec3f vec, Vec3f axis) {
		Vec3f this_p = copy();
		Vec3f vec_p = vec.copy();
		this_p.sub(axis, this_p.dot(axis));
		vec_p.sub(axis, vec_p.dot(axis));
		if (this_p.isZero() || vec_p.isZero())
			return 0F;
		this_p.unitary();
		vec_p.unitary();
		return this_p.angleUnitary(vec_p, axis);
	}
	
	
	/**
	 * Measure the angle to another vector, within the plane described
	 * by another, not necessarily orthogonal to both.<br>
	 * The angle is measured following the right hand rule from the axis
	 * vector, from this to the other vector and is within the range of
	 * 0~2π. This and vec need not be unitary, but axis should be.<br>
	 * Both vectors are first projected to the plane where the angle is
	 * measured
	 * @param vec Target vector
	 * @param axis Orientation vector, unitary
	 * @return The angle in degrees from this to vec as seen from
	 * the normal plane to axis
	 */
	public float angleProjectedDegrees(Vec3f vec, Vec3f axis) {
		return angleProjected(vec, axis) * TO_DEGREES;
	}
	
	/**
	 * Mirrors this across the axis generated by another vector
	 * @param axis Direction vector
	 */
	public void mirror(Vec3f axis) {
		float d = dot(axis) * 2F;
		x = d * axis.x - x;
		y = d * axis.y - y;
		z = d * axis.z - z;
	}
	
	/**
	 * Mirrors this across the axis generated by another
	 * vector, and inverts itself.<br>
	 * This is equivalent to calling
	 * <pre>{@code
	 *    this.mirror(axis);
	 *    this.mul(-1F);
	 * }</pre>
	 * and describes the action of reflecting this in the normal
	 * plane to axis.
	 * @param axis Normal vector of the plane of rotation
	 */
	public void reflect(Vec3f axis) {
		float d = dot(axis) * 2F;
		x = x - d * axis.x;
		y = y - d * axis.y;
		z = z - d * axis.z;
	}
	
	/**
	 * Create an orthogonal vector to this
	 */
	public Vec3f orthogonal() {
		return new Vec3f(y - z, z - x, x - y);
	}
	
	/**
	 * Create a vector orthogonal to this and unitary<br>
	 * If this is zero, XP will be returned
	 */
	public Vec3f orthogonalUnitary() {
		if (isZero())
			return XP.get();
		final Vec3f v = new Vec3f(y - z, z - x, x - y);
		v.unitary();
		return v;
	}
	
	/**
	 * Create two orthogonal vectors to this, orthogonal between them.
	 * Returned vectors are not always unitary
	 */
	public Vec3f[] orthogonalPair() {
		Vec3f u = new Vec3f(y - z, z - x, x - y);
		Vec3f v = u.copy();
		v.cross(this);
		return new Vec3f[] {u, v};
	}
	
	/**
	 * Create two orthogonal unitary vectors to this. This is not made
	 * unitary
	 */
	public Vec3f[] orthogonalUnitaryPair() {
		Vec3f[] ret = orthogonalPair();
		ret[0].unitary();
		ret[1].unitary();
		return ret;
	}
	
	// Memory optimizations
	
	/**
	 * Copy values from Vector3i
	 * @param vec Values source
	 */
	public void set(Vector3i vec) {
		x = vec.getX();
		y = vec.getY();
		z = vec.getZ();
	}
	
	/**
	 * Copy values from Vector3f
	 * @param vec Values source
	 */
	public void set(Vector3f vec) {
		x = vec.x();
		y = vec.y();
		z = vec.z();
	}
	
	/**
	 * Copy values from Vector3d
	 * @param vec Values source
	 */
	public void set(Vector3d vec) {
		x = (float)vec.x;
		y = (float)vec.y;
		z = (float)vec.z;
	}
	
	/**
	 * Set values
	 * @param x X component
	 * @param y Y component
	 * @param z Z component
	 */
	public void set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	/**
	 * Set values
	 * @param x X component
	 * @param y Y component
	 * @param z Z component
	 */
	public void set(double x, double y, double z) {
		this.x = (float)x;
		this.y = (float)y;
		this.z = (float)z;
	}
	
	/**
	 * Copy values from Vector3
	 * @param vec Values source
	 */
	public void set(Vec3f vec) {
		x = vec.x;
		y = vec.y;
		z = vec.z;
	}
	
	/**
	 * Copy values from {@link Vec3d}
	 * @param vec Values source
	 */
	public void set(Vec3d vec) {
		x = (float)vec.x;
		y = (float)vec.y;
		z = (float)vec.z;
	}
	
	/**
	 * Update the vector from spherical coordinates, as used by Minecraft<br>
	 * The vector will be made unitary.
	 * The Y axis is used as height. The Z axis is the Yaw origin, and Yaw is
	 * measured clockwise as seen from positive Y, with the axis following the
	 * right hand rule, that is, X × Y = Z
	 * @param yaw Spherical yaw in degrees, from Z axis, clockwise
	 * @param pitch Spherical pitch in degrees, from XZ plane
	 */
	public void set(float yaw, float pitch) {
		set(yaw, pitch, true);
	}
	
	/**
	 * Update the vector from spherical coordinates, as used by Minecraft<br>
	 * The vector will be made unitary.
	 * The Y axis is used as height. The Z axis is the Yaw origin, and Yaw is
	 * measured clockwise as seen from positive Y, with the axis following the
	 * right hand rule, that is, X × Y = Z
	 *
	 * @param yaw Spherical yaw, from Z axis, clockwise
	 * @param pitch Spherical pitch, from XZ plane
	 * @param degrees True if yaw and pitch are given in degrees, default
	 */
	public void set(float yaw, float pitch, boolean degrees) {
		if (degrees) {
			pitch = pitch * TO_RAD;
			yaw = yaw * TO_RAD;
		}
		float yawCos = (float)Math.cos(yaw);
		float yawSin = (float)Math.sin(yaw);
		float pitCos = (float)Math.cos(pitch);
		float pitSin = (float)Math.sin(pitch);
		x = -yawSin * pitCos;
		y = -pitSin;
		z = yawCos * pitCos;
	}
	
	
	/**
	 * Update the vector randomly, with each coordinate ranging
	 * between 0~1. Uniformly distributed as provided by
	 * {@link Random#nextFloat}
	 */
	public void setRandom() {
		x = random.nextFloat();
		y = random.nextFloat();
		z = random.nextFloat();
	}
	
	/**
	 * Update the vector randomly, with each coordinate within -range~range,
	 * uniformly distributed as provided by {@link Random#nextFloat}<br>
	 * Equivalent to calling {@code Vector3.random(-range, range)}
	 * @param range Absolute bound for each coordinate
	 */
	public void setRandom(float range) {
		setRandom();
		mul(2 * range);
		add(-range);
	}
	
	/**
	 * Update the vector randomly, with each coordinate within the
	 * given range. Uniformly distributed as provided by
	 * {@link Random#nextFloat}.
	 * @param min Lower bound
	 * @param max Upper bound
	 */
	public void setRandom(float min, float max) {
		setRandom();
		mul(max - min);
		add(min);
	}
	
	/**
	 * Update the vector randomly, the result is uniformly distributed
	 * across the sphere, by means of the Lambert cylindrical equal-area
	 * projection, as provided by {@link Random#nextFloat}.
	 */
	public void setRandomUnitary() {
		float u = random.nextFloat() * 2 * PI;
		float v = -1F + random.nextFloat() * 2;
		// Short-circuited spherical coordinates conversion
		float h = (float)sqrt(1F - v * v);
		x = cos(u) * h;
		y = sin(u) * h;
		z = v;
	}
	
	
	/**
	 * Update the vector randomly, the result has norm &#8804; 1,
	 * and is uniformly distributed across the sphere in terms of
	 * volume, by means of the Lambert cylindrical equal-area
	 * projection, multiplied by a random radius between 0~1
	 * quadratically distributed.
	 *
	 * The pseudo-random generation is provided by {@link Random#nextFloat()}
	 */
	public void setRandomSpherical() {
		final float u = random.nextFloat() * 2F * PI;
		final float v = -1F + random.nextFloat() * 2F;
		// √X where X is uniformly distributed follows a quadratic distribution
		final float r = (float)sqrt(random.nextFloat());
		// Short-circuited spherical coordinates conversion
		final float h = (float)sqrt(1F - v * v);
		x = cos(u) * h * r;
		y = sin(u) * h * r;
		z = v * r;
	}
	
	/**
	 * Set this orthogonal to other vector<br>
	 * The result may be zero if the vectors are linearly dependent<br>
	 * For a guaranteed non-zero result, see {@link Vec3f#setOrthogonalUnitary(Vec3f)}
	 */
	public void setOrthogonal(Vec3f other) {
		sub(other, dot(other));
	}
	
	/**
	 * Set this orthogonal to other vector and unitary<br>
	 * If both this and other are zero, XP is returned
	 */
	public void setOrthogonalUnitary(Vec3f other) {
		if (other.isZero() && isZero()) {
			x = 1F;
			y = 0F;
			z = 0F;
		} else {
			sub(other, dot(other));
			if (isZero()) {
				x = other.y - other.z;
				y = other.z - other.x;
				z = other.x - other.y;
			}
			unitary();
		}
	}
	
	/**
	 * Update two vectors to contain orthogonal vectors to this, which
	 * may not be unitary
	 */
	public void setOrthogonal(Vec3f u, Vec3f v) {
		u.set(y - z, z - x, x - y);
		v.set(u);
		v.cross(this);
	}
	
	/**
	 * Update two vectors to contain orthogonal vectors to this, and
	 * makes them unitary (not this)
	 */
	public void setOrthogonalUnitary(Vec3f u, Vec3f v) {
		u.set(y - z, z - x, x - y);
		u.unitary();
		v.set(u);
		v.cross(this);
		v.unitary();
	}
	
	/**
	 * Get a vector for an {@link Axis}
	 * @return Vector for axis, or 0 if null
	 */
	public static Vec3f forAxis(Axis axis) {
		if (axis == null)
			return ZERO.get();
		switch (axis) {
			case X: return XP.get();
			case Y: return YP.get();
			case Z: return ZP.get();
			default: throw new IllegalArgumentException("Unknown axis: " + axis);
		}
	}
	
	/**
	 * Get a vector for a {@link Direction}
	 * @return Vector for direction, or 0 if null
	 */
	public static Vec3f forDirection(Direction direction) {
		if (direction == null)
			return ZERO.get();
		switch (direction) {
			case UP: return YP.get();
			case DOWN: return YN.get();
			case EAST: return XP.get();
			case WEST: return XN.get();
			case SOUTH: return ZP.get();
			case NORTH: return ZN.get();
			default: throw new IllegalArgumentException("Unknown direction: " + direction);
		}
	}
	
	// Default vectors
	public static final Supplier<Vec3f> ZERO = () -> new Vec3f(0F, 0F, 0F);
	public static final Supplier<Vec3f> XP = () -> new Vec3f(1F, 0F, 0F);
	public static final Supplier<Vec3f> YP = () -> new Vec3f(0F, 1F, 0F);
	public static final Supplier<Vec3f> ZP = () -> new Vec3f(0F, 0F, 1F);
	public static final Supplier<Vec3f> XN = () -> new Vec3f(-1F, 0F, 0F);
	public static final Supplier<Vec3f> YN = () -> new Vec3f(0F, -1F, 0F);
	public static final Supplier<Vec3f> ZN = () -> new Vec3f(0F, 0F, -1F);
	
	// Object overrides
	/**
	 * Equality check
	 * @param obj Other
	 * @return this = obj
	 */
	@Override public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj != null && this.getClass() == obj.getClass()) {
			Vec3f v = (Vec3f)obj;
			if (Float.compare(v.x, x) != 0) {
				return false;
			} else if (Float.compare(v.y, y) != 0) {
				return false;
			} else {
				return Float.compare(v.z, z) == 0;
			}
		} else {
			return false;
		}
	}
	
	/**
	 * Hash code
	 */
	@Override public int hashCode() {
		int i = Float.floatToIntBits(x);
		i = 31 * i + Float.floatToIntBits(y);
		return 31 * i + Float.floatToIntBits(z);
	}
	
	/**
	 * Uses {@link Vec3f#defaultFormat}.
	 */
	@Override public String toString() {
		//return "[" + x + ", " + y + ", " + z + ']';
		return toString(defaultFormat);
	}
	
	/**
	 * Default number format used by {@link Vec3f#toString()}
	 */
	public static String defaultFormat = "%+6.3f";
	
	/**
	 * Format using the provided number format
	 * @param fmt Number format
	 */
	public String toString(String fmt) {
		return "[" + String.format(fmt, x) +
		       ", " + String.format(fmt, y) +
		       ", " + String.format(fmt, z) + ']';
	}
}
