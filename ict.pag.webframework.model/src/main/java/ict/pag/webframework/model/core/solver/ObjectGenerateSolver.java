package ict.pag.webframework.model.core.solver;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.dom4j.Attribute;
import org.dom4j.Element;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.shrikeCT.AnnotationsReader.AnnotationAttribute;
import com.ibm.wala.shrikeCT.AnnotationsReader.ArrayElementValue;
import com.ibm.wala.shrikeCT.AnnotationsReader.ConstantElementValue;
import com.ibm.wala.shrikeCT.AnnotationsReader.ElementValue;
import com.ibm.wala.shrikeCT.AnnotationsReader.EnumElementValue;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;

import ict.pag.webframework.model.answer.OptimizeAnswer;
import ict.pag.webframework.model.enumeration.MarkScope;
import ict.pag.webframework.model.enumeration.ValueFrom;
import ict.pag.webframework.model.log.CallsiteInfo;
import ict.pag.webframework.model.marks.ConcreteValueMark;
import ict.pag.webframework.model.marks.MarksHelper;
import ict.pag.webframework.model.marks.NormalMark;
import ict.pag.webframework.model.marks.ResolveMarks;
import ict.pag.webframework.model.option.ConfigUtil;
import ict.pag.webframework.model.option.SpecialHelper;

/**
 * Framework may generate objects:</br>
 * 1. Class : the scope of classes that framework will manipulate immediately
 * </br>
 * 2. Method: base, parameter initialize of entry methods <br>
 * 3. Field : field injection and field actual points-to
 * 
 */
public class ObjectGenerateSolver {
	/** Initialize */
	private ClassHierarchy cha;
	private CHACallGraph chaCG;
	private HashMap<String, HashSet<Element>> class2XMLEle;

	/** Internal */
	private HashSet<String> aliasValueSet = new HashSet<String>();

	/** Answer */
	/*
	 * 1. unreachable classes; 2. class that injected field; 3. the class generate
	 * object to assign for variable
	 */
	private HashSet<NormalMark> managedClassMarks = new HashSet<>();
	private HashSet<NormalMark> fieldInjectMarks = new HashSet<>();
	private HashSet<ConcreteValueMark> fieldPoints2Marks = new HashSet<>();
	private HashSet<ConcreteValueMark> aliasMarks = new HashSet<>();
	private HashSet<ConcreteValueMark> frameworkCallReturnPoints2Marks = new HashSet<>();

	private HashSet<String> mayEntryPointFormalParameterSet = new HashSet<>();

	public ObjectGenerateSolver(ClassHierarchy cha, CHACallGraph chaCG,
			HashMap<String, HashSet<Element>> class2xmlEle) {
		this.cha = cha;
		this.chaCG = chaCG;
		this.class2XMLEle = class2xmlEle;

		initAllClassName();
	}

	public void solve(HashSet<String> unreachableCalls, HashMap<FieldReference, String> fieldRef2target_app,
			HashMap<CallsiteInfo, String> callsite2target_app) {
		long beforeTime = System.nanoTime();

		solveClassMarks(unreachableCalls);
		solveFieldInject();
		solveFieldPoints2(fieldRef2target_app);
		solveVariablePoints2(callsite2target_app);

		// merge and remove duplicate of all answers
		OptimizeAnswer.resolveNormalMarkAnswers(managedClassMarks);
		OptimizeAnswer.resolveNormalMarkAnswers(fieldInjectMarks);
		OptimizeAnswer.resovleConcreteValueMarkAnswer(fieldPoints2Marks);
		OptimizeAnswer.resovleConcreteValueMarkAnswer(aliasMarks);
		OptimizeAnswer.resovleConcreteValueMarkAnswer(frameworkCallReturnPoints2Marks);

		// calculate the parameter type of connect methods
		calculateConenctMethodParameterType(unreachableCalls);

		double buildTime = (System.nanoTime() - beforeTime) / 1E9;
		System.out.println("[TIME-LOG] Object Relation Inferring Done in " + buildTime + " s!");
	}

	private void initAllClassName() {
		cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(cn -> {
			String className = cn.getName().getClassName().toString().toLowerCase();
			aliasValueSet.add(className);
		});

	}

	/**
	 * For normal situation, object points to its declare type or sub-class of the
	 * declare type. </br>
	 * If configured by framework, the variable may has certain points to object
	 * 
	 * @param callsite2target_app
	 */
	private void solveVariablePoints2(HashMap<CallsiteInfo, String> callsite2seq) {
		// framework call return may depends on configured
		chaCG.forEach(node -> {
			if (node.getIR() == null)
				return;

			if (!node.getMethod().getDeclaringClass().getClassLoader().getReference()
					.equals(ClassLoaderReference.Application))
				return;

			String sig = node.getMethod().getSignature();
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
								SourcePosition sp = SpecialHelper.getSourcePosition(node.getMethod(), invoke.iIndex());
								int number = sp == null ? -1 : sp.getLastLine();
								if (number == -1)
									continue;
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
												if (!actual_def.getCallSite().getDeclaredTarget().getDeclaringClass()
														.getClassLoader().equals(ClassLoaderReference.Application))
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
																		HashSet<String> allMarks = new HashSet<>();
																		allMarks.add(defInvoke);
																		ConcreteValueMark mark = new ConcreteValueMark(
																				allMarks, MarkScope.Method, i + 1 + "",
																				ValueFrom.FullClassName);
																		frameworkCallReturnPoints2Marks.add(mark);

																		addIntoManagedClassMarks(actual_class);
																	} else {
																		HashSet<String> allMarks = new HashSet<>();
																		allMarks.add(defInvoke);
																		ConcreteValueMark mark = new ConcreteValueMark(
																				allMarks, MarkScope.Method, i + "",
																				ValueFrom.FullClassName);
																		frameworkCallReturnPoints2Marks.add(mark);

																		addIntoManagedClassMarks(actual_class);
																	}
																} else {
																	/** if use alias */
																	HashSet<String> marksSet = findValue(value);
																	if (marksSet != null) {
																		if (actual_def.isStatic()) {
																			HashSet<String> allMarks = new HashSet<>();
																			allMarks.add(defInvoke);
																			ConcreteValueMark mark = new ConcreteValueMark(
																					allMarks, MarkScope.Method,
																					i + 1 + "", ValueFrom.Attribute);
																			frameworkCallReturnPoints2Marks.add(mark);

																			addIntoManagedClassMarks(actual_class);
																		} else {
																			HashSet<String> allMarks = new HashSet<>();
																			allMarks.add(defInvoke);
																			ConcreteValueMark mark = new ConcreteValueMark(
																					allMarks, MarkScope.Method, i + "",
																					ValueFrom.Attribute);
																			frameworkCallReturnPoints2Marks.add(mark);

																			addIntoManagedClassMarks(actual_class);
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
																					HashSet<String> allMarks = new HashSet<>();
																					allMarks.add(defInvoke);
																					ConcreteValueMark mark = new ConcreteValueMark(
																							allMarks, MarkScope.Method,
																							i + 1 + "",
																							ValueFrom.Clazz);
																					frameworkCallReturnPoints2Marks
																							.add(mark);
																				} else {
																					HashSet<String> allMarks = new HashSet<>();
																					allMarks.add(defInvoke);
																					ConcreteValueMark mark = new ConcreteValueMark(
																							allMarks, MarkScope.Method,
																							i + "", ValueFrom.Clazz);
																					frameworkCallReturnPoints2Marks
																							.add(mark);
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
							}
						}
					}
				}
			}
		});

		// TODO:method parameter

	}

	private HashSet<String> findValue(String value) {
		HashSet<String> ret = new HashSet<>();

		if (aliasValueSet.contains(value.toLowerCase()))
			ret.add(value);

		return ret;
	}

	/**
	 * Same as {@link solveVariablePoints2}
	 * 
	 * @param fieldRef2target_app
	 */
	private void solveFieldPoints2(HashMap<FieldReference, String> fieldRef2target_app) {
		for (FieldReference fieldRef : fieldRef2target_app.keySet()) {
			String target = fieldRef2target_app.get(fieldRef);
			String actualcallType = target.substring(0, target.lastIndexOf('.'));

			// field
			IField field = cha.resolveField(fieldRef);
			String fieldName = field.getName().toString();
			String fieldTypeStr = SpecialHelper
					.formatSignature(field.getReference().getFieldType().getName().toString());
			if (actualcallType.equals(fieldTypeStr))
				continue;

			HashSet<Annotation> annos_field = ResolveMarks.resolve_Annotation(field, MarkScope.Field);
			HashSet<Element> xmlEles_field = ResolveMarks.resolve_XML(field, MarkScope.Field, class2XMLEle);
			if (annos_field.isEmpty() && xmlEles_field.isEmpty())
				continue;

			// actual target
			String clazz = "L" + actualcallType.replaceAll("\\.", "/");
			IClass givenClass = cha.lookupClass(
					TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
			if (givenClass == null)
				continue;
			HashSet<Annotation> annos_class = ResolveMarks.resolve_Annotation(givenClass, MarkScope.Clazz);
			HashSet<Element> xmlEles_class = ResolveMarks.resolve_XML(givenClass, MarkScope.Clazz, class2XMLEle);

			String classString = SpecialHelper.formatSignature(givenClass.getName().toString());
			String className = givenClass.getName().getClassName().toString();

			// 1. if field reference defined by annotation
			boolean find1 = false;
			for (Annotation anno : annos_field) {
				Map<String, ElementValue> map = anno.getNamedArguments();
				for (String key : map.keySet()) {
					ElementValue val0 = map.get(key);
					String val = reFormatAnnosValue(val0);

					if (val != null) {
						if (val.toLowerCase().equals(classString.toLowerCase())) {
							HashSet<String> allMarks = new HashSet<String>();
							allMarks.add(
									"[anno]" + SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
							ConcreteValueMark mark = new ConcreteValueMark(allMarks, MarkScope.Field, key,
									ValueFrom.FullClassName);

							fieldPoints2Marks.add(mark);
							addIntoManagedClassMarks(actualcallType);
							find1 = true;
						} else if (val.toLowerCase().equals(className.toLowerCase())) {
							HashSet<String> allMarks = new HashSet<String>();
							allMarks.add(
									"[anno]" + SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
							ConcreteValueMark mark = new ConcreteValueMark(allMarks, MarkScope.Field, key,
									ValueFrom.ClassName);

							fieldPoints2Marks.add(mark);
							addIntoManagedClassMarks(actualcallType);
							find1 = true;
						} else {
							// class alias is defined in xml or annotation
							find1 = dealWithClassAliasInXmlAndAnnotation(actualcallType, val, annos_class,
									xmlEles_class);
							if (find1) {
								HashSet<String> allMarks = new HashSet<String>();
								allMarks.add("[anno]"
										+ SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
								ConcreteValueMark mark = new ConcreteValueMark(allMarks, MarkScope.Field, key,
										ValueFrom.Attribute);

								fieldPoints2Marks.add(mark);
								addIntoManagedClassMarks(actualcallType);
							}

						}
					}

				}
			}
			if (!find1) {
				// 2. field reference defined in xml
				boolean find2 = false;
				for (Element xmlEle : xmlEles_field) {
					String path = xmlEle.getName();
					Element parent = xmlEle.getParent();
					while (parent != null) {
						path = parent.getName().concat(";").concat(path);
						parent = parent.getParent();
					}

					String fieldPath = null;
					String refPath = null;
					for (Object attr0 : xmlEle.attributes()) {
						if (attr0 instanceof Attribute) {
							String val = ((Attribute) attr0).getValue();
							String name = ((Attribute) attr0).getName();

							if (val != null && val.toLowerCase().equals(fieldName.toLowerCase())) {
								fieldPath = "[xml]" + path + ":" + name;
								continue;
							}
							// reference is the same layer
							boolean flag = dealWithClassAliasInXmlAndAnnotation(actualcallType, val, annos_class,
									xmlEles_class);
							if (flag) {
								refPath = "[xml]" + path + ":" + name;
							}
						}
					}

					if (fieldPath == null)
						continue;
					else if (refPath == null) {
						// sub-layer find reference
						boolean flag = false;
						for (Object child0 : xmlEle.elements()) {
							if (child0 instanceof Element) {
								for (Object attr0 : ((Element) child0).attributes()) {
									String path2 = path.concat(";").concat(((Element) child0).getName());
									if (attr0 instanceof Attribute) {
										String val = ((Attribute) attr0).getValue();
										String name = ((Attribute) attr0).getName();

										flag = dealWithClassAliasInXmlAndAnnotation(actualcallType, val, annos_class,
												xmlEles_class);
										if (flag) {
											refPath = "[xml]" + path2 + ":" + name;
											break;
										}
									}
								}
							}
							if (flag)
								break;
						}
						if (refPath != null) {
							HashSet<String> allMarks = new HashSet<String>();
							allMarks.add(fieldPath);
							ConcreteValueMark mark = new ConcreteValueMark(allMarks, MarkScope.Field, refPath,
									ValueFrom.Attribute);
							fieldPoints2Marks.add(mark);
							addIntoManagedClassMarks(actualcallType);
							find2 = true;
						}

					} else {
						HashSet<String> allMarks = new HashSet<String>();
						allMarks.add(fieldPath);
						ConcreteValueMark mark = new ConcreteValueMark(allMarks, MarkScope.Field, refPath,
								ValueFrom.Attribute);
						fieldPoints2Marks.add(mark);
						addIntoManagedClassMarks(actualcallType);
						find2 = true;
					}
				}

				if (!find2) {
					// 3. field reference defined by field name
					if (fieldName.equals(className)) {
						ConcreteValueMark mark = new ConcreteValueMark(new HashSet<>(), MarkScope.Field, "field",
								ValueFrom.FieldName);

						fieldPoints2Marks.add(mark);
						addIntoManagedClassMarks(actualcallType);

					} else {
						// class alias is defined in xml or annotation
						boolean find3 = dealWithClassAliasInXmlAndAnnotation(actualcallType, fieldName, annos_class,
								xmlEles_class);
						if (find3) {
							ConcreteValueMark mark = new ConcreteValueMark(new HashSet<>(), MarkScope.Field, "field",
									ValueFrom.Attribute);

							fieldPoints2Marks.add(mark);
							addIntoManagedClassMarks(actualcallType);
						}
					}
				}

			}
		}
	}

	private boolean dealWithClassAliasInXmlAndAnnotation(String actualcallType, String val,
			HashSet<Annotation> annos_class, HashSet<Element> xmlEles_class) {
		// actual class alias defined in annotation
		ConcreteValueMark mark2 = findAnnotationWithGivenValue(annos_class, val, MarkScope.Clazz);
		if (mark2 != null) {
			aliasMarks.add(mark2);
			aliasValueSet.add(val.toLowerCase());

			return true;
		} else {
			// actual class alias defined in xml
			ConcreteValueMark classMark = findXMLeleWithGivenValue_2(actualcallType, xmlEles_class, val,
					MarkScope.Clazz);
			if (classMark != null) {
				aliasMarks.add(classMark);
				aliasValueSet.add(val.toLowerCase());

				return true;
			}
		}
		return false;
	}

	private void solveFieldPoints2_old(HashMap<FieldReference, String> fieldRef2target_app) {
		for (FieldReference fieldRef : fieldRef2target_app.keySet()) {
			String target = fieldRef2target_app.get(fieldRef);
			String actualcallType = target.substring(0, target.lastIndexOf('.'));

			// field
			IField field = cha.resolveField(fieldRef);
			String fieldName = field.getName().toString();
			String fieldTypeStr = SpecialHelper
					.formatSignature(field.getReference().getFieldType().getName().toString());
			if (actualcallType.equals(fieldTypeStr))
				continue;

			HashSet<Annotation> annos_field = ResolveMarks.resolve_Annotation(field, MarkScope.Field);
			HashSet<Element> xmlEles_field = ResolveMarks.resolve_XML(field, MarkScope.Field, class2XMLEle);
			if (annos_field.isEmpty() && xmlEles_field.isEmpty())
				continue;

			// actual target
			String clazz = "L" + actualcallType.replaceAll("\\.", "/");
			IClass givenClass = cha.lookupClass(
					TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
			if (givenClass == null)
				continue;
			HashSet<Annotation> annos_class = ResolveMarks.resolve_Annotation(givenClass, MarkScope.Clazz);
			HashSet<Element> xmlEles_class = ResolveMarks.resolve_XML(givenClass, MarkScope.Clazz, class2XMLEle);

			String className = givenClass.getName().getClassName().toString();

			// find actual connection
			if (annos_class.isEmpty() && xmlEles_class.isEmpty()) {
				/* the alias of class is class name */
				boolean find = false;
				// field defined using annotation
				for (Annotation anno : annos_field) {
					Map<String, ElementValue> map = anno.getNamedArguments();
					for (String key : map.keySet()) {
						ElementValue val0 = map.get(key);
						String val = reFormatAnnosValue(val0);
						if (val != null && val.toLowerCase().equals(className.toLowerCase())) {
							HashSet<String> allMarks = new HashSet<String>();
							allMarks.add(
									"[anno]" + SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
							ConcreteValueMark mark = new ConcreteValueMark(allMarks, MarkScope.Field, key,
									ValueFrom.ClassName);
							fieldPoints2Marks.add(mark);

							find = true;

							addIntoManagedClassMarks(actualcallType);

							break;
						}
					}
					if (find)
						break;
				}
				if (!find) {
					// field defined using xml
					boolean find2 = false;
					for (Element xmlEle : xmlEles_field) {
						String path = xmlEle.getName();
						Element parent = xmlEle.getParent();
						while (parent != null) {
							path = parent.getName().concat(";").concat(path);
							parent = parent.getParent();
						}
						// this layer
						for (Object attr0 : xmlEle.attributes()) {
							if (attr0 instanceof Attribute) {
								String val = ((Attribute) attr0).getValue();
								String name = ((Attribute) attr0).getName();

								if (!name.toLowerCase().equals("class"))
									if (val != null && (val.toLowerCase().equals(className.toLowerCase())
											|| val.toLowerCase().equals(className.toLowerCase()))) {
										HashSet<String> allMarks = new HashSet<String>();
										allMarks.add("[xml]" + path + ":" + name);
										ConcreteValueMark mark = new ConcreteValueMark(allMarks, MarkScope.Field, name,
												ValueFrom.ClassName);
										fieldPoints2Marks.add(mark);
										find2 = true;

										addIntoManagedClassMarks(actualcallType);

										break;
									}
							}
						}
						if (find2)
							break;
						// sub-layer
						for (Object child0 : xmlEle.elements()) {
							if (child0 instanceof Element) {
								for (Object attr0 : ((Element) child0).attributes()) {
									String path2 = path.concat(";").concat(((Element) child0).getName());
									if (attr0 instanceof Attribute) {
										String val = ((Attribute) attr0).getValue();
										String name = ((Attribute) attr0).getName();
										if (val != null && (val.toLowerCase().equals(className.toLowerCase())
												|| val.toLowerCase().equals(className.toLowerCase()))) {
											HashSet<String> allMarks = new HashSet<String>();
											allMarks.add("[xml]" + path2 + ":" + name);
											ConcreteValueMark mark = new ConcreteValueMark(allMarks, MarkScope.Field,
													name, ValueFrom.ClassName);
											fieldPoints2Marks.add(mark);
											find2 = true;

											addIntoManagedClassMarks(actualcallType);

											break;
										}
									}
								}
								if (find2)
									break;
							}
						}
						if (find2)
							break;
					}
				}
			} else {
				/* the alias of class is attribute */
				boolean find = false;
				// field defined in annotation
				for (Annotation anno : annos_field) {
					Map<String, ElementValue> map = anno.getNamedArguments();
					for (String key : map.keySet()) {
						ElementValue val0 = map.get(key);
						String val = reFormatAnnosValue(val0);
						if (val != null && !val.isEmpty()) {
							// alias defined in annotation
							ConcreteValueMark mark2 = findAnnotationWithGivenValue(annos_class, val, MarkScope.Clazz);
							if (mark2 != null) {
								aliasMarks.add(mark2);
								aliasValueSet.add(val.toLowerCase());

								HashSet<String> allMarks = new HashSet<String>();
								allMarks.add("[anno]"
										+ SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
								ConcreteValueMark mark = new ConcreteValueMark(allMarks, MarkScope.Field, key,
										ValueFrom.Attribute);
								fieldPoints2Marks.add(mark);
								find = true;

								addIntoManagedClassMarks(actualcallType);

								break;
							} else {
								// alias defined in xml
								ConcreteValueMark mark3 = findXMLeleWithGivenValue(xmlEles_class, val, MarkScope.Clazz);
								if (mark3 != null) {
									aliasMarks.add(mark3);
									aliasValueSet.add(val.toLowerCase());

									HashSet<String> allMarks = new HashSet<String>();
									allMarks.add("[anno]"
											+ SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
									ConcreteValueMark mark = new ConcreteValueMark(allMarks, MarkScope.Field, key,
											ValueFrom.Attribute);
									fieldPoints2Marks.add(mark);
									find = true;

									addIntoManagedClassMarks(actualcallType);

									break;
								}
							}
						}
					}
					if (find)
						break;
				}
				if (!find) {
					// field defined in xml
					boolean find2 = false;
					for (Element xmlEle : xmlEles_field) {
						String path = xmlEle.getName();
						Element parent = xmlEle.getParent();
						while (parent != null) {
							path = parent.getName().concat(";").concat(path);
							parent = parent.getParent();
						}
						// this layer
						for (Object attr0 : xmlEle.attributes()) {
							if (attr0 instanceof Attribute) {
								String val = ((Attribute) attr0).getValue();
								String name = ((Attribute) attr0).getName();
								// alias defined in annotation
								ConcreteValueMark mark2 = findAnnotationWithGivenValue(annos_class, val,
										MarkScope.Clazz);
								if (mark2 != null) {
									aliasMarks.add(mark2);
									aliasValueSet.add(val.toLowerCase());

									HashSet<String> allMarks = new HashSet<String>();
//									allMarks.add("[xml]" + path);
									allMarks.add("[xml]" + path + ":" + name);
									ConcreteValueMark mark = new ConcreteValueMark(allMarks, MarkScope.Field, name,
											ValueFrom.Attribute);
									fieldPoints2Marks.add(mark);
									find2 = true;

									addIntoManagedClassMarks(actualcallType);

									break;
								} else {
									// alias defined in xml
									ConcreteValueMark mark3 = findXMLeleWithGivenValue(xmlEles_class, val,
											MarkScope.Clazz);
									if (mark3 != null) {
										aliasMarks.add(mark3);
										aliasValueSet.add(val.toLowerCase());

										HashSet<String> allMarks = new HashSet<String>();
//										allMarks.add("[xml]" + path);
										allMarks.add("[xml]" + path + ":" + name);
										ConcreteValueMark mark = new ConcreteValueMark(allMarks, MarkScope.Field, name,
												ValueFrom.Attribute);
										fieldPoints2Marks.add(mark);
										find2 = true;

										addIntoManagedClassMarks(actualcallType);

										break;
									}
								}
							}
						}
						if (find2)
							break;
						// sub-layer
						for (Object child0 : xmlEle.elements()) {
							if (child0 instanceof Element) {
								for (Object attr0 : ((Element) child0).attributes()) {
									String path2 = path.concat(";").concat(((Element) child0).getName());
									if (attr0 instanceof Attribute) {
										String val = ((Attribute) attr0).getValue();
										String name = ((Attribute) attr0).getName();
										// alias defined in annotation
										ConcreteValueMark mark2 = findAnnotationWithGivenValue(annos_class, val,
												MarkScope.Clazz);
										if (mark2 != null) {
											aliasMarks.add(mark2);
											aliasValueSet.add(val.toLowerCase());

											HashSet<String> allMarks = new HashSet<String>();
											allMarks.add("[xml]" + path2 + ":" + name);
											ConcreteValueMark mark = new ConcreteValueMark(allMarks, MarkScope.Field,
													name, ValueFrom.Attribute);
											fieldPoints2Marks.add(mark);
											find2 = true;

											addIntoManagedClassMarks(actualcallType);

											break;
										} else {
											// alias defined in xml
											ConcreteValueMark mark3 = findXMLeleWithGivenValue(xmlEles_class, val,
													MarkScope.Clazz);
											if (mark3 != null) {
												aliasMarks.add(mark3);
												aliasValueSet.add(val.toLowerCase());

												HashSet<String> allMarks = new HashSet<String>();
												allMarks.add("[xml]" + path2 + ":" + name);
												ConcreteValueMark mark = new ConcreteValueMark(allMarks,
														MarkScope.Field, name, ValueFrom.Attribute);
												fieldPoints2Marks.add(mark);
												find2 = true;

												addIntoManagedClassMarks(actualcallType);

												break;
											}
										}
									}
								}
								if (find2)
									break;
							}
						}
						if (find2)
							break;
					}
				}
			}
		}

	}

	private ConcreteValueMark findXMLeleWithGivenValue_2(String classString, HashSet<Element> xmlEles_class,
			String target, MarkScope s) {
		if (target.toLowerCase().equals("true") || target.toLowerCase().equals("false"))
			return null;

		for (Element xmlEle : xmlEles_class) {
			String path = xmlEle.getName();
			Element parent = xmlEle.getParent();
			while (parent != null) {
				path = parent.getName().concat(";").concat(path);
				parent = parent.getParent();
			}

			if (MarkScope.Clazz.equals(s)) {
				String classPath = null;
				String aliasPath = null;
				for (Object attr0 : xmlEle.attributes()) {
					if (attr0 instanceof Attribute) {
						String val = ((Attribute) attr0).getValue();
						String name = ((Attribute) attr0).getName();
						if (val != null && val.toLowerCase().equals(target.toLowerCase())) {
							aliasPath = "[xml]" + path + ":" + name;
						}
						if (val != null && val.toLowerCase().equals(classString.toLowerCase())) {
							classPath = "[xml]" + path + ":" + name;
						}
					}
				}
				if (classPath != null && aliasPath != null) {
					HashSet<String> allMarks = new HashSet<String>();
					allMarks.add(classPath);
					ConcreteValueMark mark2 = new ConcreteValueMark(allMarks, s, aliasPath, ValueFrom.Attribute);
					return mark2;
				}
			}
		}

		return null;
	}

	private ConcreteValueMark findXMLeleWithGivenValue(HashSet<Element> xmlEles_class, String target, MarkScope s) {
		if (target.toLowerCase().equals("true") || target.toLowerCase().equals("false"))
			return null;

		for (Element xmlEle : xmlEles_class) {
			String path = xmlEle.getName();
			Element parent = xmlEle.getParent();
			while (parent != null) {
				path = parent.getName().concat(";").concat(path);
				parent = parent.getParent();
			}

			if (MarkScope.Clazz.equals(s)) {
				// this layer
				for (Object attr0 : xmlEle.attributes()) {

					if (attr0 instanceof Attribute) {
						String val = ((Attribute) attr0).getValue();
						String name = ((Attribute) attr0).getName();
						if (val.toLowerCase().equals(target.toLowerCase())
								|| val.toLowerCase().endsWith("." + target.toLowerCase())) {
							String markString = "[xml]" + path;

							HashSet<String> allMarks = new HashSet<String>();
							allMarks.add(markString);
							ConcreteValueMark mark2 = new ConcreteValueMark(allMarks, s, name, ValueFrom.Attribute);
							return mark2;
						}
					}
				}
			} else if (MarkScope.Field.equals(s)) {
				// sub-layer
				for (Object child0 : xmlEle.elements()) {
					if (child0 instanceof Element) {
						for (Object attr0 : ((Element) child0).attributes()) {

							if (attr0 instanceof Attribute) {
								String val = ((Attribute) attr0).getValue();
								String name = ((Attribute) attr0).getName();
								if (val.equals(target)) {
									String markString = "[xml]" + path;

									HashSet<String> allMarks = new HashSet<String>();
									allMarks.add(markString);
									ConcreteValueMark mark2 = new ConcreteValueMark(allMarks, s, name,
											ValueFrom.Attribute);
									return mark2;
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	private ConcreteValueMark findAnnotationWithGivenValue(HashSet<Annotation> annos_class, String val, MarkScope s) {
		if (val.toLowerCase().equals("true") || val.toLowerCase().equals("false"))
			return null;
		for (Annotation c_anno : annos_class) {
			for (String c_key : c_anno.getNamedArguments().keySet()) {
				ElementValue c_val0 = c_anno.getNamedArguments().get(c_key);
				String c_val = reFormatAnnosValue(c_val0);
				if (c_val != null && !c_val.isEmpty() && c_val.toLowerCase().equals(val.toLowerCase())) {
					HashSet<String> allMarks2 = new HashSet<String>();
					allMarks2.add("[anno]" + SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(c_anno)));
					ConcreteValueMark mark2 = new ConcreteValueMark(allMarks2, s, c_key, ValueFrom.Attribute);
					return mark2;
				}
			}
		}
		return null;
	}

	private String reFormatAnnosValue(ElementValue v) {
		if (v == null)
			return null;

		String ret = null;
		if (v instanceof ConstantElementValue) {
			ret = ((ConstantElementValue) v).val.toString();
		}
		if (v instanceof ArrayElementValue) {
			ElementValue tmp = ((ArrayElementValue) v).vals[0];
			if (tmp instanceof ConstantElementValue) {
				ret = ((ConstantElementValue) tmp).val.toString();
			}
			if (tmp instanceof EnumElementValue) {
				ret = ((EnumElementValue) tmp).enumVal;
			}
		}
		if (v instanceof EnumElementValue) {
			ret = ((EnumElementValue) v).enumVal;
		}

		/** TODO: */
		if (v instanceof AnnotationAttribute) {
			System.out.println("AnnotationAttribute :" + ((AnnotationAttribute) v).elementValues.get("vals"));
		}
		return ret;
	}

	/**
	 * The field that framework inject object into it</br>
	 * Approaches:</br>
	 * 1. directly on field</br>
	 * 2. on <init> method</br>
	 * 3. on set method
	 * 
	 * TODO: 所有putfield都没被调用
	 */
	private void solveFieldInject() {
		cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(cn -> {
			String classNameString = SpecialHelper.formatSignature(cn.getName().toString());
			Collection<IField> allFields = cn.getDeclaredInstanceFields();
			HashMap<IField, HashSet<IMethod>> field2InitializeMtds = new HashMap<>();
			for (IField f : allFields) {
				field2InitializeMtds.put(f, new HashSet<>());
			}

			Collection<? extends IMethod> declareMtds = cn.getDeclaredMethods();
			for (IMethod mtd : declareMtds) {
				if (mtd.isInit() || isSetFun(mtd, cn)) {
					CGNode node = chaCG.getNode(mtd, Everywhere.EVERYWHERE);
					if (node != null) {
						IR ir = node.getIR();
						if (ir != null) {
							SSAInstruction[] insts = ir.getInstructions();
							for (SSAInstruction inst : insts) {
								if (inst instanceof SSAPutInstruction) {
									SSAPutInstruction putInst = (SSAPutInstruction) inst;
									FieldReference initField = putInst.getDeclaredField();
									for (IField f : field2InitializeMtds.keySet()) {
										if (f.getReference().equals(initField)) {
											field2InitializeMtds.get(f).add(mtd);
										}
									}
								}
							}
						}
					}
				}
			}

			for (IField f : field2InitializeMtds.keySet()) {
				HashSet<IMethod> initMtds = field2InitializeMtds.get(f);

				if (initMtds == null || initMtds.isEmpty()) {
					// situation 1:
					HashSet<String> marks = new HashSet<>();
					HashSet<Annotation> annos = ResolveMarks.resolve_Annotation(f, MarkScope.Field);
					for (Annotation anno : annos) {
						marks.add("[anno]" + SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
					}
					HashSet<String> xmlEles = ResolveMarks.resolve_XML_String(f, MarkScope.Field, class2XMLEle);
					for (String ele : xmlEles) {
						marks.add("[xml]" + ele);
					}
					if (!marks.isEmpty()) {
						NormalMark nor = new NormalMark(marks, MarkScope.Field);
						fieldInjectMarks.add(nor);

						addIntoManagedClassMarks(classNameString);
					}
				} else {
					// situation 2-3:
					for (IMethod mtd : initMtds) {
						HashSet<String> marks = new HashSet<>();
						HashSet<Annotation> annos = ResolveMarks.resolve_Annotation(mtd, MarkScope.Method);
						for (Annotation anno : annos) {
							marks.add(
									"[anno]" + SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
						}
						HashSet<String> xmlEles = ResolveMarks.resolve_XML_String(mtd, MarkScope.Method, class2XMLEle);
						for (String ele : xmlEles) {
							marks.add("[xml]" + ele);
						}
						if (!marks.isEmpty()) {
							NormalMark nor = new NormalMark(marks, MarkScope.Method);
							fieldInjectMarks.add(nor);

							addIntoManagedClassMarks(classNameString);
						}
					}
				}
			}

		});

	}

	/** set field function: set + fieldName */
	private boolean isSetFun(IMethod m, IClass node) {
		String pattern = "set[A-Z].*";
		Pattern r = Pattern.compile(pattern);
		if (r.matcher(m.getName().toString()).matches()) {
			Collection<IField> allFields = node.getDeclaredInstanceFields();

			Set<String> allFieldsNames = new HashSet<String>();
			for (IField f : allFields) {
				String f_name = f.getName().toString();
				allFieldsNames.add(f_name.toLowerCase().replaceAll("_", ""));
			}

			String tmp = m.getName().toString().substring(3);
			if (allFieldsNames.contains(tmp.toLowerCase().replaceAll("_", "")))
				return true;
		}
		return false;
	}

	private HashSet<String> visitedLines = new HashSet<>();

	/**
	 * If the class haven't been initialized in application but been used in actual
	 */
	private void solveClassMarks(HashSet<String> unreachableCalls) {
		for (String line : unreachableCalls) {

			if (!visitedLines.contains(line)) {
				/* classes */
				String clazzName = line.substring(0, line.lastIndexOf('.'));
				addIntoManagedClassMarks(clazzName);
				/* return */
				String ret = line.substring(line.lastIndexOf(')') + 1);
				if (ret.startsWith("L") && ret.endsWith(";")) {
					ret = SpecialHelper.formatSignature(ret);
					addIntoManagedClassMarks(ret);
				}
				visitedLines.add(line);
			}
		}

	}

	public void addIntoManagedClassMarks(String clazzName) {
		String clazz = "L" + clazzName.replaceAll("\\.", "/");
		IClass givenClass = cha.lookupClass(
				TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
		if (givenClass == null)
			return;

		HashSet<String> marks = new HashSet<>();
		HashSet<Annotation> annos_class0 = ResolveMarks.resolve_Annotation(givenClass, MarkScope.Clazz);
		for (Annotation anno : annos_class0) {
			marks.add("[anno]" + SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
		}
		HashSet<String> xmlEles_class = ResolveMarks.resolve_XML_String(givenClass, MarkScope.Clazz, class2XMLEle);
		for (String ele : xmlEles_class) {
			marks.add("[xml]" + ele);
		}
		if (ConfigUtil.marksLevel == 0 || (ConfigUtil.marksLevel == 1 && marks.isEmpty())) {
			HashSet<String> inheritance_class = ResolveMarks.resolve_Inheritance(givenClass, MarkScope.Clazz, cha);
			for (String ele : inheritance_class) {
				marks.add("[inheritance]" + SpecialHelper.formatSignature(ele));
			}
		}

		if (!marks.isEmpty()) {
			NormalMark nor = new NormalMark(marks, MarkScope.Clazz);
			managedClassMarks.add(nor);
		}
	}

	private void calculateConenctMethodParameterType(HashSet<String> unreachableCalls) {
		for (String s : unreachableCalls) {
			if (s.startsWith("url started")) {
				continue;
			}
			if (s.startsWith("url finished")) {
				continue;
			}

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
				if (!ConfigUtil.isApplicationClass(type) && ConfigUtil.isFrameworkMarks(type)) {
					mayEntryPointFormalParameterSet.add(SpecialHelper.formatSignature(type));
				}
			}
		}

	}

	public HashSet<NormalMark> getManagedClassMarks() {
		return managedClassMarks;
	}

	public HashSet<NormalMark> getFieldInjectMarks() {
		return fieldInjectMarks;
	}

	public HashSet<ConcreteValueMark> getFieldPoints2Marks() {
		return fieldPoints2Marks;
	}

	public HashSet<ConcreteValueMark> getAliasMarks() {
		return aliasMarks;
	}

	public HashSet<ConcreteValueMark> getFrameworkCallReturnPoints2Marks() {
		return frameworkCallReturnPoints2Marks;
	}

	public HashSet<String> getMayEntryPointFormalParameterSet() {
		return mayEntryPointFormalParameterSet;
	}

}
