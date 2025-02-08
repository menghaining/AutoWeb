package ict.pag.m.generateFrameworkModel4App.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CLOption_backEnd {
	private final Options options = new Options();

	private String frameworkModelPath = "";
	private String appPath = "";
	private String libPath = "";

	private boolean insert = false;
	private boolean addEntries = false;
	private String cgType = "zeroOne";
	/*not enable as normal*/
	private boolean servlet = false;

	public CLOption_backEnd(String[] args) {
		setupOptions();
		parseOptions(args);
	}

	private void setupOptions() {
		options.addOption("libDir", true, "add more lib jar files into analysis scope");
		options.addOption("cg", true, "CallGraph Type");
		options.addOption("insert", false, "insert Object to reference");
		options.addOption("servlet", false, "add more servlet types as entry");
		options.addOption("addEntries", false, "add entries");
	}

	private void parseOptions(String[] args) {
		try {
			CommandLine line = new DefaultParser().parse(options, args);
			String[] myargs = line.getArgs();

			line.iterator().forEachRemaining(option -> {
				String opt = option.getOpt();

				switch (opt) {
				case "libDir":
					String path = option.getValue();
					libPath = path;
					break;
				case "insert":
					this.insert = true;
					break;
				case "addEntries":
					this.addEntries = true;
					break;
				case "cg":
					String cg = option.getValue();
					cgType = cg;
					break;
				case "servlet":
					this.servlet = true;
					break;

				}

			});

			if (myargs.length != 2 || !myargs[0].endsWith(".json")) {
				System.err.println("invalid argument.");

			} else {
				frameworkModelPath = myargs[0];
				appPath = myargs[1];
			}

			System.out.println("[info][using option] " + "[insert]:" + this.insert + "; [addEntries]:" + this.addEntries
					+ "; [servlet]:" + this.servlet);
		} catch (ParseException exp) {
			System.err.println("Unexpected exception: " + exp.getMessage());
		}
	}

	public String getFrameworkModelPath() {
		return frameworkModelPath;
	}

	public String getAppPath() {
		return appPath;
	}

	public String getLibPath() {
		return libPath;
	}

	public boolean insert() {
		return insert;
	}

	public boolean addEntries() {
		return addEntries;
	}

	public String getCgType() {
		return cgType;
	}

	public boolean getServlet() {
		return servlet;
	}

}
