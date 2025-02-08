package ict.pag.m.instrumentation.Main;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashSet;
import ict.pag.m.instrumentation.Util.ConfigureUtil;
import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.annotation.Annotation;

public class RemoveConfigurationTransformer implements ClassFileTransformer {
	private String agentArgs;

	private HashSet<String> visitedClass = new HashSet<>();

	public RemoveConfigurationTransformer(String agentArgs) {
		this.agentArgs = agentArgs;

	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		byte[] transformed = null;

		if (visitedClass.contains(className))
			return null;

		visitedClass.add(className);

		ClassPool pool = ClassPool.getDefault();
		pool.insertClassPath(new ClassClassPath(this.getClass()));

		/**
		 * [problem solved 1] Adding current classloader. </br>
		 * cannot find the classes because : Javaassist and Tomcat maintain different
		 * classloader.
		 */
		pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
//		pool.appendClassPath(new LoaderClassPath(loader));

		System.out.println("[class]" + className);
		System.out.println("[current thread context loader]" + Thread.currentThread().getContextClassLoader());
		System.out.println("[loader]" + loader);

		if (ConfigureUtil.isApplicationClass(className) || className.startsWith("org/apache/catalina/")) {
			CtClass clazz;
			try {
				clazz = pool.get(className.replaceAll("/", "."));

				/** exclude interface */
				if (Modifier.isInterface(clazz.getModifiers()))
					return null;

				if (ConfigureUtil.isApplicationClass(className)) {
					AttributeInfo annos0 = clazz.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
					if (annos0 != null) {
						AnnotationsAttribute annos = (AnnotationsAttribute) annos0;
						System.out.println("------before-----");
						Annotation rmanno = null;
						for (Annotation anno : annos.getAnnotations()) {
							System.out.println(anno);
							if (rmanno == null)
								rmanno = anno;
						}
						if (rmanno != null) {
							annos.removeAnnotation(rmanno.getTypeName());
						}
						System.out.println("------after-----");
						for (Annotation anno : ((AnnotationsAttribute) clazz.getClassFile()
								.getAttribute(AnnotationsAttribute.visibleTag)).getAnnotations()) {
							System.out.println(anno);
						}
					}
				}

				for (CtMethod mthd : clazz.getDeclaredMethods()) {
					if (Modifier.isNative(mthd.getModifiers()))
						continue;
					if (!mthd.isEmpty() && !Modifier.isAbstract(mthd.getModifiers())) {
						StringBuffer sb = new StringBuffer();
						sb.append("{");

						String s1 = "System.out.println(\"[call method]\" + \"[\" + Thread.currentThread().getId() + \"]\" +";
						sb.append(s1);
						sb.append("\"");
						sb.append(mthd.getLongName().substring(0, mthd.getLongName().indexOf('(')));
						sb.append("\"");
						sb.append("+");
						sb.append("\"");
						sb.append(mthd.getSignature());
						sb.append("\"");
						/* do not distinguish unreachable or not when instrumenting */
						sb.append(");");

//						String simpleClassName = className.substring(className.lastIndexOf('/') + 1);
//						String rt = "System.out.println(\"[runtime loader]\" + this.getClass().getClassLoader()";
//						sb.append("System.out.println( \"" + className + "\");");
//						sb.append(rt);
//
//						System.out.println("System.out.println( \"" + className + "\");");
//						System.out.println(rt);
						
						sb.append("}");

						mthd.insertBefore(sb.toString());
					}
				}
				try {
					transformed = clazz.toBytecode();
				} catch (IOException | CannotCompileException e) {
					System.err.println("[removeConfigurationError] [IOException]|[CannotCompileException]");
				}
			} catch (NotFoundException | CannotCompileException e) {
				System.err.println("[removeConfigurationError] [NotFoundException]");
			}

		}

		return transformed;
	}

}
