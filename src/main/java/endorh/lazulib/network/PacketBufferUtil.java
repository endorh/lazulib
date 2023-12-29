package endorh.lazulib.network;

import endorh.lazulib.math.MathParser.ExpressionParser;
import endorh.lazulib.math.MathParser.ExpressionParser.ParseException;
import endorh.lazulib.math.MathParser.ParsedExpression;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Much needed stuff to send complex data (like recipes) through PacketBuffers<br>
 */
public class PacketBufferUtil {
	@Nullable public static<T> T readNullable(FriendlyByteBuf buf, Function<FriendlyByteBuf, T> reader) {
		boolean hasValue = buf.readBoolean();
		if (!hasValue)
			return null;
		return reader.apply(buf);
	}
	public static<T> void writeNullable(
	  @Nullable T value, FriendlyByteBuf buf, BiConsumer<T, FriendlyByteBuf> writer
	) {
		if (value == null) {
			buf.writeBoolean(false);
		} else {
			buf.writeBoolean(true);
			writer.accept(value, buf);
		}
	}
	public static<T> void writeNullable(
	  FriendlyByteBuf buf, @Nullable T value, BiConsumer<FriendlyByteBuf, T> writer
	) {
		if (value == null) {
			buf.writeBoolean(false);
		} else {
			buf.writeBoolean(true);
			writer.accept(buf, value);
		}
	}
	public static<T> NonNullList<T> readNonNullList(
	  FriendlyByteBuf buf, Function<FriendlyByteBuf, T> reader, T fill
	) {
		int n = buf.readVarInt();
		NonNullList<T> list = NonNullList.withSize(n, fill);
		for (int i = 0; i < n; i++)
			list.set(i, reader.apply(buf));
		return list;
	}
	public static<T> List<T> readList(FriendlyByteBuf buf, Function<FriendlyByteBuf, T> reader) {
		return readList(buf, reader, new ArrayList<>());
	}
	public static<T> List<T> readList(
	  FriendlyByteBuf buf, Function<FriendlyByteBuf, T> reader, List<T> list
	) {
		int n = buf.readVarInt();
		for (int i = 0; i < n; i++)
			list.add(reader.apply(buf));
		return list;
	}
	
	public static<T> void writeList(
	  Collection<T> list, FriendlyByteBuf buf, BiConsumer<T, FriendlyByteBuf> writer
	) {
		buf.writeVarInt(list.size());
		for (T element : list)
			writer.accept(element, buf);
	}
	
	public static<T> void writeList(
	  FriendlyByteBuf buf, Collection<T> list, BiConsumer<FriendlyByteBuf, T> writer
	) {
		buf.writeVarInt(list.size());
		for (T element : list)
			writer.accept(buf, element);
	}
	
	public static<K, V> Map<K, V> readMap(
	  FriendlyByteBuf buf, Function<FriendlyByteBuf, K> keyReader, Function<FriendlyByteBuf, V> valueReader
	) {
		int n = buf.readVarInt();
		Map<K, V> map = new LinkedHashMap<>();
		for (int i = 0; i < n; i++)
			map.put(keyReader.apply(buf), valueReader.apply(buf));
		return map;
	}
	
	public static<K, V> void writeMap(
	  FriendlyByteBuf buf, Map<K, V> map,
	  BiConsumer<FriendlyByteBuf, K> keyWriter,
	  BiConsumer<FriendlyByteBuf, V> valueWriter
	) {
		buf.writeVarInt(map.size());
		for (Map.Entry<K, V> entry : map.entrySet()) {
			keyWriter.accept(buf, entry.getKey());
			valueWriter.accept(buf, entry.getValue());
		}
	}
	
	public static<K, V> void writeMap(
	  Map<K, V> map, FriendlyByteBuf buf,
	  BiConsumer<FriendlyByteBuf, K> keyWriter,
	  BiConsumer<V, FriendlyByteBuf> valueWriter
	) {
		buf.writeVarInt(map.size());
		for (Map.Entry<K, V> entry : map.entrySet()) {
			keyWriter.accept(buf, entry.getKey());
			valueWriter.accept(entry.getValue(), buf);
		}
	}
	
	public static<K, V> void writeMap2(
	  FriendlyByteBuf buf, Map<K, V> map,
	  BiConsumer<K, FriendlyByteBuf> keyWriter,
	  BiConsumer<FriendlyByteBuf, V> valueWriter
	) {
		buf.writeVarInt(map.size());
		for (Map.Entry<K, V> entry : map.entrySet()) {
			keyWriter.accept(entry.getKey(), buf);
			valueWriter.accept(buf, entry.getValue());
		}
	}
	
	public static<K, V> void writeMap2(
	  Map<K, V> map, FriendlyByteBuf buf,
	  BiConsumer<K, FriendlyByteBuf> keyWriter,
	  BiConsumer<V, FriendlyByteBuf> valueWriter
	) {
		buf.writeVarInt(map.size());
		for (Map.Entry<K, V> entry : map.entrySet()) {
			keyWriter.accept(entry.getKey(), buf);
			valueWriter.accept(entry.getValue(), buf);
		}
	}
	
	public static<T> void writeExpression(ParsedExpression<T> expression, FriendlyByteBuf buf) {
		buf.writeUtf(expression.getExpression());
	}
	
	public static<T> ParsedExpression<T> readExpression(
	  ExpressionParser<T> parser, FriendlyByteBuf buf
	) {
		String expression = buf.readUtf();
		try {
			return parser.parse(expression);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Found expression does not match given parser", e);
		}
	}
}
