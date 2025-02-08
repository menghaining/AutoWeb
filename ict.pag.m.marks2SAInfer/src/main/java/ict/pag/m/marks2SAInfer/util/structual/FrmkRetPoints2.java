package ict.pag.m.marks2SAInfer.util.structual;

public class FrmkRetPoints2 {
	private String frmkCall;
	private int paramPosition;
	private FrmkCallParamType type;

	public FrmkRetPoints2(String frmkCall, int paramPosition, FrmkCallParamType type) {
		this.frmkCall = frmkCall;
		this.paramPosition = paramPosition;
		this.type = type;
	}

	public String getFrmkCall() {
		return frmkCall;
	}

	public int getParamPosition() {
		return paramPosition;
	}

	public FrmkCallParamType getType() {
		return type;
	}

}
