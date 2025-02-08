package ict.pag.webframework.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;

import ict.pag.webframework.option.ConfigUtil;
import ict.pag.webframework.option.SpecialHelper;

public class Callsite2CallSeqMapTool {
	private final CHACallGraph chaCG;
	private final HashSet<String> applicationClasses;
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

	public Callsite2CallSeqMapTool(CHACallGraph chaCG, HashMap<String, Set<ArrayList<String>>> outer2Seq,
			HashSet<String> applicationClasses2) {
		this.chaCG = chaCG;
		this.outer2Seq = outer2Seq;
		this.applicationClasses = applicationClasses2;
	}

	public void dealWith() {
		Set<String> allouters = outer2Seq.keySet();

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
						SourcePosition sp = SpecialHelper.getSourcePosition(mthd, i);
						lineNumber = sp == null ? -1 : sp.getLastLine();
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
					CallsiteInfo callsite = CallsiteInfo.calCallsiteInfo(line);
					String callStmt = callsite.getCallStmt();
					String lineNum = callsite.getLineNumber();

					/*
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

					/*
					 * 2. calculate field
					 */
					/** do not have match target */
					Set<String> errorCallsites = new HashSet<>();
//					if (ConfigUtil.isApplicationClass(callStmt)) {
					if (hasApplicationClass(callStmt)) {
						boolean find = false;
						String match = "";
						ArrayList<String> newCalls = new ArrayList<>();
						for (int l2 = 0; l2 < actualCalls.size(); l2++) {
							String tar = actualCalls.get(l2);
							if (tar.endsWith("[normal]"))
								tar = tar.substring(0, tar.indexOf("[normal]"));
							// declare and actual call is same
							if (callStmt.equals(tar)) {
								int vn = LogHelper.concernedField(callStmt, lineNum, callsiteConcerned);
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
									int vn = LogHelper.concernedField(callStmt, lineNum, callsiteConcerned);
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

	private boolean hasApplicationClass(String check) {
		if (check.startsWith("L") && check.contains("/"))
			check = check.substring(1).replaceAll("/", ".");

		for (String c : applicationClasses) {
			if (check.startsWith(c))
				return true;
		}
		return false;
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
