package ict.pag.webframework.preInstrumental;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import ict.pag.m.instrumentation.Util.ConfigureUtil;
import javassist.CannotCompileException;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;

public class PreModify_manual {

	public static void main(String[] args) {
		String path = args[0];
		String libPath = args[1];
		String externalPath = args[2];

		ArrayList<String> classFiles = new ArrayList<>();
		ArrayList<String> xmlFiles = new ArrayList<>();

		// step1. iterate current directory, find all class files and xml files
		boolean isJar = false;
		File file = new File(path);
		if (path.endsWith(".jar")) {
			isJar = true;
			interateJarFileInternal(file, classFiles, xmlFiles);
		} else {
			iterateAllClassAndXMLFiles(file, classFiles, xmlFiles);
		}

		// step2. modify annotation
		ClassPool pool = ClassPool.getDefault();
		try {
			pool.insertClassPath(externalPath + File.separator + "*");
			pool.insertClassPath(libPath + File.separator + "*");
			ClassPath cp = pool.insertClassPath(path);

			for (String classFilePath : classFiles) {
				String fileName;
				if (isJar) {
					// jar
					fileName = classFilePath.substring("BOOT-INF/classes".length() + 1);
				} else {
					// directory
					fileName = classFilePath.substring(path.length() + 1);
				}

				if (visitedClassFiles.contains(fileName))
					continue;

				dealWithSingleClassFile(classFilePath, pool, path, cp, isJar);
			}

			System.out.println("all application classes modified");
		} catch (NotFoundException e) {
			e.printStackTrace();
		}

		// step3. modify xml

	}

	public static HashSet<String> visitedClassFiles = new HashSet<>();

	private static void dealWithSingleClassFile(String classFilePath, ClassPool pool, String classesPath, ClassPath cp,
			boolean isJar) {
		if (isJar) {
			// jar file
			try (DataInputStream dis = new DataInputStream(
					(new URL("jar:file:" + classesPath + "!/" + classFilePath)).openConnection().getInputStream())) {
				ClassFile classFile = new ClassFile(dis);
//				ConstPool constPool = classFile.getConstPool();
//				Set<String> classNames = constPool.getClassNames();

				CtClass cc = pool.makeClass(classFile);
				String name = classFile.getName();
				String className = name.replaceAll("\\.", "/");

				// load into jvm
//				for (String className : classNames) {
				if (ConfigureUtil.isApplicationClass(className)) {
					if (visitedClassFiles.contains(className))
						return;

					visitedClassFiles.add(className);

					/** core: modify annotations! */
					modifyAnnotations(cc);

					try {
						/* 2. for jar */
						// 2.1 attempt 1, fail!
						/*
						 * internal only support directory
						 */
//							String jarPath = classesPath + "/BOOT-INF/classes";
//							cc.writeFile(jarPath);
						// 2.2 attempt 2, build outputStream for write api, fail
						/*
						 * JarURLConnection instances can only be used to read from JAR files. It is not
						 * possible to get a OutputStream to modify or write to the underlying JAR file
						 * using this class.
						 */
//							String filename = "jar:file:" + classesPath + "!/" + "/BOOT-INF/classes"
//									+ className.replace('.', File.separatorChar) + ".class";
//							JarURLConnection connection = (JarURLConnection) (new URL(filename)).openConnection();
//							connection.setDoOutput(true);
//							DataOutputStream out = new DataOutputStream(connection.getOutputStream());
//							cc.writeFile(out);
						// 2.3 attempt 3
						/*
						 * removes the current JAR and replaces it with the temporary JAR using the same
						 * JAR name.
						 */
						byte[] b = cc.toBytecode();
						pool.removeClassPath(cp);
						String tarClassname = "BOOT-INF/classes/" + className.replace('.', File.separatorChar)
								+ ".class";
						JarHandler jarHandler = new JarHandler();
						jarHandler.replaceSingleJarFile(classesPath, b, tarClassname, "-updated", ".");
						System.out.println("***modified success!!" + tarClassname + "***\n");
					} catch (CannotCompileException e) {
						e.printStackTrace();
					}
				}
//				}

			} catch (IOException e) {
				System.err.println("[IOException]Modifing Annotations in Jar");
				e.printStackTrace();
			}
		} else {
			// directory
			try (DataInputStream dis = new DataInputStream(new FileInputStream(new File(classFilePath)))) {
				ClassFile classFile = new ClassFile(dis);
//				ConstPool constPool = classFile.getConstPool();
//				Set<String> classNames = constPool.getClassNames();

				CtClass cc = pool.makeClass(classFile);

				String name = classFile.getName();
				String className = name.replaceAll("\\.", "/");
				// load into jvm
//				for (String className : classNames) {

				if (ConfigureUtil.isApplicationClass(className)) {
					if (visitedClassFiles.contains(className))
						return;

//					System.out.println("******************");
//					System.out.println(className);
//					System.out.println("******************");

					visitedClassFiles.add(className);

					/** core: modify annotations! */
					modifyAnnotations(cc);

					try {
						if (cc.isFrozen()) {
							System.err.println("[failed!]" + className + " is frozen\n");
						} else {
							cc.writeFile(classesPath);
							System.out.println("[success!]" + className + " modified successfully\n");
						}

					} catch (CannotCompileException e) {

						e.printStackTrace();
					}
				}
//				}

			} catch (IOException e) {
				System.err.println("[IOException]Modifing Annotations in Directory");
				e.printStackTrace();
			}
		}
	}

	/** core: */
	private static void modifyAnnotations(CtClass cc) {
//		/** TODO: core: modify class */
//		AttributeInfo classAttrInfo = cc.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
//		if (classAttrInfo != null) {
//			if (classAttrInfo instanceof AnnotationsAttribute) {
//				AnnotationsAttribute classAttr = (AnnotationsAttribute) classAttrInfo;
//				removeAnnotation(classAttr);
//			}
//		}
//
//		/** TODO: core: modify method */
//		for (CtMethod ctm : cc.getDeclaredMethods()) {
//			MethodInfo info = ctm.getMethodInfo();
//			for (AttributeInfo mtdAttrInfo : info.getAttributes()) {
//				if (mtdAttrInfo instanceof AnnotationsAttribute) {
//					AnnotationsAttribute mtdAttr = (AnnotationsAttribute) mtdAttrInfo;
//					removeAnnotation(mtdAttr);
//				}
//			}
//		}

		/** TODO: core: modify field */
		for (CtField f : cc.getDeclaredFields()) {
			FieldInfo info = f.getFieldInfo();
			for (AttributeInfo mtdAttrInfo : info.getAttributes()) {
				if (mtdAttrInfo instanceof AnnotationsAttribute) {
					AnnotationsAttribute mtdAttr = (AnnotationsAttribute) mtdAttrInfo;
					ArrayList<Annotation> rmannos = new ArrayList<>();
					if (mtdAttr.getAnnotations().length > 0) {
						for (Annotation anno : mtdAttr.getAnnotations()) {
							System.out.println("[existing]" + anno + " in " + cc.getClassFile().getName());
							rmannos.add(anno);
						}
					}
					for (Annotation rmanno : rmannos) {
						mtdAttr.removeAnnotation(rmanno.getTypeName());
					}
				}
			}
		}

	}

	private static void removeAnnotation(AnnotationsAttribute annos) {
		System.out.println("[original exisit annotation]");
		Annotation rmanno = null;
		for (Annotation anno : annos.getAnnotations()) {
			System.out.println(anno);
			if (rmanno == null)
				rmanno = anno;
		}
		if (rmanno != null) {
			System.out.println("[remove annotation on class]" + rmanno);
			annos.removeAnnotation(rmanno.getTypeName());
		}
//		System.out.println("------after-----");
//		for (Annotation anno : ((AnnotationsAttribute) cc.getClassFile()
//				.getAttribute(AnnotationsAttribute.visibleTag)).getAnnotations()) {
//			System.out.println(anno);
//		}

	}

	/** the jar web structure is that BOOT-INF/classes */
	private static void interateJarFileInternal(File file, ArrayList<String> classFiles, ArrayList<String> xmlFiles) {
		try {
			JarFile jarFile = new JarFile(file);
			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String entryName = entry.getName();

//				if (!ConfigureUtil.isApplicationClass(entryName))
//					continue;
				if (!entryName.startsWith("BOOT-INF"))
					continue;

//				URL url = new URL("jar:file:" + file.getAbsolutePath() + "!/" + entry.toString());

				if (isClassFile(entryName)) {
					classFiles.add(entryName);
				} else if (isXMLFile(entryName)) {
					xmlFiles.add(entryName);
				} else if (entryName.endsWith(".jar")) {

				}
			}
			jarFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static void iterateAllClassAndXMLFiles(File file, ArrayList<String> classFiles,
			ArrayList<String> xmlFiles) {
		func(file, classFiles, xmlFiles);
	}

	private static void func(File file, ArrayList<String> classFiles, ArrayList<String> xmlFiles) {
		File[] fs = file.listFiles();
		for (File f : fs) {
			if (f.isDirectory())
				func(f, classFiles, xmlFiles);
			if (f.isFile()) {
				if (isClassFile(f.getPath())) {
					if (!classFiles.contains(f.getPath()))
						classFiles.add(f.getPath());
				} else if (isXMLFile(f.getPath())) {
					if (!xmlFiles.contains(f.getPath()))
						xmlFiles.add(f.getPath());
				}
			}
		}

	}

	private static boolean isXMLFile(String path) {
		if (path != null && path.endsWith(".xml"))
			return true;
		return false;
	}

	private static boolean isClassFile(String path) {
		if (path != null && path.endsWith(".class"))
			return true;
		return false;
	}

	private AnnotationsAttribute getAnnotationsAttributeFromClass(CtClass ctClass) {
		List<AttributeInfo> attrs = ctClass.getClassFile().getAttributes();
		AnnotationsAttribute attr = null;
		if (attrs != null) {
			Optional<AttributeInfo> optional = attrs.stream().filter(AnnotationsAttribute.class::isInstance)
					.findFirst();
			if (optional.isPresent()) {
				attr = (AnnotationsAttribute) optional.get();
			}
		}
		return attr;
	}

	private AnnotationsAttribute getAnnotationsAttributeFromMethod(CtMethod ctMtd) {
		AnnotationsAttribute attr = null;
		List<AttributeInfo> attrs = ctMtd.getMethodInfo().getAttributes();
		if (attrs != null) {
			Optional<AttributeInfo> optional = attrs.stream().filter(AnnotationsAttribute.class::isInstance)
					.findFirst();
			if (optional.isPresent()) {
				attr = (AnnotationsAttribute) optional.get();
			}
		}
		return attr;
	}

	private AnnotationsAttribute getAnnotationsAttributeFromField(CtField ctField) {
		List<AttributeInfo> attrs = ctField.getFieldInfo().getAttributes();
		AnnotationsAttribute attr = null;
		if (attrs != null) {
			Optional<AttributeInfo> optional = attrs.stream().filter(AnnotationsAttribute.class::isInstance)
					.findFirst();
			if (optional.isPresent()) {
				attr = (AnnotationsAttribute) optional.get();
			}
		}
		return attr;
	}
}
