package ict.pag.m.frameworkInfoUtil.customize;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CLIOption {
	private String analyseDir = "";
	private String extraDir = "";
	private String libsDir = "";

	private String logFile = "";
	private String configFiles = "";

	private String appKind = "";

	private final Options options = new Options();

	public CLIOption(String[] args) {
		setupOptions();
		parseOptions(args);
	}

	private void setupOptions() {
		options.addOption("extraDir", true, "add more jar files into analysis scope");
		options.addOption("configPath", true, "extra Files Location");
		options.addOption("appKind", true, "the simple name of application");
		options.addOption("libs", true, "the path of libs directory");
	}

	private void parseOptions(String[] args) {
		try {
			CommandLine line = new DefaultParser().parse(options, args);
			String[] myargs = line.getArgs();

			line.iterator().forEachRemaining(option -> {
				String opt = option.getOpt();

				switch (opt) {
				case "extraDir":
					String path = option.getValue();
					extraDir = path;
					break;
				case "configPath":
					String xml = option.getValue();
					configFiles = xml;
					break;
				case "appKind":
					String app = option.getValue();
					appKind = app;
					break;
				case "libs":
					String lp = option.getValue();
					libsDir = lp;
					break;
				}

			});

			if (myargs.length != 2 || !myargs[1].endsWith(".txt")) {
				System.err.println("invalid argument.");
			} else {
				analyseDir = myargs[0];
				logFile = myargs[1];
			}
		} catch (ParseException exp) {
			System.err.println("Unexpected exception: " + exp.getMessage());
		}
	}

	public String getAnalyseDir() {
		return analyseDir;
	}

	public String getLogFile() {
		return logFile;
	}

	public String getExtraDir() {
		return extraDir;
	}

	public Options getOptions() {
		return options;
	}

	public String getXmlPath() {
		return configFiles;
	}

	public String getAppKind() {
		return appKind;
	}

	public String getLibsDir() {
		return libsDir;
	}

}
