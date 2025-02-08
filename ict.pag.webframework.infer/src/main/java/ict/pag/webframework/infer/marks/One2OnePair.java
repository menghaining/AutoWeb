package ict.pag.webframework.infer.marks;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

public class One2OnePair {
	String pre;
	String post;

	List<String> others = new ArrayList<>();

	public One2OnePair(String pre, String post) {
		this.pre = pre;
		this.post = post;
	}

	public One2OnePair(String pre, String post, List<String> others) {
		this.pre = pre;
		this.post = post;
		this.others = others;
	}

	public String getPre() {
		return pre;
	}

	public String getPost() {
		return post;
	}

	public boolean isSame(One2OnePair other) {
		if (other == null)
			return false;
		if (other.pre == null) {
			if (this.pre == null && other.post.equals(this.post))
				return true;
		} else {
			if (other.pre.equals(this.pre) && other.post.equals(this.post))
				return true;
			else
				return false;
		}
		return false;
	}

	public static boolean setContains(Set<One2OnePair> set, One2OnePair one) {
		for (One2OnePair pair : set) {
			if (pair.isSame(one))
				return true;
		}
		return false;
	}

	public void addOtherInfos(List<String> o) {
		this.others = o;
	}

	public List<String> getOtherInfos() {
		return this.others;
	}
}
