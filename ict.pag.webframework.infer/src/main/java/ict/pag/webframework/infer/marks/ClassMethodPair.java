package ict.pag.webframework.infer.marks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ClassMethodPair {
	private HashSet<String> allMarks_class = new HashSet<>();
	private HashSet<String> allMarks_mtd = new HashSet<>();

	private List<String> others = new ArrayList<>();

	public ClassMethodPair() {

	}

	public ClassMethodPair(HashSet<String> allMarks_class, HashSet<String> allMarks_mtd) {
		this.allMarks_class = allMarks_class;
		this.allMarks_mtd = allMarks_mtd;
	}

	public HashSet<String> getAllMarks_class() {
		return allMarks_class;
	}

	public void setAllMarks_class(HashSet<String> allMarks_class) {
		this.allMarks_class = allMarks_class;
	}

	public HashSet<String> getAllMarks_mtd() {
		return allMarks_mtd;
	}

	public void setAllMarks_mtd(HashSet<String> allMarks_mtd) {
		this.allMarks_mtd = allMarks_mtd;
	}

	public boolean isAllEmpty() {
		return allMarks_class.isEmpty() && allMarks_mtd.isEmpty();
	}

	public boolean isSame(ClassMethodPair ele) {
		if (allMarks_class.containsAll(ele.getAllMarks_class()) && ele.getAllMarks_class().containsAll(allMarks_class)
				&& allMarks_mtd.containsAll(ele.getAllMarks_mtd()) && ele.getAllMarks_mtd().containsAll(allMarks_mtd))
			return true;
		return false;
	}

	public void addOtherInfos(List<String> o) {
		this.others = o;
	}

	public List<String> getOtherInfos() {
		return this.others;
	}
}
