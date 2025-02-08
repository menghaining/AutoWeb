package ict.pag.m.instrumentation.Main;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashSet;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

public class OnlyPrintURLs implements ClassFileTransformer {
	private String agentArgs;

	private HashSet<String> visitedClass = new HashSet<>();

	public OnlyPrintURLs(String agentArgs) {
		this.agentArgs = agentArgs;

	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		byte[] transformed = null;
		ClassPool pool = ClassPool.getDefault();
		pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
		pool.appendClassPath(new LoaderClassPath(loader));

		if (visitedClass.contains(className))
			return transformed;
		visitedClass.add(className);

		if (isHttpRequest(className)) {
			try {
				CtClass clazz = pool.get(className.replaceAll("/", "."));
				boolean flag = false;

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
							sb.append("if(request instanceof javax.servlet.http.HttpServletRequest){");
							sb.append("javax.servlet.http.HttpServletRequest mhn_req=(javax.servlet.http.HttpServletRequest)request;");
							//  tmp for logicaldoc
//							sb.append("if($1 instanceof javax.servlet.http.HttpServletRequest){");
//							sb.append("javax.servlet.http.HttpServletRequest mhn_req=(javax.servlet.http.HttpServletRequest)$1;");

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

							sb.append(
									"System.out.println(\"[ReqStart][\"+Thread.currentThread().getId()+\"]\"+mhn_URL+\"[hashcode]\"+request.hashCode()+\"[method]\"+mhn_mtd+\"[queryString]\"+mhn_querystr+\"[param]\"+mhn_params_str+\"[pathInfo]\"+mhn_pathinfo+\"[user]\"+mhn_rmuser+\"[sessionId]\"+mhn_sessionid+\"[cookie]\"+mhn_cookies_str);");
							//  for logicaldoc
//							sb.append(
//									"System.out.println(\"[ReqStart][\"+Thread.currentThread().getId()+\"]\"+mhn_URL+\"[hashcode]\"+$1.hashCode()+\"[method]\"+mhn_mtd+\"[queryString]\"+mhn_querystr+\"[param]\"+mhn_params_str+\"[pathInfo]\"+mhn_pathinfo+\"[user]\"+mhn_rmuser+\"[sessionId]\"+mhn_sessionid+\"[cookie]\"+mhn_cookies_str);");

							sb.append("}");

							/* at end */

							/*
							 * [ReqEnd][threadID]url[hashcode]xxx[headers-cookie]set-cookie[status-code]
							 * response_status
							 */
							sb_end.append(
									"if((response instanceof javax.servlet.http.HttpServletResponse)&&(request instanceof javax.servlet.http.HttpServletRequest)){");
							sb_end.append(
									"javax.servlet.http.HttpServletResponse mhn_res=(javax.servlet.http.HttpServletResponse)response;javax.servlet.http.HttpServletRequest mhn_req=(javax.servlet.http.HttpServletRequest)request;");

							// for logicaldoc
//							sb_end.append(
//									"if(($args[1] instanceof javax.servlet.http.HttpServletResponse)&&($args[0] instanceof javax.servlet.http.HttpServletRequest)){");
//							sb_end.append(
//									"javax.servlet.http.HttpServletResponse mhn_res=(javax.servlet.http.HttpServletResponse)($args[1]);javax.servlet.http.HttpServletRequest mhn_req=(javax.servlet.http.HttpServletRequest)($args[0]);");

							sb_end.append(
									"String mhn_URL=mhn_req.getRequestURL().toString();String mhn_headers_cookie=mhn_res.getHeaders(\"Set-Cookie\").toString();int mhn_status_code=mhn_res.getStatus();");
							sb_end.append(
									"System.out.println(\"[ReqEnd][\"+Thread.currentThread().getId()+\"]\"+mhn_URL+\"[hashcode]\"+request.hashCode()+\"[headers-cookie]\"+mhn_headers_cookie+\"[status-code]\"+mhn_status_code);");

							// tmp for logicaldoc
//							sb_end.append(
//									"System.out.println(\"[ReqEnd][\"+Thread.currentThread().getId()+\"]\"+mhn_URL+\"[hashcode]\"+($args[0]).hashCode()+\"[headers-cookie]\"+mhn_headers_cookie+\"[status-code]\"+mhn_status_code);");

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
				e.printStackTrace();
			} catch (CannotCompileException e) {
				System.err.println("[error!] can not compile exception in javassist! ");
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("[error!] IO exception in javassist! ");
				e.printStackTrace();
			}
		}
		return transformed;
	}

	private boolean isHttpRequest(String className) {
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
}
