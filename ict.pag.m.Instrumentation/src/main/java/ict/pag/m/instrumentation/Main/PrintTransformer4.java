package ict.pag.m.instrumentation.Main;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashSet;
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

/**
 * new instrumental write in 0620</br>
 */
public class PrintTransformer4 implements ClassFileTransformer {

	private String agentArgs;
	private int cc1 = 0;
	private int cc2 = 0;
	private HashSet<String> visitedClass = new HashSet<>();

	public PrintTransformer4(String agentArgs) {
		this.agentArgs = agentArgs;

	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
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
		pool.appendClassPath(new LoaderClassPath(loader));

		/**
		 * print request content when running
		 */
		/* in this case, instrument in tomcat / undertow to log request start and end */
		if (isTomcatHttpRequest(className) || isUndertowRequest(className)) {
//			cc2++;
//			System.out.println("**************************************"+cc2+"[class]"+className);
			try {
				CtClass clazz = pool.get(className.replaceAll("/", "."));
				boolean flag = false;

				pool.importPackage("javax.servlet.http.HttpServletRequest");
				pool.importPackage("javax.servlet.http.HttpServletResponse");
				pool.importPackage("javax.servlet.http.Cookie");
				pool.importPackage("java.util.Map");
				pool.importPackage("java.util.Set");
				pool.importPackage("java.util.Iterator");
				pool.importPackage("java.util.Arrays");

				pool.importPackage("java.util.logging.Logger");

				// TODO: add constructor instrumental
				for (CtMethod mthd : clazz.getDeclaredMethods()) {
					if (!mthd.isEmpty()) {
//						if (mthd.getLongName().equals(
//								"org.apache.catalina.connector.CoyoteAdapter.service(org.apache.coyote.Request,org.apache.coyote.Response)")) {
						if (mthd.getLongName().startsWith("org.apache.catalina.core.ApplicationFilterChain.doFilter")
								|| mthd.getLongName().startsWith("io.undertow.servlet.core.ManagedFilter.doFilter")) {
							StringBuffer sb = new StringBuffer();
							sb.append("{");

							StringBuffer sb_end = new StringBuffer();
							sb_end.append("{");

//							cc1++;
//							System.out.println("**************************************"+cc1+"[method]"+mthd.getLongName());

							// print the request content
//							sb.append(
//									"System.out.println(\"[ReqURL]\" + \"[\" + Thread.currentThread().getId() + \"]\" + req.requestURI()  + \"[hashcode]\" + req.hashCode());");
//							sb_end.append(
//									"System.out.println(\"[ReqURL_end]\"+ \"[\" + Thread.currentThread().getId() + \"]\"  + req.requestURI()  + \"[hashcode]\" + req.hashCode());");

							/**
							 * infos</br>
							 * .requestURI() request uri</br>
							 * .method() the request method, POST/GET/DELETE/......</br>
							 * .queryString() the String on the url,like ?xxx&xxx...</br>
							 * .getParameters() returns http request parameters</br>
							 * .getAttribute() is for server-side usage only
							 * 
							 */
							/* at before */

							/*
							 * [ReqStart][threadID]url[hashcode]xxx[method]mtd[queryString]xxx[param]xxx[
							 * pathInfo]xxx[user]xxx[sessionId]xxx[cookie]xxxxx
							 */
//							sb.append("if(request instanceof javax.servlet.http.HttpServletRequest){");
//							sb.append("javax.servlet.http.HttpServletRequest mhn_req=(javax.servlet.http.HttpServletRequest)request;");
							// TODO: although request / response also the names of parameters, but in
							// logicaldoc it cannot work
							// also $ 2 cannot refer to the sencond parameter
							// but, $args[0] refers to the first parameter, and $args[1] refers to the
							// second parameter
							// equal to above lines, just replace the "request" to $args[0]
							sb.append("if($args[0] instanceof javax.servlet.http.HttpServletRequest){");
							sb.append("javax.servlet.http.HttpServletRequest mhn_req=(javax.servlet.http.HttpServletRequest)($args[0]);");

							sb.append(
									"String mhn_URL=mhn_req.getRequestURL().toString();String mhn_mtd=mhn_req.getMethod();String mhn_pathinfo=mhn_req.getPathInfo();");
							sb.append(
									"String mhn_querystr=mhn_req.getQueryString();String mhn_rmuser=mhn_req.getRemoteUser();String mhn_sessionid=mhn_req.getRequestedSessionId();");
							sb.append("javax.servlet.http.Cookie[] mhn_cookies=mhn_req.getCookies();String mhn_cookies_str=\"\";");
							sb.append("if(mhn_cookies!=null) {for(int mhn_count=0;mhn_count<mhn_cookies.length;mhn_count++) {");
							sb.append("javax.servlet.http.Cookie mhn_cookie=mhn_cookies[mhn_count];");
							sb.append(
									"String mhn_cookie_name=mhn_cookie.getName();String mhn_cookie_val=mhn_cookie.getValue();String mhn_cookie_comment=mhn_cookie.getComment();");
							sb.append("String mhn_cookie_domain=mhn_cookie.getDomain();String mhn_cookie_path=mhn_cookie.getPath();");
							sb.append(
									"String mhn_cookie_info_line=\"[\"+mhn_count+\"]\"+\"[name]\"+mhn_cookie_name+\"[val]\"+mhn_cookie_val+\"[comment]\"+mhn_cookie_comment+\"[domain]\"+mhn_cookie_domain+\"[path]\"+mhn_cookie_path;");
							sb.append("mhn_cookies_str=mhn_cookies_str+mhn_cookie_info_line;");
							sb.append("}}");
							sb.append("String mhn_params_str=\"\";");
							// Javassist's compiler doesn't support generics.
							sb.append("java.util.Map mhn_paramNamesMap=mhn_req.getParameterMap();");
							sb.append("if(mhn_paramNamesMap!=null) {"
									+ "java.util.Set/*<java.util.Map.Entry<java.lang.String, java.lang.String[]>>*/ mhn_entries = mhn_req.getParameterMap().entrySet();"
									+ "if (!mhn_entries.isEmpty()){"
									+ "java.util.Iterator/*<java.util.Map.Entry<java.lang.String, java.lang.String[]>>*/ mhn_iterator = mhn_entries.iterator();"
									+ "while (mhn_iterator.hasNext()) {"
									+ "java.util.Map.Entry/*<java.lang.String, java.lang.String[]>*/ mhn_next = mhn_iterator.next();"
									+ "String mhn_attr=(String)mhn_next.getKey();" + "java.lang.Object[] mhn_vals = (java.lang.Object[])mhn_next.getValue();"
									+ "if(mhn_vals!=null){" + "for(int mhn_count=0;mhn_count<mhn_vals.length;mhn_count++){"
									+ "String mhn_val=(String)mhn_vals[mhn_count];" + "mhn_params_str=mhn_params_str+mhn_attr+\"=\"+mhn_val+\"&\";" + "}}" + "}"
									+ "}}");

//							sb.append(
//									"System.out.println(\"[ReqStart][\"+Thread.currentThread().getId()+\"]\"+mhn_URL+\"[hashcode]\"+request.hashCode()+\"[method]\"+mhn_mtd+\"[queryString]\"+mhn_querystr+\"[param]\"+mhn_params_str+\"[pathInfo]\"+mhn_pathinfo+\"[user]\"+mhn_rmuser+\"[sessionId]\"+mhn_sessionid+\"[cookie]\"+mhn_cookies_str);");
							// equal to above lines, just replace the "request" to $args[0]
							sb.append(
									"System.out.println(\"[ReqStart][\"+Thread.currentThread().getId()+\"]\"+mhn_URL+\"[hashcode]\"+($args[0]).hashCode()+\"[method]\"+mhn_mtd+\"[queryString]\"+mhn_querystr+\"[param]\"+mhn_params_str+\"[pathInfo]\"+mhn_pathinfo+\"[user]\"+mhn_rmuser+\"[sessionId]\"+mhn_sessionid+\"[cookie]\"+mhn_cookies_str);");

							sb.append("}");

							/* at end */

							/*
							 * [ReqEnd][threadID]url[hashcode]xxx[headers-cookie]set-cookie[status-code]
							 * response_status
							 */
//							sb_end.append(
//									"if((response instanceof javax.servlet.http.HttpServletResponse)&&(request instanceof javax.servlet.http.HttpServletRequest)){");
//							sb_end.append(
//									"javax.servlet.http.HttpServletResponse mhn_res=(javax.servlet.http.HttpServletResponse)response;javax.servlet.http.HttpServletRequest mhn_req=(javax.servlet.http.HttpServletRequest)request;");

							// equal to above lines, just replace the "request" to $args[0], just replace
							// the "response" to $args[1]
							sb_end.append(
									"if(($args[1] instanceof javax.servlet.http.HttpServletResponse)&&($args[0] instanceof javax.servlet.http.HttpServletRequest)){");
							sb_end.append(
									"javax.servlet.http.HttpServletResponse mhn_res=(javax.servlet.http.HttpServletResponse)($args[1]);javax.servlet.http.HttpServletRequest mhn_req=(javax.servlet.http.HttpServletRequest)($args[0]);");
							sb_end.append(
									"String mhn_URL=mhn_req.getRequestURL().toString();String mhn_headers_cookie=mhn_res.getHeaders(\"Set-Cookie\").toString();int mhn_status_code=mhn_res.getStatus();");
//							sb_end.append(
//									"System.out.println(\"[ReqEnd][\"+Thread.currentThread().getId()+\"]\"+mhn_URL+\"[hashcode]\"+request.hashCode()+\"[headers-cookie]\"+mhn_headers_cookie+\"[status-code]\"+mhn_status_code);");
							// just replace the "request" to $args[0]
							sb_end.append(
									"System.out.println(\"[ReqEnd][\"+Thread.currentThread().getId()+\"]\"+mhn_URL+\"[hashcode]\"+($args[0]).hashCode()+\"[headers-cookie]\"+mhn_headers_cookie+\"[status-code]\"+mhn_status_code);");

							sb_end.append("}");

							sb.append("}");
							sb_end.append("}");

							mthd.insertBefore(sb.toString());
							mthd.insertAfter(sb_end.toString());
							flag = true;
						}
					}
				}
				if (flag)
					transformed = clazz.toBytecode();
			} catch (NotFoundException e) {
				System.err.println("[error!] no found exception in javassist! ");
			} catch (CannotCompileException e) {
				System.err.println("[error!] can not compile exception in javassist! " + className);
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("[error!] IO exception in javassist! ");
			}
		} else if (ConfigureUtil.isApplicationClass(className)) {
			String debug_mthd = "";
			int debug_i = -2;
//			cc2++;
//			System.out.println("**************************************"+cc2+"[class]"+className);
			try {
				CtClass clazz = pool.get(className.replaceAll("/", "."));

				/** exclude interface */
				if (Modifier.isInterface(clazz.getModifiers()))
					return null;

				pool.importPackage("java.util.logging.Logger");
				pool.importPackage("java.lang.reflect.Field");
				pool.importPackage("java.util.Set");
				pool.importPackage("java.util.Map");
				pool.importPackage("java.util.Collection");

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
						/**
						 * WARNING! for avoiding runtime null pointer dereference error in
						 * javassist,</br>
						 * donot use hashcode or format like '$_.getClass.getName()'
						 */
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
									if (m.getMethod() != null) {

										String callsite = m.getClassName() + "." + m.getMethodName() + m.getSignature() + "[" + m.getLineNumber() + "]";
										String belongTo = mthd.getLongName().substring(0, mthd.getLongName().indexOf('(')) + mthd.getSignature();

										String base = "\"\"";
										if (Modifier.isStatic(m.getMethod().getModifiers())) {
											base = "\"[base]static\"";
										} else {
//											base = "\"[base]\"+$0.hashCode()";
											base = "\"[base]\"+(($w)$0).getClass().getName()";
										}

										String insert1 = "System.out.println(\"[callsite]\" " + "+ \"[\" + Thread.currentThread().getId() + \"]\" " + "+" + "\""
												+ callsite + "\" " + "+ \"" + belongTo + "\"" + "+" + base + ");";
//										String insert1 = "mhn_logger.info(\"[callsite]\" "
//												+ "+ \"[\" + Thread.currentThread().getId() + \"]\" " + "+" + "\""
//												+ callsite + "\" " + "+ \"" + belongTo + "\"" + "+" + base + ");";

										/* return may be null */
										String ret2 = "\"\"";
										if (m.getMethod().getReturnType() != null) {
											String returnName = m.getMethod().getReturnType().getName();
											if (returnName != null && returnName.equals("void")) {
												ret2 = "\"[return]void\"";
											} else {
//												ret2 = "\"[return]\" + $_";
												ret2 = "\"[return]\" + mhn_obj.getClass().getName()";
											}
										} else {
											ret2 = "\"[return]null\"";
										}

										String insert21 = "System.out.println(\"[returnSite]\" " + "+ \"[\" + Thread.currentThread().getId() + \"]\" " + "+"
												+ "\"" + callsite + "\" " + "+ \"" + belongTo + "\"" + "+" + base + "+" + "\"[return]null\"" + ");";
										String insert22 = "System.out.println(\"[returnSite]\" " + "+ \"[\" + Thread.currentThread().getId() + \"]\" " + "+"
												+ "\"" + callsite + "\" " + "+ \"" + belongTo + "\"" + "+" + base + "+" + ret2 + ");";
//										String insert21 = "mhn_logger.info(\"[returnSite]\" "
//												+ "+ \"[\" + Thread.currentThread().getId() + \"]\" " + "+" + "\""
//												+ callsite + "\" " + "+ \"" + belongTo + "\"" + "+" + base + "+"
//												+ "\"[return]null\"" + ");";
//										String insert22 = "mhn_logger.info(\"[returnSite]\" "
//												+ "+ \"[\" + Thread.currentThread().getId() + \"]\" " + "+" + "\""
//												+ callsite + "\" " + "+ \"" + belongTo + "\"" + "+" + base + "+" + ret2
//												+ ");";

										/* openkm will dismiss this for method code limit */
										String insert2 = "Object mhn_obj = (($w)$_);" + "if(mhn_obj == null) {" + insert21 + "}else {" + insert22 + "}";
//										String replaceString = "{"  + " $_ = $proceed($$);"  + insert + "}";

										StringBuffer sb = new StringBuffer();
										sb.append("{");
//										sb.append("java.util.logging.Logger mhn_logger = java.util.logging.Logger.getLogger(Object.class.getName());");
										sb.append(insert1);
										sb.append(originalStmt);
										sb.append(insert2);/* openkm will dismiss this for method code limit */
										sb.append("}");

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
											if (field != null) {

												int line = f.getLineNumber();
												String threadID = "\"[\"+Thread.currentThread().getId()+\"]\"";
												String lineNum = "\"[" + line + "]\"";
												String fieldSig = "\"[signature]\" + \"" + field.toString() + "\"";

												String collections = "\"\"";
												String declareType = f.getSignature();
												if (declareType.equals("Ljava/util/Map;") || declareType.equals("Ljava/util/HashMap;")
														|| declareType.equals("Ljava/util/HashSet;") || declareType.equals("Ljava/util/Set;")) {
													collections = "\"[collection]\" + $_";
												}

												String base = "\"\"";
												if (Modifier.isStatic(field.getModifiers())) {
													base = "\"[base]static\"";
												} else {
//													/*if base is null, runtime will also report*/
//													base = "\"[base]\"+$0.hashCode()";
//													base = "\"[base]\"+$0";
													base = "\"[base]\"+(($w)$0).getClass().getName()";
												}

												String insert = "\"\"";
												String type = null;
												if (f.isReader()) {
													type = "\"[field read]\"";
													String insert1 = "System.out.println(" + type + "+" + threadID + "+" + lineNum + "+" + fieldSig
//															+ "+ \"[runtimeType]\" + $_.getClass().getName()"
															+ "+ \"[runtimeType]\" + mhn_obj.getClass().getName()" + "+" + collections
//															+ "+ \"[fieldObject]\" + obj.hashCode()"
//															+ "+ \"[fieldObject]\" + obj"
//															+ "+ \"[fieldObject]\" + $_.hashCode()"
////															+ "+ \"[fieldObject]\" + $_"
															+ "+" + base + ");";
													String insert2 = "System.out.println(" + type + "+" + threadID + "+" + lineNum + "+" + fieldSig
															+ "+ \"[fieldObject]null\"" + "+" + base + ");";
//													String insert1 = "mhn_logger.info(" + type + "+" + threadID + "+"
//															+ lineNum + "+" + fieldSig
////															+ "+ \"[runtimeType]\" + $_.getClass().getName()"
//															+ "+ \"[runtimeType]\" + obj.getClass().getName()" + "+"
//															+ collections
////															+ "+ \"[fieldObject]\" + obj.hashCode()"
////															+ "+ \"[fieldObject]\" + obj"
////															+ "+ \"[fieldObject]\" + $_.hashCode()"
//////															+ "+ \"[fieldObject]\" + $_"
//															+ "+" + base + ");";
//													String insert2 = "mhn_logger.info(" + type + "+" + threadID + "+"
//															+ lineNum + "+" + fieldSig + "+ \"[fieldObject]null\"" + "+"
//															+ base + ");";

													insert = "Object mhn_obj = (($w)$_);" + "if(mhn_obj == null) {" + insert2 + "}else {" + insert1 + "}";
//													originalStmt = "$_ = $proceed();";
												} else if (f.isWriter()) {
													type = "\"[field write]\"";
													String insert1 = "System.out.println(" + type + "+" + threadID + "+" + lineNum + "+" + fieldSig
//															+ "+ \"[runtimeType]\" + $1.getClass().getName()"
															+ "+ \"[runtimeType]\" + mhn_obj.getClass().getName()"
//															+ "+ \"[fieldObject]\" + obj"
//															+ "+ \"[fieldObject]\" + $1.hashCode()"
//															+ "+ \"[fieldObject]\" + obj.hashCode()"
////															+ "+ \"[fieldObject]\" + $1"
															+ "+" + base + ");";
													String insert2 = "System.out.println(" + type + "+" + threadID + "+" + lineNum + "+" + fieldSig
															+ "+ \"[fieldObject]null\"" + "+" + base + ");";
//													String insert1 = "mhn_logger.info(" + type + "+" + threadID + "+"
//															+ lineNum + "+" + fieldSig
////															+ "+ \"[runtimeType]\" + $1.getClass().getName()"
//															+ "+ \"[runtimeType]\" + obj.getClass().getName()"
////															+ "+ \"[fieldObject]\" + obj"
////															+ "+ \"[fieldObject]\" + $1.hashCode()"
////															+ "+ \"[fieldObject]\" + obj.hashCode()"
//////															+ "+ \"[fieldObject]\" + $1"
//															+ "+" + base + ");";
//													String insert2 = "mhn_logger.info(" + type + "+" + threadID + "+"
//															+ lineNum + "+" + fieldSig + "+ \"[fieldObject]null\"" + "+"
//															+ base + ");";

													insert = "Object mhn_obj = (($w)$1);" + "if(mhn_obj == null) {" + insert2 + "}else {" + insert1 + "}";
//													originalStmt = "$proceed($$);";
												}

												StringBuffer sb = new StringBuffer();
												sb.append("{");
//												sb.append("java.util.logging.Logger mhn_logger = java.util.logging.Logger.getLogger(Object.class.getName());");
												sb.append(originalStmt);
												sb.append(insert);/* openkm will dismiss this for method code limit */
												sb.append("}");

												/* openkm will dismiss this for method code limit */
												f.replace(sb.toString());
											}
										} catch (NotFoundException e) {
											System.err.println("NotFoundException in field access!");
										}
									}
								}
							}

						});

						/**
						 * insert at the very beginning of each method ,</br>
						 * for knowing each invoke when running
						 */
						StringBuffer sb = new StringBuffer();
						sb.append("{");

//						sb.append("java.util.logging.Logger mhn_logger = java.util.logging.Logger.getLogger(Object.class.getName());");
						/**
						 * 1. print method long name
						 */
						String s1 = "System.out.println(\"[call method]\" + \"[\" + Thread.currentThread().getId() + \"]\" +";
//						String s1 = "mhn_logger.info(\"[call method]\" + \"[\" + Thread.currentThread().getId() + \"]\" +";
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
//							String insert1 = "System.out.println(\"[base class]\"+$0.getClass().getName() );";
							String insert1 = "System.out.println(\"[base class][\"+Thread.currentThread().getId()+\"]\"+$0.getClass().getName());";
//							String insert1 = "mhn_logger.info(\"[base class][\"+Thread.currentThread().getId()+\"]\"+$0.getClass().getName());";

							sb.append(insert1);

							String insert3 = "Field[] fields = $0.getClass().getDeclaredFields();" + "for (int i = 0; i < fields.length; i++) {"
									+ "fields[i].setAccessible(true);" + "if(fields[i].get($0)!=null){"
									+ "String actualClass=fields[i].get($0).getClass().getName();"
//									+ "System.out.println(\"[base field]\"  + fields[i].getName() + \":\"+fields[i].getGenericType().getTypeName()+\":\" +actualClass);"
									+ "System.out.println(\"[base field][\"+Thread.currentThread().getId()+\"]\"+fields[i].getName()+\":\"+fields[i].getGenericType().getTypeName()+\":\" +actualClass);"
//									+ "mhn_logger.info(\"[base field][\"+Thread.currentThread().getId()+\"]\"+fields[i].getName()+\":\"+fields[i].getGenericType().getTypeName()+\":\" +actualClass);"
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

									+ "}else{"
//									+ "System.out.println(\"[base field]\"  + fields[i].getName()+ \":\"+fields[i].getGenericType().getTypeName() + \":null\");"
									+ "System.out.println(\"[base field][\"+Thread.currentThread().getId()+\"]\"+ fields[i].getName()+\":\"+fields[i].getGenericType().getTypeName()+\":null\");"
//									+ "mhn_logger.info(\"[base field][\"+Thread.currentThread().getId()+\"]\"+ fields[i].getName()+\":\"+fields[i].getGenericType().getTypeName()+\":null\");"
									+ "}" + "}";
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
//						sb2.append("java.util.logging.Logger mhn_logger = java.util.logging.Logger.getLogger(Object.class.getName());");
						String s2 = "System.out.println(\"[call method finished]\" + \"[\" + Thread.currentThread().getId() + \"]\" +";
//						String s2 = "mhn_logger.info(\"[call method finished]\" + \"[\" + Thread.currentThread().getId() + \"]\" +";
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
	private boolean isTomcatHttpRequest(String className) {
		// old: servlet
//		if (className.equals("javax/servlet/http/HttpServlet")) {
//			return true;
//		}
		// catch from tomcat,in this level,some message have not parsed
//		if (className.equals("org/apache/catalina/connector/CoyoteAdapter")) {
//			return true;
//		}
		// catch from tomcat
		if (className.equals("org/apache/catalina/core/ApplicationFilterChain")) {
			return true;
		}
		return false;
	}

	private boolean isUndertowRequest(String className) {
		// catch from undertow
		if (className.equals("io/undertow/servlet/core/ManagedFilter")) {
			return true;
		}
		return false;
	}
}
