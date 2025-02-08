package ict.pag.m.marks2SAInfer.core.calculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import ict.pag.m.frameworkInfoUtil.customize.ConfigUtil;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoPair;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoUnit;
import ict.pag.m.marks2SAInfer.util.CallsiteInfo;
import ict.pag.m.marks2SAInfer.util.CollectionUtil;
import ict.pag.m.marks2SAInfer.util.resolveMarksUtil;
import ict.pag.m.marks2SAInfer.util.structual.frameworkIndirectInvoke;
import ict.pag.m.marks2SAInfer.util.structual.set2setPair;
import ict.pag.m.marks2SAInfer.util.structual.str2Set;

public class FrameworkIndirectInvokeCalculator {
	/* visited */
	HashMap<String, HashSet<String>> visited = new HashMap<>();

	/* framework inheritance relation */
//	HashSet<String> frameworkInheritanceRelation;

	/* all marks */
	Set<infoUnit> inheritanceSet;
	Set<infoUnit> annoSet;
	Set<infoUnit> xmlSet;

	/* Answers */
	Set<frameworkIndirectInvoke> allIndirectFrameworkInvokeSet = new HashSet<>();

	HashSet<String> indirectlySameTargetSet = new HashSet<>();

	public FrameworkIndirectInvokeCalculator(Set<infoUnit> inhe, Set<infoUnit> anno, Set<infoUnit> xml) {

		this.inheritanceSet = inhe;
		this.annoSet = anno;
		this.xmlSet = xml;

	}

	public Set<frameworkIndirectInvoke> getAllIndirectFrameworkInvokeSet() {
		return allIndirectFrameworkInvokeSet;
	}

	/** override targets */
	public HashSet<String> getIndirectlySameTargetSet() {
		return indirectlySameTargetSet;
	}

	public void calFrameworkIndirectInvoke(HashMap<CallsiteInfo, ArrayList<String>> callsite2target_frk) {
		for (CallsiteInfo callsite : callsite2target_frk.keySet()) {
			// 1. determine whether is framework call indirectly
			// 2. calculate the framework call indirectly marks
			ArrayList<String> mayInvokes = callsite2target_frk.get(callsite);

			calIndirectlyFrameworkInvoke(callsite, mayInvokes);
		}

		removeDulicated();

	}

	private void removeDulicated() {
		Set<frameworkIndirectInvoke> allIndirectFrameworkInvokeSet_new = new HashSet<>();
		for (frameworkIndirectInvoke ele : allIndirectFrameworkInvokeSet) {
			if (!alreadyHave(allIndirectFrameworkInvokeSet_new, ele)) {
				allIndirectFrameworkInvokeSet_new.add(ele);
			}
		}
		allIndirectFrameworkInvokeSet = allIndirectFrameworkInvokeSet_new;

	}

	private boolean alreadyHave(Set<frameworkIndirectInvoke> allIndirectFrameworkInvokeSet_new,
			frameworkIndirectInvoke obj) {
		if (allIndirectFrameworkInvokeSet_new.isEmpty())
			return false;

		for (frameworkIndirectInvoke ele : allIndirectFrameworkInvokeSet_new) {
			if (ele.equals(obj))
				return true;

		}
		return false;
	}

	public void calIndirectlyFrameworkInvoke(CallsiteInfo callsite, ArrayList<String> mayInvokes) {

		// framework callsite info
		String framework_csStmt = callsite.getCallStmt();
		String framework_cs_belongto = callsite.getBelongToMthd();
		String framework_cs_belongto_class = framework_cs_belongto.substring(0, framework_cs_belongto.lastIndexOf('.'));
		String framework_cs_belongto_sig = framework_cs_belongto.substring(
				framework_cs_belongto.indexOf(framework_cs_belongto_class) + framework_cs_belongto_class.length() + 1);
		String frameworkCsClass = framework_csStmt.substring(0, framework_csStmt.lastIndexOf('.'));
		String frameworkCsSig = framework_csStmt
				.substring(framework_csStmt.indexOf(frameworkCsClass) + frameworkCsClass.length() + 1);

		// framework callsite of method marks
		ArrayList<String> framework_marks = new ArrayList<String>();
		ArrayList<infoPair> annos1 = resolveMarksUtil.getMethodAnno(framework_cs_belongto, annoSet);
		ArrayList<String> xml1 = resolveMarksUtil.getXMLMarks(framework_cs_belongto, xmlSet);
		ArrayList<String> inhe1 = resolveMarksUtil.getInheritanceMarks_onclass(framework_cs_belongto_class,
				inheritanceSet);
//		ArrayList<String> inhe1 = resolveMarksUtil.getInheritanceMarks_withDecorate(framework_cs_belongto,
//				inheritanceSet);

		if (annos1.isEmpty() && xml1.isEmpty() && inhe1.isEmpty())
			return;

		HashSet<String> alltargets;
		if (visited.containsKey(framework_csStmt))
			alltargets = visited.get(framework_csStmt);
		else
			alltargets = new HashSet<>();

		for (String mayInvoke : mayInvokes) {
			HashSet<String> anno1Marks = new HashSet<>();
			HashSet<String> inhe1Marks = new HashSet<>();
			ArrayList<str2Set> mayInvokesMarks = new ArrayList<>();

			boolean flag = false;

			if (alltargets.contains(mayInvoke))
				continue;
			alltargets.add(mayInvoke);
			// mayInvoke info
			String mayInvokeClass = mayInvoke.substring(0, mayInvoke.lastIndexOf('.'));
			String mayInvokeSig = mayInvoke.substring(mayInvoke.indexOf(mayInvokeClass) + mayInvokeClass.length() + 1);

			/** 1. framework callsite has the same signature with mayInvoke */
			if (frameworkCsSig.equals(mayInvokeSig)) {
				// do not add the same signature
				indirectlySameTargetSet.add(mayInvoke);
				continue;
			}

			/**
			 * 2. 1)the method with this framework callsite , have same elements or values
			 * with mayInvoke
			 */
			// mayInvoke marks
			ArrayList<String> mayInvoke_marks = new ArrayList<String>();
			ArrayList<infoPair> annos2 = resolveMarksUtil.getMethodAnno(mayInvoke, annoSet);
			ArrayList<String> xml2 = resolveMarksUtil.getXMLMarks(mayInvoke, xmlSet);
			ArrayList<String> inhe2 = resolveMarksUtil
					.getInheritanceMarks_onclass(mayInvoke.substring(0, mayInvoke.lastIndexOf('.')), inheritanceSet);
//			ArrayList<String> inhe2 = resolveMarksUtil.getInheritanceMarks_withDecorate(mayInvoke, inheritanceSet);

			// annotations
			HashSet<String> anno2Marks = new HashSet<>();
			for (infoPair anno1 : annos1) {
				Set<String> anno1Vals = anno1.getAllValues();
				String anno1Mark = anno1.getMark();

				for (infoPair anno2 : annos2) {
					Set<String> anno2Vals = anno2.getAllValues();
					Set<String> sames = CollectionUtil.calSameElements(anno1Vals, anno2Vals);
					if (sames.isEmpty())
						continue;
					anno1Marks.add("anno:mtd:" + anno1Mark);
					String anno2Mark = anno2.getMark();
					anno2Marks.add("anno:mtd:" + anno2Mark);
					break;
				}
			}
			if (!anno1Marks.isEmpty() && !anno2Marks.isEmpty()) {
				mayInvokesMarks.add(new str2Set("annotation-value", anno2Marks));
				indirectlySameTargetSet.add(mayInvoke);
				flag = true;
			}

			if (!flag) {
				// inheritance
				if (!inhe1.isEmpty() && !inhe2.isEmpty()) {
					HashSet<String> inhe2Marks = new HashSet<>();
					for (String i1 : inhe1) {
						for (String i2 : inhe2) {
							if (i1.equals(i2)) {
//							inhe2Marks.add("inhe:class:" + i1);
								inhe2Marks.add(i1);
							}
						}
					}
					if (!inhe2Marks.isEmpty()) {
						mayInvokesMarks.add(new str2Set("same-superclass", inhe2Marks));
						flag = true;
					}
				}
			}

			/*
			 * 3. all invokes the framework may indirectly invokes
			 */
			// Coarse-grained
			if (!flag) {
//				// inheritance
//				HashSet<String> targetMarks1 = new HashSet<>();
//				for (String c : inhe2) {
//					if (inhe1.contains(c)) {
//						String full = c.substring(11) + "." + mayInvokeSig;
//						String tmp = CollectionUtil.frameworkInheritanceSetContainsEle(frameworkInheritanceRelation,
//								full);
//						if (tmp != null && !tmp.equals("")) {
//							targetMarks1.add(c);
//						}
//					}
//				}
//				if (!targetMarks1.isEmpty()) {
//					mayInvokesMarks.add(new str2Set("annotation", new HashSet<>(targetMarks1)));
//				}

				// annotation
				HashSet<String> targetMarks = new HashSet<>();
				for (infoPair i : annos2) {
					targetMarks.add("anno:mtd:" + i.getMark());
				}
				for (infoPair i : annos1) {
					anno1Marks.add("anno:mtd:" + i.getMark());
				}
				// inheritance
				for (String i : inhe1) {
					String full = i.substring(11) + "." + framework_cs_belongto_sig;
					HashSet<String> tmpSet = ConfigUtil.g().getAllFrameworkParentsCalls(full);
					for (String tmp : tmpSet) {
						anno1Marks.add("inhe:full:" + tmp);
					}

//					String tmp = CollectionUtil.frameworkInheritanceSetContainsEle(frameworkInheritanceRelation, full);
//					if (tmp != null && !tmp.equals("")) {
//						anno1Marks.add("inhe:full:" + tmp);
//					}
				}
				for (String i : inhe2) {
					String full = i.substring(11) + "." + mayInvokeSig;
					HashSet<String> tmpSet = ConfigUtil.g().getAllFrameworkParentsCalls(full);
					for (String tmp : tmpSet) {
						anno1Marks.add("inhe:full:" + tmp);
					}
//					String tmp = CollectionUtil.frameworkInheritanceSetContainsEle(frameworkInheritanceRelation, full);
//					if (tmp != null && !tmp.equals("")) {
//						targetMarks.add("inhe:full:" + tmp);
//					}
				}

				if (!targetMarks.isEmpty())
					mayInvokesMarks.add(new str2Set("any", new HashSet<>(targetMarks)));

			}

			if (!mayInvokesMarks.isEmpty()) {
				frameworkIndirectInvoke tmp = new frameworkIndirectInvoke(new str2Set(framework_csStmt, anno1Marks),
						mayInvokesMarks.get(0));
				addIntoAnswers(tmp);
//					allIndirectFrameworkInvokeSet.add(tmp);
			}
		}

	}

	private void addIntoAnswers(frameworkIndirectInvoke tmp) {
		HashMap<frameworkIndirectInvoke, frameworkIndirectInvoke> old2new = new HashMap<>();
		for (frameworkIndirectInvoke ele : allIndirectFrameworkInvokeSet) {
			HashSet<String> newFirst = null;
			HashSet<String> newSec = null;
			// 1. is same indirectly call statement
			if (ele.getCallsite().getName().equals(tmp.getCallsite().getName())) {
				// framework call
				if (CollectionUtil.contains(ele.getCallsite().getVals(), tmp.getCallsite().getVals())) {
					newFirst = tmp.getCallsite().getVals();
				} else if (CollectionUtil.contains(tmp.getCallsite().getVals(), ele.getCallsite().getVals())) {
					Set<String> diff = set2setPair.calDifferenceSet(tmp.getCallsite().getVals(),
							ele.getCallsite().getVals());
					if (!diff.isEmpty()) {
						for (String rm : diff) {
							tmp.getCallsite().getVals().remove(rm);
						}
					}
				}

				// target
				if (CollectionUtil.contains(ele.getTarget().getVals(), tmp.getTarget().getVals())) {
					newSec = tmp.getTarget().getVals();
				} else if (CollectionUtil.contains(tmp.getTarget().getVals(), ele.getTarget().getVals())) {
					Set<String> diff = set2setPair.calDifferenceSet(tmp.getTarget().getVals(),
							ele.getTarget().getVals());
					if (!diff.isEmpty()) {
						for (String rm : diff) {
							tmp.getTarget().getVals().remove(rm);
						}
					}
				}

				// replace the old
				if (newFirst != null && newSec != null) {
					old2new.put(ele, new frameworkIndirectInvoke(new str2Set(ele.getCallsite().getName(), newFirst),
							new str2Set(ele.getTarget().getName(), newSec)));
				} else if (newFirst != null) {
					old2new.put(ele, new frameworkIndirectInvoke(new str2Set(ele.getCallsite().getName(), newFirst),
							ele.getTarget()));
				} else if (newSec != null) {
					old2new.put(ele, new frameworkIndirectInvoke(ele.getCallsite(),
							new str2Set(ele.getTarget().getName(), newSec)));
				}
			}
		}

		/* 2. replace old */
		for (frameworkIndirectInvoke key : old2new.keySet()) {
			allIndirectFrameworkInvokeSet.remove(key);
			allIndirectFrameworkInvokeSet.add(old2new.get(key));
		}

		/* 3. add coming */
		if (!tmp.getCallsite().getVals().isEmpty() && !tmp.getTarget().getVals().isEmpty())
			allIndirectFrameworkInvokeSet.add(tmp);

	}

//	private void add2allIndirectFrameworkInvokeSet(frameworkIndirectInvoke tmp) {
//		str2Set tmp_cs = tmp.getCallsite();
//		HashSet<String> tmp_tar = tmp.getTarget();
//		for (frameworkIndirectInvoke ele : allIndirectFrameworkInvokeSet) {
//			str2Set ele_cs = ele.getCallsite();
//			HashSet<String> ele_tar = ele.getTarget();
//			if (tmp_cs.equals(ele_cs)) {
//				if (!CollectionUtil.isSame(ele_tar, tmp_tar)) {
//
//				}
//
//			}
//		}
//	}

}
