package ict.pag.m.marks2SAInfer.summarizeModels;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import ict.pag.m.marks2SAInfer.answersPrinter.writeAnswers;
import ict.pag.m.marks2SAInfer.util.reducedSetCollection;
import ict.pag.m.marks2SAInfer.util.structual.FrmkCallParamType;
import ict.pag.m.marks2SAInfer.util.structual.FrmkRetPoints2;
import ict.pag.m.marks2SAInfer.util.structual.set2setPair;

public class Conclude {
	private HashSet<File> list;

	/** entries */
	HashSet<set2setPair> entries = new HashSet<>();
	HashSet<String> entries_params = new HashSet<>();

	/** managed class and points-to */
	/* framework managed classes */
	reducedSetCollection managedClasses = new reducedSetCollection();
	/* alias marks */
	HashSet<String> classAlias = new HashSet<>();
	/* the type that actual points-to */
	HashSet<String> objActualPoints2 = new HashSet<>();
	/* return actual type */
	HashSet<FrmkRetPoints2> frmwkRetAcutalClass = new HashSet<>();
	/* Inject field */
	Set<HashSet<String>> fieldsInject = new HashSet<>();

	/** may be Sequence */
	HashSet<set2setPair> callSequence = new HashSet<>();

	/** indirectly call */
	Set<CallsiteM2targetM> indirectCalls = new HashSet<>();

	public Conclude(HashSet<File> list) {
		this.list = list;
	}

	private HashSet<String> allMarks = new HashSet<>();

	public HashSet<String> getAllMarks() {
		return allMarks;
	}

	public void concludes() {
		for (File f : list) {

			try {
				String content = FileUtils.readFileToString(f, "UTF-8");

				JSONArray jsonArray = new JSONArray(content);
				jsonArray.forEach(line -> {
					if (line instanceof JSONObject) {
						JSONObject obj = (JSONObject) line;
						String kind = (String) obj.get("kind");
						switch (kind) {
						case "ENTRY":
							processEntry(obj);
							break;
						case "ENTRY-Param":
							processEntryParam(obj);
							break;
						case "MANAGED-CLASS":
							processManagedClass(obj);
							break;
						case "MANAGED-CLASS-Alias":
							processClassAlias(obj);
							break;
						case "MANAGED-Actual-Class":
							processObjectActualClass(obj);
							break;
						case "MANAGED-Return-Class":
							processReturnActualClass(obj);
							break;
						case "MANAGED-FIELD-Inject":
							processInjectField(obj);
							break;
						case "ORDERED":
							processSequence(obj);
							break;
						case "IndirectInvoke":
							processIndirectlyCall(obj);
							break;
						default:
							break;
						}
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		duplicateEntriesRemove();
	}

	private void processIndirectlyCall(JSONObject obj) {
		String frameworkInvoke = (String) obj.get("frameworkCall");
		String target = (String) obj.get("target");

		JSONArray array1 = (JSONArray) obj.get("callContext");
		HashSet<String> callContext = new HashSet<>();
		if (array1.get(0) instanceof String) {
			callContext.add(array1.get(0).toString());
			recordAllMarks(array1.get(0).toString());
		} else {
			((JSONArray) array1.get(0)).forEach(node -> {
				callContext.add(node.toString());
				// record the marks
				recordAllMarks(node.toString());
			});
		}
		JSONArray array2 = (JSONArray) obj.get("targetMarks");
		HashSet<String> targetMarks = new HashSet<>();
		if (array2.get(0) instanceof String) {
			targetMarks.add(array2.get(0).toString());
			// record the marks
			recordAllMarks(array2.get(0).toString());
		} else {
			((JSONArray) array2.get(0)).forEach(node -> {
				targetMarks.add(node.toString());
				// record the marks
				recordAllMarks(node.toString());
			});
		}

		CallsiteM2targetM c2t = new CallsiteM2targetM(frameworkInvoke, target, callContext, targetMarks);
		if (!inIndirectCalls(c2t))
			indirectCalls.add(c2t);
	}

	private boolean inIndirectCalls(CallsiteM2targetM c2t) {
		for (CallsiteM2targetM i : indirectCalls) {
			if (i.equals(c2t))
				return true;
		}
		return false;
	}

	private void processSequence(JSONObject obj) {
		JSONArray array1 = (JSONArray) obj.get("before");
		HashSet<String> before = new HashSet<>();
		((JSONArray) array1.get(0)).forEach(node -> {
			before.add(node.toString());
			// record the marks
			recordAllMarks(node.toString());

		});
		JSONArray array2 = (JSONArray) obj.get("after");
		HashSet<String> after = new HashSet<>();
		((JSONArray) array2.get(0)).forEach(node -> {
			after.add(node.toString());
			// record the marks
			recordAllMarks(node.toString());
		});
		JSONArray array3 = null;
		if (obj.has("middle")) {
			array3 = (JSONArray) obj.get("middle");
		}

		// if before and after method is same, then delete
		if (isSameMethod(before, after)) {
			return;
		}

		HashMap<set2setPair, set2setPair> old2new = new HashMap<>();
		for (set2setPair en : callSequence) {
			HashSet<String> newFirst = null;
			HashSet<String> newSec = null;
			if (set2setPair.hasSameElement(before, en.getFirst())
					&& set2setPair.hasSameElement(after, en.getSecond())) {
				// 1.1 before
				if (set2setPair.contains(before, en.getFirst())) {
					Set<String> diff = set2setPair.calDifferenceSet(before, en.getFirst());
					if (!diff.isEmpty()) {
						for (String rm : diff) {
							before.remove(rm);
						}
					}
				} else if (set2setPair.contains(en.getFirst(), before)) {
					newFirst = before;
				}

				// 1.2 after
				if (set2setPair.contains(after, en.getSecond())) {
					Set<String> diff = set2setPair.calDifferenceSet(after, en.getSecond());
					if (!diff.isEmpty()) {
						for (String rm : diff) {
							after.remove(rm);
						}
					}
				} else if (set2setPair.contains(en.getSecond(), after)) {
					newSec = after;
				}
			}
			// replace the old
			if (newFirst != null && newSec != null) {
				old2new.put(en, new set2setPair(newFirst, newSec));
			} else if (newFirst != null) {
				old2new.put(en, new set2setPair(newFirst, en.getSecond()));
			} else if (newSec != null) {
				old2new.put(en, new set2setPair(en.getFirst(), newSec));
			}
		}

		/* 2. replace old */
		for (set2setPair key : old2new.keySet()) {
			callSequence.remove(key);
			callSequence.add(old2new.get(key));
		}

		/* 3. add coming */
		if (validAnno(before) && validAnno(after))
			callSequence.add(new set2setPair(before, after));

//		callSequence.add(new set2setPair(before, after));
		System.out.println();

	}

	private boolean isSameMethod(HashSet<String> before, HashSet<String> after) {
		String before_mtd = findMethod(before);
		String after_mtd = findMethod(after);
		if (before_mtd != null && after_mtd != null) {
			if (before_mtd.equals(after_mtd))
				return true;
		}
		return false;
	}

	private String findMethod(HashSet<String> before) {
		for (String m : before) {
			if (m.startsWith("anno:mtd:")) {
				String ret = m.substring("anno:mtd:".length());
				return ret;
			} else if (m.startsWith("inhe:mtd:")) {
				String ret = m.substring("inhe:mtd:".length());
				return ret;
			} else if (m.startsWith("inhe:full:")) {
				String ret = m.substring("inhe:full:".length());
				return ret;
			}
		}
		return null;
	}

	private boolean validAnno(HashSet<String> before) {
		boolean hasAnno = false;
		boolean hasAnno_mtd = false;
		for (String i : before) {
			if (i.startsWith("anno:"))
				hasAnno = true;
			if (i.startsWith("anno:mtd")) {
				hasAnno_mtd = true;
				break;
			}
		}

		if (hasAnno && !hasAnno_mtd)
			return false;

		return true;
	}

	/* do not distinguish the way is field or method */
	private void processInjectField(JSONObject obj) {
		JSONArray array1 = (JSONArray) obj.get("marks");
		HashSet<String> inject = new HashSet<>();
		array1.forEach(node -> {
			inject.add(node.toString());
			// record the marks
			recordAllMarks(node.toString());
		});

		fieldsInject.add(inject);
	}

	private void processReturnActualClass(JSONObject obj) {
		String frameworkInvoke = (String) obj.get("frameworkCall");
		String type = (String) obj.get("type");
		int parameterIndex = (int) obj.get("parameterIndex");
		switch (type) {
		case "FQName_text":
			FrmkRetPoints2 ret1 = new FrmkRetPoints2(frameworkInvoke, parameterIndex, FrmkCallParamType.FQName_text);
			frmwkRetAcutalClass.add(ret1);
			break;
		case "FQName_class":
			FrmkRetPoints2 ret2 = new FrmkRetPoints2(frameworkInvoke, parameterIndex, FrmkCallParamType.FQName_class);
			frmwkRetAcutalClass.add(ret2);
			break;
		case "Alias_text":
			FrmkRetPoints2 ret3 = new FrmkRetPoints2(frameworkInvoke, parameterIndex, FrmkCallParamType.Alias_text);
			frmwkRetAcutalClass.add(ret3);
			break;
		}

		allMarks.add(frameworkInvoke);
	}

	private void processObjectActualClass(JSONObject obj) {
		String mark = (String) obj.get("mark");
		objActualPoints2.add(mark);

		// record the marks
		recordAllMarks(mark);
	}

	private void processClassAlias(JSONObject obj) {
		String mark = (String) obj.get("mark");
		classAlias.add(mark);

		// record the marks
		recordAllMarks(mark);
	}

	private void processManagedClass(JSONObject obj) {
		JSONArray array1 = (JSONArray) obj.get("classMarks");
		HashSet<String> obj_class = new HashSet<>();
		array1.forEach(node -> {
			obj_class.add(node.toString());

			// record
			recordAllMarks(node.toString());
		});

		managedClasses.add(obj_class);

	}

	private void processEntryParam(JSONObject obj) {
		String mark = (String) obj.get("classMarks");
		entries_params.add(mark);
		// record the marks
		recordAllMarks("inhe:class:" + mark);
	}

	private void processEntry(JSONObject obj) {
		JSONArray array1 = (JSONArray) obj.get("classMarks");
		JSONArray array2 = (JSONArray) obj.get("methodMarks");

		HashSet<String> obj_class = new HashSet<>();
		array1.forEach(node -> {
			obj_class.add(node.toString());
			// record the marks
			recordAllMarks(node.toString());
		});
		HashSet<String> obj_mtd = new HashSet<>();
		array2.forEach(node -> {
			obj_mtd.add(node.toString());
			// record the marks
			recordAllMarks(node.toString());
		});
		if (obj_class.size() == 1 && obj_mtd.size() == 1) {
			entries.add(new set2setPair(obj_class, obj_mtd));
			return;
		}

		/* 1. Whether need to merge */
		HashMap<set2setPair, set2setPair> old2new = new HashMap<>();
		for (set2setPair pair : entries) {
			HashSet<String> newFirst = null;
			HashSet<String> newSec = null;

			if (set2setPair.hasSameElement(obj_mtd, pair.getSecond())) {
				// 1.1 class level
				if (set2setPair.contains(obj_class, pair.getFirst())) {
					Set<String> diff = set2setPair.calDifferenceSet(obj_class, pair.getFirst());
					if (!diff.isEmpty()) {
						for (String rm : diff) {
							obj_class.remove(rm);
						}
					}
				} else if (set2setPair.contains(pair.getFirst(), obj_class)) {
					newFirst = obj_class;
				}

				// 1.2 method level
				if (set2setPair.contains(obj_mtd, pair.getSecond())) {
					Set<String> diff = set2setPair.calDifferenceSet(obj_mtd, pair.getSecond());
					if (!diff.isEmpty()) {
						for (String rm : diff) {
							obj_mtd.remove(rm);
						}
					}
				} else if (set2setPair.contains(pair.getSecond(), obj_mtd)) {
					newSec = obj_mtd;
				}
			}

			// replace the old
			if (newFirst != null && newSec != null) {
				old2new.put(pair, new set2setPair(newFirst, newSec));
			} else if (newFirst != null) {
				old2new.put(pair, new set2setPair(newFirst, pair.getSecond()));
			} else if (newSec != null) {
				old2new.put(pair, new set2setPair(pair.getFirst(), newSec));
			}

		}

		/* 2. replace old */
		for (set2setPair key : old2new.keySet()) {
			entries.remove(key);
			entries.add(old2new.get(key));
		}

		/* 3. add coming */
		entries.add(new set2setPair(obj_class, obj_mtd));
	}

	private void duplicateEntriesRemove() {
		// remove duplicated elements
		HashSet<set2setPair> new_entries = new HashSet<>();
		for (set2setPair en : entries) {
			boolean add = true;
			for (set2setPair en2 : new_entries) {
				if (en.equals(en2)) {
					add = false;
					break;
				}
			}
			if (add) {
				new_entries.add(en);
			}
		}
		entries = new_entries;
	}

	public void write2Json() {
		writeAnswers writeAnswers1 = new writeAnswers();

		try {
			long beforeTime5 = System.nanoTime();
			writeAnswers1.writeSummaries(entries, entries_params, managedClasses.getAllElements(), classAlias,
					objActualPoints2, frmwkRetAcutalClass, fieldsInject, callSequence, indirectCalls);
			double buildTime5 = (System.nanoTime() - beforeTime5) / 1E9;
			System.out.println("[TIME-LOG] Exprot Answers Done in " + buildTime5 + " s!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void recordAllMarks(String mark) {
		if (mark.startsWith("anno:class:")) {
			allMarks.add(mark.substring("anno:class:".length()));
		} else if (mark.startsWith("inhe:class:")) {
			allMarks.add(mark.substring("inhe:class:".length()));
		} else if (mark.startsWith("xml:class:")) {
			allMarks.add(mark.substring("xml:class:".length()));
		} else if (mark.startsWith("anno:mtd:")) {
			allMarks.add(mark.substring("anno:mtd:".length()));
		} else if (mark.startsWith("inhe:mtd:")) {
			allMarks.add(mark.substring("inhe:mtd:".length()));
		} else if (mark.startsWith("xml:mtd:")) {
			allMarks.add(mark.substring("xml:mtd:".length()));
		} else if (mark.startsWith("inhe:full:")) {
			allMarks.add(mark.substring("inhe:full:".length()));
		} else if (mark.startsWith("inhe:")) {
			allMarks.add(mark.substring("inhe:".length()));
		} else if (mark.startsWith("xml:")) {
			allMarks.add(mark.substring("xml:".length()));
		} else if (mark.startsWith("anno:")) {
			allMarks.add(mark.substring("anno:".length()));
		}
	}

}
