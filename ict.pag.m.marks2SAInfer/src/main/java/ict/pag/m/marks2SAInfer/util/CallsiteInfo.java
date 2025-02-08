package ict.pag.m.marks2SAInfer.util;

public class CallsiteInfo {
	String belongToMthd;
	String lineNumber;
	String callStmt;

	public CallsiteInfo(String belongToMthd, String lineNumber, String callStmt) {
		super();
		this.belongToMthd = belongToMthd;
		this.lineNumber = lineNumber;
		this.callStmt = callStmt;
	}

	public String getBelongToMthd() {
		return belongToMthd;
	}

	public String getLineNumber() {
		return lineNumber;
	}

	public String getCallStmt() {
		return callStmt;
	}

	public String toString() {
		return callStmt + "[" + belongToMthd + "]" + "[" + lineNumber + "]";
	}

}
