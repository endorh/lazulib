package endorh.util.nbt;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import net.minecraft.nbt.*;
import net.minecraft.util.Util;
import org.intellij.lang.annotations.Language;

import java.io.Reader;
import java.io.StringReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parse NBT data from valid JSON<br>
 * See tests for examples.
 * You may check {@link JsonToNBTUtil#PATTERN_MAP} to see the
 * patterns recognized.<br>
 * To parse non-valid, lenient JSON, use instead {@link JsonToNBT},
 * which is used by the various data commands<br>
 */
public class JsonToNBTUtil {
	public static CompoundNBT getTagFromJson(JsonElement element) {
		if (!element.isJsonObject())
			throw new JsonSyntaxException("Tag must be an object");
		return readObject(element.getAsJsonObject());
	}
	
	public static CompoundNBT getTagFromJson(String json) {
		return getTagFromJson(json, false);
	}
	
	public static CompoundNBT getTagFromJson(String json, boolean lenient) {
		JsonReader reader = new JsonReader(new StringReader(json));
		reader.setLenient(lenient);
		return getTagFromJson(new JsonParser().parse(reader));
	}
	
	@SuppressWarnings("unused")
	public static CompoundNBT getTagFromJson(Reader json) {
		return getTagFromJson(new JsonParser().parse(json));
	}
	
	@SuppressWarnings("unused")
	public static CompoundNBT getTagFromJson(JsonReader json) {
		return getTagFromJson(new JsonParser().parse(json));
	}
	
	private static INBT readElement(JsonElement elem) {
		if (elem.isJsonNull()) {
			throw new JsonSyntaxException("Null value is not allowed in NBT JSON");
		} else if (elem.isJsonObject()) {
			return readObject(elem.getAsJsonObject());
		} else if (elem.isJsonArray()) {
			return readArray(elem.getAsJsonArray());
		} else if (elem.isJsonPrimitive()) {
			return readValue(elem.getAsJsonPrimitive());
		} else {
			throw new IllegalArgumentException("Unknown JSON type: " + elem);
		}
	}
	
	private static CompoundNBT readObject(JsonObject obj) {
		final CompoundNBT nbt = new CompoundNBT();
		for (Entry<String, JsonElement> child : obj.entrySet())
			nbt.put(child.getKey(), readElement(child.getValue()));
		return nbt;
	}
	
	private static INBT readArray(JsonArray array) {
		int n = array.size();
		if (n < 1)
			throw new JsonSyntaxException("Empty NBT array must specify its type");
		JsonElement first = array.get(0);
		final String differentTypes = "All elements in array must be of the same type";
		if (first.isJsonPrimitive()) {
			JsonPrimitive prim = first.getAsJsonPrimitive();
			if (prim.isString()) {
				final String type = prim.getAsString();
				if ("B".equals(type) || "I".equals(type) || "L".equals(type)) {
					List<Number> list = new ArrayList<>();
					for (int i = 1; i < n; i++) {
						try {
							JsonPrimitive p = array.get(i).getAsJsonPrimitive();
							if (p.isBoolean()) {
								list.add(p.getAsBoolean() ? 1 : 0);
							} else if (p.isNumber()) {
								list.add(p.getAsNumber());
							} else throw new JsonSyntaxException(differentTypes);
						} catch (IllegalStateException e) {
							throw new JsonSyntaxException(differentTypes);
						}
					}
					switch (type) {
						case "B": return new ByteArrayNBT(
						  list.stream().map(Number::byteValue).collect(Collectors.toList()));
						case "I": return new IntArrayNBT(
							list.stream().map(Number::intValue).collect(Collectors.toList()));
						case "L": return new LongArrayNBT(
						  list.stream().map(Number::longValue).collect(Collectors.toList()));
						default: throw new IllegalStateException();
					}
				}
			}
		}
		List<INBT> ls = new ArrayList<>();
		ListNBT list = new ListNBT();
		for (JsonElement elem : array)
			ls.add(readElement(elem));
		try {
			if (ls.isEmpty())
				return list;
			if (ls.get(0) instanceof NumberNBT) {
				Class<?> cls = null;
				for (INBT elem : ls) {
					if (!(elem instanceof NumberNBT))
						throw new JsonSyntaxException(differentTypes);
					cls = combineNumericTypes(cls, elem.getClass());
				}
				final Function<Number, NumberNBT> con = numericConstructors.get(cls);
				for (INBT elem : ls)
					list.add(con.apply(((NumberNBT) elem).getAsNumber()));
			} else list.addAll(ls);
		} catch (UnsupportedOperationException e) {
			throw new JsonSyntaxException(differentTypes);
		}
		return list;
	}
	
	protected static Class<?> combineNumericTypes(Class<?> a, Class<?> b) {
		if (a == FloatNBT.class || a == DoubleNBT.class || b == FloatNBT.class || b == DoubleNBT.class) {
			if (a == LongNBT.class || a == DoubleNBT.class || b == LongNBT.class || b == DoubleNBT.class)
				return DoubleNBT.class;
			else return FloatNBT.class;
		}
		if (a == LongNBT.class || b == LongNBT.class)
			return LongNBT.class;
		else if (a == IntNBT.class || b == IntNBT.class)
			return IntNBT.class;
		else if (a == ShortNBT.class || b == ShortNBT.class)
			return ShortNBT.class;
		return ByteNBT.class;
	}
	
	protected static final Map<Class<?>, Function<Number, NumberNBT>> numericConstructors =
	  Util.make(new HashMap<>(), m -> {
		m.put(ByteNBT.class, n -> ByteNBT.valueOf(n.byteValue()));
		m.put(ShortNBT.class, n -> ShortNBT.valueOf(n.shortValue()));
		m.put(IntNBT.class, n -> IntNBT.valueOf(n.intValue()));
		m.put(LongNBT.class, n -> LongNBT.valueOf(n.longValue()));
		m.put(FloatNBT.class, n -> FloatNBT.valueOf(n.floatValue()));
		m.put(DoubleNBT.class, n -> DoubleNBT.valueOf(n.doubleValue()));
	});
	
	private static final Map<Pattern, Function<String, INBT>> PATTERN_MAP = new LinkedHashMap<>();
	static {
		pat("([-+]?(?:[0-9]+[.]?|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?)d", Double::parseDouble, DoubleNBT::valueOf);
		pat("([-+]?(?:[0-9]+[.]?|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?)f", Float::parseFloat, FloatNBT::valueOf);
		pat("([-+]?(?:0|[1-9][0-9]*))b", Byte::parseByte, ByteNBT::valueOf);
		pat("([-+]?(?:0|[1-9][0-9]*))l", Long::parseLong, LongNBT::valueOf);
		pat("([-+]?(?:0|[1-9][0-9]*))s", Short::parseShort, ShortNBT::valueOf);
		pat("([-+]?(?:0|[1-9][0-9]*))i", Integer::parseInt, IntNBT::valueOf);
		// pat("([-+]?(?:0|[1-9][0-9]*))", Integer::parseInt, IntNBT::valueOf);
		// pat("([-+]?(?:[0-9]+[.]|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?)", Double::parseDouble, DoubleNBT::valueOf);
		pat("(false|true)", str -> "true".equalsIgnoreCase(str)? ByteNBT.ONE : ByteNBT.ZERO);
	}
	private static <T> void pat(
	  @Language("RegExp") String pattern, Function<String, T> parser, Function<T, INBT> caster
	) { pat(pattern, parser.andThen(caster)); }
	private static void pat(@Language("RegExp") String pattern, Function<String, INBT> parser) {
		PATTERN_MAP.put(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE), parser);
	}
	
	private static INBT readValue(JsonPrimitive elem) {
		if (elem.isBoolean()) {
			return ByteNBT.valueOf(elem.getAsBoolean());
		} else if (elem.isNumber()) {
			if (!elem.getAsString().contains(".")) {
				return IntNBT.valueOf(elem.getAsInt());
			} else {
				return DoubleNBT.valueOf(elem.getAsDouble());
			}
		} else if (elem.isString()) {
			String str = elem.getAsString();
			try {
				for (Pattern pattern : PATTERN_MAP.keySet()) {
					Matcher m = pattern.matcher(str);
					if (m.matches()) {
						return PATTERN_MAP.get(pattern).apply(m.group(1));
					}
				}
			} catch (NumberFormatException ignored) {}
			return str.charAt(0) == '"' && str.charAt(str.length() - 1) == '"'
			       ? StringNBT.valueOf(str.substring(1, str.length() - 1)) : StringNBT.valueOf(str);
		} else {
			throw new IllegalStateException("Unknown JSON primitive: " + elem);
		}
	}
}
