package ict.pag.m.marks2SAInfer.summarizeModels;

import java.util.HashSet;

import ict.pag.m.marks2SAInfer.util.CollectionUtil;

public class CallsiteM2targetM {
	String call;
	String target;

	HashSet<String> callsiteMarks;
	HashSet<String> targetMarks;

	public CallsiteM2targetM(String call, String target, HashSet<String> callsiteMarks, HashSet<String> targetMarks) {
		this.call = call;
		this.target = target;
		this.callsiteMarks = callsiteMarks;
		this.targetMarks = targetMarks;
	}

	public String getCall() {
		return call;
	}

	public String getTarget() {
		return target;
	}

	public HashSet<String> getCallsiteMarks() {
		return callsiteMarks;
	}

	public HashSet<String> getTargetMarks() {
		return targetMarks;
	}

	public boolean equals(CallsiteM2targetM obj) {
		if (obj == this)
			return true;

		if (obj.getCall().equals(call) && obj.getTarget().equals(target)) {
			if (CollectionUtil.isSame(obj.getCallsiteMarks(), callsiteMarks)) {
				if (CollectionUtil.isSame(obj.getTargetMarks(), targetMarks)) {
					return true;
				}
			}
		}

		return false;

	}

}
