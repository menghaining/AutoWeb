package ict.pag.webframework.model.marks;

import java.util.HashSet;

import ict.pag.webframework.model.enumeration.CallType;
import ict.pag.webframework.model.enumeration.MarkScope;

public class FrmkIndirectCallMark extends NormalMark {
	// Configuration of the method that callStmt belong to
	private HashSet<String> callConfig = new HashSet<>();
	private String callStmt;
	private CallType type;
	/* paramIndex > 0 iff type=CallType.Param; paramIndex starts form 1 */
	private int paramIndex = -1;

	public FrmkIndirectCallMark(CallType type, String callStmt, HashSet<String> targetMarkSet) {
		super(targetMarkSet, MarkScope.Method);
		this.callStmt = callStmt;
		this.type = type;
	}

	public FrmkIndirectCallMark(HashSet<String> callConfigs, CallType type, String callStmt,
			HashSet<String> targetMarkSet) {
		super(targetMarkSet, MarkScope.Method);
		this.callConfig = callConfigs;
		this.callStmt = callStmt;
		this.type = type;
	}

	public FrmkIndirectCallMark(CallType type, String callStmt, int paramIndex) {
		this.type = type;
		this.callStmt = callStmt;
		this.paramIndex = paramIndex;
	}

	public FrmkIndirectCallMark(HashSet<String> callConfigs, CallType type, String callStmt, int paramIndex) {
		this.type = type;
		this.callConfig = callConfigs;
		this.callStmt = callStmt;
		this.paramIndex = paramIndex;
	}

	public HashSet<String> getCallConfig() {
		return callConfig;
	}

	public String getCallStmt() {
		return callStmt;
	}

	public CallType getType() {
		return type;
	}

	public int getParamIndex() {
		return paramIndex;
	}

}
