package ict.pag.webframework.model.core.calculator;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.dom4j.Attribute;
import org.dom4j.Element;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.shrikeCT.AnnotationsReader.ElementValue;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.CancelException;

import ict.pag.webframework.model.enumeration.MarkScope;
import ict.pag.webframework.model.logprase.RTInfoDetailsClassifer;
import ict.pag.webframework.model.marks.MarksHelper;
import ict.pag.webframework.model.marks.NormalMark;
import ict.pag.webframework.model.marks.Points2Mark;
import ict.pag.webframework.model.marks.ResolveMarks;
import ict.pag.webframework.model.option.ConfigUtil;
import ict.pag.webframework.model.option.SpecialHelper;

public class FieldInjectCalculator2 {

	private ClassHierarchy cha;
	private CHACallGraph chaCG;

	private HashMap<String, HashSet<Element>> class2xmlEle;
	private RTInfoDetailsClassifer classfier;
	private HashMap<String, Set<ArrayList<String>>> outer2RTSeq;

	private HashSet<IField> rtUsedField = new HashSet<>();

	/**/
	private HashSet<IField> injectedFields = new HashSet<>();
	private HashSet<IMethod> injectFieldMethods = new HashSet<>();
	private HashMap<IField, HashSet<IMethod>> injectedFields2InjectFieldsMtds = new HashMap<>();

	private HashSet<IField> notInjectedFields = new HashSet<>();
	private HashSet<IMethod> notInjectFieldMethods = new HashSet<>();

	private HashSet<IClass> managedClass = new HashSet<>();

	private HashMap<IField, HashSet<String>> baseField2RTClass = new HashMap<>();
	private HashMap<IField, HashSet<String>> usedField2RTClass = new HashMap<>();
	/* the field not non-reference type */
	private HashMap<IField, HashSet<String>> allField2RTClass = new HashMap<>();

	/** Answer */
	private HashSet<NormalMark> fieldInjectMarks = new HashSet<>();
	private HashSet<NormalMark> fieldNOTInjectMarks = new HashSet<>();

	private HashSet<Points2Mark> fieldPoints2Marks = new HashSet<>();
	private HashSet<Points2Mark> fieldNOTPoints2Marks = new HashSet<>();

	private HashSet<String> classAliasMarks = new HashSet<>();
	private HashSet<NormalMark> managedClassMarks = new HashSet<>();

	private HashSet<IField> rtAllOperatedFields = new HashSet<>();

	public FieldInjectCalculator2(ClassHierarchy cha, CHACallGraph chaCG,
			HashMap<String, HashSet<Element>> class2xmlEle, RTInfoDetailsClassifer classfier,
			HashMap<String, Set<ArrayList<String>>> outer2rtSeq) {
		this.cha = cha;
		this.chaCG = chaCG;
		this.class2xmlEle = class2xmlEle;
		this.classfier = classfier;
		this.outer2RTSeq = outer2rtSeq;

		calculate();
	}

	public void calculate() {
		collectFieldRTInfos();

		calculateInjectedFieldInfos();
		calculateNotInjectedFieldInfos();

		calculateClassManagedInfos();

		calculateMarks();

	}

	private void collectFieldRTInfos() {

		for (String outer : outer2RTSeq.keySet()) {
			if (outer.equals("outer"))
				continue;
			for (ArrayList<String> seq : outer2RTSeq.get(outer)) {
				for (String stmt : seq) {
					/* base field info */
					if (stmt.startsWith("[base field]")) {
						String str1 = stmt.substring("[base field]".length());
						String[] strs = str1.split(":");
						String fieldName = strs[0];
						String fieldRTType = strs[2];
						String clazz = outer.substring(0, outer.lastIndexOf('.'));
						String fieldSig = clazz + "." + fieldName + ":" + SpecialHelper.reformatSignature(strs[1]);

						IField f = genIField(fieldSig);
						if (f != null) {
							if (baseField2RTClass.containsKey(f)) {
								baseField2RTClass.get(f).add(fieldRTType);
							} else {
								HashSet<String> tmp = new HashSet<>();
								tmp.add(fieldRTType);
								baseField2RTClass.put(f, tmp);
							}

							if (allField2RTClass.containsKey(f)) {
								allField2RTClass.get(f).add(fieldRTType);
							} else {
								HashSet<String> tmp = new HashSet<>();
								tmp.add(fieldRTType);
								allField2RTClass.put(f, tmp);
							}
						}
					}
					/* field read */
					if (stmt.startsWith("[field read]")) {
						if (stmt.contains("[runtimeType]")) {
							String fieldSig = stmt.substring(stmt.indexOf("[signature]") + "[signature]".length(),
									stmt.indexOf("[runtimeType]"));
							String fieldName = fieldSig.substring(fieldSig.lastIndexOf('.') + 1, fieldSig.indexOf(':'));

							String str1 = stmt.substring(stmt.indexOf("[runtimeType]") + "[runtimeType]".length(),
									stmt.lastIndexOf("[base]"));
							String rtType;
							if (str1.contains("[collection]")) {
								rtType = str1.substring(0, str1.indexOf("[collection]"));
							} else {
								rtType = str1;
							}

							IField f = genIField(fieldSig);
							if (f != null) {
								rtUsedField.add(f);

								if (usedField2RTClass.containsKey(f)) {
									usedField2RTClass.get(f).add(rtType);
								} else {
									HashSet<String> tmp = new HashSet<>();
									tmp.add(rtType);
									usedField2RTClass.put(f, tmp);
								}

								if (allField2RTClass.containsKey(f)) {
									allField2RTClass.get(f).add(rtType);
								} else {
									HashSet<String> tmp = new HashSet<>();
									tmp.add(rtType);
									allField2RTClass.put(f, tmp);
								}
							}

						}
					} else {
						if (stmt.startsWith("[field write]")) {
							if (stmt.contains("[runtimeType]")) {
								// write
								String fieldSig = stmt.substring(stmt.indexOf("[signature]") + "[signature]".length(),
										stmt.indexOf("[runtimeType]"));
								String fieldName = fieldSig.substring(fieldSig.lastIndexOf('.') + 1,
										fieldSig.indexOf(':'));
								String writeContent = stmt.substring(
										stmt.indexOf("[runtimeType]") + "[runtimeType]".length(),
										stmt.lastIndexOf("[base]"));
								String fieldDeclareClass = fieldSig.substring(fieldSig.indexOf(':') + 1);

								IField f = genIField(fieldSig);
								if (f != null) {
									rtUsedField.add(f);

									if (usedField2RTClass.containsKey(f)) {
										usedField2RTClass.get(f).add(writeContent);
									} else {
										HashSet<String> tmp = new HashSet<>();
										tmp.add(writeContent);
										usedField2RTClass.put(f, tmp);
									}

									if (allField2RTClass.containsKey(f)) {
										allField2RTClass.get(f).add(writeContent);
									} else {
										HashSet<String> tmp = new HashSet<>();
										tmp.add(writeContent);
										allField2RTClass.put(f, tmp);
									}
								}

							}
						}
					}
				}
			}
		}

		/* field runtime type may influenced by dynamic proxy */
		for (String fieldSig : classfier.getField2ActualUsed().keySet()) {
			IField f = genIField(fieldSig);
			if (f != null) {
				HashSet<String> actualClasses = classfier.getField2ActualUsed().get(fieldSig);
				if (usedField2RTClass.containsKey(f)) {
					usedField2RTClass.get(f).addAll(actualClasses);
				} else {
					usedField2RTClass.put(f, actualClasses);
				}

				if (allField2RTClass.containsKey(f)) {
					allField2RTClass.get(f).addAll(actualClasses);
				} else {
					allField2RTClass.put(f, actualClasses);
				}
			}
		}
	}

	private void calculateInjectedFieldInfos() {
		/* put field function set field */
		for (String call : classfier.getOuterCalls()) {
//		for (String call : classfier.getNoMatchedCallsiteCalls()) {
			if (outer2RTSeq.containsKey(call))
				for (ArrayList<String> seq : outer2RTSeq.get(call)) {
					HashMap<String, String> field2InitValue = new HashMap<>();
					for (String stmt : seq) {
						if (stmt.startsWith("[base field]")) {
							String str1 = stmt.substring("[base field]".length());
							String[] strs = str1.split(":");
							field2InitValue.put(strs[0], strs[2]);
						}
						if (stmt.startsWith("[field write]")) {
							if (stmt.contains("[runtimeType]")) {
								// write
								String fieldSig = stmt.substring(stmt.indexOf("[signature]") + "[signature]".length(),
										stmt.indexOf("[runtimeType]"));
								String fieldName = fieldSig.substring(fieldSig.lastIndexOf('.') + 1,
										fieldSig.indexOf(':'));
								String writeContent = stmt.substring(
										stmt.indexOf("[runtimeType]") + "[runtimeType]".length(),
										stmt.lastIndexOf("[base]"));
								String fieldDeclareClass = fieldSig.substring(fieldSig.indexOf(':') + 1);

								if (isValidInjectType(fieldDeclareClass))
									if (!field2InitValue.containsKey(fieldName)
											|| (field2InitValue.get(fieldName).equals("null")
													&& !writeContent.equals("null"))) {
										IField f = genIField(fieldSig);
										IMethod m = genIMethod(call);

										if (f != null) {
											boolean hasInitValue = false;
											IClass declareClass = m.getDeclaringClass();
											for (IMethod i : declareClass.getDeclaredMethods()) {
												if (i.isInit()) {
													try {
														CGNode cgNode = chaCG.findOrCreateNode(i,
																Everywhere.EVERYWHERE);
														IR ir = cgNode.getIR();
														if (ir != null) {
															SSAInstruction[] insts = ir.getInstructions();
															for (SSAInstruction inst : insts) {
																if (inst instanceof SSAPutInstruction) {
																	SSAPutInstruction putInst = (SSAPutInstruction) inst;
																	FieldReference initField = putInst
																			.getDeclaredField();
																	if (initField.equals(f.getReference())) {
																		/* private String s = "str"; */
																		int nums = i.getNumberOfParameters();
																		for (int use = 1; use < putInst
																				.getNumberOfUses(); use++) {
																			int u = putInst.getUse(use);
																			// put field use not the parameter
																			if (u > nums) {
																				// be assigned in init field
																				hasInitValue = true;
																				break;
																			}
																			SymbolTable symTable = ir.getSymbolTable();
																			if (symTable.isNullConstant(u)) {
																				hasInitValue = true;
																				break;
																			}
																		}
																	}
																}
															}

														}
													} catch (CancelException e) {
														e.printStackTrace();
													}
												}
											}

											if (!hasInitValue) {
												injectedFields.add(f);
												rtAllOperatedFields.add(f);
												if (m != null && isSetFieldFunction(m)) {
													injectFieldMethods.add(m);

													if (injectedFields2InjectFieldsMtds.keySet().contains(f)) {
														injectedFields2InjectFieldsMtds.get(f).add(m);
													} else {
														HashSet<IMethod> tmp = new HashSet<>();
														tmp.add(m);
														injectedFields2InjectFieldsMtds.put(f, tmp);
													}
												}
											}
										}
									}
							} else {
								// null
							}
						}
					}
				}
		}

//		HashSet<IField> ignorefields = new HashSet<>();
//		for (String call : classfier.getNoMatchedCallsiteCalls_followFrameCallsite()) {
//			IMethod m = genIMethod(call);
//			if (m == null)
//				continue;
//			try {
//				CGNode cgNode = chaCG.findOrCreateNode(m, Everywhere.EVERYWHERE);
//				IR ir = cgNode.getIR();
//				if (ir != null) {
//					SSAInstruction[] insts = ir.getInstructions();
//					for (SSAInstruction inst : insts) {
//						if (inst instanceof SSAPutInstruction) {
//							SSAPutInstruction putInst = (SSAPutInstruction) inst;
//							FieldReference fref = putInst.getDeclaredField();
//							IField f = cha.resolveField(fref);
//							ignorefields.add(f);
//						}
//					}
//				}
//			} catch (CancelException e) {
//				e.printStackTrace();
//			}
//		}
		/* init function / set field */
		// for all runtime used fields:
		// 1. there is no set field method for this field;
		// 2. there is setfield method, but it called by framework
		for (IField f : rtUsedField) {
			boolean noCaller = true;
			boolean noSetFieldMtd = true;

//			if (ignorefields.contains(f))
//				continue;

			String decType = f.getReference().getFieldType().getName().toString();
			if (!isValidInjectType(decType))
				continue;

			HashSet<IMethod> allInitMethods = new HashSet<>();
			for (IMethod m : f.getDeclaringClass().getDeclaredMethods()) {
				if (m.isInit() || m.getName().toString().toLowerCase().startsWith("set")) {
					try {
						CGNode cgNode = chaCG.findOrCreateNode(m, Everywhere.EVERYWHERE);
						IR ir = cgNode.getIR();
						if (ir != null) {
							SSAInstruction[] insts = ir.getInstructions();
							for (SSAInstruction inst : insts) {
								if (inst instanceof SSAPutInstruction) {
									SSAPutInstruction putInst = (SSAPutInstruction) inst;
									FieldReference initField = putInst.getDeclaredField();
									if (initField.equals(f.getReference())) {
										noSetFieldMtd = false;

										if (m.isInit()) {
											/* private String s = "str"; */
											int nums = m.getNumberOfParameters();
											for (int use = 1; use < putInst.getNumberOfUses(); use++) {
												int u = putInst.getUse(use);
												// put field use not the parameter
												if (u > nums) {
													noCaller = false;
													break;
												} else {
													noSetFieldMtd = true;
												}
												SymbolTable symTable = ir.getSymbolTable();
												if (symTable.isNullConstant(u)) {
													noCaller = false;
													break;
												}
											}
//											/* private String s = "str"; */
//											noCaller = false;
//											break;
										}

										Integer preCount = 0;
										Iterator<CGNode> it = chaCG.getPredNodes(cgNode);
										while (it.hasNext()) {
											CGNode pre = it.next();
											if (pre.getMethod().getDeclaringClass().getClassLoader().getReference()
													.equals(ClassLoaderReference.Application)) {
												// not in the same class
												IClass preClass = pre.getMethod().getDeclaringClass();
												IClass currClass = m.getDeclaringClass();
												if (preClass.equals(currClass)
														|| cha.isAssignableFrom(currClass, preClass))
//												if (pre.getMethod().getDeclaringClass().equals(m.getDeclaringClass())
//														|| cha.isAssignableFrom(pre.getMethod().getDeclaringClass(),
//																m.getDeclaringClass())
//														|| cha.isAssignableFrom(m.getDeclaringClass(),
//																pre.getMethod().getDeclaringClass()))
													preCount++;
											}
										}
										allInitMethods.add(m);
										if (preCount > 1) {
											noCaller = false;
											break;
										}
									}
								}
							}
						}

					} catch (CancelException e) {
						e.printStackTrace();
					}
				}

			}

			if (noSetFieldMtd && noCaller) {
				injectedFields.add(f);
				rtAllOperatedFields.add(f);
				if (!allInitMethods.isEmpty()) {
					injectFieldMethods.addAll(allInitMethods);
					if (injectedFields2InjectFieldsMtds.keySet().contains(f)) {
						injectedFields2InjectFieldsMtds.get(f).addAll(allInitMethods);
					} else {
						injectedFields2InjectFieldsMtds.put(f, allInitMethods);
					}

				}

			}
		}

	}

	private boolean isValidInjectType(String fieldDeclareClass) {
//		if (fieldDeclareClass.startsWith("Ljava/lang"))
//			return false;
		if (fieldDeclareClass.equals("I") || fieldDeclareClass.equals("J") || fieldDeclareClass.equals("F")
				|| fieldDeclareClass.equals("D") || fieldDeclareClass.equals("B") || fieldDeclareClass.equals("C")
				|| fieldDeclareClass.equals("S") || fieldDeclareClass.equals("Z"))
			return false;

		return true;
	}

	private void calculateNotInjectedFieldInfos() {
		// 1. not inject marks on what: runtime always null
		for (IField f : allField2RTClass.keySet()) {
//		for (IField f : baseField2RTClass.keySet()) {
			boolean allNull = true;
//			for (String val : baseField2RTClass.get(f)) {
			for (String val : allField2RTClass.get(f)) {
				if (!val.equals("null")) {
					allNull = false;
					break;
				}
			}
			if (allNull) {
				notInjectedFields.add(f);
			}
		}

		// 2. put field function called
		HashSet<String> hasCallers = new HashSet<>();
		if (!classfier.getHasMatchedCallsiteCalls().isEmpty())
			hasCallers.addAll(classfier.getHasMatchedCallsiteCalls());
		if (!classfier.getHasMatchedCallsiteCalls_NotInSameClass().isEmpty())
			hasCallers.addAll(classfier.getHasMatchedCallsiteCalls_NotInSameClass());
		if (!classfier.getNoMatchedCallsiteCalls_followCallsite().isEmpty())
			hasCallers.addAll(classfier.getNoMatchedCallsiteCalls_followCallsite());

		for (String call : hasCallers) {
			IMethod m = genIMethod(call);
			if (m != null) {
				try {
					CGNode node = chaCG.findOrCreateNode(m, Everywhere.EVERYWHERE);
					IR ir = node.getIR();
					if (ir != null) {
						SSAInstruction[] allInsts = ir.getInstructions();
						for (SSAInstruction inst : allInsts) {
							if (inst instanceof SSAPutInstruction) {
								SSAPutInstruction putInst = (SSAPutInstruction) inst;
								FieldReference f = putInst.getDeclaredField();
								IField field = cha.resolveField(f);
								if (field != null) {
									notInjectedFields.add(field);
									notInjectFieldMethods.add(m);
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

	/**
	 * 1. base object created by framework; </br>
	 * 2. field object that created by framework
	 */
	private void calculateClassManagedInfos() {
		for (String call : classfier.getNoMatchedCallsiteCalls()) {
			IMethod m = genIMethod(call);
			if (m != null) {
				managedClass.add(m.getDeclaringClass());
			}
		}

		for (IField key : usedField2RTClass.keySet()) {
			HashSet<String> classStrSet = usedField2RTClass.get(key);
			for (String classStr : classStrSet) {
				String clazz = "L" + classStr.replaceAll("\\.", "/");
				IClass givenClass = cha.lookupClass(
						TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
				if (givenClass == null)
					continue;
				managedClass.add(givenClass);

			}
		}
	}

	private void calculateMarks() {
		HashMap<IField, NormalMark> fieldHasConfiguration = new HashMap<>();
//		HashSet<IField> fieldNoConfiguration = new HashSet<>();
//		HashSet<IClass> fieldNoConfigurationClass = new HashSet<>();
//
//		/* inject */
//		/* on method */
//		for (IField f : injectedFields2InjectFieldsMtds.keySet()) {
//			HashSet<IMethod> methods = injectedFields2InjectFieldsMtds.get(f);
//			for (IMethod m : methods) {
//				// put method mark
//				NormalMark nor = genNormalMark4fieldInitMethod(m);
//				if (!nor.isAllEmpty()) {
//					fieldHasConfiguration.put(f, nor);
//				}
//			}
//			NormalMark nor2 = genNormalMark4field(f);
//			if (!nor2.isAllEmpty()) {
//				fieldHasConfiguration.put(f, nor2);
//			}
//		}
//		/* on field */
//		for (IField f : injectedFields) {
//			NormalMark nor = genNormalMark4field(f);
//			if (!nor.isAllEmpty()) {
//				fieldHasConfiguration.put(f, nor);
//			} else {
//				if (!fieldHasConfiguration.keySet().contains(f)) {
//					fieldNoConfiguration.add(f);
//					fieldNoConfigurationClass.add(f.getDeclaringClass());
//				}
//			}
//		}
//		/* add into answer */
//		for (IField f : fieldHasConfiguration.keySet()) {
//			if (!fieldNoConfigurationClass.contains(f.getDeclaringClass())) {
//				fieldInjectMarks.add(fieldHasConfiguration.get(f));
//			}
//		}

		/* inject field */
		for (IField f : injectedFields) {
			NormalMark nor = genNormalMark4field(f);
			if (!nor.isAllEmpty()) {
				fieldInjectMarks.add(nor);
				fieldHasConfiguration.put(f, nor);

				HashSet<String> marks = new HashSet<>();
				HashSet<String> xmlEles = ResolveMarks.resolve_XML_String_notGivenAttr(f, MarkScope.Field,
						class2xmlEle);
				for (String ele : xmlEles) {
					marks.add("[field][xml]" + ele);
				}
				if (!marks.isEmpty())
					fieldNOTInjectMarks.add(new NormalMark(marks, MarkScope.Field));
			}
		}

		/* inject field methods */
		for (IMethod m : injectFieldMethods) {
			NormalMark nor = genNormalMark4fieldInitMethod(m);
			if (!nor.isAllEmpty()) {
				fieldInjectMarks.add(nor);
			}
		}

		/* not inject marks */
		for (IField f : notInjectedFields) {
			NormalMark nor = genNormalMark4field(f);
			if (!nor.isAllEmpty())
				fieldNOTInjectMarks.add(nor);
		}
		for (IMethod m : notInjectFieldMethods) {
			NormalMark nor = genNormalMark4fieldInitMethod(m);
			if (!nor.isAllEmpty()) {
				fieldNOTInjectMarks.add(nor);
			}
		}
//		for (IField f : fieldHasConfiguration.keySet()) {
//			if (fieldNoConfigurationClass.contains(f.getDeclaringClass())) {
//				fieldNOTInjectMarks.add(fieldHasConfiguration.get(f));
//			}
//		}

		/* points to and alias */
		for (IField field : allField2RTClass.keySet()) {
			// for each field, attribute below
			// 1. Actual Type is Declare Type
			boolean isDecType = false;
			// 2. Actual type is configured by value directly
			HashSet<String> confDirectlySet = new HashSet<>();
			// 3. Actual type is configured by value, but the value is alias
			// (field name may also served as alias to index the actual type)
			HashSet<String> fieldConfiguredAliasSet = new HashSet<>();
			HashSet<String> classAliasConfs = new HashSet<>();

			String fieldName = field.getName().toString();
			String fieldTypeStr = SpecialHelper
					.formatSignature(field.getReference().getFieldType().getName().toString());
			HashSet<Annotation> annos_field = ResolveMarks.resolve_Annotation(field, MarkScope.Field);
			HashSet<Element> xmlEles_field = ResolveMarks.resolve_XML(field, MarkScope.Field, class2xmlEle);

			boolean allNull = true;
			for (String rtType : allField2RTClass.get(field)) {
				// only run time is application class, calculate
				String clazz = "L" + rtType.replaceAll("\\.", "/");
				IClass givenClass = cha.lookupClass(
						TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
				if (givenClass == null)
					continue;

				allNull = false;
				if (givenClass.getClassLoader().getReference().equals(ClassLoaderReference.Primordial)) {
					if (injectedFields.contains(field)) {
						NormalMark nor = genNormalMark4field(field);
						if (!nor.isAllEmpty()) {
							confDirectlySet.add("[Primordial]");
							Points2Mark mark = new Points2Mark(true, nor.getAllMarks(), confDirectlySet,
									fieldConfiguredAliasSet);
							fieldPoints2Marks.add(mark);
						}
					}
					continue;
				}

				if (!rtType.equals("null")) {
					if (fieldTypeStr.equals(rtType)) {
						isDecType = true;
					}

					// a. field-anno-values
					for (Annotation anno : annos_field) {
						Map<String, ElementValue> map = anno.getNamedArguments();
						for (String key : map.keySet()) {
							ElementValue val0 = map.get(key);
							String val = SpecialHelper.reFormatAnnosValue(val0);

							if (val != null) {

								// rt like "com.example.service.Service"
								if (val.toLowerCase().equals(rtType.toLowerCase())) {
									confDirectlySet.add("[anno]"
											+ SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno))
											+ "[attr]" + key);
								} else {
									findValueAliasConfigured(
											"[anno]" + SpecialHelper.formatSignature(
													MarksHelper.resolveAnnotationName(anno)) + "[attr]" + key,
											val, rtType, fieldConfiguredAliasSet, classAliasConfs);
								}
							}
						}
					}
					// b. field-xml-values
					HashSet<String> noMatchedAttrs = new HashSet<>();
					for (Element xmlEle : xmlEles_field) {
						String path = xmlEle.getName();
						Element parent = xmlEle.getParent();
						while (parent != null) {
							path = parent.getName().concat(";").concat(path);
							parent = parent.getParent();
						}

						boolean findAttr = false;
						HashSet<String> tmp = new HashSet<>();

						boolean find = false;
						// this layer
						for (Object attr0 : xmlEle.attributes()) {
							if (attr0 instanceof Attribute) {
								String val = ((Attribute) attr0).getValue();
								String name = ((Attribute) attr0).getName();

								String fieldConf = "[xml]" + path + ":" + name;
								if (val.toLowerCase().equals(rtType.toLowerCase())) {
									confDirectlySet.add(fieldConf);
								} else {
									find = findValueAliasConfigured(fieldConf, val, rtType, fieldConfiguredAliasSet,
											classAliasConfs);
								}

								if (find) {
									findAttr = true;
								} else {
									tmp.add(fieldConf);
								}

							}
						}
						if (findAttr)
							if (!tmp.isEmpty())
								noMatchedAttrs.addAll(tmp);
						tmp.clear();
						// sub-layer
						if (!find) {
							for (Object child0 : xmlEle.elements()) {
								if (child0 instanceof Element) {
									findAttr = false;
									for (Object attr0 : ((Element) child0).attributes()) {
										String path2 = path.concat(";").concat(((Element) child0).getName());
										if (attr0 instanceof Attribute) {
											String val = ((Attribute) attr0).getValue();
											String name = ((Attribute) attr0).getName();

											String fieldConf = "[xml]" + path2 + ":" + name;
											if (val.toLowerCase().equals(rtType.toLowerCase())) {
												confDirectlySet.add(fieldConf);
											} else {
												find = findValueAliasConfigured(fieldConf, val, rtType,
														fieldConfiguredAliasSet, classAliasConfs);
											}
											if (find) {
												findAttr = true;
											} else {
												tmp.add(fieldConf);
											}
										}
									}
									if (findAttr)
										if (!tmp.isEmpty())
											noMatchedAttrs.addAll(tmp);
									tmp.clear();
								}
							}
						}

					}
					if (!noMatchedAttrs.isEmpty())
						fieldNOTPoints2Marks.add(new Points2Mark(isDecType, new HashSet<String>(), noMatchedAttrs));

//					// c. field-name-value as alias to index
//					findValueAliasConfigured("[name][FieldSimpleName]", fieldName, rtType, fieldConfiguredAliasSet,
//							classAliasConfs);
				}
			}

			Points2Mark mark = new Points2Mark(isDecType, confDirectlySet, fieldConfiguredAliasSet);
			if (!mark.isAllEmpty()) {
				if (injectedFields.contains(field)) {
					if (fieldHasConfiguration.containsKey(field)) {
						mark = new Points2Mark(isDecType, fieldHasConfiguration.get(field).getAllMarks(),
								confDirectlySet, fieldConfiguredAliasSet);
					} else {
						mark = new Points2Mark(isDecType, new HashSet<String>(), confDirectlySet,
								fieldConfiguredAliasSet);
					}
					fieldPoints2Marks.add(mark);
					if (!classAliasConfs.isEmpty())
						classAliasMarks.addAll(classAliasConfs);
				} else {
					fieldNOTPoints2Marks.add(mark);
				}
			} else {
				if (injectedFields.contains(field)) {
					NormalMark nor = genNormalMark4field(field);
					if (!nor.isAllEmpty()) {
						if (allNull)
							confDirectlySet.add("[Proxy]");

						Points2Mark m = new Points2Mark(isDecType, nor.getAllMarks(), confDirectlySet,
								fieldConfiguredAliasSet);
						fieldPoints2Marks.add(m);

					}
				}
			}

		}

		/* managed class */
		for (IClass c : managedClass) {
			HashSet<String> tmp = new HashSet<>();
			HashSet<Annotation> annos_class0 = ResolveMarks.resolve_Annotation(c, MarkScope.Clazz);
			for (Annotation anno : annos_class0) {
				tmp.add("[class][anno]" + SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
			}
			HashSet<String> xmlEles_class = ResolveMarks.resolve_XML_String(c, MarkScope.Clazz, class2xmlEle);
			for (String ele : xmlEles_class) {
				tmp.add("[class][xml]" + ele);
			}
			/* 0311: for managed class, do not care about the inheritance */
//			if (ConfigUtil.marksLevel == 0 || (ConfigUtil.marksLevel == 1 && tmp.isEmpty())) {
//				for (String i : ResolveMarks.resolve_ExtendOrImplement(c, MarkScope.Clazz, cha)) {
//					tmp.add("[class][inheritance]" + SpecialHelper.formatSignature(i));
//				}
//			}

			if (!tmp.isEmpty())
				managedClassMarks.add(new NormalMark(tmp, MarkScope.Clazz));
		}

	}

	/**
	 * @param confName : the name of configuration for search
	 * @param val      : value of confName
	 * @param rtType   : run time field type
	 */
	private boolean findValueAliasConfigured(String confName, String val, String rtType,
			HashSet<String> fieldConfiguredAliasSet, HashSet<String> classAliasConfs) {
		boolean find = false;
		// the value of annotation equals:
		// a. class name
		// b. class configured value : i. anno-value; ii. xml-value
		String rtTypeClassName = rtType.substring(rtType.lastIndexOf('.') + 1);
//		if (val.equals(rtTypeClassName) || val.equals(rtTypeClassName.toLowerCase())) {
		if (val.equals(rtTypeClassName)) {
			classAliasConfs.add("[name][ClassSimpleName]");
			fieldConfiguredAliasSet.add(confName);
			find = true;
		}

		String clazz = "L" + rtType.replaceAll("\\.", "/");
		IClass givenClass = cha.lookupClass(
				TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
		if (givenClass == null)
			return find;

		HashSet<Annotation> annos_class = ResolveMarks.resolve_Annotation(givenClass, MarkScope.Clazz);
		HashSet<Element> xmlEles_class = ResolveMarks.resolve_XML(givenClass, MarkScope.Clazz, class2xmlEle);

		if (val.toLowerCase().equals("true") || val.toLowerCase().equals("false"))
			return find;

		// i. class annotation
		for (Annotation c_anno : annos_class) {
			for (String c_key : c_anno.getNamedArguments().keySet()) {
				ElementValue c_val0 = c_anno.getNamedArguments().get(c_key);
				String c_val = SpecialHelper.reFormatAnnosValue(c_val0);
				if (c_val != null && !c_val.isEmpty() && c_val.toLowerCase().equals(val.toLowerCase())) {
					// find (field-configured-value == class-anno-val)
					classAliasConfs
							.add("[anno]" + SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(c_anno))
									+ "[attr]" + c_key);
					fieldConfiguredAliasSet.add(confName);
					find = true;

					/* 0317: if find configured, remove class simple Name */
					if (classAliasConfs.contains("[name][ClassSimpleName]"))
						classAliasConfs.remove("[name][ClassSimpleName]");
				}
			}
		}
		// ii. class xml

		HashSet<Element> hasMatchedParent = new HashSet<>();
		for (Element xmlEle : xmlEles_class) {
			String path = xmlEle.getName();
			Element parent = xmlEle.getParent();

			while (parent != null) {
				// search in parent
				for (Object attr0 : parent.attributes()) {
					if (attr0 instanceof Attribute) {
						String c_val = ((Attribute) attr0).getValue();

						if (c_val != null) {
							if (c_val.equals(val)) {
								hasMatchedParent.add(parent);
							}
						}
					}
				}

				path = parent.getName().concat(";").concat(path);
				parent = parent.getParent();
			}
			String aliasPath = null;
			HashSet<String> tmp = new HashSet<>();
			for (Object attr0 : xmlEle.attributes()) {
				if (attr0 instanceof Attribute) {
					String c_val = ((Attribute) attr0).getValue();
					String c_name = ((Attribute) attr0).getName();
//					if (c_val != null && c_val.toLowerCase().equals(val.toLowerCase())) {
					if (c_val != null) {
						if (c_val.equals(val)) {
							aliasPath = "[xml]" + path + ":" + c_name;
							// find (field-configured-value == class-xml-val)
							classAliasConfs.add(aliasPath);
							fieldConfiguredAliasSet.add(confName);
							find = true;
							/* 0317: if find configured, remove class simple Name */
							if (classAliasConfs.contains("[name][ClassSimpleName]"))
								classAliasConfs.remove("[name][ClassSimpleName]");

						} else {
							String path3 = "[xml]" + path + ":" + c_name;
							tmp.add(path3);
						}
					}
				}
			}
		}

		/* 0317: if not find in current level, find the parent in this trace */
		if (!find) {
			for (Element xmlEle : hasMatchedParent) {
				String path = xmlEle.getName();
				Element parent = xmlEle.getParent();
				while (parent != null) {
					path = parent.getName().concat(";").concat(path);
					parent = parent.getParent();
				}

				String aliasPath = null;
				for (Object attr0 : xmlEle.attributes()) {
					if (attr0 instanceof Attribute) {
						String c_val = ((Attribute) attr0).getValue();
						String c_name = ((Attribute) attr0).getName();
//						if (c_val != null && c_val.toLowerCase().equals(val.toLowerCase())) {
						if (c_val != null) {
							if (c_val.equals(val)) {
								aliasPath = "[xml]" + path + ":" + c_name;
								// find (field-configured-value == class-xml-val)
								classAliasConfs.add(aliasPath);
								fieldConfiguredAliasSet.add(confName);
								find = true;

								/* 0317: if find configured, remove class simple Name */
								if (classAliasConfs.contains("[name][ClassSimpleName]"))
									classAliasConfs.remove("[name][ClassSimpleName]");

							}
						}
					}
				}
			}
		}
		return find;
	}

	private NormalMark genNormalMark4fieldInitMethod(IMethod mtd) {
		HashSet<String> marks = new HashSet<>();
		HashSet<Annotation> annos = ResolveMarks.resolve_Annotation(mtd, MarkScope.Method);
		for (Annotation anno : annos) {
			marks.add("[method][anno]" + SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
		}
		HashSet<String> xmlEles = ResolveMarks.resolve_XML_String(mtd, MarkScope.Method, class2xmlEle);
		for (String ele : xmlEles) {
			marks.add("[method][xml]" + ele);
		}
		return new NormalMark(marks, MarkScope.Method);
	}

	private NormalMark genNormalMark4field(IField f) {
		HashSet<String> marks = new HashSet<>();
		HashSet<Annotation> annos = ResolveMarks.resolve_Annotation(f, MarkScope.Field);
		for (Annotation anno : annos) {
			marks.add("[field][anno]" + SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
		}
		HashSet<String> xmlEles = ResolveMarks.resolve_XML_String(f, MarkScope.Field, class2xmlEle);
		for (String ele : xmlEles) {

			marks.add("[field][xml]" + ele);
		}
		return new NormalMark(marks, MarkScope.Field);
	}

	private IMethod genIMethod(String call) {
		String s = call.substring(0, call.lastIndexOf('.'));

		String clazz = "L" + s.replaceAll("\\.", "/");
		IClass givenClass = cha.lookupClass(
				TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
		if (givenClass == null)
			return null;

		for (IMethod m : givenClass.getDeclaredMethods()) {
			if (m.getSignature().equals(call)) {
				return m;
			}
		}

		return null;
	}

	private IField genIField(String fieldSig) {
		String s = fieldSig.substring(0, fieldSig.lastIndexOf('.'));
		String fieldName = fieldSig.substring(fieldSig.lastIndexOf('.') + 1, fieldSig.indexOf(':'));

		String clazz = "L" + s.replaceAll("\\.", "/");
		IClass givenClass = cha.lookupClass(
				TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
		if (givenClass == null)
			return null;

		for (IField f : givenClass.getAllInstanceFields()) {
			String name = f.getReference().getName().toString();
			if (name.equals(fieldName))
				return f;
		}

		return null;
	}

	private boolean isSetFieldFunction(IMethod mtd) {
		if (mtd.getName().toString().toLowerCase().startsWith("set"))
			return true;
		return false;
	}

	public HashSet<NormalMark> getFieldInjectMarks() {
		return fieldInjectMarks;
	}

	public HashSet<NormalMark> getFieldNOTInjectMarks() {
		return fieldNOTInjectMarks;
	}

	public HashSet<Points2Mark> getFieldPoints2Marks() {
		return fieldPoints2Marks;
	}

	public HashSet<Points2Mark> getFieldNOTPoints2Marks() {
		return fieldNOTPoints2Marks;
	}

	public HashSet<String> getClassAliasMarks() {
		return classAliasMarks;
	}

	public HashSet<IMethod> getInjectFieldMethods() {
		return injectFieldMethods;
	}

	public HashSet<NormalMark> getManagedClassMarks() {
		return managedClassMarks;
	}

	public HashSet<IField> getRuntimeAllOperatedFields() {
		return rtAllOperatedFields;
	}

}
