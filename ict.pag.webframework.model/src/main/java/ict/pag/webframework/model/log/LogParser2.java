package ict.pag.webframework.model.log;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;

import ict.pag.webframework.model.option.SpecialHelper;

public class LogParser2 {
	final String filePath;

	private Set<String> runningCall = new HashSet<String>();
	private HashMap<Integer, Set<String>> url_callsHashMap = new HashMap<Integer, Set<String>>();
	private Set<String> allRunnableMethods = new HashSet<>();

	/** record all information about actual call sequence that removed exceptions */
	private HashMap<Integer, ArrayList<String>> id2group = new HashMap<Integer, ArrayList<String>>();
	private HashMap<String, Set<ArrayList<String>>> outer2Seq;
	private Callsite2CallSeqMapTool tool;
//	/** only unreachable calls sequence , excluding callsites and end */
//	private HashMap<Integer, ArrayList<String>> id2group_unreachable_only = new HashMap<Integer, ArrayList<String>>();

	/** all calls that may invoked by framework */
	private HashSet<String> frmkInvokesCalls = new HashSet<>();

	public LogParser2(String original_log, CHACallGraph chaCG, HashSet<String> applicationClasses) {
		this.filePath = original_log;
		long beforeTime = System.nanoTime();
		parseLog(chaCG, applicationClasses);
		double buildTime = (System.nanoTime() - beforeTime) / 1E9;
		System.out.println("[TIME-LOG] Log Parse Done in " + buildTime + " s!");
	}

	private void parseLog(CHACallGraph chaCG, HashSet<String> applicationClasses) {
		readFile();
		id2group = removeExceptionCallStmts(id2group);
		outer2Seq = SplitStmt4OneFunction.split(id2group);
		tool = new Callsite2CallSeqMapTool(chaCG, outer2Seq, applicationClasses);
		tool.dealWith();
		calculateUnreachableCalls(chaCG, applicationClasses);
	}

	private void calculateUnreachableCalls(CHACallGraph chaCG, HashSet<String> applicationClasses) {
		IClassHierarchy cha = chaCG.getClassHierarchy();
		if (outer2Seq != null) {
			for (String outer : outer2Seq.keySet()) {
				Set<ArrayList<String>> seqSet = outer2Seq.get(outer);
				if (outer.equals("outer")) {
					// add all
					for (ArrayList<String> seq : seqSet) {
						for (String s : seq) {
							if (!s.endsWith("[end]"))
								frmkInvokesCalls.add(s);
						}
					}
				} else {
					// application call:if have no callsite match this call, add this call
					// match means: 1. equal 2. has inheritance relation
					// framework call: all calls add
					for (ArrayList<String> seq : seqSet) {
						int i = 0;
						while (i < seq.size()) {
							String currLine = seq.get(i);
							if (currLine.startsWith("[callsite]")) {
								ArrayList<String> calls = new ArrayList<>();
								int j = i + 1;
								for (; j < seq.size(); j++) {
									String next = seq.get(j);
									if (next.startsWith("[callsite]")) {

										break;
									} else if (!next.endsWith("[end]")) {
										calls.add(next);
									}
								}
								i = j;
								CallsiteInfo callsite = CallsiteInfo.calCallsiteInfo(currLine);
								String callStmt = callsite.getCallStmt();
								String callStmtClass = callStmt.substring(0, callStmt.lastIndexOf('.'));
								if (applicationClasses.contains(callStmtClass)) {
									String callSiteClass2 = SpecialHelper.reformatSignature(callStmtClass);
									IClass callSiteClazz = cha.getLoader(ClassLoaderReference.Application)
											.lookupClass(TypeName.findOrCreate(
													callSiteClass2.substring(0, callSiteClass2.length() - 1)));
									if (callSiteClazz == null)
										continue;
									for (String target : calls) {
										if (!target.equals(callStmt)) {
											String targetClass = target.substring(0, target.lastIndexOf('.'));

											String targetClass2 = SpecialHelper.reformatSignature(targetClass);
											IClass targetClazz = cha.getLoader(ClassLoaderReference.Application)
													.lookupClass(TypeName.findOrCreate(
															targetClass2.substring(0, targetClass2.length() - 1)));
											if (targetClazz == null)
												continue;
											if (!cha.isSubclassOf(targetClazz, callSiteClazz)) {
												if (!target.equals(callStmt))
													frmkInvokesCalls.add(target);
											}
										}
									}
								} else {
									for (String target : calls) {
										if (!target.equals(callStmt))
											frmkInvokesCalls.add(target);
									}
								}
							} else {
								i++;
							}
						}
					}
				}
			}
		}
	}

	private Set<String> unique = new HashSet<String>();

	private void readFile() {
		File file = new File(filePath);
		try {
			FileReader fileReader = new FileReader(file);
			LineNumberReader reader = new LineNumberReader(fileReader);
			String lineContent0;

			int currThread = -1;
			while ((lineContent0 = reader.readLine()) != null) {
				String lineContent = lineContent0.trim();
				currThread = getThreadID(lineContent);
				if (currThread == -1)
					continue;

				if (isNewThread(currThread)) {
					id2group.put(Integer.valueOf(currThread), new ArrayList<String>());
				}

				/** in this position start dealing with http request */
				if (lineContent.startsWith("[ReqURL]")) {
					if (currThread != -1) {

						id2group.get(Integer.valueOf(currThread)).add("url started " + getLineInfo(lineContent));
					}
				}
				/** in this position, http request done */
				if (lineContent.startsWith("[ReqURL_end]")) {

					id2group.get(Integer.valueOf(currThread)).add("url finished");
				}

				/** in the beginning of a call */
				if (lineContent.startsWith("[call method]")) {
					if (!hasGetSet(lineContent)) {
						String line0 = getLineInfo(lineContent);
						allRunnableMethods.add(line0);
						id2group.get(Integer.valueOf(currThread)).add(line0);

						if (!unique.contains(line0)) {
							unique.add(line0);
						}
					}

				}
				/** when finishing this call method */
				if (lineContent.startsWith("[call method finished]")) {
					if (!hasGetSet(lineContent)) {
						String line0 = getLineInfo(lineContent);
						line0 = line0.concat("[end]");
						id2group.get(Integer.valueOf(currThread)).add(line0);

					}

				}

				/** callsite */
				if (lineContent.startsWith("[callsite]")) {
					String subString = lineContent.substring(lineContent.indexOf(']') + 1);
					String line = subString.substring(subString.indexOf(']') + 1);
					id2group.get(Integer.valueOf(currThread)).add("[callsite]" + line);
				}
			}

			fileReader.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private boolean isNewThread(int currThread) {
		for (Integer I : id2group.keySet()) {
			if (I.intValue() == currThread) {
				return false;
			}
		}
		return true;
	}

	/** @return all methods signatures that actual running meets */
	public Set<String> getAllRunnableMethods() {
		return allRunnableMethods;
	}

	private String getLineInfo(String lineContent) {
		String subString = lineContent.substring(lineContent.lastIndexOf(']') + 1);

		return subString;
	}

	/**
	 * "[ReqURL][46]http://localhost:8080/"
	 */
	private int getThreadID(String lineContent) {
		if (!(lineContent.startsWith("[ReqURL]") || lineContent.startsWith("[ReqURL_end]")
				|| lineContent.startsWith("[call method]") || lineContent.startsWith("[call method finished]")
				|| lineContent.startsWith("[callsite]")))
			return -1;
		String sid;
		if (lineContent.startsWith("[callsite]")) {
			String subString = lineContent.substring(lineContent.indexOf(']') + 1);
			sid = subString.substring(1, subString.indexOf(']'));
		} else {
			String subString = lineContent.substring(lineContent.indexOf(']') + 1);
			sid = subString.substring(subString.indexOf('[') + 1, subString.indexOf(']'));
		}
		int ret = -1;
		try {
			ret = Integer.parseInt(sid);
		} catch (Exception e) {
			System.err.println("errer convert string to int");
			return ret;
		}

		return ret;
	}

	private boolean hasGetSet(String sig) {
//		String tmp = sig.substring(sig.lastIndexOf('.') + 1, sig.indexOf('('));
//		if (tmp.startsWith("get") || tmp.startsWith("set")) {
//			return true;
//		}
		return false;
	}

	public Set<String> getAllCalls() {
		return this.runningCall;
	}

	public HashMap<Integer, Set<String>> getallurlSequences() {
		return url_callsHashMap;
	}

	/**
	 * return all call sequences including called start and end, callsite, both
	 * unreachable and normal and request start and end
	 */
	public HashMap<Integer, ArrayList<String>> getId2group_fullInfo() {
		return id2group;
	}

//	/**
//	 * return all unreachable call sequences, only has call and request start and
//	 * end
//	 */
//	public HashMap<Integer, ArrayList<String>> getId2group_OnlyUnreachable() {
//		return id2group_unreachable_only;
//	}

	public HashSet<String> getFrameworkInvokesCalls() {
		return frmkInvokesCalls;
	}

	/** @return map: method signature -> List{callsite and calls} */
	public HashMap<String, Set<ArrayList<String>>> getOuter2Seq() {
		return outer2Seq;
	}

	public Callsite2CallSeqMapTool getCallsite2CallSeqTool() {
		return tool;
	}

	/**
	 * There call start may do not have corresponding call end because actual
	 * running may have exception
	 * 
	 * @return actual calls sequence that without exception interference
	 */
	public static HashMap<Integer, ArrayList<String>> removeExceptionCallStmts(
			HashMap<Integer, ArrayList<String>> id2group) {
		HashMap<Integer, ArrayList<String>> ret = new HashMap<Integer, ArrayList<String>>();

		for (Integer id : id2group.keySet()) {
			ArrayList<String> sequence = id2group.get(id);

			ArrayList<String> sequence_new = new ArrayList<String>();
			for (int i = 0; i < sequence.size(); i++) {
				String stmt = sequence.get(i);

				if (stmt.startsWith("[callsite]") || stmt.endsWith("[end]") || stmt.startsWith("url started")
						|| stmt.contains("url finished")) {
					sequence_new.add(stmt);
					continue;
				}

				/* find whether have corresponding return site */
				String end_stmt = stmt.concat("[end]");
				// example: foo(){...; foo();...}
				// may sequence: [start]foo();...[start]foo();...[end]foo();...[end]foo();
				// the first foo start must match the second foo end
				// the second foo start must macth the first foo end
				// same used for counting the internal same call, 'start++' and 'end--'
				// for finding the matched return site
				int same = 0;
				boolean flag = false;
				for (int j = i; j < sequence.size(); j++) {
					if (sequence.get(j).equals(stmt)) {
						same++;
					}
					if (sequence.get(j).equals(end_stmt)) {
						same--;
						if (same == 0) {
							flag = true;
							break;
						}
					}
				}
				// have corresponding return site
				if (flag) {
					sequence_new.add(stmt);
				} else {
					break;
				}
			}
			ret.put(id, sequence_new);
		}
		return ret;
	}

}
