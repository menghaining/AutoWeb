package ict.pag.webframework.model.marks;

import java.util.HashSet;

public class Points2Mark extends NormalMark {
	private boolean isDeclare;
	private HashSet<String> fieldMarks;

	private HashSet<String> valuePoints2;
	private HashSet<String> valueAliasPoints2;

	public Points2Mark(boolean isDeclare, HashSet<String> valuePoints2, HashSet<String> valueAliasPoints2) {
		this.isDeclare = isDeclare;
		this.valuePoints2 = valuePoints2;
		this.valueAliasPoints2 = valueAliasPoints2;
	}

	public Points2Mark(boolean isDeclare, HashSet<String> fieldMarks, HashSet<String> valuePoints2,
			HashSet<String> valueAliasPoints2) {
		this.isDeclare = isDeclare;
		this.fieldMarks = fieldMarks;

		this.valuePoints2 = valuePoints2;
		this.valueAliasPoints2 = valueAliasPoints2;
	}

	public Points2Mark(HashSet<String> valuePoints2, HashSet<String> valueAliasPoints2) {

		this.isDeclare = false;
		this.valuePoints2 = valuePoints2;
		this.valueAliasPoints2 = valueAliasPoints2;
	}

	public boolean isAllEmpty() {
		if ((valuePoints2 == null || valuePoints2.isEmpty())
				&& (valueAliasPoints2 == null || valueAliasPoints2.isEmpty()))
			return true;

		return false;
	}

	public boolean isDeclare() {
		return isDeclare;
	}

	public HashSet<String> getValuePoints2() {
		return valuePoints2;
	}

	public HashSet<String> getValueAliasPoints2() {
		return valueAliasPoints2;
	}

	public HashSet<String> getFieldMarks() {
		return fieldMarks;
	}

}
