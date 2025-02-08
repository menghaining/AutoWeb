package ict.pag.m.frameworkInfoUtil.infoEntity;

import java.util.ArrayList;
import java.util.List;

public class infoUnit {
	

	private int maxFieldDepth = 3;

	private int len = 0;

	private marksType kind;
	private marksLevel level;

	/**
	 * <base, fields> </br>
	 * for anno, base means the entity that has annos</br>
	 * for inheritance, base means the class name that extends/implements others for
	 * xml, base means the entity that contract with src code, e.g.
	 * package/class/method name
	 */
	private String base;

	/** means this base contains all marks and marks-values */
	private List<infoPair> fields = new ArrayList<>();

	public infoUnit(marksType kind, marksLevel level, String base, int len, List<infoPair> fields) {
		this.kind = kind;
		this.level = level;
		this.base = base;
		this.len = len;
		this.fields = fields;
	}

	public infoUnit(marksType kind, marksLevel level, String base, List<infoPair> fields) {
		this.kind = kind;
		this.level = level;
		this.base = base;
		this.fields = fields;
	}

	public String toString() {
		return "{" + " kind=" + kind + " level=" + level + " depth=" + len + " base=" + base + " fields=" + fields
				+ "}";
	}

	public String getBase() {
		return base;
	}

	public List<infoPair> getFields() {
		return fields;
	}
	
	public marksType getKind() {
		return kind;
	}

	public marksLevel getLevel() {
		return level;
	}

}
