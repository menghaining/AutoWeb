package ict.pag.m.frameworkInfoUtil.customize;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.ibm.wala.classLoader.ClassURLModule;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.io.FileProvider;

public class ConfigUtil {
	public static boolean printLogs = false;
	public static boolean onlyClass = true;
	public static boolean containsLibInfo = true;

	/** application is depends on classloaderreference type */
	public static boolean enableApplication = true;

	private String analyseDir = "";
	private String logFile = "";

	/** for add extra directory class */
//	public static String extraDir = "F:\\Framework\\Tesecases\\WebGoat\\runnable\\webgoat-container-7.1\\plugin_lessons";
//	public static String extraDir = "F:\\Framework\\Tesecases\\opencms\\classes";
	public String extraDir = "";
	public String configFiles = "";
	public static String appKind = "";

	private String libsDir = "";
	private static ClassHierarchy cha = null;

	private static ConfigUtil instance = new ConfigUtil();

	public static ConfigUtil g() {
		if (instance == null)
			instance = new ConfigUtil();
		return instance;
	}

	public void loadConfiguration(CLIOption cliOptions) {
		extraDir = cliOptions.getExtraDir();
		analyseDir = cliOptions.getAnalyseDir();
		logFile = cliOptions.getLogFile();
		configFiles = cliOptions.getXmlPath();
		appKind = cliOptions.getAppKind();
		libsDir = cliOptions.getLibsDir();
		initLibsCHA();
	}

	/** the calls that trigger all of the inheritance calls */
	private HashSet<String> keyCalls = new HashSet<>();

	/** @return all candidate framework calls */
	public HashSet<String> getAllFrameworkParentsCalls(String ele) {

		HashSet<String> ret = new HashSet<>();

		for (HashSet<String> set : sameFunctionInheritanceCollection) {
			for (String s : set) {

				if (s.equals(ele) || s.startsWith(ele.substring(0, ele.indexOf('(')))) {
//					if (s.contains("AbstractController") && s.contains("handleRequestInternal"))
//						System.out.println();
					ret.addAll(set);
//					return ret;
				}
			}
		}
		if (!ret.isEmpty())
			keyCalls.add(ele);
		return ret;
	}

	private HashSet<IMethod> visited = new HashSet<>();
	private HashSet<HashSet<String>> sameFunctionInheritanceCollection = new HashSet<>();

	private HashSet<IClass> visited_classes = new HashSet<>();
	private HashSet<HashSet<String>> inheritanceClassesGroupSet = new HashSet<>();

	private void initLibsCHA() {
		HashSet<String> libs = new HashSet<>();

		String resources = System.getProperty("user.dir");

		if (!libsDir.equals("")) {
			// 1. load application libs
			File file = new File(libsDir);
			File[] files = file.listFiles();
			for (File subFile : files) {
				libs.add(subFile.getAbsolutePath());
			}
		}

		// 2. load java servlet libs
		String servletLibsDir = resources + File.separator + "libs";
		File s_file = new File(servletLibsDir);
		File[] s_files = s_file.listFiles();
		for (File subFile : s_files) {
			libs.add(subFile.getAbsolutePath());
		}

		String exclusions = resources + File.separator + "Java60RegressionExclusions.txt";
		String initScopeFile = resources + File.separator + "initScopeFile.txt";

		try {
			AnalysisScope scope = AnalysisScopeReader.readJavaScope(initScopeFile, null,
					GraphBuilder.class.getClassLoader());
			for (String f : libs) {
				loadFiles(scope, f);
			}

			File exclusionsFile = exclusions != null ? new File(exclusions) : null;
			if (exclusionsFile != null) {
				try (final InputStream fs = exclusionsFile.exists() ? new FileInputStream(exclusionsFile)
						: FileProvider.class.getClassLoader().getResourceAsStream(exclusionsFile.getName())) {
					scope.setExclusions(new FileOfClasses(fs));
				}
			}

			cha = ClassHierarchyFactory.makeWithPhantom(scope);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
//		catch (InvalidClassFileException e) {
//			e.printStackTrace();
//		}

		if (cha != null) {
			/* calculate the collection of methods grouped by overritten */
			cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(clazz -> {
				clazz.getDeclaredMethods().forEach(method -> {
//						if (method.getSignature().contains("javax.servlet.")
//								&& !method.getSignature().contains("javax.servlet.jsp"))
//							System.out.println();
					if (!visited.contains(method)) {
						HashSet<String> collection = new HashSet<>();
						calculateAllMethodCollection(method, collection);
						if (!method.getName().toString().equals("<init>"))
							collection.add(method.getSignature());
						if (!collection.isEmpty())
							sameFunctionInheritanceCollection.add(collection);

//							if (collection.size() > 2)
//								System.out.println();
					}
				});

//					String c = Util.format(clazz.getName().toString());

			});

		}

	}

	/**
	 * find all subclasses and implementors of the given String</br>
	 * 
	 * @param given like "Ljava/lang/Object"
	 */
	public HashSet<String> findAllSubtypesAndImplementors(String given) {
		HashSet<String> ret = new HashSet<>();
		if (cha == null)
			return ret;
		if (given.endsWith(";"))
			given = given.substring(0, given.length() - 1);

		IClass givenClass = cha.lookupClass(
				TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(given)));
		if (givenClass == null)
			return ret;

		ret.add(Util.format(given));
		if (givenClass.isInterface()) {
			// immediate super class
			Collection<IClass> superClassSet = givenClass.getAllImplementedInterfaces();
			for (IClass superClass : superClassSet) {
				if (superClass != null
						&& superClass.getClassLoader().getReference().equals(ClassLoaderReference.Application))
					ret.add(Util.format(superClass.getName().toString()));
			}

			cha.getImplementors(givenClass.getReference()).forEach(impl -> {
				// all subclasses
				HashSet<IClass> allSubClasses = new HashSet<>();
				findAllLayerSubClasses(impl, allSubClasses);
				for (IClass i : allSubClasses) {
					ret.add(Util.format(i.getName().toString()));
				}
			});
		} else {
			// immediate super class
			IClass superClass = givenClass.getSuperclass();
			if (superClass != null
					&& superClass.getClassLoader().getReference().equals(ClassLoaderReference.Application))
				ret.add(Util.format(superClass.getName().toString()));
			// all subclasses
			HashSet<IClass> allSubClasses = new HashSet<>();
			findAllLayerSubClasses(givenClass, allSubClasses);
			for (IClass i : allSubClasses) {
				ret.add(Util.format(i.getName().toString()));
			}
		}

		if (!ret.isEmpty())
			keyCalls.add(given);
		return ret;

	}

	/**
	 * calculate a collection that contains the same function of all inheritance
	 * classes
	 */
	private void calculateAllMethodCollection(IMethod method, HashSet<String> collection) {
		if (method.isPrivate())
			return;
		if (visited.contains(method))
			return;
		visited.add(method);

		String funcName = method.getName().toString();
		if (funcName.equals("<init>"))
			return;

		IClass c = method.getDeclaringClass();

		if (c.isInterface()) {
			cha.getImplementors(c.getReference()).forEach(impl -> {
				impl.getDeclaredMethods().forEach(m2 -> {
					if (isOverrideFrom(m2, method)) {
						collection.add(m2.getSignature());
						calculateAllMethodCollection(m2, collection);
						visited.add(m2);
					}
				});

			});

			// upper
			HashSet<IClass> allsuperClasses = new HashSet<>();
			findAllLayerInterfaces(c, allsuperClasses);
			for (IClass sc : allsuperClasses) {
				sc.getDeclaredMethods().forEach(m2 -> {
					if (isOverrideFrom(m2, method)) {
						collection.add(m2.getSignature());
						visited.add(m2);
					}
				});
			}

		} else {
			HashSet<IClass> allSubClasses = new HashSet<>();
			findAllLayerSubClasses(c, allSubClasses);
			for (IClass subc : allSubClasses) {
				// add the methods override
				subc.getDeclaredMethods().forEach(m2 -> {
					if (isOverrideFrom(m2, method)) {
						collection.add(m2.getSignature());
						visited.add(m2);
					}
				});
				// add the methods extends from parents
				String funcSig = Util.format(subc.getName().toString())
						+ method.getSignature().substring(method.getSignature().lastIndexOf('.'));
				collection.add(funcSig);
			}

//			if (c.toString().contains("servlet") || c.toString().contains("spring"))
//				System.out.println();

			// upper
			HashSet<IClass> allsuperClasses = new HashSet<>();
			findAllLayerInterfaces(c, allsuperClasses);
			findAllLayerSuperClasses(c, allsuperClasses);
			for (IClass sc : allsuperClasses) {
				sc.getDeclaredMethods().forEach(m2 -> {
					if (isOverrideFrom(m2, method)) {
						collection.add(m2.getSignature());
						visited.add(m2);
					}
				});
			}
		}

	}

	private void findAllLayerSuperClasses(IClass c, HashSet<IClass> allsuperClasses) {
		IClass sc = c.getSuperclass();
		if (sc == null || !sc.getClassLoader().getReference().equals(ClassLoaderReference.Application)
				|| sc.toString().contains("Ljava/lang/Object"))
			return;

		if (!allsuperClasses.contains(sc)) {
			allsuperClasses.add(sc);
			findAllLayerSuperClasses(sc, allsuperClasses);
			findAllLayerInterfaces(sc, allsuperClasses);
		}

	}

	private void findAllLayerInterfaces(IClass c, HashSet<IClass> allsuperClasses) {
		for (IClass sc : c.getAllImplementedInterfaces()) {
			if (!sc.getClassLoader().getReference().equals(ClassLoaderReference.Application))
				continue;
			if (!allsuperClasses.contains(sc)) {
				allsuperClasses.add(sc);
				findAllLayerInterfaces(sc, allsuperClasses);
			}
		}

	}

	/** child is already validated as parent's child */
	private boolean isOverrideFrom(IMethod child, IMethod parent) {
		String funcName1 = parent.getReference().getName().toString();
		String funcName2 = child.getReference().getName().toString();
		if (funcName1.equals(funcName2) && (parent.getNumberOfParameters() == child.getNumberOfParameters())) {
			boolean same = true;
			int i;
			if ((parent.isStatic() && child.isStatic())) {
				i = 0;
			} else if (!parent.isStatic() && !child.isStatic()) {
				i = 1;
			} else {
				return false;
			}

			for (; i < parent.getNumberOfParameters(); i++) {
				if (!hasInheritanceRelation(parent.getParameterType(i), child.getParameterType(i))) {
					same = false;
					break;
				}
			}
			if (!hasInheritanceRelation(parent.getReturnType(), child.getReturnType())) {
				same = false;
			}
			if (same) {
				return true;

			}
		}
		return false;
	}

	private void findAllLayerSubClasses(IClass impl, HashSet<IClass> allSubClasses) {
		for (IClass sub : cha.getImmediateSubclasses(impl)) {
			allSubClasses.add(sub);
			findAllLayerSubClasses(sub, allSubClasses);
		}

	}

	private boolean hasInheritanceRelation(TypeReference tp1, TypeReference tp2) {
		if (tp1.equals(tp2)) {
			return true;
		} else if (tp1.isReferenceType() && tp2.isReferenceType()) {
			IClass c1 = cha.lookupClass(tp1);
			IClass c2 = cha.lookupClass(tp2);
			if (c1 != null && c2 != null) {
				if (cha.isSubclassOf(c1, c2) || cha.isSubclassOf(c2, c1)) {
					return true;
				}
			}
		}
		return false;
	}

	/** just load lib jars */
	private void loadFiles(AnalysisScope scope, String filePath) throws IOException, IllegalArgumentException {
		File file = new File(filePath);
		if (file.exists()) {
			switch (FileHelper.getTypeBySuffix(filePath)) {
			case JAR:
			case WAR:
				/**
				 * only analysis class file in jar/war
				 */
				JarFile jarFile = null;
				jarFile = new JarFile(new File(filePath));

				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					String entryName = entry.getName();
					if (!entry.isDirectory() && entryName.endsWith(".class")) {
						URL url = new URL("jar:file:" + filePath + "!/" + entry.toString());
						addIn2Scope(scope, url);

					}
				}
				jarFile.close();
				break;

			default:
				break;
			}

		} else {
			System.err.println(filePath + " not exist!");
			System.exit(0);
		}

	}

	private void addIn2Scope(AnalysisScope scope, URL url) {
		try {
			Module clazzFile = new ClassURLModule(url);
			scope.addToScope(ClassLoaderReference.Application, clazzFile);
		} catch (InvalidClassFileException e) {
			System.err.println("[error]" + url);
		}

	}

	public HashSet<String> getExtraLoadFiles() {
		HashSet<String> ret = new HashSet<>();

		// user configured
		if (extraDir.equals("")) {
		} else {
			File file = new File(extraDir);
			File[] files = file.listFiles();
			for (File subFile : files) {
				System.out.println("[extra File]" + subFile.getAbsolutePath());
				ret.add(subFile.getAbsolutePath());
			}
		}

		return ret;
	}

	/** need to configure application class */
	public static boolean isApplicationClass(String name) {

		if (name.startsWith("L") && name.contains("/"))
			name = name.substring(1).replaceAll("/", ".");

		if (appKind.equals("")) {
			return appClassConfiguredInFile(name);
		} else {
			return appClassConfiguredInCMD(name);
		}

	}

	private static boolean appClassConfiguredInCMD(String name) {
		switch (appKind) {

		/** input apps */
		case "demo1":
			if (name.startsWith("com.example."))
				return true;
			break;
		case "jpetstore":
			if (name.startsWith("org.mybatis.jpetstore."))
				return true;
			break;
		case "ruoyi":
			if (name.startsWith("com.ruoyi."))
				return true;
			break;
		case "imooc":
			/** weChat restaurant */
			if (name.startsWith("com.imooc."))
				return true;
			break;
		case "openkm":
			if (name.startsWith("com.openkm."))
				return true;
			break;
		case "struts2-examples":
			/** apache-struts2-examples */
			if (name.startsWith("example.actions.") || name.startsWith("org.apache.struts.edit.")
					|| name.startsWith("org.apache.struts.helloworld.")
					|| name.startsWith("org.apache.struts.register.") || name.startsWith("org.apache.struts.example.")
					|| name.startsWith("org.apache.struts.examples.") || name.startsWith("org.apache.strutsexamples.")
					|| name.startsWith("org.apache.struts.using_tags.")
					|| name.startsWith("org.apache.struts.tutorials.wildcardmethod."))
				return true;
			break;
		case "lms":
			/** LogisticsManageSystem */
			if (name.startsWith("com.wt."))
				return true;
			break;
		case "halo":
			/** LogisticsManageSystem */
			if (name.startsWith("run.halo.app."))
				return true;
			break;
		case "icloud":
			/** for iCloud */
			if (name.startsWith("cn.zju."))
				return true;
			break;
		case "newssystem":
			/** NewsSystem */
			if (name.startsWith("magicgis.newssystem"))
				return true;
			break;
		case "community":
			/** community */
			if (name.startsWith("life.majiang.community."))
				return true;
			break;
		case "newbee":
			/** newbee */
			if (name.startsWith("ltd.newbee.mall."))
				return true;
			break;
		case "logicaldoc":
			/** logicaldoc */
			if (name.startsWith("com.logicaldoc."))
				return true;
			break;

		/** below are others */
		case "opencms":
			/** for opencms */
			if (name.startsWith("org.opencms."))
				return true;
			break;
		case "webgoat":
			/** for WebGoat */
			if (name.startsWith("org.owasp.webgoat."))
				return true;
			break;
		case "shopizer":
			/** for Shopizer */
			if (name.startsWith("com.salesmanager."))
				return true;
			break;
		case "pybbs":
			/** for pybbs */
			if (name.startsWith("co.yiiu.pybbs"))
				return true;
			break;
		case "springblog":
			/** for SpringBlog */
			if (name.startsWith("com.raysmond."))
				return true;
			break;

		case "hello-web":
//			/** hello-web */
			// actframework
			if (name.startsWith("acme.hello."))
				return true;
			break;
		}
		return false;
	}

	private static boolean appClassConfiguredInFile(String name) {
//		if (name.startsWith("testInit"))
//		return true;
		/** demo1 */
//		if (name.startsWith("com.example."))
//			return true;
//
		/** for opencms */
//	if (name.startsWith("org.opencms."))
//		return true;
		/** for WebGoat */
//	if (name.startsWith("org.owasp.webgoat."))
//		return true;
		/** for Shopizer */
//	if (name.startsWith("com.salesmanager."))
//		return true;
		/** for pybbs */
//	if (name.startsWith("co.yiiu.pybbs"))
//		return true;
		/** for SpringBlog */
//	if (name.startsWith("com.raysmond."))
//		return true;

		/** for iCloud */
//	if (name.startsWith("cn.zju."))
//		return true;

		/** apache-struts2-examples */
//	// package-annotations
//	if (name.startsWith("example.actions."))
//		return true;
//	// package-bean-validation, package-control-tags
//	if (name.startsWith("org.apache.struts.edit."))
//		return true;
//	// package-coding-actions
//	if (name.startsWith("org.apache.struts.helloworld."))
//		return true;
//
//	/** hello-web */
//	// actframework
//	if (name.startsWith("acme.hello."))
//		return true;

		return false;
	}

	/** user-configured concerned framework class */
	public static boolean isFrameworkMarks(String name) {

		if (name.startsWith("L") && name.contains("/"))
			name = name.substring(1).replaceAll("/", ".");

		/** common */
		if (name.startsWith("javax.annotation."))
			return true;
		if (name.startsWith("javax.persistence."))
			return true;
		if (name.startsWith("javax.ejb."))
			return true;
		// all the WebSocket APIs common to both the client and server side.
		if (name.startsWith("javax.websocket."))
			return true;
		// High-level interfaces and annotations used to create RESTful service
		// resources.
		if (name.startsWith("javax.ws.rs."))
			return true;
		if (name.startsWith("javax.ejb."))
			return true;
		if (name.startsWith("javax.jws."))
			return true;
		if (name.startsWith("javax.persistence."))
			return true;

		/** for java servlet */
		if (name.startsWith("javax.servlet."))
			return true;

		/** for spring framework */
		if (name.startsWith("org.springframework."))
			return true;

		/** for gwt framework */
		if (name.startsWith("com.google.gwt."))
			return true;

		/** for vaadin framework */
		if (name.startsWith("com.vaadin."))
			return true;

		/** for struts */
		if (name.startsWith("org.apache.struts2."))
			return true;
		if (name.startsWith("com.opensymphony.xwork2"))
			return true;

		/** aspectJ */
		if (name.startsWith("org.aspectj."))
			return true;

		/** for act framework */
		if (name.startsWith("act.") || name.startsWith("org.osgl."))
			return true;

		/** for dropwizard */
		if (name.startsWith("io.dropwizard."))
			return true;

		/** for stripes frameworks */
		if (name.startsWith("net.sourceforge.stripes."))
			return true;

		/** shiro */
		if (name.startsWith("org.apache.shiro."))
			return true;

		return false;
//		return true;
	}

	public String getAnalyseDir() {
		return analyseDir;
	}

	public String getLogFile() {
		return logFile;
	}

	public String getExtraDir() {
		return extraDir;
	}

	public String getConfigFiles() {
		if (configFiles.equals(""))
			return analyseDir;
		return configFiles;
	}

	public static boolean isStoreLayer(String namespaceURI) {
		if (namespaceURI.contains("mybatis"))
			return true;

		return false;
	}

	public static boolean isOtherConfig(String namespaceURI) {
		if (namespaceURI.contains("maven-assembly-plugin"))
			return true;

		return false;
	}

	public static ClassHierarchy getLibCha() {
		return cha;
	}

	public HashSet<String> getKeyCalls() {
		return keyCalls;
	}

//	public static boolean isRequestInvokeOverride(String signature) {
//		if (signature.equals("getParameter(Ljava/lang/String;)Ljava/lang/String;"))
//			return true;
//		if (signature.equals("getParameterValues(Ljava/lang/String;)[Ljava/lang/String;"))
//			return true;
//
//		return false;
//	}

}
