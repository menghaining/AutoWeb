package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ContainerContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * 0-X-CFA Call graph builder which analyzes calls to "container methods" in a
 * context which is defined by the receiver instance.
 */
public class EnhanceZeroXContainerCFABuilder extends EnhanceZeroXCFABuilder {

	/**
	 * @param cha                   governing class hierarchy
	 * @param options               call graph construction options
	 * @param appContextSelector    application-specific logic to choose contexts
	 * @param appContextInterpreter application-specific logic to interpret a method
	 *                              in context
	 * @throws IllegalArgumentException if options is null
	 */
	public EnhanceZeroXContainerCFABuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache,
			ContextSelector appContextSelector, SSAContextInterpreter appContextInterpreter, int instancePolicy) {

		super(Language.JAVA, cha, options, cache, appContextSelector, appContextInterpreter, instancePolicy);

		ContextSelector CCS = makeContainerContextSelector(cha, (ZeroXInstanceKeys) getInstanceKeys());
		DelegatingContextSelector DCS = new DelegatingContextSelector(CCS, contextSelector);
		setContextSelector(DCS);
	}

	/**
	 * @return an object which creates contexts for call graph nodes based on the
	 *         container disambiguation policy
	 */
	protected ContextSelector makeContainerContextSelector(IClassHierarchy cha, ZeroXInstanceKeys keys) {
		return new ContainerContextSelector(cha, keys);
	}

}
