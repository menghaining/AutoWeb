package ict.pag.webframework.main;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.dom4j.Element;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;

import ict.pag.webframework.answer.AnswerExporter;
import ict.pag.webframework.core.solver.FrameworkMarksSolver;
import ict.pag.webframework.enumeration.MarkScope;
import ict.pag.webframework.graph.GraphBuilder;
import ict.pag.webframework.log.LogParser;
import ict.pag.webframework.marks.MarksHelper;
import ict.pag.webframework.marks.ResolveMarks;
import ict.pag.webframework.marks.XMLMarksExtractor;
import ict.pag.webframework.option.CLIOption;
import ict.pag.webframework.option.ConfigUtil;
import ict.pag.webframework.option.FileHelper;

public class Main {
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
		Set<String> unreachableRoots = builder.calAllUnreachableEntryPoints();
		ClassHierarchy cha = builder.getCHA();
		HashSet<String> applicationClasses = builder.getApplicationClasses();

		/**
		 * Step 1. deal with running logs
		 */
		String log_file = ConfigUtil.g().getLogFile();
		LogParser logparser = new LogParser(log_file, unreachableRoots);

		/**
		 * Step 2. Extract xml infos
		 */
		XMLMarksExtractor xmlex = new XMLMarksExtractor(FileHelper.getAllConcernedXMLs(ConfigUtil.g().getConfigFiles()),
				applicationClasses);
		HashMap<String, HashSet<Element>> class2XMLEle = xmlex.getClass2XMLElement();

		/* calculate all runnable marks */
		HashSet<String> allrunnableMarks = new HashSet<>();
		HashSet<String> dynamicCoversClasses = new HashSet<>();
		for (String mtd : logparser.getAllRunnableMethods()) {
			String clazzName = mtd.substring(0, mtd.lastIndexOf('.'));
			dynamicCoversClasses.add(clazzName);

			String clazzStr = "L" + clazzName.replaceAll("\\.", "/");
			IClass clazz = cha.lookupClass(
					TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazzStr)));
			if (clazz == null)
				continue;
			for (Annotation anno : ResolveMarks.resolve_Annotation(clazz, MarkScope.Clazz)) {
				allrunnableMarks.add("[anno]" + MarksHelper.resolveAnnotationName(anno));
			}
			for (String ele : ResolveMarks.resolve_XML_String(clazz, MarkScope.Clazz, class2XMLEle)) {
				allrunnableMarks.add("[xml]" + ele);
			}
			for (String ele : ResolveMarks.resolve_Inheritance(clazz, MarkScope.Clazz, cha)) {
				allrunnableMarks.add("[inheritance]" + ele);
			}

			IMethod method = null;
			for (IMethod m : clazz.getAllMethods()) {
				if (m.getSignature().equals(mtd)) {
					method = m;
					break;
				}
			}
			if (method == null)
				continue;

			for (Annotation anno : ResolveMarks.resolve_Annotation(method, MarkScope.Method)) {
				allrunnableMarks.add("[anno]" + MarksHelper.resolveAnnotationName(anno));
			}
			for (String ele : ResolveMarks.resolve_XML_String(method, MarkScope.Method, class2XMLEle)) {
				allrunnableMarks.add("[xml]" + ele);
			}
			for (String ele : ResolveMarks.resolve_Inheritance(method, MarkScope.Method, cha)) {
				allrunnableMarks.add("[inheritance]" + ele);
			}
		}
		System.out.println("[info][running covers classes] " + dynamicCoversClasses.size());
		System.out.println("[info][running covers methods] " + logparser.getAllRunnableMethods().size());
		System.out.println("[info]=========[runnable covers mark]===========");
		for (String m : allrunnableMarks)
			System.out.println("[info][runnable covers mark] " + m);
		System.out.println("[info]=========[runnable covers mark]===========");

		/**
		 * Step 3. solve marks
		 */
		FrameworkMarksSolver solver = new FrameworkMarksSolver(builder, logparser, class2XMLEle);
		solver.sovle();

		/** Export Answers to Json File */
		AnswerExporter exporter = new AnswerExporter(solver.getEntryMarkSet(), solver.getManagedClassMarks(),
				solver.getFieldInjectMarks(), solver.getFieldPoints2Marks(), solver.getAliasMarks(),
				solver.getFrameworkCallReturnPoints2Marks(), solver.getMayEntryPointFormalParameterSet(),
				solver.getFrameworkCallMarks());
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
