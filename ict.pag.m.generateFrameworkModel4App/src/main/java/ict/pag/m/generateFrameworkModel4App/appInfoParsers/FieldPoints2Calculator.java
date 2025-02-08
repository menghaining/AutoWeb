package ict.pag.m.generateFrameworkModel4App.appInfoParsers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.shrikeCT.AnnotationsReader.ElementValue;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.annotations.Annotation;

import ict.pag.m.frameworkInfoUtil.customize.Util;
import ict.pag.m.frameworkInfoUtil.frameworkInfoInAppExtract.extractAnnos;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.AnnotationEntity;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.FieldPoints2Entity;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.xmlClassNodeEntity;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.xmlClassNodeEntity.SubInfo;
import ict.pag.m.generateFrameworkModel4App.entity.FrameworkModel;

public class FieldPoints2Calculator {
	private ClassHierarchy cha;

	private FrameworkModel frameworkModel;

	private AnnotationExtractor annoExtractor;
	private XMLExtractor xmlExtractor;
	private InheritanceExtractor inheExtractor;

	/** alias and its class */
	private HashMap<String, IClass> alias2Class = new HashMap<>();

	/** class to its field that need to inject */
	private HashMap<String, HashSet<String>> class2fieldsName = new HashMap<>();

	/** Answer */
	HashSet<FieldPoints2Entity> fieldPoints2targetSet = new HashSet<>();
	int count = 0;

	public FieldPoints2Calculator(ClassHierarchy cha, FrameworkModel frameworkModel, AnnotationExtractor anno,
			XMLExtractor xml, InheritanceExtractor inheExtractor2) {
		this.cha = cha;
		this.frameworkModel = frameworkModel;
		this.annoExtractor = anno;
		this.xmlExtractor = xml;
		this.inheExtractor = inheExtractor2;
	}

	public int getInjectObjectsNumber() {
		int count = 0;
//		for (String key : class2fieldsName.keySet()) {
//			count = count + class2fieldsName.get(key).size();
//		}
		count = fieldPoints2targetSet.size();
		return count;
	}

	public void matches() {

		/* 1. calculate all classes and its alias */
		HashSet<String> classAliasMarks = frameworkModel.getClassAlias();
		HashMap<String, HashSet<AnnotationEntity>> marks2classSet_anno = annoExtractor.getMark2classSet();
		HashMap<String, HashSet<xmlClassNodeEntity>> marks2classSet_xml = xmlExtractor.getMark2classSet();

		for (String frmk_mark : classAliasMarks) {
			// annotation-level
			if (frmk_mark.startsWith("anno:")) {
				String anno_frmk = frmk_mark.substring("anno:".length());
				for (String classanno : marks2classSet_anno.keySet()) {
					if (classanno.equals(anno_frmk)) {
						HashSet<AnnotationEntity> candidates = marks2classSet_anno.get(classanno);
						for (AnnotationEntity c1 : candidates) {
							Annotation anno = c1.getAnnotation();
							Map<String, ElementValue> name2val = anno.getNamedArguments();
							if (name2val.containsKey("value")) {
								ElementValue vale = name2val.get("value");
								String val = extractAnnos.reFormatAnnosValue(vale);
								if (val != null) {
									if (c1.getType().equals("class")) {
										alias2Class.put(val, c1.getClazz());
									}
								}
							} else if (name2val.containsKey("name")) {
								ElementValue vale = name2val.get("name");
								String val = extractAnnos.reFormatAnnosValue(vale);
								if (val != null) {
									if (c1.getType().equals("class")) {
										alias2Class.put(val, c1.getClazz());
									}

								}
							}
						}
					}
				}
			}
			// xml-level
			Set<String> appMarks = marks2classSet_xml.keySet();
			if (frmk_mark.startsWith("xml:")) {
				String xml_frmk = frmk_mark.substring("xml:".length());

				String frmk_path = "";
				String frmk_attr = "";
				String[] alls = xml_frmk.split(";");
				for (int i = 0; i < alls.length; i++) {
					String curr = alls[i];
					if (i == alls.length - 1) {
						frmk_attr = curr;
					} else {
						frmk_path = frmk_path + ";" + curr.substring(curr.indexOf(':') + 1);
					}
				}
				frmk_path = frmk_path.substring(1);

				if (frmk_attr.contains("<") && frmk_attr.contains(",")) {
					String idAttr = frmk_attr.substring(1, frmk_attr.lastIndexOf(','));
					String classAttr = frmk_attr.substring(frmk_attr.lastIndexOf(',') + 1, frmk_attr.length() - 1);

					frmk_path = frmk_path + ";" + classAttr;
					for (String appm : appMarks) {
						if (appm.equals(frmk_path)) {
							HashSet<xmlClassNodeEntity> entities = marks2classSet_xml.get(appm);
							for (xmlClassNodeEntity entity : entities) {
								if (entity.getName2value().containsKey(idAttr)) {
									String alias = entity.getName2value().get(idAttr);
									String className = entity.getClazz();
									IClass clazz = convertString2Class(className);
									if (clazz != null) {
										alias2Class.put(alias, clazz);
									}
								}
							}

						}
					}
				}
			}
		}

		/* 2. matches annotation */
		matches_annotation(annoExtractor.getMark2fieldSet(), annoExtractor.getMark2classSet());
//		System.out.println(fieldPoints2targetSet.size());
//		System.out.println(getInjectObjectsNumber());
		/* 3. matches xml */
		matches_xml(xmlExtractor.getMark2classSet());
//		System.out.println(fieldPoints2targetSet.size());
//		System.out.println(getInjectObjectsNumber());
	}

	private void matches_xml(HashMap<String, HashSet<xmlClassNodeEntity>> mark2classSet) {
		/* application information */
		Set<String> allmarks = mark2classSet.keySet();
		/* framework model information */
		Set<HashSet<String>> fieldsMarks = frameworkModel.getFieldsInject();
		HashSet<String> points2Alias = frameworkModel.getObjActualPoints2Alias();

		/* 1. calculate the field that need to inject */
		HashSet<IField> fieldNeed2Inject = new HashSet<>();
		for (HashSet<String> inject_annos : fieldsMarks) {
			for (String xml_frmk : inject_annos) {
				if (xml_frmk.startsWith("xml:")) {
					xml_frmk = xml_frmk.substring("xml:".length());

					String frmk_path = "";
					String frmk_attr = "";
					String[] alls = xml_frmk.split(";");
					for (int i = 0; i < alls.length; i++) {
						String curr = alls[i];
						if (i == alls.length - 1) {
							frmk_attr = curr.substring(curr.indexOf(':') + 1);
						} else {
							frmk_path = frmk_path + ";" + curr.substring(curr.indexOf(':') + 1);
						}
					}
					frmk_path = frmk_path.substring(1);

					for (String xml_app : allmarks) {
						String xml_app_path = xml_app.substring(0, xml_app.lastIndexOf(';'));
						String xml_app_attr = xml_app.substring(xml_app.lastIndexOf(';') + 1);
						HashSet<xmlClassNodeEntity> entities = mark2classSet.get(xml_app);

						for (xmlClassNodeEntity entity : entities) {
							String className = entity.getClazz();
							if (className.contains("HL7ServiceImpl"))
								System.out.println();
							if (xml_app_path.equals(frmk_path)) {
								// in the same layer
								if (entity.getName2value().containsKey(frmk_attr)) {
									String fieldName = entity.getName2value().get(frmk_attr);
									IField f = convertString2Field(fieldName, className);
									if (f != null) {
										fieldNeed2Inject.add(f);
									} else {
										f = mayTheField(fieldName, className);
										if (f != null) {
											fieldNeed2Inject.add(f);
										} else {
//											System.err.println("[!!] " + className + "====" + fieldName);
										}
									}

									if (class2fieldsName.containsKey(className)) {
										class2fieldsName.get(className).add(fieldName);
									} else {
										HashSet<String> tmp = new HashSet<>();
										tmp.add(fieldName);
										class2fieldsName.put(className, tmp);
									}
								}
							} else if (frmk_path.startsWith(xml_app_path)) {
								// in the lower layer
								for (SubInfo sub : entity.getSubInfos()) {
									String xml_app_path2 = xml_app_path + ";" + sub.getTagName();
									if (xml_app_path2.equals(frmk_path)) {
										if (sub.getName2value_sub().containsKey(frmk_attr)) {
											String fieldName = sub.getName2value_sub().get(frmk_attr);
											IField f = convertString2Field(fieldName, className);
											if (f != null) {
												fieldNeed2Inject.add(f);
											} else {
												f = mayTheField(fieldName, className);
												if (f != null) {
													fieldNeed2Inject.add(f);
												} else {
//													System.err.println("[!!] " + className + "====" + fieldName);
												}
											}
											if (class2fieldsName.containsKey(className)) {
												class2fieldsName.get(className).add(fieldName);
											} else {
												HashSet<String> tmp = new HashSet<>();
												tmp.add(fieldName);
												class2fieldsName.put(className, tmp);
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

		/* calculate actual points to */
		HashMap<IField, IClass> field2Target = new HashMap<>();
		for (String xml_frmk : points2Alias) {
			if (xml_frmk.startsWith("xml:")) {
				xml_frmk = xml_frmk.substring("xml:".length());

				String[] tmp = xml_frmk.split("<");
				String prefix = tmp[0];
				String suffix = tmp[1];
				suffix = suffix.substring(0, suffix.length() - 1);

				String frmk_path = "";

				String[] alls = prefix.split(";");
				for (int i = 0; i < alls.length; i++) {
					String curr = alls[i];
					frmk_path = frmk_path + ";" + curr.substring(curr.indexOf(':') + 1);
				}
				frmk_path = frmk_path.substring(1);

//				if (frmk_attr.contains("<") && frmk_attr.contains(",")) {
//					String idAttr = frmk_attr.substring(1, frmk_attr.lastIndexOf(','));
//					String classAttr = frmk_attr.substring(frmk_attr.lastIndexOf(',') + 1, frmk_attr.length() - 1);
//
//				}
				String[] tmp2 = suffix.split(",");
				String fieldTag = tmp2[0];
				fieldTag = fieldTag.substring(fieldTag.indexOf(':') + 1);
				String suffix2 = tmp2[1];
				String[] alls2 = suffix2.split(";");
				String refTag = "";
				for (int i = 0; i < alls2.length; i++) {
					String curr = alls2[i];
					refTag = refTag + ";" + curr.substring(curr.indexOf(':') + 1);
				}
				refTag = refTag.substring(1);
				for (String xml_app : allmarks) {
					for (xmlClassNodeEntity en : mark2classSet.get(xml_app)) {
						for (SubInfo sub : en.getSubInfos()) {
							if (frmk_path.endsWith(sub.getTagName())) {
								HashMap<String, String> name2val = sub.getName2value_sub();
								if (name2val.containsKey(fieldTag)) {
									String fieldName = name2val.get(fieldTag);
									String refName = null;
									for (String key : name2val.keySet()) {
										if (refTag.contains(key)) {
											refName = name2val.get(key);
											break;
										}
									}
									if (refName != null) {
										IField f = convertString2Field(fieldName, en.getClazz());
										if (f != null) {
											if (alias2Class.containsKey(refTag)) {
												field2Target.put(f, alias2Class.get(refTag));
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
		/* merge information */
		for (IField toInject : fieldNeed2Inject) {
			if (field2Target.containsKey(toInject)) {
				FieldPoints2Entity tmp = new FieldPoints2Entity(toInject, false, field2Target.get(toInject));
				fieldPoints2targetSet.add(tmp);
			} else {
				FieldPoints2Entity tmp = new FieldPoints2Entity(toInject, true);
				fieldPoints2targetSet.add(tmp);
			}
		}

	}

	private IField mayTheField(String fieldName, String className) {
		String class2typeString = ("L" + className).replace(".", "/");
		TypeName T = TypeName.string2TypeName(class2typeString);
		IClass clazz = cha.getLoader(ClassLoaderReference.Application).lookupClass(T);
		if (clazz != null) {
			for (IField f : clazz.getAllFields()) {
				if (f.getFieldTypeReference().toString().toLowerCase().contains(fieldName.toLowerCase()))
					return f;
			}
		}
		return null;
	}

	private IField convertString2Field(String fieldName, String className) {
		String class2typeString = ("L" + className).replace(".", "/");
		TypeName T = TypeName.string2TypeName(class2typeString);
		IClass clazz = cha.getLoader(ClassLoaderReference.Application).lookupClass(T);
		if (clazz != null) {
			for (IField f : clazz.getAllFields()) {
				if (f.getName().toString().contains(fieldName))
					return f;
			}
		}
		return null;
	}

	private IClass convertString2Class(String className) {
		String class2typeString = ("L" + className).replace(".", "/");
		TypeName T = TypeName.string2TypeName(class2typeString);
		return cha.getLoader(ClassLoaderReference.Application).lookupClass(T);
	}

	private void matches_annotation(HashMap<String, HashSet<AnnotationEntity>> mark2fieldSet,
			HashMap<String, HashSet<AnnotationEntity>> marks2classSet) {
		Set<String> allmarks = mark2fieldSet.keySet();

		/* framework model information */
		Set<HashSet<String>> fieldsMarks = frameworkModel.getFieldsInject();
		HashSet<String> points2Alias = frameworkModel.getObjActualPoints2Alias();

//		Set<HashSet<String>> managedClasses = frameworkModel.getManagedClasses().getAllElements();

		HashSet<IField> fieldNeed2Inject = new HashSet<>();
		// the field need to generate object and inject
		for (HashSet<String> inject_annos : fieldsMarks) {
			for (String anno_frmk : inject_annos) {
				if (anno_frmk.startsWith("anno:field:")) {
					anno_frmk = anno_frmk.substring("anno:field:".length());
					for (String anno_app : allmarks) {
						if (anno_frmk.equals(anno_app)) {
							HashSet<AnnotationEntity> need2InjectEntities = mark2fieldSet.get(anno_app);
							for (AnnotationEntity en : need2InjectEntities) {
								if (en.getType().equals("field")) {
									IField f = en.getField();
									fieldNeed2Inject.add(f);

									String f_class = Util
											.format(f.getReference().getDeclaringClass().getName().toString());
									String f_name = f.getReference().getName().toString();
									if (class2fieldsName.containsKey(f_class)) {
										class2fieldsName.get(f_class).add(f_name);
									} else {
										HashSet<String> tmp = new HashSet<>();
										tmp.add(f_name);
										class2fieldsName.put(f_class, tmp);
									}
								}
							}
						}
					}
				}
			}
		}

//		HashMap<String, IClass> alias2Class = new HashMap<>();
		// find alias actual points to
//		for (String anno_frmk : classAliasMarks) {
//			if (anno_frmk.startsWith("anno:")) {
//				anno_frmk = anno_frmk.substring("anno:".length());
//				for (String classanno : marks2classSet.keySet()) {
//					if (classanno.equals(anno_frmk)) {
//						HashSet<AnnotationEntity> candidates = marks2classSet.get(classanno);
//						for (AnnotationEntity c1 : candidates) {
//							Annotation anno = c1.getAnnotation();
//							Map<String, ElementValue> name2val = anno.getNamedArguments();
//							if (name2val.containsKey("value")) {
//								ElementValue vale = name2val.get("value");
//								String val = extractAnnos.reFormatAnnosValue(vale);
//								if (val != null) {
//									if (c1.getType().equals("class")) {
//										alias2Class.put(val, c1.getClazz());
//									}
//								}
//							} else if (name2val.containsKey("name")) {
//								ElementValue vale = name2val.get("name");
//								String val = extractAnnos.reFormatAnnosValue(vale);
//								if (val != null) {
//									if (c1.getType().equals("class")) {
//										alias2Class.put(val, c1.getClazz());
//									}
//
//								}
//							}
//						}
//					}
//				}
//			}
//		}

		HashMap<IField, IClass> field2Target = new HashMap<>();
		// actual point
		for (String anno_frmk : points2Alias) {
			if (anno_frmk.startsWith("anno:")) {
				anno_frmk = anno_frmk.substring("anno:".length());
				for (String anno_app : allmarks) {
					if (anno_frmk.equals(anno_app)) {
						HashSet<AnnotationEntity> fieldPoints2Alias = mark2fieldSet.get(anno_app);
						for (AnnotationEntity en : fieldPoints2Alias) {

							Annotation anno = en.getAnnotation();
							Map<String, ElementValue> name2val = anno.getNamedArguments();
							if (name2val.containsKey("value")) {
								ElementValue vale = name2val.get("value");
								String val = extractAnnos.reFormatAnnosValue(vale);
								if (val != null) {
									if (en.getType().equals("field")) {
										for (String key : alias2Class.keySet()) {
											if (key.equals(val)) {
												field2Target.put(en.getField(), alias2Class.get(key));
												break;
											}
										}
									}
								}
							} else if (name2val.containsKey("name")) {
								ElementValue vale = name2val.get("name");
								String val = extractAnnos.reFormatAnnosValue(vale);
								if (val != null) {
									if (en.getType().equals("field")) {
										for (String key : alias2Class.keySet()) {
											if (key.equals(val)) {
												field2Target.put(en.getField(), alias2Class.get(key));
												break;
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

		// merge information
		for (IField toInject : fieldNeed2Inject) {
			if (field2Target.containsKey(toInject)) {
				FieldPoints2Entity tmp = new FieldPoints2Entity(toInject, false, field2Target.get(toInject));
				fieldPoints2targetSet.add(tmp);
			} else {
				FieldPoints2Entity tmp = new FieldPoints2Entity(toInject, true);
				fieldPoints2targetSet.add(tmp);
			}
		}
	}

	public HashSet<FieldPoints2Entity> getFieldPoints2targetSet() {
		return fieldPoints2targetSet;
	}

}
