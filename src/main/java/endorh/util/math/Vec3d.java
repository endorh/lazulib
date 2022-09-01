package endorh.util.math;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;

import static java.lang.Math.*;

/**
 * Mutable 3D double vector implementation as an alternative to
 * {@link Vec3}, with unified interface across server/client side.<br>
 * It does not necessarily support all methods that {@link Vec3} offers.
 * It is also not thread-safe, since it's mutable.<br><br>
 * Some features:
 * <ul>
 *   <li>Standard vector operations</li>
 *   <li>Several rotation methods</li>
 *   <li>Conversion from/to {@link Vec3}, {@link Vector3f} and {@link Vec3i}.</li>
 *   <li>Conversion from/to spherical coordinates</li>
 *   <li>De/serialization from/into {@link FriendlyByteBuf}</li>
 *   <li>Various random vector generation methods</li>
 * </ul>
 * </ul>
 * @see Vec3f
 */
@SuppressWarnings("unused")
public class Vec3d {
	public double x;
	public double y;
	public double z;
	
	public static final Codec<Vec3d> CODEC = RecordCodecBuilder.create(
	  instance -> instance.group(
		 Codec.DOUBLE.fieldOf("x").forGetter(d -> d.x),
		 Codec.DOUBLE.fieldOf("y").forGetter(d -> d.y),
		 Codec.DOUBLE.fieldOf("z").forGetter(d -> d.z)
	  ).apply(instance, Vec3d::new)
	);
	
	
	/**
	 * Degrees to radians factor
	 */
	public static final double TO_RAD = Math.PI / 180D;
	/**
	 * Radians to degrees factor
	 */
	public static final double TO_DEGREES = 180D / Math.PI;
	/**
	 * π
	 */
	public static final double PI = Math.PI;
	/**
	 * π/2
	 */
	public static final double PI_HALF = Math.PI * 0.5D;
	/**
	 * Random generator
	 */
	private static final Random random = new Random();
	
	/**
	 * Used by other constructors
	 */
	private Vec3d() {}
	
	/**
	 * Create a new vector
	 * @param x X component
	 * @param y Y component
	 * @param z Z component
	 */
	public Vec3d(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	/**
	 * Create copy vector
	 * @param vec Vec3d to copy
	 */
	public Vec3d(Vec3d vec) {
		x = vec.x;
		y = vec.y;
		z = vec.z;
	}
	
	/**
	 * Create copy vector
	 * @param vec {@link Vec3f} to copy
	 */
	public Vec3d(Vec3f vec) {
		x = vec.x;
		y = vec.y;
		z = vec.z;
	}
	
	/**
	 * Create copy vector
	 * @param vec Vector3i to copy
	 */
	public Vec3d(Vec3i vec) {
		x = vec.getX();
		y = vec.getY();
		z = vec.getZ();
	}
	
	/**
	 * Create copy vector
	 * @param vec Vector3i to copy
	 * @param center true if coordinates should be centered
	 */
	public Vec3d(Vec3i vec, boolean center) {
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
	public Vec3d(Vector3f vec) {
		x = vec.x();
		y = vec.y();
		z = vec.z();
	}
	
	/**
	 * Create copy vector
	 * @param vec Vector3d to copy
	 */
	public Vec3d(Vec3 vec) {
		x = vec.x;
		y = vec.y;
		z = vec.z;
	}
	
	/**
	 * Create unitary vector from spherical coordinates, as used by Minecraft<br>
	 * The Y axis is used as height. The Z axis is the Yaw origin, and Yaw is
	 * measured clockwise as seen from positive Y, with the axis following the
	 * right hand rule, that is, X × Y = Z
	 * @param yaw Spherical yaw in degrees, from Z axis, clockwise
	 * @param pitch Spherical pitch in degrees, from XZ plane
	 */
	public Vec3d(double yaw, double pitch) {
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
	public Vec3d(double yaw, double pitch, boolean degrees) {
		if (degrees) {
			pitch = pitch * TO_RAD;
			yaw = yaw * TO_RAD;
		}
		double yawCos = Math.cos(yaw);
		double yawSin = Math.sin(yaw);
		double pitCos = Math.cos(pitch);
		double pitSin = Math.sin(pitch);
		x = -yawSin * pitCos;
		y = -pitSin;
		z = yawCos * pitCos;
	}
	
	/**
	 * Create vector from double[]. The first three elements of the
	 * list will be stored as x, y and z respectively<br>
	 * Is the inverse of {@link Vec3d#asArray()}.
	 * @param array Array containing the coordinates, must have at least
	 *             three elements
	 */
	public Vec3d(double[] array) {
		try {
			x = array[0];
			y = array[1];
			z = array[2];
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException(
			  "Unable to read Vec3d from array, too few elements");
		}
	}
	
	/**
	 * Create vector from {@code List<Double>}. The first three elements of the list will
	 * be stored as x, y and z respectively<br>
	 * Is the inverse of {@link Vec3d#asList}
	 * @param list List containing the coordinates, must have at least three elements
	 */
	public Vec3d(List<Double> list) {
		try {
			x = list.get(0);
			y = list.get(1);
			z = list.get(2);
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException(
			  "Unable to read Vec3d from list, too few elements");
		}
	}
	
	/**
	 * Creates a random vector, with each coordinate ranging
	 * between 0~1. Uniformly distributed as provided by
	 * {@link Random#nextDouble}
	 */
	public static Vec3d random() {
		return new Vec3d(random.nextDouble(), random.nextDouble(), random.nextDouble());
	}
	
	/**
	 * Creates a random vector, with each coordinate within -range~range,
	 * uniformly distributed as provided by {@link Random#nextDouble}<br>
	 * Equivalent to calling {@code Vec3d.random(-range, range)}
	 * @param range Absolute bound for each coordinate
	 */
	public static Vec3d random(double range) {
		Vec3d res = Vec3d.random();
		res.mul(2 * range);
		res.add(-range);
		return res;
	}
	
	/**
	 * Creates a random vector, with each coordinate within the
	 * given range. Uniformly distributed as provided by
	 * {@link Random#nextDouble}.
	 * @param min Lower bound
	 * @param max Upper bound
	 */
	public static Vec3d random(double min, double max) {
		Vec3d res = Vec3d.random();
		res.mul(max - min);
		res.add(min);
		return res;
	}
	
	/**
	 * Creates a random unitary vector, uniformly distributed across
	 * the sphere's surface, by means of the Lambert cylindrical equal-area
	 * projection, as provided by {@link Random#nextDouble}.
	 */
	public static Vec3d randomUnitary() {
		final double u = random.nextDouble() * 2 * PI;
		final double v = -1F + random.nextDouble() * 2;
		// Short-circuited spherical coordinates conversion
		final double h = sqrt(1F - v * v);
		return new Vec3d(cos(u) * h, sin(u) * h, v);
	}
	
	/**
	 * Creates a random vector of norm &#8804; 1, uniformly distributed
	 * across the sphere in terms of volume, by means of the
	 * Lambert cylindrical equal-area projection, multiplied by a
	 * random radius between 0~1 quadratically distributed.
	 *
	 * The pseudo-random generation is provided by {@link Random#nextDouble()}
	 */
	public static Vec3d randomSpherical() {
		final double u = random.nextDouble() * 2 * PI;
		final double v = -1F + random.nextDouble() * 2;
		// √X where X is uniformly distributed follows a quadratic distribution
		final double r = Math.sqrt(random.nextDouble());
		// Short-circuited spherical coordinates conversion
		final double h = sqrt(1F - v * v);
		return new Vec3d(cos(u) * h * r, sin(u) * h * r, v * r);
	}
	
	/**
	 * Add vector
	 * @param vec Vector to add
	 */
	public void add(Vec3d vec) {
		x += vec.x;
		y += vec.y;
		z += vec.z;
	}
	
	/**
	 * Add vector
	 * @param vec Vector to add
	 */
	public void add(Vec3f vec) {
		x += vec.x;
		y += vec.y;
		z += vec.z;
	}
	
	/**
	 * Add scaled vector
	 * @param vec Vector to add, scaled by scale.
	 *            Unmodified by the call.
	 * @param scale Scale to apply to vec before addition
	 */
	public void add(Vec3d vec, double scale) {
		x += vec.x * scale;
		y += vec.y * scale;
		z += vec.z * scale;
	}
	
	/**
	 * Add scaled vector
	 * @param vec Vector to add, scaled by scale. Unmodified by the call
	 * @param scale Scale to apply to vec before addition
	 */
	public void add(Vec3f vec, double scale) {
		x += vec.x * scale;
		y += vec.y * scale;
		z += vec.z * scale;
	}
	
	/**
	 * Add scalar
	 * @param a Amount added to every component
	 */
	public void add(double a) {
		x += a;
		y += a;
		z += a;
	}
	
	/**
	 * Add scalars to each component
	 * @param ax X component
	 * @param ay Y component
	 * @param az Z component
	 */
	public void add(double ax, double ay, double az) {
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
	public void mul(double mx, double my, double mz) {
		x *= mx;
		y *= my;
		z *= mz;
	}
	
	/**
	 * Product with scalar
	 * @param m Factor
	 */
	public void mul(double m) {
		x *= m;
		y *= m;
		z *= m;
	}
	
	/**
	 * Substract vector
	 * @param vec Vector to substract
	 */
	public void sub(Vec3d vec) {
		x -= vec.x;
		y -= vec.y;
		z -= vec.z;
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
	public void sub(Vec3d vec, double scale) {
		x -= vec.x * scale;
		y -= vec.y * scale;
		z -= vec.z * scale;
	}
	
	/**
	 * Substract scaled vector
	 * @param vec Vector to substract, scaled by scale
	 * @param scale Scale applied to vec
	 */
	public void sub(Vec3f vec, double scale) {
		x -= vec.x * scale;
		y -= vec.y * scale;
		z -= vec.z * scale;
	}
	
	/**
	 * Shorthand for clamp(-range, range)<br>
	 * Clamps all coordinates within the given centered absolute range
	 * @see Vec3d#clamp(double, double)
	 */
	public void clamp(double range) {
		clamp(-range, range);
	}
	
	/**
	 * Clamps all coordinates
	 * @param mn Minimum value
	 * @param mx Maximum value
	 */
	public void clamp(double mn, double mx) {
		x = max(mn, min(mx, x));
		y = max(mn, min(mx, y));
		z = max(mn, min(mx, z));
	}
	
	/**
	 * Dot product
	 * @param vec Other vector
	 * @return this·vec
	 */
	public double dot(Vec3d vec) {
		return x * vec.x + y * vec.y + z * vec.z;
	}
	
	/**
	 * Dot product
	 * @param vec Other vector
	 * @return this·vec
	 */
	public double dot(Vec3f vec) {
		return x * vec.x + y * vec.y + z * vec.z;
	}
	
	/**
	 * Cross product<br>
	 * Sets this to this×vec
	 * @param vec Other vector
	 */
	public void cross(Vec3d vec) {
		double x = this.x;
		double y = this.y;
		double z = this.z;
		this.x = y * vec.z - z * vec.y;
		this.y = z * vec.x - x * vec.z;
		this.z = x * vec.y - y * vec.x;
	}
	
	/**
	 * Cross product<br>
	 * Sets this to this×vec
	 * @param vec Other vector
	 */
	public void cross(Vec3f vec) {
		double x = this.x;
		double y = this.y;
		double z = this.z;
		this.x = y * vec.z - z * vec.y;
		this.y = z * vec.x - x * vec.z;
		this.z = x * vec.y - y * vec.x;
	}
	
	/**
	 * Reverse cross product<br>
	 * Sets this to vec×this
	 * @param vec Other vector
	 */
	public void reverseCross(Vec3d vec) {
		double x = this.x;
		double y = this.y;
		double z = this.z;
		this.x = vec.y * z - vec.z * y;
		this.y = vec.z * x - vec.x * z;
		this.z = vec.x * y - vec.y * x;
	}
	
	
	/**
	 * Reverse cross product<br>
	 * Sets this to vec×this
	 * @param vec Other vector
	 */
	public void reverseCross(Vec3f vec) {
		double x = this.x;
		double y = this.y;
		double z = this.z;
		this.x = vec.y * z - vec.z * y;
		this.y = vec.z * x - vec.x * z;
		this.z = vec.x * y - vec.y * x;
	}
	
	/**
	 * Squared 2-norm
	 * @return ‖this‖²
	 */
	public double normSquared() {
		return x * x + y * y + z * z;
	}
	
	/**
	 * 2-norm
	 * @return ‖this‖
	 */
	public double norm() {
		return sqrt(x * x + y * y + z * z);
	}
	
	/**
	 * Squared horizontal 2-norm
	 * @return ‖this - (this·Y)Y‖²
	 */
	public double hNormSquared() {
		return x * x + z * z;
	}
	
	/**
	 * Compute the horizontal norm
	 * @return ‖this - (this·Y)Y‖
	 */
	public double hNorm() {
		return sqrt(x * x + z * z);
	}
	
	/**
	 * Compute distance to other vector
	 * @param other Other vector
	 * @return ‖this - other‖
	 */
	public double distance(Vec3d other) {
		return sqrt(distanceSquared(other));
	}
	
	/**
	 * Compute squared distance to other vector
	 * @param other Other vector
	 * @return ‖this - other‖²
	 */
	public double distanceSquared(Vec3d other) {
		double x_d = x - other.x;
		double y_d = y - other.y;
		double z_d = z - other.z;
		return x_d * x_d + y_d * y_d + z_d * z_d;
	}
	
	/**
	 * Scale as unitary.<br>
	 * this must have sufficient norm.
	 */
	public void unitary() {
		double i = 1D / norm();
		x *= i;
		y *= i;
		z *= i;
	}
	
	
	/**
	 * Check if the norm of this vector is so small that
	 * operations which depend on the direction given by this
	 * vector become unstable, that is, computing the unitary vector
	 * is imprecise.
	 * @return true if the squared norm of this is smaller than 1E-24
	 * @see Vec3d#isZero(double)
	 * @see Vec3d#norm()
	 * @see Vec3d#normSquared()
	 */
	public boolean isZero() {
		return normSquared() < 1E-24D;
	}
	
	/**
	 * Check if the norm of this vector is smaller than a given epsilon
	 * @see Vec3d#isZero()
	 * @see Vec3d#norm()
	 * @see Vec3d#normSquared()
	 */
	public boolean isZero(double epsilon) {
		return normSquared() < epsilon * epsilon;
	}
	
	/**
	 * Transform to {@link Vec3f}
	 * @return new Vector3
	 */
	public Vec3f toVector3() {
		return new Vec3f((float)x, (float)y, (float)z);
	}
	
	/**
	 * Transform to Vector3f
	 * @return new Vector3f
	 */
	public Vector3f toVector3f() {
		return new Vector3f((float)x, (float)y, (float)z);
	}
	
	/**
	 * Transform to Vector3d
	 * @return new Vector3d
	 */
	public Vec3 toVector3d() {
		return new Vec3(x, y, z);
	}
	
	/**
	 * Transform to Vector3i, applies rounding
	 * @return new Vector3i
	 */
	public Vec3i toVector3i() {
		return new Vec3i(round(x), round(y), round(z));
	}
	
	/**
	 * Packs a vector with each coordinate between 0~1 into
	 * a compressed int
	 * @return Compressed vector
	 * @see Vec3d#unpack
	 */
	public int pack() {
		return (round((float)x * 255F) & 0xFF) << 16
		       | (round((float)y * 255F) & 0xFF) << 8
		       | round((float)z * 255F) & 0xFF;
	}
	
	/**
	 * Unpacks vector from int
	 * @param packed Compressed vector
	 * @return Unpacked Vec3d, with each coordinate within 0~1
	 * @see Vec3d#pack
	 */
	public static Vec3d unpack(int packed) {
		return new Vec3d(
		  (packed >> 16 & 255) / 255F,
		  (packed >> 8 & 255) / 255F,
		  (packed & 255) / 255F);
	}
	
	/**
	 * Serialize to buffer
	 * @param buf PacketBuffer
	 */
	public void write(FriendlyByteBuf buf) {
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
	}
	
	/**
	 * Read from PacketBuffer
	 * @param buf PacketBuffer
	 */
	public static Vec3d read(FriendlyByteBuf buf) {
		return new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
	}
	
	/**
	 * Read as command argument with syntax {@code %f %f %f}.
	 */
	public static Vec3d readCommand(StringReader reader) throws CommandSyntaxException {
		double x = reader.readDouble();
		reader.expect(' ');
		double y = reader.readDouble();
		reader.expect(' ');
		double z = reader.readDouble();
		return new Vec3d(x, y, z);
	}
	
	/**
	 * Write as command argument.
	 */
	public String writeCommand() {
		return String.format("%.3f %.3f %.3f", x, y, z);
	}
	
	/**
	 * Write into NBT
	 */
	public CompoundTag toNBT() {
		CompoundTag nbt = new CompoundTag();
		nbt.putDouble("x", x);
		nbt.putDouble("y", y);
		nbt.putDouble("z", z);
		return nbt;
	}
	
	/**
	 * Read from NBT
	 * @param nbt Compound NBT
	 */
	public static Vec3d fromNBT(CompoundTag nbt) {
		return new Vec3d(
		  nbt.getDouble("x"), nbt.getDouble("y"), nbt.getDouble("z"));
	}
	
	/**
	 * Set values from NBT
	 * @param nbt Compound NBT
	 */
	public void readNBT(CompoundTag nbt) {
		x = nbt.getDouble("x");
		y = nbt.getDouble("y");
		z = nbt.getDouble("z");
	}
	
	/**
	 * Linear interpolation
	 * @param vec Target vector
	 * @param t Progress (0~1)
	 */
	public void lerp(Vec3d vec, double t) {
		double r = 1D - t;
		x = x * r + vec.x * t;
		y = y * r + vec.y * t;
		z = z * r + vec.z * t;
	}
	
	/**
	 * Determine the average of a collection of vectors
	 * @param vectors Vector collection
	 * @return (∑vectors) / #vectors
	 */
	public static Vec3d average(Collection<Vec3d> vectors) {
		Vec3d res = ZERO.get();
		int n = 0;
		for (Vec3d vec : vectors) {
			res.add(vec);
			n++;
		}
		res.mul(1F/n);
		return res;
	}
	
	/**
	 * Serializes the vector as a double[]
	 * @return new double[]{x, y, z}
	 */
	public double[] asArray() {
		return new double[]{x, y, z};
	}
	
	/**
	 * Serializes the vector as a {@code List<Double>}
	 * @return List.of(x, y, z)
	 */
	public List<Double> asList() {
		List<Double> l = new ArrayList<>();
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
	public double getPitch() {
		return -asin(y) * TO_DEGREES;
	}
	
	/**
	 * Spherical coordinates getter<br>
	 * The pitch is usually computed before<br>
	 * Assumes the vector is unitary
	 * @return The yaw of this vector in degrees, starting from Z clockwise
	 */
	public double getYaw() {
		return - Math.atan2(x, z) * TO_DEGREES;
	}
	
	/**
	 * Copy vector
	 */
	public Vec3d copy() {
		return new Vec3d(this);
	}
	
	// Transformations
	
	/**
	 * General Quaternion transformation
	 * @param quaternion Transformation
	 */
	public void transform(Quaternion quaternion) {
		Quaternion q = new Quaternion(quaternion);
		q.mul(new Quaternion((float)x, (float)y, (float)z, 0F));
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
	public Quaternion rotation(double angle) {
		final double s = sin(angle * 0.5D);
		return new Quaternion((float)(x * s), (float)(y * s), (float)(z * s),
		                      (float)cos(angle * 0.5D));
	}
	
	/**
	 * Obtain rotation quaternion using this as axis
	 * @param degrees Angle of rotation, in degrees
	 * @return Rotation quaternion
	 */
	public Quaternion rotationDegrees(double degrees) {
		return rotation(degrees * TO_RAD);
	}
	
	/**
	 * Rotate around axis vector counter-clockwise
	 * @param axis Axis vector, unitary
	 * @param angle Radians
	 */
	public void rotateAlongVec(Vec3d axis, double angle) {
		transform(axis.rotation(angle));
	}
	
	/**
	 * Rotates around an axis vector counter-clockwise
	 * @param axis Axis vector, unitary
	 * @param degrees Rotation
	 */
	public void rotateAlongVecDegrees(Vec3d axis, double degrees) {
		transform(axis.rotationDegrees(degrees));
	}
	
	/**
	 * Rotate around an orthogonal axis vector counter-clockwise
	 * @param axis Axis vector, unitary and orthogonal to toRotate
	 * @param angle Radians
	 */
	public void rotateAlongOrtVec(Vec3d axis, double angle) {
		Vec3d cross = axis.copy();
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
	public void rotateAlongOrtVecDegrees(Vec3d axis, double degrees) {
		rotateAlongOrtVec(axis, degrees * TO_RAD);
	}
	
	/**
	 * Measure the angle to another vector.
	 * The angle is within the range of 0~π.<br>
	 * If this and vec are known to be unitary, use
	 * {@link Vec3d#angleUnitary}
	 * @param vec Target vector
	 * @return The angle from this to vec in radians
	 */
	public double angle(Vec3d vec) {
		return acos(Mth.clamp(dot(vec), -1D, 1D)) / (norm() * vec.norm());
	}
	
	/**
	 * Measure the angle to another vector.
	 * The angle is within the range of 0~180.<br>
	 * If this and vec are known to be unitary, use
	 * {@link Vec3d#angleUnitary}
	 * @param vec Target vector
	 * @return The angle from this to vec in degrees
	 */
	public double angleDegrees(Vec3d vec) {
		return angle(vec) * TO_DEGREES;
	}
	
	/**
	 * Measure the angle to another unitary vector.
	 * The angle is within the range of 0~π.
	 * This and vec must be unitary
	 * @param vec Target vector, unitary
	 * @return The angle from this to vec in radians
	 *
	 * @see Vec3d#angleUnitaryDegrees(Vec3d)
	 * @see Vec3d#angleUnitary(Vec3d, Vec3d)
	 */
	public double angleUnitary(Vec3d vec) {
		return acos(Mth.clamp(dot(vec), -1F, 1F));
	}
	
	/**
	 * Measure the angle to another unitary vector.
	 * The angle is within the range of 0~180°.
	 * This and vec must be unitary
	 * @param vec Target vector, unitary
	 * @return The angle from this to vec in degrees
	 *
	 * @see Vec3d#angleUnitary(Vec3d)
	 * @see Vec3d#angleUnitaryDegrees(Vec3d, Vec3d)
	 */
	public double angleUnitaryDegrees(Vec3d vec) {
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
	 * @see Vec3d#angleUnitaryDegrees(Vec3d, Vec3d)
	 * @see Vec3d#angleUnitary(Vec3d)
	 */
	public double angleUnitary(Vec3d vec, Vec3d axis) {
		double angle = acos(Mth.clamp(dot(vec), -1D, 1D));
		Vec3d compare = axis.copy();
		compare.cross(this);
		return (vec.dot(compare) > 0)? angle : 2 * Math.PI - angle;
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
	 * @see Vec3d#angleUnitary(Vec3d, Vec3d)
	 * @see Vec3d#angleUnitaryDegrees(Vec3d)
	 */
	public double angleUnitaryDegrees(Vec3d vec, Vec3d axis) {
		return angleUnitary(vec, axis) * TO_DEGREES;
	}
	
	/**
	 * Mirrors this across the axis generated by another vector
	 * @param axis Direction vector
	 */
	public void mirror(Vec3d axis) {
		final double d = dot(axis) * 2D;
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
	public void reflect(Vec3d axis) {
		final double d = dot(axis) * 2D;
		x = x - d * axis.x;
		y = y - d * axis.y;
		z = z - d * axis.z;
	}
	
	/**
	 * Create an orthogonal vector to this
	 */
	public Vec3d orthogonal() {
		return new Vec3d(y - z, z - x, x - y);
	}
	
	/**
	 * Create a vector orthogonal to this and unitary<br>
	 * If this is zero, XP will be returned
	 */
	public Vec3d orthogonalUnitary() {
		if (isZero())
			return XP.get();
		final Vec3d v = new Vec3d(y - z, z - x, x - y);
		v.unitary();
		return v;
	}
	
	/**
	 * Create two orthogonal vectors to this, orthogonal between them.
	 * Returned vectors are not always unitary
	 */
	public Vec3d[] orthogonalPair() {
		Vec3d u = new Vec3d(y - z, z - x, x - y);
		Vec3d v = u.copy();
		v.cross(this);
		return new Vec3d[] {u, v};
	}
	
	/**
	 * Create two orthogonal unitary vectors to this. This is not made
	 * unitary
	 */
	public Vec3d[] orthogonalUnitaryPair() {
		Vec3d[] ret = orthogonalPair();
		ret[0].unitary();
		ret[1].unitary();
		return ret;
	}
	
	// In place setters
	
	/**
	 * Copy values from {@link Vec3f}
	 * @param vec Values source
	 */
	public void set(Vec3f vec) {
		x = vec.x;
		y = vec.y;
		z = vec.z;
	}
	
	/**
	 * Copy values from Vector3i
	 * @param vec Values source
	 */
	public void set(Vec3i vec) {
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
	public void set(Vec3 vec) {
		x = vec.x;
		y = vec.y;
		z = vec.z;
	}
	
	/**
	 * Set values
	 * @param x X component
	 * @param y Y component
	 * @param z Z component
	 */
	public void set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	/**
	 * Copy values from Vec3d
	 * @param vec Values source
	 */
	public void set(Vec3d vec) {
		x = vec.x;
		y = vec.y;
		z = vec.z;
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
	public void set(double yaw, double pitch) {
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
	public void set(double yaw, double pitch, boolean degrees) {
		if (degrees) {
			pitch = pitch * TO_RAD;
			yaw = yaw * TO_RAD;
		}
		final double yawCos = Math.cos(yaw);
		final double yawSin = Math.sin(yaw);
		final double pitCos = Math.cos(pitch);
		final double pitSin = Math.sin(pitch);
		x = -yawSin * pitCos;
		y = -pitSin;
		z = yawCos * pitCos;
	}
	
	
	/**
	 * Update the vector randomly, with each coordinate ranging
	 * between 0~1. Uniformly distributed as provided by
	 * {@link Random#nextDouble}
	 */
	public void setRandom() {
		x = random.nextDouble();
		y = random.nextDouble();
		z = random.nextDouble();
	}
	
	/**
	 * Update the vector randomly, with each coordinate within -range~range,
	 * uniformly distributed as provided by {@link Random#nextDouble}<br>
	 * Equivalent to calling {@code Vec3d.random(-range, range)}
	 * @param range Absolute bound for each coordinate
	 */
	public void setRandom(double range) {
		setRandom();
		mul(2 * range);
		add(-range);
	}
	
	/**
	 * Update the vector randomly, with each coordinate within the
	 * given range. Uniformly distributed as provided by
	 * {@link Random#nextDouble}.
	 * @param min Lower bound
	 * @param max Upper bound
	 */
	public void setRandom(double min, double max) {
		setRandom();
		mul(max - min);
		add(min);
	}
	
	/**
	 * Update the vector randomly, the result is uniformly distributed
	 * across the sphere, by means of the Lambert cylindrical equal-area
	 * projection, as provided by {@link Random#nextDouble}.
	 */
	public void setRandomUnitary() {
		final double u = random.nextDouble() * 2 * PI;
		final double v = -1D + random.nextDouble() * 2;
		// Short-circuited spherical coordinates conversion
		final double h = sqrt(1F - v * v);
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
	 * The pseudo-random generation is provided by {@link Random#nextDouble()}
	 */
	public void setRandomSpherical() {
		final double u = random.nextDouble() * 2 * PI;
		final double v = -1F + random.nextDouble() * 2;
		// √X where X is uniformly distributed follows a quadratic distribution
		final double r = Math.pow(random.nextDouble(), 0.5D);
		// Short-circuited spherical coordinates conversion
		final double h = sqrt(1F - v * v);
		x = cos(u) * h * r;
		y = sin(u) * h * r;
		z = v * r;
	}
	
	/**
	 * Set this orthogonal to other vector<br>
	 * The result may be zero if the vectors are linearly dependent<br>
	 * For a guaranteed non-zero result, see {@link Vec3d#setOrthogonalUnitary(Vec3d)}
	 */
	public void setOrthogonal(Vec3d other) {
		sub(other, dot(other));
	}
	
	/**
	 * Set this orthogonal to other vector and unitary<br>
	 * If both this and other are zero, XP is returned
	 */
	public void setOrthogonalUnitary(Vec3d other) {
		if (other.isZero() && isZero()) {
			x = 1D;
			y = 0D;
			z = 0D;
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
	public void setOrthogonal(Vec3d u, Vec3d v) {
		u.set(y - z, z - x, x - y);
		v.set(u);
		v.cross(this);
	}
	
	/**
	 * Update two vectors to contain orthogonal vectors to this, and
	 * makes them unitary (not this)
	 */
	public void setOrthogonalUnitary(Vec3d u, Vec3d v) {
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
	public static Vec3d forAxis(Axis axis) {
		if (axis == null)
			return ZERO.get();
		return switch (axis) {
			case X -> XP.get();
			case Y -> YP.get();
			case Z -> ZP.get();
		};
	}
	
	/**
	 * Get a vector for a {@link Direction}
	 * @return Vector for direction, or 0 if null
	 */
	public static Vec3d forDirection(Direction direction) {
		if (direction == null)
			return ZERO.get();
		return switch (direction) {
			case UP -> YP.get();
			case DOWN -> YN.get();
			case EAST -> XP.get();
			case WEST -> XN.get();
			case SOUTH -> ZP.get();
			case NORTH -> ZN.get();
		};
	}
	
	// Default vectors' suppliers
	public static final Supplier<Vec3d> ZERO = () -> new Vec3d(0D, 0D, 0D);
	public static final Supplier<Vec3d> XP = () -> new Vec3d(1D, 0D, 0D);
	public static final Supplier<Vec3d> YP = () -> new Vec3d(0D, 1D, 0D);
	public static final Supplier<Vec3d> ZP = () -> new Vec3d(0D, 0D, 1D);
	public static final Supplier<Vec3d> XN = () -> new Vec3d(-1D, 0D, 0D);
	public static final Supplier<Vec3d> YN = () -> new Vec3d(0D, -1D, 0D);
	public static final Supplier<Vec3d> ZN = () -> new Vec3d(0D, 0D, -1D);
	
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
			Vec3d v = (Vec3d)obj;
			if (Double.compare(v.x, x) != 0) {
				return false;
			} else if (Double.compare(v.y, y) != 0) {
				return false;
			} else {
				return Double.compare(v.z, z) == 0;
			}
		} else {
			return false;
		}
	}
	
	/**
	 * Hash code
	 */
	@Override public int hashCode() {
		int i = Double.hashCode(x);
		i = 31 * i + Double.hashCode(y);
		return 31 * i + Double.hashCode(z);
	}
	
	/**
	 * Create {@link DoubleStream}{@code .of(x, y, z)}.
	 */
	public DoubleStream stream() {
		return DoubleStream.of(x, y, z);
	}
	
	/**
	 * Uses {@link Vec3d#defaultFormat}.
	 */
	@Override public String toString() {
		return toString(defaultFormat);
	}
	
	/**
	 * Default number format used by {@link Vec3d#toString()}
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
