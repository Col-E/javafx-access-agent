package software.coley.fxaccess;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Access check util. Records where JavaFX method calls are made that are not on the JavaFX thread.
 *
 * @author Matt Coley
 */
@SuppressWarnings("unused")
public class AccessCheck {
	private static final String THIS_NAME = AccessCheck.class.getName();
	private static final String THREAD_NAME = "JavaFX Application Thread";
	private static final List<AccessCheckListener> listeners = new CopyOnWriteArrayList<>();
	private static final Map<String, String> locationToSignature = new TreeMap<>();
	private static final Set<String> failedLocations = new TreeSet<>();

	/**
	 * Called by the {@link Agent} when instrumenting classes to track all locations where FX methods are called.
	 * The recorded data is used by the zero-argument {@link #check()} to determine what FX method has been invoked.
	 *
	 * @param className
	 * 		Class dot-name where the FX method is called.
	 * @param methodName
	 * 		Method name in class where the FX method is called.
	 * @param lineNumber
	 * 		Line number in method where the FX method is called.
	 * @param calledMethodSignature
	 * 		Signature of JavaFX method invoked.
	 */
	protected static void register(@Nonnull String className, @Nonnull String methodName, int lineNumber,
	                               @Nonnull String calledMethodSignature) {
		locationToSignature.put(key(className, methodName, lineNumber), calledMethodSignature);
	}

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
	 * Called by the instrumented agent code.
	 */
	public static void check() {
		// Lookup known signature at the given calling context location.
		String signature;
		StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
		Optional<StackWalker.StackFrame> callingFrame = walker.walk(frames -> frames.skip(1).findFirst());
		if (callingFrame.isPresent()) {
			StackWalker.StackFrame frame = callingFrame.get();
			String locationKey = key(frame.getClassName(), frame.getMethodName(), frame.getLineNumber());
			signature = locationToSignature.get(locationKey);
		} else {
			signature = null;
		}

		// Do the standard thread check.
		check(signature);
	}

	/**
	 * Called by the instrumented agent code.
	 *
	 * @param calledMethodSignature
	 * 		Signature of JavaFX method invoked incorrectly, or {@code null} if it could not be determined.
	 */
	public static void check(@Nullable String calledMethodSignature) {
		Thread thread = Thread.currentThread();
		String threadName = thread.getName();
		if (!THREAD_NAME.equals(threadName)) {
			// Trace model:
			//   [0] java.base/java.lang.Thread.getStackTrace
			//   [1] software.coley.fxaccess.AccessCheck.check (this method)
			//   [2] <calling-context> (could be the no-arg 'check' method)
			StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
			List<StackWalker.StackFrame> traceFrames = walker.walk(frames -> frames.skip(1).limit(2).toList());
			if (traceFrames.isEmpty())
				return;
			StackWalker.StackFrame frame = traceFrames.get(0);
			if (THIS_NAME.equals(frame.getClassName()))
				frame = traceFrames.get(1);

			// Record the calling context that was not on the right thread.
			String className = frame.getClassName();
			String methodName = frame.getMethodName();
			int lineNumber = frame.getLineNumber();
			String key = className + "#" + methodName + " (Line:" + lineNumber + ")";
			if (failedLocations.add(key))
				listeners.forEach(l -> l.onCheckFailed(className, methodName, lineNumber, threadName, calledMethodSignature));
		}
	}

	@Nonnull
	private static String key(@Nonnull StackTraceElement element) {
		String className = element.getClassName();
		String methodName = element.getMethodName();
		int lineNumber = element.getLineNumber();
		return key(className, methodName, lineNumber);
	}

	@Nonnull
	private static String key(@Nonnull String className, @Nonnull String methodName, int lineNumber) {
		return className.replace('/', '.') + "#" + methodName + " (Line:" + lineNumber + ")";
	}

	@Nonnull
	protected static String signature(@Nonnull String className, @Nonnull String methodName, @Nonnull String methodDesc) {
		return className.replace('/', '.') + "#" + methodName + methodDesc;
	}
}
