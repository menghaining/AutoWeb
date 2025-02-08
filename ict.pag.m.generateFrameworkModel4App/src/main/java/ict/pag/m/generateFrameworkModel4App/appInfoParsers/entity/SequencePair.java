package ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity;

import com.ibm.wala.classLoader.IMethod;

public class SequencePair {
	IMethod before;
	IMethod after;

	public SequencePair(IMethod before, IMethod after) {
		this.before = before;
		this.after = after;
	}

	public IMethod getBefore() {
		return before;
	}

	public IMethod getAfter() {
		return after;
	}

	public boolean equals(SequencePair obj) {
		if (obj.getBefore().equals(before) && obj.getAfter().equals(after))
			return true;
		else
			return false;
	}

}
