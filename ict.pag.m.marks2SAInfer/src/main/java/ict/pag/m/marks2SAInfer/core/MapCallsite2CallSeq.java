package ict.pag.m.marks2SAInfer.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;

import ict.pag.m.frameworkInfoUtil.customize.ConfigUtil;
import ict.pag.m.frameworkInfoUtil.customize.Util;
import ict.pag.m.marks2SAInfer.util.CallsiteInfo;
import ict.pag.m.marks2SAInfer.util.StringUtil;
import ict.pag.m.marks2SAInfer.util.callsite2TargetUtil;

public class MapCallsite2CallSeq {
	private final CHACallGraph chaCG;
	/* outer2Seq: in 'outer' function, all callistes and directly actual invokes */
	private final HashMap<String, Set<ArrayList<String>>> outer2Seq;

	/** Answers below */
	// record the callsite to real target when the real target and the base object
	// declare type are different. The call site is application codes.
	HashMap<CallsiteInfo, String> callsite2target_app = new HashMap<>();
	HashMap<FieldReference, String> fieldRef2target_app = new HashMap<>();
	// field declared type is the same as actual call
	HashMap<FieldReference, String> fieldRef2target_app_same = new HashMap<>();
	// the framework and its likely targets
	HashMap<CallsiteInfo, ArrayList<String>> callsite2target_frk = new HashMap<>();

	// callsite and its actual calls map,
	// but the actual calls not the same class with callsite declared
	HashMap<String, HashSet<String>> callsite2sameSig_app = new HashMap<>();
	HashMap<String, HashSet<String>> callsite2sameSig_frk = new HashMap<>();

	// record call sequence after one line callsite
	HashSet<ArrayList<String>> actualSeqAfterOneLineCallsite = new HashSet<>();

	HashSet<ArrayList<String>> actualSeqAfterOneLineCallsite2 = new HashSet<>();

	HashMap<String, ArrayList<String>> actualSeqAfterOneLineCallsite3 = new HashMap<>();

	// callsite is application, and the following actual calls have match with
	// callsite but except this
	HashSet<String> others1 = new HashSet<>();
	// callsite is application, and the following actual calls donot have match with
	// callsite
	HashSet<String> others2 = new HashSet<>();
	// callsite is not application, the following actual calls
	HashSet<String> others3 = new HashSet<>();

	public MapCallsite2CallSeq(CHACallGraph chaCG, HashMap<String, Set<ArrayList<String>>> outer2Seq) {
		this.chaCG = chaCG;
		this.outer2Seq = outer2Seq;
	}

	public void dealWith() {
		Set<String> allouters = outer2Seq.keySet();
		int counts = chaCG.getNumberOfNodes();

		Set<ArrayList<String>> outer = outer2Seq.get("outer");
		for (ArrayList<String> seq : outer) {
			ArrayList<String> tmp = new ArrayList<>();
			tmp.add("outer");
			for (String s : seq) {
				if (!s.endsWith("[end]"))
					tmp.add(s);
			}
			if (tmp.size() > 2) {
				actualSeqAfterOneLineCallsite.add(tmp);
				actualSeqAfterOneLineCallsite2.add(tmp);
			}
		}

		chaCG.forEach(node -> {
			if (node.getMethod().getDeclaringClass().getClassLoader().getReference()
					.equals(ClassLoaderReference.Application)) {
				if (node.getIR() == null)
					return;

				DefUse duChain = node.getDU();

				String clazz = node.getMethod().getDeclaringClass().getName().toString();
				IMethod mthd = node.getMethod();
				String mthdSig = mthd.getSignature();

				if (allouters.contains(mthdSig)) {
					Set<ArrayList<String>> Allsequences = outer2Seq.get(mthdSig);

					SSAInstruction[] insts = node.getIR().getInstructions();
					if (insts == null)
						return;
					/** STEP1 */
					/**
					 * collect concerned only application call statement // example:</br>
					 * ClassA extends ClassB ...</br>
					 * Class1 {ClassB field1; ... function(){ this.field1.foo();}}</br>
					 * and the type of 'foo()' is ClassA
					 */
					ArrayList<String> callsiteConcerned = new ArrayList<>();
					ArrayList<FieldReference> fieldSigConcerned = new ArrayList<>();
					for (int i = 0; i < insts.length; i++) {
						SSAInstruction inst = insts[i];

						int lineNumber = -1;
						try {
							SourcePosition sp = Util.getSourcePosition(mthd, i);
							lineNumber = sp == null ? -1 : sp.getLastLine();
						} catch (InvalidClassFileException e) {
							e.printStackTrace();
						}
						if (inst instanceof SSAInvokeInstruction) {
							SSAInvokeInstruction invokeInst = (SSAInvokeInstruction) inst;
							String targetSig = invokeInst.getDeclaredTarget().getSignature();
							if ((node.getMethod().getDeclaringClass().getClassLoader().getReference()
									.equals(ClassLoaderReference.Application) && ConfigUtil.enableApplication)
									|| ConfigUtil.isApplicationClass(targetSig)) {
								if (!invokeInst.isStatic()) {
									int baseVn = invokeInst.getUse(0);
//									String targetSig = invokeInst.getCallSite().getDeclaredTarget().getSignature();
									SSAInstruction baseDef = duChain.getDef(baseVn);
									if (baseDef != null && (baseDef instanceof SSAGetInstruction)) {
										SSAGetInstruction getBaseDefInst = (SSAGetInstruction) baseDef;
										FieldReference fieldRef = getBaseDefInst.getDeclaredField();

										String fieldClass = fieldRef.getDeclaringClass().getName().toString();
										String fieldType = fieldRef.getFieldType().getName().toString();
										if (fieldClass != null && clazz != null && fieldClass.equals(clazz)) {
											callsiteConcerned.add(targetSig + "[" + lineNumber + "]");
											fieldSigConcerned.add(fieldRef);
										}
									}
								}
							}
						}
					}
					/**
					 * STEP2</br>
					 * deal with actual call target including framework call and field.invoke()
					 */

					policy2(Allsequences, callsiteConcerned, fieldSigConcerned);
				}

			}
		});
		System.out.println();
	}

	private void policy2(Set<ArrayList<String>> Allsequences, ArrayList<String> callsiteConcerned,
			ArrayList<FieldReference> fieldSigConcerned) {
		for (ArrayList<String> sequence : Allsequences) {
			for (int i = 0; i < sequence.size();) {
				String line = sequence.get(i);

				// change i
				i++;
				if (line.startsWith("[callsite]")) {
					CallsiteInfo callsite = StringUtil.calCallsiteInfo(line);
					String callStmt = callsite.getCallStmt();
					String lineNum = callsite.getLineNumber();

					/**
					 * 1. callsite corresponding actual calls and only contains call start
					 */
					ArrayList<String> actualCalls = new ArrayList<>();
					for (; i < sequence.size();) {
						String next = sequence.get(i);
						if (next.startsWith("[callsite]")) {
							break;
						} else {
							if (!next.endsWith("[end]"))
								actualCalls.add(next);
							// change i
							i++;
						}
					}

					// do not have actuals, continue
					if (actualCalls.isEmpty()) {
						continue;
					}

					// add the sequence after one callsite
					if (actualCalls.size() > 1)
						actualSeqAfterOneLineCallsite.add(actualCalls);

					// no any callsite is framework call or application call, continue
//					if (!ConfigUtil.isApplicationClass(callStmt) && !ConfigUtil.isFrameworkMarks(callStmt)) {
//						continue;
//					}

					/**
					 * 2. calculate field
					 */
					/** do not have match target */
					Set<String> errorCallsites = new HashSet<>();
//					if (ConfigUtil.isApplicationClass(callStmt)) {
					if (Util.hasApplicationClass(callStmt)) {
						boolean find = false;
						String match = "";
						ArrayList<String> newCalls = new ArrayList<>();
						for (int l2 = 0; l2 < actualCalls.size(); l2++) {
							String tar = actualCalls.get(l2);
							if (tar.endsWith("[normal]"))
								tar = tar.substring(0, tar.indexOf("[normal]"));
							// declare and actual call is same
							if (callStmt.equals(tar)) {
								int vn = callsite2TargetUtil.concernedField(callStmt, lineNum, callsiteConcerned);
								if (vn != -1) {
									fieldRef2target_app_same.put(fieldSigConcerned.get(vn), tar);
								}
								find = true;
								match = tar;
								newCalls.add(tar);
								if (actualCalls.size() > 1)
									actualSeqAfterOneLineCallsite3.put(tar, actualCalls);
								break;
							} else {
								// function signature is the same
								String tarFunction = tar.substring(tar.lastIndexOf('.') + 1);
								String csFunction = callStmt.substring(callStmt.lastIndexOf('.') + 1);
								if (tarFunction.equals(csFunction)) {
									callsite2target_app.put(callsite, tar);
									// only add concerned callsite
									int vn = callsite2TargetUtil.concernedField(callStmt, lineNum, callsiteConcerned);
									if (vn != -1) {
										fieldRef2target_app.put(fieldSigConcerned.get(vn), tar);
									} else {
										if (callsite2sameSig_app.keySet().contains(callStmt)) {
											callsite2sameSig_app.get(callStmt).add(tar);
										} else {
											HashSet<String> tmp = new HashSet<>();
											tmp.add(tar);
											callsite2sameSig_app.put(callStmt, tmp);
										}
									}
									find = true;
									match = tar;
									newCalls.add(tar);
									if (actualCalls.size() > 1)
										actualSeqAfterOneLineCallsite3.put(tar, actualCalls);
									break;
								} else {

								}
							}
						}
						if (!find)
							errorCallsites.add(callStmt + "[" + lineNum + "]");
						if (match.equals("")) {
							others2.addAll(actualCalls);
						} else {
							if (actualCalls.size() > 1) {
								newCalls.addAll(actualCalls);
								actualSeqAfterOneLineCallsite2.add(newCalls);
//								actualSeqAfterOneLineCallsite2.add(actualCalls);
							}

							for (String call : actualCalls) {
								if (!call.startsWith(match))
									others1.add(call);
							}
						}
					} else {
						others3.addAll(actualCalls);
						/**
						 * 3. framework </br>
						 * situation1. only a framework invoke without any call back to app </br>
						 * situation2. framework invoke --> ... --> application call
						 */
						if (ConfigUtil.isFrameworkMarks(callStmt)) {
							for (int l2 = 0; l2 < actualCalls.size(); l2++) {
								String tar = actualCalls.get(l2);
								if (tar.endsWith("[normal]")) {
									continue;
								}

								if (callsite2target_frk.containsKey(callsite)) {
									callsite2target_frk.get(callsite).add(tar);
								} else {
									ArrayList<String> tmp = new ArrayList<>();
									tmp.add(tar);
									callsite2target_frk.put(callsite, tmp);
								}

								// B, I is in framework; A, C in application
								// A extends B; B implements I; C implements I;
								// I i; i.f1();
								// logs below:
								// [callsite] I.f1();
								// [call method] A.f1();
								// want konw: A is also implements I;
								// P.s. (from application code, we can only know A's ancestor is B)
								// function signature is the same
								String tarFunction = tar.substring(tar.lastIndexOf('.') + 1);
								String csFunction = callStmt.substring(callStmt.lastIndexOf('.') + 1);
								if (tarFunction.equals(csFunction)) {
									if (callsite2sameSig_frk.keySet().contains(callStmt)) {
										callsite2sameSig_frk.get(callStmt).add(tar);
									} else {
										HashSet<String> tmp = new HashSet<>();
										tmp.add(tar);
										callsite2sameSig_frk.put(callStmt, tmp);
									}
								}
							}

						} else {
							// the callsite is neither application nor concerned framework
							// the actucal calls following also ignored
//							System.out.println();
						}
					}

				}
			}
		}
	}

	/**
	 * old: callsite with same line will mixed the call methods because the
	 * instrumental stage 'inserAT'
	 */
	public void policy1(Set<ArrayList<String>> Allsequences, ArrayList<String> callsiteConcerned,
			ArrayList<FieldReference> fieldSigConcerned) {
		/**
		 * deal with actual call target including framework call and field.invoke()
		 */
		for (ArrayList<String> sequence : Allsequences) {
//		if (callsiteConcerned.isEmpty())
//			return;

			ArrayList<CallsiteInfo> sameLocCallsiteSet = new ArrayList<>();
			for (int i = 0; i < sequence.size();) {
				String line = sequence.get(i);
				// change i
				i++;
				if (line.startsWith("[callsite]")) {
					sameLocCallsiteSet.clear();

					CallsiteInfo callsite = StringUtil.calCallsiteInfo(line);
					String callStmt = callsite.getCallStmt();
					String lineNum = callsite.getLineNumber();

//				if (ConfigUtil.isApplicationClass(callStmt) || ConfigUtil.isFrameworkMarks(callStmt)) {

					sameLocCallsiteSet.add(callsite);

					/* 1. search callsites with same line number */
					for (; i < sequence.size();) {
						String next = sequence.get(i);
						if (next.startsWith("[callsite]")) {
							CallsiteInfo nextInfo = StringUtil.calCallsiteInfo(next);
							String nextLine = nextInfo.getLineNumber();
							if (nextLine.equals(lineNum)) {
								sameLocCallsiteSet.add(nextInfo);
								// change i
								i++;
							} else {
								break;
							}
						} else {
							break;
						}
					}

					/*
					 * 2. search the actual calls before the next callsite only contains call start
					 */
					ArrayList<String> actualCalls = new ArrayList<>();
					for (; i < sequence.size();) {
						String next = sequence.get(i);
						if (next.startsWith("[callsite]")) {
							break;
						} else {
							if (!next.endsWith("[end]"))
								actualCalls.add(next);
							// change i
							i++;
						}
					}

					// do not have actuals, continue
					if (actualCalls.isEmpty()) {
						for (CallsiteInfo tmp : sameLocCallsiteSet) {
							String tmpStmt = tmp.getCallStmt();
//							if (ConfigUtil.isFrameworkMarks(tmpStmt)) {
//								singleFrmkInvoke.add(tmpStmt);
//							}
						}
						continue;
					}

					actualSeqAfterOneLineCallsite.add(actualCalls);

					// no any callsite is framework call or application call, continue
					boolean allUnrelated = true;
					for (CallsiteInfo tmp : sameLocCallsiteSet) {
						String tmpStmt = tmp.getCallStmt();
//						if (ConfigUtil.isApplicationClass(tmpStmt) || ConfigUtil.isFrameworkMarks(tmpStmt)) {
						if (Util.hasApplicationClass(tmpStmt) || ConfigUtil.isFrameworkMarks(tmpStmt)) {
							allUnrelated = false;
							break;
						}
					}
					if (allUnrelated) {
						for (CallsiteInfo tmp : sameLocCallsiteSet) {
							String tmpStmt = tmp.getCallStmt();
//							if (ConfigUtil.isFrameworkMarks(tmpStmt)) {
//								singleFrmkInvoke.add(tmpStmt);
//							}
						}
						continue;
					}

					/**
					 * 1. first tag application callsite and target </br>
					 * 2. then tag framework call and sequence </br>
					 * value 0: haven't deal with value 1: application call deal with
					 */
					int[] dealwithed = new int[actualCalls.size()];
					String[] callsiteRecord = new String[actualCalls.size()];

					/** do not have match target */
					Set<String> errorCallsites = new HashSet<>();
					// one callsite map to one target
					// l1 record the callsite
					// l2 record the potential targets
					/** application */
					int l2 = 0;
					for (int l1 = 0; l1 < sameLocCallsiteSet.size(); l1++) {
						CallsiteInfo cs = sameLocCallsiteSet.get(l1);
						String csStmt = cs.getCallStmt();
						String csLoc = cs.getLineNumber();

//					if (!ConfigUtil.isApplicationClass(csStmt))
//						continue;

//						if (ConfigUtil.isApplicationClass(csStmt)) {
						if (Util.hasApplicationClass(callStmt)) {

							boolean find = false;
							for (; l2 < actualCalls.size(); l2++) {
								String tar = actualCalls.get(l2);
								if (tar.endsWith("[normal]"))
									tar = tar.substring(0, tar.indexOf("[normal]"));
								// declare and actual call is same
								if (csStmt.equals(tar)) {
//									if (dealwithed[l2] == 2)
//										singleFrmkInvoke.add(csStmt);
									dealwithed[l2] = 1;
									callsiteRecord[l2] = csStmt;
									l2++;
									find = true;

									break;
								} else {
									String tarFunction = tar.substring(tar.lastIndexOf('.') + 1);
									String csFunction = csStmt.substring(csStmt.lastIndexOf('.') + 1);
									if (tarFunction.equals(csFunction)) {
										// only add concerned callsite
										int vn = callsite2TargetUtil.concernedField(csStmt, csLoc, callsiteConcerned);
										if (vn != -1) {
											callsite2target_app.put(cs, tar);
											fieldRef2target_app.put(fieldSigConcerned.get(vn), tar);
										}
//									if (callsite2TargetUtil.isConcerned(csStmt, csLoc, callsiteConcerned)) {
//										callsite2target_app.put(cs, tar);
//									}
//										if (dealwithed[l2] == 2)
//											singleFrmkInvoke.add(csStmt);
										dealwithed[l2] = 1;
										callsiteRecord[l2] = csStmt;
										l2++;
										find = true;
										break;
									} else {

//								otherSeq.add(tar);
									}
								}
							}
							if (!find)
								errorCallsites.add(csStmt + "[" + csLoc + "]");
						}

						System.out.print(" ");

					}

					/**
					 * framework </br>
					 * situation1. only a framework invoke without any call back to app </br>
					 * situation2. framework invoke --> ... --> application call
					 */
					l2 = 0;
					for (int l1 = 0; l1 < sameLocCallsiteSet.size(); l1++) {
						CallsiteInfo cs = sameLocCallsiteSet.get(l1);
						String csStmt = cs.getCallStmt();
						String csLoc = cs.getLineNumber();

//						if (ConfigUtil.isApplicationClass(csStmt)) {
						if (Util.hasApplicationClass(callStmt)) {
							if (errorCallsites.contains(csStmt + "[" + csLoc + "]"))
								break;
							for (; l2 < callsiteRecord.length;) {
								if (callsiteRecord[l2] == null) {
									l2++;
									continue;
								}
								if (callsiteRecord[l2].equals(csStmt)) {
									l2++;
									break;
								}
							}
						}

						if (ConfigUtil.isFrameworkMarks(csStmt)) {

							for (; l2 < callsiteRecord.length; l2++) {
								if (callsiteRecord[l2] == null) {
									String tar = actualCalls.get(l2);

									if (tar.endsWith("[normal]")) {
//									tar = tar.substring(0, tar.indexOf("[normal]"));
										continue;
									}
									dealwithed[l2] = 2;
									if (callsite2target_frk.containsKey(cs)) {
										callsite2target_frk.get(cs).add(tar);
									} else {
										ArrayList<String> tmp = new ArrayList<>();
										tmp.add(tar);
										callsite2target_frk.put(cs, tmp);
									}
								} else {
									break;
								}
							}
						}

//						System.out.print(" ");

					}

					/**
					 * calculate the sequence of framework calls,</br>
					 * that have no callsites match
					 */
					ArrayList<String> otherSeq = new ArrayList<String>();
					l2 = 0;
					for (int l1 = 0; l1 < sameLocCallsiteSet.size(); l1++) {

					}
//					System.out.print(" ");

//				}
				}
//				System.out.println(i - 1 + "th processed!");
			}

//			System.out.print(counts + " ");
		}
	}

	public HashMap<CallsiteInfo, String> getCallsite2target_app() {
		return callsite2target_app;
	}

	public HashMap<FieldReference, String> getFieldRef2target_app() {
		return fieldRef2target_app;
	}

	public HashMap<CallsiteInfo, ArrayList<String>> getCallsite2target_frk() {
		return callsite2target_frk;
	}

	public HashSet<ArrayList<String>> getActualSeqAfterOneLineCallsite() {
		return actualSeqAfterOneLineCallsite2;
	}

	public HashSet<String> getEntryIgnores() {
		HashSet<String> tmp = new HashSet<>();
//		if (!others2.isEmpty())
//			tmp.addAll(others2);
		if (!others3.isEmpty())
			tmp.addAll(others3);
		return tmp;
	}

	public HashMap<String, HashSet<String>> getFrameworkInternalInheritanceRelation() {
		return this.callsite2sameSig_frk;
	}

	public HashMap<String, ArrayList<String>> getActualSeqAfterOneLineCallsiteMap() {
		return actualSeqAfterOneLineCallsite3;
	}

}
