package ict.pag.m.generateFrameworkModel4App.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.traverse.SCCIterator;

public class UnreachableCollector {
	private final IClassHierarchy cha;
	private final CHACallGraph chaCG;

	private List<IMethod> allAppMethods = new ArrayList<IMethod>();

	private Set<IMethod> handledRootMethods = new HashSet<>();

	private static UnreachableCollector instance;

	private UnreachableCollector(IClassHierarchy cha) {
		this.cha = cha;
		this.chaCG = new CHACallGraph(cha, true);
		try {
			this.chaCG.init();
		} catch (CancelException e) {
			e.printStackTrace();
		}
		cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(clazz -> {
			clazz.getDeclaredMethods().forEach(method -> {

				allAppMethods.add(method);
			});
		});
	}

	public static UnreachableCollector getInstance(IClassHierarchy cha) {
		if (instance == null)
			instance = new UnreachableCollector(cha);
		return instance;
	}

	public static void clear() {
		if (instance != null) {
			instance.allAppMethods.clear();
			instance.handledRootMethods.clear();
			instance.unanalysedScc.clear();
			instance = null;
		}
	}

	public List<Entrypoint> getEntryPoints() {
		return getUnreachableRoots(new HashSet<>());
	}

	private boolean firstSolve = true;
	private boolean stopUnreachable = false;
	private Set<Set<CGNode>> unanalysedScc = new HashSet<>();

	public List<Entrypoint> getUnreachableRoots(Set<IMethod> visted) {
		Map<CGNode, Boolean> zeroIncomingMap = new HashMap<>();

		for (IMethod appMethod : allAppMethods) {

			if (appMethod.isAbstract() || visted.contains(appMethod) || handledRootMethods.contains(appMethod))
				continue;

			try {
				CGNode node = chaCG.findOrCreateNode(appMethod, Everywhere.EVERYWHERE);
//				IR ir = node.getIR();
				node.iterateCallSites().forEachRemaining(callsite -> {
					chaCG.getPossibleTargets(node, callsite).forEach(callee -> {
						zeroIncomingMap.put(callee, false);
					});
				});
				if (!zeroIncomingMap.containsKey(node)) {
					zeroIncomingMap.put(node, true);
				}
			} catch (CancelException e) {
				e.printStackTrace();
			}
		}

		if (firstSolve) {
			firstSolve = false;
			List<CGNode> sccEntries = new ArrayList<>();
			for (IMethod appMethod : allAppMethods) {
				if (appMethod.isAbstract() || visted.contains(appMethod) || handledRootMethods.contains(appMethod))
					continue;
				try {
					CGNode node = chaCG.findOrCreateNode(appMethod, Everywhere.EVERYWHERE);
					if (zeroIncomingMap.get(node) == true)
						sccEntries.add(node);
				} catch (CancelException e) {
					e.printStackTrace();
				}
			}
			SCCIterator<CGNode> sccIter = new SCCIterator<CGNode>(chaCG, sccEntries.iterator());
			while (sccIter.hasNext()) {
				if (stopUnreachable) {
					System.err.println("unreachable searching solver timeout");
					break;
				}
				Set<CGNode> scc = sccIter.next();
				unanalysedScc.add(scc);
			}
		}

		List<Entrypoint> result = new ArrayList<>();

		List<Set<CGNode>> deleteSccs = new ArrayList<>();
		// first, handle scc
		outouter: for (Set<CGNode> scc : unanalysedScc) {
			boolean isolatedScc = true;
			outer: for (CGNode node : scc) {
				Iterator<CGNode> iter = chaCG.getPredNodes(node);
				while (iter.hasNext()) {
					if (stopUnreachable) {
						System.err.println("unreachable searching solver timeout");
						break outouter;
					}
					if (!scc.contains(iter.next())) {
						isolatedScc = false;
						break outer;
					}
				}
			}
			if (isolatedScc) {
				IMethod randomNode = null;
				Iterator<CGNode> sccIter = scc.iterator();

				while (sccIter.hasNext()) {
					randomNode = sccIter.next().getMethod();
					if (!randomNode.isPrivate() && !randomNode.isAbstract()) {
						if (randomNode != null) {
							handledRootMethods.add(randomNode);
							if (randomNode.toString().contains("fakeRootMethod"))
								continue;
							result.add(new DefaultEntrypoint(randomNode, cha));
						}
					}
				}

				for (CGNode node : scc) {
					zeroIncomingMap.put(node, false);
				}
				deleteSccs.add(scc);
			}
		}
		deleteSccs.forEach(deleteScc -> {
			unanalysedScc.remove(deleteScc);
		});

//		List<Entrypoint> result = new ArrayList<>();
		zeroIncomingMap.forEach((key, value) -> {

			if (value) {
				IMethod tmpMethod = key.getMethod();
				handledRootMethods.add(tmpMethod);
				if (tmpMethod.isPrivate() || tmpMethod.getDeclaringClass().isAbstract())
					return;
				if (tmpMethod.toString().contains("fakeRootMethod"))
					return;
				Entrypoint ep = new DefaultEntrypoint(tmpMethod, cha);
				result.add(ep);
			}
		});
		/** !!! remember to clear */
		clear();
		return result;
	}
}
