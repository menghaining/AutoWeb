package com.ibm.wala.ipa.callgraph.propagation.scg;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph.ExplicitNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * simple call graph intraproduce analysis and pass parameter
 */
public class BasicSCGBuilder extends AbstractSCGBuilder {

	public BasicSCGBuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache,
			ContextSelector contextSelector, SSAContextInterpreter contextInterpreter) {
		super(cha, options, cache, contextSelector, contextInterpreter);
	}

	@Override
	protected void updateSetsForNewClass(IClass klass, InstanceKey iKey, CGNode node, NewSiteReference n) {

		// set up the selector map to record each method that class implements
		registerImplementedMethods(klass, iKey);

		for (IClass c : klass.getAllImplementedInterfaces()) {
			registerImplementedMethods(c, iKey);
		}
		klass = klass.getSuperclass();
		while (klass != null) {
			registerImplementedMethods(klass, iKey);
			klass = klass.getSuperclass();
		}
	}

	private void registerImplementedMethods(IClass declarer, InstanceKey iKey) {
		if (DEBUG) {
			System.err.println(("registerImplementedMethods: " + declarer + ' ' + iKey));
		}
		for (IMethod M : declarer.getDeclaredMethods()) {
			PointerKey sKey = getKeyForSig(M.getSignature());
			if (DEBUG) {
				System.err.println(("Add constraint: " + M.getSignature() + " U= " + iKey.getConcreteType()));
			}
			system.newConstraint(sKey, iKey);
		}
	}

	@Override
	protected PointerKey getKeyForSite(CallSiteReference site) {
		return new SCGSignatureKey(site.getDeclaredTarget().getSignature());
	}

	@Override
	protected SCGSignatureKey getKeyForSig(String sig) {
		return new SCGSignatureKey(sig);
	}

	private final class DispatchOperator extends UnaryOperator<PointsToSetVariable> {
		private final CallSiteReference site;

		private final ExplicitCallGraph.ExplicitNode caller;

		DispatchOperator(CallSiteReference site, ExplicitNode caller) {
			this.site = site;
			this.caller = caller;
		}

		@SuppressWarnings("unused")
		@Override
		public byte evaluate(PointsToSetVariable lhs, PointsToSetVariable rhs) {
			boolean changed = false;
			for (CGNode n : BasicSCGBuilder.this.getTargetByIntraDF(caller, site)) {
				if (processResolvedCall(caller, site, n))
					changed = true;
			}
			if (changed)
				return CHANGED;
			else
				return NOT_CHANGED;
		}

		@Override
		public String toString() {
			return "Dispatch";
		}

		@Override
		public int hashCode() {
			return caller.hashCode() + 8707 * site.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof DispatchOperator) {
				DispatchOperator other = (DispatchOperator) o;
				return caller.equals(other.caller) && site.equals(other.site);
			} else {
				return false;
			}
		}
	}

	@Override
	protected UnaryOperator<PointsToSetVariable> makeDispatchOperator(CallSiteReference site, CGNode node) {
		return new DispatchOperator(site, (ExplicitNode) node);
	}
}
