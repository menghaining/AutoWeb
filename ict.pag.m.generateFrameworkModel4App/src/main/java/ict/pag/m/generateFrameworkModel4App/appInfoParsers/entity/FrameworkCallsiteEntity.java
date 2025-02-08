package ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity;

import com.ibm.wala.classLoader.IMethod;

public class FrameworkCallsiteEntity {
	IMethod contextMethod;
	String invoke;

	public FrameworkCallsiteEntity(IMethod contextMethod, String invoke) {
		this.contextMethod = contextMethod;
		this.invoke = invoke;
	}

	public boolean equals(FrameworkCallsiteEntity obj) {
		if (obj.getContextMethod().equals(contextMethod) && obj.getInvoke().equals(invoke))
			return true;
		return false;
	}

	public IMethod getContextMethod() {
		return contextMethod;
	}

	public String getInvoke() {
		return invoke;
	}

	public String toString() {
		return invoke + "[" + contextMethod.getSignature() + "]";
	}

}
