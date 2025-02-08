package ict.pag.m.marks2SAInfer.util;

import java.util.Collection;
import java.util.function.Predicate;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.CollectionFilter;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.viz.DotUtil;

public class CGUtil {

	public static void exportCallGraph(CallGraph appCHACG) {
		Graph<CGNode> g = pruneGraph(appCHACG, new Predicate<CGNode>() {
			@Override
			public boolean test(CGNode o) {
				if (o == null)
					return false;
				return o.getMethod().getDeclaringClass().getClassLoader().getReference()
						.equals(ClassLoaderReference.Application)
						|| o.getMethod().getDeclaringClass().getClassLoader().getReference()
								.equals(ClassLoaderReference.Extension);
			}
		});
		if (g != null) {
			try {
				DotUtil.writeDotFile(g, null, null, "cg.dot");
			} catch (WalaException e) {
				e.printStackTrace();
			}
		}

	}

	private static <T> Graph<T> pruneGraph(Graph<T> g, Predicate<T> f) {
		Collection<T> slice = GraphSlicer.slice(g, f);
		return GraphSlicer.prune(g, new CollectionFilter<>(slice));
	}

}
