package endorh.lazulib.common;

import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Some logging utils
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class LogUtil {
	/**
	 * Used as a default set for {@link LogUtil#logOnce}
	 */
	public static final Set<String> logSet = new HashSet<>();
	public static final Set<String> debugSet = new HashSet<>();
	public static final Set<String> infoSet = new HashSet<>();
	public static final Set<String> warnSet = new HashSet<>();
	public static final Set<String> errorSet = new HashSet<>();
	
	public static boolean debugOnce(Logger logger, String msg) {
		return logOnce(logger::debug, debugSet, msg);
	}
	
	public static boolean infoOnce(Logger logger, String msg) {
		return logOnce(logger::info, infoSet, msg);
	}
	
	public static boolean warnOnce(Logger logger, String msg) {
		return logOnce(logger::warn, warnSet, msg);
	}
	
	public static boolean errorOnce(Logger logger, String msg) {
		return logOnce(logger::error, errorSet, msg);
	}
	
	public static boolean logOnce(Consumer<String> logger, String msg) {
		return logOnce(logger, logSet, msg);
	}
	
	public static Consumer<String> oneTimeLogger(Consumer<String> logger) {
		return s -> logOnce(logger, s);
	}
	
	/**
	 * Abstract logOnce method taking a consumer instead of a Logger
	 */
	public static boolean logOnce(
	  Consumer<String> logger, Set<String> set, String key
	) {
		if (!set.contains(key)) {
			set.add(key);
			logger.accept(key);
			return true;
		}
		return false;
	}
}
