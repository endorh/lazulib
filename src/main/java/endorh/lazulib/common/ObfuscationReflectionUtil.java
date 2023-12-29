package endorh.lazulib.common;

import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper.UnknownConstructorException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Optional reflection methods, which return {@link Optional} or
 * null, and optionally log a message, instead of throwing exceptions<br>
 * Convenient for non-critical reflection use cases<br>
 * Take care to not overuse
 */
@SuppressWarnings("unused")
public class ObfuscationReflectionUtil {
	
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static <T, C> Optional<T> getStaticFieldValue(
		Class<C> clazz, String srgName
	) {
		return getField(clazz, srgName).map(f -> {
			if (!Modifier.isStatic(f.getModifiers())) {
				LOGGER.warn("Cannot get static field value from non static field " + f.getDeclaringClass().getName() + "$" + f.getName());
				return null;
			}
			try {
				f.setAccessible(true);
				//noinspection unchecked
				return (T) f.get(null);
			} catch (IllegalAccessException e) {
				return null;
			}
		});
	}
	
	public static @Nullable <T, C> T getStaticFieldValueOrLog(
	  Class<C> clazz, String srgName, @Nullable String mcpName, @Nullable Consumer<String> logger
	) { return getStaticFieldValueOrLog(clazz, srgName, mcpName, logger, ""); }
	
	public static @Nullable <T, C> T getStaticFieldValueOrLog(
	  Class<C> clazz, String srgName, @Nullable String mcpName,
	  @Nullable Consumer<String> logger, @Nullable String logExtra
	) {
		return ObfuscationReflectionUtil.<T, C>getStaticFieldValue(clazz, srgName).orElseGet(() -> {
			if (logger != null)
				logger.accept(
				  "Could not get value of static field \"" + clazz.getName() + "$" + srgName
				  + (mcpName != null? " (MCP " + mcpName + ")" : "")
				  + (logExtra != null && !logExtra.isEmpty()? ": " + logExtra : ""));
			return null;
		});
	}
	
	public static @Nullable <T> Field getFieldOrLog(
	  Class<T> clazz, String srgName, @Nullable String mcpName, @Nullable Consumer<String> logger
	) { return getFieldOrLog(clazz, srgName, mcpName, logger, ""); }
	
	public static @Nullable <T> Field getFieldOrLog(
	  Class<T> clazz, String srgName, @Nullable String mcpName,
	  @Nullable Consumer<String> logger, @Nullable String logExtra
	) {
		return getField(clazz, srgName).orElseGet(() -> {
			if (logger != null)
				logger.accept(
				  "Unable to find field " + clazz.getSimpleName() + "#" + srgName +
				  (mcpName != null? " (MCP " + mcpName + ")" : "") +
				  (logExtra != null && !logExtra.isEmpty()? ": " + logExtra : ""));
		  	return null;
		});
	}
	
	public static @Nullable <T> Method getMethodOrLog(
	  Class<T> clazz, String srgName, @Nullable String mcpName,
	  @Nullable Consumer<String> logger, Class<?>... parameterTypes
	) { return getMethodOrLog(clazz, srgName, mcpName, logger, "", parameterTypes); }
	
	public static @Nullable <T> Method getMethodOrLog(
	  Class<T> clazz, String srgName, @Nullable String mcpName,
	  @Nullable Consumer<String> logger, @Nullable String logExtra, Class<?>... parameterTypes
	) {
		return getMethod(clazz, srgName, parameterTypes).orElseGet(() -> {
			if (logger != null)
				logger.accept(
				  "Unable to find method " + clazz.getSimpleName() + "#" + srgName +
				  "(" + Arrays.stream(parameterTypes).map(Class::getSimpleName)
					 .collect(Collectors.joining(", ")) + ")" +
				  (mcpName != null ? " (MCP " + mcpName + ")" : "") +
				  (logExtra != null && !logExtra.isEmpty()? ": " + logExtra : ""));
			return null;
		});
	}
	
	public static @Nullable <T> Constructor<T> getConstructorOrLog(
	  Class<T> clazz, Consumer<String> logger, Class<?>... parameterTypes
	) { return getConstructorOrLog(clazz, logger, "", parameterTypes); }
	
	public static @Nullable <T> Constructor<T> getConstructorOrLog(
	  Class<T> clazz, Consumer<String> logger, String logExtra, Class<?>... parameterTypes
	) {
		return getConstructor(clazz, parameterTypes).orElseGet(() -> {
			logger.accept(
			  "Unable to find constructor for class " + clazz.getName() + " with parameters " +
			  "(" + Arrays.stream(parameterTypes).map(Class::getSimpleName)
			    .collect(Collectors.joining(", ")) + ")" +
			  (!logExtra.isEmpty()? ": " + logExtra : ""));
			return null;
		});
	}
	
	public static <T> Optional<Field> getField(Class<T> clazz, String srgName) {
		try {
			Field result = ObfuscationReflectionHelper.findField(
			  clazz, srgName);
			result.setAccessible(true);
			return Optional.of(result);
		} catch (ObfuscationReflectionHelper.UnableToFindFieldException e) {
			return Optional.empty();
		}
	}
	
	public static <T> Optional<Method> getMethod(
	  Class<T> clazz, String srgName, Class<?>... parameterTypes
	) {
		try {
			Method result = ObfuscationReflectionHelper.findMethod(
			  clazz, srgName, parameterTypes);
			result.setAccessible(true);
			return Optional.of(result);
		} catch (ObfuscationReflectionHelper.UnableToFindMethodException e) {
			return Optional.empty();
		}
	}
	
	public static <T> Optional<Constructor<T>> getConstructor(
	  Class<T> clazz, Class<?>... parameterTypes
	) {
		try {
			Constructor<T> result = ObfuscationReflectionHelper.findConstructor(
			  clazz, parameterTypes);
			result.setAccessible(true);
			return Optional.of(result);
		} catch (UnknownConstructorException e) {
			return Optional.empty();
		}
	}
	
	public static <C, V> SoftField<C, V> getSoftField(
	  Class<C> clazz, String srgName
	) { return getSoftField(clazz, srgName, null, null, null, null); }
	
	public static <C, V> SoftField<C, V> getSoftField(
	  Class<C> clazz, String srgName, @Nullable String mcpName
	) { return getSoftField(clazz, srgName, mcpName, null, null, null); }
	
	public static <C, V> SoftField<C, V> getSoftField(
	  Class<C> clazz, String srgName, @Nullable String mcpName, @Nullable Consumer<String> logger
	) { return getSoftField(clazz, srgName, mcpName, logger, null, null); }
	
	public static <C, V> SoftField<C, V> getSoftField(
	  Class<C> clazz, String srgName, @Nullable String mcpName, @Nullable Consumer<String> logger,
	  @Nullable String logExtra
	) { return getSoftField(clazz, srgName, mcpName, logger, logExtra, null); }
	
	public static <C, V> SoftField<C, V> getSoftField(
	  Class<C> clazz, String srgName, @Nullable String mcpName, @Nullable Consumer<String> logger,
	  @Nullable String fetchLogExtra, @Nullable String logExtra
	) {
		final Field field = getFieldOrLog(clazz, srgName, mcpName, logger, fetchLogExtra);
		return new SoftField<>(
		  clazz, clazz.getSimpleName() + "#" + srgName + (mcpName != null? " (MCP: " + mcpName + ")" : ""),
		  field, logger, logExtra != null? logExtra : fetchLogExtra);
	}
	
	public static <C, R> SoftMethod<C, R> getSoftMethod(
	  Class<C> clazz, String srgName, Class<?>... parameterTypes
	) { return getSoftMethod(clazz, srgName, null, null, null, null, parameterTypes); }
	
	public static <C, R> SoftMethod<C, R> getSoftMethod(
	  Class<C> clazz, String srgName, String mcpName, Class<?>... parameterTypes
	) { return getSoftMethod(clazz, srgName, mcpName, null, null, null, parameterTypes); }
	
	public static <C, R> SoftMethod<C, R> getSoftMethod(
	  Class<C> clazz, String srgName, String mcpName,
	  @Nullable Consumer<String> logger, Class<?>... parameterTypes
	) { return getSoftMethod(clazz, srgName, mcpName, logger, null, null, parameterTypes); }
	
	public static <C, R> SoftMethod<C, R> getSoftMethod(
	  Class<C> clazz, String srgName, String mcpName, @Nullable Consumer<String> logger,
	  @Nullable String logExtra, Class<?>... parameterTypes
	) { return getSoftMethod(clazz, srgName, mcpName, logger, null, logExtra, parameterTypes); }
	
	public static <C, R> SoftMethod<C, R> getSoftMethod(
	  Class<C> clazz, String srgName, String mcpName, @Nullable Consumer<String> logger,
	  @Nullable String fetchLogExtra, @Nullable String logExtra, Class<?>... parameterTypes
	) {
		final Method method = getMethodOrLog(
		  clazz, srgName, mcpName, logger, fetchLogExtra, parameterTypes);
		return new SoftMethod<>(
		  clazz, clazz.getSimpleName() + "#" + srgName + " (MCP: " + mcpName + ")",
		  method, logger, logExtra != null? logExtra : fetchLogExtra);
	}
	
	public static <C> SoftConstructor<C> getSoftConstructor(
	  Class<C> clazz, Class<?>... parameterTypes
	) { return getSoftConstructor(clazz, null, null, null, parameterTypes); }
	
	public static <C> SoftConstructor<C> getSoftConstructor(
	  Class<C> clazz, @Nullable Consumer<String> logger, Class<?>... parameterTypes
	) { return getSoftConstructor(clazz, logger, null, null, parameterTypes); }
	
	public static <C> SoftConstructor<C> getSoftConstructor(
	  Class<C> clazz, @Nullable Consumer<String> logger, @Nullable String logExtra,
	  Class<?>... parameterTypes
	) { return getSoftConstructor(clazz, logger, null, logExtra, parameterTypes); }
	
	public static <C> SoftConstructor<C> getSoftConstructor(
	  Class<C> clazz, @Nullable Consumer<String> logger, @Nullable String fetchLogExtra,
	  @Nullable String logExtra, Class<?>... parameterTypes
	) {
		final Constructor<C> constructor = getConstructorOrLog(
		  clazz, logger, fetchLogExtra, parameterTypes);
		return new SoftConstructor<>(clazz, constructor, logger, logExtra != null? logExtra : "");
	}
	
	/**
	 * Wrapper for a {@link Field} which returns null/logs on errors
	 * instead of throwing exceptions<br>
	 * Convenient for non-critical reflection use cases
	 * @param <C> Type of the class containing the method
	 * @param <V> Type of the field
	 */
	public static class SoftField<C, V> {
		public final Class<C> clazz;
		protected final String name;
		public final @Nullable Field field;
		public @Nullable Consumer<String> logger;
		public @Nullable String logExtra;
		
		protected SoftField(
		  Class<C> clazz, String name, @Nullable Field field,
		  @Nullable Consumer<String> logger, @Nullable String logExtra
		) {
			this.clazz = clazz;
			this.name = name;
			this.field = field;
			this.logger = logger;
			this.logExtra = logExtra;
			if (this.field != null)
				this.field.setAccessible(true);
		}
		
		/**
		 * Attempt to get value for this field in self instance
		 * @return The field value or null on error
		 */
		public @Nullable V get(@Nullable C self) {
			if (field == null)
				log(getUnavailableErrorMessage());
			else try {
				//noinspection unchecked
				return (V) field.get(self);
			} catch (IllegalAccessException | IllegalArgumentException | ClassCastException e) {
				log(getReadErrorMessage(e));
			}
			return null;
		}
		
		/**
		 * Attempt to set value for this field on self instance
		 * @return true on success
		 */
		public boolean set(@Nullable C self, V value) {
			if (field == null)
				log(getUnavailableErrorMessage());
			else try {
				field.set(self, value);
				return true;
			} catch (IllegalAccessException | IllegalArgumentException e) {
				log(getWriteErrorMessage(e));
			}
			return false;
		}
		
		protected void log(String msg) {
			if (logger != null && msg != null)
				logger.accept(msg);
		}
		
		/**
		 * Logged when trying to use the field, but it is null
		 */
		protected @Nullable String getUnavailableErrorMessage() {
			return null; // Do not log
		}
		
		protected @Nullable String getReadErrorMessage(Exception e) {
			return "Could not access field " + name + (!getLogExtra().isEmpty()? ": " + logExtra : "")
			       + "\n  Details: " + e.getMessage();
		}
		
		protected @Nullable String getWriteErrorMessage(Exception e) {
			return "Could not modify field " + name + (!getLogExtra().isEmpty()? ": " + logExtra : "")
			       + "\n  Details: " + e.getMessage();
		}
		
		protected @Nonnull String getLogExtra() {
			return logExtra != null? logExtra : "";
		}
	}
	
	/**
	 * Wrapper for a {@link Method} which returns null/logs on errors
	 * instead of throwing exceptions<br>
	 * Convenient for non-critical reflection use cases
	 * @param <C> Type of the class containing the method
	 * @param <R> Return type of the method
	 */
	public static class SoftMethod<C, R> {
		public final Class<C> clazz;
		public final String name;
		public @Nullable final Method method;
		public @Nullable Consumer<String> logger;
		public @Nullable String logExtra;
		
		protected SoftMethod(
		  Class<C> clazz, String name, @Nullable Method method,
		  @Nullable Consumer<String> logger, @Nullable String logExtra
		) {
			this.clazz = clazz;
			this.name = name;
			this.method = method;
			this.logger = logger;
			this.logExtra = logExtra;
			if (this.method != null)
				this.method.setAccessible(true);
		}
		
		/**
		 * Try to invoke this method on self instance with the passed args<br>
		 * @return The value returned by the call, or null on error
		 * @see SoftMethod#testInvoke(Object, Object...)
		 */
		public @Nullable R invoke(C self, Object... args) {
			if (method == null)
				log(getUnavailableErrorMessage());
			else try {
				//noinspection unchecked
				return (R) method.invoke(self, args);
			} catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
				log(getInvokeErrorMessage(e));
			}
			return null;
		}
		
		/**
		 * Try to invoke this method on self instance with the passed args
		 * @return True on success
		 * @see SoftMethod#invoke(Object, Object...)
		 */
		public boolean testInvoke(C self, Object... args) {
			if (method == null)
				log(getUnavailableErrorMessage());
			else try {
				method.invoke(self, args);
				return true;
			} catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
				log(getInvokeErrorMessage(e));
			}
			return false;
		}
		
		protected void log(String msg) {
			if (logger != null && msg != null)
				logger.accept(msg);
		}
		
		protected @Nullable String getUnavailableErrorMessage() {
			return null;
		}
		
		protected @Nullable String getInvokeErrorMessage(Exception e) {
			return "Could not invoke method " + name + (!getLogExtra().isEmpty()? ": " + logExtra : "")
			  + "\n  Details: " + e.getMessage();
		}
		
		protected @Nonnull String getLogExtra() {
			return logExtra != null? logExtra : "";
		}
	}
	
	/**
	 * Wrapper for a {@link Constructor} which returns null/logs on errors
	 * instead of throwing exceptions<br>
	 * Convenient for non-critical reflection use cases
	 * @param <C> Type of the class of the constructor
	 */
	public static class SoftConstructor<C> {
		public final Class<C> clazz;
		public final Constructor<C> constructor;
		public final String name;
		public @Nullable Consumer<String> logger;
		public @Nullable String logExtra;
		
		protected SoftConstructor(
		  Class<C> clazz, Constructor<C> constructor,
		  @Nullable Consumer<String> logger, @Nullable String logExtra
		) {
			this.clazz = clazz;
			this.constructor = constructor;
			this.name = clazz.getSimpleName();
			this.logger = logger;
			this.logExtra = logExtra;
			if (this.constructor != null)
				this.constructor.setAccessible(true);
		}
		
		/**
		 * Attempt to create a new instance using this constructor
		 * and the passed args
		 * @return The new instance, or null on error
		 */
		public @Nullable C newInstance(Object... args) {
			if (constructor == null)
				log(getUnavailableErrorMessage());
			else try {
				return constructor.newInstance(args);
			} catch (InvocationTargetException | InstantiationException | IllegalAccessException | IllegalArgumentException e) {
				log(getConstructionErrorMessage(e));
			}
			return null;
		}
		
		protected void log(@Nullable String msg) {
			if (logger != null && msg != null)
				logger.accept(msg);
		}
		
		protected @Nullable String getUnavailableErrorMessage() {
			return null;
		}
		
		protected @Nullable String getConstructionErrorMessage(Exception e) {
			return "Could not create instance of class " + name + (!getLogExtra().isEmpty()? ": " + logExtra : "")
			       + "\n  Details: " + e.getMessage();
		}
		
		protected @Nonnull String getLogExtra() {
			return logExtra != null? logExtra : "";
		}
	}
}
