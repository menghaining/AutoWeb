package ict.pag.webframework.model.answer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import ict.pag.webframework.model.enumeration.MarkScope;
import ict.pag.webframework.model.marks.EntryMark;
import ict.pag.webframework.model.marks.NormalMark;
import jxl.write.WriteException;

public class BuildMatrixMain_details {
	public HashSet<EntryMark> entryMarkSet = new HashSet<>();
	public HashSet<EntryMark> notEntryMarkSet = new HashSet<>();
	private HashSet<NormalMark> fieldInjectMarks = new HashSet<>();
	private HashSet<NormalMark> notFieldInjectMarks = new HashSet<>();

	/** feathers */
	private ArrayList<String> feathers_entries = new ArrayList<>();
	private ArrayList<String> feathers_injectFields = new ArrayList<>();

	/** feather data */
	private ArrayList<HashSet<Integer>> entryMarksFeathersSet = new ArrayList<>();
	private ArrayList<HashSet<Integer>> notEntryMarksFeathersSet = new ArrayList<>();
	private ArrayList<HashSet<Integer>> injectFieldsMarksFeathersSet = new ArrayList<>();
	private ArrayList<HashSet<Integer>> notInjectFieldsMarksFeathersSet = new ArrayList<>();

	/** verified data */
	private HashSet<HashSet<Integer>> verifiedEntryMarksSet = new HashSet<>();
	private HashSet<HashSet<Integer>> verifiedFieldInjectMarksSet = new HashSet<>();

	public static void main(String[] args) {
		String dirPath = args[0];
		File dir = new File(dirPath);

		HashSet<File> list = new HashSet<>();
		BuildMatrixMain_details builder = new BuildMatrixMain_details();
		builder.findAllJsonFiles(dir, list);

		for (File f : list) {
			builder.extractFrameworkModle_fromJsonFile(f);
		}

		/** build verified data */

		XMLExporter exporter = new XMLExporter();
		try {
			// input data
			exporter.exportPositiveAndNegtiveData(builder.getEntryMarkSet(),
					builder.getNotEntryMarkSet(), "Entry.xls", "data", false);
		} catch (IOException | WriteException e) {
			e.printStackTrace();
		}

	}

	/**
	 * return collection X all subSets
	 * 
	 * @param verifiedEntryMarksSet2
	 */
	public void calSubSet(int[] set, HashSet<HashSet<Integer>> verifiedfSet) {
//		Set<Set<Integer>> result = new HashSet<Set<Integer>>();
		int length = set.length;
		int times = length == 0 ? 0 : 1 << (length);

		for (int i = 0; i < times; i++) {
			HashSet<Integer> subSet = new HashSet<Integer>();

			int index = i;
			for (int j = 0; j < length; j++) {
				if ((index & 1) == 1) {
					subSet.add(set[j]);
				}
				index >>= 1;
			}
			if (!subSet.isEmpty())
				verifiedfSet.add(subSet);
		}
	}

	private void extractFrameworkModle_fromJsonFile(File f) {
		String content;
		try {
			content = FileUtils.readFileToString(f, "UTF-8");
			JSONArray jsonArray = new JSONArray(content);
			jsonArray.forEach(line -> {
				if (line instanceof JSONObject) {
					JSONObject obj = (JSONObject) line;
					String kind = (String) obj.get("kind");
					switch (kind) {
					case "Entry":
						add2EntryMarkSet(obj, entryMarkSet, false);
						break;
					case "No-Entry":
						add2EntryMarkSet(obj, notEntryMarkSet, true);
						break;
					case "Inject-Field":
						add2NormalMarkSet(obj, fieldInjectMarks, MarkScope.Field, "fieldMarks");
						break;
					case "No-Inject-Field":
						add2NormalMarkSet(obj, notFieldInjectMarks, MarkScope.Field, "fieldMarks");
						break;
					}
				}
			});

		} catch (IOException e) {
			System.out.println("[error][IOException]" + f.getAbsolutePath());
		}
	}

	private void add2EntryMarkSet(JSONObject obj, HashSet<EntryMark> entryMarkSet2, boolean b) {
		HashSet<String> classSet = new HashSet<String>();
		HashSet<String> mtdSet = new HashSet<String>();

		if (obj.get("classMarks") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("classMarks"), classSet);

		}
		if (obj.get("methodMarks") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("methodMarks"), mtdSet);
		}
		entryMarkSet2.add(new EntryMark(classSet, mtdSet));
		// debug
		if (b) {
			if (mtdSet.contains("[xml]struts;package;action:method"))
				System.out.println();
			if (mtdSet.contains(
					"[inheritance]javax.servlet.http.HttpServlet.doGet(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"))
				System.out.println();
			if (mtdSet.contains("[anno]Lorg/springframework/web/bind/annotation/GetMapping"))
				System.out.println();
			if (mtdSet.contains("[inheritance]com.opensymphony.xwork2.ActionSupport.execute()Ljava/lang/String;"))
				System.out.println();
			if (mtdSet.contains(
					"[inheritance]org.springframework.context.ApplicationListener.onApplicationEvent(Lorg/springframework/context/ApplicationEvent;)V"))
				System.out.println();
			if (classSet.contains("[inheritance]Lcom/opensymphony/xwork2/ActionSupport"))
				System.out.println();
		}
	}

	private void add2NormalMarkSet(JSONObject obj, HashSet<NormalMark> set, MarkScope m, String str) {
		HashSet<String> fieldSet = new HashSet<String>();
		if (obj.get(str) instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get(str), fieldSet);
		}

		if (!fieldSet.isEmpty()) {
			set.add(new NormalMark(fieldSet, m));
		}
	}

	private void addJsonArray2StringSet(JSONArray jsonArray, HashSet<String> classSet) {
		jsonArray.forEach(n -> {
			classSet.add(n.toString());
		});

	}

	private void findAllJsonFiles(File dir, HashSet<File> list) {
		File[] fileList = dir.listFiles();

		for (File f : fileList) {
			if (f.isFile()) {
				if (f.getAbsolutePath().endsWith(".json")) {
					list.add(f);
				}
			} else if (f.isDirectory()) {
				findAllJsonFiles(f, list);
			}
		}
	}

	public ArrayList<String> getFeathers_entries() {
		return feathers_entries;
	}

	public ArrayList<String> getFeathers_injectFields() {
		return feathers_injectFields;
	}

	public ArrayList<HashSet<Integer>> getEntryMarksFeathersSet() {
		return entryMarksFeathersSet;
	}

	public ArrayList<HashSet<Integer>> getNotEntryMarksFeathersSet() {
		return notEntryMarksFeathersSet;
	}

	public ArrayList<HashSet<Integer>> getInjectFieldsMarksFeathersSet() {
		return injectFieldsMarksFeathersSet;
	}

	public ArrayList<HashSet<Integer>> getNotInjectFieldsMarksFeathersSet() {
		return notInjectFieldsMarksFeathersSet;
	}

	public HashSet<HashSet<Integer>> getVerifiedEntryMarksSet() {
		return verifiedEntryMarksSet;
	}

	public HashSet<HashSet<Integer>> getVerifiedFieldInjectMarksSet() {
		return verifiedFieldInjectMarksSet;
	}

	public HashSet<EntryMark> getEntryMarkSet() {
		return entryMarkSet;
	}

	public HashSet<EntryMark> getNotEntryMarkSet() {
		return notEntryMarkSet;
	}

}
