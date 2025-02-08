package ict.pag.webframework.model.marks;

import java.util.HashSet;

import ict.pag.webframework.model.enumeration.MarkScope;
import ict.pag.webframework.model.enumeration.ValueFrom;

public class ConcreteValueMark extends NormalMark {
	private ValueFrom val;
	private String attributeName;

	public ConcreteValueMark(HashSet<String> allMarks, MarkScope s, String attr, ValueFrom val) {
		super(allMarks, s);
		this.val = val;
		this.attributeName = attr;
	}

	public boolean isSame(ConcreteValueMark obj) {
		boolean same = false;

		if (allMarks.isEmpty() && obj.getAllMarks().isEmpty() && val.equals(obj.getVal())
				&& attributeName.equals(obj.getAttributeName())) {
			same = true;
		} else {
			HashSet<String> set1DiffSet2 = MarksHelper.contains(allMarks, obj.getAllMarks());
			if (set1DiffSet2 != null && set1DiffSet2.isEmpty())
				if (val.equals(obj.getVal()) && attributeName.equals(obj.getAttributeName()))
					same = true;
		}

		if (same)
			return true;
		return false;
	}

	public ValueFrom getVal() {
		return val;
	}

	public String getAttributeName() {
		return attributeName;
	}

}
