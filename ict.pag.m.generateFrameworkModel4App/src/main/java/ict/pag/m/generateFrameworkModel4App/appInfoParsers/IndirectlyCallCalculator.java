package ict.pag.m.generateFrameworkModel4App.appInfoParsers;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.shrikeCT.AnnotationsReader.ElementValue;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.CancelException;

import ict.pag.m.frameworkInfoUtil.frameworkInfoInAppExtract.extractAnnos;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.AnnotationEntity;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.FrameworkCallsiteEntity;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.xmlClassNodeEntity;
import ict.pag.m.generateFrameworkModel4App.entity.FrameworkModel;
import ict.pag.m.generateFrameworkModel4App.util.SpecialHelper;
import ict.pag.m.marks2SAInfer.summarizeModels.CallsiteM2targetM;
import ict.pag.m.marks2SAInfer.util.CollectionUtil;

public class IndirectlyCallCalculator {
	private ClassHierarchy cha;
	CHACallGraph chaCG;

	private FrameworkModel frameworkModel;

	private AnnotationExtractor annoExtractor;
	private XMLExtractor xmlExtractor;
	private InheritanceExtractor inheExtractor;

	/* Framework information */
	Set<CallsiteM2targetM> indirectlyCallsEntities;

	/* Application information */
	HashMap<String, HashSet<AnnotationEntity>> mark2method_anno;
	HashMap<String, HashSet<IClass>> mark2class_inhe;
	HashMap<String, HashSet<xmlClassNodeEntity>> mark2class_xml;

	/** Answers */
	HashMap<FrameworkCallsiteEntity, HashSet<IMethod>> call2targets = new HashMap<>();

	public IndirectlyCallCalculator(ClassHierarchy cha, CHACallGraph chaCG2, FrameworkModel frameworkModel2,
			AnnotationExtractor anno, XMLExtractor xml, InheritanceExtractor inheExtractor2) {
		this.cha = cha;
		this.chaCG = chaCG2;
		this.frameworkModel = frameworkModel2;
		this.annoExtractor = anno;
		this.xmlExtractor = xml;
		this.inheExtractor = inheExtractor2;

		indirectlyCallsEntities = frameworkModel.getIndirectCalls();
		mark2method_anno = annoExtractor.getMark2methodSet();
		mark2class_inhe = inheExtractor.getMark2Class();
		mark2class_xml = xmlExtractor.getMark2classSet();
	}

	public int getIndirectlyCallsCount() {
		int count = 0;
		for (FrameworkCallsiteEntity key : call2targets.keySet()) {
			count = count + call2targets.get(key).size();
		}
		return count;
	}

	public void matches() {

		for (CallsiteM2targetM entity : indirectlyCallsEntities) {
			String invoke_frmk = entity.getCall();
			HashSet<String> callsiteMarks_frmk = entity.getCallsiteMarks();
			String targetType = entity.getTarget();
			HashSet<String> targetMarks_frmk = entity.getTargetMarks();

			for (String csm_frmk0 : callsiteMarks_frmk) {
				if (csm_frmk0.startsWith("inhe:full:")) {
					String csm_frmk1 = csm_frmk0.substring("inhe:full:".length());
					String csm_clazz = csm_frmk1.substring(0, csm_frmk1.lastIndexOf('.'));
//					String csm_mtd = csm_frmk1.substring(csm_frmk1.lastIndexOf('.') + 1);
					String csm_mtd = csm_frmk1.substring(csm_frmk1.lastIndexOf('.') + 1, csm_frmk1.indexOf('('));
					if (mark2class_inhe.containsKey(csm_clazz)) {
						HashSet<IClass> candidateMethods = mark2class_inhe.get(csm_clazz);
						for (IClass candidate : candidateMethods) {
							// whether has specific invoke in this method
							IMethod m = calculateSpecifiedInvoke(candidate, csm_mtd, invoke_frmk);
							if (m != null) {
								// has this invoke in method
								FrameworkCallsiteEntity cse = new FrameworkCallsiteEntity(m, invoke_frmk);
								if (targetType.equals("any")) {
									HashSet<IMethod> ret = findTargets(targetMarks_frmk);
									if (!ret.isEmpty()) {
										addIntoResults(cse, ret);
//										if (call2targets.containsKey(cse)) {
//											call2targets.get(cse).addAll(ret);
//										} else {
//											call2targets.put(cse, ret);
//										}
									}
								}
							}

						}
					}

				} else if (csm_frmk0.startsWith("anno:mtd:")) {
					String csm_frmk1 = csm_frmk0.substring("anno:mtd:".length());
					if (mark2method_anno.containsKey(csm_frmk1)) {
						HashSet<AnnotationEntity> candidateMethods = mark2method_anno.get(csm_frmk1);
						for (AnnotationEntity candidate : candidateMethods) {
							IMethod contextMtd = candidate.getMethod();
							if (hasSpecificInvoke(contextMtd, invoke_frmk)) {
								FrameworkCallsiteEntity cse = new FrameworkCallsiteEntity(contextMtd, invoke_frmk);

								// calculate targets
								if (targetType.equals("any")) {
									HashSet<IMethod> ret = findTargets(targetMarks_frmk);
									if (!ret.isEmpty()) {
										addIntoResults(cse, ret);
//										if (call2targets.containsKey(cse)) {
//											call2targets.get(cse).addAll(ret);
//										} else {
//											call2targets.put(cse, ret);
//										}
									}
								} else if (targetType.equals("annotation-value")) {
									HashSet<IMethod> ret = findTargets(targetMarks_frmk);
									for (String frmk : targetMarks_frmk) {
										if (frmk.startsWith("anno:mtd:")) {
											String tar_frmk1 = frmk.substring("anno:mtd:".length());
											if (mark2method_anno.containsKey(tar_frmk1)) {
												HashSet<AnnotationEntity> candidates = mark2method_anno.get(tar_frmk1);
												for (AnnotationEntity can : candidates) {
													Annotation tar_anno = can.getAnnotation();
													if (hasSameAnnotationValue(contextMtd, tar_anno)) {
														ret.add(can.getMethod());
													}
												}
											}
										}
									}

									if (!ret.isEmpty()) {
										addIntoResults(cse, ret);
//										if (call2targets.containsKey(cse)) {
//											call2targets.get(cse).addAll(ret);
//										} else {
//											call2targets.put(cse, ret);
//										}
									}
								}
							}
						}
					}
				}
			}
		}

	}

	private void addIntoResults(FrameworkCallsiteEntity cse, HashSet<IMethod> ret) {
		boolean has = false;
		for (FrameworkCallsiteEntity key : call2targets.keySet()) {
			if (key.equals(cse)) {
				has = true;
				call2targets.get(key).addAll(ret);
				break;
			}
		}
		if (!has) {
			call2targets.put(cse, ret);
		}

	}

	private boolean hasSameAnnotationValue(IMethod contextMtd, Annotation tar_anno) {
		HashSet<String> targetVals = calculateAnnotationAllValues(tar_anno);
		if (targetVals.isEmpty())
			return false;

		for (Annotation a1 : contextMtd.getAnnotations()) {
			HashSet<String> vals = calculateAnnotationAllValues(a1);
			if (!vals.isEmpty()) {
				Set<String> same = CollectionUtil.calSameElements(vals, targetVals);
				if (same != null && !same.isEmpty())
					return true;
			}
		}
		return false;
	}

	private HashSet<String> calculateAnnotationAllValues(Annotation anno) {
		HashSet<String> ret = new HashSet<>();
		if (anno.getNamedArguments() != null) {
			for (String key : anno.getNamedArguments().keySet()) {
				ElementValue val = anno.getNamedArguments().get(key);
				String val2 = extractAnnos.reFormatAnnosValue(val);
				if (val2 != null) {
					ret.add(val2);
				}
			}
		}
		return ret;

	}

	private HashSet<IMethod> findTargets(HashSet<String> targetMarks_frmk) {
		HashSet<IMethod> ret = new HashSet<IMethod>();
		for (String frmk : targetMarks_frmk) {
			if (frmk.startsWith("inhe:full:")) {
				String tar_frmk1 = frmk.substring("inhe:full:".length());
				String tar_clazz = tar_frmk1.substring(0, tar_frmk1.lastIndexOf('.'));
				String tar_mtd = tar_frmk1.substring(tar_frmk1.lastIndexOf('.') + 1);
				if (mark2class_inhe.containsKey(tar_clazz)) {
					HashSet<IClass> candidateMethods = mark2class_inhe.get(tar_clazz);
					for (IClass candidate : candidateMethods) {
						HashSet<IMethod> mset = SpecialHelper.findSpecificMethod(candidate, tar_mtd);
						if (!mset.isEmpty())
							ret.addAll(mset);
					}
				}
			} else if (frmk.startsWith("anno:mtd:")) {
				String tar_frmk1 = frmk.substring("anno:mtd:".length());
				if (mark2method_anno.containsKey(tar_frmk1)) {
					HashSet<AnnotationEntity> candidates = mark2method_anno.get(tar_frmk1);
					for (AnnotationEntity can : candidates) {
						ret.add(can.getMethod());
					}
				}

			}
		}

		return ret;
	}

//	private HashSet<IMethod> findSpecificMethod(IClass candidate, String tar_mtd) {
//		HashSet<IMethod> ret = new HashSet<IMethod>();
//		for (IMethod m : candidate.getAllMethods()) {
//			if (m.getSignature().contains(tar_mtd)) {
//				ret.add(m);
//			}
//		}
//		return ret;
//	}

	private boolean hasSpecificInvoke(IMethod mtd, String invoke_frmk) {
		try {
			CGNode cgNode = chaCG.findOrCreateNode(mtd, Everywhere.EVERYWHERE);
			IR ir = cgNode.getIR();
			if (ir != null) {
				SSAInstruction[] allInsts = ir.getInstructions();
				for (int i = 0; i < allInsts.length; i++) {
					SSAInstruction inst0 = allInsts[i];
					if (inst0 instanceof SSAInvokeInstruction) {
						SSAInvokeInstruction inst1 = (SSAInvokeInstruction) inst0;
						String targetSigs = inst1.getCallSite().getDeclaredTarget().getSignature();
						if (targetSigs.equals(invoke_frmk)) {
							return true;
						}
					}
				}
			}
		} catch (CancelException e) {
			e.printStackTrace();
		}
		return false;
	}

	private IMethod calculateSpecifiedInvoke(IClass clazz, String mtdSig, String invoke_frmk) {
		Iterator<IClass> iterator = cha.getLoader(ClassLoaderReference.Application).iterateAllClasses();
		while (iterator.hasNext()) {
			IClass c0 = iterator.next();
			if (c0.equals(clazz)) {
				Collection<? extends IMethod> allMtds = c0.getAllMethods();
				for (IMethod mtd : allMtds) {
					if (mtd.getSignature().contains(mtdSig)) {
						try {
							CGNode cgNode = chaCG.findOrCreateNode(mtd, Everywhere.EVERYWHERE);
							IR ir = cgNode.getIR();
							if (ir != null) {
								SSAInstruction[] allInsts = ir.getInstructions();
								for (int i = 0; i < allInsts.length; i++) {
									SSAInstruction inst0 = allInsts[i];
									if (inst0 instanceof SSAInvokeInstruction) {
										SSAInvokeInstruction inst1 = (SSAInvokeInstruction) inst0;
										String targetSigs = inst1.getCallSite().getDeclaredTarget().getSignature();
										if (targetSigs.equals(invoke_frmk)) {
//											return inst1;
											return mtd;
										}
									}

								}
							}
						} catch (CancelException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		return null;
	}

}
