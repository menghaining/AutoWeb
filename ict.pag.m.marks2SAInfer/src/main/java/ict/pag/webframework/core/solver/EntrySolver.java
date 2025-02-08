package ict.pag.webframework.core.solver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.dom4j.Element;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;

import ict.pag.webframework.enumeration.MarkScope;
import ict.pag.webframework.marks.EntryMark;
import ict.pag.webframework.marks.MarksHelper;
import ict.pag.webframework.marks.ResolveMarks;
import ict.pag.webframework.option.ConfigUtil;

public class EntrySolver {
	private ClassHierarchy cha;
	private HashMap<String, HashSet<Element>> class2xmlEle;
	private Set<String> unreachableRoots;

	private HashSet<String> ignore_class = new HashSet<>();
	private HashSet<String> ignore_mtd = new HashSet<>();

	/** Answer */
	HashSet<EntryMark> entryMarkSet = new HashSet<>();

	public EntrySolver(ClassHierarchy cha0, HashMap<String, HashSet<Element>> class2xmlEle,
			Set<String> unreachableRoots0) {
		this.cha = cha0;
		this.class2xmlEle = class2xmlEle;
		this.unreachableRoots = unreachableRoots0;
	}

	public HashSet<EntryMark> getEntryMarkSet() {
		return entryMarkSet;
	}

	/**
	 * @param the id2group is only reachable as default
	 */
	public void calculateEntry(HashMap<Integer, ArrayList<String>> id2group) {
		long beforeTime = System.nanoTime();

		for (Integer I : id2group.keySet()) {
			ArrayList<String> group = id2group.get(I);

			for (String s : group) {
				if (s.startsWith("url started")) {
					continue;
				}
				if (s.startsWith("url finished")) {
					continue;
				}

				String clazz = "L" + s.substring(0, s.lastIndexOf('.')).replaceAll("\\.", "/");
				IClass givenClass = cha.lookupClass(
						TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
				if (givenClass == null)
					continue;

				// class-level
				HashSet<Annotation> annos_class0 = ResolveMarks.resolve_Annotation(givenClass, MarkScope.Clazz);
				HashSet<String> annos_class = new HashSet<>();
				for (Annotation anno : annos_class0) {
					annos_class.add(MarksHelper.resolveAnnotationName(anno));
				}
				HashSet<String> xmlEles_class = ResolveMarks.resolve_XML_String(givenClass, MarkScope.Clazz,
						class2xmlEle);
				HashSet<String> inheritance_class = ResolveMarks.resolve_Inheritance(givenClass, MarkScope.Clazz, cha);

				EntryMark mark = new EntryMark(annos_class, xmlEles_class, inheritance_class);

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
				if (givenMethod != null) {
					HashSet<Annotation> annos_mtd0 = ResolveMarks.resolve_Annotation(givenMethod, MarkScope.Method);
					HashSet<String> annos_mtd = new HashSet<>();
					for (Annotation anno : annos_mtd0) {
						annos_mtd.add(MarksHelper.resolveAnnotationName(anno));
					}
					HashSet<String> xmlEles_mtd = ResolveMarks.resolve_XML_String(givenMethod, MarkScope.Method,
							class2xmlEle);
					HashSet<String> inheritance_mtd = ResolveMarks.resolve_Inheritance(givenMethod, MarkScope.Method,
							cha);
					mark.setAnnos_mtd(annos_mtd);
					mark.setInheritance_mtd(inheritance_mtd);
					mark.setXmlEles_mtd(xmlEles_mtd);
				}

				if (!mark.isAllEmpty())
					entryMarkSet.add(mark);

			}
		}

		// optimize answers
		merges();
		RemoveIgnores();
		RemoveDuplicated();
		double buildTime = (System.nanoTime() - beforeTime) / 1E9;
		System.out.println("[TIME-LOG] Entry Sovler Done in " + buildTime + " s!");
	}

	private void merges() {
		/// reduce to minimal collection and merge
		HashSet<EntryMark> entryMarkSet_new = new HashSet<>();

		Iterator<EntryMark> it0 = entryMarkSet.iterator();
		while (it0.hasNext()) {
			EntryMark mark = it0.next();
			if (entryMarkSet_new.isEmpty()) {
				entryMarkSet_new.add(mark);
				continue;
			}

			Iterator<EntryMark> it = entryMarkSet_new.iterator();
			while (it.hasNext()) {
				EntryMark curr = it.next();
				mark.mergeMarks(curr, ignore_class, ignore_mtd);
			}
			entryMarkSet_new.add(mark);
		}

		entryMarkSet = entryMarkSet_new;

	}

	private void RemoveDuplicated() {
		HashSet<EntryMark> entryMarkSet_new = new HashSet<>();
		for (EntryMark mark : entryMarkSet) {
			if (entryMarkSet_new.isEmpty()) {
				entryMarkSet_new.add(mark);
				continue;
			}

			boolean has = false;
			for (EntryMark curr : entryMarkSet_new) {
				if (curr.isSame(mark)) {
					has = true;
					break;
				}
			}
			if (!has) {
				entryMarkSet_new.add(mark);
			}
		}

		entryMarkSet = entryMarkSet_new;
	}

	private void RemoveIgnores() {
		// 1. calculate ignore marks
		Iterator<IClass> it = cha.getLoader(ClassLoaderReference.Application).iterateAllClasses();

		while (it.hasNext()) {
			IClass cn = it.next();
			if (cn.isInterface() || cn.isAbstract())
				continue;
			IClass superClazz = cn.getSuperclass();
			if (superClazz != null && ConfigUtil.isApplicationClass(superClazz.getName().toString()))
				continue;

			boolean isAllCalled = true;
			for (IMethod m : cn.getDeclaredMethods()) {
				String name = m.getName().toString();

				if (!name.equals("<init>") && !name.equals("<cinit>")) {
					if (unreachableRoots.contains(m.getSignature())) {
						isAllCalled = false;
					} else {
						HashSet<Annotation> annos_mtd0 = ResolveMarks.resolve_Annotation(m, MarkScope.Method);
						for (Annotation anno : annos_mtd0) {
							ignore_mtd.add("[anno]" + MarksHelper.resolveAnnotationName(anno));
						}
						HashSet<String> xmlEles_mtd = ResolveMarks.resolve_XML_String(m, MarkScope.Method,
								class2xmlEle);
						for (String ele : xmlEles_mtd) {
							ignore_mtd.add("[xml]" + ele);
						}
						HashSet<String> inheritance_mtd = ResolveMarks.resolve_Inheritance(m, MarkScope.Method, cha);
						for (String ele : inheritance_mtd) {
							ignore_mtd.add("[inheritance]" + ele);
						}
					}
				}

			}
			if (isAllCalled) {
				HashSet<Annotation> annos_class0 = ResolveMarks.resolve_Annotation(cn, MarkScope.Clazz);
				for (Annotation anno : annos_class0) {
					ignore_class.add("[anno]" + MarksHelper.resolveAnnotationName(anno));
				}
				HashSet<String> xmlEles_class = ResolveMarks.resolve_XML_String(cn, MarkScope.Clazz, class2xmlEle);
				for (String ele : xmlEles_class) {
					ignore_class.add("[xml]" + ele);
				}
				HashSet<String> inheritance_class = ResolveMarks.resolve_Inheritance(cn, MarkScope.Clazz, cha);
				for (String ele : inheritance_class) {
					ignore_class.add("[inheritance]" + ele);
				}
			}

		}

		// 2. remove ignores
		if (!ignore_class.isEmpty() || !ignore_mtd.isEmpty()) {
			Iterator<EntryMark> it0 = entryMarkSet.iterator();
			while (it0.hasNext()) {
				EntryMark mark = it0.next();
				if (mark.isAllEmpty()) {
					it0.remove();
				} else {
					if (!ignore_class.isEmpty()) {
						HashSet<String> allmarks_classes = mark.getAllMarks_class();
						for (String ignore : ignore_class) {
							if (allmarks_classes.contains(ignore))
								allmarks_classes.remove(ignore);
						}
					}
					if (!ignore_mtd.isEmpty()) {
						HashSet<String> allmarks_mtds = mark.getAllMarks_methods();
						for (String ignore : ignore_mtd) {
							if (allmarks_mtds.contains(ignore))
								allmarks_mtds.remove(ignore);
						}
					}
				}
			}
		}

	}

}
