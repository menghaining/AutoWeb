package ict.pag.webframework.log;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;

import ict.pag.webframework.log.util.LogReaderHelper;

public class LogReader {
	/*
	 * new log form means that the base/field class also has thread id and
	 * [ReqStart]&&[ReqEnd]. @22/6/21
	 */
	private static boolean newLogForm = false;

	/** true iff using new log form */
	public static void setForm(boolean newForm) {
		newLogForm = newForm;
	}

	/**
	 * sequence consist of 10 type of header type</br>
	 * 22-6-21 support new log form: base/field class also has thread id and
	 * [ReqStart]&&[ReqEnd]</br>
	 * 
	 * when in *new* form, do not removeExceptionCallStmts!</br>
	 * default newform=false
	 * 
	 * @return log sequence distinguished from thread id
	 */
	public static HashMap<Integer, ArrayList<String>> calId2Sequence(String path) {
		HashMap<Integer, ArrayList<String>> id2group = read(path);
		HashMap<Integer, ArrayList<String>> id2group2;
		if (newLogForm) {
			id2group2 = id2group;
		} else {
			id2group2 = removeExceptionCallStmts(id2group);
		}
		return id2group2;
	}

	/** for debug */
	public static void main(String[] args) {
		newLogForm = true;
		HashMap<Integer, ArrayList<String>> id2callList = calId2Sequence(args[0]);
	}

	private static HashMap<Integer, ArrayList<String>> read(String filePath) {
		HashMap<Integer, ArrayList<String>> id2group = new HashMap<Integer, ArrayList<String>>();

		File file = new File(filePath);
		try {
			FileReader fileReader = new FileReader(file);
			LineNumberReader reader = new LineNumberReader(fileReader);
			String lineContent0;

			int id = -1;

			int currThread = -1;
			while ((lineContent0 = reader.readLine()) != null) {
				String lineContent = lineContent0.trim();

				// in old form, [base xx]donot have thread id
				if (!newLogForm) {
					/** add the run time info about fields of 'this' object */
					if (lineContent.startsWith("[base")) {
						if (id != -1) {
							id2group.get(Integer.valueOf(id)).add(lineContent);
						}
					}
				}

				currThread = LogReaderHelper.getThreadID(lineContent, newLogForm);
				if (currThread == -1)
					continue;

				if (isNewThread(id2group, currThread)) {
					id2group.put(Integer.valueOf(currThread), new ArrayList<String>());
				}

				if (!newLogForm) {
					/** in this position start dealing with http request */
					if (lineContent.startsWith("[ReqURL]")) {
						if (currThread != -1) {
							id2group.get(Integer.valueOf(currThread))
									.add("url started " + lineContent.substring(lineContent.lastIndexOf(']') + 1));
						}
						id = -1;
						continue;
					} else if (lineContent.startsWith("[ReqURL_end]")) {
						/** in this position, http request done */
						id2group.get(Integer.valueOf(currThread)).add("url finished");
						id = -1;
						continue;
					}
				} else {
					// remove header_tag
					String line1 = lineContent.substring(lineContent.indexOf(']') + 1);
					// remove id
					String line2 = line1.substring(line1.indexOf(']') + 1);
					if (lineContent.startsWith("[ReqStart]")) {
						/** in this position start dealing with http request */
						if (currThread != -1) {
							id2group.get(Integer.valueOf(currThread)).add("[ReqStart]" + line2);
						}
						id = -1;
						continue;
					} else if (lineContent.startsWith("[ReqEnd]")) {
						/** in this position, http request done */
						id2group.get(Integer.valueOf(currThread)).add("[ReqEnd]" + line2);
						id = -1;
						continue;
					} else if (lineContent.startsWith("[base ")) {
						/** add the run time info about fields of 'this' object */
						if (id != -1) {
							id2group.get(Integer.valueOf(currThread))
									.add(lineContent.substring(0, lineContent.indexOf(']') + 1) + line2);
						}
						continue;
					}
				}

				// remove header_tag
				String line1 = lineContent.substring(lineContent.indexOf(']') + 1);
				// remove id
				String line2 = line1.substring(line1.indexOf(']') + 1);

				/** in the beginning of a call */
				if (lineContent.startsWith("[call method]")) {
					id2group.get(Integer.valueOf(currThread)).add(line2);
					id = Integer.valueOf(currThread);
				}

				/** when finishing this call method */
				if (lineContent.startsWith("[call method finished]")) {
					id2group.get(Integer.valueOf(currThread)).add(line2 + "[end]");
					id = -1;
				}

				/** callsite */
				if (lineContent.startsWith("[callsite]")) {
					id2group.get(Integer.valueOf(currThread)).add("[callsite]" + line2);
					id = -1;
				}

				/** returnSite */
				if (lineContent.startsWith("[returnSite]")) {
					id2group.get(Integer.valueOf(currThread)).add("[returnSite]" + line2);
					id = -1;
				}

				/** field write */
				if (lineContent.startsWith("[field write]")) {
					id2group.get(Integer.valueOf(currThread)).add("[field write]" + line2);
					id = -1;
				}

				/** field read */
				if (lineContent.startsWith("[field read]")) {
					id2group.get(Integer.valueOf(currThread)).add("[field read]" + line2);
					id = -1;
				}

			}

			fileReader.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return id2group;
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

				if (stmt.startsWith("[base ") || stmt.startsWith("[callsite]") || stmt.startsWith("[returnSite]")
						|| stmt.endsWith("[end]") || stmt.startsWith("[field write]") || stmt.startsWith("[field read]")
						|| stmt.startsWith("url started") || stmt.contains("url finished")
						|| (newLogForm && (stmt.startsWith("[ReqStart]") || stmt.startsWith("[ReqEnd]")))) {
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
				int j = i;
				for (; j < sequence.size(); j++) {
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
//					if (j == sequence.size()) {
//						ArrayList<String> sequence_back = fromback(sequence, i);
//						sequence_new.addAll(sequence_back);
//					}
					break;
				}
			}
			ret.put(id, sequence_new);
		}
		return ret;
	}

	private static ArrayList<String> fromback(ArrayList<String> sequence, int currpos) {
		ArrayList<String> sequence_back = new ArrayList<String>();

		int ensure = sequence.size() - 1;
		for (int i = sequence.size() - 1; i > currpos; i--) {
			String line = sequence.get(i);

			if (line.endsWith("[end]")) {
				/* find whether have corresponding call site */
				String start_stmt = line.substring(0, line.lastIndexOf('['));

				int same = 0;
				boolean flag = false;
				int j = i;

				for (; j > currpos; j--) {
					if (sequence.get(j).equals(line)) {
						same++;
					}
					if (sequence.get(j).equals(start_stmt)) {
						same--;
						if (same == 0) {
							flag = true;
							ensure = j;
							break;
						}
					}
				}
				if (flag) {
					sequence_back.add(0, line);
				} else {
					break;
				}
			} else {
				if (i >= ensure)
					sequence_back.add(0, line);
				continue;
			}
		}

		return sequence_back;
	}

	private static boolean isNewThread(HashMap<Integer, ArrayList<String>> id2group, int currThread) {
		for (Integer I : id2group.keySet()) {
			if (I.intValue() == currThread) {
				return false;
			}
		}
		return true;
	}

}
