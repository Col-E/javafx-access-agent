package software.coley.fxaccess;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Access check util. Records where JavaFX method calls are made that are not on the JavaFX thread.
 *
 * @author Matt Coley
 */
@SuppressWarnings("unused")
public class AccessCheck {
	private static final String THREAD_NAME = "JavaFX Application Thread";
	private static final List<AccessCheckListener> listeners = new CopyOnWriteArrayList<>();
	private static final Set<String> failedLocations = new TreeSet<>();

	/**
	 * Register a listener to be notified of access check failures.
	 *
	 * @param listener
	 * 		Listener to register.
	 */
	public static void addAccessCheckListener(@Nonnull AccessCheckListener listener) {
		listeners.add(listener);
	}

	/**
	 * @return Set of locations that have failed the FX thread access check.
	 */
	@Nonnull
	public static Set<String> getFailedLocations() {
		return failedLocations;
	}

	/**
	 * Called by the instrumented agent code via {@link Bootstrapper}.
	 *
	 * @param callingMethodKey
	 * 		Data for the method that is calling the JavaFX method.
	 * @param calledMethodKey
	 * 		Data of the called JavaFX method.
	 */
	public static void check(@Nonnull String callingMethodKey, @Nonnull String calledMethodKey) {
		Thread thread = Thread.currentThread();
		String threadName = thread.getName();
		if (THREAD_NAME.equals(threadName))
			return;

		Keying.Key calling = Keying.parseKey(callingMethodKey);
		notifyListeners(
				// Calling context
				threadName, calling.className(), calling.methodName(), calling.methodDesc(), calling.lineNumber(),
				// Called method
				callingMethodKey, calledMethodKey
		);
	}

	private static void notifyListeners(@Nonnull String threadName,
	                                    @Nonnull String className,
	                                    @Nonnull String methodName,
	                                    @Nonnull String methodDesc,
	                                    int lineNumber,
	                                    @Nonnull String callingMethodKey,
	                                    @Nullable String calledMethodSignature) {
		if (failedLocations.add(callingMethodKey))
			listeners.forEach(l -> l.onCheckFailed(className, methodName, lineNumber, threadName, calledMethodSignature));
	}
}
