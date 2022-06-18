package endorh.util.network;

import endorh.util.math.MathParser.ExpressionParser;
import endorh.util.math.MathParser.ExpressionParser.ParseException;
import endorh.util.math.MathParser.ParsedExpression;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Much needed stuff to send complex data (like recipes) through PacketBuffers<br>
 * Also, it's annoying that {@link PacketBuffer#readString()} only
 * exists on the client having to pass 32767 as the parameter most of the time.
 */
public class PacketBufferUtil {
	@Nullable public static<T> T readNullable(PacketBuffer buf, Function<PacketBuffer, T> reader) {
		boolean hasValue = buf.readBoolean();
		if (!hasValue)
			return null;
		return reader.apply(buf);
	}
	public static<T> void writeNullable(
	  @Nullable T value, PacketBuffer buf, BiConsumer<T, PacketBuffer> writer
	) {
		if (value == null) {
			buf.writeBoolean(false);
		} else {
			buf.writeBoolean(true);
			writer.accept(value, buf);
		}
	}
	public static<T> void writeNullable(
	  PacketBuffer buf, @Nullable T value, BiConsumer<PacketBuffer, T> writer
	) {
		if (value == null) {
			buf.writeBoolean(false);
		} else {
			buf.writeBoolean(true);
			writer.accept(buf, value);
		}
	}
	public static<T> NonNullList<T> readNonNullList(
	  PacketBuffer buf, Function<PacketBuffer, T> reader, T fill
	) {
		int n = buf.readVarInt();
		NonNullList<T> list = NonNullList.withSize(n, fill);
		for (int i = 0; i < n; i++)
			list.set(i, reader.apply(buf));
		return list;
	}
	public static<T> List<T> readList(PacketBuffer buf, Function<PacketBuffer, T> reader) {
		return readList(buf, reader, new ArrayList<>());
	}
	public static<T> List<T> readList(
	  PacketBuffer buf, Function<PacketBuffer, T> reader, List<T> list
	) {
		int n = buf.readVarInt();
		for (int i = 0; i < n; i++)
			list.add(reader.apply(buf));
		return list;
	}
	
	public static<T> void writeList(
	  Collection<T> list, PacketBuffer buf, BiConsumer<T, PacketBuffer> writer
	) {
		buf.writeVarInt(list.size());
		for (T element : list)
			writer.accept(element, buf);
	}
	
	public static<T> void writeList(
	  PacketBuffer buf, Collection<T> list, BiConsumer<PacketBuffer, T> writer
	) {
		buf.writeVarInt(list.size());
		for (T element : list)
			writer.accept(buf, element);
	}
	
	public static<K, V> Map<K, V> readMap(
	  PacketBuffer buf, Function<PacketBuffer, K> keyReader, Function<PacketBuffer, V> valueReader
	) {
		int n = buf.readVarInt();
		Map<K, V> map = new LinkedHashMap<>();
		for (int i = 0; i < n; i++)
			map.put(keyReader.apply(buf), valueReader.apply(buf));
		return map;
	}
	
	public static<K, V> void writeMap(
	  PacketBuffer buf, Map<K, V> map,
	  BiConsumer<PacketBuffer, K> keyWriter,
	  BiConsumer<PacketBuffer, V> valueWriter
	) {
		buf.writeVarInt(map.size());
		for (Map.Entry<K, V> entry : map.entrySet()) {
			keyWriter.accept(buf, entry.getKey());
			valueWriter.accept(buf, entry.getValue());
		}
	}
	
	public static<K, V> void writeMap(
	  Map<K, V> map, PacketBuffer buf,
	  BiConsumer<PacketBuffer, K> keyWriter,
	  BiConsumer<V, PacketBuffer> valueWriter
	) {
		buf.writeVarInt(map.size());
		for (Map.Entry<K, V> entry : map.entrySet()) {
			keyWriter.accept(buf, entry.getKey());
			valueWriter.accept(entry.getValue(), buf);
		}
	}
	
	public static<K, V> void writeMap2(
	  PacketBuffer buf, Map<K, V> map,
	  BiConsumer<K, PacketBuffer> keyWriter,
	  BiConsumer<PacketBuffer, V> valueWriter
	) {
		buf.writeVarInt(map.size());
		for (Map.Entry<K, V> entry : map.entrySet()) {
			keyWriter.accept(entry.getKey(), buf);
			valueWriter.accept(buf, entry.getValue());
		}
	}
	
	public static<K, V> void writeMap2(
	  Map<K, V> map, PacketBuffer buf,
	  BiConsumer<K, PacketBuffer> keyWriter,
	  BiConsumer<V, PacketBuffer> valueWriter
	) {
		buf.writeVarInt(map.size());
		for (Map.Entry<K, V> entry : map.entrySet()) {
			keyWriter.accept(entry.getKey(), buf);
			valueWriter.accept(entry.getValue(), buf);
		}
	}
	
	public static<T> void writeExpression(ParsedExpression<T> expression, PacketBuffer buf) {
		buf.writeString(expression.getExpression());
	}
	
	public static<T> ParsedExpression<T> readExpression(
	  ExpressionParser<T> parser, PacketBuffer buf
	) {
		String expression = readString(buf);
		try {
			return parser.parse(expression);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Found expression does not match given parser", e);
		}
	}
	
	/**
	 * Since {@code readString()} is only in Dist.CLIENT
	 * @deprecated Removed in 1.17 since {@link FriendlyByteBuf#readUtf()} becomes available on both
	 * sides
	 */
	@Deprecated
	public static String readString(PacketBuffer buf) {
		return buf.readString(32767);
	}
}
