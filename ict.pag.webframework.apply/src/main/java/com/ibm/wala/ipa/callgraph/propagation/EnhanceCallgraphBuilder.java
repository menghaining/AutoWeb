package com.ibm.wala.ipa.callgraph.propagation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.AbstractRootMethod;
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.warnings.Warnings;

import ict.pag.webframework.model.graph.UnreachableEntryPoints;

public class EnhanceCallgraphBuilder extends SSAPropagationCallGraphBuilder {

	protected EnhanceCallgraphBuilder(IMethod abstractRootMethod, AnalysisOptions options, IAnalysisCacheView cache,
			PointerKeyFactory pointerKeyFactory) {
		super(abstractRootMethod, options, cache, pointerKeyFactory);
	}

	protected BeanConstraintVisitor makeBeanVisitor(CGNode node) {
		return new BeanConstraintVisitor(this, node);

	}

	/**
	 * Add pointer flow constraints based on instructions in a given node
	 */
	@Override
	protected void addNodeInstructionConstraints(CGNode node, IProgressMonitor monitor) throws CancelException {
		this.monitor = monitor;
		BeanConstraintVisitor v = makeBeanVisitor(node);
		IRView ir = v.getIR();
		for (ISSABasicBlock sbb : Iterator2Iterable.make(ir.getBlocks())) {
			BasicBlock b = (BasicBlock) sbb;
			addBlockInstructionConstraints(node, ir, b, v, monitor);
			if (wasChanged(node)) {
				return;
			}
		}
	}

	protected static class BeanConstraintVisitor extends ConstraintVisitor {
		protected IRView getIR() {
			return ir;
		}

		public BeanConstraintVisitor(SSAPropagationCallGraphBuilder builder, CGNode node) {
			super(builder, node);
		}
	}

	/*
	 * @see
	 * com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder#makeSolver
	 * ()
	 */
	@Override
	protected IPointsToSolver makeSolver() {
		return new EnhanceSolver(system, this);
	}

	protected void handleUnreachable() {
		Set<IMethod> visitedMethods = new HashSet<>();
		alreadyVisited.forEach(node -> {
			visitedMethods.add(node.getMethod());
		});
		List<Entrypoint> roots = UnreachableEntryPoints.getInstance(cha).getUnreachableRoots(visitedMethods);
		if (!roots.isEmpty()) {
			roots.forEach(E -> {
				SSAAbstractInvokeInstruction call = E
						.addCall((AbstractRootMethod) callGraph.getFakeRootNode().getMethod());

				if (call == null) {
					Warnings.add(EntrypointResolutionWarning.create(E));
				} else {
					entrypointCallSites.add(call.getCallSite());
				}
			});
			markChanged(callGraph.getFakeRootNode());
		}
	}
}
