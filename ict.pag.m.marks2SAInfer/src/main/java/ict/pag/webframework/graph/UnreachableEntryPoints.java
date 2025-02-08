package ict.pag.webframework.graph;

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
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.traverse.SCCIterator;

import ict.pag.webframework.option.ConfigUtil;

public class UnreachableEntryPoints {
	private final IClassHierarchy cha;
	private final CHACallGraph chaCG;

	private List<IMethod> allAppMethods = new ArrayList<IMethod>();

	private Set<IMethod> handledRootMethods = new HashSet<>();
	private Set<Set<CGNode>> unanalysedScc = new HashSet<>();

	private static UnreachableEntryPoints instance;

	private UnreachableEntryPoints(IClassHierarchy cha) {
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

	public static UnreachableEntryPoints getInstance(IClassHierarchy cha) {
		if (instance == null)
			instance = new UnreachableEntryPoints(cha);
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

	/**
	 * @return entrypoint set that contains all nodes do not have caller and all
	 *         nodes of isolated SCCs
	 */
	public List<Entrypoint> getEntryPoints() {
		return getUnreachableRoots(new HashSet<>());
	}

	public boolean firstSolve = true;

	/**
	 * if has isolated scc, add all nodes of scc to unreachable set
	 */
	public List<Entrypoint> getUnreachableRoots(Set<IMethod> visted) {
		Map<CGNode, Boolean> zeroIncomingMap = new HashMap<>();

		/* 1. for common situation */
		for (IMethod appMethod : allAppMethods) {

			// only record the application codes unreachable
			String signature = appMethod.getSignature();

			if (!((appMethod.getDeclaringClass().getClassLoader().getReference()
					.equals(ClassLoaderReference.Application) && ConfigUtil.enableApplication)
					|| ConfigUtil.isApplicationClass(signature)))
				continue;

			if (appMethod.isAbstract() || visted.contains(appMethod) || handledRootMethods.contains(appMethod))
				continue;

			try {
				// just find the caller from application method
				CGNode node = chaCG.findOrCreateNode(appMethod, Everywhere.EVERYWHERE);
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

		/* 2. calclulate isolated sccs */
		// sccs should be calculated only once
		if (firstSolve) {
			firstSolve = false;
			List<CGNode> sccEntries = new ArrayList<>();
			for (IMethod appMethod : allAppMethods) {
				if (appMethod.isAbstract() || visted.contains(appMethod) || handledRootMethods.contains(appMethod))
					continue;
				try {
					/** add each node of scc into entry set */
					CGNode node = chaCG.findOrCreateNode(appMethod, Everywhere.EVERYWHERE);
					if (zeroIncomingMap.get(node) == true)
						sccEntries.add(node);
				} catch (CancelException e) {
					e.printStackTrace();
				}
			}
			SCCIterator<CGNode> sccIter = new SCCIterator<CGNode>(chaCG, sccEntries.iterator());
			while (sccIter.hasNext()) {
				Set<CGNode> scc = sccIter.next();
				unanalysedScc.add(scc);
			}
		}

		List<Entrypoint> result = new ArrayList<>();

		List<Set<CGNode>> deleteSccs = new ArrayList<>();
		// first, handle scc
		for (Set<CGNode> scc : unanalysedScc) {
			boolean isolatedScc = true;
			outer: for (CGNode node : scc) {
				Iterator<CGNode> iter = chaCG.getPredNodes(node);
				while (iter.hasNext()) {
					if (!scc.contains(iter.next())) {
						isolatedScc = false;
						break outer;
					}
				}
			}
			if (isolatedScc) {
				Iterator<CGNode> sccIter = scc.iterator();
				// if is an isolated scc, add all nodes in the scc into result
				while (sccIter.hasNext()) {
					IMethod randomNode = sccIter.next().getMethod();
					if (randomNode != null) {
						handledRootMethods.add(randomNode);
						if (randomNode.getDeclaringClass().getClassLoader().getReference()
								.equals(ClassLoaderReference.Application)) {
							if (!randomNode.isPrivate() && !randomNode.isAbstract()) {
								if (randomNode.toString().contains("fakeRootMethod"))
									continue;
								result.add(new DefaultEntrypoint(randomNode, cha));
							}
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

		/* 3. add into result */
		zeroIncomingMap.forEach((key, value) -> {
			if (value) {
				IMethod tmpMethod = key.getMethod();
				handledRootMethods.add(tmpMethod);

				if (!tmpMethod.getDeclaringClass().getClassLoader().getReference()
						.equals(ClassLoaderReference.Application))
					return;
				if (tmpMethod.isPrivate() || tmpMethod.getDeclaringClass().isAbstract())
					return;
				if (tmpMethod.toString().contains("fakeRootMethod"))
					return;

				Entrypoint ep = new DefaultEntrypoint(tmpMethod, cha);
				result.add(ep);
			}
		});

		return result;
	}

}
