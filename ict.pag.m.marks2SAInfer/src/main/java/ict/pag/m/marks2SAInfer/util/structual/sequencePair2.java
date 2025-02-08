package ict.pag.m.marks2SAInfer.util.structual;

import ict.pag.m.marks2SAInfer.util.reducedSetCollection;

public class sequencePair2 {
	private reducedSetCollection pre;
	private reducedSetCollection post;

	public sequencePair2(reducedSetCollection pre, reducedSetCollection post) {
		this.pre = pre;
		this.post = post;
	}

	public reducedSetCollection getPre() {
		return pre;
	}

	public reducedSetCollection getPost() {
		return post;
	}

	public String toString() {
		return "<" + pre + ", " + post + ">";
	}

	public boolean equals(sequencePair2 p) {
		if (reducedSetCollection.elementEquals(this.pre, p.getPre())
				&& reducedSetCollection.elementEquals(this.post, p.getPost()))
			return true;

		return false;
	}
}
