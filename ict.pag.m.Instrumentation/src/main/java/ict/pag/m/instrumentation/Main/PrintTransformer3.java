package ict.pag.m.instrumentation.Main;

import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashSet;

import javax.xml.XMLConstants;

import ict.pag.m.instrumentation.Util.ConfigureUtil;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

public class PrintTransformer3 implements ClassFileTransformer {

	private String agentArgs;

	private HashSet<String> visitedClass = new HashSet<>();

	public PrintTransformer3(String agentArgs) {
		this.agentArgs = agentArgs;

	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		byte[] transformed = null;

//		System.out.println("***[loader]"+loader);
		if (visitedClass.contains(className))
			return null;

		visitedClass.add(className);

		ClassPool pool = ClassPool.getDefault();
		/**
		 * [problem solved 1] Adding current classloader. </br>
		 * cannot find the classes because : Javaassist and Tomcat maintain different
		 * classloader.
		 */
		pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
//		pool.appendClassPath(new LoaderClassPath(loader));

		/**
		 * print request content when running
		 */
		if (isHttpRequest(className)) {
			try {
				CtClass clazz = pool.get(className.replaceAll("/", "."));
				boolean flag = false;

				for (CtMethod mthd : clazz.getDeclaredMethods()) {
					if (!mthd.isEmpty()) {
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
//		} else if (ConfigureUtil.isApplicationClass(className) && !ConfigureUtil.isExclude(className)) {
		} else if (ConfigureUtil.isApplicationClass(className)) {
//			System.out.println("***[instrumental class] " + className);
			String debug_mthd = "";
			int debug_i = -2;
			try {
				CtClass clazz = pool.get(className.replaceAll("/", "."));

				/** exclude interface */
				if (Modifier.isInterface(clazz.getModifiers()))
					return null;

				boolean flag = false;
				for (CtMethod mthd : clazz.getDeclaredMethods()) {

					if (Modifier.isNative(mthd.getModifiers()))
						continue;

					/** exclude abstract methods */
					if (!mthd.isEmpty() && !Modifier.isAbstract(mthd.getModifiers())) {
						/**
						 * insert print statement before each framework and application code invoke
						 * instruction
						 */
//						/**WARNING! for avoiding runtime null pointer dereference error in javassist,</br>
//						 * 			donot use hashcode or format like '$_.getClass.getName()'*/
						mthd.instrument(new ExprEditor() {
							/** callsite */
							public void edit(MethodCall m) throws CannotCompileException {
								String originalStmt = "$_ = $proceed($$);";

								/** [IMPORTANT] add call site object runtime info */
//								String baseInfo = "";
//								String callTargetClassDeclare = m.getClassName().replaceAll("\\.", "/");
//								if (ConfigureUtil.isApplicationClass(callTargetClassDeclare))
//									baseInfo = getBaseInfo(m);
								
								try {
									if(m.getMethod()!=null) {
//										String mtdName = m.getMethodName();
////										System.out.println("********"+mtdName);
//										if(mtdName!=null&&mtdName.equals("toString"))
//											return;
										
										String callsite = m.getClassName() + "." + m.getMethodName() + m.getSignature() + "["
												+ m.getLineNumber() + "]";
										String belongTo = mthd.getLongName().substring(0, mthd.getLongName().indexOf('('))
												+ mthd.getSignature();
										
										String base = "\"\"";
										if(Modifier.isStatic(m.getMethod().getModifiers())) {
											base = "\"[base]static\"";
										}else {
//											base = "\"[base]\"+$0.hashCode()";
											base = "\"[base]\"+(($w)$0).getClass().getName()";
										}
										
										
										String insert1  = "System.out.println(\"[callsite]\" "
												+ "+ \"[\" + Thread.currentThread().getId() + \"]\" "
												+ "+" + "\"" + callsite + "\" "
												+ "+ \"" + belongTo + "\""
												+ "+" + base
												+ ");";
										
										/*return may be null*/
										String ret2 = "\"\"";
										if(m.getMethod().getReturnType()!=null){
											String returnName = m.getMethod().getReturnType().getName();
											if(returnName != null && returnName.equals("void")) {
												ret2 = "\"[return]void\"";
											}else {
//												ret2 = "\"[return]\" + $_";
												ret2 = "\"[return]\" + obj.getClass().getName()";
											}
										}else {
											ret2 = "\"[return]null\"";
										}
										
										String insert21  = "System.out.println(\"[returnSite]\" "
												+ "+ \"[\" + Thread.currentThread().getId() + \"]\" "
												+ "+" + "\"" + callsite + "\" "
												+ "+ \"" + belongTo + "\""
												+ "+" + base
												+ "+" + "\"[return]null\""
												+ ");";
										String insert22  = "System.out.println(\"[returnSite]\" "
												+ "+ \"[\" + Thread.currentThread().getId() + \"]\" "
												+ "+" + "\"" + callsite + "\" "
												+ "+ \"" + belongTo + "\""
												+ "+" + base
												+ "+" + ret2
												+ ");";
										
										/*openkm will dismiss this for method code limit*/
										String insert2 = "Object obj = (($w)$_);"
												+ "if(obj == null) {"
												+ insert21
												+ "}else {"
												+ insert22
												+ "}";
//										String replaceString = "{"  + " $_ = $proceed($$);"  + insert + "}";
										
										StringBuffer sb = new StringBuffer();
										sb.append("{");
										sb.append(insert1);
										sb.append(originalStmt);
										sb.append(insert2);
										sb.append("}");
										
//										System.out.println(insert2);
										
										m.replace(sb.toString());
									}
								} catch (NotFoundException e) {
									System.err.println("NotFoundException in method call!");
								}
							}

							/**
							 * field access
							 * 
							 * @throws CannotCompileException
							 */
							public void edit(FieldAccess f) throws CannotCompileException {
								String originalStmt = "$_ = $proceed($$);";

								if (f != null && f.getClassName() != null) {
									String belong2Class = f.getClassName().replaceAll("\\.", "/");
									if (ConfigureUtil.isApplicationClass(belong2Class)) {
										try {
											CtField field = f.getField();
											if(field != null) {
												
												int line = f.getLineNumber();
												String threadID = "\"[\"+Thread.currentThread().getId()+\"]\"";
												String lineNum = "\"[" + line + "]\"";
												String fieldSig =  "\"[signature]\" + \"" + field.toString() + "\"";	
												
												String collections = "\"\"";
												String declareType = f.getSignature();
												if(declareType.equals("Ljava/util/Map;") 
														|| declareType.equals("Ljava/util/HashMap;") 
														|| declareType.equals("Ljava/util/HashSet;")
														|| declareType.equals("Ljava/util/Set;")) {
													collections = "\"[collection]\" + $_";
												}
												
												String base = "\"\"";
												if(Modifier.isStatic(field.getModifiers())) {
													base = "\"[base]static\"";
												}else {
//													/*if base is null, runtime will also report*/
//													base = "\"[base]\"+$0.hashCode()";
//													base = "\"[base]\"+$0";
													base = "\"[base]\"+(($w)$0).getClass().getName()";
												}

												String insert = "\"\"";
												String type=null;
												if(f.isReader()) {
													type = "\"[field read]\"";
													String insert1 = "System.out.println(" + type 
															+ "+" + threadID 
															+ "+" + lineNum
															+ "+" + fieldSig 
//															+ "+ \"[runtimeType]\" + $_.getClass().getName()"
															+ "+ \"[runtimeType]\" + obj.getClass().getName()"
															+ "+" + collections
//															+ "+ \"[fieldObject]\" + obj.hashCode()"
//															+ "+ \"[fieldObject]\" + obj"
//															+ "+ \"[fieldObject]\" + $_.hashCode()"
////															+ "+ \"[fieldObject]\" + $_"
															+ "+" + base
															+ ");";
													String insert2 = "System.out.println(" + type 
															+ "+" + threadID 
															+ "+" + lineNum
															+ "+" + fieldSig 
															+ "+ \"[fieldObject]null\""
															+ "+" + base
															+ ");";
													
													insert = "Object obj = (($w)$_);"
															+ "if(obj == null) {"
															+ insert2
															+ "}else {"
															+ insert1
															+ "}";
//													originalStmt = "$_ = $proceed();";
												}else if(f.isWriter()) {
													type = "\"[field write]\"";
													String insert1  = "System.out.println(" + type 
															+ "+" + threadID 
															+ "+" + lineNum
															+ "+" + fieldSig 
//															+ "+ \"[runtimeType]\" + $1.getClass().getName()"
															+ "+ \"[runtimeType]\" + obj.getClass().getName()"
//															+ "+ \"[fieldObject]\" + obj"
//															+ "+ \"[fieldObject]\" + $1.hashCode()"
//															+ "+ \"[fieldObject]\" + obj.hashCode()"
////															+ "+ \"[fieldObject]\" + $1"
															+ "+" + base
															+ ");";
													String insert2 = "System.out.println(" + type 
															+ "+" + threadID 
															+ "+" + lineNum
															+ "+" + fieldSig 
															+ "+ \"[fieldObject]null\""
															+ "+" + base
															+ ");";
													
													insert = "Object obj = (($w)$1);"
															+ "if(obj == null) {"
															+ insert2
															+ "}else {"
															+ insert1
															+ "}";
//													originalStmt = "$proceed($$);";
												}
												
												StringBuffer sb = new StringBuffer();
												sb.append("{");
												sb.append(originalStmt);
												sb.append(insert);
												sb.append("}");
												
												/*openkm will dismiss this for method code limit*/
												f.replace(sb.toString());
											}
										} catch (NotFoundException e) {
											System.err.println("NotFoundException in field access!");
										}
									}
								}
							}

//							private String getBaseInfo(MethodCall m) {
//								String ret = "";
//								try {
//									if (m.getMethod() != null) {
//										if (!Modifier.isStatic(m.getMethod().getModifiers())) {
////											String insert = "System.out.println(\"[callsite info]\");";
//											String insert0 = "System.out.println(\"[callsite obj]\"+$0);";
//											String insert1 = "System.out.println(\"[callsite class]\"+$0.getClass().getName() + \":\" + $0.getClass().isSynthetic());";
//											String insert2 = "System.out.println(\"[callsite super]\"+$0.getClass().getSuperclass().getName());";
//
//											pool.importPackage("java.lang.reflect.Field");
//											String insert3 = "Field[] fields = $0.getClass().getDeclaredFields();"
//													+ "for (int i = 0; i < fields.length; i++) {"
//													+ "fields[i].setAccessible(true);" + "if(fields[i].get($0)!=null){"
//													+ "System.out.println(\"[callsite field]\"  + fields[i].getName() + \":\"+fields[i].getGenericType().getTypeName()+\":\" +fields[i].get($0).getClass().getName());"
//													+ "}else{System.out.println(\"[callsite field]\"  + fields[i].getName() + \":null\");}"
//													+ "}";
//											/** WARNING: in ruoyi, this position will make appliation cannot run */
//											ret = insert0 + insert1 + insert2;
//										}
//									}
//								} catch (NotFoundException e) {
//									e.printStackTrace();
//								}
//								return ret;
//							}
						});

						/**
						 * insert at the very beginning of each method ,</br>
						 * for knowing each invoke when running
						 */
						StringBuffer sb = new StringBuffer();
						sb.append("{");

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
						/* do not distinguish unreachable or not when instrumenting */
						sb.append(");");

						if (!Modifier.isStatic(mthd.getModifiers())) {
//							String insert1 = "System.out.println(\"[base class]\"+$0.getClass().getName() + \"[base]\" + $0.hashCode() );";
//							String insert1 = "System.out.println(\"[base class]\"+$0.getClass().getName() + \"[base]\" + $0 );";
							String insert1 = "System.out.println(\"[base class]\"+$0.getClass().getName() );";
							sb.append(insert1);
							
							pool.importPackage("java.lang.reflect.Field");
							pool.importPackage("java.util.Set");
							pool.importPackage("java.util.Map");
							pool.importPackage("java.util.Collection");
							String insert3 = "Field[] fields = $0.getClass().getDeclaredFields();"
									+ "for (int i = 0; i < fields.length; i++) {" + "fields[i].setAccessible(true);"
									+ "if(fields[i].get($0)!=null){"
									+ "String actualClass=fields[i].get($0).getClass().getName();"
									+ "System.out.println(\"[base field]\"  + fields[i].getName() + \":\"+fields[i].getGenericType().getTypeName()+\":\" +actualClass);"
//									+ "if(actualClass.startsWith(\"java.util.HashMap\")){"
//									+ "System.out.println(\"[runtime field map]\" + fields[i].get($0));"
//									+ "if(Map.class.isAssignableFrom(fields[i].get($0).getClass())){"
//									+ "Map collection = (Map)(fields[i].get($0));"
//									+ "System.out.println(\"[runtime field map]\" + collection);" + "}"

//									+ "if(Set.class.isAssignableFrom(fields[i].get($0).getClass())){"
//									+ "Set collection = (Set)(fields[i].get($0));"
//									+ "System.out.println(\"[runtime field set]\" + collection);"
////									+ "Set keySet = fields[i].get($0).keySet();"
////									+ "for (Object key : fields[i].get($0).keySet()) {"
////									+ "if (fields[i].get($0).get(key) != null) {"
////									+ "System.out.println(key.getClass() + \":\" + fields[i].get($0).get(key).getClass());"
////									+ "} " 
////									+ "}"
//									+ "}"

									+ "}else{System.out.println(\"[base field]\"  + fields[i].getName()+ \":\"+fields[i].getGenericType().getTypeName() + \":null\");}"
									+ "}";
							sb.append(insert3);
						}

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
						/* do not distinguish unreachable or not when instrumenting */
						sb2.append(");");

						sb2.append("}");
						mthd.insertAfter(sb2.toString());

					}
				}
				if (flag)
					transformed = clazz.toBytecode();

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
	
}
