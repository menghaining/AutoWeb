package ict.pag.webframework.model.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import org.dom4j.Element;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;

import ict.pag.webframework.XML.XMLMarksExtractor;
import ict.pag.webframework.model.answer.AnswerExporter2;
import ict.pag.webframework.model.core.calculator.EntryCalculator;
import ict.pag.webframework.model.core.calculator.FieldInjectCalculator;
import ict.pag.webframework.model.graph.GraphBuilder;
import ict.pag.webframework.model.log.Callsite2CallSeqMapTool;
import ict.pag.webframework.model.log.RuntimeInfoParser;
import ict.pag.webframework.model.option.CLIOption;
import ict.pag.webframework.model.option.ConfigUtil;
import ict.pag.webframework.model.option.FileHelper;

/** Goal: find the semantic calls and export the marks on it */
public class ModelMain {
	/**
	 * arg[0]: classes directory including configuration files</br>
	 * arg[1]: running log txt file
	 */
	public static void main(String args[]) {
		long beforeTime = System.nanoTime();

		CLIOption cliOptions = new CLIOption(args);
		ConfigUtil.g().loadConfiguration(cliOptions);
		String path = ConfigUtil.g().getAnalyseDir();

		System.out.println("......[info][start]" + path);

		/** Information prepare */
		GraphBuilder builder = new GraphBuilder(true);
		ClassHierarchy cha = builder.getCHA();
		CHACallGraph chaCG = builder.getAppCHACG();
		HashSet<String> applicationClasses = builder.getApplicationClasses();

		/**
		 * Step 1. deal with running logs
		 */
		RuntimeInfoParser p = new RuntimeInfoParser();
		p.parser(ConfigUtil.g().getLogFile());
		HashMap<Integer, ArrayList<String>> id2group = p.getId2group();

		Callsite2CallSeqMapTool tool = new Callsite2CallSeqMapTool(chaCG, p.getOuter2Seq(), applicationClasses);
		tool.dealWith();
		/**
		 * Step 2. Extract xml infos
		 */
		XMLMarksExtractor xmlex = new XMLMarksExtractor(FileHelper.getAllConcernedXMLs(ConfigUtil.g().getConfigFiles()),
				applicationClasses);
		HashMap<String, HashSet<Element>> class2XMLEle = xmlex.getClass2XMLElement();

		/* calculate entry */
		EntryCalculator entry_calculator = new EntryCalculator(cha, chaCG, class2XMLEle, p.getOuter2Seq());
		entry_calculator.calculate(tool);
		/* calculate all runnable marks */
		FieldInjectCalculator f_calculator = new FieldInjectCalculator(cha, chaCG, class2XMLEle, id2group);
		f_calculator.calculate(tool);

		/**
		 * Step 3. export marks
		 */
		/** Export Answers to Json File */
		AnswerExporter2 exporter = new AnswerExporter2(entry_calculator.getEntryMarkSet(),
				entry_calculator.getNotEntryMarkSet(), f_calculator.getFieldInjectMarks(),
				f_calculator.getFieldNOTInjectMarks());
		try {
			exporter.export2Json();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		double buildTime = (System.nanoTime() - beforeTime) / 1E9;
		System.out.println("[TIME-LOG] All Solvers Done in " + buildTime + " s!");

	}
}
