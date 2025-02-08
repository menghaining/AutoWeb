package ict.pag.webframework.preInstrumental;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import ict.pag.webframework.XML.Util.XMLConfigurationUtil;

public class IterateWebApplication {
	/** the jar web structure is that BOOT-INF/classes */
	public static void interateJarFileInternal(File file, ArrayList<String> classFiles, ArrayList<String> xmlFiles, ArrayList<String> libJars) {
		try {
			JarFile jarFile = new JarFile(file);
			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String entryName = entry.getName();

				if (!entryName.startsWith("BOOT-INF"))
					continue;

				if (isClassFile(entryName)) {
					classFiles.add(entryName);
				} else if (isXMLFile(entryName)) {
					xmlFiles.add(entryName);
				} else if (entryName.endsWith(".jar")) {
					libJars.add(entryName);
				}
			}
			jarFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void iterateAllClassAndXMLFiles(File file, ArrayList<String> classFiles, ArrayList<String> xmlFiles) {
		func(file, classFiles, xmlFiles);
	}

	public static void func(File file, ArrayList<String> classFiles, ArrayList<String> xmlFiles) {
		File[] fs = file.listFiles();
		for (File f : fs) {
			if (f.isDirectory())
				func(f, classFiles, xmlFiles);
			if (f.isFile()) {
				if (isClassFile(f.getName())) {
					if (!classFiles.contains(f.getPath()))
						classFiles.add(f.getPath());
				} else if (isXMLFile(f.getName())) {
					if (!xmlFiles.contains(f.getPath()))
						xmlFiles.add(f.getPath());
				}
			}
		}
	}

	public static boolean isXMLFile(String path) {
		if (path == null || !path.endsWith(".xml"))
			return false;

		if (XMLConfigurationUtil.isConcernedXMLFile(path))
			return true;

		return false;
	}

	public static boolean isClassFile(String path) {
		if (path != null && path.endsWith(".class"))
			return true;
		return false;
	}

	public static void interateAllXMLFiles(File file, ArrayList<String> xmlFiles) {
		File[] fs = file.listFiles();
		for (File f : fs) {
			if (f.isDirectory()) {
				interateAllXMLFiles(f, xmlFiles);
			} else if (f.isFile()) {
				if (isXMLFile(f.getName())) {
					if (!xmlFiles.contains(f.getPath()))
					xmlFiles.add(f.getPath());
				}
			}
		}
	}
}
