package ict.pag.webframework.model.marks;

import java.util.HashSet;

import ict.pag.webframework.model.enumeration.MarkScope;
import ict.pag.webframework.model.option.SpecialHelper;

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
	 * @return whether change some element
	 */
	public boolean mergeMarks(EntryMark obj, HashSet<String> ignore_class, HashSet<String> ignore_mtd) {
		boolean ret = false;
		// 1. merge methods marks
		HashSet<String> currMarks_mtd = getAllMarks_methods();
		HashSet<String> comingMarks_mtd = obj.getAllMarks_methods();
		HashSet<String> ignoreMarks_mtd = MarksHelper.mergeMarks(currMarks_mtd, comingMarks_mtd);
		if (ignoreMarks_mtd != null && !ignoreMarks_mtd.isEmpty())
			ignore_mtd.addAll(ignoreMarks_mtd);

		// 2. merge class marks
		HashSet<String> currMarks_class = getAllMarks_class();
		HashSet<String> comingMarks_class = obj.getAllMarks_class();
		HashSet<String> ignoreMarks_class = MarksHelper.mergeMarks(currMarks_class, comingMarks_class);
		if (ignoreMarks_class != null && !ignoreMarks_class.isEmpty())
			ignore_class.addAll(ignoreMarks_class);

		// 3. check some situations
		// 3.1 methods set are same, but one of the class set is empty
		HashSet<String> f1 = MarksHelper.contains(currMarks_mtd, comingMarks_mtd);
		if (f1 != null && f1.isEmpty()) {
			if (currMarks_class.isEmpty() && !comingMarks_class.isEmpty()) {
				// clear
				comingMarks_class.clear();
			} else if (comingMarks_class.isEmpty() && !currMarks_class.isEmpty()) {
				// clear
				currMarks_class.clear();
			}
		}
		// 3.2 if class sets are same, but one of the methods is empty
		HashSet<String> f2 = MarksHelper.contains(currMarks_class, comingMarks_class);
		if (f2 != null && f2.isEmpty()) {
			if (currMarks_mtd.isEmpty() && !comingMarks_mtd.isEmpty()) {
				// add the null-empty to empty set
				currMarks_mtd.addAll(comingMarks_mtd);
			} else if (comingMarks_mtd.isEmpty() && !currMarks_mtd.isEmpty()) {
				// add the null-empty to empty set
				comingMarks_mtd.addAll(currMarks_mtd);
			}
		}

		// 3.3 if the merge operation removed the inheritance in class wrongly, add it
		String clazzStr1 = null;
		for (String m : currMarks_mtd) {
			if (m.startsWith("[inheritance]")) {
				String mtd = m.substring("[inheritance]".length());
				String t = SpecialHelper.reformatSignature(mtd.substring(0, mtd.lastIndexOf('.')));
				clazzStr1 = "[inheritance]" + t.substring(0, t.length() - 1);
				break;
			}
		}
		if (clazzStr1 != null) {
			currMarks_class.add(clazzStr1);
			if (ignore_class != null) {
				ignore_class.remove(clazzStr1);
				if (ignoreMarks_class != null)
					ignoreMarks_class.remove(clazzStr1);
			}
		}
		String clazzStr2 = null;
		for (String m : comingMarks_mtd) {
			if (m.startsWith("[inheritance]")) {
				String mtd = m.substring("[inheritance]".length());
				String t = SpecialHelper.reformatSignature(mtd.substring(0, mtd.lastIndexOf('.')));
				clazzStr2 = "[inheritance]" + t.substring(0, t.length() - 1);
				break;
			}
		}
		if (clazzStr2 != null) {
			comingMarks_class.add(clazzStr2);
			if (ignore_class != null) {
				ignore_class.remove(clazzStr2);
				if (ignoreMarks_class != null)
					ignoreMarks_class.remove(clazzStr2);
			}
		}

		/* if the ignore sets is not empty, that changed */
		if ((ignoreMarks_class != null && !ignoreMarks_class.isEmpty())
				|| (ignoreMarks_mtd != null && !ignoreMarks_mtd.isEmpty()))
			ret = true;

		return ret;
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
