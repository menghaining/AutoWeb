package ict.pag.m.frameworkInfoUtil.customize;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;

public class Util {
	public static HashSet<String> applicationClasses = new HashSet<>();

	public static String format(String base) {
		if (!base.contains("/"))
			return base;
		String newS = null;
		newS = base.replace('/', '.').substring(1);

		return newS;
	}

	public static String reformatClass(String base) {

		String newStr = "L";
		return newStr + base.replaceAll("\\.", "/") + ";";

	}

	public static void setApplicationClasses(HashSet<String> applicationClasses2) {
		applicationClasses = applicationClasses2;
	}

	public static boolean hasApplicationClass(String check) {

		if (check.startsWith("L") && check.contains("/"))
			check = check.substring(1).replaceAll("/", ".");

		for (String c : applicationClasses) {
			if (check.startsWith(c))
				return true;
		}
		return false;
	}

	/**
	 * from
	 * 'co.yiiu.pybbs.interceptor.CommonInterceptor.preHandle(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;)Z'
	 * to</br>
	 * 'co.yiiu.pybbs.interceptor.CommonInterceptor.preHandle(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse,java.lang.Object)'
	 */
	public static String format2(String base) {
		String tmp = base.substring(base.indexOf('(') + 1, base.length() - 1);
		String ret = "";
		String[] args = tmp.split(";");
		for (String arg : args) {
			ret.concat(format(arg)).concat(";");
		}
		return base.substring(0, base.indexOf('(') + 1) + ret + ")";

	}

	public static Set<String> getAllCodeClazz(ClassHierarchy cha) {
		Set<String> clazzSigs = new HashSet<String>();
		cha.forEach(nodeClazz -> {
			if (nodeClazz.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
				clazzSigs.add(format(nodeClazz.getName().toString()));
			}
		});
		return clazzSigs;
	}

	/**
	 * calculate the elements occur count in 'marks', save into 'marks2count'
	 */
	public static void calculateCounts(Map<String, Integer> marks2count, ArrayList<String> marks) {
		for (String mark : marks) {
			if (marks2count.containsKey(mark)) {
				marks2count.put(mark, marks2count.get(mark).intValue() + 1);
			} else {
				marks2count.put(mark, 1);
			}
		}

	}

	public static SourcePosition getSourcePosition(IMethod method, int instIndex) throws InvalidClassFileException {
		try {
			if (method instanceof IBytecodeMethod) {
				return ((IBytecodeMethod) method)
						.getSourcePosition(((IBytecodeMethod) method).getBytecodeIndex(instIndex));
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			// TODO: handle exception
		}
		return null;
	}

}
