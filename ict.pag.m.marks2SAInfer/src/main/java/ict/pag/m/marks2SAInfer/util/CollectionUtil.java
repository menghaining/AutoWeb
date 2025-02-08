package ict.pag.m.marks2SAInfer.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ict.pag.m.frameworkInfoUtil.customize.ConfigUtil;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoPair;

public class CollectionUtil {
	public static Set<String> calSameElements(Set<String> s1, Set<String> s2) {
		Set<String> ret = new HashSet<>();

		if (s1 == null || s1 == null)
			return ret;

		for (String e1 : s1) {
			if (s2.contains(e1))
				ret.add(e1);
		}

		return ret;
	}

	public static ArrayList<String> removeAllSetGetcall(ArrayList<String> sequences) {
		ArrayList<String> ret = new ArrayList<String>();
		for (String c : sequences) {
			if (c.lastIndexOf(".") < 0)
				continue;
			String mthdName = c.substring(c.lastIndexOf(".") + 1).toLowerCase();
			if (!mthdName.startsWith("set") && !mthdName.startsWith("get")) {
				ret.add(c);
			}
		}

		return ret;

	}

	public static boolean isSameFields(List<infoPair> fields1, List<infoPair> fields2) {
		if (fields1 == null || fields2 == null)
			return false;
		if (fields1.isEmpty() || fields2.isEmpty())
			return false;

		infoPair currLayer1 = fields1.get(fields1.size() - 1);
		infoPair currLayer2 = fields2.get(fields2.size() - 1);

		Set<String> vals1 = currLayer1.getAllValues();
		Set<String> vals2 = currLayer2.getAllValues();

		if (isSame(vals1, vals2))
			return true;

		return false;
	}

	public static boolean isSame(Set<String> vals1, Set<String> vals2) {

		for (String val1 : vals1) {
			if (vals2 == null || vals2.isEmpty())
				return false;
			if (!vals2.contains(val1))
				return false;
		}

		for (String val2 : vals2) {
			if (vals1 == null || vals1.isEmpty())
				return false;
			if (!vals1.contains(val2))
				return false;
		}

		return true;
	}

	/** whether c1 contains c2 */
	public static boolean contains(Set<String> c1, Set<String> c2) {
		if (c1 == null || c2 == null || c1.isEmpty() || c2.isEmpty())
			return false;

		boolean contains = true;
		for (String cc2 : c2) {
//			boolean eq = false;
//			for (String cc1 : c1) {
//				if (cc1.equals(cc2)) {
//					eq = true;
//					break;
//				}
//				if (!eq) {
//					contains = false;
//					break;
//				}
//			}
			if (!c1.contains(cc2)) {
				contains = false;
				break;
			}
		}

		return contains;
	}

	/** return A-B Set iff A contains B */
	public static HashSet<String> calDifferenceSet(Set<String> A, Set<String> B) {
		HashSet<String> ret = new HashSet<String>();

		if (contains(A, B)) {
			for (String a : A) {
				if (!B.contains(a))
					ret.add(a);
			}
		}

		return ret;
	}

	/**
	 * This function created because :</br>
	 * set.contains(ele) implemented by hashcode,</br>
	 * and same string may have different hashcode
	 */
//	public static boolean hashsetContainsEle(HashSet<String> set, String ele) {
//
//		for (String s : set) {
//			if (s.equals(ele))
//				return true;
//		}
//
//		return false;
//	}

	@Deprecated
	public static String frameworkInheritanceSetContainsEle(HashSet<String> set, String ele) {
		String ret = "";
		// if there is no
		if (!ConfigUtil.containsLibInfo)
			return ele;

		for (String s : set) {
//			if (s.contains("contextInitialized")) {
//				System.out.println(s);
//			}
			if (s.equals(ele)) {
				return ele;
			}

			// return type and parameters may be subclass of frameworkClass
			if (s.startsWith(ele.substring(0, ele.indexOf('('))))
				return s;
		}

		return ret;
	}
}
