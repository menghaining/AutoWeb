package ict.pag.m.marks2SAInfer.util.structual;

import ict.pag.m.frameworkInfoUtil.infoEntity.marksType;

public class markkind2String {
	marksType kind;
	String value;

	public markkind2String(marksType kind, String value) {
		this.kind = kind;
		this.value = value;
	}

	public boolean equals(markkind2String obj) {
		marksType k2 = obj.getKind();
		String v2 = obj.getValue();
		if (kind == null || value == null)
			return false;

		if (kind.equals(k2) && value.equals(v2))
			return true;

		return false;
	}

	public marksType getKind() {
		return kind;
	}

	public void setKind(marksType kind) {
		this.kind = kind;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
