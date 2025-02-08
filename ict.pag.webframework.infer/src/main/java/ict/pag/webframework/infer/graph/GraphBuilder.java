package ict.pag.webframework.infer.graph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.ibm.wala.classLoader.ClassURLModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.io.FileProvider;

import ict.pag.webframework.infer.helper.FileHelper;
import ict.pag.webframework.infer.helper.SpecialHelper;

public class GraphBuilder {
	/** configure option */
	private static boolean onlyClass = true;
	private boolean withLibs = false;

	private String file_path = "";
//	private String applicationlibsDir = "";

	private HashSet<File> dependencylibDirs = new HashSet<>(); /* find in web app dir */

	static String resources = System.getProperty("user.dir") + File.separator + "data";
	static String exclusions = resources + File.separator + "Java60RegressionExclusions.txt";
	static String initScopeFile = resources + File.separator + "initScopeFile.txt";
	static String servletLibsDir = resources + File.separator + "libs";

	private ClassHierarchy cha = null;
	private AnalysisScope scope = null;
	private CHACallGraph appCHACG = null;

	/** all application classes */
	HashSet<String> applicationClasses = new HashSet<>();

	/**
	 * @param withLib true iff construct cha using both libs and application, and
	 *                libs with load in Primordial
	 */
	public GraphBuilder(String filePath, boolean withLib) {
		this.file_path = filePath;
		this.withLibs = withLib;
//		if (applicationlibPath != null)
//			this.applicationlibsDir = applicationlibPath;
//		if (externalLibPath != null)
//			this.dependencylibDir = externalLibPath;
		initGraphBuilder();
	}

	private void initGraphBuilder() {
		System.out.println("exclusions file location: " + exclusions);
		System.out.println("initScopeFile file location: " + initScopeFile);
		System.out.println("resources file location: " + resources);

		if (file_path.equals("")) {
			System.err.println("[ERROR]Input Path Is Empty!");
			System.exit(0);
		}
		try {
			long beforeTime = System.nanoTime();
			init();
			double buildTime = (System.nanoTime() - beforeTime) / 1E9;
			System.out.println("[TIME-LOG] GraphBuilder Done in " + buildTime + " s!");
		} catch (ClassHierarchyException e) {
			System.err.println("cha generate exception!");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("file operation exception!");
			e.printStackTrace();
		}
	}

	/**
	 * @throws IOException             at files operations
	 * @throws ClassHierarchyException at ClassHierarchyFactory.makeWithPhantom()
	 */
	private void init() throws IOException, ClassHierarchyException {
		scope = AnalysisScopeReader.readJavaScope(initScopeFile, null, GraphBuilder.class.getClassLoader());

		try {

			// load applications
			loadApplicationFiles(scope, file_path);
			// load libs
			if (withLibs) {
				HashSet<String> libs = new HashSet<>();

				// 1. load dependency libs
				for (File file : dependencylibDirs) {
					File[] files = file.listFiles();
					for (File subFile : files) {
						if (subFile.isFile() && subFile.getAbsolutePath().endsWith(".jar"))
							libs.add(subFile.getAbsolutePath());
					}
				}

				// 2. load common java servlet libs
				File s_file = new File(servletLibsDir);
				if (s_file.exists()) {
					File[] s_files = s_file.listFiles();
					for (File subFile : s_files) {
						libs.add(subFile.getAbsolutePath());
					}
				}

				// load dependency external Libs
				for (String f : libs) {
					loadExternalLibsJars(scope, f);
				}
			}

		} catch (IllegalArgumentException | InvalidClassFileException e1) {
			e1.printStackTrace();
		}

		File exclusionsFile = exclusions != null ? new File(exclusions) : null;
		if (exclusionsFile != null) {
			try (final InputStream fs = exclusionsFile.exists() ? new FileInputStream(exclusionsFile)
					: FileProvider.class.getClassLoader().getResourceAsStream(exclusionsFile.getName())) {
				scope.setExclusions(new FileOfClasses(fs));
			}
		}

		cha = ClassHierarchyFactory.makeWithPhantom(scope);

		/* Construct cha graph only includes application codes */
		this.appCHACG = new CHACallGraph(cha, true);
		try {
			this.appCHACG.init();
		} catch (CancelException e) {
			e.printStackTrace();
		}

		cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(clazz -> {
//			applicationClasses.add(SpecialHelper.formatSignature(clazz.getName().toString()));

//			appClassCount++;
//
//			if (clazz.isInterface())
//				appClassCount_interface++;
//			if (clazz.isAbstract())
//				appClassCount_abstract++;
//			if (clazz.isPrivate())
//				appClassCount_private++;

			clazz.getDeclaredMethods().forEach(method -> {
				try {
					appCHACG.findOrCreateNode(method, Everywhere.EVERYWHERE);

					// record method messages
//					appMethodCount++;
//					allMtdSigs.add(method.getSignature());
//					allMtd.add(method);
//					if (method.isAbstract())
//						appMethodCount_abstract++;
//					if (method.isNative())
//						appMethodCount_native++;
//					if (method.isInit() || method.isClinit())
//						appMethodCount_constructor++;
//					if (method.isPrivate())
//						appMethodCount_private++;
				} catch (CancelException e) {
					e.printStackTrace();
				}

			});
		});
//
//		// print app classes and methods properties
//		System.out.println("[info][Application Classes]\n\t[Total] " + appClassCount + "\n\t[interface] " + appClassCount_interface + "\n\t[abstract] "
//				+ appClassCount_abstract + "\n\t[private] " + appClassCount_private);
//		System.out.println("[info][Application Methods]\n\t[Total] " + appMethodCount + "\n\t[abstract] " + appMethodCount_abstract + "\n\t[native] "
//				+ appMethodCount_native + "\n\t[constructor] " + appMethodCount_constructor + "\n\t[private] " + appMethodCount_private);

	}

//	public int getConcreteApplicationMethodNumber() {
//		return appMethodCount - appMethodCount_abstract - appMethodCount_native;
//	}

	/**
	 * only load class file in directory/wars/Jars
	 */
	private static boolean isDirectory = false;

	private void loadApplicationFiles(AnalysisScope scope, String filePath) throws IOException, IllegalArgumentException, InvalidClassFileException {
		File file = new File(filePath);
		if (file.exists()) {
			if (file.isDirectory()) {
				String name = file.getName();
				if (name.equals("lib")) {
					dependencylibDirs.add(file);
				} else {
					isDirectory = true;
					File[] files = file.listFiles();
					for (File subFile : files) {
						loadApplicationFiles(scope, subFile.getAbsolutePath());
					}
				}
			} else {
				switch (FileHelper.getTypeBySuffix(filePath)) {
				case JAR:
				case WAR:
					if (!onlyClass) {
						/**
						 * if not -OC, just add jar/war files have jar/war in it
						 */
						Module M = (new FileProvider()).getJarFileModule(filePath, GraphBuilder.class.getClassLoader());
						scope.addToScope(ClassLoaderReference.Application, M);
					} else {
						if (!isDirectory) {
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
									Module clazzFile = new ClassURLModule(url);
									scope.addToScope(ClassLoaderReference.Application, clazzFile);
								}
							}
							jarFile.close();
						}
					}
					break;
				case CLASS:
					if (filePath.contains("EMailSender"))
						System.out.println();
					scope.addClassFileToScope(ClassLoaderReference.Application, new File(filePath));
					break;
//				case JAVA:
//					scope.addSourceFileToScope(ClassLoaderReference.Application, new File(filePath), filePath);
//					break;
				default:
					break;
				}
			}
		} else {
			System.err.println(filePath + " not exist!");
			System.exit(0);
		}
	}

	private void loadExternalLibsJars(AnalysisScope scope2, String filePath) throws IOException {
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
						addIn2PrimordialScope(scope, url);
					}
				}
				jarFile.close();
				break;

			default:
				break;
			}

		} else {
			System.err.println(filePath + " not exist!");
		}

	}

	private void addIn2PrimordialScope(AnalysisScope scope, URL url) {
		try {
			Module clazzFile = new ClassURLModule(url);
			scope.addToScope(ClassLoaderReference.Primordial, clazzFile);
		} catch (InvalidClassFileException e) {
			System.err.println("[cannot load]" + url);
		}

	}

	public ClassHierarchy getCHA() {
		return cha;
	}

	/** return cha graph only includes application classes and facke root */
	public CHACallGraph getAppCHACG() {
		return appCHACG;
	}

}
