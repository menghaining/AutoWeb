package ict.pag.webframework.log.dynamicCG;

import java.util.ArrayList;

public class CallsiteInfo {
	private String stmt;
	private ArrayList<DynamicCGNode> actual_targets;
	private CallType type;

	private boolean closed = false;

	public CallsiteInfo(String stmt) {
		this.stmt = stmt;
		this.actual_targets = new ArrayList<>();
	}

	public String getStmt() {
		return stmt;
	}

	public ArrayList<DynamicCGNode> getActual_targets() {
		return actual_targets;
	}

	public void setActual_targets(ArrayList<DynamicCGNode> actual_targets) {
		this.actual_targets = actual_targets;
	}

	public CallType getType() {
		return type;
	}

	public void setType(CallType type) {
		this.type = type;
	}

	public boolean isClosed() {
		return closed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

}
