package ict.pag.webframework.marks;

import java.util.HashSet;
import java.util.Iterator;

import com.ibm.wala.types.annotations.Annotation;

public class MarksHelper {

	public static String resolveAnnotationName(Annotation anno) {
		return anno.getType().getName().toString();
	}

	/**
	 * If collection1 contains colletion2, then calculate collection1-collection2
	 * and return</br>
	 * 
	 * @return null iff collection1 do not contains collection2, or one of them is
	 *         empty; empty set iff the two set are same.
	 */
	public static HashSet<String> contains(HashSet<String> collection1, HashSet<String> collection2) {
		if (collection1 == null || collection1.isEmpty() || collection2 == null || collection2.isEmpty())
			return null;

		HashSet<String> diffSet = null;

		boolean allContains = true;
		// 1. all elements in c2, existing one in collection1 equals to
		for (String c2 : collection2) {
			boolean containsOne = false;
			for (String c1 : collection1) {
				if (c1.equals(c2)) {
					containsOne = true;
					break;
				}
			}
			if (!containsOne) {
				allContains = false;
				break;
			}
		}

		// if collection1 contains collection2, calculate redundant elements
		if (allContains) {
			diffSet = new HashSet<>();
			for (String c1 : collection1) {
				boolean hasOne = false;
				for (String c2 : collection2) {
					if (c1.equals(c2)) {
						hasOne = true;
						break;
					}
				}
				if (!hasOne) {
					diffSet.add(c1);
				}
			}
		}
		return diffSet;
	}

	/**
	 * merge set1 and set2, if they has contains relation</br>
	 * If they do not has contains relation, return null;</br>
	 * else, return the difference set
	 */
	public static HashSet<String> mergeMarks(HashSet<String> set1, HashSet<String> set2) {

		if (set1 == null || set1.isEmpty() || set2 == null || set2.isEmpty())
			return null;

		HashSet<String> set1DiffSet2 = MarksHelper.contains(set1, set2);
		HashSet<String> set2DiffSet1 = MarksHelper.contains(set2, set1);

		if (set1DiffSet2 == null) {
			if (set2DiffSet1 == null) {
				// set1 and set2 do not has contains relation
				return null;
			} else {
				// set2 contains set1, and has more elements
				for (String reduce : set2DiffSet1) {
					Iterator<String> it = set2.iterator();
					while (it.hasNext()) {
						String curr = it.next();
						if (reduce.equals(curr)) {
							it.remove();
						}
					}
				}
				return set2DiffSet1;
			}

		} else if (set1DiffSet2.isEmpty()) {
			// set1 equals set2
			return null;
		} else {
			// set1 contains set2, and has more elements
			for (String reduce : set1DiffSet2) {
				Iterator<String> it = set1.iterator();
				while (it.hasNext()) {
					String curr = it.next();
					if (reduce.equals(curr)) {
						it.remove();
					}
				}
			}
			return set1DiffSet2;
		}

	}
}
