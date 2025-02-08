package ict.pag.m.generateFrameworkModel4App.myGraphBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.classLoader.ClassURLModule;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.Selector;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;

import ict.pag.m.frameworkInfoUtil.customize.FileHelper;
import ict.pag.m.frameworkInfoUtil.customize.GraphBuilder;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.FieldPoints2Entity;
import ict.pag.m.generateFrameworkModel4App.util.Configuration_backend;
import ict.pag.m.generateFrameworkModel4App.util.UnreachableCollector;

public class myCGBuilder {
	private static boolean onlyClass = true;

	private String file_path = "";
	static String resources = System.getProperty("user.dir");
	static String exclusions = resources + File.separator + "Java60RegressionExclusions.txt";
	static String initScopeFile = resources + File.separator + "initScopeFile.txt";

	private ClassHierarchy cha = null;
	private CHACallGraph chaCG;
	private AnalysisScope scope = null;

	/** to calculate entry points */
	private Iterable<String> entrypointSigs = null;
	/** entry points */
	private Iterable<Entrypoint> entrypoints = null;
	/** whether use all unreachable roots as entry points */
	boolean unreachableEnable = false;
	/** insert object */
	private HashSet<FieldPoints2Entity> fieldPoints2targetSet;

	/** builder */
	PropagationCallGraphBuilder builder;

	/** constructor with entry-points */
	public myCGBuilder(String string, Iterable<String> entrypointSigs,
			HashSet<FieldPoints2Entity> fieldPoints2targetSet) {
//		System.out.println("[info][exclusions file location]: " + exclusions);
//		System.out.println("[info][initScopeFile file location]: " + initScopeFile);
//		System.out.println("[info][resources file location]: " + resources);

		this.file_path = string;
		this.entrypointSigs = entrypointSigs;
		this.fieldPoints2targetSet = fieldPoints2targetSet;

		try {
			long beforeTime = System.nanoTime();
			init();
			double buildTime = (System.nanoTime() - beforeTime) / 1E9;
			System.out.println("[TIME-LOG] GraphBuilder Done in " + buildTime + " s!");
		} catch (ClassHierarchyException e) {
			System.err.println("exception!");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("exception!");
			e.printStackTrace();
		}

	}

	/** constructor without entry-points */
	public myCGBuilder(String string, HashSet<FieldPoints2Entity> fieldPoints2targetSet) {
//		System.out.println("exclusions file location: " + exclusions);
//		System.out.println("initScopeFile file location: " + initScopeFile);
//		System.out.println("resources file location: " + resources);

		this.file_path = string;
		this.fieldPoints2targetSet = fieldPoints2targetSet;

		try {
			long beforeTime = System.nanoTime();
			init();
			double buildTime = (System.nanoTime() - beforeTime) / 1E9;
			System.out.println("[TIME-LOG] GraphBuilder Done in " + buildTime + " s!");
		} catch (ClassHierarchyException e) {
			System.err.println("exception!");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("exception!");
			e.printStackTrace();
		}

	}

	int actualEP = 0;

	private void init() throws IOException, ClassHierarchyException {

		scope = AnalysisScopeReader.readJavaScope(initScopeFile, null, myCGBuilder.class.getClassLoader());

		try {
			// load apps
			loadFiles(scope, file_path);
			// load lib
			HashSet<String> loadLibs = Configuration_backend.g().getLibs();
			if (!loadLibs.isEmpty()) {
				for (String file : loadLibs) {
					isDirectory = false;
					loadLibFiles(scope, file);
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

		this.cha = ClassHierarchyFactory.makeWithPhantom(scope);

		/** insert objects */
		if (Configuration_backend.g().insert)
			InsertObjects.insert(cha, fieldPoints2targetSet);

		this.chaCG = new CHACallGraph(cha, true);
		try {
			this.chaCG.init();
		} catch (CancelException e) {
			e.printStackTrace();
		}

		/* if the entry points do not passed in , then calculate */
		if (entrypointSigs == null || !Configuration_backend.g().addEntries) {
			System.out.println("[info] calculate entry points default");
			calculateEntrypointsDefault();
		} else {
			System.out.println("[info] calculate entry points using given information");
			calculateEntrypoints();
		}
		if (entrypoints == null || !entrypoints.iterator().hasNext()) {
			System.err.println("no entry point!");
//			System.exit(0);
			return;
		}

		entrypoints.forEach(en -> {
			actualEP++;
//			System.out.println("[ep]" + en.getMethod().getSignature());
		});
		System.out.println("[actual ep ]" + actualEP);

		/** build callgraph */
		AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
		AnalysisCache cache = new AnalysisCacheImpl();

		String CGType = Configuration_backend.g().getCgType();
		if (CGType.equals("zeroOne")) {
			System.out.println("[info] Use ZeroOneCFABuilder");
			builder = Util.makeZeroOneCFABuilder(Language.JAVA, options, cache, cha, scope);
		} else {
			System.out.println("[info] Use ZeroCFABuilder");
			builder = Util.makeZeroCFABuilder(Language.JAVA, options, cache, cha, scope);
		}

		try {
			System.out.println("[info] begin make callgraph");
			long beforeMakeCGTime = System.nanoTime();
			builder.makeCallGraph(options, null);
			System.out.println(
					"[info] Callgraph construction spend " + (System.nanoTime() - beforeMakeCGTime) / 1E9 + " seconds");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (CallGraphBuilderCancelException e) {
			e.printStackTrace();
		}

//		builder.getCallGraph().forEach(node -> {
//			if (node.toString().contains("fakeRootMethod")) {
//				IR ir = node.getIR();
//				System.out.println();
//
//			}
//		});

//		CGUtil.exportCallGraph(builder.getCallGraph());
	}

	private void loadLibFiles(AnalysisScope scope, String file) {

		try {
			Module M = (new FileProvider()).getJarFileModule(file, GraphBuilder.class.getClassLoader());
			scope.addToScope(ClassLoaderReference.Primordial, M);
		} catch (IOException e) {
			System.err.println("[Lib Jars Load ERROR]" + file + " not exist!");
		}

	}

	private int entryCount = 0;

	private void calculateEntrypoints() {
		List<Entrypoint> result = new ArrayList<>();
		cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(clazz -> {
			clazz.getDeclaredMethods().forEach(m -> {
				entrypointSigs.forEach(enSig -> {
					if (m.getSignature().equals(enSig) && !m.isAbstract() && !m.isPrivate()) {
						Entrypoint ep = new DefaultEntrypoint(m, cha);
						result.add(ep);
						entryCount++;
						return;
					}
				});
			});
		});

//		List<String> mainEntries = searchEntriesByMain(cha);
//		if ((mainEntries != null && !mainEntries.isEmpty())) {
//			Iterable<Entrypoint> it = Util.makeMainEntrypoints(scope, cha,
//					mainEntries.toArray(new String[mainEntries.size()]));
//			it.forEach(en -> {
//				result.add(en);
//			});
//		}

		Iterable<Entrypoint> it = Util.makeMainEntrypoints(scope, cha);
		it.forEach(en -> {
			result.add(en);
		});

		entrypoints = result;
//		System.out.println("[RESULT][add entry points total] : " + entryCount);
	}

	private void calculateEntrypointsDefault() {
		if (unreachableEnable) {
			System.out.println("[info] entry points unreachable!");
			entrypoints = UnreachableCollector.getInstance(cha).getEntryPoints();
//			entrypoints.forEach(en->{
//				if(en.getMethod().getSignature().contains("org.owasp.webgoat.session.LessonSession.getCurrentLessonScreen()Ljava/lang/String;")) {
//					System.out.println();
//				}
//			});
		} else {
			// use main as entry points
			entrypoints = Util.makeMainEntrypoints(scope, cha);

//			List<String> mainEntries = searchEntriesByMain(cha);
//			if ((mainEntries == null || mainEntries.isEmpty())) {
//				System.err.println("no main entry point!");
//				System.exit(0);
//			}
//			entrypoints = Util.makeMainEntrypoints(scope, cha, mainEntries.toArray(new String[mainEntries.size()]));
		}

	}

	private static List<String> result = new ArrayList<>();
	private static boolean calculated = false;

	public static List<String> searchEntriesByMain(IClassHierarchy cha) {
		if (calculated) {
			return result;
		}

		cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(clazz -> {
			final Atom mainMethod = Atom.findOrCreateAsciiAtom("main");
			Selector selector = new Selector(mainMethod, Descriptor.findOrCreateUTF8("([Ljava/lang/String;)V"));
			clazz.getDeclaredMethods().forEach(method -> {
				if (method.getSelector().equals(selector)) {
					String className = clazz.getName().toString();
					result.add(className);
				}
			});
		});
		calculated = true;
		return result;
	}

	/**
	 * only load class file in directory/wars/Jars
	 */
	private static boolean isDirectory = false;

	private static void loadFiles(AnalysisScope scope, String filePath)
			throws IOException, IllegalArgumentException, InvalidClassFileException {
		File file = new File(filePath);
		if (file.exists()) {
			if (file.isDirectory()) {
				isDirectory = true;
				File[] files = file.listFiles();
				for (File subFile : files) {
					loadFiles(scope, subFile.getAbsolutePath());
				}
			} else {
				switch (FileHelper.getTypeBySuffix(filePath)) {
				case JAR:
				case WAR:
					if (!onlyClass) {
						/**
						 * if not -OC, just add jar/war files have jar/war in it
						 */
						Module M = (new FileProvider()).getJarFileModule(filePath, myCGBuilder.class.getClassLoader());
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

	public PropagationCallGraphBuilder getBuilder() {
		return builder;
	}

}
