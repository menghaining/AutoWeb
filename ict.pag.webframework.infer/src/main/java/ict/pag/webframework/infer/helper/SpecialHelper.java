package ict.pag.webframework.infer.helper;

import java.util.HashSet;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

import ict.pag.webframework.infer.marks.ClassMethodPair;

public class SpecialHelper {

	/**
	 * 
	 * @param base like Lcom/example/service/StudentService/getAllStus()
	 * @return like com.example.service.StudentService.getAllStus()
	 */
	public static String formatSignature(String base) {
		if (!base.contains("/"))
			return base;
		String newS = null;
		newS = base.replace('/', '.').substring(1);

		return newS;
	}

	public static boolean contains(HashSet<ClassMethodPair> set, ClassMethodPair ele) {
		for (ClassMethodPair pair : set) {
			if (pair.isSame(ele))
				return true;
		}
		return false;
	}

	/**
	 * 
	 * @param base like com.example.service.StudentService.getAllStus()
	 * @return like Lcom/example/service/StudentService/getAllStus();
	 */
//	public static String reformatSignature(String base) {
//		String newStr = "L";
//		return newStr + base.replaceAll("\\.", "/") + ";";
//
//	}
//
//	public static SourcePosition getSourcePosition(IMethod method, int instIndex) {
//		try {
//			if (method instanceof IBytecodeMethod) {
//				return ((IBytecodeMethod) method)
//						.getSourcePosition(((IBytecodeMethod) method).getBytecodeIndex(instIndex));
//			}
//		} catch (ArrayIndexOutOfBoundsException | InvalidClassFileException e) {
//			// TODO: handle exception
//			return null;
//		}
//
//		return null;
//	}
//
//	/**
//	 * @param cha
//	 * @param clazz the format need to be "Ljava/lang/Object"
//	 * 
//	 * @return the class finded in cha-application scope, null if not find
//	 */
//	public static IClass getClassFromApplicationCHA(ClassHierarchy cha, String clazz) {
//		IClass givenClass = cha.lookupClass(
//				TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
//
//		return givenClass;
//	}
//
//	public static String reFormatAnnosValue(ElementValue v) {
//		if (v == null)
//			return null;
//
//		String ret = null;
//		if (v instanceof ConstantElementValue) {
//			ret = ((ConstantElementValue) v).val.toString();
//		}
//		if (v instanceof ArrayElementValue) {
//			ElementValue tmp = ((ArrayElementValue) v).vals[0];
//			if (tmp instanceof ConstantElementValue) {
//				ret = ((ConstantElementValue) tmp).val.toString();
//			}
//			if (tmp instanceof EnumElementValue) {
//				ret = ((EnumElementValue) tmp).enumVal;
//			}
//		}
//		if (v instanceof EnumElementValue) {
//			ret = ((EnumElementValue) v).enumVal;
//		}
//
//		/** TODO: */
//		if (v instanceof AnnotationAttribute) {
//			System.out.println("AnnotationAttribute :" + ((AnnotationAttribute) v).elementValues.get("vals"));
//		}
//		return ret;
//	}

	public static IMethod genIMethod(ClassHierarchy cha, String call) {
		String s = call.substring(0, call.lastIndexOf('.'));

		String clazz = "L" + s.replaceAll("\\.", "/");
		IClass givenClass = cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
		if (givenClass == null)
			return null;

		for (IMethod m : givenClass.getDeclaredMethods()) {
			if (m.getSignature().equals(call)) {
				return m;
			}
		}

		return null;
	}

	/**
	 * input like "cn.zju.action.LoginAction.username:Ljava/lang/String;"
	 */
	public static IField genIField(ClassHierarchy cha, String fieldSig) {
		String s = fieldSig.substring(0, fieldSig.lastIndexOf('.'));
		String fieldName = fieldSig.substring(fieldSig.lastIndexOf('.') + 1, fieldSig.indexOf(':'));

		String clazz = "L" + s.replaceAll("\\.", "/");
		IClass givenClass = cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
		if (givenClass == null)
			return null;

		for (IField f : givenClass.getAllInstanceFields()) {
			String name = f.getReference().getName().toString();
			if (name.equals(fieldName))
				return f;
		}

		return null;
	}

	public static boolean isSame(HashSet<String> s1, HashSet<String> s2) {
		if (s1 == null && s2 == null)
			return true;
		if (s1 == null || s2 == null)
			return false;
		if (s1.isEmpty() && s2.isEmpty())
			return true;
		if (s1.isEmpty() || s2.isEmpty())
			return false;

		if (s1.containsAll(s2) && s2.containsAll(s1))
			return true;

		return false;

	}

	public static boolean setSetContains(HashSet<HashSet<String>> setSet, HashSet<String> set2) {
		for (HashSet<String> s1 : setSet) {
			if (isSame(s1, set2))
				return true;
		}
		return false;
	}

	/**
	 * exclude the elements in set1 which also in set2
	 */
	public static HashSet<String> excludeElementsFrom(HashSet<String> set1, HashSet<String> set2) {
		HashSet<String> ret = new HashSet<>();
		for (String ele : set1) {
			if (!set2.contains(ele))
				ret.add(ele);
		}
		return ret;
	}

}
