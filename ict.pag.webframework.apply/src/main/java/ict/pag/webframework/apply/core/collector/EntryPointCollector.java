package ict.pag.webframework.apply.core.collector;

import java.util.HashMap;
import java.util.HashSet;

import org.dom4j.Element;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;

import ict.pag.webframework.apply.core.FrameworkSpecification;
import ict.pag.webframework.model.enumeration.MarkScope;
import ict.pag.webframework.model.marks.EntryMark;
import ict.pag.webframework.model.marks.MarksHelper;
import ict.pag.webframework.model.marks.ResolveMarks;
import ict.pag.webframework.model.option.SpecialHelper;

public class EntryPointCollector {
	private static int policy = 0;

	public static HashSet<String> collect(ClassHierarchy cha, HashMap<String, HashSet<Element>> class2XMLElement,
			FrameworkSpecification frmkSpecification) {
		HashSet<EntryMark> entryMarksSet = frmkSpecification.getEntryMarkSet();
		HashSet<String> mayEntryParamSet = frmkSpecification.getMayEntryPointFormalParameterSet();

		/* internal answers */
		HashSet<IClass> entryClass = new HashSet<>();
		HashSet<IClass> mayEntryClass = new HashSet<>();
		HashSet<IMethod> entryMethod = new HashSet<>();

		cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(clazz -> {
			// 1. class-level marks
			HashSet<Annotation> annos_class0 = ResolveMarks.resolve_Annotation(clazz, MarkScope.Clazz);
			HashSet<String> annos_class = new HashSet<>();
			for (Annotation anno : annos_class0) {
				annos_class.add(MarksHelper.resolveAnnotationName(anno));
			}
			HashSet<String> xmlEles_class = ResolveMarks.resolve_XML_String(clazz, MarkScope.Clazz, class2XMLElement);
			HashSet<String> inheritance_class = ResolveMarks.resolve_Inheritance(clazz, MarkScope.Clazz, cha);

			for (EntryMark mark : entryMarksSet) {
				HashSet<String> classMarks = mark.getAllMarks_class();
				HashSet<String> methodMarks = mark.getAllMarks_methods();

				// whether class match
				boolean allHave = true;
				if (!classMarks.isEmpty()) {
					for (String cm : classMarks) {
						String prifix = cm.substring(1, cm.indexOf(']'));
						String value = cm.substring(cm.indexOf(']') + 1);
						switch (prifix) {
						case "anno":
							if (!annos_class.contains(value))
								allHave = false;
							break;
						case "xml":
							if (!xmlEles_class.contains(value))
								allHave = false;
							break;
						case "inheritance":
							if (!inheritance_class.contains(value))
								allHave = false;
							break;
						}
					}
				}

				if (allHave) {
					if (methodMarks.isEmpty()) {
						// find entry class!
						entryClass.add(clazz);
					} else {
						// whether method match
						for (IMethod m : clazz.getDeclaredMethods()) {
							HashSet<Annotation> annos_mtd0 = ResolveMarks.resolve_Annotation(m, MarkScope.Method);
							HashSet<String> annos_mtd = new HashSet<>();
							for (Annotation anno : annos_mtd0) {
								annos_mtd.add(MarksHelper.resolveAnnotationName(anno));
							}
							HashSet<String> xmlEles_mtd = ResolveMarks.resolve_XML_String(m, MarkScope.Method,
									class2XMLElement);
							HashSet<String> inheritance_mtd = ResolveMarks.resolve_Inheritance(m, MarkScope.Method,
									cha);

							boolean satisfyAll = true;
							for (String mm : methodMarks) {
								String prifix = mm.substring(1, mm.indexOf(']'));
								String value = mm.substring(mm.indexOf(']') + 1);
								switch (prifix) {
								case "anno":
									if (!annos_mtd.contains(value))
										satisfyAll = false;
									break;
								case "xml":
									if (!xmlEles_mtd.contains(value))
										satisfyAll = false;
									break;
								case "inheritance":
									if (!inheritance_mtd.contains(value))
										satisfyAll = false;
									break;
								}
							}
							if (satisfyAll) {
								// find entry methods!
								entryMethod.add(m);
							}

							// param
							int i = 0;
							if (m.isStatic()) {
								i = 1;
							}
							if (m.getNumberOfParameters() > i) {
								for (i = i + 1; i < m.getNumberOfParameters(); i++) {
									TypeReference typeRef = m.getParameterType(i);
									if (typeRef.isReferenceType()) {
										if (mayEntryParamSet
												.contains(SpecialHelper.formatSignature(typeRef.getName().toString())))
											mayEntryClass.add(clazz);
									}
								}
							}
						}
					}
				}
			}
		});

		/* add all entry method signatures */
		HashSet<String> ret = new HashSet<>();
		// add
		switch (policy) {
		case 0:
			// add all declaring methods of possible Entry Class
			for (IClass c : mayEntryClass) {
				for (IMethod m : c.getDeclaredMethods()) {
					ret.add(m.getSignature());
				}
			}
		case 1:
			// add all declaring methods of entry class
			for (IClass c : entryClass) {
				for (IMethod m : c.getDeclaredMethods()) {
					ret.add(m.getSignature());
				}
			}
		case 2:
			// add entryMethod
			for (IMethod m : entryMethod) {
				ret.add(m.getSignature());
			}
			break;
		}

		return ret;
	}
}
