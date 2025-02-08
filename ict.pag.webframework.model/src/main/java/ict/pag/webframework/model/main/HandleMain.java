package ict.pag.webframework.model.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.dom4j.Element;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;

import ict.pag.webframework.XML.XMLMarksExtractor;
import ict.pag.webframework.log.LogReader;
import ict.pag.webframework.model.answer.Exporter;
import ict.pag.webframework.model.core.calculator.CallSequenceCalculator;
import ict.pag.webframework.model.core.calculator.EntryCalculator2;
import ict.pag.webframework.model.core.calculator.FieldInjectCalculator2;
import ict.pag.webframework.model.core.calculator.IndirectCallCalculator2;
import ict.pag.webframework.model.enumeration.MarkScope;
import ict.pag.webframework.model.graph.GraphBuilder;
import ict.pag.webframework.model.logprase.ExtractRuntimeInfoEachMethod;
import ict.pag.webframework.model.logprase.RTInfoDetailsClassifer;
import ict.pag.webframework.model.marks.MarksHelper;
import ict.pag.webframework.model.marks.ResolveMarks;
import ict.pag.webframework.model.option.CLIOption;
import ict.pag.webframework.model.option.ConfigUtil;
import ict.pag.webframework.model.option.FileHelper;
import ict.pag.webframework.model.option.SpecialHelper;

/** Goal: find the semantic calls and export the marks on it */
public class HandleMain {
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

		/** handle running logs */
		/* 1. read */
		HashMap<Integer, ArrayList<String>> id2group = LogReader.calId2Sequence(ConfigUtil.g().getLogFile());
		/* 2. extract run-time info for each method */
		HashMap<String, Set<ArrayList<String>>> mtd2RTSeq = ExtractRuntimeInfoEachMethod.split(id2group);

		/** Information prepare */
		GraphBuilder builder = new GraphBuilder(true);
		ClassHierarchy cha = builder.getCHA();
		CHACallGraph chaCG = builder.getAppCHACG();
		HashSet<String> applicationClasses = builder.getApplicationClasses();

		/* 3. classify more details about runtime sequence */
		RTInfoDetailsClassifer classfier = new RTInfoDetailsClassifer(cha, mtd2RTSeq);
		classfier.classify();

		/** Extract xml infos */
		XMLMarksExtractor xmlex = new XMLMarksExtractor(FileHelper.getAllConcernedXMLs(ConfigUtil.g().getConfigFiles()),
				applicationClasses);
		HashMap<String, HashSet<Element>> class2XMLEle = xmlex.getClass2XMLElement();

		IndirectCallCalculator2 indirectCallCalculator = new IndirectCallCalculator2(cha, class2XMLEle, classfier);
		CallSequenceCalculator callSeqCalculator = new CallSequenceCalculator(class2XMLEle, cha, classfier);

		/**
		 * Step 1. calculate field Inject
		 */
		FieldInjectCalculator2 fieldCalculator = new FieldInjectCalculator2(cha, chaCG, class2XMLEle, classfier,
				mtd2RTSeq);
		/**
		 * Step 2. calculate entry
		 */
		EntryCalculator2 entryCalculator = new EntryCalculator2(cha, chaCG, class2XMLEle, classfier);

		/**
		 * Extra: runtime all corresponding configurations
		 */
		HashSet<String> runtimeMarks = calculateAllRuntimeMarks(cha, mtd2RTSeq, class2XMLEle,
				fieldCalculator.getInjectFieldMethods());

		try {
//			Exporter.export2Json(entryCalculator, fieldCalculator);
			Exporter.export2Json(entryCalculator, fieldCalculator, indirectCallCalculator, callSeqCalculator);
			Exporter.export2TXT(runtimeMarks, "run-time-configurations");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		double buildTime = (System.nanoTime() - beforeTime) / 1E9;
		System.out.println("[TIME-LOG] All Solvers Done in " + buildTime + " s!");
	}

	private static HashSet<String> calculateAllRuntimeMarks(ClassHierarchy cha,
			HashMap<String, Set<ArrayList<String>>> mtd2RTSeq, HashMap<String, HashSet<Element>> class2XMLEle,
			HashSet<IMethod> mset) {
		HashSet<String> runtimeMarks = new HashSet<>();

		for (String key : mtd2RTSeq.keySet()) {
			if (!key.equals("outer"))
				addAllLineInfor2RTMarksSet(cha, runtimeMarks, class2XMLEle, key);
			for (ArrayList<String> seq : mtd2RTSeq.get(key)) {
				for (String line : seq) {
					if (line.endsWith("[end]") || line.startsWith("[base class]") || line.startsWith("[base field]")
							|| line.startsWith("[callsite]") || line.startsWith("[returnSite]") || line.equals("outer")
							|| line.startsWith("url started") || line.contains("url finished"))
						continue;

					if (line.startsWith("[field read]") || line.startsWith("[field write]")) {
						// field
						String str1 = line.substring(line.indexOf("[signature]") + "[signature]".length());
						String str2 = str1.substring(0, str1.indexOf(']'));
						String fieldSig = str2.substring(0, str2.lastIndexOf('['));

						IField f = SpecialHelper.genIField(cha, fieldSig);
						if (f != null) {
							HashSet<Annotation> annos = ResolveMarks.resolve_Annotation(f, MarkScope.Field);
							for (Annotation anno : annos) {
								runtimeMarks.add("[field][anno]"
										+ SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
							}
							HashSet<String> xmlEles = ResolveMarks.resolve_XML_String(f, MarkScope.Field, class2XMLEle);
							for (String ele : xmlEles) {
								runtimeMarks.add("[field][xml]" + ele);
							}
						}
					} else {
						addAllLineInfor2RTMarksSet(cha, runtimeMarks, class2XMLEle, line);
					}
				}
			}
		}

		for (IMethod m : mset) {
			HashSet<Annotation> annos_mtd0 = ResolveMarks.resolve_Annotation(m, MarkScope.Method);
			HashSet<String> annos_mtd = new HashSet<>();
			for (Annotation anno : annos_mtd0) {
				runtimeMarks
						.add("[method][anno]" + SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
			}
			HashSet<String> xmlEles_mtd = ResolveMarks.resolve_XML_String(m, MarkScope.Method, class2XMLEle);
			for (String i : xmlEles_mtd) {
				runtimeMarks.add("[method][xml]" + i);
			}
			HashSet<String> inheritance_mtd = new HashSet<>();
			if (ConfigUtil.marksLevel == 0
					|| (ConfigUtil.marksLevel == 1 && annos_mtd.isEmpty() && xmlEles_mtd.isEmpty())) {
				inheritance_mtd = ResolveMarks.resolve_ExtendOrImplement(m, MarkScope.Method, cha);
				for (String i : inheritance_mtd) {
					runtimeMarks.add("[method][inheritance]" + i);
				}
			}
		}
		return runtimeMarks;

	}

	private static void addAllLineInfor2RTMarksSet(ClassHierarchy cha, HashSet<String> runtimeMarks,
			HashMap<String, HashSet<Element>> class2XMLEle, String line) {
		// called method: method and class
		String clazz = "L" + line.substring(0, line.lastIndexOf('.')).replaceAll("\\.", "/");
		IClass givenClass = cha.lookupClass(
				TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
		if (givenClass == null)
			return;

		// class
		HashSet<Annotation> annos_class0 = ResolveMarks.resolve_Annotation(givenClass, MarkScope.Clazz);
		HashSet<String> annos_class = new HashSet<>();
		for (Annotation anno : annos_class0) {
			runtimeMarks.add("[class][anno]" + SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
		}
		HashSet<String> xmlEles_class = ResolveMarks.resolve_XML_String(givenClass, MarkScope.Clazz, class2XMLEle);
		for (String i : xmlEles_class) {
			runtimeMarks.add("[class][xml]" + i);
		}
		HashSet<String> inheritance_class = new HashSet<>();
		if (ConfigUtil.marksLevel == 0
				|| (ConfigUtil.marksLevel == 1 && annos_class.isEmpty() && xmlEles_class.isEmpty())) {
			inheritance_class = ResolveMarks.resolve_ExtendOrImplement(givenClass, MarkScope.Clazz, cha);
			for (String i : inheritance_class) {
				runtimeMarks.add("[class][inheritance]" + SpecialHelper.formatSignature(i));
			}
		}

		// method
		Collection<? extends IMethod> methods = givenClass.getDeclaredMethods();
		IMethod givenMethod = null;
		for (IMethod m : methods) {
			String fullSig = m.getSignature();
			if (fullSig.equals(line)) {
				givenMethod = m;
				break;
			}
		}
		if (givenMethod != null) {
			HashSet<Annotation> annos_mtd0 = ResolveMarks.resolve_Annotation(givenMethod, MarkScope.Method);
			HashSet<String> annos_mtd = new HashSet<>();
			for (Annotation anno : annos_mtd0) {
				runtimeMarks
						.add("[method][anno]" + SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
			}
			HashSet<String> xmlEles_mtd = ResolveMarks.resolve_XML_String(givenMethod, MarkScope.Method, class2XMLEle);
			for (String i : xmlEles_mtd) {
				runtimeMarks.add("[method][xml]" + i);
			}
			HashSet<String> inheritance_mtd = new HashSet<>();
			if (ConfigUtil.marksLevel == 0
					|| (ConfigUtil.marksLevel == 1 && annos_mtd.isEmpty() && xmlEles_mtd.isEmpty())) {
				inheritance_mtd = ResolveMarks.resolve_ExtendOrImplement(givenMethod, MarkScope.Method, cha);
				for (String i : inheritance_mtd) {
					runtimeMarks.add("[method][inheritance]" + i);
				}
			}
		}

	}
}
