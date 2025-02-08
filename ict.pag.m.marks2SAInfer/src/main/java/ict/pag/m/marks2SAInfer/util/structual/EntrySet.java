package ict.pag.m.marks2SAInfer.util.structual;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * entry marks represents as pair like:<br>
 * <{class level marks, method level marks}>
 */
public class EntrySet {
//	private HashMap<str2strPair, Integer> entrySet = new HashMap<>();

	private HashMap<set2setPair, Integer> entrySet = new HashMap<>();

	private Set<String> certain_entryIgnoreMarks = new HashSet<String>();
	private Set<String> added_entryIgnoreMarks = new HashSet<String>();

//	public boolean contains(set2setPair obj) {
//		for (set2setPair en : entrySet.keySet()) {
//			if (en.equals(obj))
//				return true;
//		}
//		return false;
//	}

	/**
	 * 1. this will check duplicate before add into Collection </br>
	 * 2. calculating minimal marks collection when adding</br>
	 * 3. calculating the marks that are certainly not entry
	 * 
	 */
	public void add(set2setPair obj0) {

		Set<String> new_first = new HashSet<>();
		Set<String> new_sec = new HashSet<>();
		set2setPair obj = null;

		for (set2setPair en : entrySet.keySet()) {
			// 1. remove ignore marks

			for (String tmp : obj0.getFirst()) {
				if (added_entryIgnoreMarks.contains(tmp)) {
//					obj.getFirst().remove(tmp);
				} else {
					new_first.add(tmp);
				}
			}

			for (String tmp : obj0.getSecond()) {
				if (added_entryIgnoreMarks.contains(tmp)) {
//					obj.getSecond().remove(tmp);
				} else {
					new_sec.add(tmp);
				}
			}

//			if (obj.getSecond().isEmpty())
//				return;
//			if (new_sec.isEmpty())
			// changed in 2021.9.7 class and method has one enough
			if (new_sec.isEmpty() && new_first.isEmpty())
				return;

			obj = new set2setPair(new_first, new_sec);

			// 2. deal with current element in entrySet, reduce to minimal collection
			// reduce the collection
			if (set2setPair.hasSameElement(obj.getFirst(), en.getFirst())
					&& set2setPair.hasSameElement(obj.getSecond(), en.getSecond())) {
				/** 1.1 class level marks */
				if (set2setPair.contains(obj.getFirst(), en.getFirst())) {
					Set<String> diff = set2setPair.calDifferenceSet(obj.getFirst(), en.getFirst());
					if (!diff.isEmpty()) {
						for (String rm : diff) {

							obj.getFirst().remove(rm);
//						certain_entryIgnoreMarks.add(type + ":class:" + rm);
							certain_entryIgnoreMarks.add(rm);

						}
					}
				} else if (set2setPair.contains(en.getFirst(), obj.getFirst())) {
					Set<String> diff = set2setPair.calDifferenceSet(en.getFirst(), obj.getFirst());
					if (!diff.isEmpty()) {
						for (String rm : diff) {

							en.getFirst().remove(rm);
							certain_entryIgnoreMarks.add(rm);
//						certain_entryIgnoreMarks.add(type + ":class:" + rm);

						}
					}
				}
				/** 1.2 method level marks */
				if (set2setPair.contains(obj.getSecond(), en.getSecond())) {
					Set<String> diff = set2setPair.calDifferenceSet(obj.getSecond(), en.getSecond());
					if (!diff.isEmpty()) {
						for (String rm : diff) {
							obj.getSecond().remove(rm);
//						certain_entryIgnoreMarks.add(type + ":mtd:" + rm);
							certain_entryIgnoreMarks.add(rm);
						}
					}
				} else if (set2setPair.contains(en.getSecond(), obj.getSecond())) {
					Set<String> diff = set2setPair.calDifferenceSet(en.getSecond(), obj.getSecond());
					if (!diff.isEmpty()) {
						for (String rm : diff) {
							en.getSecond().remove(rm);
//						certain_entryIgnoreMarks.add(type + ":mtd:" + rm);
							certain_entryIgnoreMarks.add(rm);
						}
					}
				}
			}

//			if (en.equals(obj)) {
//				int count = entrySet.get(en);
//				entrySet.put(en, count + 1);
//				nofound = false;
//				break;
//			}
		}

		if (obj == null)
			obj = obj0;

		// 3. add into entries
		boolean nofound = true;
		for (set2setPair en : entrySet.keySet()) {
			if (en.equals(obj)) {
				int count = entrySet.get(en);
				entrySet.put(en, count + 1);
				nofound = false;
				break;
			}
		}
		if (nofound) {
			entrySet.put(obj, 1);
		}

	}

	public HashMap<set2setPair, Integer> getEntrySet() {
		return entrySet;
	}

	public Set<String> getCertain_entryIgnoreMarks() {
		return certain_entryIgnoreMarks;
	}

	public void addEntryIgnore(Set<String> ignores) {
		added_entryIgnoreMarks.addAll(ignores);
	}

	public void duplicateRemove() {
		// remove duplicated elements
		HashMap<set2setPair, Integer> entries = new HashMap<>();
		for (set2setPair en : entrySet.keySet()) {
			boolean add = true;
			for (set2setPair en2 : entries.keySet()) {
				if (en.equals(en2)) {
					int count = entrySet.get(en);
					entries.put(en2, count + entries.get(en2));
					add = false;
					break;
				}
			}
			if (add) {
				entries.put(en, entrySet.get(en));
			}
		}
		entrySet = entries;
	}

	/** only for entry points */
	public void removeCalculatedIgnores() {
		HashMap<set2setPair, Integer> entries = new HashMap<>();

		for (set2setPair en : entrySet.keySet()) {
			Set<String> new_first = new HashSet<>();
			Set<String> new_sec = new HashSet<>();

			for (String first : en.getFirst()) {
				if (!certain_entryIgnoreMarks.contains(first))
					new_first.add(first);
			}

			for (String sec : en.getSecond()) {

				if (!certain_entryIgnoreMarks.contains(sec)) {

					new_sec.add(sec);
				}
			}

			if (new_first.isEmpty() && new_sec.isEmpty())
				continue;

			set2setPair obj = new set2setPair(new_first, new_sec);

			entries.put(obj, entrySet.get(en));
		}

		entrySet = entries;

	}

}
