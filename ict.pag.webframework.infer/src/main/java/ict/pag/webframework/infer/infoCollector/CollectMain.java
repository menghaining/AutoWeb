package ict.pag.webframework.infer.infoCollector;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentType;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.wala.classLoader.ClassURLModule;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.config.AnalysisScopeReader;

import ict.pag.webframework.XML.XMLMarksExtractor;
import ict.pag.webframework.XML.Util.XMLConfigurationUtil;
import ict.pag.webframework.infer.WriteHelper.WriteHelper;
import ict.pag.webframework.log.util.LogReaderHelper;
import ict.pag.webframework.preInstrumental.IterateWebApplication;
import ict.pag.webframework.preInstrumental.helper.ConfigurationCollector;

public class CollectMain {
	public static void main(String[] args) {
		collect();
	}

	public static void collect() {
		ArrayList<String> apps = new ArrayList<>();
		ArrayList<String> logs = new ArrayList<>();
		Testcases.init(apps, logs);

		for (int i = 0; i < apps.size(); i++) {
			String webappDir = apps.get(i);
			String logPath = logs.get(i);

			File f = new File(webappDir);
			String appName = f.getName();
			if (appName.equals("BOOT-INF"))
				appName = f.getParentFile().getName();

			try {
				calAndWrite(webappDir, logPath, appName);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			WriteHelper.writeCollect2file("all-result", -1, -1, -1, cMarks, mMarks, fMarks, crMarks, mrMarks, frMarks);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static HashSet<String> cMarks = new HashSet<>();
	static HashSet<String> mMarks = new HashSet<>();
	static HashSet<String> fMarks = new HashSet<>();
	static HashSet<String> crMarks = new HashSet<>();
	static HashSet<String> mrMarks = new HashSet<>();
	static HashSet<String> frMarks = new HashSet<>();

	public static void calAndWrite(String webappDir, String logPath, String appName) throws IOException {
		int appClasses = 0;
		int allClasses = 0;
		int reachableAppClasses = 0;

		/* 1. app information */
		String resources = System.getProperty("user.dir") + File.separator + "data";
		String initScopeFile = resources + File.separator + "initScopeFile.txt";

		AnalysisScope scope = AnalysisScopeReader.readJavaScope(initScopeFile, null, CollectMain.class.getClassLoader());
		int base = 0;
		try {
			ClassHierarchy cha0 = ClassHierarchyFactory.makeWithPhantom(scope);
			base = cha0.getNumberOfClasses();
			System.out.println("[basic classes] " + base);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
		}

		HashSet<File> dependencylibs = new HashSet<>();

		/** all */
		HashSet<String> appClassSigs = new HashSet<>(); /* all classes,like Ljava/lang/Object */
		HashSet<String> applicationClasses = new HashSet<>();/* all classes,like java.lang.Object */

		// 1. only add application classes files
		loadApplicationClasses(scope, webappDir, dependencylibs);
		try {
			ClassHierarchy cha_app = ClassHierarchyFactory.makeWithPhantom(scope);
			appClasses = cha_app.getNumberOfClasses() - base;
			System.out.println("[application classes] " + appClasses);
			cha_app.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(clazz -> {
				String classStr = clazz.getName().toString();
				appClassSigs.add(classStr);
				applicationClasses.add(classStr.replace('/', '.').substring(1));
			});
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
		}
		// 2. add the dependency libs
		for (File f : dependencylibs) {
			JarFile jarFile = null;
			try {
				jarFile = new JarFile(f);

				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					String entryName = entry.getName();
					if (!entry.isDirectory() && entryName.endsWith(".class")) {
						URL url = new URL("jar:file:" + f.getAbsolutePath() + "!/" + entry.toString());
						try {
							Module clazzFile = new ClassURLModule(url);
							scope.addToScope(ClassLoaderReference.Primordial, clazzFile);
						} catch (InvalidClassFileException e) {
							System.err.println("[cannot load]" + url);
						}
					}
				}
				jarFile.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		try {
			ClassHierarchy cha1 = ClassHierarchyFactory.makeWithPhantom(scope);
			allClasses = cha1.getNumberOfClasses() - base;
			System.out.println("[all classes] " + (allClasses));
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
		}

		/* add more jars */
		String servletLibsDir = resources + File.separator + "libs";
		File s_file = new File(servletLibsDir);
		if (s_file.exists()) {
			File[] s_files = s_file.listFiles();
			for (File f : s_files) {
				JarFile jarFile = null;
				try {
					jarFile = new JarFile(f);

					Enumeration<JarEntry> entries = jarFile.entries();
					while (entries.hasMoreElements()) {
						JarEntry entry = entries.nextElement();
						String entryName = entry.getName();
						if (!entry.isDirectory() && entryName.endsWith(".class")) {
							URL url = new URL("jar:file:" + f.getAbsolutePath() + "!/" + entry.toString());
							try {
								Module clazzFile = new ClassURLModule(url);
								scope.addToScope(ClassLoaderReference.Primordial, clazzFile);
							} catch (InvalidClassFileException e) {
								System.err.println("[cannot load]" + url);
							}
						}
					}
					jarFile.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		ClassHierarchy cha = null;
		try {
			cha = ClassHierarchyFactory.makeWithPhantom(scope);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
		}

		/** reachable */
		HashMap<String, HashSet<String>> class2methods = new HashMap<>(); /* reachable class and its declare methods, all declare fields considered */
		HashMap<String, HashSet<String>> class2otherFields = new HashMap<>();/* class : only some field occurs */

		File file = new File(logPath);
		try {
			FileReader fileReader = new FileReader(file);
			LineNumberReader reader = new LineNumberReader(fileReader);

			String lineContent0;
			while ((lineContent0 = reader.readLine()) != null) {
				String lineContent = lineContent0.trim();
				int currThread = LogReaderHelper.getThreadID(lineContent, true);
				if (currThread == -1)/* not instrumented log are filtered */
					continue;

				// remove header_tag
				String header = lineContent.substring(1, lineContent.indexOf(']'));
				String line1 = lineContent.substring(lineContent.indexOf(']') + 1);
				// remove id
				String line2 = line1.substring(line1.indexOf(']') + 1);

				switch (header) {
				case "call method":
					String classStr1 = line2.substring(0, line2.lastIndexOf('.')); /* separate by . */
					String clazz1 = "L" + classStr1.replace('.', '/');
					if (appClassSigs.contains(clazz1)) {
						if (!class2methods.containsKey(clazz1))
							class2methods.put(clazz1, new HashSet<>());
						class2methods.get(clazz1).add(line2);
						if (class2otherFields.containsKey(clazz1))
							class2otherFields.remove(clazz1);
					}
					break;
				case "callsite":
					String line3 = line2.substring(0, line2.indexOf(']'));
					String method = line3.substring(0, line3.lastIndexOf('['));
					String classStr2 = method.substring(0, method.lastIndexOf('.')); /* separate by . */
					String clazz2 = "L" + classStr2.replace('.', '/');
					if (appClassSigs.contains(clazz2)) {
						if (!class2methods.containsKey(clazz2))
							class2methods.put(clazz2, new HashSet<>());
						class2methods.get(clazz2).add(method);
						if (class2otherFields.containsKey(clazz2))
							class2otherFields.remove(clazz2);
					}
					break;
				case "field read":
				case "field write":
					String line4 = line2.substring(line2.indexOf("[signature]") + "[signature]".length());
					String line5 = line4.substring(0, line4.indexOf(']'));
					String fieldSig0 = line5.substring(0, line5.lastIndexOf('['));/* com.logicaldoc.util.config.ContextProperties.docPath:Ljava/lang/String; */
					String[] eles = fieldSig0.split(":");
					String fieldFullName = eles[0];
					String decType = eles[1];
					String fieldName = fieldFullName.substring(fieldFullName.lastIndexOf('.') + 1);
					String c = fieldFullName.substring(0, fieldFullName.lastIndexOf('.'));
					String fieldSig = "L" + c.replace('.', '/') + "." + fieldName + " " + decType;
					String classStr3 = fieldFullName.substring(fieldFullName.lastIndexOf('.'));
					String clazz3 = "L" + classStr3.replace(".", "/");
					if (appClassSigs.contains(clazz3)) {
						if (!class2methods.containsKey(clazz3)) {
							if (!class2otherFields.containsKey(clazz3))
								class2otherFields.put(clazz3, new HashSet<>());
							class2otherFields.get(clazz3).add(fieldSig);
						}
					}
					// runtime type
					String line6 = line4.substring(line4.indexOf(']') + 1);
					String line7 = line6.substring(0, line6.indexOf(']'));
					String runtimeType0 = line7.substring(0, line7.lastIndexOf('['));
					String runtimeType = "L" + runtimeType0.replace(".", "/");
					if (appClassSigs.contains(runtimeType)) {
						if (!class2methods.containsKey(runtimeType)) {
							if (!class2otherFields.containsKey(runtimeType))
								class2otherFields.put(runtimeType, new HashSet<>());
							class2otherFields.get(runtimeType).add(fieldSig);
						}
					}
					break;
				case "base field":
					String[] alls = line2.split(":");
					String rt0 = alls[alls.length - 1];
					String rt = "L" + rt0.replace('.', '/');
					if (appClassSigs.contains(rt)) {
						if (!class2methods.containsKey(rt)) {
							class2methods.put(rt, new HashSet<>());
						}
					}
					break;
//					default:
//						System.out.println("Donot handel: " + lineContent);
				}
			}
			fileReader.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// calculate all reachable application classes
		HashSet<String> reachableClassesSet = new HashSet<>();
		if (!class2methods.keySet().isEmpty())
			reachableClassesSet.addAll(class2methods.keySet());
		if (!class2otherFields.keySet().isEmpty())
			reachableClassesSet.addAll(class2otherFields.keySet());
		reachableAppClasses = reachableClassesSet.size();

		/** core: calculate all needed */
		/* configuration number */
		HashSet<String> classMarks_reachable = new HashSet<>();
		HashSet<String> methodMarks__reachable = new HashSet<>();
		HashSet<String> fieldMarks__reachable = new HashSet<>();
		HashSet<String> classMarks = new HashSet<>();
		HashSet<String> methodMarks = new HashSet<>();
		HashSet<String> fieldMarks = new HashSet<>();

		ArrayList<String> xmlFiles = new ArrayList<>();
		File dir = new File(webappDir);
		IterateWebApplication.interateAllXMLFiles(dir, xmlFiles);
		XMLMarksExtractor xmlex = new XMLMarksExtractor(xmlFiles, applicationClasses);
		HashMap<String, HashSet<String>> class2XMLFile = xmlex.getClass2XMLFile();

		cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(clazz -> {
			String classStr = clazz.getName().toString(); /* Ljava/lang/Objet */
			String classStr2 = classStr.replace('/', '.').substring(1); /* for xml */
			if (appClassSigs.contains(classStr)) {
				/** annos */
				// 1. class level
				HashSet<String> class_annos = new HashSet<String>();
				if (clazz.getAnnotations() != null)
					for (Annotation anno : clazz.getAnnotations()) {
						if (!appClassSigs.contains(anno.getType().getName().toString())) {
							class_annos.add(anno.getType().getName().toString());
						}
					}
				if (!class_annos.isEmpty()) {
					classMarks.addAll(class_annos);
					if (class2methods.containsKey(classStr) || class2otherFields.containsKey(classStr))
						classMarks_reachable.addAll(class_annos);
				}
				// 2. method level
				for (IMethod mtd : clazz.getDeclaredMethods()) {
					HashSet<String> mtd_annos = new HashSet<String>();
					if (mtd.getAnnotations() != null) {
						for (Annotation anno : mtd.getAnnotations()) {
							if (!appClassSigs.contains(anno.getType().getName().toString())) {
								mtd_annos.add(anno.getType().getName().toString());
							}
						}
					}
					if (!mtd_annos.isEmpty()) {
						methodMarks.addAll(mtd_annos);
						if (class2methods.containsKey(classStr) && class2methods.get(classStr).contains(mtd.getSignature()))
							methodMarks__reachable.addAll(mtd_annos);
					}
				}

				// 3. field level
				for (IField field : clazz.getDeclaredInstanceFields()) {
					HashSet<String> field_annos = new HashSet<String>();
					if (field.getAnnotations() != null) {
						for (Annotation anno : field.getAnnotations())
							if (!appClassSigs.contains(anno.getType().getName().toString())) {
								field_annos.add(anno.getType().getName().toString());
							}
					}
					if (!field_annos.isEmpty()) {
						fieldMarks.addAll(field_annos);
						if (class2methods.containsKey(classStr))
							fieldMarks__reachable.addAll(field_annos);
						else if (class2otherFields.containsKey(classStr) && class2otherFields.get(classStr).contains(field.getReference().getSignature()))
							fieldMarks__reachable.addAll(field_annos);
					}
				}

				/** xmls */
				if (class2XMLFile.containsKey(classStr2)) {
					for (String xmlFile : class2XMLFile.get(classStr2)) {

						SAXReader reader = new SAXReader();
						reader.setIgnoreComments(true);
						reader.setValidation(false);
						reader.setEntityResolver(new EntityResolver() {
							@Override
							public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
								return new InputSource(new ByteArrayInputStream("".getBytes()));
							}
						});
						try {
							Document document = reader.read(xmlFile);
							/* exclude database configure */
							DocumentType docType = document.getDocType();
							if (docType != null)
								if (XMLConfigurationUtil.notConcernedXMLID(docType.getPublicID())
										|| XMLConfigurationUtil.notConcernedXMLID(docType.getSystemID()))
									continue;

							Element root = document.getRootElement();

							// 1. class
							// 2. method
							for (IMethod method : clazz.getDeclaredMethods()) {
								String stmt = method.getSignature();
								HashSet<String> cMarks = new HashSet<>();
								HashSet<String> mMarks = new HashSet<>();
								ConfigurationCollector.collectMethodXMLConfigurationMarks(root, stmt, xmlFile, false, cMarks, mMarks);
								if (!cMarks.isEmpty()) {
									classMarks.addAll(cMarks);
									if (class2methods.containsKey(classStr) || class2otherFields.containsKey(classStr))
										classMarks_reachable.addAll(cMarks);
								}
								if (!mMarks.isEmpty()) {
									methodMarks.addAll(mMarks);
									if (class2methods.containsKey(classStr) && class2methods.get(classStr).contains(method.getSignature()))
										methodMarks__reachable.addAll(mMarks);
								}
							}

							// 3. field
							for (IField f : clazz.getDeclaredInstanceFields()) {
								String sig = f.getReference().getSignature();
								// covert to
								// org.springframework.samples.petclinic.model.NamedEntity.name:Ljava/lang/String;
								String[] eles = sig.split(" ");
								String fSig = (eles[0]).replace('/', '.').substring(1) + ":" + eles[1];
								HashSet<String> fMarks = new HashSet<>();
								ConfigurationCollector.collectFieldXMLConfigurationMarks(root, fSig, xmlFile, false, fMarks);
								if (!fMarks.isEmpty()) {
									fieldMarks.addAll(fMarks);
									if (class2methods.containsKey(classStr))
										fieldMarks__reachable.addAll(fMarks);
									else if (class2otherFields.containsKey(classStr)
											&& class2otherFields.get(classStr).contains(f.getReference().getSignature()))
										fieldMarks__reachable.addAll(fMarks);
								}
							}

						} catch (DocumentException e) {
							System.err.println("[error][DocumentException]" + e.getMessage() + " when parse " + xmlFile);
						}

					}
				}
			}
		});

		System.out.println();

		// write to file
		WriteHelper.writeCollect2file(appName, appClasses, allClasses, reachableAppClasses, classMarks, methodMarks, fieldMarks, classMarks_reachable,
				methodMarks__reachable, fieldMarks__reachable);

		cMarks.addAll(classMarks);
		mMarks.addAll(methodMarks);
		fMarks.addAll(fieldMarks);
		crMarks.addAll(classMarks_reachable);
		mrMarks.addAll(methodMarks__reachable);
		frMarks.addAll(fieldMarks__reachable);
	}

	private static void loadApplicationClasses(AnalysisScope scope, String filePath, HashSet<File> dependencylibs) {
		File file = new File(filePath);
		if (file.exists()) {
			if (file.isDirectory()) {
				File[] files = file.listFiles();
				for (File subFile : files) {
					loadApplicationClasses(scope, subFile.getAbsolutePath(), dependencylibs);
				}

			} else if (filePath.endsWith(".class")) {
				try {
					scope.addClassFileToScope(ClassLoaderReference.Application, new File(filePath));
				} catch (IllegalArgumentException | InvalidClassFileException e) {
					e.printStackTrace();
				}
			} else if (filePath.endsWith(".jar")) {
				dependencylibs.add(file);
			}
		}

	}
}
