package ict.pag.webframework.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/** unable */
public class LogSpliter4URL {
	/** for debug */
	public static void main(String[] args) {
		LogReader.setForm(true);
		HashMap<Integer, ArrayList<String>> id2callList = LogReader.calId2Sequence(args[0]);
		HashMap<String, HashSet<ArrayList<String>>> url2group = LogSpliter4URL.split(id2callList);

		System.out.println();
	}

	public static HashMap<String, HashSet<ArrayList<String>>> split(HashMap<Integer, ArrayList<String>> id2group) {
		HashMap<String, HashSet<ArrayList<String>>> url2group = new HashMap<>();

		for (Integer id : id2group.keySet()) {
			ArrayList<String> sequence = id2group.get(id);

			boolean hasReqInfo = false;
			for (int i = 0; i < sequence.size(); i++) {
				String line = sequence.get(i);
				if (line.startsWith("[ReqStart]")) {
					hasReqInfo = true;
					break;
				}
			}

			if (!hasReqInfo) {
				if (url2group.containsKey("configuration")) {
					url2group.get("configuration").add(sequence);
				} else {
					HashSet<ArrayList<String>> tmp = new HashSet<>();
					tmp.add(sequence);
					url2group.put("configuration", tmp);
				}

				continue;
			}

			ArrayList<String> url = new ArrayList<>();
			ArrayList<Boolean> closed = new ArrayList<>();/* record whether start and end matched */
			ArrayList<ArrayList<String>> seqs = new ArrayList<>();

			int index = -1;/* current url index */

			for (int i = 0; i < sequence.size(); i++) {
				String line = sequence.get(i);

				if (line.startsWith("[ReqStart]")) {
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
					// normal lines
					int tmp = url.size() - 1;
					while (tmp != -1 && closed.get(tmp)) {
						tmp--;
					}
					if (tmp > -1)
						seqs.get(tmp).add(line);
				}
			}

		}

		return url2group;
	}

}
