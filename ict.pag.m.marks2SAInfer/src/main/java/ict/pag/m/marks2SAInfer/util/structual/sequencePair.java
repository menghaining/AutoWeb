package ict.pag.m.marks2SAInfer.util.structual;

public class sequencePair {
	private String pre;
	private String post;

	public sequencePair(String pre, String post) {
		super();
		this.pre = pre;
		this.post = post;
	}

	public String getPre() {
		return pre;
	}

	public String getPost() {
		return post;
	}

	public String toString() {
		return "<" + pre + ", " + post + ">";
	}

}
