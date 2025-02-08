package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.analysis.reflection.ReflectionContextInterpreter;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultPointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultSSAInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DelegatingSSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFAContextSelector;

/**
 * nCFA Call graph builder. Note that by default, this builder uses a
 * {@link ClassBasedInstanceKeys} heap model.
 */
public class EnhanceNCFABuilder extends EnhanceCallgraphBuilder {

	public EnhanceNCFABuilder(int n, IMethod abstractRootMethod, AnalysisOptions options, IAnalysisCacheView cache,
			ContextSelector appContextSelector, SSAContextInterpreter appContextInterpreter) {

		super(abstractRootMethod, options, cache, new DefaultPointerKeyFactory());
		if (options == null) {
			throw new IllegalArgumentException("options is null");
		}

		setInstanceKeys(new ClassBasedInstanceKeys(options, cha));

		ContextSelector def = new DefaultContextSelector(options, cha);
		ContextSelector contextSelector = appContextSelector == null ? def
				: new DelegatingContextSelector(appContextSelector, def);
		contextSelector = new nCFAContextSelector(n, contextSelector);
		setContextSelector(contextSelector);

		SSAContextInterpreter defI = new DefaultSSAInterpreter(options, cache);
		defI = new DelegatingSSAContextInterpreter(
				ReflectionContextInterpreter.createReflectionContextInterpreter(cha, options, getAnalysisCache()),
				defI);
		SSAContextInterpreter contextInterpreter = appContextInterpreter == null ? defI
				: new DelegatingSSAContextInterpreter(appContextInterpreter, defI);
		setContextInterpreter(contextInterpreter);
	}

}
