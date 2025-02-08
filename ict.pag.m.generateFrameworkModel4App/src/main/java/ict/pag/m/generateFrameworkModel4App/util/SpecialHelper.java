package ict.pag.m.generateFrameworkModel4App.util;

import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;

public class SpecialHelper {

	public static HashSet<IMethod> findSpecificMethod(IClass candidate, String tar_mtd) {
		HashSet<IMethod> ret = new HashSet<IMethod>();
		for (IMethod m : candidate.getAllMethods()) {
			if (m.getSignature().contains(tar_mtd)) {
				ret.add(m);
			}
		}
		return ret;
	}

	public static Set<IMethod> findAllInitMethods(IClass c1) {
		Set<IMethod> initMethods = new HashSet<>();
		if (c1 != null) {
			c1.getDeclaredMethods().forEach(node -> {
				if (node.isInit())
					initMethods.add(node);
			});
		}

		return initMethods;
	}

	public static Set<IMethod> findAllClinitMethods(IClass c1) {
		Set<IMethod> ClinitMethods = new HashSet<>();
		if (c1 != null) {
			c1.getDeclaredMethods().forEach(node -> {
				if (node.isClinit())
					ClinitMethods.add(node);
			});
		}

		return ClinitMethods;
	}
	
}
