package ict.pag.webframework.preInstrumental;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CLIOption {
	private String analyseDir = "";
	private String logFile = "";

	private String libsDir = "";
	private String extraDir = "";

	private boolean isJar = false;
	private String jarPath = "";

	private String outPath = System.getProperty("user.dir") + File.separator + "outs" + File.separator + "out-preModify";

	private final Options options = new Options();

	public CLIOption(String[] args) {
		setupOptions();
		parseOptions(args);
	}

	private void setupOptions() {
		options.addOption("extraDir", true, "add more jar files into analysis scope");
		options.addOption("libs", true, "the path of libs directory");
		options.addOption("out", true, "the path of preModify content");
		options.addOption("isJar", true, "the path of jar when web app is jar");
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
				case "libs":
					String lp = option.getValue();
					libsDir = lp;
					break;
				case "out":
					outPath = option.getValue();
					break;
				case "isJar":
					isJar = true;
					jarPath = option.getValue();
					if (!jarPath.endsWith(".jar")) {
						System.err.println("the jar app must ends with .jar");
						System.exit(0);
					}
					break;
				}

			});

			if (myargs.length != 2 || !myargs[1].endsWith(".txt")) {
				System.err.println("invalid log file argument.");
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

	public String getLibsDir() {
		return libsDir;
	}

	public String getOutPath() {
		return outPath;
	}

	public boolean isJar() {
		return isJar;
	}

	public String getJarPath() {
		return jarPath;
	}

}
