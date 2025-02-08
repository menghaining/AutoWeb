package ict.pag.webframework.preInstrumental;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentType;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ict.pag.m.instrumentation.Util.ConfigureUtil;
import ict.pag.webframework.XML.XMLMarksExtractor;
import ict.pag.webframework.XML.Util.XMLConfigurationUtil;
import ict.pag.webframework.log.RequestInfoExtractor;
import ict.pag.webframework.log.dynamicCG.CallsiteInfo;
import ict.pag.webframework.log.dynamicCG.DynamicCG;
import ict.pag.webframework.log.dynamicCG.DynamicCGNode;
import ict.pag.webframework.preInstrumental.entity.ModifiedInfo;
import ict.pag.webframework.preInstrumental.entity.Modification;
import ict.pag.webframework.preInstrumental.entity.ModifiedSemantic;
import ict.pag.webframework.preInstrumental.helper.ConfigurationCollector;
import ict.pag.webframework.preInstrumental.helper.CtObjectCopyHelper;
import ict.pag.webframework.preInstrumental.helper.SearchInCtClassHelper;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ClassFile;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;

/**
 * 1. determine which element concerned?</br>
 * 2. find all configurations on this element</br>
 * 3. change one and generate an output
 */
/**
 * modify @22-11-14; only override to add more functions
 */
public class PreModifier2 extends PreModifier {

	public PreModifier2(String appPath, String libPath, String externalPath, String outPath, XMLMarksExtractor xmlex, RequestInfoExtractor extractor,
			ArrayList<String> classFiles2, boolean isJar, String jarP) {
		super(appPath, libPath, externalPath, outPath, xmlex, extractor, classFiles2, isJar, jarP);
	}

	/**
	 * Build the configuration content and checkbody content.</br>
	 * 
	 * @param otherinfos: the 0th means the other info about check body, when it
	 *                    represents indirect call semantic the other is other info
	 *                    about configuration : 1. caller-class; 2. caller-method;
	 *                    3. callee-class; 4. callee-method
	 */
	private void buildConfigAndCheckBody(ArrayList<String> configs, ArrayList<String> checkbody, String config1, String stmt, ArrayList<String> otherinfos) {
		configs.add(config1);

		if (otherinfos != null && !otherinfos.isEmpty()) {
			String otherchecker = otherinfos.get(0);
			checkbody.add(otherchecker);

			for (int i = 1; i < otherinfos.size(); i++)
				configs.add(otherinfos.get(i));
		}

		checkbody.add(stmt);
	}

	/**
	 * mutate the annotation on class: </br>
	 * - delete: remove </br>
	 * - add1: copy and rename the class/method/field and only retain the concerned
	 * annotation</br>
	 * - add2: if has attribute value and find the connect point, copy and rename
	 * the class/method/field with old annotations then modify the connect point
	 * (only do this when it is not deploy log)
	 */
	@Override
	protected boolean modifyOnClass_annotation(ClassFile classFile, String classFilePath, String stmt, ModifiedSemantic type, ArrayList<String> otherinfo,
			HashMap<String, Integer> visitedTags2Count, boolean urlEnable) {
		String requestInfoLog = url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).getUrl();
		boolean isDeploy = false;
		if (requestInfoLog == "deploy")
			isDeploy = true;

		boolean modified1 = false;
		boolean modified2 = false;
		boolean modified3 = false;
		CtClass cc = ClassPool.getDefault().makeClass(classFile);
		AttributeInfo classAttrInfo = cc.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
		if (classAttrInfo != null) {
			if (classAttrInfo instanceof AnnotationsAttribute) {
				AnnotationsAttribute classAttr = (AnnotationsAttribute) classAttrInfo;
				ArrayList<Annotation> candidateMtdAnnos = new ArrayList<>();

				// collect annos on this class
				if (classAttr.getAnnotations().length > 0) {
					for (Annotation anno : classAttr.getAnnotations()) {
						String annoname = anno.getTypeName();
						if (!ConfigureUtil.isApplicationClass(annoname.replace('.', '/')))
							candidateMtdAnnos.add(anno);
					}
				}

				/*
				 * record, modify current annotation, write and recover
				 */
				for (Annotation anno : candidateMtdAnnos) {
					if (cc.isFrozen()) {
						cc.defrost();
					}

					// remove duplicate
					if (!visitedTags2Count.containsKey(anno.getTypeName()))
						visitedTags2Count.put(anno.getTypeName(), 0);
					if (visitedTags2Count.get(anno.getTypeName()) > thresdhold)
						continue;
					visitedTags2Count.put(anno.getTypeName(), visitedTags2Count.get(anno.getTypeName()) + 1);

					String outSubPath = calSubPath(cc.getName(), classFilePath); // for write file
					/*
					 * mutation-1 : remove directly remove and record each annotation
					 */
					// 1.1 mutate
					classAttr.removeAnnotation(anno.getTypeName());
					// 1.2 record and write
					ArrayList<String> tmp_Configs1 = new ArrayList<>();
					ArrayList<String> tmp_body1 = new ArrayList<>();
					buildConfigAndCheckBody(tmp_Configs1, tmp_body1, anno.getTypeName(), stmt, otherinfo);
					INDEX++;
					url2ModifyedInfo.get(url2ModifyedInfo.size() - 1)
							.addModifiedRecord(new ModifiedInfo(stmt, 0, type, Modification.remove, tmp_Configs1, tmp_body1, false), INDEX);
					boolean success = write2SingleClassFile(cc, outSubPath);
					writeRequestSequence();
					if (success)
						System.out.println(" [anno]" + anno.getTypeName() + " on class: " + cc.getName());
					// 1.3 recover this annotation
					classAttr.addAnnotation(anno);
					modified1 = success;

					// latter do change on the copy of original cc
					String addClassName = cc.getPackageName() + "." + "mhnClass_" + cc.getSimpleName();
					/*
					 * mutation-2: if the attribute has no value, duplicate one with only current
					 * annotation
					 */
					// 2.1 mutate
					if (cc.isFrozen())
						cc.defrost();
					cc = ClassPool.getDefault().makeClass(classFile);
					CtClass addClass2 = CtObjectCopyHelper.addNewClassCopy(cc, addClassName, false);
					AttributeInfo classAttrInfo2 = addClass2.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
					if (classAttrInfo2 != null) {
						if (classAttrInfo2 instanceof AnnotationsAttribute) {
							AnnotationsAttribute classAttr2 = (AnnotationsAttribute) classAttrInfo2;
							for (Annotation anno2 : classAttr2.getAnnotations()) {
								if (!anno2.getTypeName().equals(anno.getTypeName()))
									classAttr2.removeAnnotation(anno2.getTypeName());
							}
						}
					}
					// 2.2 record and write
					ArrayList<String> tmp_Configs2 = new ArrayList<>();
					ArrayList<String> tmp_body2 = new ArrayList<>();
					String stmt2 = addClass2.getName() + "." + stmt.substring(stmt.lastIndexOf('.') + 1);
					buildConfigAndCheckBody(tmp_Configs2, tmp_body2, anno.getTypeName(), stmt2, otherinfo);
					INDEX++;
					url2ModifyedInfo.get(url2ModifyedInfo.size() - 1)
							.addModifiedRecord(new ModifiedInfo(stmt, 0, type, Modification.add, tmp_Configs2, tmp_body2, true), INDEX);
					success = false;
					success = write2SingleClassFile(addClass2, outSubPath);
					writeRequestSequence();
					// 2.3 recover
					// do nothing since not modify original classes
					addClass2.detach();
					modified2 = success;

					/*
					 * mutation-3: if find the connection then change
					 */
					Set<String> mems = anno.getMemberNames();
					if (!isDeploy && mems != null && !mems.isEmpty() && urlEnable) {
						if (cc.isFrozen())
							cc.defrost();
						cc = ClassPool.getDefault().makeClass(classFile);
						CtClass addClass3 = CtObjectCopyHelper.addNewClassCopy(cc, addClassName, false);
						Annotation concernAnno = null;
						// find annotation in this class
						AttributeInfo classAttrInfo3 = addClass3.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
						if (classAttrInfo3 != null && (classAttrInfo3 instanceof AnnotationsAttribute)) {
							AnnotationsAttribute classAttr3 = (AnnotationsAttribute) classAttrInfo3;
							concernAnno = classAttr3.getAnnotation(anno.getTypeName());

							for (String mem : concernAnno.getMemberNames()) {
								MemberValue val = concernAnno.getMemberValue(mem);
								if (val instanceof StringMemberValue) {
									String originalValue = ((StringMemberValue) val).getValue();
									// find the connection between configure value and coming url
									String[] newInfo = buildNewTriggerURL(requestInfoLog, originalValue, "_new");
									String newReqInfoLog = newInfo[0];
									String newVal = newInfo[1];
									if (newReqInfoLog != null) {
										// 3.1 mutate
										((StringMemberValue) val).setValue(newVal);
										classAttr3.addAnnotation(concernAnno); // rewrite this modified annotation
										// 3.2 record and write
										ArrayList<String> tmp_Configs3 = new ArrayList<>();
										ArrayList<String> tmp_body3 = new ArrayList<>();
										String stmt3 = addClass3.getName() + "." + stmt.substring(stmt.lastIndexOf('.') + 1);
										buildConfigAndCheckBody(tmp_Configs3, tmp_body3, anno.getTypeName(), stmt3, otherinfo);
										INDEX++;
										url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
												new ModifiedInfo(stmt, 0, type, Modification.add, tmp_Configs3, tmp_body3, true), INDEX, newReqInfoLog);
										success = false;
										success = write2SingleClassFile(addClass3, outSubPath);
										writeRequestSequence(newReqInfoLog);
										// 3.3 recover
										// later mem may use this addClass
										((StringMemberValue) val).setValue(originalValue);
										classAttr3.addAnnotation(concernAnno); // rewrite this modified annotation
										modified3 = success;
									}
								} else if (val instanceof ArrayMemberValue) {
									ArrayMemberValue arrayVal = (ArrayMemberValue) val;
									MemberValue[] vals = arrayVal.getValue();
									if (arrayVal != null && vals != null && (arrayVal.getType() instanceof StringMemberValue) && (vals.length > 0)) {
										String originalValue = null;
										String newVal = null;
										String newReqInfoLog = null;
										int ii = 0;
										boolean modifyArray = false;
										for (; ii < vals.length; ii++) {
											originalValue = ((StringMemberValue) (vals[ii])).getValue();
											// find the connection between configure value and coming url
											String[] newInfo = buildNewTriggerURL(requestInfoLog, originalValue, "_new");
											newReqInfoLog = newInfo[0];
											newVal = newInfo[1];
											if (newReqInfoLog != null) {
												modifyArray = true;
												break;
											}
										}
										if (modifyArray) {
											// 3.1 mutate
											((StringMemberValue) (vals[ii])).setValue(newVal);
											classAttr3.addAnnotation(concernAnno); // rewrite this modified annotation
											// 3.2 record and write
											ArrayList<String> tmp_Configs3 = new ArrayList<>();
											ArrayList<String> tmp_body3 = new ArrayList<>();
											String stmt3 = addClass3.getName() + "." + stmt.substring(stmt.lastIndexOf('.') + 1);
											buildConfigAndCheckBody(tmp_Configs3, tmp_body3, anno.getTypeName(), stmt3, otherinfo);
											INDEX++;
											url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
													new ModifiedInfo(stmt, 0, type, Modification.add, tmp_Configs3, tmp_body3, true), INDEX, newReqInfoLog);
											success = false;
											success = write2SingleClassFile(addClass3, outSubPath);
											writeRequestSequence(newReqInfoLog);
											// 3.3 recover
											// later mem may use this addClass
											((StringMemberValue) (vals[ii])).setValue(originalValue);
											classAttr3.addAnnotation(concernAnno); // rewrite this modified annotation
											modified3 = success;
										}
									}
								}
							}
						}
						addClass3.detach();
					}
				}
			}
		}
		return (modified1 || modified2 || modified3);
	}

	/**
	 * mutate the annotation on method: </br>
	 * - delete: remove </br>
	 * - add1: copy and rename the class/method/field and only retain the concerned
	 * annotation</br>
	 * - add2: if has attribute value and find the connect point, copy and rename
	 * the class/method/field with old annotations then modify the connect point
	 * (only do this when it is not deploy log)
	 */
	@Override
	protected boolean modifyOnMethod_annotation(CtMethod method, ClassFile classFile, String classFilePath, String stmt, ModifiedSemantic type,
			ArrayList<String> otherinfo, HashMap<String, Integer> visitedTags2Count, boolean urlEnable) {
		String requestInfoLog = url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).getUrl();
		boolean isDeploy = false;
		if (requestInfoLog == "deploy")
			isDeploy = true;

		boolean modified1 = false;
		boolean modified2 = false;
		boolean modified3 = false;
		MethodInfo info = method.getMethodInfo();
		AttributeInfo mtdAttrInfo = info.getAttribute(AnnotationsAttribute.visibleTag);
		if (mtdAttrInfo instanceof AnnotationsAttribute) {
			HashSet<Annotation> candidateMtdAnnos = new HashSet<>();

			// collect annotations on this method
			AnnotationsAttribute mtdAttr = (AnnotationsAttribute) mtdAttrInfo;
			if (mtdAttr.getAnnotations().length > 0) {
				for (Annotation anno : mtdAttr.getAnnotations()) {
					String annoname = anno.getTypeName();
					if (!ConfigureUtil.isApplicationClass(annoname.replace('.', '/')))
						candidateMtdAnnos.add(anno);
				}
			}

			/* remove and write, then recover */
			for (Annotation anno : candidateMtdAnnos) {
				CtClass cc = ClassPool.getDefault().makeClass(classFile);
				if (cc.isFrozen()) {
					cc.defrost();
				}
				// remove duplicate
				if (!visitedTags2Count.containsKey(anno.getTypeName()))
					visitedTags2Count.put(anno.getTypeName(), 0);
				if (visitedTags2Count.get(anno.getTypeName()) > thresdhold)
					continue;
				visitedTags2Count.put(anno.getTypeName(), visitedTags2Count.get(anno.getTypeName()) + 1);

				String outSubPath1 = calSubPath(cc.getName(), classFilePath);
				/*
				 * mutation-1 : remove directly remove and record each annotation
				 */
				// 1.1 mutate
				mtdAttr.removeAnnotation(anno.getTypeName());
				// 1.2 record and write
				ArrayList<String> tmp_Configs = new ArrayList<>();
				ArrayList<String> tmp_body = new ArrayList<>();
				buildConfigAndCheckBody(tmp_Configs, tmp_body, anno.getTypeName(), stmt, otherinfo);
				INDEX++;
				url2ModifyedInfo.get(url2ModifyedInfo.size() - 1)
						.addModifiedRecord(new ModifiedInfo(stmt, 1, type, Modification.remove, tmp_Configs, tmp_body, false), INDEX);
				boolean success = write2SingleClassFile(cc, outSubPath1);
				writeRequestSequence();
				// 1.3 recover
				mtdAttr.addAnnotation(anno);
				modified1 = success;

				// will add new method then change
				String newMtdName = "mhn_" + method.getName();
				/*
				 * mutation-2: if the attribute has no value, duplicate one with only current
				 * annotation
				 */
				if (cc.isFrozen())
					cc.defrost();
				cc = ClassPool.getDefault().makeClass(classFile);
				CtClass addClass2 = CtObjectCopyHelper.addNewClassCopy(cc, cc.getName(), false);
				CtMethod addMtd2 = CtObjectCopyHelper.addNewMethodCopy(addClass2, method, newMtdName, false); /* modify class */
				if (addMtd2 != null) {
					// 2.1 mutate
					MethodInfo addMtdInfo2 = addMtd2.getMethodInfo();
					if (addMtdInfo2 != null) {
						AttributeInfo addMtdAttrInfo2 = addMtdInfo2.getAttribute(AnnotationsAttribute.visibleTag);
						if (addMtdAttrInfo2 instanceof AnnotationsAttribute) {
							AnnotationsAttribute addMtdAttr2 = (AnnotationsAttribute) addMtdAttrInfo2;
							// only retain the concerned annotation
							for (Annotation anno2 : addMtdAttr2.getAnnotations())
								if (!anno2.getTypeName().equals(anno.getTypeName()))
									addMtdAttr2.removeAnnotation(anno2.getTypeName());
						}
					}
					// 2.2 record and write
					ArrayList<String> tmp_Configs2 = new ArrayList<>();
					ArrayList<String> tmp_body2 = new ArrayList<>();
					String stmt2 = stmt.substring(0, stmt.lastIndexOf('.')) + "." + newMtdName + stmt.substring(stmt.indexOf('('));
					buildConfigAndCheckBody(tmp_Configs2, tmp_body2, anno.getTypeName(), stmt2, otherinfo);
					INDEX++;
					url2ModifyedInfo.get(url2ModifyedInfo.size() - 1)
							.addModifiedRecord(new ModifiedInfo(stmt, 1, type, Modification.add, tmp_Configs2, tmp_body2, true), INDEX);
					success = false;
					success = write2SingleClassFile(addClass2, outSubPath1);
					writeRequestSequence();
					// 2.3 recover
					// do nothing since it is modified on copied class
					addClass2.detach();
					modified2 = success;
				}

				/*
				 * mutation-3: if find the connection then change
				 */
				Set<String> mems = anno.getMemberNames();
				if (urlEnable && !isDeploy && mems != null && !mems.isEmpty()) {
					if (cc.isFrozen())
						cc.defrost();
					cc = ClassPool.getDefault().makeClass(classFile);
					CtClass addClass3 = CtObjectCopyHelper.addNewClassCopy(cc, cc.getName(), false);
					CtMethod addMtd3 = CtObjectCopyHelper.addNewMethodCopy(addClass3, method, newMtdName, false);/* modify class */

					// find annotation on this method
					if (addMtd3 != null) {
						MethodInfo addMtdInfo3 = addMtd3.getMethodInfo();
						if (addMtdInfo3 != null) {
							AttributeInfo addMtdAttrInfo3 = addMtdInfo3.getAttribute(AnnotationsAttribute.visibleTag);
							if (addMtdAttrInfo3 != null && (addMtdAttrInfo3 instanceof AnnotationsAttribute)) {
								AnnotationsAttribute addMtdAttr3 = (AnnotationsAttribute) addMtdAttrInfo3;
								Annotation concernAnno = addMtdAttr3.getAnnotation(anno.getTypeName());
								for (String mem : concernAnno.getMemberNames()) {
									MemberValue memVal = concernAnno.getMemberValue(mem);

									if (memVal instanceof StringMemberValue) {
										String originalValue = ((StringMemberValue) memVal).getValue();
										// find the connection between configure value and coming url
										String[] newInfo = buildNewTriggerURL(requestInfoLog, originalValue, "_new");
										String newReqInfoLog = newInfo[0];
										String newVal = newInfo[1];
										if (newReqInfoLog != null) {
											// 3.1 mutate
											((StringMemberValue) memVal).setValue(newVal);
											addMtdAttr3.addAnnotation(concernAnno); // rewrite this modified annotation
											// 3.2 record and write
											ArrayList<String> tmp_Configs3 = new ArrayList<>();
											ArrayList<String> tmp_body3 = new ArrayList<>();
											String stmt3 = stmt.substring(0, stmt.lastIndexOf('.')) + "." + newMtdName + stmt.substring(stmt.indexOf('('));
											buildConfigAndCheckBody(tmp_Configs3, tmp_body3, anno.getTypeName(), stmt3, otherinfo);
											INDEX++;
											url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
													new ModifiedInfo(stmt, 1, type, Modification.add, tmp_Configs3, tmp_body3, true), INDEX, newReqInfoLog);
											success = false;
											success = write2SingleClassFile(addClass3, outSubPath1);
											writeRequestSequence(newReqInfoLog);
											// 3.3 recover
											((StringMemberValue) memVal).setValue(originalValue);
											addMtdAttr3.addAnnotation(concernAnno); // rewrite this modified annotation
											modified3 = success;
										}
									} else if (memVal instanceof ArrayMemberValue) {
										ArrayMemberValue arrayMemVal = (ArrayMemberValue) memVal;
										MemberValue[] memVals = arrayMemVal.getValue();
										if (arrayMemVal != null && memVals != null && (arrayMemVal.getType() instanceof StringMemberValue)
												&& (memVals.length > 0)) {
											String originalValue = null;
											String newVal = null;
											String newReqInfoLog = null;
											int ii = 0;
											boolean modifyArray = false;
											for (; ii < memVals.length; ii++) {
												originalValue = ((StringMemberValue) (memVals[ii])).getValue();
												// find the connection between configure value and coming url
												String[] newInfo = buildNewTriggerURL(requestInfoLog, originalValue, "_new");
												newReqInfoLog = newInfo[0];
												newVal = newInfo[1];
												if (newReqInfoLog != null) {
													modifyArray = true;
													break;
												}
											}
											if (modifyArray) {
												// 3.1 mutate
												((StringMemberValue) (memVals[ii])).setValue(newVal);
												addMtdAttr3.addAnnotation(concernAnno); // rewrite this modified annotation
												// 3.2 record and write
												ArrayList<String> tmp_Configs3 = new ArrayList<>();
												ArrayList<String> tmp_body3 = new ArrayList<>();
												String stmt3 = stmt.substring(0, stmt.lastIndexOf('.')) + "." + newMtdName + stmt.substring(stmt.indexOf('('));
												buildConfigAndCheckBody(tmp_Configs3, tmp_body3, anno.getTypeName(), stmt3, otherinfo);
												INDEX++;
												url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
														new ModifiedInfo(stmt, 1, type, Modification.add, tmp_Configs3, tmp_body3, true), INDEX, newReqInfoLog);
												success = false;
												success = write2SingleClassFile(addClass3, outSubPath1);
												writeRequestSequence(newReqInfoLog);
												// 3.3 recover
												// later mem may use this addClass
												((StringMemberValue) (memVals[ii])).setValue(originalValue);
												addMtdAttr3.addAnnotation(concernAnno); // rewrite this modified annotation
												modified3 = success;
											}
										}
									}
								}
							}
						}
						addClass3.detach();
					}
				}
			}
		}
		return (modified1 || modified2 || modified3);
	}

	/**
	 * mutate the annotation on field: </br>
	 * - delete </br>
	 * - add1 : copy and rename the class/method/field and only retain the concerned
	 * annotation</br>
	 */
	@Override
	protected boolean modifyOnField_annotation(CtField field, String fieldSig, CtClass cc, String classFilePath, String stmt, ModifiedSemantic type,
			HashMap<String, Integer> visitedTags2Count) {
		boolean modified1 = false;
		boolean modified2 = false;

		FieldInfo info = field.getFieldInfo();
		AttributeInfo fieldAttrInfo = info.getAttribute(AnnotationsAttribute.visibleTag);
		if (fieldAttrInfo instanceof AnnotationsAttribute) {
			HashSet<Annotation> candidateFieldAnnos = new HashSet<>();

			// collect annos on this method
			AnnotationsAttribute fieldAttr = (AnnotationsAttribute) fieldAttrInfo;
			if (fieldAttr.getAnnotations().length > 0) {
				for (Annotation anno : fieldAttr.getAnnotations()) {
					String annoname = anno.getTypeName();
					if (!ConfigureUtil.isApplicationClass(annoname.replace('.', '/')))
						candidateFieldAnnos.add(anno);
				}
			}

			for (Annotation anno : candidateFieldAnnos) {
				String path = anno.getTypeName();
				String outSubPath = calSubPath(cc.getName(), classFilePath);

				// remove duplicate
				if (!visitedTags2Count.containsKey(path))
					visitedTags2Count.put(path, 0);
				if (visitedTags2Count.get(path) > thresdhold)
					continue;
				visitedTags2Count.put(path, visitedTags2Count.get(path) + 1);

				/*
				 * mutation-1 : remove directly remove and record each annotation
				 */
				// 1.1 mutate
				fieldAttr.removeAnnotation(anno.getTypeName());
				// 1.2 record and write
				ArrayList<String> tmp_Configs1 = new ArrayList<>();
				ArrayList<String> tmp_body1 = new ArrayList<>();
				tmp_Configs1.add(anno.getTypeName());
				tmp_body1.add(fieldSig);
				tmp_body1.add(stmt); // check position, a function call statement
				INDEX++;
				url2ModifyedInfo.get(url2ModifyedInfo.size() - 1)
						.addModifiedRecord(new ModifiedInfo(fieldSig, -1, type, Modification.remove, tmp_Configs1, tmp_body1, false), INDEX);
				boolean success = write2SingleClassFile(cc, outSubPath);
				writeRequestSequence();
				// 1.3 recover this annotation
				fieldAttr.addAnnotation(anno);
				modified1 = success;

				// later create and add this field into the copy of cc
				String addFieldName = "mhnField_" + field.getName();
				/*
				 * mutation-2: if the attribute has no value, duplicate one with only current
				 * annotation
				 */
				CtClass addClass2 = CtObjectCopyHelper.addNewClassCopy(cc, cc.getName(), false);
				CtField addField2 = CtObjectCopyHelper.addNewFieldCopy(addClass2, field, addFieldName, false, null);
				if (addField2 != null) {
					// 2.1 mutate
					FieldInfo addFieldInfo2 = addField2.getFieldInfo();
					if (addFieldInfo2 != null) {
						AttributeInfo addFieldAttrInfo2 = addFieldInfo2.getAttribute(AnnotationsAttribute.visibleTag);
						if (addFieldAttrInfo2 instanceof AnnotationsAttribute) {
							AnnotationsAttribute addFieldAttr2 = (AnnotationsAttribute) addFieldAttrInfo2;
							// only retain the concerned annotation
							for (Annotation anno2 : addFieldAttr2.getAnnotations())
								if (!anno2.getTypeName().equals(anno.getTypeName()))
									addFieldAttr2.removeAnnotation(anno2.getTypeName());
						}
					}
					// 2.2 record and write
					ArrayList<String> tmp_Configs2 = new ArrayList<>();
					ArrayList<String> tmp_body2 = new ArrayList<>();
					tmp_Configs2.add(anno.getTypeName());
					tmp_body2.add(addFieldName);
					// if the statement is getField/setField, also change the statement
					String stmt2 = stmt;
					String str1 = stmt.substring(0, stmt.indexOf('('));
					if (str1.toLowerCase().endsWith(field.getName().toLowerCase())) {
						String str2 = str1.substring(str1.lastIndexOf('.'));
						String prefix = str2.substring(0, str2.length() - field.getName().length());
						stmt2 = str1.substring(0, stmt.lastIndexOf('.')) + prefix + addFieldName.substring(0, 1).toUpperCase() + addFieldName.substring(1)
								+ stmt.substring(stmt.indexOf('('));
					}
					tmp_body2.add(stmt2); // check position, a function call statement
					INDEX++;
					url2ModifyedInfo.get(url2ModifyedInfo.size() - 1)
							.addModifiedRecord(new ModifiedInfo(fieldSig, -1, type, Modification.add, tmp_Configs2, tmp_body2, true), INDEX);
					// write
					success = write2SingleClassFile(addClass2, outSubPath);
					writeRequestSequence();
					// 2.3 recover
					// do nothing since it is modified on copied class
					modified2 = success;
				}
			}
		}
		return modified1 || modified2;
	}

	/**
	 * mutate the xml on class:</br>
	 * - delete: remove the xml node </br>
	 * - add1: copy and rename the pure class/method/field without annotation, and
	 * replace the XML-node configure with new copied class/method/field</br>
	 * - add2: if has attribute value and find the connect point, copy the
	 * class/method/field with old annotations then modify the connect point (only
	 * do this when it is not deploy log)</br>
	 * </br>
	 * 
	 * a XML configuration may be in two forms:</br>
	 * 1. node-...-node-text (the text value is concerned class/method/field). The
	 * configuration represent as "node-...-node-[text]". The configuration value
	 * elements share the same parent node and the text of this node is the
	 * configure value</br>
	 * 2. node-...-node-attr (the attr value is concerned class/method/field). The
	 * configuration represent as "node-...-node-attrName". The mark value set in
	 * other attr that under the same node</br>
	 * 
	 */
	@Override
	protected boolean modifyOnClass_XML(Document document, HashMap<Element, HashSet<Attribute>> node2attrs_class, String classFilePath, String stmt,
			ClassPool pool, String xmlFile, ModifiedSemantic type, ArrayList<String> otherinfo, HashMap<String, Integer> visitedTags2Count, boolean urlEnable) {
		String requestInfoLog = url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).getUrl();
		boolean isDeploy = false;
		if (requestInfoLog == "deploy")
			isDeploy = true;

		boolean modified1 = false;
		boolean modified2 = false;
		boolean modified3 = false;

		for (Element ele : node2attrs_class.keySet()) {
			HashSet<Attribute> attrs = node2attrs_class.get(ele);
			// for each attribute
			for (Attribute attr : attrs) {
				String path;
				boolean isText = false;
				if (attr == null) {
					// text
					path = ele.getPath();
					isText = true;
				} else {
					path = attr.getPath();
				}

				// remove duplicate
				if (!visitedTags2Count.containsKey(path))
					visitedTags2Count.put(path, 0);
				if (visitedTags2Count.get(path) > thresdhold)
					continue;
				visitedTags2Count.put(path, visitedTags2Count.get(path) + 1);

				/*
				 * mutation-1 : remove directly remove and record each annotation
				 */
				// mutate, record, write and recover
				ArrayList<String> tmp_Configs1 = new ArrayList<>();
				ArrayList<String> tmp_body1 = new ArrayList<>();
				buildConfigAndCheckBody(tmp_Configs1, tmp_body1, path, stmt, otherinfo);
				removeXMLElementRecordRecover(document, ele, new ModifiedInfo(stmt, 0, type, Modification.remove, tmp_Configs1, tmp_body1, false), xmlFile);
				modified1 = true;

				// latter do change on the copy of original cc
				CtClass cc = readCtClass(classFilePath, pool);
				if (cc == null)
					continue;
				String addClassName = cc.getPackageName() + "." + "mhnClass_" + cc.getSimpleName();
				String outSubPath = calSubPath(cc.getName(), classFilePath); // for write file

				/*
				 * mutation-2: duplicate one with only current annotation
				 */
				CtClass addClass2 = CtObjectCopyHelper.addNewClassCopy(cc, addClassName, true);
				String originalVal;
				// 2.1 mutate
				if (isText) {
					originalVal = ele.getText();
					ele.setText(addClassName);
				} else {
					originalVal = attr.getValue();
					attr.setValue(addClassName);
				}
				// 2.2 record and write
				ArrayList<String> tmp_Configs2 = new ArrayList<>();
				ArrayList<String> tmp_body2 = new ArrayList<>();
				String stmt2 = addClass2.getName() + "." + stmt.substring(stmt.lastIndexOf('.') + 1);
				buildConfigAndCheckBody(tmp_Configs2, tmp_body2, path, stmt2, otherinfo);
				INDEX++;
				url2ModifyedInfo.get(url2ModifyedInfo.size() - 1)
						.addModifiedRecord(new ModifiedInfo(stmt, 0, type, Modification.add, tmp_Configs2, tmp_body2, true), INDEX);
				// write multiple files
				boolean success;
				if (isJar) {
					ArrayList<CtClass> ctClasses = new ArrayList<>();
					ArrayList<String> subPaths = new ArrayList<>();
					ArrayList<Document> documents = new ArrayList<>();
					ArrayList<String> xmlFiles = new ArrayList<>();
					ctClasses.add(addClass2);
					subPaths.add(outSubPath);
					documents.add(document);
					xmlFiles.add(xmlFile);
					success = write2JarFiles(ctClasses, subPaths, documents, xmlFiles);
				} else {
					// write class
					success = write2SingleClassFile(addClass2, outSubPath);
					// write xml
					write2SingleXMLFile(document, xmlFile);
				}
				writeRequestSequence();
				// 2.3 recover
				if (isText) {
					ele.setText(originalVal);
					modified2 = success;
				} else {
					attr.setValue(originalVal);
					modified2 = success;
				}

				/*
				 * mutation-3: if find the connection then change
				 */
				if (!isDeploy && urlEnable) {
					CtClass addClass3 = CtObjectCopyHelper.addNewClassCopy(cc, addClassName, false);
					// find this xml configuration path whether has connection point
					if (isText) {
						// xml configuration is text
						HashSet<Element> candidateTextConfigValueEles = getXMLTextConfigureValue(ele);
						// whether it has connection with url
						for (Element configEle : candidateTextConfigValueEles) {
							String originalConnectValue = configEle.getText();
							String originalClassVal = ele.getText();
							// find the connection between configure value and coming url
							String[] newInfo = buildNewTriggerURL(requestInfoLog, originalConnectValue.trim(), "_new");
							String newReqInfoLog = newInfo[0];
							String newConnectVal = newInfo[1];
							if (newReqInfoLog != null) {
								// 3.1 mutate
								configEle.setText(newConnectVal);
								ele.setText(addClassName);
								// 3.2 record and write
								ArrayList<String> tmp_Configs3 = new ArrayList<>();
								ArrayList<String> tmp_body3 = new ArrayList<>();
								String stmt3 = addClass3.getName() + "." + stmt.substring(stmt.lastIndexOf('.') + 1);
								buildConfigAndCheckBody(tmp_Configs3, tmp_body3, path, stmt3, otherinfo);
								INDEX++;
								url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
										new ModifiedInfo(stmt, 0, type, Modification.add, tmp_Configs3, tmp_body3, true), INDEX, newReqInfoLog);
								// write multiple files
								if (isJar) {
									ArrayList<CtClass> ctClasses = new ArrayList<>();
									ArrayList<String> subPaths = new ArrayList<>();
									ArrayList<Document> documents = new ArrayList<>();
									ArrayList<String> xmlFiles = new ArrayList<>();
									ctClasses.add(addClass3);
									subPaths.add(outSubPath);
									documents.add(document);
									xmlFiles.add(xmlFile);
									success = write2JarFiles(ctClasses, subPaths, documents, xmlFiles);
								} else {
									// write class
									success = write2SingleClassFile(addClass3, outSubPath);
									// write xml
									write2SingleXMLFile(document, xmlFile);
								}
								writeRequestSequence(newReqInfoLog);
								// 3.3 recover
								configEle.setText(originalConnectValue);
								ele.setText(originalClassVal);
								modified3 = success;
							}
						}
					} else {
						// xml configuration is attribute
						String originalClassVal = attr.getValue();
						for (Object configAttr0 : ele.attributes()) {
							if (configAttr0.equals(attr))
								continue;
							if (configAttr0 instanceof Attribute) {
								Attribute configAttr = (Attribute) configAttr0;
								String originalConnectValue = configAttr.getValue();
								// find the connection between configure value and coming url
								String[] newInfo = buildNewTriggerURL(requestInfoLog, originalConnectValue.trim(), "_new");
								String newReqInfoLog = newInfo[0];
								String newConnectVal = newInfo[1];
								if (newReqInfoLog != null) {
									// 3.1 mutate: copy one and change
									configAttr.setValue(newConnectVal);
									attr.setValue(addClassName);
									Element copy = ele.createCopy();
									configAttr.setValue(originalConnectValue);
									attr.setValue(originalClassVal);
									ele.getParent().add(copy);
									// 3.2 record and write
									ArrayList<String> tmp_Configs3 = new ArrayList<>();
									ArrayList<String> tmp_body3 = new ArrayList<>();
									String stmt3 = addClass3.getName() + "." + stmt.substring(stmt.lastIndexOf('.') + 1);
									buildConfigAndCheckBody(tmp_Configs3, tmp_body3, path, stmt3, otherinfo);
									INDEX++;
									url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
											new ModifiedInfo(stmt, 0, type, Modification.add, tmp_Configs3, tmp_body3, true), INDEX, newReqInfoLog);
									// write multiple files
									if (isJar) {
										ArrayList<CtClass> ctClasses = new ArrayList<>();
										ArrayList<String> subPaths = new ArrayList<>();
										ArrayList<Document> documents = new ArrayList<>();
										ArrayList<String> xmlFiles = new ArrayList<>();
										ctClasses.add(addClass3);
										subPaths.add(outSubPath);
										documents.add(document);
										xmlFiles.add(xmlFile);
										success = write2JarFiles(ctClasses, subPaths, documents, xmlFiles);
									} else {
										// write class
										success = write2SingleClassFile(addClass3, outSubPath);
										// write xml
										write2SingleXMLFile(document, xmlFile);
									}
									writeRequestSequence(newReqInfoLog);
									// 3.3 recover
									ele.getParent().remove(copy);
									modified3 = success;
								}
							}
						}
					}
				}
			}
		}
		return (modified1 || modified2 || modified3);
	}

	/**
	 * mutate the XML path on method:</br>
	 * - delete: remove the xml node </br>
	 * - add1: copy and rename the pure class/method/field without annotation, and
	 * replace the XML-node configure with new copied class/method/field</br>
	 * - add2: if has attribute value and find the connect point, copy the
	 * class/method/field with old annotations then modify the connect point (only
	 * do this when it is not deploy log) </br>
	 * </br>
	 * a XML configuration may be in two forms:</br>
	 * 1. node-...-node-text (the text value is concerned class/method/field). The
	 * configuration represent as "node-...-node-[text]". The configuration value
	 * elements share the same parent node and the text of this node is the
	 * configure value</br>
	 * 2. node-...-node-attr (the attr value is concerned class/method/field). The
	 * configuration represent as "node-...-node-attrName". The mark value set in
	 * other attr that under the same node</br>
	 * </br>
	 * 
	 */
	@Override
	protected boolean modifyOnMethod_XML(Document document, HashMap<Element, HashSet<Attribute>> node2attrs_method, String classFilePath, String stmt,
			ClassPool pool, String xmlFile, ModifiedSemantic type, ArrayList<String> otherinfo, HashMap<String, Integer> visitedTags2Count, boolean urlEnable) {
		String requestInfoLog = url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).getUrl();
		boolean isDeploy = false;
		if (requestInfoLog == "deploy")
			isDeploy = true;

		boolean modified1 = false;
		boolean modified2 = false;
		boolean modified3 = false;

		for (Element ele : node2attrs_method.keySet()) {
			HashSet<Attribute> attrs = node2attrs_method.get(ele);

			for (Attribute attr : attrs) {
				String path;
				boolean isText = false;
				if (attr == null) {
					// text
					path = ele.getPath();
					isText = true;
				} else {
					path = attr.getPath();
				}

				// remove duplicate
				if (!visitedTags2Count.containsKey(path))
					visitedTags2Count.put(path, 0);
				if (visitedTags2Count.get(path) > thresdhold)
					continue;
				visitedTags2Count.put(path, visitedTags2Count.get(path) + 1);

				/*
				 * mutation-1 : remove directly remove and record each annotation if only one
				 * attribute value matches
				 */
				if (attrs.size() == 1) {
					// mutate, record, write and recover
					ArrayList<String> tmp_Configs1 = new ArrayList<>();
					ArrayList<String> tmp_body1 = new ArrayList<>();
					buildConfigAndCheckBody(tmp_Configs1, tmp_body1, path, stmt, otherinfo);
					removeXMLElementRecordRecover(document, ele, new ModifiedInfo(stmt, 1, type, Modification.remove, tmp_Configs1, tmp_body1, false), xmlFile);
					modified1 = true;
				}

				// latter do change on the copy of original cc
				CtClass cc = readCtClass(classFilePath, pool);
				if (cc == null)
					continue;
				String name2 = stmt.substring(0, stmt.lastIndexOf('.'));
				if (!cc.getClassFile().getName().equals(name2))
					continue;
				CtMethod method = SearchInCtClassHelper.findCtMethod(cc, stmt);
				if (method == null)
					continue;
				String newMtdName = "mhn_" + method.getName();
				CtMethod addMtd = CtObjectCopyHelper.addNewMethodCopy(cc, method, newMtdName, true);/* modify class */
				if (addMtd == null)
					continue;
				String outSubPath = calSubPath(cc.getName(), classFilePath); // for write file

				/*
				 * mutation-2: duplicate one with only current annotation
				 * 
				 * for each matched method attr, copy a method and modify one xml attribute
				 */
				// 2.1 mutate
				String originalVal;
				// 2.1 mutate
				if (isText) {
					originalVal = ele.getText();
					ele.setText(newMtdName);
				} else {
					originalVal = attr.getValue();
					attr.setValue(newMtdName);
				}
				// 2.2 record, write
				ArrayList<String> tmp_Configs2 = new ArrayList<>();
				ArrayList<String> tmp_body2 = new ArrayList<>();
				String stmt2 = stmt.substring(0, stmt.lastIndexOf('.')) + "." + newMtdName + stmt.substring(stmt.indexOf('('));
				buildConfigAndCheckBody(tmp_Configs2, tmp_body2, path, stmt2, otherinfo);
				INDEX++;
				url2ModifyedInfo.get(url2ModifyedInfo.size() - 1)
						.addModifiedRecord(new ModifiedInfo(stmt, 1, type, Modification.add, tmp_Configs2, tmp_body2, true), INDEX);
				// write multiple files
				boolean success;
				if (isJar) {
					ArrayList<CtClass> ctClasses = new ArrayList<>();
					ArrayList<String> subPaths = new ArrayList<>();
					ArrayList<Document> documents = new ArrayList<>();
					ArrayList<String> xmlFiles = new ArrayList<>();
					ctClasses.add(cc);
					subPaths.add(outSubPath);
					documents.add(document);
					xmlFiles.add(xmlFile);
					success = write2JarFiles(ctClasses, subPaths, documents, xmlFiles);
				} else {
					// write class
					success = write2SingleClassFile(cc, outSubPath);
					// write xml
					write2SingleXMLFile(document, xmlFile);
				}
				writeRequestSequence();
				// 2.3 recover
				if (isText) {
					ele.setText(originalVal);
					modified2 = success;
				} else {
					attr.setValue(originalVal);
					modified2 = success;
				}
				/*
				 * mutation-3: if find the connection then change
				 */
				if (!isDeploy && urlEnable) {
					if (isText) {
						// xml configuration is text
						// TODO：now do not deal with method in text
					} else {
						// xml configuration is attribute
						String originalMtdVal = attr.getValue();
						for (Object configAttr0 : ele.attributes()) {
							if (configAttr0.equals(attr))
								continue;
							if (configAttr0 instanceof Attribute) {
								Attribute configAttr = (Attribute) configAttr0;
								String originalConnectValue = configAttr.getValue();
								// find the connection between configure value and coming url
								String[] newInfo = buildNewTriggerURL(requestInfoLog, originalConnectValue.trim(), "_new");
								String newReqInfoLog = newInfo[0];
								String newConnectVal = newInfo[1];
								if (newReqInfoLog != null) {
									// 3.1 mutate
									configAttr.setValue(newConnectVal);
									attr.setValue(newMtdName);
									Element copy = ele.createCopy();
									configAttr.setValue(originalConnectValue);
									attr.setValue(originalMtdVal);
									ele.getParent().add(copy);
									// 3.2 record and write
									ArrayList<String> tmp_Configs3 = new ArrayList<>();
									ArrayList<String> tmp_body3 = new ArrayList<>();
									String stmt3 = stmt.substring(0, stmt.lastIndexOf('.')) + "." + newMtdName + stmt.substring(stmt.indexOf('('));
									buildConfigAndCheckBody(tmp_Configs3, tmp_body3, path, stmt3, otherinfo);
									INDEX++;
									url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
											new ModifiedInfo(stmt, 1, type, Modification.add, tmp_Configs3, tmp_body3, true), INDEX, newReqInfoLog);
									// write multiple files
									if (isJar) {
										ArrayList<CtClass> ctClasses = new ArrayList<>();
										ArrayList<String> subPaths = new ArrayList<>();
										ArrayList<Document> documents = new ArrayList<>();
										ArrayList<String> xmlFiles = new ArrayList<>();
										ctClasses.add(cc);
										subPaths.add(outSubPath);
										documents.add(document);
										xmlFiles.add(xmlFile);
										success = write2JarFiles(ctClasses, subPaths, documents, xmlFiles);
									} else {
										// write class
										success = write2SingleClassFile(cc, outSubPath);
										// write xml
										write2SingleXMLFile(document, xmlFile);
									}
									writeRequestSequence(newReqInfoLog);
									// 3.3 recover
									ele.getParent().remove(copy);
									modified3 = success;
								}
							}
						}
					}
				}
			}
		}
		return modified1 || modified2 || modified3;
	}

	/**
	 * mutate the XML path on method:</br>
	 * - delete: remove the xml node </br>
	 * - add1: copy and rename the pure class/method/field without annotation, and
	 * replace the XML-node configure with new copied class/method/field</br>
	 * 
	 * </br>
	 * a XML configuration may be in two forms:</br>
	 * 1. node-...-node-text (the text value is concerned class/method/field). The
	 * configuration represent as "node-...-node-[text]". The configuration value
	 * elements share the same parent node and the text of this node is the
	 * configure value</br>
	 * 2. node-...-node-attr (the attr value is concerned class/method/field). The
	 * configuration represent as "node-...-node-attrName". The mark value set in
	 * other attr that under the same node</br>
	 * </br>
	 * 
	 */
	@Override
	protected boolean modifyOnField_XML(String fieldSig, Document document, HashMap<Element, HashSet<Attribute>> node2attrs_fields, String classFilePath,
			String stmt, ClassPool pool, String xmlFile, ModifiedSemantic type, HashMap<String, Integer> visitedTags2Count) {
		boolean modified1 = false;
		boolean modified2 = false;

		String fieldFullName = fieldSig.substring(0, fieldSig.indexOf(':'));
		String fieldName = fieldFullName.substring(fieldFullName.lastIndexOf('.') + 1);

		for (Element ele : node2attrs_fields.keySet()) {
			if (ele.getParent() == null)
				continue;
			HashSet<Attribute> attrs = node2attrs_fields.get(ele);

			for (Attribute attr : attrs) {
				String path;
				boolean isText = false;
				if (attr == null) {
					// text
					path = ele.getPath();
					isText = true;
				} else {
					path = attr.getPath();
				}

				// remove duplicate
				if (!visitedTags2Count.containsKey(path))
					visitedTags2Count.put(path, 0);
				if (visitedTags2Count.get(path) > thresdhold)
					continue;
				visitedTags2Count.put(path, visitedTags2Count.get(path) + 1);

				/*
				 * mutation-1 : remove directly remove and record each annotation if only one
				 * attribute value matches
				 */
				if (attrs.size() == 1) {
					// mutate, record, write and recover
					ArrayList<String> tmp_Configs1 = new ArrayList<>();
					ArrayList<String> tmp_body1 = new ArrayList<>();
					tmp_Configs1.add(path);
					tmp_body1.add(fieldName);
					// TODO: haven't apply
//					tmp_body1.add(fieldSig);
					tmp_body1.add(stmt); // check position, a function call statement
					removeXMLElementRecordRecover(document, ele, new ModifiedInfo(fieldSig, -1, type, Modification.remove, tmp_Configs1, tmp_body1, false),
							xmlFile);
					modified1 = true;
				}

				// just add a new field to this cc
				CtClass cc = readCtClass(classFilePath, pool);
				if (cc == null)
					continue;
				String name2 = stmt.substring(0, stmt.lastIndexOf('.'));
				if (!cc.getClassFile().getName().equals(name2))
					continue;
				CtField field = SearchInCtClassHelper.findCtField(cc, fieldName);
				if (field == null)
					continue;

				String newFieldName = "mhn_" + field.getName();
				CtField addfield = CtObjectCopyHelper.addNewFieldCopy(cc, field, newFieldName, true, null);/* modify class */
				if (addfield == null)
					continue;
				String outSubPath = calSubPath(cc.getName(), classFilePath); // for write file

				/*
				 * mutation-2: duplicate one with only current xml
				 * 
				 * for each matched method attr, copy a method and modify one xml attribute
				 */
				// 2.1 mutate
				String originalVal;
				if (isText) {
					// xml configuration is text
					// TODO：now do not deal with method in text
				} else {
					// 2.1 mutate
					originalVal = attr.getValue();
					attr.setValue(newFieldName);
					Element copy = ele.createCopy();
					attr.setValue(originalVal);
					ele.getParent().add(copy);
					// 2.2 record and write
					ArrayList<String> tmp_Configs2 = new ArrayList<>();
					ArrayList<String> tmp_body2 = new ArrayList<>();
					tmp_Configs2.add(attr.getPath());
					tmp_body2.add(newFieldName);
					// if the statement is getField/setField, also change the statement
					String stmt2 = stmt;
					String str1 = stmt.substring(0, stmt.indexOf('('));
					if (str1.toLowerCase().endsWith(field.getName().toLowerCase())) {
						String str2 = str1.substring(str1.lastIndexOf('.'));
						String prefix = str2.substring(0, str2.length() - field.getName().length());
						stmt2 = str1.substring(0, stmt.lastIndexOf('.')) + prefix + newFieldName.substring(0, 1).toUpperCase() + newFieldName.substring(1)
								+ stmt.substring(stmt.indexOf('('));
					}
					tmp_body2.add(stmt2); // check position, a function call statement
					INDEX++;
					url2ModifyedInfo.get(url2ModifyedInfo.size() - 1)
							.addModifiedRecord(new ModifiedInfo(fieldSig, -1, type, Modification.add, tmp_Configs2, tmp_body2, true), INDEX);
					// write multiple files
					boolean success;
					if (isJar) {
						ArrayList<CtClass> ctClasses = new ArrayList<>();
						ArrayList<String> subPaths = new ArrayList<>();
						ArrayList<Document> documents = new ArrayList<>();
						ArrayList<String> xmlFiles = new ArrayList<>();
						ctClasses.add(cc);
						subPaths.add(outSubPath);
						documents.add(document);
						xmlFiles.add(xmlFile);
						success = write2JarFiles(ctClasses, subPaths, documents, xmlFiles);
					} else {
						// write to xml
						// write class
						success = write2SingleClassFile(cc, outSubPath);
						// write xml
						write2SingleXMLFile(document, xmlFile);
					}
					writeRequestSequence();
					// 2.3 recover
					ele.getParent().remove(copy);
					modified2 = success;
				}
			}
		}

		return modified1 || modified2;
	}

	/**
	 * First find the connect between the function that contains framework call and
	 * the target function. </br>
	 * Then change the target configurations
	 */
	@Override
	public void modify_IndirectCall(DynamicCG cg, ClassPool pool, HashMap<String, HashSet<String>> visited_indirectCall,
			HashMap<String, Integer> visitedTags2Count_indirectCall) {
		String requestInfoLog = url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).getUrl();
		boolean isDeploy = false;
		if (requestInfoLog == "deploy")
			isDeploy = true;

		if (isDeploy)
			super.modify_IndirectCall(cg, pool, visited_indirectCall, visitedTags2Count_indirectCall);
		else {
			for (DynamicCGNode node : cg.getNodes()) {
				String callerStmt = node.getStmt();
				String callerFullClass0 = callerStmt.substring(0, callerStmt.lastIndexOf('.'));
				String caller_file = findMatchedClassFile(callerFullClass0);
				if (caller_file == null)
					continue;
				boolean isMain = false;
				if (callerStmt.contains(".main([Ljava/lang/String;)V"))
					isMain = true;
				// collect all xml configurations on caller method
				ArrayList<String> caller_xml_class = new ArrayList<>();
				ArrayList<String> caller_xml_method = new ArrayList<>();
				ArrayList<Set<String>> caller_xml_vals = new ArrayList<>();
				HashMap<String, HashMap<Element, HashSet<Attribute>>> caller_xmlFile2node2XMLattrs_class = new HashMap<>();
				HashMap<String, HashMap<Element, HashSet<Attribute>>> caller_xmlFile2node2XMLattrs_method = new HashMap<>();
				collectAllXMLConfiguration(callerStmt, callerFullClass0, caller_xmlFile2node2XMLattrs_class, caller_xmlFile2node2XMLattrs_method,
						caller_xml_class, caller_xml_method, caller_xml_vals);
				// for collecting annotations on caller method
				CtClass callerCC = readCtClass(caller_file, pool);
				if (callerCC == null)
					continue;
				ArrayList<Annotation> caller_NotNullAnnos_class = new ArrayList<>();
				ArrayList<Annotation> caller_NotNullAnnos_method = new ArrayList<>();
				ArrayList<Set<String>> caller_anno_vals = new ArrayList<>();
				collectAllNotNULLAnnoConfiguration(callerStmt, callerCC, caller_NotNullAnnos_class, caller_NotNullAnnos_method, caller_anno_vals);

				if (caller_xmlFile2node2XMLattrs_class.isEmpty() && caller_xmlFile2node2XMLattrs_method.isEmpty() && caller_NotNullAnnos_class.isEmpty()
						&& caller_NotNullAnnos_method.isEmpty())
					continue;

				for (CallsiteInfo cs : node.getCallsites()) {
					String csStmt = cs.getStmt();

					String cl0 = csStmt.substring("[callsite]".length(), csStmt.indexOf('('));
					String csClassName = cl0.replace('.', '/');

					String csPureStmt0 = csStmt.substring("[callsite]".length());
					String csPureStmt1 = csPureStmt0.substring(0, csPureStmt0.indexOf(']'));
					String csPureStmt = csPureStmt1.substring(0, csPureStmt1.lastIndexOf('['));

					if (!ConfigureUtil.isApplicationClass(csClassName) && !ConfigureUtil.isCommonJavaClass(csClassName)) {
						if (!cs.isClosed())
							continue;

						// only concern about framework invoke
						for (DynamicCGNode targetCall : cs.getActual_targets()) {
							String targetStmt = targetCall.getStmt();
							// remove duplicate
							if (visited_indirectCall.containsKey(callerStmt + csPureStmt)
									&& visited_indirectCall.get(callerStmt + csPureStmt).contains(targetStmt))
								continue;
							if (!visited_indirectCall.containsKey(callerStmt + csPureStmt)) {
								visited_indirectCall.put(callerStmt + csPureStmt, new HashSet<>());
							}
							visited_indirectCall.get(callerStmt + csPureStmt).add(targetStmt);

							String targetFullClass = targetStmt.substring(0, targetStmt.lastIndexOf('.'));
							String target_file = findMatchedClassFile(targetFullClass);
							if (target_file == null)
								continue;

							// collect all xml configurations on target method
							ArrayList<String> target_xml_class = new ArrayList<>();
							ArrayList<String> target_xml_method = new ArrayList<>();
							ArrayList<Set<String>> target_xml_vals = new ArrayList<>();
							HashMap<String, HashMap<Element, HashSet<Attribute>>> target_xmlFile2node2XMLattrs_class = new HashMap<>();
							HashMap<String, HashMap<Element, HashSet<Attribute>>> target_xmlFile2node2XMLattrs_method = new HashMap<>();
							collectAllXMLConfiguration(targetStmt, targetFullClass, target_xmlFile2node2XMLattrs_class, target_xmlFile2node2XMLattrs_method,
									target_xml_class, target_xml_method, target_xml_vals);
							// for collecting annotations on target method
							CtClass targetCC = readCtClass(target_file, pool);
							if (targetCC == null)
								continue;
							ArrayList<Annotation> target_NotNullAnnos_class = new ArrayList<>();
							ArrayList<Annotation> target_NotNullAnnos_method = new ArrayList<>();
							ArrayList<Set<String>> target_anno_vals = new ArrayList<>();
							collectAllNotNULLAnnoConfiguration(targetStmt, targetCC, target_NotNullAnnos_class, target_NotNullAnnos_method, target_anno_vals);

							if (target_xmlFile2node2XMLattrs_class.isEmpty() && target_xmlFile2node2XMLattrs_method.isEmpty()
									&& target_NotNullAnnos_class.isEmpty() && target_NotNullAnnos_method.isEmpty())
								continue;

							// TODO:debug
							// 1. caller-anno && target-anno
							if ((!caller_NotNullAnnos_class.isEmpty() || !caller_NotNullAnnos_method.isEmpty())
									&& (!target_NotNullAnnos_class.isEmpty() || !target_NotNullAnnos_method.isEmpty())) {
								for (int i = 0; i < caller_anno_vals.size(); i++) {
									Set<String> vals1 = caller_anno_vals.get(i);
									for (int j = 0; j < target_anno_vals.size(); j++) {
										Set<String> vals2 = target_anno_vals.get(j);
										if (isMain || mayContainsMatch(vals1, vals2)) {
											// i and j are the compare configure value
											// add the annotation into the result
											ArrayList<String> matchedConfigs = new ArrayList<>();
											appendConfigsOnClassAndMethod_Annotation(caller_NotNullAnnos_class, caller_NotNullAnnos_method, i, matchedConfigs);
											appendConfigsOnClassAndMethod_Annotation(target_NotNullAnnos_class, target_NotNullAnnos_method, j, matchedConfigs);
											mutateTarget(csPureStmt, targetStmt, pool, visitedTags2Count_indirectCall, matchedConfigs);
										}
									}
								}
							}
							// 2. caller-anno && target-xml
							if ((!caller_NotNullAnnos_class.isEmpty() || !caller_NotNullAnnos_method.isEmpty())
									&& (!target_xmlFile2node2XMLattrs_class.isEmpty() || !target_xmlFile2node2XMLattrs_method.isEmpty())) {
								// caller
								for (int i = 0; i < caller_anno_vals.size(); i++) {
									Set<String> vals1 = caller_anno_vals.get(i);
									// target
									for (int j = 0; j < target_xml_vals.size(); j++) {
										Set<String> vals2 = target_xml_vals.get(j);
										if (isMain || mayContainsMatch(vals1, vals2)) {
											ArrayList<String> matchedConfigs = new ArrayList<>();
											appendConfigsOnClassAndMethod_Annotation(caller_NotNullAnnos_class, caller_NotNullAnnos_method, i, matchedConfigs);
											appendConfigsOnClassAndMethod_XML(target_xml_class, target_xml_method, j, matchedConfigs);
											mutateTarget(csPureStmt, targetStmt, pool, visitedTags2Count_indirectCall, matchedConfigs);
										}
									}
								}
							}
							// 3. caller-xml && target-anno
							if ((!caller_xmlFile2node2XMLattrs_class.isEmpty() || !caller_xmlFile2node2XMLattrs_method.isEmpty())
									&& (!target_NotNullAnnos_class.isEmpty() || !target_NotNullAnnos_method.isEmpty())) {
								for (int i = 0; i < caller_xml_vals.size(); i++) {
									Set<String> vals1 = caller_xml_vals.get(i);
									// target-anno
									for (int j = 0; j < target_anno_vals.size(); j++) {
										Set<String> vals2 = target_anno_vals.get(j);
										if (isMain || mayContainsMatch(vals1, vals2)) {
											ArrayList<String> matchedConfigs = new ArrayList<>();
											appendConfigsOnClassAndMethod_XML(caller_xml_class, caller_xml_method, i, matchedConfigs); // caller
											appendConfigsOnClassAndMethod_Annotation(target_NotNullAnnos_class, target_NotNullAnnos_method, j, matchedConfigs); // target
											mutateTarget(csPureStmt, targetStmt, pool, visitedTags2Count_indirectCall, matchedConfigs);
										}
									}
								}
							}
							// 4. caller-xml && target-xml
							if ((!caller_xmlFile2node2XMLattrs_class.isEmpty() || !caller_xmlFile2node2XMLattrs_method.isEmpty())
									&& ((!target_xmlFile2node2XMLattrs_class.isEmpty() || !target_xmlFile2node2XMLattrs_method.isEmpty()))) {
								for (int i = 0; i < caller_xml_vals.size(); i++) {
									Set<String> vals1 = caller_xml_vals.get(i);
									for (int j = 0; j < target_xml_vals.size(); j++) {
										Set<String> vals2 = target_xml_vals.get(j);
										if (isMain || mayContainsMatch(vals1, vals2)) {
											ArrayList<String> matchedConfigs = new ArrayList<>();
											appendConfigsOnClassAndMethod_XML(caller_xml_class, caller_xml_method, i, matchedConfigs); // caller
											appendConfigsOnClassAndMethod_XML(target_xml_class, target_xml_method, j, matchedConfigs); // target
											mutateTarget(csPureStmt, targetStmt, pool, visitedTags2Count_indirectCall, matchedConfigs);
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

	/**
	 * Append configurations into matchedConfigs which consistent of : </br>
	 * 0: class configuration; </br>
	 * 1: method configuration; </br>
	 * null iff there is not configuration on the class/method;</br>
	 * content calculated from notNullAnnos_class and notNullAnnos_method, while
	 * which value depends on index.
	 */
	private void appendConfigsOnClassAndMethod_Annotation(ArrayList<Annotation> notNullAnnos_class, ArrayList<Annotation> notNullAnnos_method, int index,
			ArrayList<String> matchedConfigs) {
		if (index < notNullAnnos_class.size()) {
			// on class
			matchedConfigs.add(notNullAnnos_class.get(index).getTypeName());
			matchedConfigs.add(null); // method
		} else {
			// on method
			matchedConfigs.add(null);// class
			matchedConfigs.add(notNullAnnos_method.get(index - notNullAnnos_class.size()).getTypeName());
		}
	}

	private void appendConfigsOnClassAndMethod_XML(ArrayList<String> notNullAnnos_class, ArrayList<String> notNullAnnos_method, int index,
			ArrayList<String> matchedConfigs) {
		if (index < notNullAnnos_class.size()) {
			// on class
			matchedConfigs.add(notNullAnnos_class.get(index));
			matchedConfigs.add(null); // method
		} else {
			// on method
			matchedConfigs.add(null);// class
			matchedConfigs.add(notNullAnnos_method.get(index - notNullAnnos_class.size()));
		}
	}

	/**
	 * @param matchedConfigs: the configuration that matched, matchedConfigs[0] on
	 *                        callsite and matchedConfigs[1] on target
	 */
	private void mutateTarget(String csPureStmt, String targetStmt, ClassPool pool, HashMap<String, Integer> visitedTags2Count_indirectCall,
			ArrayList<String> matchedConfigs) {
		/** core1: modify annotations! */
		String fullClass0 = targetStmt.substring(0, targetStmt.lastIndexOf('.'));
		String f = findMatchedClassFile(fullClass0);
		ArrayList<String> tmp = new ArrayList<>();
		tmp.add(csPureStmt);
		tmp.addAll(matchedConfigs);
		if (f != null) {
			dealWithMethod_Annotation(f, targetStmt, pool, ModifiedSemantic.IndirectCall, false, tmp, visitedTags2Count_indirectCall, false);
		}

		/** core2: modify xmls! */
		if (class2XMLFile.keySet().contains(fullClass0)) {
			for (String xmlFile : class2XMLFile.get(fullClass0)) {
				dealWithMethod_XML(f, targetStmt, pool, xmlFile, ModifiedSemantic.IndirectCall, tmp, visitedTags2Count_indirectCall, false);
			}
		}

	}

	// TODO: debug
	private void collectAllNotNULLAnnoConfiguration(String stmt, CtClass cc, ArrayList<Annotation> notNullAnnos_class,
			ArrayList<Annotation> notNullAnnos_method, ArrayList<Set<String>> anno_vals) {
		// class
		AttributeInfo classAttrInfo = cc.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
		if (classAttrInfo != null) {
			if (classAttrInfo instanceof AnnotationsAttribute) {
				AnnotationsAttribute classAttr = (AnnotationsAttribute) classAttrInfo;
				if (classAttr.getAnnotations().length > 0) {
					for (Annotation anno : classAttr.getAnnotations()) {
						String annoname = anno.getTypeName();
						if (!ConfigureUtil.isApplicationClass(annoname.replace('.', '/'))) {
							Set<String> mems = anno.getMemberNames();
							if (mems != null && !mems.isEmpty()) {
								notNullAnnos_class.add(anno);
//								anno_vals.add(mems);
								for (String mem : mems) {
									MemberValue memval = anno.getMemberValue(mem);
									HashSet<String> tmp = new HashSet<>();
									tmp.add(memval.toString());
									anno_vals.add(tmp);
								}
							}
						}
					}
				}
			}
		}
		// method
		CtMethod method = SearchInCtClassHelper.findCtMethod(cc, stmt);
		if (method != null) {
			MethodInfo info = method.getMethodInfo();
			AttributeInfo mtdAttrInfo = info.getAttribute(AnnotationsAttribute.visibleTag);
			if (mtdAttrInfo instanceof AnnotationsAttribute) {
				AnnotationsAttribute mtdAttr = (AnnotationsAttribute) mtdAttrInfo;
				if (mtdAttr.getAnnotations().length > 0) {
					for (Annotation anno : mtdAttr.getAnnotations()) {
						String annoname = anno.getTypeName();
						if (!ConfigureUtil.isApplicationClass(annoname.replace('.', '/'))) {
							Set<String> mems = anno.getMemberNames();
							if (mems != null && !mems.isEmpty()) {
								notNullAnnos_method.add(anno);
								for (String mem : mems) {
									MemberValue memval = anno.getMemberValue(mem);
									HashSet<String> tmp = new HashSet<>();
									tmp.add(memval.toString());
									anno_vals.add(tmp);
								}
//								anno_vals.add(mems);
							}
						}
					}
				}
			}
		}
	}

	// TODO: debug
	private void collectAllXMLConfiguration(String stmt, String fullClass0, HashMap<String, HashMap<Element, HashSet<Attribute>>> xmlFile2node2XMLattrs_class,
			HashMap<String, HashMap<Element, HashSet<Attribute>>> xmlFile2node2XMLattrs_method, ArrayList<String> xml_class, ArrayList<String> xml_method,
			ArrayList<Set<String>> xml_vals) {
		if (class2XMLFile.keySet().contains(fullClass0)) {
			HashSet<String> xmls = class2XMLFile.get(fullClass0);
			for (String xmlFile : xmls) {
				SAXReader reader = new SAXReader();
				reader.setIgnoreComments(true);
				reader.setValidation(false);
				reader.setEntityResolver(new EntityResolver() {
					@Override
					public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
						return new InputSource(new ByteArrayInputStream("".getBytes()));
					}
				});
				try {
					Document document = reader.read(xmlFile);
					/* exclude database configure */
					DocumentType docType = document.getDocType();
					if (docType != null)
						if (XMLConfigurationUtil.notConcernedXMLID(docType.getPublicID()) || XMLConfigurationUtil.notConcernedXMLID(docType.getSystemID()))
							return;
					Element root = document.getRootElement();
					HashMap<Element, HashSet<Attribute>> node2attrs_class = new HashMap<>();
					HashMap<Element, HashSet<Attribute>> node2attrs_method = new HashMap<>();
					ConfigurationCollector.collectMethodXMLConfiguration(root, stmt, xmlFile, false, node2attrs_class, node2attrs_method);

					// class
					if (!node2attrs_class.isEmpty()) {
						xmlFile2node2XMLattrs_class.put(xmlFile, node2attrs_class);

						for (Element nodeele : node2attrs_class.keySet()) {
							for (Attribute attr : node2attrs_class.get(nodeele)) {
								if (attr == null) {
									// is text
									HashSet<Element> valueElements = getXMLTextConfigureValue(nodeele);
									for (Element xmlele : valueElements) {
										xml_class.add(xmlele.getPath());

										HashSet<String> vals2 = new HashSet<>();
										vals2.add(xmlele.getText());
										xml_vals.add(vals2);
									}
								} else {
									// is attribute
									xml_class.add(attr.getPath());

									HashSet<String> vals2 = new HashSet<>();
									vals2.add(attr.getValue());
									xml_vals.add(vals2);
								}
							}
							// may configured on parent attribute
							if (node2attrs_class.get(nodeele).size() > 0) {
								HashSet<Attribute> valueAttrs = getXMLParentAttrs(nodeele);
								for (Attribute attr : valueAttrs) {
									xml_class.add(attr.getPath());

									HashSet<String> vals2 = new HashSet<>();
									vals2.add(attr.getValue());
									xml_vals.add(vals2);
								}
							}
						}
					}
					// method
					if (!node2attrs_method.isEmpty()) {
						xmlFile2node2XMLattrs_method.put(xmlFile, node2attrs_method);

						for (Element nodeele : node2attrs_method.keySet()) {
							for (Attribute attr : node2attrs_method.get(nodeele)) {
								if (attr == null) {
									// is text
									HashSet<Element> valueElements = getXMLTextConfigureValue(nodeele);
									for (Element xmlele : valueElements) {
										xml_method.add(xmlele.getPath());

										HashSet<String> vals2 = new HashSet<>();
										vals2.add(xmlele.getText());
										xml_vals.add(vals2);
									}
								} else {
									// is attribute
									xml_method.add(attr.getPath());

									HashSet<String> vals2 = new HashSet<>();
									vals2.add(attr.getValue());
									xml_vals.add(vals2);
								}
							}
						}
					}
				} catch (DocumentException e) {
					System.err.println("[error][DocumentException]" + e.getMessage() + " when parse " + xmlFile);
				}
			}
		}

	}

	/**
	 * Creates a new class (or interface) from the given class file.If there already
	 * exists a class with the same name, the new classoverwrites that previous
	 * class.</br>
	 * NULL if occurs exception.
	 */
	private CtClass readCtClass(String classFilePath, ClassPool pool) {
		CtClass cc = null;
		if (classFilePath != null) {
			File clFile = new File(classFilePath);
			try (DataInputStream dis = new DataInputStream(new FileInputStream(clFile))) {
				ClassFile classFile = new ClassFile(dis);
				cc = pool.makeClass(classFile);
				String name = classFile.getName();

				checkClassDirAbsolutePathAdded(clFile.getAbsolutePath(), name, pool);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return cc;
	}

	/**
	 * the configuration value of ele is the element share the same parent with ele
	 * and the text is not null;</br>
	 * contain once indirect index !!!
	 */
	private HashSet<Element> getXMLTextConfigureValue(Element ele) {
		HashSet<Element> candidateTextConfigValueEles = new HashSet<>();
		HashMap<Element, HashSet<Element>> config2Closure = new HashMap<>();
		// find the configure value of this ele
		Element parent = ele.getParent();
		if (parent != null && !parent.equals(ele))
			for (Object child0 : parent.elements()) {
				if (child0 instanceof Element) {
					Element child = (Element) child0;
					if (child.equals(ele))
						continue;
					String tt = child.getTextTrim();
					if (tt.length() > 0)
						config2Closure.put(child, new HashSet<Element>());
				}
			}
		if (!config2Closure.isEmpty()) {
			// find the nodes which text equals to the configure value of ele
			Element pparent = parent.getParent();
			if (pparent != null && !pparent.equals(ele))
				for (Object p0 : pparent.elements()) {
					Element p = (Element) p0;
					if (p.equals(parent))
						continue;

					for (Object child0 : p.elements()) {
						if (child0 instanceof Element) {
							Element child = (Element) child0;
							String tt = child.getText().trim();
							if (tt.length() > 0) {
								// find the element share the same value with configure value element
								for (Element configValueEle : config2Closure.keySet()) {
									if (tt.equals(configValueEle.getText().trim()))
										config2Closure.get(configValueEle).add(child);
								}
							}
						}
					}
				}
			for (Element configValueEle : config2Closure.keySet()) {
				candidateTextConfigValueEles.add(configValueEle);
				for (Element same : config2Closure.get(configValueEle)) {
					for (Object v : same.getParent().elements()) {
						Element velement = (Element) v;
						if (velement.equals(same))
							continue;
						if (velement.getTextTrim().length() > 0)
							candidateTextConfigValueEles.add(velement);
					}
				}
			}
		}
		return candidateTextConfigValueEles;
	}

	private HashSet<Attribute> getXMLParentAttrs(Element nodeele) {
		HashSet<Attribute> ret = new HashSet<>();
		Element p = nodeele.getParent();
		if (p != null) {
			for (Object attr0 : p.attributes()) {
				if (attr0 instanceof Attribute) {
					Attribute attr = (Attribute) attr0;
					String val = attr.getValue();
					if (val != null && val.length() > 0)
						ret.add(attr);
				}
			}
		}
		return ret;
	}

	/**
	 * true iff there is ele1 in vals1 mat match ele2 in vals2
	 */
	private boolean mayContainsMatch(Set<String> vals1, Set<String> vals2) {
		for (String val1 : vals1) {
			for (String val2 : vals2) {
				if (mayMatch(val1, val2))
					return true;
			}
		}
		return false;
	}

	// TODO: to validate
	private boolean mayMatch(String val1, String val2) {
		if (val1.length() == 0 || val2.length() == 0)
			return false;

		// A. whole string
		// 1. equals directly
		if (val1.equals(val2))
			return true;
		// 2. attempt regular match: may fail since may not regular expression
		if (mayRegularPattern(val1, val2))
			return true;
		if (mayRegularPattern(val2, val1))
			return true;

		// 3. heuristic
		// 3.1 if one of them is the root, that is the "/"
		if (val1.equals("/") || val2.equals("/"))
			return true;
		// 3.2 if ends with *, match the string before *
		if (checkStar(val1, val2))
			return true;
		if (checkStar(val2, val1))
			return true;

		// B. each phase
		if ((val1.contains("/") && val2.contains("/")) || ((val1.contains("/") || val2.contains("/")) && !val1.equals("/") && !val2.equals("/"))) {
			String[] vv1s = val1.split("/");
			String[] vv2s = val2.split("/");
			int len = Math.min(vv1s.length, vv2s.length);
			if (len > 0) {
				boolean flag1 = true; // 1 head->tail
				boolean flag2 = true; // 2 tail->head
				for (int i = 0; i < len; i++) {
					if (flag1 && vv1s[i].length() != 0 && vv2s[i].length() != 0)
						flag1 = mayMatch(vv1s[i], vv2s[i]);
					if (flag2 && vv1s[vv1s.length - 1 - i].length() != 0 && vv2s[vv2s.length - 1 - i].length() != 0)
						flag2 = mayMatch(vv1s[vv1s.length - 1 - i], vv2s[vv2s.length - 1 - i]);

					if (!flag1 && !flag2)
						break;
				}
				if (flag1 || flag2)
					return true;
			}
		}

		return false;
	}

	private boolean mayRegularPattern(String val1, String val2) {
		try {
			Pattern p = Pattern.compile(val1);
			Matcher m = p.matcher(val2);
			return m.matches();
		} catch (PatternSyntaxException e) {
			// do nothing...
		}
		return false;
	}

	private boolean checkStar(String val1, String val2) {
		if (val1.endsWith("*")) {
			String v1 = val1.replaceAll("\\*", "");
			if (val2.startsWith(v1))
				return true;
		}
		return false;
	}

	/**
	 * @param tail
	 * @param requestInfoLog: the runtime requestInfoLog with specific url
	 * @param originalValue:  the configured url pattern
	 * 
	 * @return 0:the newRequestInfoLog; 1: newValue
	 */
	private String[] buildNewTriggerURL(String requestInfoLog, String originalValue, String tail) {
		String[] ret = new String[2];
		String url = getURL(requestInfoLog);
		if (originalValue.equals("/") || originalValue.equals("\\")) {
			// match everything
			// do nothing
			return ret;
		} else {
			if (url.equals(originalValue) || url.endsWith(originalValue)) {
				// 1. equals
				String newVal = originalValue + tail;
				String prefix = url.substring(0, url.lastIndexOf(originalValue));
				String newURL = prefix + newVal;
				ret[0] = reBuildReqInfoLog(requestInfoLog, newURL);
				ret[1] = newVal;
				return ret;
			} else if (url.contains(originalValue)) {
				// 2. contains
				String newVal = originalValue + tail;
				String prefix = url.substring(0, url.indexOf(originalValue));
				String suffix = url.substring(url.indexOf(originalValue) + originalValue.length());
				String newURL = prefix + newVal + suffix;
				ret[0] = reBuildReqInfoLog(requestInfoLog, newURL);
				ret[1] = newVal;
				return ret;
			} else {
				// 3. may contain: * at tails, and url contains
				String[] vvs = originalValue.split("\\*");
				int j = 0;
				for (; j < vvs.length; j++) {
					String middleValue = vvs[j];
					if (middleValue.length() > 0) {
						String[] eles = middleValue.split("/");
						int i = eles.length - 1;
						for (; i > 0; i--) {
							if (eles[i].length() > 0) {
								if (url.contains(eles[i])) {
									// match from the tail to head
									String newVal = eles[i] + tail;
									String prefix = url.substring(0, url.lastIndexOf(eles[i]));
									String suffix = url.substring(url.lastIndexOf(eles[i]) + eles[i].length());
									String newURL = prefix + newVal + suffix;
									ret[0] = reBuildReqInfoLog(requestInfoLog, newURL);
									ret[1] = newVal;
									break;
								}
							}
						}
						String tmp = "";
						if (ret[1] != null) {
							for (int k = 0; k < eles.length; k++) {
								if (k != i) {
									tmp = tmp + eles[k] + "/";
								} else {
									tmp = tmp + ret[1] + "/";
								}
							}
							ret[1] = tmp;
							break;
						}
					}
				}
				String tmp = "";
				if (ret[1] != null) {
					for (int k = 0; k < vvs.length; k++) {
						if (k != j) {
							tmp = tmp + vvs[k] + "*";
						} else {
							tmp = tmp + ret[1] + "*";
						}
					}
					ret[1] = tmp;
				}
			}
		}

		return ret;
	}

	private String reBuildReqInfoLog(String requestInfoLog, String newURL) {
		String suffix = requestInfoLog.substring(requestInfoLog.indexOf("[hashcode]"));
		String prefix1 = requestInfoLog.substring(0, requestInfoLog.indexOf(']') + 1);
		String str1 = requestInfoLog.substring(requestInfoLog.indexOf(']') + 1);
		String prefix2 = str1.substring(0, str1.indexOf(']') + 1);

		return prefix1 + prefix2 + newURL + suffix;
	}

	/**
	 * write the request sequence to trigger this testcase</br>
	 * add newReqInfoLog at the end
	 */
	private void writeRequestSequence(String newReqInfoLog) {
//		String path = outPath + File.separator + modifiedRecord.size() + File.separator + "requestsTrigger.txt";
		String path = outPath + File.separator + INDEX + File.separator + "requestsTrigger.txt";

		FileOutputStream out = null;
		OutputStreamWriter outWriter = null;
		BufferedWriter buffer = null;
		try {
			out = new FileOutputStream(new File(path));
			outWriter = new OutputStreamWriter(out, "UTF-8");
			buffer = new BufferedWriter(outWriter);

			for (int i = 0; i < request.size(); i++) {
				buffer.append(request.get(i) + "\n");
				buffer.append(response.get(i) + "\n");
			}
			// add new construct url
			buffer.append(newReqInfoLog + "\n");
			buffer.append(response.get(response.size() - 1) + "\n"); // place holder
		} catch (IOException e) {
			e.printStackTrace();
		} finally {

			if (buffer != null)
				try {
					buffer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (outWriter != null)
				try {
					outWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (out != null)
				try {
					out.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		}
	}

	private String getURL(String requestInfoLog) {
		String str1 = requestInfoLog.substring("[ReqStart]".length(), requestInfoLog.indexOf("[hashcode]"));
		return str1.substring(str1.indexOf(']') + 1);
	}

}
