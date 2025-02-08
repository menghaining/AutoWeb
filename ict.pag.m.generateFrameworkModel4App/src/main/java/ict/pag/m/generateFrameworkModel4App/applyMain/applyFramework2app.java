package ict.pag.m.generateFrameworkModel4App.applyMain;

import ict.pag.m.generateFrameworkModel4App.parseApp.AppParser;
import ict.pag.m.generateFrameworkModel4App.util.CLOption_backEnd;
import ict.pag.m.generateFrameworkModel4App.util.Configuration_backend;

public class applyFramework2app {

	public static void main(String[] args) {
		CLOption_backEnd clOptions = new CLOption_backEnd(args);
		Configuration_backend.g().loadConfiguration(clOptions);
		
		String frameworkRulesPath = Configuration_backend.g().getFrameworkModelPath();
		String appPath = Configuration_backend.g().getAppPath();

		AppParser parser = new AppParser(frameworkRulesPath, appPath);
	}

}
