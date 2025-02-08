package ict.pag.m.marks2SAInfer.util.structual;

import java.util.HashSet;

public class frameworkIndirectInvoke {
	str2Set callsite;
	str2Set target;

	public frameworkIndirectInvoke(str2Set callsite, str2Set target) {
		this.callsite = callsite;
		this.target = target;
	}

	public str2Set getCallsite() {
		return callsite;
	}

	public str2Set getTarget() {
		return target;
	}

	public boolean equals(frameworkIndirectInvoke obj) {
		str2Set obj_cs = obj.getCallsite();
		String obj_frameworkCall = obj_cs.getName();
		HashSet<String> obj_framework_ctx = obj_cs.getVals();
		str2Set obj_targets = obj.getTarget();

		if (obj_cs.equals(callsite)) {
			if (!target.equals(obj_targets)) {
				return false;
			}

		} else {
			return false;
		}

		return true;
	}

}
