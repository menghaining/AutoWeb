package ict.pag.m.frameworkInfoUtil.frameworkInfoInAppExtract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.shrikeCT.AnnotationsReader.AnnotationAttribute;
import com.ibm.wala.shrikeCT.AnnotationsReader.ArrayElementValue;
import com.ibm.wala.shrikeCT.AnnotationsReader.ConstantElementValue;
import com.ibm.wala.shrikeCT.AnnotationsReader.ElementValue;
import com.ibm.wala.shrikeCT.AnnotationsReader.EnumElementValue;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.annotations.Annotation;

import ict.pag.m.frameworkInfoUtil.customize.ConfigUtil;
import ict.pag.m.frameworkInfoUtil.customize.GraphBuilder;
import ict.pag.m.frameworkInfoUtil.customize.Util;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoPair;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoUnit;
import ict.pag.m.frameworkInfoUtil.infoEntity.marksLevel;
import ict.pag.m.frameworkInfoUtil.infoEntity.marksType;

public class extractAnnos {
	private Set<infoUnit> infoUnits_clazz = new HashSet<infoUnit>();
	private Set<infoUnit> infoUnits_field = new HashSet<infoUnit>();
	private Set<infoUnit> infoUnits_mthd = new HashSet<infoUnit>();

	private Set<String> bases = new HashSet<String>();
	private Set<String> bases_clazz = new HashSet<String>();
	private Set<String> bases_field = new HashSet<String>();
	private Set<String> bases_mthd = new HashSet<String>();

	private Set<String> AOPaspectJ_mark = new HashSet<String>();

	/** all framework marks meets */
	private HashSet<String> allMarks = new HashSet<>();

	public HashSet<String> getAllMarks() {
		return allMarks;
	}

	public Set<infoUnit> extract(ClassHierarchy cha) {
		Set<infoUnit> infoUnits = new HashSet<infoUnit>();

		cha.forEach(nodeClazz -> {
			if (nodeClazz.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
				/**
				 * 1. class level </br>
				 * Lco/yiiu/pybbs/controller/front/TopicController
				 */
				Collection<Annotation> clazzAnnos = nodeClazz.getAnnotations();
				if (clazzAnnos != null && !clazzAnnos.isEmpty()) {
					List<infoPair> clazzUnitFs = new ArrayList<>();
					for (Annotation anno : clazzAnnos) {
						Map<String, String> anno_key_value = new HashMap<String, String>();
						for (String key : anno.getNamedArguments().keySet()) {
							ElementValue v = anno.getNamedArguments().get(key);
							anno_key_value.put(key, reFormatAnnosValue(v));
						}

						if (ConfigUtil.isFrameworkMarks(anno.getType().getName().toString())) {
//							if (Util.isFrameworkAnnos(anno.getType().getName().toString())) {
							infoPair pair = new infoPair(Util.format(anno.getType().getName().toString()),
									anno_key_value);

							clazzUnitFs.add(pair);

							allMarks.add(Util.format(anno.getType().getName().toString()));

						}
					}
					if (!clazzUnitFs.isEmpty()) {
						infoUnit unit = new infoUnit(marksType.anno, marksLevel.Clazz,
								Util.format(nodeClazz.getName().toString()), clazzUnitFs);
						infoUnits.add(unit);
						infoUnits_clazz.add(unit);
						bases_clazz.add(Util.format(nodeClazz.getName().toString()));
					}
				}
				/**
				 * 2. fields level </br>
				 * base like </br>
				 * "Lcom/example/service/StudentService2.dao Lcom/example/dao/StudentDao"
				 */
				Set<IField> allFields = new HashSet<>();
				allFields.addAll(nodeClazz.getDeclaredInstanceFields());
				allFields.addAll(nodeClazz.getDeclaredStaticFields());
				if (allFields != null) {
					allFields.forEach(f1 -> {
						if (f1 != null && f1.getAnnotations() != null && !f1.getAnnotations().isEmpty()) {
							String base = f1.getReference().getSignature();
							List<infoPair> clazzUnitFs = new ArrayList<>();
							for (Annotation anno : f1.getAnnotations()) {

								Map<String, String> anno_key_value = new HashMap<String, String>();
								for (String key : anno.getNamedArguments().keySet()) {
									ElementValue v = anno.getNamedArguments().get(key);
									anno_key_value.put(key, reFormatAnnosValue(v));
								}

//								if (Util.isFrameworkAnnos(anno.getType().getName().toString())) {
								if (ConfigUtil.isFrameworkMarks(anno.getType().getName().toString())) {
									infoPair pair = new infoPair(Util.format(anno.getType().getName().toString()),
											anno_key_value);

									clazzUnitFs.add(pair);
									allMarks.add(Util.format(anno.getType().getName().toString()));
								}

							}
							if (!clazzUnitFs.isEmpty()) {
								infoUnit unit = new infoUnit(marksType.anno, marksLevel.Field, Util.format(base),
										clazzUnitFs);
								infoUnits.add(unit);
								infoUnits_field.add(unit);
								bases_field.add(Util.format(nodeClazz.getName().toString()));
							}
						}
					});
				}
				/**
				 * 3. method level</br>
				 * "com.example.aop.printStatus.aroundAction(Lorg/aspectj/lang/ProceedingJoinPoint;)Ljava/lang/Object;"
				 */
				if (nodeClazz.getDeclaredMethods() != null) {
					nodeClazz.getDeclaredMethods().forEach(m1 -> {
						if (m1 != null && m1.getAnnotations() != null && !m1.getAnnotations().isEmpty()) {
							String base = m1.getSignature();
							List<infoPair> clazzUnitFs = new ArrayList<>();
							for (Annotation anno : m1.getAnnotations()) {
								Map<String, String> anno_key_value = new HashMap<String, String>();
								for (String key : anno.getNamedArguments().keySet()) {
									ElementValue v = anno.getNamedArguments().get(key);
									String s = reFormatAnnosValue(v);
									if (s != null) {
										anno_key_value.put(key, s);
										/** AOP1: */
										if (s.contains("execution(")) {
											AOPaspectJ_mark.add(Util.format(anno.getType().getName().toString()));
										}
									}

								}
//								if (Util.isFrameworkAnnos(anno.getType().getName().toString())) {
								if (ConfigUtil.isFrameworkMarks(anno.getType().getName().toString())) {
									infoPair pair = new infoPair(Util.format(anno.getType().getName().toString()),
											anno_key_value);

									clazzUnitFs.add(pair);

									allMarks.add(Util.format(anno.getType().getName().toString()));

								}
							}
							if (!clazzUnitFs.isEmpty()) {
								infoUnit unit = new infoUnit(marksType.anno, marksLevel.Method, base, clazzUnitFs);
								infoUnits.add(unit);
								infoUnits_mthd.add(unit);
								bases_mthd.add(Util.format(nodeClazz.getName().toString()));
							}
						}
					});
				}
			}
		});

		bases.addAll(bases_clazz);
		bases.addAll(bases_field);
		bases.addAll(bases_mthd);

		return infoUnits;
	}

	/** TODO: can improve more precise */
	public static String reFormatAnnosValue(ElementValue v) {
		if (v == null)
			return null;

		String ret = null;
		if (v instanceof ConstantElementValue) {
			ret = ((ConstantElementValue) v).val.toString();
//			System.out.println("ConstantElementValue :" + ((ConstantElementValue) v).val);
		}
		if (v instanceof ArrayElementValue) {
			ElementValue tmp = ((ArrayElementValue) v).vals[0];
			if (tmp instanceof ConstantElementValue) {
				ret = ((ConstantElementValue) tmp).val.toString();
			}
			if (tmp instanceof EnumElementValue) {
				ret = ((EnumElementValue) tmp).enumVal;
			}
//			System.out.println("ArrayElementValue :" + ((ArrayElementValue) v).vals[0]);
		}
		if (v instanceof EnumElementValue) {
			ret = ((EnumElementValue) v).enumVal;
//			System.out.println("EnumElementValue :" + ((EnumElementValue) v).enumVal);
		}

		/** TODO: */
		if (v instanceof AnnotationAttribute) {
			System.out.println("AnnotationAttribute :" + ((AnnotationAttribute) v).elementValues.get("vals"));
		}

		return ret;

	}

	public Set<infoUnit> getInfoUnits_clazz() {
		return infoUnits_clazz;
	}

	public Set<infoUnit> getInfoUnits_field() {
		return infoUnits_field;
	}

	public Set<infoUnit> getInfoUnits_mthd() {
		return infoUnits_mthd;
	}

	public Set<String> getBases() {
		return bases;
	}

	public Set<String> getBases_clazz() {
		return bases_clazz;
	}

	public Set<String> getBases_field() {
		return bases_field;
	}

	public Set<String> getBases_mthd() {
		return bases_mthd;
	}

	public Set<String> getAOPaspectJ_mark() {
		return AOPaspectJ_mark;
	}

	public static void main(String args[]) {
		String path = args[0];
		GraphBuilder builder = new GraphBuilder(path);

		extractAnnos exAnnos = new extractAnnos();
		exAnnos.extract(builder.getCHA());
	}

}
