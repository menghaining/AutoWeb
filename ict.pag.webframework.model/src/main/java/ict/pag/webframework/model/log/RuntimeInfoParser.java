package ict.pag.webframework.model.log;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

// TODO : collection info parser
public class RuntimeInfoParser {
	/* record all information about actual call sequence without exception calls */
	private HashMap<Integer, ArrayList<String>> id2group = new HashMap<Integer, ArrayList<String>>();
	/* map method to all its calls internal */
	private HashMap<String, Set<ArrayList<String>>> outer2Seq;

	public void parser(String path) {
		readFile(path);
		/** remove exception statements */
		id2group = removeExceptionCallStmts(id2group);
		outer2Seq = SplitStmt4OneFunction.split(id2group);
	}

	public HashMap<String, Set<ArrayList<String>>> getOuter2Seq() {
		return outer2Seq;
	}

	private Set<String> unique = new HashSet<String>();

	public void readFile(String filePath) {
		File file = new File(filePath);
		try {
			FileReader fileReader = new FileReader(file);
			LineNumberReader reader = new LineNumberReader(fileReader);
			String lineContent0;

			int id = -1;

			int currThread = -1;
			while ((lineContent0 = reader.readLine()) != null) {
				String lineContent = lineContent0.trim();

				/** add the run time info about fields of 'this' object */
				if (lineContent.startsWith("[base")) {
					if (id != -1) {
						id2group.get(Integer.valueOf(id)).add(lineContent);
					}
				}

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
					id = -1;
				}

				/** in this position, http request done */
				if (lineContent.startsWith("[ReqURL_end]")) {
					id2group.get(Integer.valueOf(currThread)).add("url finished");
					id = -1;
				}

				/** in the beginning of a call */
				if (lineContent.startsWith("[call method]")) {
					String line0 = getLineInfo(lineContent);
					id2group.get(Integer.valueOf(currThread)).add(line0);
					if (!unique.contains(line0)) {
						unique.add(line0);
					}
					id = Integer.valueOf(currThread);
				}

				/** when finishing this call method */
				if (lineContent.startsWith("[call method finished]")) {
					String line0 = getLineInfo(lineContent);
					line0 = line0.concat("[end]");
					id2group.get(Integer.valueOf(currThread)).add(line0);
					id = -1;
				}

				/** callsite */
				if (lineContent.startsWith("[callsite]")) {
					String subString = lineContent.substring(lineContent.indexOf(']') + 1);
					String line = subString.substring(subString.indexOf(']') + 1);
					id2group.get(Integer.valueOf(currThread)).add("[callsite]" + line);
					id = -1;
				}

			}

			fileReader.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

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

	private boolean isNewThread(int currThread) {
		for (Integer I : id2group.keySet()) {
			if (I.intValue() == currThread) {
				return false;
			}
		}
		return true;
	}

	private String getLineInfo(String lineContent) {
		return lineContent.substring(lineContent.lastIndexOf(']') + 1);
	}

	/**
	 * There call start may do not have corresponding call end because actual
	 * running may have exception
	 * 
	 * @return actual calls sequence that without exception interference
	 */
	private static HashMap<Integer, ArrayList<String>> removeExceptionCallStmts(
			HashMap<Integer, ArrayList<String>> id2group) {
		HashMap<Integer, ArrayList<String>> ret = new HashMap<Integer, ArrayList<String>>();

		for (Integer id : id2group.keySet()) {
			ArrayList<String> sequence = id2group.get(id);

			ArrayList<String> sequence_new = new ArrayList<String>();
			for (int i = 0; i < sequence.size(); i++) {
				String stmt = sequence.get(i);

				if (stmt.startsWith("[base ") || stmt.startsWith("[callsite]") || stmt.endsWith("[end]")
						|| stmt.startsWith("url started") || stmt.contains("url finished")) {
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

	public HashMap<Integer, ArrayList<String>> getId2group() {
		return id2group;
	}

	public static void main(String[] args) {
		RuntimeInfoParser p = new RuntimeInfoParser();
		p.parser(args[0]);
		System.out.println();
	}
}
