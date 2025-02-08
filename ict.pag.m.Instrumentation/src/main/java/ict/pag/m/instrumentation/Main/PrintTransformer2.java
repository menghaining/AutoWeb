package ict.pag.m.instrumentation.Main;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import ict.pag.m.instrumentation.Util.ConfigureUtil;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class PrintTransformer2 implements ClassFileTransformer {
	private String agentArgs;
//	private static Set<String> invokes;

	public PrintTransformer2(String agentArgs) {
		this.agentArgs = agentArgs;

	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		byte[] transformed = null;

		ClassPool pool = ClassPool.getDefault();
		pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));

		if (ConfigureUtil.isApplicationClass(className)) {
			try {
				CtClass clazz = pool.get(className.replaceAll("/", "."));
				System.out.println("*START* " + className);
				for (CtMethod mthd : clazz.getDeclaredMethods()) {
					System.out.println("\t*Method start* " + mthd.getLongName());
					mthd.instrument(new ExprEditor() {
						public void edit(MethodCall m) throws CannotCompileException {
							String callsite = "[callsite]" + m.getClassName() + "." + m.getMethodName()
									+ m.getSignature() + "[" + m.getLineNumber() + "]";
							String insertCallsite = "System.out.println(" + "\"" + callsite+ "\");";
							String replaceString = "{" + insertCallsite + " $_ = $proceed($$);}";
							m.replace(replaceString);
//							m.replace("{ insertCallsite; $_ = $proceed($$);}");
						}
					});
				}

				transformed = clazz.toBytecode();
				System.out.println("*END* " + className);
			} catch (NotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CannotCompileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return transformed;
	}
}
