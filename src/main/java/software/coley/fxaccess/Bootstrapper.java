package software.coley.fxaccess;

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * A bootstrap method handler that takes in two {@link String} arguments from the
 * {@code invokedynamic} instruction's bootstrap method arguments array and passes them along
 * to {@link AccessCheck#check(String, String)}.
 *
 * @author Matt Coley
 */
public final class Bootstrapper {
	@SuppressWarnings("unused")
	public static Object delegate(
			// Inferred arguments
			MethodHandles.Lookup lookup,
			String callerName,
			MethodType callerType,
			// BSM arguments
			String calling,
			String called) {
		try {
			MethodType targetMethodType = MethodType.methodType(void.class, String.class, String.class);
			MethodHandle checkHandle = lookup.findStatic(AccessCheck.class, "check", targetMethodType);
			MethodHandle checkWithArgsHandle = MethodHandles.insertArguments(checkHandle, 0, calling, called).asType(callerType);
			return new ConstantCallSite(checkWithArgsHandle);
		} catch (Exception ex) {
			throw new BootstrapMethodError("Failed to lookup access check method", ex);
		}
	}
}