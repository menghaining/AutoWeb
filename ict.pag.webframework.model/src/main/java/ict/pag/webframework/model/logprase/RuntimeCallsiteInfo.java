package ict.pag.webframework.model.logprase;

public class RuntimeCallsiteInfo {
	/* type */
	CallsiteKind kind;

	/* declared information */
	String belongToMthd;
	String lineNumber;
	String callStmt;

	/* runtime information */
	String baseRTType;
	// only for returnSite
	String returnRTType;

	public RuntimeCallsiteInfo(CallsiteKind kind, String belongToMthd, String lineNumber, String callStmt,
			String baseRTType, String returnRTType) {
		this.kind = kind;

		this.belongToMthd = belongToMthd;
		this.lineNumber = lineNumber;
		this.callStmt = callStmt;

		this.baseRTType = baseRTType;
		this.returnRTType = returnRTType;
	}

	public static RuntimeCallsiteInfo calCallsiteInfo(String line) {

		String[] strs = line.split("]");

		CallsiteKind k = CallsiteKind.callsite;
		String k1 = strs[0].substring(1);
		if (k1.equals("returnSite"))
			k = CallsiteKind.returnSite;

		String lineNumber = strs[1].substring(strs[1].lastIndexOf('[') + 1);
		String callsiteStmt = strs[1].substring(0, strs[1].lastIndexOf('['));
		String belongTo = strs[2].substring(0, strs[2].lastIndexOf('['));

		String RTBase = strs[3];
		String RTReturn = null;
		if (k.equals(CallsiteKind.returnSite)) {
			RTBase = strs[3].substring(0, strs[3].lastIndexOf('['));
			RTReturn = strs[4];
		}

		return new RuntimeCallsiteInfo(k, belongTo, lineNumber, callsiteStmt, RTBase, RTReturn);
	}

	public CallsiteKind getKind() {
		return kind;
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

	public String getBaseRTType() {
		return baseRTType;
	}

	public String getReturnRTType() {
		return returnRTType;
	}

}
