package ict.pag.webframework.log;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import ict.pag.webframework.log.util.LogReaderHelper;

public class RequestInfoExtractor {
	ArrayList<String> url_request = new ArrayList<>(); /* record the sequence of urls request occur */
	ArrayList<String> url_response = new ArrayList<>(); /* record the sequence of urls response occur */
	ArrayList<ArrayList<String>> seqs = new ArrayList<>(); /* url's log content */
	ArrayList<Boolean> closed = new ArrayList<>();/* record whether start and end matched */
	ArrayList<Integer> thread = new ArrayList<>();/* record thread id */

	ArrayList<ArrayList<String>> configsSeqs = new ArrayList<>();/* sequence donot has req at deployment phrase */
	ArrayList<ArrayList<String>> otherSeqs = new ArrayList<>(); /* may exception */

	public boolean deploySuccess = false;

	public void readAndParse(String filePath) {
		HashMap<Integer, ArrayList<String>> id2Sequence = new HashMap<Integer, ArrayList<String>>();
		HashMap<Integer, ArrayList<Integer>> id2UrlStartIndex = new HashMap<>(); /* record the global index */

		// 1. collect all lines separated by thread, as well record global index of
		// request start
		File file = new File(filePath);
		try {
			FileReader fileReader = new FileReader(file);
			LineNumberReader reader = new LineNumberReader(fileReader);

			int urlStartIndex = 0; /* from 1. smaller means earlier */
			String lineContent0;
			while ((lineContent0 = reader.readLine()) != null) {
				String lineContent = lineContent0.trim();
				int currThread = LogReaderHelper.getThreadID(lineContent, true);
				if (currThread == -1)/* not instrumented log are filtered */
					continue;
				if (isNewThread(id2Sequence.keySet(), currThread)) {
					id2Sequence.put(Integer.valueOf(currThread), new ArrayList<String>());
					id2UrlStartIndex.put(Integer.valueOf(currThread), new ArrayList<Integer>());
				}

				id2Sequence.get(Integer.valueOf(currThread)).add(lineContent);

				if (lineContent.startsWith("[ReqStart]")) {
					urlStartIndex++;
					id2UrlStartIndex.get(Integer.valueOf(currThread)).add(urlStartIndex);
					deploySuccess = true;
				}
			}

			fileReader.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		ArrayList<ArrayList<String>> allSubSequences = new ArrayList<>();
		ArrayList<Integer> subSeqIndex = new ArrayList<>();
		ArrayList<String> urls_start = new ArrayList<>();
		ArrayList<String> urls_end = new ArrayList<>();

		ArrayList<ArrayList<String>> otherSequences = new ArrayList<>();
		// 2. combine the logs, and record as <global_url_index, sequence>
		for (Integer thread : id2Sequence.keySet()) {

			ArrayList<String> sequence = id2Sequence.get(thread);
			ArrayList<Integer> urlIndexs = id2UrlStartIndex.get(thread);

			if (urlIndexs.isEmpty()) {// is configuration logs
				if (!sequence.isEmpty())
					configsSeqs.add(sequence);
			} else {
				int curr_index = -1; /* the position of urlIndexs */

				for (int i = 0; i < sequence.size();) {
					String url_start = sequence.get(i);
					if (url_start.startsWith("[ReqStart]")) {
						ArrayList<String> subSeq = new ArrayList<>();

						List<String> subList = sequence.subList(i + 1, sequence.size());
						int closed = closedLine(url_start, subList, subSeq);

						if (closed != -1) {
							// closed
							allSubSequences.add(subSeq);
							urls_start.add(url_start);
							urls_end.add(subList.get(closed));
							otherSequences.add(new ArrayList<>());

							curr_index++; /* points to current */
							subSeqIndex.add(urlIndexs.get(curr_index));
							for (String s : subList.subList(0, closed))
								if (s.startsWith("[ReqStart]"))
									curr_index++;

							i = i + 1 + closed + 1;

						} else {
							// this has exception, only record until next [ReqStart]
							subSeq.clear();
							i++;
							int j = i;
							for (; j < sequence.size() - 1; j++) {
								String stmt = sequence.get(j);
								if (stmt.startsWith("[ReqStart]")) {
									// add exception sequence which not complete
									allSubSequences.add(subSeq);
									urls_start.add(url_start);
									urls_end.add("none");
									otherSequences.add(new ArrayList<>());

									curr_index++; /* points to current */
									subSeqIndex.add(urlIndexs.get(curr_index));

									i = j;
									break;
								} else {
									subSeq.add(stmt);
								}
							}
							// add the last
							if (!subSeq.isEmpty() && (j == sequence.size() - 1)) {
								allSubSequences.add(subSeq);
								urls_start.add(url_start);
								urls_end.add("none");
								curr_index++; /* points to current */
								subSeqIndex.add(urlIndexs.get(curr_index));
								otherSequences.add(new ArrayList<>());
							}
						}
					} else {
						// TODO: logs not normal in seq_start&end
						// situation 1: may handle timeout
						// situation 2: req end but log not finish due to exception
						int more = urls_start.size() - otherSequences.size();
						while (more >= 1) {
							otherSequences.add(new ArrayList<>());
							more--;
						}
						if (otherSequences.size() > 0)
							otherSequences.get(otherSequences.size() - 1).add(url_start);

						i++;
						continue;
					}
				}
			}
		}

		// 3. record to answer
		int[] numbers = new int[subSeqIndex.size()];
		int[] indexes = new int[subSeqIndex.size()];
		for (int i = 0; i < subSeqIndex.size(); i++) {
			numbers[i] = subSeqIndex.get(i);
			indexes[i] = i;
		}
		if (subSeqIndex.size() > 2)
			for (int i = 0; i < numbers.length - 1; i++) {
				boolean isSorted = true;
				for (int j = 0; j < numbers.length - 1 - i; j++) {
					if (numbers[j] > numbers[j + 1]) {
						isSorted = false;
						int temp1 = numbers[j];
						numbers[j] = numbers[j + 1];
						numbers[j + 1] = temp1;

						int temp2 = indexes[j];
						indexes[j] = indexes[j + 1];
						indexes[j + 1] = temp2;
					}
				}
				if (isSorted)
					break;
			}

		for (int i : indexes) {
			url_request.add(urls_start.get(i));
			url_response.add(urls_end.get(i));
			seqs.add(allSubSequences.get(i));
			otherSeqs.add(otherSequences.get(i));
			if (urls_end.get(i).equals("none"))
				closed.add(false);
			else
				closed.add(true);
		}

	}

	private int closedLine(String url_start, List<String> sequence, ArrayList<String> subSeq) {
		int ret = -1;/*-1 means not closed*/

		String addr1 = calAddrInReqLine(url_start);
		String code1 = calCodeInReqLine(url_start);

		int same = 1;
		for (int i = 0; i < sequence.size(); i++) {
			String stmt = sequence.get(i);

			if (stmt.startsWith("[ReqStart]")) {
				String addr2 = calAddrInReqLine(stmt);
				String code2 = calCodeInReqLine(stmt);
				if (addr2.equals(addr1) && code2.equals(code1))
					same++;
			} else if (stmt.startsWith("[ReqEnd]")) {
				String addr2 = calAddrInResLine(stmt);
				String code2 = calCodeInResLine(stmt);
				if (addr2.equals(addr1) && code2.equals(code1)) {
					same--;
					if (same == 0)
						return i;
				}
			} else {
				subSeq.add(stmt);
				continue;
			}
		}

		if (ret == -1)
			subSeq.clear();
		return ret;
	}

	private String calAddrInReqLine(String url_start) {
		String uu1 = url_start.substring("[ReqStart]".length());
		String addr1 = uu1.substring(uu1.indexOf(']') + 1, uu1.indexOf("[hashcode]"));
		return addr1;
	}

	private String calAddrInResLine(String line) {
		String uu1 = line.substring("[ReqEnd]".length());
		String addr1 = uu1.substring(uu1.indexOf(']') + 1, uu1.indexOf("[hashcode]"));
		return addr1;
	}

	private String calCodeInReqLine(String url_start) {
		String code1 = url_start.substring(url_start.indexOf("[hashcode]") + "[hashcode]".length(), url_start.indexOf("[method]"));
		return code1;
	}

	private String calCodeInResLine(String line) {
		String code1 = line.substring(line.indexOf("[hashcode]") + "[hashcode]".length(), line.indexOf("[headers-cookie]"));
		return code1;
	}

	private boolean isNewThread(Set<Integer> set, int currThread) {
		for (Integer I : set) {
			if (I.intValue() == currThread) {
				return false;
			}
		}
		return true;
	}

	public ArrayList<String> getUrlRequest() {
		return url_request;
	}

	public ArrayList<String> getUrlResponse() {
		return url_response;
	}

	public ArrayList<ArrayList<String>> getSeqs() {
		return seqs;
	}

	public ArrayList<Boolean> getClosed() {
		return closed;
	}

	public ArrayList<Integer> getThread() {
		return thread;
	}

	public int getLength() {
		if (seqs.size() == url_request.size() && url_request.size() == closed.size() && url_request.size() == url_response.size())
			return url_request.size();
		else
			return -1;
	}

	public ArrayList<ArrayList<String>> getConfigsSeqs() {
		return configsSeqs;
	}

	public ArrayList<ArrayList<String>> getOtherSeqs() {
		return otherSeqs;
	}

}
