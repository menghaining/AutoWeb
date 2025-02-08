package ict.pag.webframework.marks;

import java.util.HashSet;
import ict.pag.webframework.enumeration.MarkScope;

/**
 * do not care about MarkScope extends from parent
 */
public class EntryMark extends NormalMark {
	private HashSet<String> annos_class;
	private HashSet<String> xmlEles_class;
	private HashSet<String> inheritance_class;

	private HashSet<String> allMarks_class = new HashSet<>();

	private HashSet<String> annos_mtd;
	private HashSet<String> xmlEles_mtd;
	private HashSet<String> inheritance_mtd;

	private HashSet<String> allMarks_mtd = new HashSet<>();

	public EntryMark(HashSet<String> annos_class, HashSet<String> xmlEles_class, HashSet<String> inheritance_class) {
		super();
		this.annos_class = annos_class;
		this.xmlEles_class = xmlEles_class;
		this.inheritance_class = inheritance_class;
		add2ClassMarksSet();
	}

	public EntryMark(HashSet<String> allMarks_class, HashSet<String> allMarks_mtd) {
		super();
		this.allMarks_class = allMarks_class;
		this.allMarks_mtd = allMarks_mtd;
	}

	private void add2ClassMarksSet() {
		if (annos_class != null)
			add2Set(annos_class, MarkScope.Clazz, "[anno]");
		if (xmlEles_class != null)
			add2Set(xmlEles_class, MarkScope.Clazz, "[xml]");
		if (inheritance_class != null && !inheritance_class.isEmpty())
			add2Set(inheritance_class, MarkScope.Clazz, "[inheritance]");

	}

	private void add2Set(HashSet<String> collection, MarkScope scope, String type) {
		if (scope.equals(MarkScope.Clazz)) {
			for (String c : collection) {
				allMarks_class.add(type + c);
			}
		} else if (scope.equals(MarkScope.Method)) {
			for (String c : collection) {
				allMarks_mtd.add(type + c);
			}
		}
	}

	public boolean isAllEmpty() {
		if ((allMarks_class == null || allMarks_class.isEmpty()) && (allMarks_mtd == null || allMarks_mtd.isEmpty()))
			return true;

		return false;
	}

	/**
	 * merge this and obj to calculate minimal collection</br>
	 * 
	 * @return the changed obj, null iff the two is same
	 */
	public void mergeMarks(EntryMark obj, HashSet<String> ignore_class, HashSet<String> ignore_mtd) {
		// 1. merge class marks
		HashSet<String> currMarks_class = getAllMarks_class();
		HashSet<String> comingMarks_class = obj.getAllMarks_class();
		HashSet<String> ignoreMarks_class = MarksHelper.mergeMarks(currMarks_class, comingMarks_class);
		if (ignoreMarks_class != null && !ignoreMarks_class.isEmpty())
			ignore_class.addAll(ignoreMarks_class);
		// 2. merge methods marks
		HashSet<String> currMarks_mtd = getAllMarks_methods();
		HashSet<String> comingMarks_mtd = obj.getAllMarks_methods();
		HashSet<String> ignoreMarks_mtd = MarksHelper.mergeMarks(currMarks_mtd, comingMarks_mtd);
		if (ignoreMarks_mtd != null && !ignoreMarks_mtd.isEmpty())
			ignore_mtd.addAll(ignoreMarks_mtd);

	}

	public boolean isSame(EntryMark obj) {
		boolean classSame = false;
		boolean mtdSame = false;

		if (getAllMarks_class().isEmpty() && obj.getAllMarks_class().isEmpty()) {
			classSame = true;
		} else {
			HashSet<String> set1DiffSet2 = MarksHelper.contains(getAllMarks_class(), obj.getAllMarks_class());
			if (set1DiffSet2 != null && set1DiffSet2.isEmpty())
				classSame = true;
		}

		if (getAllMarks_methods().isEmpty() && obj.getAllMarks_methods().isEmpty()) {
			mtdSame = true;
		} else {
			HashSet<String> set1DiffSet2 = MarksHelper.contains(getAllMarks_methods(), obj.getAllMarks_methods());
			if (set1DiffSet2 != null && set1DiffSet2.isEmpty())
				mtdSame = true;
		}

		if (classSame && mtdSame)
			return true;
		
		return false;
	}

	/** @return All kinds marks of class */
	public HashSet<String> getAllMarks_class() {
		return this.allMarks_class;
	}

	/** @return All kinds marks of method */
	public HashSet<String> getAllMarks_methods() {
		return this.allMarks_mtd;
	}

	public HashSet<String> getAnnos_class() {
		return annos_class;
	}

	public HashSet<String> getXmlEles_class() {
		return xmlEles_class;
	}

	public HashSet<String> getInheritance_class() {
		return inheritance_class;
	}

	public HashSet<String> getAnnos_mtd() {
		return annos_mtd;
	}

	public void setAnnos_mtd(HashSet<String> annos_mtd) {
		this.annos_mtd = annos_mtd;
		add2Set(annos_mtd, MarkScope.Method, "[anno]");
	}

	public HashSet<String> getXmlEles_mtd() {
		return xmlEles_mtd;
	}

	public void setXmlEles_mtd(HashSet<String> xmlEles_mtd) {
		this.xmlEles_mtd = xmlEles_mtd;
		add2Set(xmlEles_mtd, MarkScope.Method, "[xml]");
	}

	public HashSet<String> getInheritance_mtd() {
		return inheritance_mtd;
	}

	public void setInheritance_mtd(HashSet<String> inheritance_mtd) {
		this.inheritance_mtd = inheritance_mtd;
		add2Set(inheritance_mtd, MarkScope.Method, "[inheritance]");
	}

	public String toString() {
		String ret = "[MARK]";
		ret = ret.concat("\n\t[CLASS]");
		for (String s : allMarks_class) {
			ret = ret.concat(s + ", ");
		}
		ret = ret.substring(0, ret.length() - 2);
		ret = ret.concat("\n\t[METHOD]");
		for (String s : allMarks_mtd) {
			ret = ret.concat(s + ", ");
		}
		ret = ret.substring(0, ret.length() - 2);
		return ret;
	}

}
