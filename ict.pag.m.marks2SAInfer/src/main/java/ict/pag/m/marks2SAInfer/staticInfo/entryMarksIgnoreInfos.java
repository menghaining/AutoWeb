package ict.pag.m.marks2SAInfer.staticInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.annotations.Annotation;

import ict.pag.m.frameworkInfoUtil.customize.GraphBuilder;
import ict.pag.m.frameworkInfoUtil.customize.Util;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoPair;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoUnit;
import ict.pag.m.marks2SAInfer.util.resolveMarksUtil;

public class entryMarksIgnoreInfos {

	private GraphBuilder builder;
	private ClassHierarchy cha;

	private Set<String> allMarks = new HashSet<>();
	private Set<String> mostMarks = new HashSet<>();

	private int k = 2;

	/**
	 * calculate the marks that may not represent entry
	 */
	public entryMarksIgnoreInfos(GraphBuilder builder2, Set<infoUnit> AnnoSet, Set<infoUnit> inheritanceSet,
			Set<infoUnit> xmlSet) {

		builder = builder2;
		cha = builder.getCHA();

		calculateAllInfos(AnnoSet, inheritanceSet, xmlSet);

	}

	/**
	 * add all types of marks, including annotations, inheritance, xmls</br>
	 * TODO:support regular expression now
	 */
	private void calculateAllInfos(Set<infoUnit> annoSet, Set<infoUnit> inheritanceSet, Set<infoUnit> xmlSet) {
		Set<String> entriesSigs = builder.getAllUnreachableEntryPoints();

		Set<String> unreachableMtdMarksSet = new HashSet<>();
		cha.forEach(cn -> {
			if (cn.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
				if (cn.isInterface() || cn.isAbstract())
					return;

			}
		});

		ArrayList<String> normalMarksList = new ArrayList<>();

		cha.forEach(cn -> {
			if (cn.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
				if (cn.isInterface() || cn.isAbstract())
					return;

				boolean isAllCalled = true;
				/** 1. method level */
				ArrayList<String> normalMarksList0 = new ArrayList<>();
				int normal_mthd_count = 0;
				for (IMethod m : cn.getDeclaredMethods()) {
					String name = m.getName().toString();

					if (!name.equals("<init>")) {
						normal_mthd_count++;
						if (entriesSigs.contains(m.getSignature())) {
							isAllCalled = false;
						}
						/** 1.1 add called method annotations */
						Collection<Annotation> annos = m.getAnnotations();
						if (annos != null) {
							for (Annotation anno : annos) {
								String m_mark = "anno:mtd:" + Util.format(anno.getType().getName().toString());
								if (entriesSigs.contains(m.getSignature())) {
									unreachableMtdMarksSet.add(m_mark);
									/**
									 * if unreachable root also has this annotation, this annotation cannot be
									 * ignored
									 */
									if (normalMarksList0.contains(m_mark)) {
										normalMarksList0.remove(m_mark);
									}

								} else {
									/** add called method annotations */
									normalMarksList0.add(m_mark);
								}
							}

						}

						/** inheritance only find class level marks */

						/** 1.2 add called method xml configurations */
						String mthd = m.getSignature();
//						Set<String> tmpRet2 = findXMLMarks(mthd, xmlSet, "method");
						ArrayList<String> tmpRet2 = resolveMarksUtil.getXMLMarks_mthd(mthd, xmlSet);
						for (String r : tmpRet2) {
							String m_mark;
							if (r.startsWith("xml:mtd"))
								m_mark = r;
							else if (!r.startsWith("xml:"))
								m_mark = "xml:mtd" + r;
							else
								continue;
							if (entriesSigs.contains(m.getSignature())) {
								unreachableMtdMarksSet.add(m_mark);
								/**
								 * if unreachable root also has this annotation, this annotation cannot be
								 * ignored
								 */
								if (normalMarksList0.contains(m_mark)) {
									normalMarksList0.remove(m_mark);
								}
							} else {
//								for (String r : tmpRet2) {
								normalMarksList0.add("xml:mtd" + r);
//								}
							}
						}
					}
				}

				/** add normal method marks */
				for (String m : normalMarksList0) {
					if (!unreachableMtdMarksSet.contains(m))
						normalMarksList.add(m);
				}

				// do not have methods except init
				if (normal_mthd_count == 0)
					isAllCalled = false;

				/** 2. class level */
				if (isAllCalled) {
					/** all methods in the class had been called except init */

					/** 2.1 add class annotations */
					Collection<Annotation> c_annos = cn.getAnnotations();
					if (c_annos != null) {
						if (c_annos != null) {
							c_annos.forEach(anno -> {
//								System.out.println("["+cn.getName()+"]"+anno.getType().getName());
								normalMarksList.add("anno:class:" + Util.format(anno.getType().getName().toString()));
							});
						}
					}

					// class name
					String baseClass = Util.format(cn.getName().toString());

					/** 2.2 add class inheritances */
					Set<String> tmpRet1 = findInheritanceMarks(baseClass, inheritanceSet);
					for (String r : tmpRet1) {
						normalMarksList.add("inhe:class:" + r);
					}

					/** 2.3 add class xml configurations */
//					Set<String> tmpRet2 = findXMLMarks(baseClass, xmlSet, "class");
					ArrayList<String> tmpRet2 = resolveMarksUtil.getXMLMarks_class(baseClass, xmlSet);
					for (String r : tmpRet2) {
						normalMarksList.add("xml:class:" + r);
					}

				}
			}
		});

		/** 3. conclude all results */
		Map<String, Integer> marks2count = new HashMap<>();
		Util.calculateCounts(marks2count, normalMarksList);
		for (String key : marks2count.keySet()) {
			allMarks.add(key);
			if (marks2count.get(key) >= k)
				mostMarks.add(key);
		}
	}

	private Set<String> findXMLMarks(String baseClass, Set<infoUnit> xmlSet, String type) {
		Set<String> ret = new HashSet<String>();
		for (infoUnit info : xmlSet) {
			String base = info.getBase();
			if (maySame(base, baseClass, type)) {
				String mark = "";
				List<infoPair> fields = info.getFields();
				for (infoPair f : fields) {
					mark = mark.concat(">").concat(f.getMark());
				}
				ret.add(mark);
			}
		}

		return ret;
	}

	/**
	 * the two string may be similar </br>
	 * TODO: regular expression
	 * 
	 * @param type
	 */
	private boolean maySame(String base1, String base2, String type) {
		if (base1.equals(base2))
			return true;

		if (type.equals("calss")) {
			if (base1.contains(base2))
				return true;
			if (base2.contains(base1))
				return true;
		}

		return false;
	}

	private Set<String> findInheritanceMarks(String baseClass, Set<infoUnit> inheritanceSet) {
		Set<String> ret = new HashSet<String>();

		for (infoUnit m : inheritanceSet) {
			if (m.getBase().equals(baseClass)) {
				List<infoPair> fields = m.getFields();
				for (infoPair f : fields) {
					ret.addAll(f.getAllValues());
				}
			}
		}

		return ret;

	}

//	private void calculateAnnotationsInfo() {
//		Set<String> entriesSigs = builder.getAllUnreachableEntryPoints();
//
//		ArrayList<String> normalMarksList = new ArrayList<>();
//
//		cha.forEach(cn -> {
//			if (cn.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
//
//				boolean isAllCalled = true;
//				for (IMethod m : cn.getDeclaredMethods()) {
//					String name = m.getName().toString();
//
//					if (!name.equals("<init>")) {
//						if (entriesSigs.contains(m.getSignature())) {
//							isAllCalled = false;
//
//						} else {
//							/** add called method annotations */
//							Collection<Annotation> annos = m.getAnnotations();
//							if (annos != null) {
//								annos.forEach(anno -> {
////									System.out.println("["+m.getSignature()+"]"+anno.getType().getName());
//									normalMarksList.add("anno:mtd:" + Util.format(anno.getType().getName().toString()));
//								});
//							}
//						}
//					}
//
//				}
//				if (isAllCalled) {
//					Collection<Annotation> c_annos = cn.getAnnotations();
//					if (c_annos != null) {
//						if (c_annos != null) {
//							c_annos.forEach(anno -> {
////								System.out.println("["+cn.getName()+"]"+anno.getType().getName());
//								normalMarksList.add("anno:class:" + Util.format(anno.getType().getName().toString()));
//							});
//						}
//					}
//				}
//			}
//		});
//		Map<String, Integer> marks2count = new HashMap<>();
//		Util.calculateCounts(marks2count, normalMarksList);
//		for (String key : marks2count.keySet()) {
//			allMarks.add(key);
//			if (marks2count.get(key) >= k)
//				mostMarks.add(key);
//		}
//	}

	public Set<String> getAllMarks() {
		return allMarks;
	}

	public Set<String> getMostMarks() {
		return mostMarks;
	}

}
