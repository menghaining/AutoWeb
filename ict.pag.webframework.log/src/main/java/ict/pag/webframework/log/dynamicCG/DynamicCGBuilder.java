package ict.pag.webframework.log.dynamicCG;

import java.util.ArrayList;

public class DynamicCGBuilder {
	private ArrayList<DynamicCGNode> nodes = new ArrayList<>();
	private DynamicCG cg = new DynamicCG();

	public DynamicCGBuilder() {

	}

	public DynamicCG getDynamicCallGraph() {
		return cg;
	}

	public void calculate(ArrayList<String> runtimeSeq) {
		/** 1. split by url */
		boolean hasReqInfo = false;

		ArrayList<String> url = new ArrayList<>();
		ArrayList<Boolean> closed = new ArrayList<>();/* record whether start and end matched */
		ArrayList<ArrayList<String>> seqs = new ArrayList<>();

		ArrayList<String> configsSeq = new ArrayList<>();

		int index = -1;/* current url index */

		for (int i = 0; i < runtimeSeq.size(); i++) {
			String line = runtimeSeq.get(i);

			if (line.startsWith("[ReqStart]")) {
				hasReqInfo = true;
				/* new an element */
				ArrayList<String> callSquence = new ArrayList<>();
				url.add(line);
				seqs.add(callSquence);
				closed.add(false);

				index = url.size() - 1;

			} else if (line.startsWith("[ReqEnd]")) {
				int tmp = url.size() - 1;
				while (tmp != -1 && (closed.get(tmp))) {
					tmp--;
					if (tmp == -1)
						break;
				}
				if (tmp > -1) {
					closed.remove(tmp);
					closed.add(tmp, true);
				}

			} else {
				if (hasReqInfo) {
					// normal lines
					int tmp = url.size() - 1;
					while (tmp != -1 && closed.get(tmp)) {
						tmp--;
					}
					if (tmp > -1)
						seqs.get(tmp).add(line);
				} else {
					configsSeq.add(line);
				}

			}
		}

		/** 2. build dynamic cg */
		if (configsSeq.size() != 0) {
			nodes.addAll(buildCGNodes(configsSeq, MethodCalledType.configuration));
		}

		for (int i = 0; i < url.size(); i++) {
			if (seqs.get(i).isEmpty())
				continue;
			nodes.addAll(buildCGNodes(seqs.get(i), MethodCalledType.request));
		}

//		cg.getNodes().addAll(nodes);
//		System.out.println();
	}

	public ArrayList<DynamicCGNode> buildCGNodes(ArrayList<String> sequence, MethodCalledType type) {
		ArrayList<DynamicCGNode> cgNodes = new ArrayList<>();
		ArrayList<DynamicCGNode> cgNodes_extra = new ArrayList<>();

		ArrayList<String> mtds = new ArrayList<>();
		ArrayList<ArrayList<String>> internalSeq = new ArrayList<>();
		ArrayList<Boolean> closed = new ArrayList<>();/* record whether start and end matched */

		for (String stmt : sequence) {
			if (stmt.contains("CommonResultControllerAdvice"))
				System.out.println();
			if (stmt.startsWith("[base ") || stmt.startsWith("[field ") || stmt.startsWith("[callsite]") || stmt.startsWith("[returnSite]")) {
				int tmp = mtds.size() - 1;
				while (tmp != -1 && closed.get(tmp)) {
					tmp--;
				}
				if (tmp > -1) {
					// all infos
					internalSeq.get(tmp).add(stmt);

					if (stmt.startsWith("[callsite]")) {
						cgNodes.get(tmp).getCallsites().add(new CallsiteInfo(stmt));
					} else if (stmt.startsWith("[returnSite]")) {
						ArrayList<CallsiteInfo> css = cgNodes.get(tmp).getCallsites();
						int end = css.size() - 1;
						while (end > -1 && css.get(end).isClosed()) {
							end--;
						}
						if (end > -1) {
							css.get(end).setClosed(true);
						} else {
							System.err.println("unmatched");
						}
					}

				} else {
					// may occur error at some time before
					cgNodes.clear();
					break;
				}

			} else if (stmt.endsWith("[end]")) {
				int tmp = mtds.size() - 1;
				// 0208: find backward until find;
				// if not find, break
				while (tmp != -1) {
					if (closed.get(tmp)) {
						// continue find backward
					} else {
						String mtd = mtds.get(tmp);
						if (stmt.equals(mtd + "[end]")) {
							closed.remove(tmp);
							closed.add(tmp, true);
							cgNodes.get(tmp).setClosed(true);
							break;
						}
						// else: continue find backward
					}
					tmp--;
				}
				if (tmp == -1)
					break;
//				// Backward find the nearest not closed method
//				while (tmp != -1 && closed.get(tmp)) {
//					tmp--;
//				}
//				if (tmp > -1) {
//					String mtd = mtds.get(tmp);
//					if (stmt.equals(mtd + "[end]")) {
//						closed.remove(tmp);
//						closed.add(tmp, true);
//
//						cgNodes.get(tmp).setClosed(true);
//					} else {
//						// add 1101 : one more chance
//						tmp--;
//						while (tmp != -1 && closed.get(tmp)) {
//							tmp--;
//						}
//						if (tmp != -1 && stmt.equals(mtds.get(tmp) + "[end]")) {
//							closed.remove(tmp);
//							closed.add(tmp, true);
//
//							cgNodes.get(tmp).setClosed(true);
//						} else {
//							for (int ii = 0; ii < cgNodes.size(); ii++)
//								if (closed.get(ii))
//									cgNodes_extra.add(cgNodes.get(ii));
//							// may occur error at some time before
//							cgNodes.clear();
//							break;
//						}
//					}
//				} else {
//					// find the first method still not find
//					// may occur error at some time before
//					cgNodes.clear();
//					break;
//				}
			} else {
				mtds.add(stmt);
				ArrayList<String> seq = new ArrayList<>();
				internalSeq.add(seq);
				closed.add(false);

				DynamicCGNode node = new DynamicCGNode(stmt);
				node.setSequenceinfo(seq);
				node.setType(type);
				/* occurred ordered */
				cgNodes.add(node);

				// find pre-node
				DynamicCGNode pre = null;
				int tmp = mtds.size() - 2;
				while (tmp != -1 && closed.get(tmp)) {
					tmp--;
				}
				if (tmp > -1) {
					pre = cgNodes.get(tmp);
					node.setFather(pre);

					boolean find = false;
					for (CallsiteInfo ele : pre.getCallsites()) {
						if (ele.isClosed())
							continue;

						ele.getActual_targets().add(node);
						find = true;
						break;
					}
					if (!find)
						pre.getOtherTargets().add(node);
				}
				if (pre == null) {
					node.setFather(null);
					/* ordered by occurred sequence */
					cg.getRoots().add(node);
				}
			}
		}

		for (DynamicCGNode node : cgNodes) {
			if (node.getFather() != null) {
				if (!node.getFather().isClosed()) {
					// 0208
//					if (node.getFather().getOtherTargets().contains(node)) {
					node.setFather(null);
					cg.getRoots().add(node);
//					}
				}
			}
		}
		for (DynamicCGNode node : cgNodes_extra) {
			if (node.getFather() != null) {
				if (!node.getFather().isClosed()) {
					if (node.getFather().getOtherTargets().contains(node)) {
						node.setFather(null);
						cg.getRoots().add(node);
					}
				}
			}
		}

		cg.getNodes().addAll(cgNodes);
		cg.getNodes().addAll(cgNodes_extra);
		return cgNodes;

	}

}
