package ict.pag.webframework.infer.helper;

import java.util.Collection;
import java.util.HashSet;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Selector;
import ict.pag.m.instrumentation.Util.ConfigureUtil;

public class GraphHelper {

	public static HashSet<String> findOverrideFromFramework(IClass clazz, String stmt) {
		HashSet<String> allOverrideMethods = new HashSet<>();

		if (clazz != null) {
			IMethod method = null;
			for (IMethod mtd : clazz.getDeclaredMethods()) {
				if (mtd.getSignature().equals(stmt)) {
					method = mtd;
					break;
				}
			}
			if (method != null) {
				Selector selector = method.getSelector();
				HashSet<IClass> allSupers = new HashSet<>();
				collectAllFrameworkInheritance(clazz, allSupers);

				for (IClass superClass : allSupers) {
					IMethod m = superClass.getMethod(selector);
					if (m != null)
						if (ConfigureUtil.isFrameworkMarks(SpecialHelper.formatSignature(m.getDeclaringClass().getName().toString())))
							allOverrideMethods.add(m.getSignature());
				}
			}
		}

		return allOverrideMethods;
	}

	/** find all superclass or interfaces recursively */
	public static void collectAllFrameworkInheritance(IClass clazz, HashSet<IClass> ret) {
		if (clazz.isInterface()) {
			Collection<? extends IClass> superClassSet = clazz.getAllImplementedInterfaces();
			for (IClass superClass : superClassSet) {
				if (superClass != null) {
					if (superClass.getClassLoader().getReference().equals(ClassLoaderReference.Primordial)) {
						if (ConfigureUtil.isFrameworkMarks(SpecialHelper.formatSignature(superClass.getName().toString()))) {
							ret.add(superClass);
						}
					}
				}
			}
		} else {
			// current is not interface
			// current super class
			IClass superClazz = clazz.getSuperclass();
			if (superClazz != null) {
				if (ConfigureUtil.isFrameworkMarks(SpecialHelper.formatSignature(superClazz.getName().toString()))) {
					ret.add(superClazz);
					collectAllFrameworkInheritance(superClazz, ret);
				} else if (superClazz.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
					collectAllFrameworkInheritance(superClazz, ret);
				}
			}
			// current interface
			Collection<? extends IClass> superClassSet = clazz.getAllImplementedInterfaces();
			for (IClass superClass : superClassSet) {
				if (superClass != null) {
					if (superClass.getClassLoader().getReference().equals(ClassLoaderReference.Primordial)) {
						if (ConfigureUtil.isFrameworkMarks(SpecialHelper.formatSignature(superClass.getName().toString()))) {
							ret.add(superClass);
						}
					}
				}
			}
		}
	}

	/** child is already validated as parent's child */
//	public static boolean isOverrideFrom(IMethod child, IMethod parent, ClassHierarchy cha) {
//		String funcName1 = parent.getReference().getName().toString();
//		String funcName2 = child.getReference().getName().toString();
//		if (funcName1.equals(funcName2) && (parent.getNumberOfParameters() == child.getNumberOfParameters())) {
//			boolean same = true;
//			int i;
//			if ((parent.isStatic() && child.isStatic())) {
//				i = 0;
//			} else if (!parent.isStatic() && !child.isStatic()) {
//				i = 1;
//			} else {
//				return false;
//			}
//
//			for (; i < parent.getNumberOfParameters(); i++) {
//				if (!hasInheritanceRelation(parent.getParameterType(i), child.getParameterType(i), cha)) {
//					same = false;
//					break;
//				}
//			}
//			if (!hasInheritanceRelation(parent.getReturnType(), child.getReturnType(), cha)) {
//				same = false;
//			}
//			if (same) {
//				return true;
//
//			}
//		}
//		return false;
//	}
//
//	private static boolean hasInheritanceRelation(TypeReference tp1, TypeReference tp2, ClassHierarchy cha) {
//		if (tp1.equals(tp2)) {
//			return true;
//		} else if (tp1.isReferenceType() && tp2.isReferenceType()) {
//			IClass c1 = cha.lookupClass(tp1);
//			IClass c2 = cha.lookupClass(tp2);
//			if (c1 != null && c2 != null) {
//				if (cha.isSubclassOf(c1, c2) || cha.isSubclassOf(c2, c1)) {
//					return true;
//				}
//			}
//		}
//		return false;
//	}
}
