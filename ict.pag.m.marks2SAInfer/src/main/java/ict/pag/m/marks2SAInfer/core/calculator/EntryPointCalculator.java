package ict.pag.m.marks2SAInfer.core.calculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.ipa.cha.ClassHierarchy;

import ict.pag.m.frameworkInfoUtil.customize.ConfigUtil;
import ict.pag.m.frameworkInfoUtil.customize.Util;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoUnit;
import ict.pag.m.marks2SAInfer.util.resolveMarksUtil;
import ict.pag.m.marks2SAInfer.util.structual.EntrySet;
import ict.pag.m.marks2SAInfer.util.structual.set2setPair;

public class EntryPointCalculator {
	private HashSet<String> entryIgnoreCalls;
	private Set<String> ignoreMarksSet;

	/* all unreachable call sequences, only has call and request start andend */
	private HashMap<Integer, ArrayList<String>> id2group;

	private Set<infoUnit> AnnoSet;

	private Set<infoUnit> inheritanceSet;
//	HashSet<String> frameworkInheritanceRelation;

	// only class information
	private Set<infoUnit> xmlSet;
	// all information
	private Set<infoUnit> xmlSet_all;

	// all entry acual calls
	private HashSet<String> entryCalls = new HashSet<>();

	/** Answers below */
	private EntrySet entries = new EntrySet();

	private HashSet<String> mayEntryPointFormalParameterSet = new HashSet<>();

	public EntryPointCalculator(HashSet<String> entryIgnoreCalls, Set<String> ignoreMarksSet,
			HashMap<Integer, ArrayList<String>> id2group, Set<infoUnit> inheritanceSet, Set<infoUnit> AnnoSet,
			Set<infoUnit> xmlSet, Set<infoUnit> xmlSet_all) {
		this.entryIgnoreCalls = entryIgnoreCalls;
		this.ignoreMarksSet = ignoreMarksSet;
		this.id2group = id2group;

		this.inheritanceSet = inheritanceSet;

		this.AnnoSet = AnnoSet;

		this.xmlSet = xmlSet;
		this.xmlSet_all = xmlSet_all;

		entries.addEntryIgnore(ignoreMarksSet);
		calEntryPoints();
	}

	public EntrySet getEntries() {
		return entries;
	}

	/**
	 * if the method has marks, add method marks</br>
	 * else, add class marks
	 */
	private void calEntryPoints() {
		for (Integer I : id2group.keySet()) {
			ArrayList<String> group = id2group.get(I);

			for (String s : group) {
				if (s.startsWith("url started")) {
					continue;
				}
				if (s.startsWith("url finished")) {
					continue;
				}

				// do not consider this the framework indirectly call as entry
				if (entryIgnoreCalls.contains(s))
					continue;

				// 1. check if has marks in inheritance
				for (String t : resolveMarksUtil.getInheritanceMarks(s, inheritanceSet)) {
					HashSet<String> tmpSet = ConfigUtil.g().getAllFrameworkParentsCalls(t);
					if (!tmpSet.isEmpty()) {
						for (String tmp : tmpSet) {
//							method_set.add("inhe:full:" + tmp);
							Set<String> class_set = new HashSet<String>();
							Set<String> method_set = new HashSet<String>();
							method_set.add("inhe:full:" + tmp);
							set2setPair adding = new set2setPair(class_set, method_set);
							entries.add(adding);
						}
					} else {
//						class_set.add("inhe:class:" + t.substring(0, t.lastIndexOf('.')));
						HashSet<String> tmp_collection = ConfigUtil.g()
								.findAllSubtypesAndImplementors(Util.reformatClass(t.substring(0, t.lastIndexOf('.'))));
						for (String c : tmp_collection) {
							Set<String> class_set = new HashSet<String>();
							Set<String> method_set = new HashSet<String>();
							class_set.add("inhe:class:" + Util.format(c));
							set2setPair adding = new set2setPair(class_set, method_set);
							entries.add(adding);
						}
					}
				}

				Set<String> class_set = new HashSet<String>();
				Set<String> method_set = new HashSet<String>();

				// 2. check if has marks in annotations
				ArrayList<String> alls = resolveMarksUtil.getAnnosMarksWithSplit(s, AnnoSet);
				// <classmarks, methodmarks>
				ArrayList<String> tmp_m = new ArrayList<>();
				ArrayList<String> tmp_c = new ArrayList<>();
				boolean ism = true;
				for (String anno : alls) {
					if (anno.equals("*")) {
						ism = false;
						continue;
					}
					if (ism) {
						tmp_m.add("anno:mtd:" + anno);
					} else {
						tmp_c.add("anno:class:" + anno);
					}
				}

				// need to add
				if (!tmp_m.isEmpty()) {
					class_set.addAll(tmp_c);
					method_set.addAll(tmp_m);
				} else if (!tmp_c.isEmpty()) {
					class_set.addAll(tmp_c);
				}

				// 3. check if unreachable methods has marks in xml configuration
				ArrayList<String> xml_alls = resolveMarksUtil.getXMLMarks_mthd(s, xmlSet);
				for (String ele : xml_alls) {
					if (ele.startsWith("xml:class:"))
						class_set.add(ele);
					else if (ele.startsWith("xml:mtd:"))
						method_set.add(ele);
				}

				set2setPair adding = new set2setPair(class_set, method_set);
				entries.add(adding);
				entryCalls.add(s);

				/** add formal parameter type */
				HashSet<String> paramsTypes = new HashSet<>();
				if (s.indexOf('(') + 1 != s.indexOf(')')) {
					// if has parameter
					String paramsString = s.substring(s.indexOf('(') + 1, s.indexOf(')'));
					if (paramsString.contains(";")) {
						// split parameters

						for (int i = 0; i < paramsString.length();) {
							char c = paramsString.charAt(i);
							switch (c) {
							case 'Z':
							case 'B':
							case 'C':
							case 'S':
							case 'I':
							case 'J':
							case 'F':
							case 'D':
								i++;
								break;
							case 'L':
								String sub0 = paramsString.substring(i);
								String param1 = sub0.substring(0, sub0.indexOf(';'));
								paramsTypes.add(param1);
								i = i + sub0.indexOf(';') + 1;
								break;
							default:
								i++;
							}
						}

					}
				}
				/* add framework types */
				for (String type : paramsTypes) {
					if (ConfigUtil.isFrameworkMarks(type)) {
						// add the framework type and its superClass and all childClass/implementors
						HashSet<String> tmp = ConfigUtil.g().findAllSubtypesAndImplementors(type);
						mayEntryPointFormalParameterSet.addAll(tmp);
					}
				}

			}
		}

		entries.removeCalculatedIgnores();
		entries.duplicateRemove();
	}

	public HashSet<String> getEntryCalls() {
		return entryCalls;
	}

	public HashSet<String> getMayEntryPointFormalParameterSet() {
		return mayEntryPointFormalParameterSet;
	}

}
