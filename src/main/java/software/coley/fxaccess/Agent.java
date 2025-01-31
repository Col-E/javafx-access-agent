package software.coley.fxaccess;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;

/**
 * Agent that inserts calls to {@link AccessCheck#check(String, String)} after calls to JavaFX methods.
 *
 * @author Matt Coley
 */
public class Agent {
	private static final int ASM_API = ASM9;

	@SuppressWarnings("unused")
	public static void premain(String argument, Instrumentation instrumentation) {
		String[] whitelistedPackages = argument.isBlank() ?
				new String[0] :
				argument.split(";");

		// Do nothing and warn user that no packaged were registered for instrumentation
		if (whitelistedPackages.length == 0) {
			System.err.print("Missing whitelisted package argument to JFX-TACA. Example format: com.foo;org.bar;fizz.buzz");
			return;
		}

		// Map all packages to internal format
		for (int i = 0; i < whitelistedPackages.length; i++)
			whitelistedPackages[i] = whitelistedPackages[i].replace('.', '/');

		instrumentation.addTransformer(new ClassFileTransformer() {
			@Override
			public byte[] transform(Module module, ClassLoader loader, String name, Class<?> typeIfLoaded, ProtectionDomain domain, byte[] cls) {
				// Only transform classes in whitelisted packages
				for (String whitelistedPackage : whitelistedPackages)
					if (name.startsWith(whitelistedPackage))
						return doTransform(cls);
				return null;
			}

			private static byte[] doTransform(byte[] cls) {
				ClassReader reader = new ClassReader(cls);
				ClassWriter writer = new ClassWriter(reader, 0);
				AccessInsertionClassVisitor accessChecker = new AccessInsertionClassVisitor(ASM_API, writer);
				reader.accept(accessChecker, 0);
				if (accessChecker.isTransformed())
					return writer.toByteArray();
				return null;
			}
		});
	}

	private static class AccessInsertionClassVisitor extends ClassVisitor {
		private static final Handle BOOTSTRAP_HANDLE = new Handle(H_INVOKESTATIC, Bootstrapper.class.getName().replace('.', '/'),
				"delegate", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", false);
		private String className;
		private boolean transformed;

		public AccessInsertionClassVisitor(int api, ClassVisitor cv) {
			super(api, cv);
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			super.visit(version, access, name, signature, superName, interfaces);
			className = name;
		}

		@Override
		public MethodVisitor visitMethod(int access, String declaredMethodName, String declaredMethodDesc, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, declaredMethodName, declaredMethodDesc, signature, exceptions);

			// Skip processing special case methods (constructors and static initializers)
			if (declaredMethodName.charAt(0) == '<')
				return mv;

			return new MethodVisitor(api, mv) {
				private int currentLine = -1;

				@Override
				public void visitLineNumber(int line, Label start) {
					super.visitLineNumber(line, start);
					currentLine = line;
				}

				@Override
				public void visitMethodInsn(int opcode, String invocationOwner, String invokedMethodName,
				                            String invokedMethodDesc, boolean isInterface) {
					super.visitMethodInsn(opcode, invocationOwner, invokedMethodName, invokedMethodDesc, isInterface);

					// Insert a call after calls to JavaFX methods to check if we're on the JFX thread.
					if (invocationOwner.startsWith("javafx")) {
						// Skip calls to platform methods. These are used to properly check if the calling code
						// is on the FX thread, and re-dispatching to execute on it via 'runLater'
						if (invocationOwner.equals("javafx/application/Platform"))
							return;

						// Pass along keys for the current location, and the target JavaFX method location
						String ourSignature = Keying.key(className, declaredMethodName, declaredMethodDesc, currentLine);
						String fxSignature = Keying.signature(invocationOwner, invokedMethodName, invokedMethodDesc);
						mv.visitInvokeDynamicInsn("acc-check", "()V", BOOTSTRAP_HANDLE, ourSignature, fxSignature);
						transformed = true;
					}
				}
			};
		}

		/**
		 * @return {@code true} when at least one check call has been inserted.
		 */
		public boolean isTransformed() {
			return transformed;
		}
	}
}
