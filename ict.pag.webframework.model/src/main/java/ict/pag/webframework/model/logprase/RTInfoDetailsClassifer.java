package ict.pag.webframework.model.logprase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;

import ict.pag.webframework.model.option.SpecialHelper;

public class RTInfoDetailsClassifer {
	private ClassHierarchy cha;

	private HashMap<String, Set<ArrayList<String>>> mtd2RTSeq;

	/** Intermediate results */
//	private 

	/** Results */
	/* calls */
	private HashSet<String> hasMatchedCallsiteCalls = new HashSet<>();
	private HashSet<String> hasMatchedCallsiteCalls_NotInSameClass = new HashSet<>();
	private HashSet<String> outerCalls = new HashSet<>();

	// 1. the outer calls; 2. have no matched callsite
	private HashSet<String> noMatchedCallsiteCalls = new HashSet<>();
	// follow callsite, and ahead of return site, but no matched callsite
	private HashSet<String> noMatchedCallsiteCalls_followCallsite = new HashSet<>();
	// callsite is application
	private HashSet<String> noMatchedCallsiteCalls_followAppCallsite = new HashSet<>();
	// callsite is framework
	private HashSet<String> noMatchedCallsiteCalls_followFrameCallsite = new HashSet<>();
	// framework callsite -> call sequence
	private HashMap<String, HashSet<String>> frameworkcall2seq = new HashMap<>();
	private HashMap<RuntimeCallsiteInfo, HashSet<String>> frameworkcallsite2seq = new HashMap<>();

	private HashMap<String, Set<ArrayList<String>>> mtdCall2itsFollowingSeq = new HashMap<>();
	private HashMap<String, HashSet<String>> field2ActualUsed = new HashMap<>();
	private HashSet<ArrayList<String>> callSeqSet = new HashSet<>();

	/* field */

	public RTInfoDetailsClassifer(ClassHierarchy cha, HashMap<String, Set<ArrayList<String>>> mtd2rtSeq) {
		this.cha = cha;
		this.mtd2RTSeq = mtd2rtSeq;
	}

	public void classify() {
		calSequenceSet();
		for (String mtd : mtd2RTSeq.keySet()) {
			Set<ArrayList<String>> seqSet = mtd2RTSeq.get(mtd);
			if (mtd.equals("outer")) {
				for (ArrayList<String> seq : seqSet) {
					for (String s : seq) {
						if (!s.endsWith("[end]")) {
							outerCalls.add(s);
							noMatchedCallsiteCalls.add(s);
						}
					}
				}

			} else {
				for (ArrayList<String> seq : seqSet) {

					ArrayList<String> fieldReadedList = new ArrayList<>();
					/* 1. extract call infos */
					for (int i = 0; i < seq.size();) {
						String s = seq.get(i);

						if (s.startsWith("[field read]"))
							fieldReadedList.add(s);

						// change i
						i++;

						if (s.startsWith("[callsite]")) {
							RuntimeCallsiteInfo callsite = RuntimeCallsiteInfo.calCallsiteInfo(s);
							String callStmt = callsite.getCallStmt();
							String callBelongTo = callsite.getBelongToMthd();
							String outerClassStr = callBelongTo.substring(0, callBelongTo.lastIndexOf('.'));

							String csClassStr = SpecialHelper
									.reformatSignature(callStmt.substring(0, callStmt.lastIndexOf('.')));
							IClass csClass = SpecialHelper.getClassFromApplicationCHA(cha,
									csClassStr.substring(0, csClassStr.length() - 1));

							ArrayList<String> actualCalls_tmp = new ArrayList<>();
							/* only add, when the callee method and caller method are different */
							ArrayList<String> actualCalls_tmp2 = new ArrayList<>();
							for (; i < seq.size();) {
								String next = seq.get(i);
								String nextClassStr0 = next.substring(0, next.lastIndexOf('.'));
								if (next.startsWith("[field read]"))
									fieldReadedList.add(next);

								if (next.startsWith("[callsite]") || next.startsWith("[returnSite]")) {
									break;
								} else {
									if (!next.endsWith("[end]") && !next.startsWith("[")) {

										boolean find = false;

										if (callStmt.equals(next)) {
											// callsite and callee are same
											actualCalls_tmp.add(next);
											/* 0306: only add, when the callee method and caller method are different */
											if (!nextClassStr0.equals(outerClassStr)) {
												actualCalls_tmp2.add(next);
											}

											find = true;

											add2FieldUsed(fieldReadedList, s, next);
										} else {

											String nextClassStr = SpecialHelper
													.reformatSignature(next.substring(0, next.lastIndexOf('.')));
											IClass nextClass = SpecialHelper.getClassFromApplicationCHA(cha,
													nextClassStr.substring(0, nextClassStr.length() - 1));

											if (csClass != null && nextClass != null) {
												// class inheritance
												if (cha.isSubclassOf(nextClass, csClass)
														|| cha.isAssignableFrom(csClass, nextClass)) {
													// method name is same
													String name1 = callStmt.substring(callStmt.lastIndexOf('.') + 1,
															callStmt.indexOf('('));
													String name2 = next.substring(next.lastIndexOf('.') + 1,
															next.indexOf('('));
													if (name1.equals(name2)) {
														actualCalls_tmp.add(next);
														/*
														 * 0306: only add, when the callee method and caller method are
														 * different
														 */
														if (!nextClassStr0.equals(outerClassStr))
															actualCalls_tmp2.add(next);
														find = true;

														add2FieldUsed(fieldReadedList, s, next);
													}
												}
											}
										}

										if (!find) {
											if (csClass != null) {
												noMatchedCallsiteCalls.add(next);
												noMatchedCallsiteCalls_followCallsite.add(next);
												if (csClass.getClassLoader().getReference()
														.equals(ClassLoaderReference.Application)) {
													noMatchedCallsiteCalls_followAppCallsite.add(next);
												} else {
													noMatchedCallsiteCalls_followFrameCallsite.add(next);
													if (frameworkcall2seq.containsKey(callStmt)) {
														frameworkcall2seq.get(callStmt).add(next);
													} else {
														HashSet<String> tmp = new HashSet<>();
														tmp.add(next);
														frameworkcall2seq.put(callStmt, tmp);
													}
													if (frameworkcallsite2seq.containsKey(callsite)) {
														frameworkcallsite2seq.get(callsite).add(next);
													} else {
														HashSet<String> tmp = new HashSet<>();
														tmp.add(next);
														frameworkcallsite2seq.put(callsite, tmp);
													}
												}
											}
										}

									}

									// change i
									i++;
								}
							}
							if (!actualCalls_tmp.isEmpty()) {
								hasMatchedCallsiteCalls.addAll(actualCalls_tmp);
							}
							if (!actualCalls_tmp2.isEmpty()) {
								hasMatchedCallsiteCalls_NotInSameClass.addAll(actualCalls_tmp2);
							}
						}

					}
				}
			}

		}
	}

	private void calSequenceSet() {
		for (String mtd : mtd2RTSeq.keySet()) {
			if (mtd.equals("outer"))
				continue;
			Set<ArrayList<String>> seqSet = mtd2RTSeq.get(mtd);
			for (ArrayList<String> seq : seqSet) {
				ArrayList<String> filteredSeq = new ArrayList<String>();
				for (String s : seq) {
					if (!s.endsWith("[end]") && !s.startsWith("[")) {
						filteredSeq.add(s);
					}
				}
				if (!filteredSeq.isEmpty())
					callSeqSet.add(filteredSeq);
			}
		}

	}

	private void add2FieldUsed(ArrayList<String> fieldReadedList, String callsite, String call) {
		RuntimeCallsiteInfo cs = RuntimeCallsiteInfo.calCallsiteInfo(callsite);
		String callstmt = cs.getCallStmt();
		String callsiteDecMtdClass = callstmt.substring(0, callstmt.lastIndexOf('.'));
		String callsiteBaseType = cs.getBaseRTType();

		String callClass = call.substring(0, call.lastIndexOf('.'));

		for (String fr : fieldReadedList) {
			if (fr.contains("[runtimeType]")) {
				String fieldSig = fr.substring(fr.indexOf("[signature]") + "[signature]".length(),
						fr.indexOf("[runtimeType]"));

				String fieldType1 = fieldSig.substring(fieldSig.indexOf(':') + 1);
				String fieldType = SpecialHelper.formatSignature(fieldType1.substring(0, fieldType1.length() - 1));

				String str1 = fr.substring(fr.indexOf("[runtimeType]") + "[runtimeType]".length(),
						fr.lastIndexOf("[base]"));
				String rtType;
				if (str1.contains("[collection]")) {
					rtType = str1.substring(0, str1.indexOf("[collection]"));
				} else {
					rtType = str1;
				}

				if (rtType.equals(callsiteBaseType) && fieldType.equals(callsiteDecMtdClass)) {
					if (field2ActualUsed.containsKey(fieldSig)) {
						field2ActualUsed.get(fieldSig).add(callClass);
					} else {
						HashSet<String> tmp = new HashSet<>();
						tmp.add(callClass);
						field2ActualUsed.put(fieldSig, tmp);
					}
				}

			}
		}
	}

	public HashSet<String> getHasMatchedCallsiteCalls() {
		return hasMatchedCallsiteCalls;
	}

	public HashSet<String> getHasMatchedCallsiteCalls_NotInSameClass() {
		return hasMatchedCallsiteCalls_NotInSameClass;
	}

	public HashSet<String> getOuterCalls() {
		return outerCalls;
	}

	public HashSet<String> getNoMatchedCallsiteCalls() {
		return noMatchedCallsiteCalls;
	}

	public HashMap<String, HashSet<String>> getField2ActualUsed() {
		return field2ActualUsed;
	}

	/** callsite is application and framework */
	public HashSet<String> getNoMatchedCallsiteCalls_followCallsite() {
		return noMatchedCallsiteCalls_followCallsite;
	}

	public HashSet<String> getNoMatchedCallsiteCalls_followAppCallsite() {
		return noMatchedCallsiteCalls_followAppCallsite;
	}

	public HashSet<String> getNoMatchedCallsiteCalls_followFrameCallsite() {
		return noMatchedCallsiteCalls_followFrameCallsite;
	}

	public HashMap<String, HashSet<String>> getFrameworkcall2seq() {
		return frameworkcall2seq;
	}

	public HashMap<RuntimeCallsiteInfo, HashSet<String>> getFrameworkcallsite2seq() {
		return frameworkcallsite2seq;
	}

	public HashSet<ArrayList<String>> getCallSeqSet() {
		return callSeqSet;
	}

}
