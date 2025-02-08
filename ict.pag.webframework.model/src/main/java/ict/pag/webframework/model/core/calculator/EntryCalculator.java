package ict.pag.webframework.model.core.calculator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.dom4j.Element;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.CancelException;

import ict.pag.webframework.model.enumeration.MarkScope;
import ict.pag.webframework.model.log.Callsite2CallSeqMapTool;
import ict.pag.webframework.model.marks.EntryMark;
import ict.pag.webframework.model.marks.MarksHelper;
import ict.pag.webframework.model.marks.ResolveMarks;
import ict.pag.webframework.model.option.ConfigUtil;
import ict.pag.webframework.model.option.SpecialHelper;

public class EntryCalculator {
	private ClassHierarchy cha;
	private CHACallGraph chaCG;
	private HashMap<String, HashSet<Element>> class2xmlEle;
	private HashMap<String, Set<ArrayList<String>>> outer2Seq;

	/* the call statement of entry */
	private HashSet<String> entryCalls;

	/** Answer */
	HashSet<EntryMark> entryMarkSet = new HashSet<>();
	HashSet<EntryMark> notEntryMarkSet = new HashSet<>();

	public EntryCalculator(ClassHierarchy cha0, CHACallGraph chaCG0, HashMap<String, HashSet<Element>> class2xmlEle,
			HashMap<String, Set<ArrayList<String>>> outer2Seq) {
		this.cha = cha0;
		this.chaCG = chaCG0;
		this.class2xmlEle = class2xmlEle;
		this.outer2Seq = outer2Seq;

	}

	public HashSet<EntryMark> getEntryMarkSet() {
		return entryMarkSet;
	}

	public HashSet<EntryMark> getNotEntryMarkSet() {
		return notEntryMarkSet;
	}

	public void calculate(Callsite2CallSeqMapTool tool) {
		// 1. calculate all entry calls
		getEntryCalls(tool);
		// 2. extract all marks
		calculateMarks();

		// 3. calculate not entry marks
		calculateNotEntryMarks(tool);
		
		System.out.println();
	}

	private void calculateMarks() {
		for (String s : entryCalls) {
			String clazz = "L" + s.substring(0, s.lastIndexOf('.')).replaceAll("\\.", "/");
			IClass givenClass = cha.lookupClass(
					TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
			if (givenClass == null)
				continue;

			// 1. class-level
			HashSet<Annotation> annos_class0 = ResolveMarks.resolve_Annotation(givenClass, MarkScope.Clazz);
			HashSet<String> annos_class = new HashSet<>();
			for (Annotation anno : annos_class0) {
				annos_class.add(MarksHelper.resolveAnnotationName(anno));
			}
			HashSet<String> xmlEles_class = ResolveMarks.resolve_XML_String(givenClass, MarkScope.Clazz, class2xmlEle);
			HashSet<String> inheritance_class = new HashSet<>();
			if (ConfigUtil.marksLevel == 0
					|| (ConfigUtil.marksLevel == 1 && annos_class.isEmpty() && xmlEles_class.isEmpty())) {
				inheritance_class = ResolveMarks.resolve_Inheritance(givenClass, MarkScope.Clazz, cha);
			}

			EntryMark mark = null;

			// 2.method-level
			Collection<? extends IMethod> methods = givenClass.getDeclaredMethods();
			IMethod givenMethod = null;
			for (IMethod m : methods) {
				String fullSig = m.getSignature();
				if (fullSig.equals(s)) {
					givenMethod = m;
					break;
				}
			}
			if (givenMethod != null) {
				HashSet<Annotation> annos_mtd0 = ResolveMarks.resolve_Annotation(givenMethod, MarkScope.Method);
				HashSet<String> annos_mtd = new HashSet<>();
				for (Annotation anno : annos_mtd0) {
					annos_mtd.add(MarksHelper.resolveAnnotationName(anno));
				}
				HashSet<String> xmlEles_mtd = ResolveMarks.resolve_XML_String(givenMethod, MarkScope.Method,
						class2xmlEle);
				HashSet<String> inheritance_mtd = new HashSet<>();
				if (ConfigUtil.marksLevel == 0
						|| (ConfigUtil.marksLevel == 1 && annos_mtd.isEmpty() && xmlEles_mtd.isEmpty())) {
					inheritance_mtd = ResolveMarks.resolve_Inheritance(givenMethod, MarkScope.Method, cha);
				}

				// deal with inheritance:
				// if method has override, class inheritance will be removed
				if (!inheritance_class.isEmpty() && !inheritance_mtd.isEmpty()) {
					inheritance_class.clear();
				}
				mark = new EntryMark(annos_class, xmlEles_class, inheritance_class);

//				// deal with inheritance
//				HashSet<String> tmp = new HashSet<>();
//				for (String mtd : inheritance_mtd) {
//					String correctClass = mtd.substring(0, mtd.lastIndexOf('.'));
//					if (inheritance_class.isEmpty()) {
//						// add the inheritance in method-level into class-level
//						String toAdd = SpecialHelper.reformatSignature(correctClass);
//						tmp.add(toAdd.substring(0, toAdd.length() - 1));
//					} else {
//						// reduce the redundant inheritance in class-level
//						for (String c : inheritance_class) {
//							String curr = SpecialHelper.formatSignature(c);
//							if (curr.equals(correctClass)) {
//								tmp.add(c);
//							}
//						}
//					}
//				}
//				if (tmp.isEmpty())
//					mark = new EntryMark(annos_class, xmlEles_class, inheritance_class);
//				else
//					mark = new EntryMark(annos_class, xmlEles_class, tmp);

				mark.setAnnos_mtd(annos_mtd);
				mark.setXmlEles_mtd(xmlEles_mtd);
				mark.setInheritance_mtd(inheritance_mtd);

			} else {
				mark = new EntryMark(annos_class, xmlEles_class, inheritance_class);
			}

			if (mark != null && !mark.isAllEmpty())
				entryMarkSet.add(mark);
		}

	}

	/**
	 * @return full signatures of all methods that framework calls, represent these
	 *         as entry
	 */
	public HashSet<String> getEntryCalls(Callsite2CallSeqMapTool tool) {
		Set<String> notEntries = tool.getCallsite2itsActualCalls().keySet();

		if (entryCalls == null) {
			entryCalls = new HashSet<>();
			if (outer2Seq != null) {
//				for (ArrayList<String> seq : outer2Seq.get("outer1")) {
				for (ArrayList<String> seq : outer2Seq.get("outer")) {
					for (String s : seq) {
						if (!s.endsWith("[end]"))
							if (!notEntries.contains(s))
								entryCalls.add(s);
					}
				}
			}
		}
		return entryCalls;
	}

	private void calculateNotEntryMarks(Callsite2CallSeqMapTool tool) {
		HashMap<String, ArrayList<String>> callsite2calls = tool.getCallsite2itsActualCalls();

		HashSet<String> entryClassSet = new HashSet<>();
		HashSet<IClass> entryClasses = new HashSet<>();
		for (String call : entryCalls) {
			String classStr = call.substring(0, call.lastIndexOf('.'));
			entryClassSet.add(classStr);

			String classStr2 = SpecialHelper.reformatSignature(classStr);
			IClass targetClass = SpecialHelper.getClassFromApplicationCHA(cha,
					classStr2.substring(0, classStr2.length() - 1));
			if (targetClass != null)
				entryClasses.add(targetClass);
		}

		// the marks of methods not entry
		HashSet<IMethod> nonEntries = new HashSet<>();
		for (String s : callsite2calls.keySet()) {
			String clazz = "L" + s.substring(0, s.lastIndexOf('.')).replaceAll("\\.", "/");
			IClass givenClass = cha.lookupClass(
					TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
			if (givenClass == null)
				continue;

			ArrayList<String> tars = callsite2calls.get(s);
			for (String tar : tars) {
				String classStr = tar.substring(0, tar.lastIndexOf('.'));
				String classStr2 = SpecialHelper.reformatSignature(classStr);
				IClass targetClass = SpecialHelper.getClassFromApplicationCHA(cha,
						classStr2.substring(0, classStr2.length() - 1));
				if (targetClass == null)
					continue;

				if (targetClass.equals(givenClass) || cha.isSubclassOf(targetClass, givenClass)) {
					IMethod targetMethod = null;
					for (IMethod m : targetClass.getDeclaredMethods()) {
						String fullSig = m.getSignature();
						if (fullSig.equals(s)) {
							targetMethod = m;
							break;
						}
					}
					if (targetMethod != null) {
						nonEntries.add(targetMethod);
					}
				}
			}
		}

		for (IMethod ele : nonEntries) {
			IClass c = ele.getDeclaringClass();
			String mtdDecClass = SpecialHelper.formatSignature(c.getName().toString());
			// if is equals to entry class or is Super class of entry class, ignore
			if (entryClassSet.contains(mtdDecClass))
				continue;
			boolean goon = true;
			for (IClass c1 : entryClasses) {
				if (cha.isSubclassOf(c1, c)) {
					goon = false;
					break;
				}
			}
			if (!goon)
				continue;

			HashSet<Annotation> annos_mtd0 = ResolveMarks.resolve_Annotation(ele, MarkScope.Method);
			HashSet<String> annos_mtd = new HashSet<>();
			for (Annotation anno : annos_mtd0) {
				annos_mtd.add(MarksHelper.resolveAnnotationName(anno));
			}
			HashSet<String> xmlEles_mtd = ResolveMarks.resolve_XML_String(ele, MarkScope.Method, class2xmlEle);
			HashSet<String> inheritance_mtd = new HashSet<>();
//			if (ConfigUtil.marksLevel == 0
//					|| (ConfigUtil.marksLevel == 1 && annos_mtd.isEmpty() && xmlEles_mtd.isEmpty())) {
//				inheritance_mtd = ResolveMarks.resolve_Inheritance(ele, MarkScope.Method, cha);
//			}

			// deal with inheritance
			HashSet<String> tmp = new HashSet<>();
//			for (String mtd : inheritance_mtd) {
//				String correctClass = mtd.substring(0, mtd.lastIndexOf('.'));
//
//				// add the inheritance in method-level into class-level
//				String toAdd = SpecialHelper.reformatSignature(correctClass);
//				tmp.add(toAdd.substring(0, toAdd.length() - 1));
//			}

			EntryMark mark = new EntryMark(new HashSet<String>(), new HashSet<String>(), tmp);
			mark.setAnnos_mtd(annos_mtd);
			mark.setXmlEles_mtd(xmlEles_mtd);
			mark.setInheritance_mtd(inheritance_mtd);

			if (mark != null && !mark.isAllEmpty())
				notEntryMarkSet.add(mark);
		}

		// 2. the marks of the class not entry
		// and the class not entry class!
		HashSet<IClass> normalClasses = new HashSet<>();
		cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(clazz -> {
			String className = SpecialHelper.formatSignature(clazz.getName().toString());
			if (entryClassSet.contains(className))
				return;

			int declCount = 0;
			int called = 0;
			for (IMethod m : clazz.getDeclaredMethods()) {
				if (m.isInit() || m.isClinit())
					continue;
				try {
					CGNode node = chaCG.findOrCreateNode(m, Everywhere.EVERYWHERE);
					Integer preCount = 0;
					Iterator<CGNode> it = chaCG.getPredNodes(node);
					while (it.hasNext()) {
						CGNode pre = it.next();
						if (pre.getMethod().getDeclaringClass().getClassLoader().getReference()
								.equals(ClassLoaderReference.Application)) {
							preCount++;
						}
					}

					declCount++;
					if (preCount > 0) {
						called++;
					}

				} catch (CancelException e) {
					e.printStackTrace();
				}
			}
			if ((declCount == called) && declCount > 0) {
				normalClasses.add(clazz);
			}
		});

		for (IClass c : normalClasses) {
			HashSet<Annotation> annos_class0 = ResolveMarks.resolve_Annotation(c, MarkScope.Clazz);
			HashSet<String> annos_class = new HashSet<>();
			for (Annotation anno : annos_class0) {
				annos_class.add(MarksHelper.resolveAnnotationName(anno));
			}
			HashSet<String> xmlEles_class = ResolveMarks.resolve_XML_String(c, MarkScope.Clazz, class2xmlEle);
			HashSet<String> inheritance_class = new HashSet<>();
//			if (ConfigUtil.marksLevel == 0
//					|| (ConfigUtil.marksLevel == 1 && annos_class.isEmpty() && xmlEles_class.isEmpty())) {
//				inheritance_class = ResolveMarks.resolve_Inheritance(c, MarkScope.Clazz, cha);
//			}

			EntryMark mark = new EntryMark(annos_class, xmlEles_class, inheritance_class);
			if (mark != null && !mark.isAllEmpty())
				notEntryMarkSet.add(mark);
		}

	}
}
