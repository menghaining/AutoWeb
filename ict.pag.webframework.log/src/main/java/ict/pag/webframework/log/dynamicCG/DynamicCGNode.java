package ict.pag.webframework.log.dynamicCG;

import java.util.ArrayList;

public class DynamicCGNode {
	private String stmt;
	private ArrayList<String> sequenceinfo;

	private ArrayList<CallsiteInfo> callsites;
	private ArrayList<DynamicCGNode> otherTargets;

	/* whether this node has caller */
	private DynamicCGNode father;

	private boolean closed = false;/* whether this method is closed */

	private MethodCalledType type;

	public DynamicCGNode(String stmt) {
		this.stmt = stmt;

		this.callsites = new ArrayList<>();
		this.otherTargets = new ArrayList<>();

	}

	public String toString() {
		return stmt;
	}

	public String getStmt() {
		return stmt;
	}

	public void setStmt(String stmt) {
		this.stmt = stmt;
	}

	public ArrayList<String> getSequenceinfo() {
		return sequenceinfo;
	}

	public void setSequenceinfo(ArrayList<String> sequenceinfo) {
		this.sequenceinfo = sequenceinfo;
	}

	public ArrayList<CallsiteInfo> getCallsites() {
		return callsites;
	}

	public void setCallsites(ArrayList<CallsiteInfo> callsites) {
		this.callsites = callsites;
	}

	public ArrayList<DynamicCGNode> getOtherTargets() {
		return otherTargets;
	}

	public void setOtherTargets(ArrayList<DynamicCGNode> otherTargets) {
		this.otherTargets = otherTargets;
	}

	public DynamicCGNode getFather() {
		return father;
	}

	public void setFather(DynamicCGNode father) {
		this.father = father;
	}

	public boolean isClosed() {
		return closed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	public MethodCalledType getType() {
		return type;
	}

	public void setType(MethodCalledType type) {
		this.type = type;
	}

}
