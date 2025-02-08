package ict.pag.webframework.log;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class LogParser {
	final String filePath;

	private Set<String> unreachableRoots;

	private Set<String> runningCall = new HashSet<String>();

	private HashMap<Integer, Set<String>> url_callsHashMap = new HashMap<Integer, Set<String>>();

	private Set<String> allRunnableMethods = new HashSet<>();

	/** full informations */
	private HashMap<Integer, ArrayList<String>> id2group_full = new HashMap<Integer, ArrayList<String>>();
	/** only unreachable calls sequence , excluding callsites and end */
	private HashMap<Integer, ArrayList<String>> id2group_unreachable_only = new HashMap<Integer, ArrayList<String>>();

	public LogParser(String original_log, Set<String> unreachableRoots2) {
		this.filePath = original_log;

		unreachableRoots = unreachableRoots2;

		long beforeTime = System.nanoTime();
		readFile();
		double buildTime = (System.nanoTime() - beforeTime) / 1E9;
		System.out.println("[TIME-LOG] Log Parse Done in " + buildTime + " s!");
	}

	public Set<String> getAllRunnableMethods() {
		return allRunnableMethods;
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

					id2group_full.put(Integer.valueOf(currThread), new ArrayList<String>());
					id2group_unreachable_only.put(Integer.valueOf(currThread), new ArrayList<String>());
				}

				/** in this position start dealing with http request */
				if (lineContent.startsWith("[ReqURL]")) {
					if (currThread != -1) {

						id2group_full.get(Integer.valueOf(currThread)).add("url started " + getLineInfo(lineContent));
						id2group_unreachable_only.get(Integer.valueOf(currThread))
								.add("url started " + getLineInfo(lineContent));
					}
				}
				/** in this position, http request done */
				if (lineContent.startsWith("[ReqURL_end]")) {

					id2group_full.get(Integer.valueOf(currThread)).add("url finished");
					id2group_unreachable_only.get(Integer.valueOf(currThread)).add("url finished");
				}

				/** in the beginning of a call */
				if (lineContent.startsWith("[call method]")) {
					if (!hasGetSet(lineContent)) {
						String line0 = getLineInfo(lineContent);
						allRunnableMethods.add(line0);
						if (isUnreachable(line0)) {
							id2group_unreachable_only.get(Integer.valueOf(currThread)).add(line0);
						} else {
							line0 = line0.concat("[normal]");
						}
						id2group_full.get(Integer.valueOf(currThread)).add(line0);

						if (!unique.contains(line0)) {
							unique.add(line0);
						}
					}

				}
				/** when finishing this call method */
				if (lineContent.startsWith("[call method finished]")) {
					if (!hasGetSet(lineContent)) {
						String line0 = getLineInfo(lineContent);
						if (!isUnreachable(line0)) {
							line0 = line0.concat("[normal]");
						}
						line0 = line0.concat("[end]");
						id2group_full.get(Integer.valueOf(currThread)).add(line0);

					}

				}

				/** callsite */
				if (lineContent.startsWith("[callsite]")) {
					String subString = lineContent.substring(lineContent.indexOf(']') + 1);
					String line = subString.substring(subString.indexOf(']') + 1);
					id2group_full.get(Integer.valueOf(currThread)).add("[callsite]" + line);
				}
			}

			fileReader.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private boolean isUnreachable(String lineInfo) {
		for (String unr : this.unreachableRoots) {
			if (unr.equals(lineInfo))
				return true;
		}
//		if (this.unreachableRoots.contains(lineInfo))
//			return true;
		return false;
	}

	private boolean isNewThread(int currThread) {
		for (Integer I : id2group_full.keySet()) {
			if (I.intValue() == currThread) {
				return false;
			}
		}

		return true;
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
		return id2group_full;
	}

	/**
	 * return all unreachable call sequences, only has call and request start and
	 * end
	 */
	public HashMap<Integer, ArrayList<String>> getId2group_OnlyUnreachable() {
		return id2group_unreachable_only;
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
