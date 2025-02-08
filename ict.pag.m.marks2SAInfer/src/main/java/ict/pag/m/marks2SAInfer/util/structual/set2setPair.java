package ict.pag.m.marks2SAInfer.util.structual;

import java.util.HashSet;
import java.util.Set;

public class set2setPair {
	Set<String> fst = null;
	Set<String> sec = null;

	public set2setPair(Set<String> fst, Set<String> sec) {
		this.fst = fst;
		this.sec = sec;
	}

	public Set<String> getFirst() {
		return fst;
	}

	public Set<String> getSecond() {
		return sec;
	}

	public boolean equals(set2setPair obj) {
		Set<String> obj_fst = obj.getFirst();
		Set<String> obj_sec = obj.getSecond();

		if (fst == null || sec == null)
			return false;

		if (contains(fst, obj_fst) && contains(obj_fst, fst)) {
			if (contains(sec, obj_sec) && contains(obj_sec, sec))
				return true;
		}

		return false;
	}

	/** whether parentSet contains subSet */
	public static boolean contains(Set<String> parentSet, Set<String> subSet) {
		for (String sub : subSet) {
			if (!parentSet.contains(sub))
				return false;
		}

		return true;
	}

	/** return A-B Set iff A contains V=B */
	public static Set<String> calDifferenceSet(Set<String> A, Set<String> B) {
		Set<String> ret = new HashSet<String>();

		if (contains(A, B)) {
			for (String a : A) {
				if (!B.contains(a))
					ret.add(a);
			}
		}

		return ret;
	}

	public static boolean hasSameElement(Set<String> A, Set<String> B) {
		if (A.isEmpty() && B.isEmpty())
			return true;
		for (String a : A) {
			if (B.contains(a))
				return true;
		}

		return false;
	}

	public String toString() {
		return "<" + fst + ", " + sec + ">";
	}

}
