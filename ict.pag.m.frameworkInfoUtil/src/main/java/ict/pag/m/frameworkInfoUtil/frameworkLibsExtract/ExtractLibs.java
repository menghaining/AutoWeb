package ict.pag.m.frameworkInfoUtil.frameworkLibsExtract;

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
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.io.FileProvider;

import ict.pag.m.frameworkInfoUtil.customize.FileHelper;
import ict.pag.m.frameworkInfoUtil.customize.GraphBuilder;

public class ExtractLibs {
//	private String file_path = "";
	static String resources = System.getProperty("user.dir");
	static String exclusions = resources + File.separator + "Java60RegressionExclusions.txt";
	static String initScopeFile = resources + File.separator + "initScopeFile.txt";

	private static ClassHierarchy cha = null;
	private static AnalysisScope scope = null;

	private HashSet<String> allFrameworkInheritanceRelation = new HashSet<>();

	public ExtractLibs(String frameworkJarPath) {
		try {
			long beforeTime = System.nanoTime();
			init(frameworkJarPath);
			double buildTime = (System.nanoTime() - beforeTime) / 1E9;
			System.out.println("[TIME-LOG] Get All Framework Inheritance Relation in " + buildTime + " s!");
		} catch (IllegalArgumentException | IOException | InvalidClassFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public HashSet<String> getAllFrameworkInheritanceRelation() {
		return allFrameworkInheritanceRelation;
	}

	private void init(String path) throws IOException, IllegalArgumentException, InvalidClassFileException {
		HashSet<String> ret = new HashSet<>();
		if (path.equals("")) {
			System.err.println("please input valid path");
			return;
		} else {
			File file = new File(path);
			File[] files = file.listFiles();
			for (File subFile : files) {
				ret.add(subFile.getAbsolutePath());
			}
		}

		scope = AnalysisScopeReader.readJavaScope(initScopeFile, null, GraphBuilder.class.getClassLoader());

		// add extra files
		HashSet<String> extraLoadFiles = ret;

		if (!extraLoadFiles.isEmpty()) {
			int count = 0;
			for (String file : extraLoadFiles) {
				isDirectory = false;
				loadFiles(scope, file);
				count++;
			}
			System.out.println("Load Framework Jars: " + count);
		}

		File exclusionsFile = exclusions != null ? new File(exclusions) : null;
		if (exclusionsFile != null) {
			try (final InputStream fs = exclusionsFile.exists() ? new FileInputStream(exclusionsFile)
					: FileProvider.class.getClassLoader().getResourceAsStream(exclusionsFile.getName())) {
				scope.setExclusions(new FileOfClasses(fs));
			}
		}

		try {
			cha = ClassHierarchyFactory.makeWithPhantom(scope);
			cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(clazz -> {

				/**
				 * for a class, get all methods belong to it, </br>
				 * including the methods decalred in superclass</br>
				 * example. </br>
				 * ClassA extends ClassB{ function_a(){}}</br>
				 * ClassB{ function_b(){}}</br>
				 * when recording ClassA's methods, including ClassA.function_b();
				 */
				clazz.getAllMethods().forEach(method -> {
					if (!method.getSignature().startsWith("java.lang.Object")) {
						allFrameworkInheritanceRelation.add(method.getSignature());
//						System.out.println("\t" + method.getSignature());
					}

				});
			});

			/* optional: Construct cha graph only includes application codes */
//			CHACallGraph appCHACG = new CHACallGraph(cha, true);
//			try {
//				appCHACG.init();
//			} catch (CancelException e) {
//				e.printStackTrace();
//			}
//			cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(clazz -> {
//				clazz.getDeclaredMethods().forEach(method -> {
//					try {
//						CGNode node = appCHACG.findOrCreateNode(method, Everywhere.EVERYWHERE);
//					} catch (CancelException e) {
//						e.printStackTrace();
//					}
//
//				});
//			});

		} catch (ClassHierarchyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

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
					break;
				case CLASS:
					scope.addClassFileToScope(ClassLoaderReference.Application, new File(filePath));
					break;
				default:
					break;
				}
			}
		} else {
			System.err.println(filePath + " not exist!");
			System.exit(0);
		}
	}
}
