package ict.pag.m.instrumentation.Entity;

public class InstrumentationPair {

	private String belong2MethodSignature;
	private String mthdSignature;
	/**
	 * isInvoke means whether instrument at invoke position
	 */
	private boolean isInvoke = false;

	public InstrumentationPair(String method1, String method2, boolean isInvoke) {
		this.belong2MethodSignature = method1;
		this.mthdSignature = method2;
		this.isInvoke = isInvoke;
	}
	
	public InstrumentationPair(String method1, String method2) {
		this.belong2MethodSignature = method1;
		this.mthdSignature = method2;
	}

	public String getBelong2MethodSignature() {
		return belong2MethodSignature;
	}

	public void setBelong2MethodSignature(String belong2MethodSignature) {
		this.belong2MethodSignature = belong2MethodSignature;
	}

	public String getMthdSignature() {
		return mthdSignature;
	}

	public void setMthdSignature(String mthdSignature) {
		this.mthdSignature = mthdSignature;
	}

	public boolean isInvoke() {
		return isInvoke;
	}

	public void setInvoke(boolean isInvoke) {
		this.isInvoke = isInvoke;
	}


}
