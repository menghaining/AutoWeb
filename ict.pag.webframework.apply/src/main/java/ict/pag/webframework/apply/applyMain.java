package ict.pag.webframework.apply;

import ict.pag.webframework.apply.core.FrameworkModel4App;
import ict.pag.webframework.apply.core.ModelExtractor;
import ict.pag.webframework.apply.option.CLOption_backEnd;
import ict.pag.webframework.apply.option.Configuration_backend;

public class applyMain {
	public static void main(String[] args) {
		CLOption_backEnd clOptions = new CLOption_backEnd(args);
		Configuration_backend.g().loadConfiguration(clOptions);

		String frameworkRulesPath = Configuration_backend.g().getFrameworkModelPath();
		String appPath = Configuration_backend.g().getAppPath();

		/* 1. extract framework model for specific application */
		ModelExtractor extractor = new ModelExtractor(appPath, frameworkRulesPath);
		extractor.extract();
		FrameworkModel4App model4app = extractor.getFrameworkModel4Application();

		/* 2. building call graph using extra information provided by framework */

		/* 3. evaluate results */
	}
}
