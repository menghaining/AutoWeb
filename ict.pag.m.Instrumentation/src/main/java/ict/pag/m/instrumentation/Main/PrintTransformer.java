package ict.pag.m.instrumentation.Main;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import ict.pag.m.instrumentation.Util.ConfigureUtil;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Mnemonic;
import javassist.bytecode.Opcode;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

public class PrintTransformer implements ClassFileTransformer {

	private String agentArgs;
//	private static Set<String> invokes;
	private HashSet<String> visitedClass = new HashSet<>();

	public PrintTransformer(String agentArgs) {
		this.agentArgs = agentArgs;

//		try {
//			Parse2InstrumentationInfo.parseInfo();
//			/** like com.instrument.testcase.A.method2(Ljava/lang/String;)I */
////			Set<InstrumentationPair> infos = Parse2InstrumentationInfo.getInfoSet();
//			invokes = Parse2InstrumentationInfo.getInvokeSet();
//
//		} catch (IOException | ParseException e) {
//			System.err.println("[parse JSON Exception]");
//		}
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		byte[] transformed = null;

		if (visitedClass.contains(className))
			return null;

		visitedClass.add(className);

		ClassPool pool = ClassPool.getDefault();
		/**
		 * [problem solved 1] Adding current classloader. cannot find the classes
		 * because : Javaassist and Tomcat maintain different classloader.
		 */
		pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
		long threadId = Thread.currentThread().getId();
		/**
		 * print request content when running
		 */
		if (isHttpRequest(className)) {
			try {
				CtClass clazz = pool.get(className.replaceAll("/", "."));
				boolean flag = false;

				for (CtMethod mthd : clazz.getDeclaredMethods()) {
					if (!mthd.isEmpty()) {
//						System.out.println(mthd.getLongName());
						StringBuffer sb = new StringBuffer();
						sb.append("{");

						StringBuffer sb_end = new StringBuffer();
						sb_end.append("{");

						if (mthd.getLongName().equals(
								"javax.servlet.http.HttpServlet.service(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse)")) {

							// print the request content
							sb.append(
									"System.out.println(\"[ReqURL]\" + \"[\" + Thread.currentThread().getId() + \"]\" + req.getRequestURL());");
							sb_end.append(
									"System.out.println(\"[ReqURL_end]\"+ \"[\" + Thread.currentThread().getId() + \"]\"  + req.getRequestURL());");
						}
						sb.append("}");
						sb_end.append("}");

						mthd.insertBefore(sb.toString());
						mthd.insertAfter(sb_end.toString());
						flag = true;

					}
				}
				if (flag)
					transformed = clazz.toBytecode();
			} catch (NotFoundException e) {
				System.err.println("[error!] no found exception in javassist! ");
			} catch (CannotCompileException e) {
				System.err.println("[error!] can not compile exception in javassist! ");
			} catch (IOException e) {
				System.err.println("[error!] IO exception in javassist! ");
			}
		} else if (ConfigureUtil.isApplicationClass(className) && !ConfigureUtil.isExclude(className)) {
//			System.out.println("*START* " + className);

			String debug_mthd = "";
			int debug_i = -2;
			try {
				CtClass clazz = pool.get(className.replaceAll("/", "."));

				/** exclude interface */
				if (Modifier.isInterface(clazz.getModifiers()))
					return null;

				boolean flag = false;
				for (CtMethod mthd : clazz.getDeclaredMethods()) {
//					System.out.println("\t*Method start* " + mthd.getLongName());

					if (Modifier.isNative(mthd.getModifiers()))
						continue;

					/** exclude abstract methods */
					if (!mthd.isEmpty() && !Modifier.isAbstract(mthd.getModifiers())) {
//						System.out.println("[test]" + mthd.getLongName());
						/**
						 * insert print statement before each framework and application code invoke
						 * instruction
						 */
						mthd.instrument(new ExprEditor() {
							public void edit(MethodCall m) throws CannotCompileException {
								String callsite = m.getClassName() + "." + m.getMethodName() + m.getSignature() + "["
										+ m.getLineNumber() + "]";
								String belongTo = mthd.getLongName().substring(0, mthd.getLongName().indexOf('('))
										+ mthd.getSignature();

								String insertCallsite = "System.out.println(" + "\"" + callsite + "\");";

								String insert = "System.out.println(\"[callsite]\" + \"[\" + Thread.currentThread().getId() + \"]\" +"
										+ "\"" + callsite + "\" + \"" + belongTo + "\");";

								String replaceString = "{" + insert + " $_ = $proceed($$);}";
								m.replace(replaceString);
//								m.replace("{ insertCallsite; $_ = $proceed($$);}");
							}
						});

//						HashMap<Integer, ArrayList<String>> line2Invoke = new HashMap<Integer, ArrayList<String>>();
//						collectAllInvokes(mthd, line2Invoke);
//						for (Integer key : line2Invoke.keySet()) {
//							ArrayList<String> toBeInserted = line2Invoke.get(key);
//							Collections.reverse(toBeInserted);
//							int line = key.intValue();
//							for (String ss : toBeInserted) {
//								StringBuffer sb = new StringBuffer();
//								ss = ss + "[" + line + "]";
//								sb.append("{");
//
//								sb.append(
//										"System.out.println(\"[callsite]\" + \"[\" + Thread.currentThread().getId() + \"]\" +");
//
//								sb.append("\"");
//								sb.append(ss);
//								sb.append("\"");
//
//								sb.append("+");
//
//								sb.append("\"[\"");
//								sb.append("+");
//								sb.append("\"");
//								sb.append(mthd.getLongName().substring(0, mthd.getLongName().indexOf('(')));
//								sb.append("\"");
//								sb.append("+");
//								sb.append("\"");
//								sb.append(mthd.getSignature());
//								sb.append("\"");
//								sb.append("+");
//								sb.append("\"]\"");
//
//								sb.append(");");
//								sb.append("}");
//
//								debug_i = line;
//								debug_mthd = mthd.getLongName();
//
//								System.out.println("\t\t*insertAt* " + line + ":" + sb.toString());
//								mthd.insertAt(line, sb.toString());
//							}
//						}

						/**
						 * insert at the very beginning of each method ,</br>
						 * for knowing each invoke when running
						 */
						StringBuffer sb = new StringBuffer();
						sb.append("{");
//						sb.append("System.out.println();");

						/**
						 * 1. print method long name
						 */
						String s1 = "System.out.println(\"[call method]\" + \"[\" + Thread.currentThread().getId() + \"]\" +";
						sb.append(s1);
						sb.append("\"");
						sb.append(mthd.getLongName().substring(0, mthd.getLongName().indexOf('(')));
						sb.append("\"");
						sb.append("+");
						sb.append("\"");
						sb.append(mthd.getSignature());
						sb.append("\"");
						// do not distinguish unreachable or not when instrumenting
						sb.append(");");
//						if (isConcernedMethod(mthd, invokes)) {
//							sb.append(");");
//						} else {
//							sb.append("+\"[normal]\");");
//						}

						/**
						 * 2. print current stack
						 */
//						if (mthd.getName().contains("setId")) {
//							sb.append("System.out.println(\"[current stack] :\");");
//							sb.append("StackTraceElement[] stackele = Thread.currentThread().getStackTrace();\r\n"
//									+ "						if (stackele != null) {\r\n"
//									+ "							for (int i = 0; i < stackele.length; i++) {\r\n"
//									+ "								System.out.println(\"\" + stackele[i]);\r\n"
//									+ "							}\r\n" + "						}");
//						}
//						sb.append("System.out.println(\"[current stack] :\");");
//						sb.append("StackTraceElement[] stackele = Thread.currentThread().getStackTrace();\r\n"
//								+ "						if (stackele != null) {\r\n"
//								+ "							for (int i = 0; i < stackele.length; i++) {\r\n"
//								+ "								System.out.println(\"\" + stackele[i]);\r\n"
//								+ "							}\r\n" + "						}");

						sb.append("}");
						mthd.insertBefore(sb.toString());
						flag = true;

						// actual insert operation

						/***
						 * means this invoke completed //
						 */
						StringBuffer sb2 = new StringBuffer();
						sb2.append("{");
						String s2 = "System.out.println(\"[call method finished]\" + \"[\" + Thread.currentThread().getId() + \"]\" +";
						sb2.append(s2);
						sb2.append("\"");
						sb2.append(mthd.getLongName().substring(0, mthd.getLongName().indexOf('(')));
						sb2.append("\"");
						sb2.append("+");
						sb2.append("\"");
						sb2.append(mthd.getSignature());
						sb2.append("\"");
						// do not distinguish unreachable or not when instrumenting
						sb2.append(");");
//						if (isConcernedMethod(mthd, invokes)) {
//							sb2.append(");");
//						} else {
//							sb2.append("+\"[normal]\");");
//						}

						sb2.append("}");
						mthd.insertAfter(sb2.toString());

					}
//					System.out.println("\t*Method end* " + mthd.getLongName());
				}
				if (flag)
					transformed = clazz.toBytecode();

//				System.out.println("*END* " + className);
			} catch (Exception e1) {
				System.out.println("*EXCEPTION* " + className);
				if (e1 instanceof NotFoundException)
					System.err.println("[NotFoundException] " + className);
				else if (e1 instanceof CannotCompileException) {
					System.err.println("[CannotCompileException] " + className + ":1:" + e1.getLocalizedMessage());
					System.err.println("[CannotCompileException] " + className + ":2:" + e1.getCause());
					System.err.println("[CannotCompileException] " + ":method:" + debug_mthd);
					System.err.println("[CannotCompileException] " + ":line:" + debug_i);
					e1.printStackTrace();
				} else if (e1 instanceof IOException)
					System.err.println("[IOException] " + className);
				else
					System.err.println("[OTHER EXCEPTION] " + className);
			}

		}

		return transformed;
	}

	private boolean isConcernedMethod(CtMethod mthd, Set<String> invokes) {
		if (invokes == null)
			return false;

		String longname = mthd.getLongName();
		String prefix = longname.substring(0, longname.indexOf('('));
		String suffix = mthd.getSignature();
		String mthdSig = prefix + suffix;
		if (invokes.contains(mthdSig)) {
			return true;
		}

		return false;
//		return true;
	}

	/**
	 * return all invokeInst line number
	 * 
	 * @param applicationTypeSet
	 */
	private void collectAllInvokes(CtMethod method, HashMap<Integer, ArrayList<String>> line2Invoke) {

		MethodInfo info = method.getMethodInfo2();
		ConstPool pool = info.getConstPool();
		CodeAttribute code = info.getCodeAttribute();
		if (code == null)
			return;

		String prefix = method.getLongName();
//		System.out.println("~method:" + prefix);

		CodeIterator iterator = code.iterator();
		while (iterator.hasNext()) {
			int pos;
			try {
				pos = iterator.next();
			} catch (BadBytecode e) {
				throw new RuntimeException(e);
			}

			String instString = toInstructionString(iterator, pos, pool);
//			if (!instString.equals("null"))
//				System.out.println("~\t inst:" + instString);

//			if (!(ConfigureUtil.isFrameworkCall(instString)
//					|| ConfigureUtil.isApplicationClass(instString.replaceAll("\\.", "/"))))
//				continue;
//
//			System.out.println("~\t call:" + instString);

			if (!instString.equals("null")) {
				int lineNumber = method.getMethodInfo().getLineNumber(pos);
//				System.out.println("instruction: " + instString + " [line]" + lineNumber);
				if (lineNumber > 0) {
					Integer currInteger = new Integer(lineNumber);
					if (line2Invoke.containsKey(currInteger)) {
						line2Invoke.get(currInteger).add(instString);
					} else {
						ArrayList<String> tmp = new ArrayList<String>();
						tmp.add(instString);
						line2Invoke.put(currInteger, tmp);
					}
				}
			}

		}

	}

	/**
	 * INVOKEVIRTUAL/INVOKESPECIAL/INVOKESTATIC/INVOKEINTERFACE/INVOKEDYNAMIC</br>
	 * putfield
	 */
	private String toInstructionString(CodeIterator iter, int pos, ConstPool pool) {
		int opcode = iter.byteAt(pos);

		if (opcode > Mnemonic.OPCODE.length || opcode < 0)
			throw new IllegalArgumentException("Invalid opcode, opcode: " + opcode + " pos: " + pos);

//		String opstring = Mnemonic.OPCODE[opcode];
		switch (opcode) {
		case Opcode.INVOKEVIRTUAL:
		case Opcode.INVOKESPECIAL:
		case Opcode.INVOKESTATIC:
			return methodInfo(pool, iter.u16bitAt(pos + 1));
		case Opcode.INVOKEINTERFACE:
			return interfaceMethodInfo(pool, iter.u16bitAt(pos + 1));
		case Opcode.INVOKEDYNAMIC:
			return "" + iter.u16bitAt(pos + 1);
		}

		return "null";
	}

	private String interfaceMethodInfo(ConstPool pool, int index) {
		return pool.getInterfaceMethodrefClassName(index) + "." + pool.getInterfaceMethodrefName(index)
				+ pool.getInterfaceMethodrefType(index);
	}

	private String methodInfo(ConstPool pool, int index) {
		return pool.getMethodrefClassName(index) + "." + pool.getMethodrefName(index) + pool.getMethodrefType(index);
	}

	private static String fieldInfo(ConstPool pool, int index) {
		return "[Field]" + pool.getFieldrefClassName(index) + "." + pool.getFieldrefName(index) + "("
				+ pool.getFieldrefType(index) + ")";
	}

	/**
	 * filter the methods that has annotations or other tags in xml files
	 * 
	 * TODO: add more form tags like file defined !!
	 */
	private boolean hasTags(CtMethod mthd) {
		/**
		 * Form 1. annotations
		 */
		Object[] annos = mthd.getAvailableAnnotations();
		if (annos != null && annos.length > 0) {
			return true;
		}
		/**
		 * TODO: Form 2. defined in files
		 */

		/**
		 * TODO: Form3. user configure?
		 */

		return false;
	}

	/**
	 * Whether it is javax.servlet.http.HttpServlet class
	 * 
	 * @param className
	 * @return
	 */
	private boolean isHttpRequest(String className) {
		if (className.equals("javax/servlet/http/HttpServlet")) {
			return true;
		}
		return false;
	}

	private boolean isFrameworkClass(String className) {
//		if (className.startsWith("java") || className.startsWith("javax") || className.startsWith("jdk/")
//				|| className.startsWith("org/apache") || className.startsWith("sun/")
//				|| className.startsWith("com/sun/") || className.startsWith("org/w3c/dom/")
//				|| className.startsWith("sun/reflect") || className.startsWith("org/hsqldb")
////				|| className.startsWith("org/spring") 
//				|| className.startsWith("org/bouncycastle/") || className.startsWith("org/ietf/jgss")
//				|| className.startsWith("org/xml/sax/") || className.startsWith("org/jcp/xml/dsig/internal/dom/"))
//			return false;
//		return true;
		if (className.startsWith("org/spring"))
			return true;

		return false;
	}

	/**
	 * TODO: DELETE!! </br>
	 * for find the correct signature
	 * 
	 * @param className
	 */
	private void printHttpRequest(String className) {
		if (className.equals("javax/servlet/http/HttpServlet")) {
//			System.out.println("!!!!!!!!!!!!!find it!!!!!!!!!");
			ClassPool pool = ClassPool.getDefault();
			/**
			 * Adding current classloader. cannot find the classes because : Javaassist and
			 * Tomcat maintain different classloader.
			 */
			pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
			try {
				CtClass clazz = pool.get(className.replaceAll("/", "."));
				for (CtMethod mthd : clazz.getDeclaredMethods()) {
					if (!mthd.isEmpty()) {
						System.out.println("!!!!!!!!!!!!!!" + mthd.getLongName());
						StringBuffer sb = new StringBuffer();
						sb.append("{");
						// print method long name
						sb.append("System.out.println(\"[Instrument servlet method] \" + ");
						sb.append("\"");
						sb.append(mthd.getLongName());
						sb.append("\"");
						sb.append(");\n");

						if (mthd.getLongName().equals(
								"javax.servlet.http.HttpServlet.service(javax.servlet.ServletRequest,javax.servlet.ServletResponse)")) {
							System.out.println("@@@@@@@@@@@@@@@@real find");
							// print the request
							sb.append("System.out.println(\"[Req] \" + req);");

						}
						sb.append("}");
					}
				}
			} catch (NotFoundException e) {
				System.err.println("no found exception in javassist! ");
			}
		}

	}

}
