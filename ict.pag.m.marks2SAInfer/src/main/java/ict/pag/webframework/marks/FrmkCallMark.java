package ict.pag.webframework.marks;

import java.util.HashSet;

import ict.pag.webframework.enumeration.CallType;
import ict.pag.webframework.enumeration.MarkScope;

public class FrmkCallMark extends NormalMark {
	private CallType type;
	private String callStmt;

	/* only type=CallType.Attribute, targetMarkSet!=null */
//	private HashSet<String> targetMarkSet;
	/* paramIndex > 0 iff type=CallType.Param; paramIndex starts form 1 */
	private int paramIndex = -1;

	public FrmkCallMark(CallType type, String callStmt, HashSet<String> targetMarkSet) {
		super(targetMarkSet, MarkScope.Method);
		this.type = type;
		this.callStmt = callStmt;
	}

	public FrmkCallMark(CallType type, String callStmt, int paramIndex) {
		this.type = type;
		this.callStmt = callStmt;
		this.paramIndex = paramIndex;
	}

	public CallType getType() {
		return type;
	}

	public String getCallStmt() {
		return callStmt;
	}

	public int getParamIndex() {
		return paramIndex;
	}

}
