package software.coley.fxaccess;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Listener to be notified of instrumented code that accesses JavaFX methods while not on the JavaFX thread.
 *
 * @author Matt Coley
 * @see AccessCheck#addAccessCheckListener(AccessCheckListener)
 */
public interface AccessCheckListener {
	/**
	 * @param className
	 * 		Class dot-name where the check failed.
	 * @param methodName
	 * 		Method name in class where the check failed.
	 * @param lineNumber
	 * 		Line number in method where the check failed, or {@code -1} if not known.
	 * @param threadName
	 * 		Name of thread the JavaFX method was invoked on.
	 * @param calledMethodSignature
	 * 		Signature of JavaFX method invoked incorrectly, or {@code null} if it could not be determined.
	 */
	void onCheckFailed(@Nonnull String className, @Nonnull String methodName, int lineNumber,
	                   @Nonnull String threadName, @Nullable String calledMethodSignature);
}
