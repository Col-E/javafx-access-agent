package software.coley.fxaccess;

import jakarta.annotation.Nonnull;

/**
 * Util for packing method data into strings.
 *
 * @author Matt Coley
 */
public class Keying {
	/**
	 * @param className
	 * 		Declaring class name.
	 * @param methodName
	 * 		Declared method name.
	 * @param methodDesc
	 * 		Declared method descriptor.
	 * @param lineNumber
	 * 		Some line number in the method.
	 *
	 * @return Formatted key string for the location within the given declared method.
	 */
	@Nonnull
	public static String key(@Nonnull String className, @Nonnull String methodName, @Nonnull String methodDesc, int lineNumber) {
		return key(signature(className, methodName, methodDesc), lineNumber);
	}

	/**
	 * @param signature
	 * 		Signature key of the declared method.
	 * @param lineNumber
	 * 		Some line number in the method.
	 *
	 * @return Formatted key string for the location within the given declared method.
	 */
	@Nonnull
	public static String key(@Nonnull String signature, int lineNumber) {
		return signature + "#" + lineNumber;
	}

	/**
	 * @param className
	 * 		Declaring class name.
	 * @param methodName
	 * 		Declared method name.
	 * @param methodDesc
	 * 		Declared method descriptor.
	 *
	 * @return Formatted key string for the given declared method.
	 */
	@Nonnull
	public static String signature(@Nonnull String className, @Nonnull String methodName, @Nonnull String methodDesc) {
		return className.replace('/', '.') + "#" + methodName + methodDesc;
	}

	/**
	 * @param key
	 * 		Formatted key string.
	 *
	 * @return Signature portion from the key.
	 */
	@Nonnull
	public static String signatureFromKey(@Nonnull String key) {
		return key.substring(0, key.lastIndexOf('#'));
	}

	/**
	 * @param key
	 * 		Formatted key string.
	 *
	 * @return Model of key portions.
	 */
	@Nonnull
	public static Key parseKey(@Nonnull String key) {
		try {
			String[] sections = key.split("#");
			String className = sections[0];
			String methodNameType = sections[1];
			int descIndex = methodNameType.indexOf('(');
			String methodName = methodNameType.substring(0, descIndex);
			String methodDesc = methodName.substring(descIndex);
			int lineNumber = sections.length >= 3 ? Integer.parseInt(sections[2]) : -1;
			return new Key(className, methodName, methodDesc, lineNumber);
		} catch (Throwable t) {
			throw new IllegalStateException("Invalid method key: " + key, t);
		}
	}

	/**
	 * @param className
	 * 		Declaring class name.
	 * @param methodName
	 * 		Declared method name.
	 * @param methodDesc
	 * 		Declared method descriptor.
	 * @param lineNumber
	 * 		Some line number in the method.
	 */
	public record Key(@Nonnull String className,
	                  @Nonnull String methodName,
	                  @Nonnull String methodDesc,
	                  int lineNumber) {}
}
