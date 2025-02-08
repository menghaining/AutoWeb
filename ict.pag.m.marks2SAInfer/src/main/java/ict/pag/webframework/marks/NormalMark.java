package ict.pag.webframework.marks;

import java.util.HashSet;

import ict.pag.webframework.enumeration.MarkScope;

public class NormalMark {
	protected HashSet<String> allMarks = new HashSet<>();

	protected MarkScope scope;

	public NormalMark() {
	}

	public NormalMark(HashSet<String> allMarks, MarkScope s) {
		this.allMarks = allMarks;
		this.scope = s;
	}

	public void mergeMarks(NormalMark obj, HashSet<String> ignoreSet) {
		HashSet<String> currMarks = allMarks;
		HashSet<String> comingMarks = obj.getAllMarks();
		HashSet<String> ignoreMarks = MarksHelper.mergeMarks(currMarks, comingMarks);
		if (ignoreMarks != null && !ignoreMarks.isEmpty())
			ignoreSet.addAll(ignoreMarks);

	}

	public boolean isAllEmpty() {
		if (allMarks == null || allMarks.isEmpty())
			return true;
		return false;
	}

	public boolean isSame(NormalMark obj) {
		boolean same = false;

		if (allMarks.isEmpty() && obj.getAllMarks().isEmpty()) {
			same = true;
		} else {
			HashSet<String> set1DiffSet2 = MarksHelper.contains(allMarks, obj.getAllMarks());
			if (set1DiffSet2 != null && set1DiffSet2.isEmpty())
				same = true;
		}

		if (same)
			return true;
		return false;
	}

	public HashSet<String> getAllMarks() {
		return allMarks;
	}

	public MarkScope getScope() {
		return scope;
	}

}
