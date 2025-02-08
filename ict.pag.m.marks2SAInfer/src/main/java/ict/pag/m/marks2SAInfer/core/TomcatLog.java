package ict.pag.m.marks2SAInfer.core;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TomcatLog {
	final String filePath;

	private Set<String> runningCall = new HashSet<String>();

	private HashMap<Integer, Set<String>> url_callsHashMap = new HashMap<Integer, Set<String>>();

	private HashMap<Integer, ArrayList<String>> id2group = new HashMap<Integer, ArrayList<String>>();
	/** flag usage: when url_end called, status = false */
	private HashMap<Integer, Boolean> id2status = new HashMap<Integer, Boolean>();

	public TomcatLog(String path) {
		this.filePath = path;

		readFile();
//		System.out.println("Started PybbsApplication");
	}

	private Set<String> unique = new HashSet<String>();

	private void readFile() {

		File file = new File(filePath);
		try {
			FileReader fileReader = new FileReader(file);
			LineNumberReader reader = new LineNumberReader(fileReader);
			String lineContent0;

			int i = 0;
			Integer key = null;

			/** one call sequence for identify aop */
			ArrayList<String> group;
			int currThread = -1;
			boolean flag = false;
			while ((lineContent0 = reader.readLine()) != null) {
				String lineContent = lineContent0.trim();
//				/**
//				 * tmp
//				 */
//				if (lineContent.contains("Started PybbsApplication")) {
//					System.out.println("Started PybbsApplication");
//				}

				if (lineContent.startsWith("[ReqURL]")) {
					i++;

					key = new Integer(i);

//					Set<String> callseqSet = new HashSet<String>();
//					url_callsHashMap.put(key, callseqSet);

//					System.out.println(lineContent);

					currThread = getThreadID(lineContent);
					if (currThread != -1) {
						if (canContinue(currThread, true)) {
							flag = true;
							id2group.get(Integer.valueOf(currThread)).add("url started " + getLineInfo(lineContent));

						}
					}

				}

				if (lineContent.startsWith("[ReqURL_end]")) {

//					System.out.println(lineContent);

					currThread = getThreadID(lineContent);
					if (canContinue(currThread, false)) {
						flag = true;
						/** close the status */
						id2group.get(Integer.valueOf(currThread)).add("url finished");
						id2status.put(Integer.valueOf(currThread), Boolean.valueOf(false));
					}

				}

				if (lineContent.startsWith("[call method]")) {
//					int begin = lineContent.indexOf("]");
//					String sig = lineContent.substring(begin + 2);
//					if (!runningCall.contains(sig))
//						runningCall.add(sig);
//
//					if (key != null && flag) {
//						url_callsHashMap.get(key).add(sig);
//					}
					if (!hasGetSet(lineContent)) {
//						System.out.println(lineContent);

						if (flag) {
							currThread = getThreadID(lineContent);
							if (canContinue(currThread, false)) {
								flag = false;
								String line = getLineInfo(lineContent);
								id2group.get(Integer.valueOf(currThread)).add(line);
								if (!unique.contains(line)) {
									unique.add(line);
								}
							}

						}
					}

				}
				if (lineContent.startsWith("[call method finished]")) {
					if (!hasGetSet(lineContent)) {
//						System.out.println(lineContent);

						currThread = getThreadID(lineContent);
						if (canContinue(currThread, false)) {
							ArrayList<String> currs = id2group.get(Integer.valueOf(currThread));
							String tmp = currs.get(currs.size() - 1);
							if (tmp.equals(getLineInfo(lineContent))) {
								flag = true;
							}

						}
					}

				}

//				if (lineContent.startsWith("[internal call method]")) {
//					System.out.println(lineContent);
//				}

			}

			fileReader.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private boolean canContinue(int currThread, boolean isRequest) {
		for (Integer I : id2status.keySet()) {
			if (I.intValue() == currThread) {
				if (id2status.get(I).booleanValue())
					return true;
				else
					return false;
			}
		}

		/** do not has the ThreadID */
		if (isRequest) {
			id2group.put(Integer.valueOf(currThread), new ArrayList<String>());
			id2status.put(Integer.valueOf(currThread), Boolean.valueOf(true));
			return true;
		}

		return false;
	}

	private String getLineInfo(String lineContent) {
		String subString = lineContent.substring(lineContent.lastIndexOf(']') + 1);

		return subString;
	}

	/**
	 * "[ReqURL][46]http://localhost:8080/"
	 */
	private int getThreadID(String lineContent) {
		String subString = lineContent.substring(lineContent.indexOf(']') + 1);
		String sid = subString.substring(subString.indexOf('[') + 1, subString.indexOf(']'));
		int ret = -1;
		try {
			ret = Integer.parseInt(sid);
		} catch (Exception e) {
			System.err.println("errer convert string ro int");
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

	public HashMap<Integer, ArrayList<String>> getId2Group() {
		return id2group;
	}

	public static void main(String args[]) {
		String path = "F:\\pybbs_log.txt";
		TomcatLog log = new TomcatLog(path);
	}
}
