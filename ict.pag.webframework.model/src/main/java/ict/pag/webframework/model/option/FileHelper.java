package ict.pag.webframework.model.option;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileHelper {
	public static FileType getTypeBySuffix(String filePath) {
		if (!filePath.contains("."))
			return FileType.UNKOWN;
		String extension = filePath.substring(filePath.lastIndexOf("."));
		switch (extension) {
		case ".xml":
			return FileType.XML;
		case ".txt":
			return FileType.TXT;
		case ".jar":
			return FileType.JAR;
		case ".war":
			return FileType.WAR;
		case ".json":
			return FileType.JSON;
		case ".class":
			return FileType.CLASS;
		case ".java":
			return FileType.JAVA;
		case ".apk":
			return FileType.APK;
		default:
			return FileType.UNKOWN;
		}
	}

	public static List<String> getAllConcernedXMLs(String path) {
		List<String> ret = new ArrayList<String>();
		File file = new File(path);
		func(file, ret);
		return ret;

	}

	private static void func(File file, List<String> ret) {
		File[] fs = file.listFiles();
		for (File f : fs) {
			if (f.isDirectory())
				func(f, ret);
			if (f.isFile()) {
				if (isConcerned(f.getPath())) {
					if (!ret.contains(f.getPath()))
						ret.add(f.getPath());
				}
			}

		}
	}

	private static boolean isConcerned(String file) {
		if (!file.endsWith(".xml"))
			return false;

		// do not care about dao
		if (file.toLowerCase().contains("dao") || file.toLowerCase().contains("sql"))
			return false;

		// update: parse file.endsWith("web.xml")
		if (file.endsWith("pom.xml"))
			return false;
		// database
		if (file.endsWith("hibernate.cfg.xml") || file.endsWith(".hbm.xml"))
			return false;
		// database tracker
		if (file.contains("liquibase"))
			return false;

		// cache
		if (file.contains("ehcache"))
			return false;

		// JDBC
		if (file.contains("c3p0"))
			return false;

		// Maven
		if (file.contains("Maven") || file.contains("maven"))
			return false;
		// log
		if (file.contains("log4j"))
			return false;

		if (file.contains("javadoc") || file.contains("properties.xml"))
			return false;

		if (ConfigUtil.printLogs)
			System.out.println("[configured file]" + file);

		return true;
	}
}
