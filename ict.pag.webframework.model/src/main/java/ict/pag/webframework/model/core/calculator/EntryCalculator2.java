package ict.pag.webframework.model.core.calculator;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

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
import ict.pag.webframework.model.logprase.RTInfoDetailsClassifer;
import ict.pag.webframework.model.marks.EntryMark;
import ict.pag.webframework.model.marks.MarksHelper;
import ict.pag.webframework.model.marks.ResolveMarks;
import ict.pag.webframework.model.option.ConfigUtil;
import ict.pag.webframework.model.option.SpecialHelper;

public class EntryCalculator2 {

	private ClassHierarchy cha;
	private CHACallGraph chaCG;

	private HashMap<String, HashSet<Element>> class2xmlEle;
	private RTInfoDetailsClassifer classfier;

	/** Answer */
	HashSet<EntryMark> entryMarkSet2;
	HashSet<EntryMark> notEntryMarkSet2;

	public EntryCalculator2(ClassHierarchy cha, CHACallGraph chaCG, HashMap<String, HashSet<Element>> class2xmlEle,
			RTInfoDetailsClassifer classfier) {
		this.cha = cha;
		this.chaCG = chaCG;
		this.class2xmlEle = class2xmlEle;
		this.classfier = classfier;

		calculate();
	}

	public HashSet<EntryMark> getEntryMarkSet() {
		return entryMarkSet2;
	}

	public HashSet<EntryMark> getNotEntryMarkSet() {
		return notEntryMarkSet2;
	}

	private void calculate() {
		/* 1. calculate configurations of entries */
		entryMarkSet2 = extractEntryCallsConfigs();

		/* 2. calculate not entries */
		notEntryMarkSet2 = extractNotEntryCallsConfigs();
	}

	private HashSet<EntryMark> extractEntryCallsConfigs() {

		HashSet<String> entryCalls = new HashSet<>();
//		HashSet<String> notEntries = classfier.getHasMatchedCallsiteCalls();

		for (String call : classfier.getOuterCalls()) {
//			if (!notEntries.contains(call))
//			if (call.contains("prePersist"))
//				System.out.println();
			entryCalls.add(call);
		}

		/* 0224. add the call that has no matched callsite as entry calls */
		for (String call : classfier.getNoMatchedCallsiteCalls()) {
//			if (!notEntries.contains(call))
//			if (call.contains("prePersist"))
//				System.out.println();
			entryCalls.add(call);
		}

		HashSet<EntryMark> ret = calculateEntryMarks(entryCalls);

		return ret;
	}

	private HashSet<EntryMark> extractNotEntryCallsConfigs() {

//		HashSet<String> notEntries = classfier.getHasMatchedCallsiteCalls();
		HashSet<String> notEntries = classfier.getHasMatchedCallsiteCalls_NotInSameClass();
		HashSet<EntryMark> ret = calculateNotEntryMarks(notEntries);

		return ret;
	}

	private EntryMark genEntryMark(IClass givenClass) {
		HashSet<Annotation> annos_class0 = ResolveMarks.resolve_Annotation(givenClass, MarkScope.Clazz);
		HashSet<String> annos_class = new HashSet<>();
		for (Annotation anno : annos_class0) {
			annos_class.add(SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
		}
		HashSet<String> xmlEles_class = ResolveMarks.resolve_XML_String(givenClass, MarkScope.Clazz, class2xmlEle);
		HashSet<String> inheritance_class = new HashSet<>();
		if (ConfigUtil.marksLevel == 0
				|| (ConfigUtil.marksLevel == 1 && annos_class.isEmpty() && xmlEles_class.isEmpty())) {
			for (String i : ResolveMarks.resolve_ExtendOrImplement(givenClass, MarkScope.Clazz, cha)) {
				inheritance_class.add(SpecialHelper.formatSignature(i));
			}
		}

		return new EntryMark(annos_class, xmlEles_class, inheritance_class);
	}

	private EntryMark addMethodInfo2EntryMark(EntryMark curr, IMethod givenMethod) {
		HashSet<Annotation> annos_mtd0 = ResolveMarks.resolve_Annotation(givenMethod, MarkScope.Method);
		HashSet<String> annos_mtd = new HashSet<>();
		for (Annotation anno : annos_mtd0) {
			annos_mtd.add(SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
		}
		HashSet<String> xmlEles_mtd = ResolveMarks.resolve_XML_String(givenMethod, MarkScope.Method, class2xmlEle);
		HashSet<String> inheritance_mtd = new HashSet<>();
		if (ConfigUtil.marksLevel == 0
				|| (ConfigUtil.marksLevel == 1 && annos_mtd.isEmpty() && xmlEles_mtd.isEmpty())) {
			inheritance_mtd = ResolveMarks.resolve_ExtendOrImplement(givenMethod, MarkScope.Method, cha);
		}

		if (curr == null) {
			EntryMark mark = new EntryMark(new HashSet<String>(), new HashSet<String>(), new HashSet<String>());

			mark.setAnnos_mtd(annos_mtd);
			mark.setXmlEles_mtd(xmlEles_mtd);
			mark.setInheritance_mtd(inheritance_mtd);

			return mark;

		} else {
			HashSet<String> inheritance_class = curr.getInheritance_class();

			// deal with inheritance on class:
			if (!inheritance_class.isEmpty() && !inheritance_mtd.isEmpty()) {
				/* policy 1: */
//				// if method has override, class inheritance will be removed
//				curr = new EntryMark(curr.getAnnos_class(), curr.getXmlEles_class(), new HashSet<String>());
				/* policy 2: filter the class inheritance using method inheritance */
				// deal with inheritance
				HashSet<String> tmp = new HashSet<>();
				for (String mtd : inheritance_mtd) {
					if (mtd.equals("main([Ljava/lang/String;)V[MAIN]"))
						continue;
					String correctClass = mtd.substring(0, mtd.lastIndexOf('.'));
					// reduce the redundant inheritance in class-level
					for (String c : inheritance_class) {
						String c_inhe = c.substring(0, c.lastIndexOf('['));
						if (c_inhe.equals(correctClass)) {
							tmp.add(c);
						}
					}
				}
				curr = new EntryMark(curr.getAnnos_class(), curr.getXmlEles_class(), tmp);
			}

			curr.setAnnos_mtd(annos_mtd);
			curr.setXmlEles_mtd(xmlEles_mtd);
			curr.setInheritance_mtd(inheritance_mtd);

			return curr;
		}

	}

	private HashSet<IClass> entryClassSet = new HashSet<>();

	private HashSet<EntryMark> calculateEntryMarks(HashSet<String> entryCalls) {
		HashSet<EntryMark> ret = new HashSet<>();

		for (String s : entryCalls) {
			String clazz = "L" + s.substring(0, s.lastIndexOf('.')).replaceAll("\\.", "/");
			IClass givenClass = cha.lookupClass(
					TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
			if (givenClass == null)
				continue;

//			if (s.contains("prePersist"))
//				System.out.println();
			// 1. class-level
			EntryMark mark = genEntryMark(givenClass);

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

			if (givenMethod == null)
				continue;
			/*
			 * 0305: entry methods not init methods
			 */
			if (givenMethod.isInit() || givenMethod.isClinit())
				continue;

			mark = addMethodInfo2EntryMark(mark, givenMethod);

			if (mark != null && !mark.isAllEmpty()) {
				/* 0305: the entry determined by both class and method marks */
				if (!mark.getAllMarks_methods().isEmpty()) {
					/* 1. this method is entry call, the method has marks */
					add2EntryClass(s.substring(0, s.lastIndexOf('.')));
					ret.add(mark);
				} else {
//					/* 2. there is no marks on method */
//					/* if all the declared methods of this class have no 'outer' caller */
//					for (IMethod m : methods) {
//						if (m.isInit() || m.isClinit())
//							continue;
//
//						boolean hasOuterCaller = false;
//						try {
//							CGNode cgNode = chaCG.findOrCreateNode(m, Everywhere.EVERYWHERE);
//							Iterator<CGNode> it = chaCG.getPredNodes(cgNode);
//							while (it.hasNext()) {
//								CGNode pre = it.next();
//								if (pre.getMethod().getDeclaringClass().getClassLoader().getReference()
//										.equals(ClassLoaderReference.Application)) {
//									IClass preClass = pre.getMethod().getDeclaringClass();
//									if ((preClass != givenClass) && (!cha.isAssignableFrom(preClass, givenClass))
//											&& (!cha.isAssignableFrom(givenClass, preClass))
//											&& (!cha.implementsInterface(preClass, givenClass))
//											&& (!cha.implementsInterface(givenClass, preClass))) {
//										hasOuterCaller = true;
//										break;
//									}
//								}
//							}
//
//						} catch (CancelException e) {
//							e.printStackTrace();
//						}
//
//						if (!hasOuterCaller) {
//							add2EntryClass(s.substring(0, s.lastIndexOf('.')));
//							ret.add(mark);
//						}
//
//					}
				}
			}

		}
		return ret;
	}

	private HashSet<EntryMark> calculateNotEntryMarks(HashSet<String> notEntries) {
		HashSet<EntryMark> ret = new HashSet<>();

//		HashSet<String> entryClassSet = new HashSet<>();
		// 1. not entry class; 2. class of callee
		HashSet<IClass> normalClasses = new HashSet<>();

//		for (String call : classfier.getOuterCalls()) {
//			String classStr = call.substring(0, call.lastIndexOf('.'));
//			entryClassSet.add(classStr);
//		}

		/* 1. called method */
		for (String s : notEntries) {
			String classStr = s.substring(0, s.lastIndexOf('.'));
			String clazz = "L" + classStr.replaceAll("\\.", "/");
			IClass givenClass = cha.lookupClass(
					TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
			if (givenClass == null)
				continue;

			// 0314:class-level
			EntryMark mark = genEntryMark(givenClass);

			// method-level
			Collection<? extends IMethod> methods = givenClass.getDeclaredMethods();
			IMethod givenMethod = null;
			for (IMethod m : methods) {
				String fullSig = m.getSignature();
				if (fullSig.equals(s)) {
					givenMethod = m;
					break;
				}
			}

			// 0314: add not entry method calls
//			EntryMark mark = null;
			if (givenMethod != null) {
				/* 0315: if is static method do not care about the class */
				if (givenMethod.isStatic())
					mark = addMethodInfo2EntryMark(null, givenMethod);
				else
					mark = addMethodInfo2EntryMark(mark, givenMethod);
			}

			/* add in 0306: if only inheritance mark, do not care */
			if (mark != null && !mark.isAllEmpty()) {
				if (mark.getInheritance_mtd() != null && !mark.getInheritance_mtd().isEmpty()) {
					if (!((mark.getAnnos_mtd() == null || mark.getAnnos_mtd().isEmpty())
							&& (mark.getXmlEles_mtd() == null || mark.getXmlEles_mtd().isEmpty()))) {
						ret.add(mark);
					}
				} else {
					ret.add(mark);
				}
			}

			// 0314:
//			// class
//			if (!isEntryClass(givenClass)) {
//				if (givenMethod != null && !givenMethod.isStatic())
//					normalClasses.add(givenClass);
//			}

		}

		// 0314:
//		/* class mark */
//		for (IClass c : normalClasses) {
//			EntryMark mark = genEntryMark(c);
//			if (mark != null && !mark.isAllEmpty())
//				ret.add(mark);
//		}

		return ret;
	}

	public void addInitializer(HashSet<IMethod> injectFieldMethods) {
		for (IMethod m : injectFieldMethods) {
			EntryMark mark = addMethodInfo2EntryMark(null, m);
			if (!mark.isAllEmpty())
				entryMarkSet2.add(mark);
		}

	}

	private boolean isEntryClass(IClass givenClass) {
		if (entryClassSet.contains(givenClass))
			return true;

		// is sub-class of entry class
		for (IClass ic : entryClassSet) {
			if (cha.isSubclassOf(givenClass, ic) || cha.isAssignableFrom(ic, givenClass)
					|| cha.implementsInterface(givenClass, ic))
				return true;
		}

		return false;
	}

	private void add2EntryClass(String s) {
		String clazz = "L" + s.replaceAll("\\.", "/");
		IClass givenClass = cha.lookupClass(
				TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
		if (givenClass == null)
			return;

		// all super classes
		HashSet<IClass> superClassSet = new HashSet<>();
		findAllSuperClass(givenClass, superClassSet);

		entryClassSet.add(givenClass);
		if (!superClassSet.isEmpty())
			entryClassSet.addAll(superClassSet);

	}

	private void findAllSuperClass(IClass clazz, HashSet<IClass> superClassSet) {

		IClass superClazz = clazz.getSuperclass();
		if (superClazz != null) {
			if (superClazz.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
				superClassSet.add(superClazz);
				findAllSuperClass(superClazz, superClassSet);
			}
		}

		Collection<? extends IClass> interfaces = clazz.getDirectInterfaces();
		for (IClass i : interfaces) {
			if (i != null)
				if (i.getClassLoader().getReference().equals(ClassLoaderReference.Application))
					superClassSet.add(i);
		}
	}

}
