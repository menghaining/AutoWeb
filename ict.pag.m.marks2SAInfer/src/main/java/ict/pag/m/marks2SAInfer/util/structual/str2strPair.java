package ict.pag.m.marks2SAInfer.util.structual;

public class str2strPair {
	String first;
	String sec;

	public str2strPair(String first, String sec) {
		this.first = first;
		this.sec = sec;
	}

	public String getFirst() {
		return first;
	}

	public String getSec() {
		return sec;
	}

	public boolean equals(str2strPair obj) {
		if (obj.getFirst().equals(first) && obj.getSec().equals(sec))
			return true;
		return false;
	}

}
