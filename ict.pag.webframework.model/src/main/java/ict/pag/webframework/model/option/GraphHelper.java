package ict.pag.webframework.model.option;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.TypeReference;

public class GraphHelper {
	/** child is already validated as parent's child */
	public static boolean isOverrideFrom(IMethod child, IMethod parent, ClassHierarchy cha) {
		String funcName1 = parent.getReference().getName().toString();
		String funcName2 = child.getReference().getName().toString();
		if (funcName1.equals(funcName2) && (parent.getNumberOfParameters() == child.getNumberOfParameters())) {
			boolean same = true;
			int i;
			if ((parent.isStatic() && child.isStatic())) {
				i = 0;
			} else if (!parent.isStatic() && !child.isStatic()) {
				i = 1;
			} else {
				return false;
			}

			for (; i < parent.getNumberOfParameters(); i++) {
				if (!hasInheritanceRelation(parent.getParameterType(i), child.getParameterType(i), cha)) {
					same = false;
					break;
				}
			}
			if (!hasInheritanceRelation(parent.getReturnType(), child.getReturnType(), cha)) {
				same = false;
			}
			if (same) {
				return true;

			}
		}
		return false;
	}

	private static boolean hasInheritanceRelation(TypeReference tp1, TypeReference tp2, ClassHierarchy cha) {
		if (tp1.equals(tp2)) {
			return true;
		} else if (tp1.isReferenceType() && tp2.isReferenceType()) {
			IClass c1 = cha.lookupClass(tp1);
			IClass c2 = cha.lookupClass(tp2);
			if (c1 != null && c2 != null) {
				if (cha.isSubclassOf(c1, c2) || cha.isSubclassOf(c2, c1)) {
					return true;
				}
			}
		}
		return false;
	}
}
