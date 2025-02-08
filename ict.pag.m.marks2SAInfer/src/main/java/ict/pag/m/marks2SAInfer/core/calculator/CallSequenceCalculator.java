package ict.pag.m.marks2SAInfer.core.calculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import ict.pag.m.frameworkInfoUtil.customize.ConfigUtil;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoUnit;
import ict.pag.m.marks2SAInfer.util.CollectionUtil;
import ict.pag.m.marks2SAInfer.util.reducedSetCollection;
import ict.pag.m.marks2SAInfer.util.resolveMarksUtil;
import ict.pag.m.marks2SAInfer.util.structual.SequenceSet2;
import ict.pag.m.marks2SAInfer.util.structual.sequencePair2;

public class CallSequenceCalculator {

	/* ignore normal marks */
	Set<String> normalMarksSet;

	/* framework inheritance relations */
//	HashSet<String> frameworkInheritanceRelation;

	/* all marks */
	Set<infoUnit> inheritanceSet;
	Set<infoUnit> annoSet;
	Set<infoUnit> xmlSet;

	/* ignore invokes */
	HashSet<String> ignoreInvokes = null;
	/* entry calls statments */
	HashSet<String> entryCalls = null;

	/** answers */
	HashMap<sequencePair2, HashSet<String>> connectPointMap = new HashMap<>();
	SequenceSet2 seqSet_new = new SequenceSet2();
//	HashSet<sequencePair2> sequenceSet = new HashSet<>();

	public CallSequenceCalculator(Set<infoUnit> inhe, Set<infoUnit> anno, Set<infoUnit> xml, Set<String> normal) {

		this.inheritanceSet = inhe;
		this.annoSet = anno;
		this.xmlSet = xml;

		this.normalMarksSet = normal;
	}

	public HashMap<sequencePair2, HashSet<String>> getConnectPointMap() {
		return connectPointMap;
	}

	public SequenceSet2 getSequenceSet2() {
		return seqSet_new;
	}

//	public HashSet<sequencePair2> getSequenceSet() {
//		return sequenceSet;
//	}

	/** target on concered */
	public void calSequence3(ArrayList<String> actualCalls) {
		if (actualCalls.isEmpty())
			return;
		String concern = actualCalls.get(0);
		ArrayList<String> seq = new ArrayList<String>();

		if (!concern.equals("outer")) {
			// after one callsite
			// 1. remove reverse sequence
			String curr = actualCalls.get(1);
			for (int i = 2; i < actualCalls.size(); i++) {
				if (curr.endsWith("[normal]"))
					curr = curr.substring(0, curr.lastIndexOf("["));

				String next = actualCalls.get(i);
				if (seq.contains(curr)) {
					if (!seq.get(seq.size() - 1).equals(curr))
						return;
				} else {
					seq.add(curr);
				}
				curr = next;
			}
			String lastOne = actualCalls.get(actualCalls.size() - 1);
			if (lastOne.endsWith("[normal]"))
				lastOne = lastOne.substring(0, lastOne.lastIndexOf("["));
			if (seq.contains(lastOne)) {
				if (!seq.get(seq.size() - 1).equals(lastOne))
					return;
			} else {
				seq.add(lastOne);
			}

			// 2. calculate sequence
//			calSequence(seq, actualCalls);
			int pos = -1;
			for (int i = 0; i < seq.size(); i++) {
				if (seq.get(i).equals(concern)) {
					pos = i;
					break;
				}
			}
			if (pos != -1) {
				// before concerned
				for (int i = 0; i < pos; i++) {
					HashSet<String> pre_allMarks = new HashSet<>();
					HashSet<String> sec_allMarks = new HashSet<>();
					calculateMarks(seq.get(i), concern, pre_allMarks, sec_allMarks);
					reducedSetCollection rc1 = new reducedSetCollection();
					rc1.add(pre_allMarks);
					reducedSetCollection rc2 = new reducedSetCollection();
					rc2.add(sec_allMarks);
					if (rc1.getAllElements().isEmpty() || rc2.getAllElements().isEmpty()) {
						continue;
					}
					sequencePair2 p2 = new sequencePair2(rc1, rc2);
					seqSet_new.add(p2);
				}
				// after concerned
				for (int i = pos + 1; i < seq.size(); i++) {
					HashSet<String> pre_allMarks = new HashSet<>();
					HashSet<String> sec_allMarks = new HashSet<>();
					calculateMarks(concern, seq.get(i), pre_allMarks, sec_allMarks);
					reducedSetCollection rc1 = new reducedSetCollection();
					rc1.add(pre_allMarks);
					reducedSetCollection rc2 = new reducedSetCollection();
					rc2.add(sec_allMarks);
					if (rc1.getAllElements().isEmpty() || rc2.getAllElements().isEmpty()) {
						continue;
					}
					sequencePair2 p2 = new sequencePair2(rc1, rc2);
					seqSet_new.add(p2);
				}
			}

//			calSeqPolicy2(seq);
		}

	}

	private boolean mtd_anno = false;

	private void calculateMarks(String curr_pre, String curr_sec, HashSet<String> pre_marks,
			HashSet<String> sec_marks) {
		/* first marks */
		ArrayList<String> pre_allMarks = new ArrayList<>();
		resolveMarksUtil.getInheritanceMarks(curr_pre, inheritanceSet).forEach(mark -> {
			HashSet<String> tmpSet = ConfigUtil.g().getAllFrameworkParentsCalls(mark);
			for (String tmp : tmpSet) {
				pre_allMarks.add("inhe:full:" + tmp);
			}
//			String m = CollectionUtil.frameworkInheritanceSetContainsEle(frameworkInheritanceRelation, mark);
//			if (m != null && !m.equals(""))
//				pre_allMarks.add("inhe:full:" + m);
		});
		ArrayList<String> tmp1 = new ArrayList<String>();
		resolveMarksUtil.getAnnosMarks(curr_sec, annoSet).forEach(mark -> {
			if (mark.startsWith("anno:mtd:"))
				mtd_anno = true;
			tmp1.add(mark);
		});
		if (mtd_anno)
			pre_allMarks.addAll(tmp1);
		mtd_anno = false;

		resolveMarksUtil.getXMLMarks(curr_pre, xmlSet).forEach(mark -> {
			pre_allMarks.add("xml:class:" + mark);
		});
		if (pre_allMarks.isEmpty()) {
			return;
		}

		/* following marks */
		ArrayList<String> sec_allMarks = new ArrayList<String>();
		resolveMarksUtil.getInheritanceMarks(curr_sec, inheritanceSet).forEach(mark -> {
			HashSet<String> tmpSet = ConfigUtil.g().getAllFrameworkParentsCalls(mark);
			for (String m : tmpSet) {
				sec_allMarks.add("inhe:full:" + m);
			}
//			String m = CollectionUtil.frameworkInheritanceSetContainsEle(frameworkInheritanceRelation, mark);
//			if (m != null && !m.equals(""))
//				sec_allMarks.add("inhe:full:" + m);

		});
		ArrayList<String> tmp2 = new ArrayList<String>();
		resolveMarksUtil.getAnnosMarks(curr_sec, annoSet).forEach(mark -> {
			if (mark.startsWith("anno:mtd:"))
				mtd_anno = true;
			tmp2.add(mark);
		});
		if (mtd_anno)
			sec_allMarks.addAll(tmp2);
		mtd_anno = false;

		resolveMarksUtil.getXMLMarks(curr_sec, xmlSet).forEach(mark -> {
			sec_allMarks.add("xml:class:" + mark);
		});
		if (sec_allMarks.isEmpty()) {
			// do not have marks
			return;
		}

		// add into sequence Set
		HashSet<String> pre_tmp_marks = new HashSet<String>();
		HashSet<String> post_tmp_marks = new HashSet<String>();
		for (String pre_m : pre_allMarks) {
			if (normalMarksSet.contains(pre_m))
				continue;
			pre_tmp_marks.add(pre_m);
		}
		for (String sec_m : sec_allMarks) {
			if (normalMarksSet.contains(sec_m))
				continue;
			post_tmp_marks.add(sec_m);
		}

		// if there is no element except common elements, ignore
		if (CollectionUtil.contains(pre_tmp_marks, post_tmp_marks)) {
			return;
		}
		if (CollectionUtil.contains(post_tmp_marks, pre_tmp_marks)) {
			return;
		}
		if (pre_tmp_marks.isEmpty() || post_tmp_marks.isEmpty())
			return;

		pre_marks.addAll(pre_tmp_marks);
		sec_marks.addAll(post_tmp_marks);
	}

	/**
	 * @param actualCalls may contains [normal] call
	 */
	public void calSequence2(ArrayList<String> actualCalls) {
		if (actualCalls.isEmpty())
			return;
		String first = actualCalls.get(0);
		ArrayList<String> seq = new ArrayList<String>();
		if (first.equals("outer")) {
			// outer calls
			// do not care about connect
			// 1. remove reverse sequence
			String curr = actualCalls.get(1);
			for (int i = 2; i < actualCalls.size(); i++) {
				if (curr.endsWith("[normal]"))
					continue;

				String next = actualCalls.get(i);
				if (seq.contains(curr)) {
					if (!seq.get(seq.size() - 1).equals(curr))
						return;
				} else {
					seq.add(curr);
				}
				curr = next;
			}
			String lastOne = actualCalls.get(actualCalls.size() - 1);
			if (!lastOne.endsWith("[normal]")) {
				if (seq.contains(lastOne)) {
					if (!seq.get(seq.size() - 1).equals(lastOne))
						return;
				} else {
					seq.add(lastOne);
				}
			}
			// 2. calculate sequence
			calSequence(seq, actualCalls);

		} else {
			// after one callsite
			// 1. remove reverse sequence
			String curr = actualCalls.get(1);
			for (int i = 2; i < actualCalls.size(); i++) {
				if (curr.endsWith("[normal]"))
					curr = curr.substring(0, curr.lastIndexOf("["));

				String next = actualCalls.get(i);
				if (seq.contains(curr)) {
					if (!seq.get(seq.size() - 1).equals(curr))
						return;
				} else {
					seq.add(curr);
				}
				curr = next;
			}
			String lastOne = actualCalls.get(actualCalls.size() - 1);
			if (lastOne.endsWith("[normal]"))
				lastOne = lastOne.substring(0, lastOne.lastIndexOf("["));
			if (seq.contains(lastOne)) {
				if (!seq.get(seq.size() - 1).equals(lastOne))
					return;
			} else {
				seq.add(lastOne);
			}

			// 2. calculate sequence
			calSequence(seq, actualCalls);
			calSeqPolicy2(seq);
		}
	}

	// for annotation tags
	boolean hasMtd_anno = false;

	public void calSeqPolicy2(ArrayList<String> actualCallSeq) {
		if (actualCallSeq.size() < 2)
			return;

		String curr_pre = actualCallSeq.get(0);
		for (int i = 1; i < actualCallSeq.size(); i++) {
			String curr_sec = actualCallSeq.get(i);

			if (ignoreInvokes.contains(curr_pre)) {
				// ignores
				curr_pre = curr_sec;
				continue;
			}

			ArrayList<String> pre_allMarks = new ArrayList<>();
			resolveMarksUtil.getInheritanceMarks(curr_pre, inheritanceSet).forEach(mark -> {
				HashSet<String> tmpSet = ConfigUtil.g().getAllFrameworkParentsCalls(mark);
				for (String m : tmpSet) {
					pre_allMarks.add("inhe:full:" + m);
				}
//				String m = CollectionUtil.frameworkInheritanceSetContainsEle(frameworkInheritanceRelation, mark);
//				if (m != null && !m.equals(""))
//					pre_allMarks.add("inhe:full:" + m);
			});
//			resolveMarksUtil.getAnnosMarks(curr_pre, annoSet).forEach(mark -> {
//				pre_allMarks.add(mark);
//			});
			ArrayList<String> tmp1 = new ArrayList<String>();
			resolveMarksUtil.getAnnosMarks(curr_sec, annoSet).forEach(mark -> {
//				sec_allMarks.add(mark);
				if (mark.startsWith("anno:mtd:"))
					hasMtd_anno = true;
				tmp1.add(mark);
			});
			if (hasMtd_anno)
				pre_allMarks.addAll(tmp1);
			hasMtd_anno = false;

			resolveMarksUtil.getXMLMarks(curr_pre, xmlSet).forEach(mark -> {
				pre_allMarks.add("xml:class:" + mark);
			});
			if (pre_allMarks.isEmpty()) {
				// do not have marks
				curr_pre = curr_sec;
				continue;
			}

			ArrayList<String> sec_allMarks = new ArrayList<String>();
			resolveMarksUtil.getInheritanceMarks(curr_sec, inheritanceSet).forEach(mark -> {
				HashSet<String> tmpSet = ConfigUtil.g().getAllFrameworkParentsCalls(mark);
				for (String m : tmpSet) {
					sec_allMarks.add("inhe:full:" + m);
				}
				
//				String m = CollectionUtil.frameworkInheritanceSetContainsEle(frameworkInheritanceRelation, mark);
//				if (m != null && !m.equals(""))
//					sec_allMarks.add("inhe:full:" + m);

			});
			ArrayList<String> tmp2 = new ArrayList<String>();
			resolveMarksUtil.getAnnosMarks(curr_sec, annoSet).forEach(mark -> {
//				sec_allMarks.add(mark);
				if (mark.startsWith("anno:mtd:"))
					hasMtd_anno = true;
				tmp2.add(mark);
			});
			if (hasMtd_anno)
				sec_allMarks.addAll(tmp2);
			hasMtd_anno = false;

			resolveMarksUtil.getXMLMarks(curr_sec, xmlSet).forEach(mark -> {
				sec_allMarks.add("xml:class:" + mark);
			});
			if (sec_allMarks.isEmpty()) {
				// do not have marks
				curr_pre = curr_sec;
				continue;
			}

			// if first is the same as second, donot compare
			if (isSameSignature(curr_pre, curr_sec)) {
				curr_pre = curr_sec;
				continue;
			}

			// add into sequence Set
			HashSet<String> pre_tmp_marks = new HashSet<String>();
			HashSet<String> post_tmp_marks = new HashSet<String>();
			for (String pre_m : pre_allMarks) {
				if (normalMarksSet.contains(pre_m))
					continue;
				pre_tmp_marks.add(pre_m);
			}
			for (String sec_m : sec_allMarks) {
				if (normalMarksSet.contains(sec_m))
					continue;
				post_tmp_marks.add(sec_m);
			}

			// if there is no element except common elements, ignore
			if (CollectionUtil.contains(pre_tmp_marks, post_tmp_marks)) {
				continue;
			}
			if (CollectionUtil.contains(post_tmp_marks, pre_tmp_marks)) {
				continue;
			}
			if (pre_tmp_marks.isEmpty() || post_tmp_marks.isEmpty())
				continue;

			reducedSetCollection rc1 = new reducedSetCollection();
			rc1.add(pre_tmp_marks);
			reducedSetCollection rc2 = new reducedSetCollection();
			rc2.add(post_tmp_marks);
			if (rc1.getAllElements().isEmpty() || rc2.getAllElements().isEmpty()) {
				curr_pre = curr_sec;
				continue;
			}
			sequencePair2 p2 = new sequencePair2(rc1, rc2);
			seqSet_new.add(p2);

			curr_pre = curr_sec;
		}

	}

	private boolean isSameSignature(String pre, String sec) {
		String sig1 = pre.substring(pre.lastIndexOf('.') + 1);
		String sig2 = sec.substring(sec.lastIndexOf('.') + 1);
		if (sig1.equals(sig2))
			return true;
		return false;
	}

	public void calSequence(ArrayList<String> actualCallSeq, ArrayList<String> actualCallSeq_all) {
		if (actualCallSeq.isEmpty())
			return;
		// deal with sub-group sequence
		int[] visitedFlag = new int[actualCallSeq.size()];
		for (int i = 0; i < actualCallSeq.size(); i++) {
			if (visitedFlag[i] != 0) {
				// has visited
				continue;
			}

			String curr_pre = actualCallSeq.get(i);

			if (ignoreInvokes.contains(curr_pre)) {
				// ignores
				visitedFlag[i] = -2;
				continue;
			}

			ArrayList<String> pre_allMarks = new ArrayList<>();
			resolveMarksUtil.getInheritanceMarks(curr_pre, inheritanceSet).forEach(mark -> {
				HashSet<String> tmpSet = ConfigUtil.g().getAllFrameworkParentsCalls(mark);
				for (String m : tmpSet) {
					pre_allMarks.add("inhe:full:" + m);
				}
				
//				String m = CollectionUtil.frameworkInheritanceSetContainsEle(frameworkInheritanceRelation, mark);
//				if (m != null && !m.equals(""))
//					pre_allMarks.add("inhe:full:" + m);
			});
			resolveMarksUtil.getAnnosMarks(curr_pre, annoSet).forEach(mark -> {
				pre_allMarks.add(mark);
			});
			resolveMarksUtil.getXMLMarks(curr_pre, xmlSet).forEach(mark -> {
				pre_allMarks.add("xml:class:" + mark);
			});
			if (pre_allMarks.isEmpty()) {
				// do not have marks
				visitedFlag[i] = -1;
				continue;
			}

			visitedFlag[i] = 1;
			/* find the next */
			for (int j = i + 1; j < actualCallSeq.size(); j++) {

				String curr_sec = actualCallSeq.get(j);
				if (ignoreInvokes.contains(curr_sec)) {
					visitedFlag[j] = -2;
					continue;
				}

				ArrayList<String> sec_allMarks = new ArrayList<String>();
				resolveMarksUtil.getInheritanceMarks(curr_sec, inheritanceSet).forEach(mark -> {
					HashSet<String> tmpSet = ConfigUtil.g().getAllFrameworkParentsCalls(mark);
					for (String m : tmpSet) {
						sec_allMarks.add("inhe:full:" + m);
					}
//					String m = CollectionUtil.frameworkInheritanceSetContainsEle(frameworkInheritanceRelation, mark);
//					if (m != null && !m.equals(""))
//						sec_allMarks.add("inhe:full:" + m);
				});
				resolveMarksUtil.getAnnosMarks(curr_sec, annoSet).forEach(mark -> {
					sec_allMarks.add(mark);
				});
				resolveMarksUtil.getXMLMarks(curr_sec, xmlSet).forEach(mark -> {
					sec_allMarks.add("xml:class:" + mark);
				});
				if (sec_allMarks.isEmpty()) {
					// do not have marks
					visitedFlag[j] = -1;
					continue;
				}

				if (canCompareSequence(curr_pre, pre_allMarks, curr_sec, sec_allMarks)
						|| (isEntryCall(curr_pre) && isEntryCall(curr_sec))) {
					// 1. calculate connect point marks
					HashSet<String> connectPointMarks = new HashSet<String>();

					int loc1 = actualCallSeq_all.indexOf(curr_pre);
					int loc2 = actualCallSeq_all.indexOf(curr_sec);
					if (loc1 != -1 && loc2 != -1 && loc1 < loc2) {
						String next = null;
						for (int loc = loc1 + 1; loc < loc2; loc++) {
							String firstNext = actualCallSeq_all.get(loc);
							ArrayList<String> firstNext_allMarks = new ArrayList<String>();
							resolveMarksUtil.getInheritanceMarks(firstNext, inheritanceSet).forEach(mark -> {
								HashSet<String> tmpSet = ConfigUtil.g().getAllFrameworkParentsCalls(mark);
								for (String m : tmpSet) {
									firstNext_allMarks.add("inhe:full:" + m);
								}
								
//								String m = CollectionUtil
//										.frameworkInheritanceSetContainsEle(frameworkInheritanceRelation, mark);
//								if (m != null && !m.equals(""))
//									firstNext_allMarks.add("inhe:full:" + m);
							});
							resolveMarksUtil.getAnnosMarks(firstNext, annoSet).forEach(mark -> {
								firstNext_allMarks.add(mark);
							});
							resolveMarksUtil.getXMLMarks(firstNext, xmlSet).forEach(mark -> {
								firstNext_allMarks.add("xml:class:" + mark);
							});
							if (isFirstNext(curr_pre, pre_allMarks, firstNext, firstNext_allMarks)) {
								next = firstNext;
								break;
							}

						}
						String before = null;
						for (int loc = loc2 - 1; loc > loc1; loc--) {
							String firstNext = actualCallSeq_all.get(loc);
							ArrayList<String> firstNext_allMarks = new ArrayList<String>();
							resolveMarksUtil.getInheritanceMarks(firstNext, inheritanceSet).forEach(mark -> {
								HashSet<String> tmpSet = ConfigUtil.g().getAllFrameworkParentsCalls(mark);
								for (String m : tmpSet) {
									firstNext_allMarks.add("inhe:full:" + m);
								}
								
//								String m = CollectionUtil
//										.frameworkInheritanceSetContainsEle(frameworkInheritanceRelation, mark);
//								if (m != null && !m.equals(""))
//									firstNext_allMarks.add("inhe:full:" + m);
							});
							resolveMarksUtil.getAnnosMarks(firstNext, annoSet).forEach(mark -> {
								firstNext_allMarks.add(mark);
							});
							resolveMarksUtil.getXMLMarks(firstNext, xmlSet).forEach(mark -> {
								firstNext_allMarks.add("xml:class:" + mark);
							});
							if (isFirstNext(curr_sec, sec_allMarks, firstNext, firstNext_allMarks)) {
								before = firstNext;
								break;
							}

						}
						if (before != null && next != null) {
							// remove duplicated
							if (before.equals(next)) {
								resolveMarksUtil.getInheritanceMarks(before, inheritanceSet).forEach(mark -> {
									HashSet<String> tmpSet = ConfigUtil.g().getAllFrameworkParentsCalls(mark);
									for (String m : tmpSet) {
										connectPointMarks.add("inhe:full:" + m);
									}
									
//									String m = CollectionUtil
//											.frameworkInheritanceSetContainsEle(frameworkInheritanceRelation, mark);
//									if (m != null && !m.equals(""))
//										connectPointMarks.add("inhe:full:" + m);
								});
								HashSet<String> anno_tmp = new HashSet<String>();
								resolveMarksUtil.getAnnosMarks(before, annoSet).forEach(mark -> {
									anno_tmp.add(mark);
								});
								boolean hasMthd = false;
								for (String anno : anno_tmp) {
									if (anno.startsWith("anno:mtd:")) {
										hasMthd = true;
										break;
									}
								}
								if (hasMthd)
									connectPointMarks.addAll(anno_tmp);
								resolveMarksUtil.getXMLMarks(before, xmlSet).forEach(mark -> {
									connectPointMarks.add("xml:class:" + mark);
								});
							}
						}
					}

//					// add all marks sequence
//					for (String pre_m : pre_allMarks) {
//						if (normalMarksSet.contains(pre_m))
//							continue;
//						for (String sec_m : sec_allMarks) {
//							if (normalMarksSet.contains(sec_m))
//								continue;
//							sequencePair p = new sequencePair(pre_m, sec_m);
//							if (!connectPointMarks.isEmpty())
//								connectPointMap.put(p, connectPointMarks);
//						}
//					}
					// collection representation
					HashSet<String> pre_tmp_marks = new HashSet<String>();
					HashSet<String> post_tmp_marks = new HashSet<String>();
					for (String pre_m : pre_allMarks) {
						if (normalMarksSet.contains(pre_m))
							continue;
						pre_tmp_marks.add(pre_m);
//						if (pre_m.contains("afterCompletion"))
//							System.out.println();
					}
					for (String sec_m : sec_allMarks) {
						if (normalMarksSet.contains(sec_m))
							continue;
						post_tmp_marks.add(sec_m);
					}

					// if there is no element except common elements, ignore
					if (CollectionUtil.contains(pre_tmp_marks, post_tmp_marks)) {
						continue;
					}
					if (CollectionUtil.contains(post_tmp_marks, pre_tmp_marks)) {
						continue;
					}

					if (pre_tmp_marks.isEmpty() || post_tmp_marks.isEmpty())
						continue;

					reducedSetCollection rc1 = new reducedSetCollection();
					rc1.add(pre_tmp_marks);
					reducedSetCollection rc2 = new reducedSetCollection();
					rc2.add(post_tmp_marks);
					sequencePair2 p2 = new sequencePair2(rc1, rc2);
					seqSet_new.add(p2);
					if (!connectPointMarks.isEmpty()) {
						connectPointMap.put(p2, connectPointMarks);
					}

					i = j;
					break;
				}

			}

		}

	}

	private boolean isEntryCall(String curr) {
		if (this.entryCalls == null || this.entryCalls.isEmpty())
			return false;
		for (String c : entryCalls) {
			if (c.equals(curr))
				return true;
		}
		return false;
	}

	/**
	 * 1.</br>
	 * 
	 */
	private static boolean canCompareSequence(String curr_pre, ArrayList<String> sec_allMarks, String curr_sec,
			ArrayList<String> sec_allMarks2) {
		if (curr_pre.equals(curr_sec))
			return false;
		// all marks same, think they are the same
		boolean allMarksSame = false;
		if (sec_allMarks.size() == sec_allMarks2.size()) {
			allMarksSame = true;
			for (String m : sec_allMarks) {
				if (!sec_allMarks2.contains(m)) {
					allMarksSame = false;
					break;
				}
			}
		}
		if (allMarksSame)
			return false;

		String pre_class = curr_pre.substring(0, curr_pre.lastIndexOf('.'));
		String sec_class = curr_sec.substring(0, curr_sec.lastIndexOf('.'));
		// if satisfies the conditions below, return true
		// 1. is same class or has extends relation
		// TODO :2. marks belong to the same class
		// TODO: 函数上的标记的value有相似之处
		if (pre_class.equals(sec_class)) {
			return true;
		}

		return false;
	}

	private static boolean isFirstNext(String curr_pre, ArrayList<String> pre_allMarks, String firstNext,
			ArrayList<String> firstNext_allMarks) {
		if (curr_pre.equals(firstNext))
			return false;
		if (firstNext_allMarks.isEmpty() || pre_allMarks.isEmpty())
			return false;
		// all marks same, think they are the same
		boolean allMarksSame = false;
		if (pre_allMarks.size() == firstNext_allMarks.size()) {
			allMarksSame = true;
			for (String m : pre_allMarks) {
				if (!firstNext_allMarks.contains(m)) {
					allMarksSame = false;
					break;
				}
			}
		}
		if (allMarksSame)
			return false;

		return true;
	}

	public void setIgnores(HashSet<String> indirectlySameTargetSet) {
		ignoreInvokes = indirectlySameTargetSet;

	}

	public void setEntryCalls(HashSet<String> entryCalls) {
		this.entryCalls = entryCalls;
	}

}
