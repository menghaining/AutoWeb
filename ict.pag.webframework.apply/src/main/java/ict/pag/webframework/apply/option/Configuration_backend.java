package ict.pag.webframework.apply.option;

import java.io.File;
import java.util.HashSet;

public class Configuration_backend {
	String frameworkModelPath = "";
	String appPath = "";
	String libPath = "";

	public boolean insert;
	public boolean addEntries;
	public boolean servlet;

	private String cgType;

	private static Configuration_backend instance = new Configuration_backend();

	public static Configuration_backend g() {
		if (instance == null)
			instance = new Configuration_backend();
		return instance;
	}

	public void loadConfiguration(CLOption_backEnd clOptions) {
		frameworkModelPath = clOptions.getFrameworkModelPath();
		appPath = clOptions.getAppPath();
		libPath = clOptions.getLibPath();

		this.insert = clOptions.insert();
		this.addEntries = clOptions.addEntries();
		this.servlet = clOptions.getServlet();

		this.cgType = clOptions.getCgType();
	}

	public HashSet<String> getLibs() {
		HashSet<String> ret = new HashSet<>();

		// user configured
		if (libPath.equals("")) {
		} else {
			File file = new File(libPath);
			File[] files = file.listFiles();
			for (File subFile : files) {
				System.out.println("[extra File]" + subFile.getAbsolutePath());
				ret.add(subFile.getAbsolutePath());
			}
		}

		return ret;
	}

	public String getLibDir() {
		return this.libPath;
	}

	public String getFrameworkModelPath() {
		return frameworkModelPath;
	}

	public String getAppPath() {
		return appPath;
	}

	public String getCgType() {
		return cgType;
	}

}
