package ict.pag.m.generateFrameworkModel4App.appInfoParsers;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.annotations.Annotation;

import ict.pag.m.frameworkInfoUtil.customize.Util;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.AnnotationEntity;

public class AnnotationExtractor {

	/** Answers: annotations */
	HashMap<String, HashSet<AnnotationEntity>> mark2classSet = new HashMap<>();
	HashMap<String, HashSet<AnnotationEntity>> mark2fieldSet = new HashMap<>();
	HashMap<String, HashSet<AnnotationEntity>> mark2methodSet = new HashMap<>();

	public void extract(ClassHierarchy cha) {
		cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(classNode -> {
			// 1. class layer
			Collection<Annotation> clazzAnnos = classNode.getAnnotations();
			if (clazzAnnos != null && !clazzAnnos.isEmpty()) {
				for (Annotation anno : clazzAnnos) {
					String annoName = Util.format(anno.getType().getName().toString());
					if (mark2classSet.containsKey(annoName)) {
						mark2classSet.get(annoName).add(new AnnotationEntity(anno, classNode));
					} else {
						HashSet<AnnotationEntity> tmp = new HashSet<>();
						tmp.add(new AnnotationEntity(anno, classNode));
						mark2classSet.put(annoName, tmp);
					}

				}
			}

			// 2. field level
			Set<IField> allFields = new HashSet<>();
			allFields.addAll(classNode.getDeclaredInstanceFields());
			allFields.addAll(classNode.getDeclaredStaticFields());
			if (allFields != null) {
				allFields.forEach(f1 -> {
					if (f1 != null && f1.getAnnotations() != null && !f1.getAnnotations().isEmpty()) {
						for (Annotation anno : f1.getAnnotations()) {
							String annoName = Util.format(anno.getType().getName().toString());
							if (mark2fieldSet.containsKey(annoName)) {
								mark2fieldSet.get(annoName).add(new AnnotationEntity(anno, f1));
							} else {
								HashSet<AnnotationEntity> tmp = new HashSet<>();
								tmp.add(new AnnotationEntity(anno, f1));
								mark2fieldSet.put(annoName, tmp);
							}
						}
					}
				});
			}

			// 3. method level
			if (classNode.getDeclaredMethods() != null) {
				classNode.getDeclaredMethods().forEach(m1 -> {
					if (m1 != null && m1.getAnnotations() != null && !m1.getAnnotations().isEmpty()) {
						for (Annotation anno : m1.getAnnotations()) {
							String annoName = Util.format(anno.getType().getName().toString());
							if (mark2methodSet.containsKey(annoName)) {
								mark2methodSet.get(annoName).add(new AnnotationEntity(anno, m1));
							} else {
								HashSet<AnnotationEntity> tmp = new HashSet<>();
								tmp.add(new AnnotationEntity(anno, m1));
								mark2methodSet.put(annoName, tmp);
							}
						}
					}
				});
			}
		});
	}

	public HashMap<String, HashSet<AnnotationEntity>> getMark2classSet() {
		return mark2classSet;
	}

	public HashMap<String, HashSet<AnnotationEntity>> getMark2fieldSet() {
		return mark2fieldSet;
	}

	public HashMap<String, HashSet<AnnotationEntity>> getMark2methodSet() {
		return mark2methodSet;
	}

}
