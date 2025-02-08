package ict.pag.m.marks2SAInfer.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;

import ict.pag.m.frameworkInfoUtil.customize.ConfigUtil;

public class FileUtil {

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

	/** use inheritance relationship extracted before */
	public static HashSet<String> getAllFrameworkLibs() {
		HashSet<String> frameworkInheritance = new HashSet<>();

		String libsFile = System.getProperty("user.dir");
		File file = new File(libsFile + File.separator + "FrameworkMarksInfoManaged.json");
		String content = null;
		try {

			content = FileUtils.readFileToString(file, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		JSONArray jsonArray = new JSONArray(content);
		jsonArray.forEach(line -> {
			frameworkInheritance.add(line.toString());
		});

		return frameworkInheritance;
	}

}
