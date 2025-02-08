package com.ibm.wala.ipa.callgraph.propagation.scg;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;

public class SCGSignatureKey implements PointerKey {

	private final String sig;

	SCGSignatureKey(String sig) {
		this.sig = sig;
	}

	@Override
	public int hashCode() {
		return 131 * sig.hashCode();
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 == null) {
			return false;
		}
		if (arg0.getClass().equals(getClass())) {
			SCGSignatureKey other = (SCGSignatureKey) arg0;
			return sig.equals(other.sig);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return "RTAKey:" + sig;
	}

	public IClass getTypeFilter() throws UnimplementedError {
		Assertions.UNREACHABLE();
		return null;
	}
}
