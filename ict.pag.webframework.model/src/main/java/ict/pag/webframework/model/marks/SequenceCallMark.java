package ict.pag.webframework.model.marks;

import java.util.HashSet;

import ict.pag.webframework.model.enumeration.MarkScope;

public class SequenceCallMark extends NormalMark {
	HashSet<String> preSet;

	public SequenceCallMark(MarkScope s, HashSet<String> preSet, HashSet<String> afterMarks) {
		super(afterMarks, s);
		this.preSet = preSet;
	}

	public HashSet<String> getPreSet() {
		return preSet;
	}

}
