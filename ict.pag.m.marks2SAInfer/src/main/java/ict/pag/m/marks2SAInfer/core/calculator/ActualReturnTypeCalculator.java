package ict.pag.m.marks2SAInfer.core.calculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;

import ict.pag.m.frameworkInfoUtil.customize.ConfigUtil;
import ict.pag.m.frameworkInfoUtil.customize.Util;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoPair;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoUnit;
import ict.pag.m.marks2SAInfer.util.CallsiteInfo;
import ict.pag.m.marks2SAInfer.util.resolveMarksUtil;
import ict.pag.m.marks2SAInfer.util.structual.FrmkCallParamType;
import ict.pag.m.marks2SAInfer.util.structual.FrmkRetPoints2;

public class ActualReturnTypeCalculator {
	HashMap<CallsiteInfo, String> callsite2seq;
	CHACallGraph chaCG;

	Set<infoUnit> annoSet;
	Set<infoUnit> xmlSet;

	HashSet<FrmkRetPoints2> frameworkRetActualPoints2 = new HashSet<>();

	public ActualReturnTypeCalculator(HashMap<CallsiteInfo, String> callsite2target_app, CHACallGraph chaCG,
			Set<infoUnit> annoSet, Set<infoUnit> xmlSet_all) {
		this.callsite2seq = callsite2target_app;
		this.chaCG = chaCG;

		this.annoSet = annoSet;
		this.xmlSet = xmlSet_all;

		calculate();
	}

	private void calculate() {

		chaCG.forEach(node -> {
			if (node.getIR() == null)
				return;
			String sig = node.getMethod().getSignature();
			if ((node.getMethod().getDeclaringClass().getClassLoader().getReference()
					.equals(ClassLoaderReference.Application) && ConfigUtil.enableApplication)
					|| ConfigUtil.isApplicationClass(sig)) {

				for (CallsiteInfo cs : callsite2seq.keySet()) {
					String actual_belongToMethod = cs.getBelongToMthd();

					if (actual_belongToMethod.equals(sig)) {
						String actual_invokeStmt = cs.getCallStmt();
						String actual_invokeLine = cs.getLineNumber();
						SSAInstruction[] insts = node.getIR().getInstructions();
						for (SSAInstruction inst0 : insts) {
							if (inst0 instanceof SSAInvokeInstruction) {
								SSAInvokeInstruction invoke = (SSAInvokeInstruction) inst0;
								String targetSig = invoke.getCallSite().getDeclaredTarget().getSignature();
								if (targetSig.equals(actual_invokeStmt)) {
									try {
										int number = Util.getSourcePosition(node.getMethod(), invoke.iIndex())
												.getLastLine();
										String num = Integer.toString(number);
										if (actual_invokeLine.equals(num)
												|| actual_invokeLine.equals(Integer.toString(number + 1))
												|| actual_invokeLine.equals(Integer.toString(number - 1))) {
											// local same and invoke same
											if (!invoke.isStatic()) {
												DefUse duchain = node.getDU();
												int base = invoke.getUse(0);
												SSAInstruction def1 = duchain.getDef(base);
												if (def1 instanceof SSACheckCastInstruction) {
													SSACheckCastInstruction cast = (SSACheckCastInstruction) def1;
													int def2 = cast.getUse(0);

													SSAInstruction mayInst = duchain.getDef(def2);
													if (mayInst instanceof SSAInvokeInstruction) {
														SSAInvokeInstruction actual_def = (SSAInvokeInstruction) mayInst;
														String defInvoke = actual_def.getCallSite().getDeclaredTarget()
																.getSignature();
														if (ConfigUtil.isFrameworkMarks(defInvoke)) {
															// the base is from framework call
															SymbolTable sym = node.getIR().getSymbolTable();
															for (int i = 0; i < actual_def.getNumberOfUses(); i++) {
																int use = actual_def.getUse(i);
																if (sym.isStringConstant(use)) {
																	String value = sym.getStringValue(use);

																	String actual_class = actual_invokeStmt.substring(0,
																			actual_invokeStmt.lastIndexOf('.'));
																	if (value.equals(actual_class)) {
																		/** if equals class full name */
																		if (actual_def.isStatic()) {
																			FrmkRetPoints2 tmp = new FrmkRetPoints2(
																					defInvoke, i + 1,
																					FrmkCallParamType.FQName_text);
																			frameworkRetActualPoints2.add(tmp);
																		} else {
																			FrmkRetPoints2 tmp = new FrmkRetPoints2(
																					defInvoke, i,
																					FrmkCallParamType.FQName_text);
																			frameworkRetActualPoints2.add(tmp);
																		}
																	} else {
																		/** if use alias */
																		HashSet<String> marksSet = findValue(value);
																		if (marksSet != null) {
																			if (actual_def.isStatic()) {
																				FrmkRetPoints2 tmp = new FrmkRetPoints2(
																						defInvoke, i + 1,
																						FrmkCallParamType.Alias_text);
																				frameworkRetActualPoints2.add(tmp);
																			} else {
																				FrmkRetPoints2 tmp = new FrmkRetPoints2(
																						defInvoke, i,
																						FrmkCallParamType.Alias_text);
																				frameworkRetActualPoints2.add(tmp);
																			}
																		}
																	}

																} else {
																	SSAInstruction from = duchain.getDef(use);
																	if (from instanceof SSALoadMetadataInstruction) {
																		SSALoadMetadataInstruction f = (SSALoadMetadataInstruction) from;
																		TypeReference type = f.getType();
																		Object token0 = f.getToken();
																		if (token0 instanceof TypeReference) {
																			TypeReference token = (TypeReference) token0;
																			String name = token.getName().toString();
																			if ((node.getMethod().getDeclaringClass()
																					.getClassLoader().getReference()
																					.equals(ClassLoaderReference.Application)
																					&& ConfigUtil.enableApplication)
																					|| ConfigUtil
																							.isApplicationClass(name)) {
																				if (type.getName().toString()
																						.equals("Ljava/lang/Class")) {
																					if (actual_def.isStatic()) {
																						FrmkRetPoints2 tmp = new FrmkRetPoints2(
																								defInvoke, i + 1,
																								FrmkCallParamType.FQName_class);
																						frameworkRetActualPoints2
																								.add(tmp);
																					} else {
																						FrmkRetPoints2 tmp = new FrmkRetPoints2(
																								defInvoke, i,
																								FrmkCallParamType.FQName_class);
																						frameworkRetActualPoints2
																								.add(tmp);
																					}
																				}

																			}

																		}
																	}
																}
															}
														}
													}
												}

											}

										}
									} catch (InvalidClassFileException e) {
										e.printStackTrace();
									}

								}
							}

						}
					}
				}
			}
		});
	}

	private HashSet<String> findValue(String value) {
		HashSet<String> ret = new HashSet<>();

		// annotation
		for (infoUnit anno : annoSet) {
			List<infoPair> alls = anno.getFields();
			for (infoPair pair : alls) {
				Map<String, String> tag2val = pair.getValue();
				for (String key : tag2val.keySet()) {
					String val = tag2val.get(key);
					if (val != null && val.equals(value)) {
						String mark = pair.getMark();
						ret.add(mark);
					}
				}
			}
		}
		// xml
		ArrayList<String> tmp = resolveMarksUtil.getXMLMarks_common_allMatch(value, xmlSet);
		if (!tmp.isEmpty())
			ret.addAll(tmp);

		return ret;
	}

	public HashSet<FrmkRetPoints2> getFrameworkRetActualPoints2() {
		return frameworkRetActualPoints2;
	}

}
