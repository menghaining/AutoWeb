package ict.pag.m.marks2SAInfer.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import ict.pag.m.marks2SAInfer.util.CallsiteInfo;
import ict.pag.m.marks2SAInfer.util.StringUtil;

public class SplitStmtPerFunctionLayer {

	public static HashMap<String, Set<ArrayList<String>>> split(HashMap<Integer, ArrayList<String>> id2group) {
		/* outer2Seq: in 'outer' function, all callistes and directly actual invokes */
		HashMap<String, Set<ArrayList<String>>> outer2Seq = new HashMap<>();

		for (Integer id : id2group.keySet()) {
			ArrayList<String> sequence = id2group.get(id);
			// record whether the direct call in this scope have been handled
			// if handled, call start and end recorded as 1
			int[] dealwithedInternal = new int[sequence.size()];
			for (int i = 0; i < sequence.size(); i++) {

				// when i==0, calculate the outer call
				if (i == 0) {
					/* 1. collect the outer call sequence, and there are no callsites */
					ArrayList<String> callSeq = new ArrayList<>();
					for (int j = 0; j < sequence.size(); j++) {
						String line = sequence.get(j);
						if (line.startsWith("[callsite]")) {
							continue;
						}
						if (line.startsWith("url started") || line.contains("url finished")) {
							continue;
						}
						if (line.endsWith("[end]")) {
							continue;
						}

						// find the end of call method
						String end_stmt = line + "[end]";
						int k = j;
						int same = 0;
						for (; k < sequence.size(); k++) {
							if (sequence.get(k).equals(line)) {
								same++;
							}
							if (sequence.get(k).equals(end_stmt)) {
								same--;
								if (same == 0)
									break;
							}
						}
						// only find the outer calls
						if (k < sequence.size()) {
							callSeq.add(sequence.get(j));
							callSeq.add(sequence.get(k));
							// from the ends' next element
							j = k;
							continue;
						}
					}

					// when in the outer, only infer call sequence
					if (outer2Seq.containsKey("outer")) {
						outer2Seq.get("outer").add(callSeq);
					} else {
						HashSet<ArrayList<String>> tmp = new HashSet<>();
						tmp.add(callSeq);
						outer2Seq.put("outer", tmp);
					}

				}

				String stmt = sequence.get(i);

//				else {
				/* 2. find the call method start position */
				if (stmt.startsWith("[callsite]") || stmt.endsWith("[end]") || stmt.startsWith("url started")
						|| stmt.contains("url finished")) {
					continue;
				}
				if (dealwithedInternal[i] == 1)
					continue;

				/** from i to j means a call start at i and end at j */
				// if stmt is the start of call
				String end_stmt = stmt.concat("[end]");
				// find call end position
				int j = i;
				int same = 0;
				for (; j < sequence.size(); j++) {
					if (sequence.get(j).equals(stmt)) {
						same++;
					}
					if (sequence.get(j).equals(end_stmt)) {
						same--;
						if (same == 0)
							break;

					}
				}
				dealwithedInternal[i] = 1;
				if (j >= sequence.size())
					continue;
				dealwithedInternal[j] = 1;
				if (j == i + 1 || i == j)
					continue;

				String outerCall;
				if (stmt.endsWith("[normal]")) {
					outerCall = stmt.substring(0, stmt.indexOf("[normal]"));
				} else {
					outerCall = stmt;
				}

				/** collect the sequence directly belong to this call */
				ArrayList<String> callSeq = new ArrayList<>();
				int callLayer = 0;
				boolean allCallsite = true;
				for (int k = i + 1; k < j; k++) {
					String internal = sequence.get(k);

					if (internal.startsWith("url started") || internal.contains("url finished"))
						continue;

					if (internal.startsWith("[callsite]")) {
						CallsiteInfo lineInfo = StringUtil.calCallsiteInfo(internal);
						if (outerCall.equals(lineInfo.getBelongToMthd()) && callLayer == 0) {
							callSeq.add(internal);
						}
						continue;
					}

					if (internal.endsWith("[end]")) {
						if (callSeq.isEmpty()) {
							break;
						} else {
							callLayer--;
							String tmp = callSeq.get(callSeq.size() - 1);
							String tmp_end = tmp + "[end]";
							if (tmp_end.equals(internal)) {
								if (callLayer == 0) {
									allCallsite = false;
									callSeq.add(internal);
								}
							} else {
								continue;
							}
						}
					} else {
						if (callLayer == 0) {
							allCallsite = false;
							callSeq.add(internal);
						}
						callLayer++;
					}
				}

				// for callsite calculate and call sequence
				if (!allCallsite) {
					if (outer2Seq.containsKey(outerCall)) {
						outer2Seq.get(outerCall).add(callSeq);
					} else {
						HashSet<ArrayList<String>> tmp = new HashSet<>();
						tmp.add(callSeq);
						outer2Seq.put(outerCall, tmp);
					}

				}

//				}
			}
		}
		return outer2Seq;
	}

	// save times
	@Deprecated
	public static HashMap<String, Set<ArrayList<String>>> split2(HashMap<Integer, ArrayList<String>> id2group) {
		/* outer2Seq: in 'outer' function, all callistes and directly actual invokes */
		HashMap<String, Set<ArrayList<String>>> outer2Seq = new HashMap<>();

		for (Integer id : id2group.keySet()) {
			// for each thread sequence
			ArrayList<String> sequence = id2group.get(id);

			/** 1. first record outer-call sequence */
			// collect the outer call sequence, and there are no callsites
			ArrayList<String> callSeq = new ArrayList<>();
			for (int j = 0; j < sequence.size(); j++) {
				String line = sequence.get(j);
				if (line.startsWith("[callsite]")) {
					continue;
				}
				if (line.startsWith("url started") || line.contains("url finished")) {
					continue;
				}
				if (line.endsWith("[end]")) {
					continue;
				}

				// find the end of call method
				String end_stmt = line + "[end]";
				int k = j;
				int same = 0;
				for (; k < sequence.size(); k++) {
					if (sequence.get(k).equals(line)) {
						same++;
					}
					if (sequence.get(k).equals(end_stmt)) {
						same--;
						if (same == 0)
							break;
					}
				}
				// only find the outer calls
				if (k < sequence.size()) {
					callSeq.add(sequence.get(j));
					callSeq.add(sequence.get(k));
					// from the ends' next element
					j = k;
					continue;
				}
			}

			// when in the outer, only infer call sequence
			if (!callSeq.isEmpty()) {
				if (outer2Seq.containsKey("outer")) {
					outer2Seq.get("outer").add(callSeq);
				} else {
					HashSet<ArrayList<String>> tmp = new HashSet<>();
					tmp.add(callSeq);
					outer2Seq.put("outer", tmp);
				}
			}

			/** 2. find the method to it's internal direct call sequence */
			ArrayList<ArrayList<String>> mtd2Seq = new ArrayList<>();
			ArrayList<String> mtdName = new ArrayList<>();
			int ptr = -1;

			boolean error = false;
			for (int j = 0; j < sequence.size(); j++) {
				String line = sequence.get(j);

				if (line.startsWith("url started") || line.contains("url finished")) {
					continue;
				}

				if (line.startsWith("[callsite]")) {
					continue;
				}
				if (line.endsWith("[end]")) {
					if (line.startsWith(mtdName.get(ptr))) {
						continue;
					} else {
						System.err.println("ERROR ! Cannot match call_end to call!");
						error = true;
						break;
					}
				}
				// call start
				// 1. add into caller
				// 1.1 find its caller

				// 2. create itself sequence
				ArrayList<String> seq = new ArrayList<>();

			}

			if (!error) {

			}

		}

		return outer2Seq;
	}
}
