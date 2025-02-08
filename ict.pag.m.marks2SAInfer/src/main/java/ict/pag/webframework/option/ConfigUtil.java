package ict.pag.webframework.option;

public class ConfigUtil {
	public static boolean printLogs = false;
	public static boolean onlyClass = true;
	public static boolean containsLibInfo = true;

	/** application is depends on classloaderreference type */
	public static boolean enableApplication = true;

	private String analyseDir = "";
	private String libsDir = "";
	private String logFile = "";

	public String configFiles = "";

//	private ClassHierarchy cha_appWithLibs;

	public static String appKind = "";

	private static ConfigUtil instance = new ConfigUtil();

	public static ConfigUtil g() {
		if (instance == null)
			instance = new ConfigUtil();
		return instance;
	}

	public void loadConfiguration(CLIOption cliOptions) {
		analyseDir = cliOptions.getAnalyseDir();
		logFile = cliOptions.getLogFile();
		configFiles = cliOptions.getXmlPath();
		appKind = cliOptions.getAppKind();
		libsDir = cliOptions.getLibsDir();
	}

	/** need to configure application class */
	public static boolean isApplicationClass(String name) {

		if (name.startsWith("L") && name.contains("/"))
			name = name.substring(1).replaceAll("/", ".");

		if (appKind.equals("")) {
			return true;
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

	/** @return the directory that contains only the application code */
	public String getAnalyseDir() {
		return analyseDir;
	}

	public String getLogFile() {
		return logFile;
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

	public String getLibsDir() {
		return libsDir;
	}

}
