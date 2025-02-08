package ict.pag.m.marks2SAInfer.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class reducedSetCollection {
	private HashMap<HashSet<String>, Integer> collection = new HashMap<>();

	private Set<String> added_entryIgnoreMarks = new HashSet<String>();

	public Set<HashSet<String>> getCollection() {
		return collection.keySet();
	}

	public void addIgnoreClassMarks(Set<String> ignores) {
		added_entryIgnoreMarks.addAll(ignores);
	}

	public void addAll(reducedSetCollection alls) {
		for (HashSet<String> marksEle : alls.getCollection()) {
			add(marksEle);
		}

	}

	public void add(HashSet<String> add) {
		// prepare
		HashSet<String> add2 = new HashSet<String>();
		if (added_entryIgnoreMarks.isEmpty()) {
			add2 = add;
		} else {
			for (String a : add) {
				if (!added_entryIgnoreMarks.contains(a))
					add2.add(a);
			}
		}
		if (add2.isEmpty())
			return;

		// 1. reduce collection
		HashMap<HashSet<String>, HashSet<String>> need2Reput = new HashMap<>();
		for (HashSet<String> key : collection.keySet()) {
			if (contains(key, add2)) {
				HashSet<String> diff = calDifferenceSet(key, add2);
				HashSet<String> tmp = new HashSet<>();
				tmp.addAll(key);

				if (!diff.isEmpty()) {
					for (String rm : diff) {
						tmp.remove(rm);
					}
				}

				need2Reput.put(key, tmp);
			} else if (contains(add2, key)) {
				HashSet<String> diff = calDifferenceSet(add2, key);
				for (String rm : diff) {
					add2.remove(rm);
				}
			}
		}
		for (HashSet<String> key : need2Reput.keySet()) {
			int count = collection.get(key);
			collection.remove(key);
			collection.put(need2Reput.get(key), count);
		}
		// 2. add into collection
		boolean nofound = true;
		for (HashSet<String> key : collection.keySet()) {
			if (elementEquals(key, add2)) {
				int count = collection.get(key);
				collection.put(key, count + 1);
				nofound = false;
				break;
			}
		}
		if (nofound) {
			collection.put(add2, 1);
		}

//		// 3. reduce duplicated
//		// remove duplicated elements
//		HashMap<HashSet<String>, Integer> tmp = new HashMap<>();
//		for (HashSet<String> key : collection.keySet()) {
//			boolean add = true;
//			for (HashSet<String> en2 : tmp.keySet()) {
//				if (elementEquals(key, en2)) {
//					int count = collection.get(key);
//					tmp.put(en2, count + 1);
//					add = false;
//					break;
//				}
//
//			}
//			if (add) {
//				tmp.put(key, 1);
//			}
//		}
//		collection = tmp;

	}

	/** whether c1 elements equals c2 elements */
	public static boolean elementEquals(reducedSetCollection c1, reducedSetCollection c2) {
		Set<HashSet<String>> sc1 = c1.getAllElements();
		Set<HashSet<String>> sc2 = c2.getAllElements();

		if (sc1.size() != sc2.size())
			return false;

		for (HashSet<String> cc1 : sc1) {
			boolean flag2 = false;
			for (HashSet<String> cc2 : sc2) {
				if (contains(cc1, cc2) && contains(cc2, cc1)) {
					flag2 = true;
					break;
				}
			}
			if (!flag2)
				return false;
		}

		return true;
	}

	/** whether c1 elements equals c2 elements */
	public boolean elementEquals(Set<String> c1, Set<String> c2) {
		if (contains(c1, c2) && contains(c2, c1))
			return true;
		return false;
	}

	/** whether c1 contains c2 */
	public static boolean contains(Set<String> c1, Set<String> c2) {
		if (c1 == null || c2 == null || c1.isEmpty() || c2.isEmpty())
			return false;

		boolean contains = true;
		for (String cc2 : c2) {
			if (!c1.contains(cc2)) {
				contains = false;
				break;
			}
		}

		return contains;
	}

	/** return A-B Set iff A contains V=B */
	public HashSet<String> calDifferenceSet(Set<String> A, Set<String> B) {
		HashSet<String> ret = new HashSet<String>();

		if (contains(A, B)) {
			for (String a : A) {
				if (!B.contains(a))
					ret.add(a);
			}
		}

		return ret;
	}

	public Set<HashSet<String>> getAllElements() {
		return collection.keySet();
	}
}
