package ict.pag.webframework.model.log;

public class CallsiteInfo {
	String belongToMthd;
	String lineNumber;
	String callStmt;

	public CallsiteInfo(String belongToMthd, String lineNumber, String callStmt) {
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

	public static CallsiteInfo calCallsiteInfo(String curr) {

		String[] strs = curr.split("]");
		String line = strs[1].substring(strs[1].lastIndexOf('[') + 1);
		String callsiteStmt = strs[1].substring(0, strs[1].lastIndexOf('['));
		String belongTo = strs[2];

		return new CallsiteInfo(belongTo, line, callsiteStmt);

	}

}
