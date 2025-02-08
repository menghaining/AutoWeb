package ict.pag.webframework.preInstrumental.helper;

import java.util.ArrayList;
import java.util.HashSet;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;

public class CtObjectCopyHelper {
	/**
	 * add an copy of originalField with addfieldName,</br>
	 * add it to cc, and also add setter/getter/constructor(also annotations) if any
	 * on originalField</br>
	 * ONLY modify the class bytecode!</br>
	 * also copy annotation in FieldInfo
	 * 
	 * @param clean: clean is that this copyed field donot has annotation
	 */
	public static CtField addNewFieldCopy(CtClass cc, CtField originalField, String addfieldName, boolean clean, CtClass fieldDecType) {
		CtField addfield = null;
		if (cc.isFrozen()) {
			cc.defrost();
		}
		// add a field only name different
		try {
			// 1. copy this field and add to cc class
			addfield = new CtField(originalField, cc);
			if (fieldDecType != null)
				addfield.setType(fieldDecType);
			if (clean) {
				// remove all annotation of this field
				FieldInfo info = addfield.getFieldInfo();
				if (info != null) {
					AttributeInfo attr0 = info.getAttribute(AnnotationsAttribute.visibleTag);
					if (attr0 instanceof AnnotationsAttribute) {
						AnnotationsAttribute attr = (AnnotationsAttribute) attr0;
						ArrayList<Annotation> candidates = new ArrayList<>();
						for (Annotation anno : attr.getAnnotations()) {
							candidates.add(anno);
						}
						for (Annotation anno : candidates) {
							attr.removeAnnotation(anno.getTypeName());
						}
					}
				}
			}

			addfield.setName(addfieldName);
			cc.addField(addfield);
			// 2. if the field has default setter/getter, then add, also the
			String originalFieldName = originalField.getName();
			boolean setOnce = false;
			boolean getOnce = false;
			for (CtMethod mtd : cc.getDeclaredMethods()) {
				String mtdName = mtd.getName();
				if (mtdName != null) {
					if (mtdName.toLowerCase().equals("set" + originalFieldName.toLowerCase())) {
						setOnce = true;
					} else if (mtdName.toLowerCase().equals("get" + originalFieldName.toLowerCase())) {
						getOnce = true;
					}
				}
			}
			if (setOnce) {
				CtMethod fieldSetterMtd = CtNewMethod.setter("set" + addfieldName.substring(0, 1).toUpperCase() + addfieldName.substring(1), addfield);
				cc.addMethod(fieldSetterMtd);
			}
			if (getOnce) {
				CtMethod fieldGetterMtd = CtNewMethod.getter("get" + addfieldName.substring(0, 1).toUpperCase() + addfieldName.substring(1), addfield);
				cc.addMethod(fieldGetterMtd);
			}
			// 3. if constructor has write statement of original_field, also write new_field
//			for (CtConstructor cons : cc.getConstructors()) {
//				try {
//					cons.instrument(new ExprEditor() {
//						public void edit(FieldAccess f) throws CannotCompileException {
//							if (f.isWriter()) {
//								try {
//									CtField field = f.getField();
//									if (field != null) {
//										if (field.getName().equals(originalField.getName())) {
//											String originalStmt = "$0 = $proceed($$);";
//											String addStmt = addfieldName + "=$1;";
//											StringBuffer sb = new StringBuffer();
//											sb.append("{");
//											sb.append(originalStmt);
//											sb.append(addStmt);
//											sb.append("}");
//											f.replace(sb.toString());
//										}
//									}
//								} catch (NotFoundException e1) {
//									e1.printStackTrace();
//								}
//							}
//						}
//					});
//				} catch (CannotCompileException e1) {
//					e1.printStackTrace();
//				}
//			}
		} catch (CannotCompileException e1) {
			e1.printStackTrace();
		}
		return addfield;
	}

	/**
	 * only add a copy of original method with name addMethodName in cc</br>
	 * copy annotation in methodInfo or not depends on the parameter clean
	 * 
	 * @param clean: method without any annotation
	 */
	public static CtMethod addNewMethodCopy(CtClass cc, CtMethod originalMethod, String addMethodName, boolean clean) {
		CtMethod mtd = null;
		if (cc.isFrozen()) {
			cc.defrost();
		}
		try {
			mtd = CtNewMethod.copy(originalMethod, addMethodName, cc, null);// do not copy annotations
			if (clean) {
				// remove attr
				MethodInfo info = mtd.getMethodInfo();
				if (info != null) {
					AttributeInfo mtdAttrInfo = info.getAttribute(AnnotationsAttribute.visibleTag);
					if (mtdAttrInfo instanceof AnnotationsAttribute) {
						AnnotationsAttribute mtdAttr = (AnnotationsAttribute) mtdAttrInfo;
						ArrayList<Annotation> candidates = new ArrayList<>();
						for (Annotation anno : mtdAttr.getAnnotations()) {
							candidates.add(anno);
						}
						for (Annotation anno : candidates) {
							mtdAttr.removeAnnotation(anno.getTypeName());
						}
					}
				}
			} else {
				// add annotation attrs
				AttributeInfo originalAnnoAttr = originalMethod.getMethodInfo().getAttribute(AnnotationsAttribute.visibleTag);
				MethodInfo mtdInfo = mtd.getMethodInfo();
				if (mtdInfo != null && originalAnnoAttr != null) {
					mtdInfo.addAttribute(originalAnnoAttr.copy(mtdInfo.getConstPool(), null));
				}
			}
			cc.addMethod(mtd);
		} catch (CannotCompileException e) {
			e.printStackTrace();
		}
		return mtd;
	}

	/**
	 * @return a copy of cc0 with addClassName
	 * 
	 * @param clean: method without any annotation
	 */
	public static CtClass addNewClassCopy(CtClass cc0, String addClassName, boolean clean) {
		CtClass addClass = null;
		if (cc0.isFrozen()) {
			cc0.defrost();
		}
		try {
			addClass = javassist.ClassPool.getDefault().getAndRename(cc0.getName(), addClassName);
			if (addClass == null)
				return null;
			if (clean) {
				AttributeInfo classAttrInfo = addClass.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
				if (classAttrInfo != null) {
					if (classAttrInfo instanceof AnnotationsAttribute) {
						AnnotationsAttribute classAttr = (AnnotationsAttribute) classAttrInfo;
						HashSet<Annotation> candidates = new HashSet<>();
						for (Annotation anno : classAttr.getAnnotations()) {
							candidates.add(anno);
						}
						for (Annotation anno : candidates) {
							classAttr.removeAnnotation(anno.getTypeName());
						}
					}
				}
			}
		} catch (NotFoundException e) {
			e.printStackTrace();
		}

		// dp not use setName any more
//		/* setName function will also modify the constructor name */
//		cc.setName(addClassName);

		return addClass;
	}

}
