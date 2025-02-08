package ict.pag.m.marks2SAInfer.dynamicInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import ict.pag.m.frameworkInfoUtil.customize.ConfigUtil;
import ict.pag.m.frameworkInfoUtil.customize.Util;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoUnit;
import ict.pag.m.marks2SAInfer.util.reducedSetCollection;
import ict.pag.m.marks2SAInfer.util.resolveMarksUtil;

public class ManagedClass {
	private HashMap<Integer, ArrayList<String>> id2group;

	private Set<infoUnit> anno_clazz_marks;
	private Set<infoUnit> xmls_marks_class;
	private Set<infoUnit> inheritance_marks;

	private reducedSetCollection managedClassMarks = new reducedSetCollection();

	public ManagedClass(HashMap<Integer, ArrayList<String>> id2group_unreachable, Set<infoUnit> c_marks,
			Set<infoUnit> xml_marks, Set<infoUnit> inhe) {
		this.id2group = id2group_unreachable;

		// annotation
		this.anno_clazz_marks = c_marks;
		// xml
		this.xmls_marks_class = xml_marks;
		// inheritance
		this.inheritance_marks = inhe;

		calculate();
	}

	/** if the class haven't been inited in application but been called in actual */
	HashSet<String> actualRoots = new HashSet<>();

	private void calculate() {

		for (Integer id : id2group.keySet()) {
			ArrayList<String> seq = id2group.get(id);
			for (String line : seq) {
				if (line.endsWith("[end]") || line.startsWith("url started") || line.equals("url finished"))
					continue;

				if (!actualRoots.contains(line)) {
					/* classes */
					String clazzName = line.substring(0, line.lastIndexOf('.'));
					addIntoManagedClassMarks(clazzName);
					/* return */
					String ret = line.substring(line.lastIndexOf(')') + 1);
					if (ret.startsWith("L") && ret.endsWith(";")) {
						ret = Util.format(ret);
						addIntoManagedClassMarks(ret);
					}

					actualRoots.add(line);
				}
			}

		}

	}

	public void addIntoManagedClassMarks(String clazzName) {
		// annotations
		ArrayList<String> need2add_anno = resolveMarksUtil.getClassAnnos_withDecorate(clazzName, anno_clazz_marks,
				"class");
//		if (!need2add_anno.isEmpty()) {
//			managedClassMarks.add(new HashSet<>(need2add_anno));
//		}
		// xml configured
		ArrayList<String> need2add_xml = resolveMarksUtil.getXMLMarks_withDecorate(clazzName, xmls_marks_class);
//		if (!need2add_xml.isEmpty()) {
//			managedClassMarks.add(new HashSet<>(need2add_xml));
//		}
		HashSet<String> annoAndXmls = new HashSet<>();
		if (!need2add_anno.isEmpty()) {
			annoAndXmls.addAll(need2add_anno);
		}
		if (!need2add_xml.isEmpty()) {
			annoAndXmls.addAll(need2add_xml);
		}
		if (!annoAndXmls.isEmpty())
			managedClassMarks.add(annoAndXmls);

		// inheritance configured
		ArrayList<String> need2add_inhe = resolveMarksUtil.getInheritanceMarks_onclass(clazzName, inheritance_marks);
		HashSet<String> singletmp = new HashSet<String>();
		for (String i : need2add_inhe) {

			singletmp.add(i);

			String i2 = Util.reformatClass(i.substring("inhe:class:".length()));
			HashSet<String> collection = ConfigUtil.g()
					.findAllSubtypesAndImplementors(i2.substring(0, i2.length() - 1));
			for (String c : collection) {
				String t = "inhe:class:" + Util.format(c);
				singletmp.add(t);
			}
		}
		for (String i : singletmp) {
			HashSet<String> tmp = new HashSet<>();
			tmp.add(i);
			managedClassMarks.add(tmp);
		}
	}

	public reducedSetCollection getManagedClassMarks() {
		return managedClassMarks;
	}

}
