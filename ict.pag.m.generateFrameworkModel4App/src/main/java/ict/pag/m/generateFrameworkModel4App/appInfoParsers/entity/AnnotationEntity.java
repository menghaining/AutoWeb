package ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.annotations.Annotation;

public class AnnotationEntity {
	Annotation annotation;
	IClass clazz = null;
	IField field = null;
	IMethod method = null;
	String type;

	public AnnotationEntity(Annotation anno, Object obj) {
		annotation = anno;
		if (obj instanceof IClass) {
			clazz = (IClass) obj;
			type = "class";
		} else if (obj instanceof IField) {
			field = (IField) obj;
			type = "field";
		} else if (obj instanceof IMethod) {
			method = (IMethod) obj;
			type = "method";
		} else {
			System.err.println("error Annotation Objetc type!");
		}
	}

	public String toString() {
		return annotation.getType().getName().toString() + ":" + type;
	}

	public Annotation getAnnotation() {
		return annotation;
	}

	public String getType() {
		return type;
	}

	public IClass getClazz() {
		return clazz;
	}

	public IField getField() {
		return field;
	}

	public IMethod getMethod() {
		return method;
	}

}
