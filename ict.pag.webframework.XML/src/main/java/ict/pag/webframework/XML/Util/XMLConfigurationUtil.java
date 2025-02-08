package ict.pag.webframework.XML.Util;

public class XMLConfigurationUtil {
	public static boolean notConcernedXMLID(String type) {
		if (type != null) {
			if (type.toLowerCase().contains("hibernate") || type.toLowerCase().contains("mybatis")
					|| type.toLowerCase().contains("log4j") || type.toLowerCase().contains("plugin"))
				return true;
		}

		return false;
	}

	public static boolean isConcernedXMLFile(String file) {
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
		if (file.toLowerCase().contains("hibernate") || file.toLowerCase().contains("mybatis")
				|| file.toLowerCase().contains("log4j") || file.toLowerCase().contains("plugin"))
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

		return true;
	}
}
