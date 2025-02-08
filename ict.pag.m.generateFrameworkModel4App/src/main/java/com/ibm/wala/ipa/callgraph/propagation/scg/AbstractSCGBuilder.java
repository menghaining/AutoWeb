package com.ibm.wala.ipa.callgraph.propagation.scg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import com.ibm.wala.analysis.reflection.ReflectionContextInterpreter;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.impl.FakeRootMethod;
import com.ibm.wala.ipa.callgraph.impl.FakeWorldClinitMethod;
import com.ibm.wala.ipa.callgraph.propagation.ClassBasedInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.IPointsToSolver;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.PropagationSystem;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.StandardSolver;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultPointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultSSAInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DelegatingSSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.rta.DelegatingExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.rta.TypeBasedPointerAnalysis;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.HashSetMultiMap;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.MultiMap;
import com.ibm.wala.util.collections.Pair;

/** Abstract superclass of various SCG flavors */
public abstract class AbstractSCGBuilder extends PropagationCallGraphBuilder {
	private final MultiMap<Pair<CGNode, Integer>, IClass> parasMap = HashSetMultiMap.make();

	protected static final int DEBUG_LEVEL = 0;

	protected static final boolean DEBUG = (DEBUG_LEVEL > 0);

	private static final int VERBOSE_INTERVAL = 10000;

	private static final int PERIODIC_MAINTAIN_INTERVAL = 10000;

	/** Should we change calls to clone() to assignments? */
	protected final boolean clone2Assign = true;

	/** set of classes whose clinit are processed */
	protected final Set<IClass> clinitProcessed = HashSetFactory.make();

	/** set of classes (IClass) discovered to be allocated */
	protected final HashSet<IClass> allocatedClasses = HashSetFactory.make();

	/**
	 * set of class names that are implicitly pre-allocated Note: for performance
	 * reasons make sure java.lang.Object comes first
	 */
	private static final TypeReference[] PRE_ALLOC = {
			TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/Object"),
			TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/ArithmeticException"),
			TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/ArrayStoreException"),
			TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/ClassCastException"),
			TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/ClassNotFoundException"),
			TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/IndexOutOfBoundsException"),
			TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/NegativeArraySizeException"),
			TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/ExceptionInInitializerError"),
			TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/lang/NullPointerException") };

	protected AbstractSCGBuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache,
			ContextSelector appContextSelector, SSAContextInterpreter appContextInterpreter) {
		super(Language.JAVA.getFakeRootMethod(cha, options, cache), options, cache, new DefaultPointerKeyFactory());
		setInstanceKeys(new ClassBasedInstanceKeys(options, cha));
		setContextSelector(makeContextSelector(appContextSelector));
		setContextInterpreter(makeContextInterpreter(appContextInterpreter));
	}

	protected RTAContextInterpreter getRTAContextInterpreter() {
		return getContextInterpreter();
	}

	/**
	 * Visit all instructions in a node, and add dataflow constraints induced by
	 * each statement relevat to SCG
	 */
	@Override
	protected boolean addConstraintsFromNode(CGNode node, IProgressMonitor monitor) {

		if (haveAlreadyVisited(node)) {
			return false;
		} else {
			markAlreadyVisited(node);
		}
		if (DEBUG) {
			System.err.println(("\n\nAdd constraints from node " + node));
		}

		// add all relevant constraints
		addNewConstraints(node);
		addCallConstraints(node);
		addFieldConstraints(node);
		// conservatively assume something changed.
		return true;
	}

	/** Add a constraint for each allocate */
	private void addNewConstraints(CGNode node) {
		IR ir = node.getIR();
		if (ir == null)
			return;
		for (NewSiteReference n : Iterator2Iterable.make(ir.iterateNewSites())) {
			visitNew(node, n);
		}
	}

	/** Add a constraint for each invoke */
	private void addCallConstraints(CGNode node) {
		for (CallSiteReference c : Iterator2Iterable.make(getRTAContextInterpreter().iterateCallSites(node))) {
			visitInvoke(node, c);
		}
	}

	/** Handle accesses to static fields */
	private void addFieldConstraints(CGNode node) {
		for (FieldReference f : Iterator2Iterable.make(getRTAContextInterpreter().iterateFieldsRead(node))) {
			processFieldAccess(f);
		}
		for (FieldReference f : Iterator2Iterable.make(getRTAContextInterpreter().iterateFieldsWritten(node))) {
			processFieldAccess(f);
		}
	}

	/**
	 * Is s is a getstatic or putstatic, then potentially add the relevant
	 * &lt;clinit&gt; to the newMethod set.
	 */
	private void processFieldAccess(FieldReference f) {
		if (DEBUG) {
			System.err.println(("processFieldAccess: " + f));
		}
		TypeReference t = f.getDeclaringClass();
		IClass klass = getClassHierarchy().lookupClass(t);
		if (klass == null) {
		} else {
			processClassInitializer(klass);
		}
	}

	protected void processClassInitializer(IClass klass) {

		if (!clinitProcessed.add(klass)) {
			return;
		}

		if (klass.getClassInitializer() != null) {
			if (DEBUG) {
				System.err.println(("process class initializer for " + klass));
			}

			// add an invocation from the fake root method to the <clinit>
			FakeWorldClinitMethod fakeWorldClinitMethod = (FakeWorldClinitMethod) callGraph.getFakeWorldClinitNode()
					.getMethod();
			MethodReference m = klass.getClassInitializer().getReference();
			CallSiteReference site = CallSiteReference.make(1, m, IInvokeInstruction.Dispatch.STATIC);
			IMethod targetMethod = options.getMethodTargetSelector().getCalleeTarget(callGraph.getFakeRootNode(), site,
					null);
			if (targetMethod != null) {
				CGNode target = callGraph.getNode(targetMethod, Everywhere.EVERYWHERE);
				if (target == null) {
					SSAAbstractInvokeInstruction s = fakeWorldClinitMethod.addInvocation(null, site);
					try {
						target = callGraph.findOrCreateNode(targetMethod, Everywhere.EVERYWHERE);
						processResolvedCall(callGraph.getFakeWorldClinitNode(), s.getCallSite(), target);
					} catch (CancelException e) {
						if (DEBUG) {
							System.err
									.println("Could not add node for class initializer: " + targetMethod.getSignature()
											+ " due to constraints on the maximum number of nodes in the call graph.");
							return;
						}
					}
				}
			}
		}

		klass = klass.getSuperclass();
		if (klass != null && !clinitProcessed.contains(klass))
			processClassInitializer(klass);
	}

	/**
	 * Add a constraint for a call instruction
	 *
	 * @throws IllegalArgumentException if site is null
	 */
	public void visitInvoke(CGNode node, CallSiteReference site) {

		if (site == null) {
			throw new IllegalArgumentException("site is null");
		}
		if (DEBUG) {
			System.err.println(("visitInvoke: " + site));
		}

		// if non-virtual, add callgraph edges directly
		IInvokeInstruction.IDispatch code = site.getInvocationCode();

		if (code == IInvokeInstruction.Dispatch.STATIC) {
			CGNode n = getTargetForCall(node, site, null, null);
			if (n != null) {
				processResolvedCall(node, site, n);

				processClassInitializer(cha.lookupClass(site.getDeclaredTarget().getDeclaringClass()));
			}
		} else {
			PointerKey lhs = getKeyForSite(site);
			PointerKey rhs = getKeyForSig(node.getMethod().getSignature());
			if (lhs == null || rhs == null) {
				return;
			}

			if (DEBUG) {
				System.err.println(("Add side effect, dispatch to " + site));
			}
			UnaryOperator<PointsToSetVariable> dispatchOperator = makeDispatchOperator(site, node);
			system.newConstraint(lhs, dispatchOperator, rhs);
		}
	}

	/**
	 * find concrete allocsite in the caller by UD
	 */
	protected ArrayList<CGNode> getTargetByIntraDF(CGNode caller, CallSiteReference site) {
		ArrayList<CGNode> result = new ArrayList<CGNode>();
		boolean hasLocalDef = false;
		for (IClass clazz : getLocalAllocOfInvoke(caller, site)) {
			hasLocalDef = true;
			IMethod targetMethod = options.getMethodTargetSelector().getCalleeTarget(caller, site, clazz);
			if (targetMethod == null)
				continue;
			try {
				result.add(getCallGraph().findOrCreateNode(targetMethod, Everywhere.EVERYWHERE));
			} catch (CancelException e) {
				e.printStackTrace();
			}
		}
		if (!hasLocalDef) {
			CGNode n = getTargetByDeclClass(caller, site);
			if (n != null)
				result.add(n);
		}
		return result;
	}

	private CGNode getTargetByDeclClass(CGNode caller, CallSiteReference site) {
		IClass targetClass = cha.lookupClass(site.getDeclaredTarget().getDeclaringClass());
		if (targetClass == null)
			return null;
		IMethod targetMethod = options.getMethodTargetSelector().getCalleeTarget(caller, site, targetClass);
		if (targetMethod != null && !targetMethod.isAbstract()) {
			try {
				return getCallGraph().findOrCreateNode(targetMethod, Everywhere.EVERYWHERE);
			} catch (CancelException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private Set<IClass> getLocalAllocOfInvoke(CGNode caller, CallSiteReference site) {
		Set<IClass> result = new HashSet<IClass>();
		IR ir = caller.getIR();
		Set<Integer> vns = new HashSet<Integer>();
		try {
			for (SSAAbstractInvokeInstruction invoke : ir.getCalls(site)) {
				assert !invoke.isStatic();
				vns.add(invoke.getUse(0));
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

		result.addAll(getLocalAlloc(caller, vns));
		return result;
	}

	private Set<IClass> getLocalAlloc(CGNode caller, Set<Integer> vns) {
		IMethod callerM = caller.getMethod();
		Set<IClass> result = new HashSet<IClass>();
		if (vns == null || vns.isEmpty())
			return result;
		DefUse du = caller.getDU();
		Queue<Integer> worklist = new LinkedList<Integer>();
		worklist.addAll(vns);
		Set<Integer> visited = new HashSet<Integer>();
		while (!worklist.isEmpty()) {
			int var = worklist.poll();
			visited.add(var);
			if (var <= callerM.getNumberOfParameters()) {
				Pair<CGNode, Integer> tmpPair = Pair.make(caller, var);
				if (parasMap.containsKey(tmpPair)) {
					result.addAll(parasMap.get(tmpPair));
				}
				continue;
			}
			SSAInstruction defInst = du.getDef(var);
			if (defInst instanceof SSAGetInstruction) {
				IClass clazz = cha.lookupClass(((SSAGetInstruction) defInst).getDeclaredField().getDeclaringClass());
				if (clazz != null)
					result.add(clazz);
			} else if (defInst instanceof SSANewInstruction) {
				IClass clazz = options.getClassTargetSelector().getAllocatedTarget(caller,
						((SSANewInstruction) defInst).getNewSite());
				if (clazz != null)
					result.add(clazz);
			} else if (defInst instanceof SSACheckCastInstruction) {
				int use = ((SSACheckCastInstruction) defInst).getUse(0);
				if (!visited.contains(use))
					worklist.add(use);
			} else if (defInst instanceof SSAPhiInstruction) {
				SSAPhiInstruction phi = (SSAPhiInstruction) defInst;
				for (int i = 0; i < phi.getNumberOfUses(); i++) {
					int use = phi.getUse(i);
					if (!visited.contains(use))
						worklist.add(use);
				}
			}
		}
		return result;
	}

	protected abstract UnaryOperator<PointsToSetVariable> makeDispatchOperator(CallSiteReference site, CGNode node);

	protected abstract PointerKey getKeyForSite(CallSiteReference site);

	protected abstract PointerKey getKeyForSig(String sig);

	/**
	 * Add constraints for a call site after we have computed a reachable target for
	 * the dispatch
	 *
	 * <p>
	 * Side effect: add edge to the call graph.
	 */
	@SuppressWarnings("deprecation")
	boolean processResolvedCall(CGNode caller, CallSiteReference site, CGNode target) {

		if (DEBUG) {
			System.err.println(("processResolvedCall: " + caller + " ," + site + " , " + target));
		}
		caller.addTarget(site, target);

		if (caller.equals(callGraph.getFakeRootNode())) {
			if (entrypointCallSites.contains(site)) {
				callGraph.registerEntrypoint(target);
			}
		}

		if (!haveAlreadyVisited(target)) {
			markDiscovered(target);
		}

		return handlePara(caller, site, target);
	}

	/**
	 * if allocSites of parameters of callee can be found in body of caller or
	 * parameter of caller, then save the info.
	 * 
	 * @param caller
	 * @param site
	 * @param target
	 * @return
	 */
	boolean handlePara(CGNode caller, CallSiteReference site, CGNode target) {
		boolean changed = false;
		IR ir = caller.getIR();
		for (SSAAbstractInvokeInstruction invoke : ir.getCalls(site)) {
			for (int i = 0; i < invoke.getNumberOfUses(); i++) {
				Set<Integer> vns = new HashSet<Integer>();
				vns.add(invoke.getUse(i));
				Set<IClass> tmpSet = getLocalAlloc(caller, vns);
				if (!tmpSet.isEmpty()) {
					for (IClass clazz : tmpSet) {
						if (parasMap.put(Pair.make(target, i + 1), clazz))
							changed = true;
					}
				}
			}
		}
		return changed;
	}

	/**
	 * Add a constraint for an allocate
	 *
	 * @throws IllegalArgumentException if newSite is null
	 */
	public void visitNew(CGNode node, NewSiteReference newSite) {

		if (newSite == null) {
			throw new IllegalArgumentException("newSite is null");
		}
		if (DEBUG) {
			System.err.println(("visitNew: " + newSite));
		}
		InstanceKey iKey = getInstanceKeyForAllocation(node, newSite);
		if (iKey == null) {
			// something went wrong. I hope someone raised a warning.
			return;
		}
		IClass klass = iKey.getConcreteType();

		if (DEBUG) {
			System.err.println(("iKey: " + iKey + ' ' + system.findOrCreateIndexForInstanceKey(iKey)));
		}

		if (klass == null) {
			return;
		}
		if (!allocatedClasses.add(klass)) {
			return;
		}
		updateSetsForNewClass(klass, iKey, node, newSite);

		// side effect of new: may call class initializer
		processClassInitializer(klass);
	}

	/** Perform needed bookkeeping when a new class is discovered. */
	protected abstract void updateSetsForNewClass(IClass klass, InstanceKey iKey, CGNode node, NewSiteReference ns);

	/*
	 * @see
	 * com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder#customInit
	 * ()
	 */
	@Override
	protected void customInit() {
		super.customInit();

		FakeRootMethod m = (FakeRootMethod) getCallGraph().getFakeRootNode().getMethod();

		for (TypeReference element : PRE_ALLOC) {
			SSANewInstruction n = m.addAllocation(element);
			// visit now to ensure java.lang.Object is visited first
			visitNew(getCallGraph().getFakeRootNode(), n.getNewSite());
		}
	}

	/** @return set of IClasses determined to be allocated */
	@SuppressWarnings("unchecked")
	public Set<IClass> getAllocatedTypes() {
		return (Set<IClass>) allocatedClasses.clone();
	}

	/*
	 * @see
	 * com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder#makeSolver
	 * ()
	 */
	@Override
	protected IPointsToSolver makeSolver() {
		return new StandardSolver(system, this);
	}

	protected ContextSelector makeContextSelector(ContextSelector appContextSelector) {
		ContextSelector def = new DefaultContextSelector(options, cha);
		ContextSelector contextSelector = appContextSelector == null ? def
				: new DelegatingContextSelector(appContextSelector, def);
		return contextSelector;
	}

	protected SSAContextInterpreter makeContextInterpreter(SSAContextInterpreter appContextInterpreter) {

		SSAContextInterpreter defI = new DefaultSSAInterpreter(getOptions(), getAnalysisCache());
		defI = new DelegatingSSAContextInterpreter(
				ReflectionContextInterpreter.createReflectionContextInterpreter(cha, getOptions(), getAnalysisCache()),
				defI);
		SSAContextInterpreter contextInterpreter = appContextInterpreter == null ? defI
				: new DelegatingSSAContextInterpreter(appContextInterpreter, defI);
		return contextInterpreter;
	}

	@Override
	protected boolean unconditionallyAddConstraintsFromNode(CGNode node, IProgressMonitor monitor) {
		// add all relevant constraints
		addNewConstraints(node);
		addCallConstraints(node);
		addFieldConstraints(node);
		markAlreadyVisited(node);
		return true;
	}

	@Override
	protected ExplicitCallGraph createEmptyCallGraph(IMethod fakeRootClass, AnalysisOptions options) {
		return new DelegatingExplicitCallGraph(fakeRootClass, options, getAnalysisCache());
	}

	/*
	 * @see
	 * com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder#makeSystem
	 * (com.ibm.wala.ipa.callgraph.AnalysisOptions)
	 */
	@Override
	protected PropagationSystem makeSystem(AnalysisOptions options) {
		PropagationSystem result = super.makeSystem(options);
		result.setVerboseInterval(VERBOSE_INTERVAL);
		result.setPeriodicMaintainInterval(PERIODIC_MAINTAIN_INTERVAL);
		return result;
	}

	/*
	 * @see com.ibm.wala.ipa.callgraph.CallGraphBuilder#getPointerAnalysis()
	 */
	@Override
	public PointerAnalysis<InstanceKey> getPointerAnalysis() {
		return TypeBasedPointerAnalysis.make(getOptions(), allocatedClasses, getCallGraph());
	}
}
