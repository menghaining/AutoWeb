package ict.pag.webframework.model.answer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import ict.pag.webframework.model.enumeration.CallType;
import ict.pag.webframework.model.enumeration.MarkScope;
import ict.pag.webframework.model.marks.EntryMark;
import ict.pag.webframework.model.marks.FrmkIndirectCallMark;
import ict.pag.webframework.model.marks.NormalMark;
import ict.pag.webframework.model.marks.Points2Mark;
import ict.pag.webframework.model.marks.SequenceCallMark;
import ict.pag.webframework.model.option.SpecialHelper;

public class BuildMatrixMain_Integer {
	private static String suffix = ".json";

	public HashSet<EntryMark> entryMarkSet = new HashSet<>();
	public HashSet<EntryMark> notEntryMarkSet = new HashSet<>();
	private HashSet<NormalMark> fieldInjectMarks = new HashSet<>();
	private HashSet<NormalMark> notFieldInjectMarks = new HashSet<>();

	private HashSet<Points2Mark> points2MarkSet = new HashSet<>();
	private HashSet<Points2Mark> notPoints2MarkSet = new HashSet<>();

	private HashSet<String> classAliasSet = new HashSet<>();

	private HashSet<HashSet<String>> managedSet = new HashSet<>();

	private HashSet<FrmkIndirectCallMark> frameworkCallSet = new HashSet<>();
	private HashSet<FrmkIndirectCallMark> notframeworkCallSet = new HashSet<>();

	private HashSet<SequenceCallMark> callSeqSet = new HashSet<>();
	private HashSet<SequenceCallMark> notCallSeqSet = new HashSet<>();

	/** feathers */
	private ArrayList<String> feathers_entries = new ArrayList<>();
	private ArrayList<String> feathers_injectFields = new ArrayList<>();
	private ArrayList<String> feathers_points2 = new ArrayList<>();
	private ArrayList<String> feathers_indirectCall = new ArrayList<>();
	private ArrayList<String> feathers_callSeq = new ArrayList<>();

	/** feather data */
	private ArrayList<HashSet<Integer>> entryMarksFeathersSet = new ArrayList<>();
	private ArrayList<HashSet<Integer>> notEntryMarksFeathersSet = new ArrayList<>();
	private ArrayList<HashSet<Integer>> injectFieldsMarksFeathersSet = new ArrayList<>();
	private ArrayList<HashSet<Integer>> notInjectFieldsMarksFeathersSet = new ArrayList<>();

	private ArrayList<HashSet<Integer>> points2MarksFeathersSet = new ArrayList<>();
	private ArrayList<HashSet<Integer>> notPoints2MarksFeathersSet = new ArrayList<>();

	private ArrayList<HashSet<Integer>> indirectCallMarksFeathersSet = new ArrayList<>();
	private ArrayList<HashSet<Integer>> notindirectCallMarksFeathersSet = new ArrayList<>();
	
	private ArrayList<HashSet<Integer>> callSeqMarksFeathersSet = new ArrayList<>();
	private ArrayList<HashSet<Integer>> notCallSeqMarksFeathersSet = new ArrayList<>();
	/** verified data */
	private HashSet<HashSet<Integer>> verifiedEntryMarksSet = new HashSet<>();
	private HashSet<HashSet<Integer>> verifiedFieldInjectMarksSet = new HashSet<>();

	public static void main(String[] args) {
		String dirPath = args[0];
		File dir = new File(dirPath);

		HashSet<File> list = new HashSet<>();
		BuildMatrixMain_Integer builder = new BuildMatrixMain_Integer();
		builder.findAllJsonFiles(dir, list);

		for (File f : list) {
			builder.extractFrameworkModle_fromJsonFile(f);
		}

		builder.generateMarksMatrix();

		/** build verified data */
//		// 1. entry
//		builder.buildVerifyMatrix(builder.getEntryMarksFeathersSet(), builder.getVerifiedEntryMarksSet());
//		// 2. field inject
//		builder.buildVerifyMatrix(builder.getInjectFieldsMarksFeathersSet(), builder.getVerifiedFieldInjectMarksSet());
//
//		// analysis the occur frequency of each feathers according to feathers
//		HashMap<String, int[]> feather2PositiveNegative = new HashMap<>();
//		for (HashSet<Integer> s : builder.getEntryMarksFeathersSet()) {
//			for (int i : s) {
//				String feather = builder.getFeathers_entries().get(i);
//				if (feather2PositiveNegative.keySet().contains(feather)) {
//					int[] vals = feather2PositiveNegative.get(feather);
//					vals[0] = vals[0] + 1;
//				} else {
//					int[] vals = new int[2];
//					vals[0] = 1;
//					vals[1] = 0;
//					feather2PositiveNegative.put(feather, vals);
//				}
//			}
//		}
//		for (HashSet<Integer> s : builder.getNotEntryMarksFeathersSet()) {
//			for (int i : s) {
//				String feather = builder.getFeathers_entries().get(i);
//				if (feather2PositiveNegative.keySet().contains(feather)) {
//					int[] vals = feather2PositiveNegative.get(feather);
//					vals[1] = vals[1] + 1;
//				} else {
//					int[] vals = new int[2];
//					vals[0] = 0;
//					vals[1] = 1;
//					feather2PositiveNegative.put(feather, vals);
//				}
//			}
//		}
//		// may in positive and negative
//		for (String key : feather2PositiveNegative.keySet()) {
//			int[] vals = feather2PositiveNegative.get(key);
//			if (vals[0] != 0 && vals[1] != 0)
//				System.out.println(key);
//		}

//		XMLExporter exporter = new XMLExporter();
//		try {
//			exporter.exportPositiveAndNegtiveData(builder.getEntryMarksFeathersSet(),
//					builder.getNotEntryMarksFeathersSet(), builder.getFeathers_entries(), "Entry.xls", "data", false);
//			exporter.exportPositiveAndNegtiveData(builder.getInjectFieldsMarksFeathersSet(),
//					builder.getNotInjectFieldsMarksFeathersSet(), builder.getFeathers_injectFields(), "FieldInject.xls",
//					"data", false);
//			exporter.exportPositiveAndNegtiveData(builder.getPoints2MarksFeathersSet(),
//					builder.getNotPoints2MarksFeathersSet(), builder.getFeathers_points2(), "Points2.xls", "data",
//					false);
//			Exporter.export2TXT(builder.getClassAliasSet(), "classAlias");
//			Exporter.exportSetElement2TXT(builder.getManagedSet(), "managed-class");
//		} catch (IOException | WriteException e) {
//			e.printStackTrace();
//		}

		try {
			XLXSExporter exporter = new XLXSExporter();
			exporter.exportPositiveAndNegtiveData(builder.getEntryMarksFeathersSet(),
					builder.getNotEntryMarksFeathersSet(), builder.getFeathers_entries(), "Entry.xlsx", "data", false);
			exporter.exportPositiveAndNegtiveData(builder.getInjectFieldsMarksFeathersSet(),
					builder.getNotInjectFieldsMarksFeathersSet(), builder.getFeathers_injectFields(),
					"FieldInject.xlsx", "data", false);
			exporter.exportPositiveAndNegtiveData(builder.getPoints2MarksFeathersSet(),
					builder.getNotPoints2MarksFeathersSet(), builder.getFeathers_points2(), "Points2.xlsx", "data",
					false);
			exporter.exportPositiveAndNegtiveData(builder.getIndirectCallMarksFeathersSet(),
					builder.getNotIndirectCallMarksFeathersSet(), builder.getFeathers_indirectCall(),
					"IndirectCall.xlsx", "data", false);
			Exporter.export2TXT(builder.getClassAliasSet(), "classAlias");
			Exporter.exportSetElement2TXT(builder.getManagedSet(), "managed-class");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void buildVerifyMatrix(ArrayList<HashSet<Integer>> feathersSet, HashSet<HashSet<Integer>> verifiedfSet) {

		for (HashSet<Integer> group : feathersSet) {
			int[] arr = new int[group.size()];
			int c = 0;
			for (int i : group) {
				arr[c] = i;
				c++;
			}
			calSubSet(arr, verifiedfSet);
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

	private void generateMarksMatrix() {
		/* 1. entry */
		// 1.1 generate entry marks feathers matrix
		for (EntryMark en : entryMarkSet) {
			HashSet<String> c_marks = en.getAllMarks_class();
			HashSet<String> m_marks = en.getAllMarks_methods();

			HashSet<Integer> marksIndexSet = new HashSet<>();
			calculateMarksIndexSet(c_marks, marksIndexSet, "[class]", feathers_entries);
			calculateMarksIndexSet(m_marks, marksIndexSet, "[method]", feathers_entries);

			entryMarksFeathersSet.add(marksIndexSet);
		}
		// 1.2 not entry marks matrix data
		for (EntryMark en : notEntryMarkSet) {
			HashSet<String> c_marks = en.getAllMarks_class();
			HashSet<String> m_marks = en.getAllMarks_methods();

			HashSet<Integer> marksIndexSet = new HashSet<>();
			calculateMarksIndexSet(c_marks, marksIndexSet, "[class]", feathers_entries);
			calculateMarksIndexSet(m_marks, marksIndexSet, "[method]", feathers_entries);

			notEntryMarksFeathersSet.add(marksIndexSet);
		}

		/* 2. field inject */
		// 2.1 generate inject marks feathers matrix
		for (NormalMark nm : fieldInjectMarks) {
			HashSet<String> allmarks = nm.getAllMarks();

			HashSet<Integer> marksIndexSet = new HashSet<>();
			if (allmarks != null && !allmarks.isEmpty()) {
				calculateMarksIndexSet(allmarks, marksIndexSet, "", feathers_injectFields);
			}
			injectFieldsMarksFeathersSet.add(marksIndexSet);
		}
		// 2.2 not field inject marks matrix data
		for (NormalMark nm : notFieldInjectMarks) {
			HashSet<String> allmarks = nm.getAllMarks();

			HashSet<Integer> marksIndexSet = new HashSet<>();
			if (allmarks != null && !allmarks.isEmpty()) {
				calculateMarksIndexSet(allmarks, marksIndexSet, "", feathers_injectFields);
			}
			notInjectFieldsMarksFeathersSet.add(marksIndexSet);
		}

		/* 3. actual inject */
		// 3.1 is inject
		for (Points2Mark ele : points2MarkSet) {
			HashSet<String> direct_value = ele.getValuePoints2();
			HashSet<String> alias_value = ele.getValueAliasPoints2();
			HashSet<String> field_marks = ele.getFieldMarks();
			if (direct_value.isEmpty() && alias_value.isEmpty())
				if (ele.isDeclare())
					direct_value.add("[declareType]");
//				else
//					System.out.println();
			HashSet<Integer> marksIndexSet = new HashSet<>();
			calculateMarksIndexSet(direct_value, marksIndexSet, "[value]", feathers_points2);
			calculateMarksIndexSet(alias_value, marksIndexSet, "[alias]", feathers_points2);
			calculateMarksIndexSet(field_marks, marksIndexSet, "", feathers_points2);

			points2MarksFeathersSet.add(marksIndexSet);
		}
		// 3.2 irrelevant to inject
		for (Points2Mark ele : notPoints2MarkSet) {
			HashSet<String> direct_value = ele.getValuePoints2();
			HashSet<String> alias_value = ele.getValueAliasPoints2();

			HashSet<Integer> marksIndexSet = new HashSet<>();
			calculateMarksIndexSet(direct_value, marksIndexSet, "[value]", feathers_points2);
			calculateMarksIndexSet(alias_value, marksIndexSet, "[alias]", feathers_points2);

			notPoints2MarksFeathersSet.add(marksIndexSet);
		}
		for (NormalMark nm : notFieldInjectMarks) {
			HashSet<String> allmarks = nm.getAllMarks();

			HashSet<Integer> marksIndexSet = new HashSet<>();
			if (allmarks != null && !allmarks.isEmpty()) {
				calculateMarksIndexSet(allmarks, marksIndexSet, "", feathers_points2);
			}
			notPoints2MarksFeathersSet.add(marksIndexSet);
		}

		/* 4. indirect call */
		processIndirectCallSet(frameworkCallSet, notframeworkCallSet);
		for (FrmkIndirectCallMark ele : frameworkCallSet) {
			HashSet<String> caller = ele.getCallConfig();
			HashSet<String> target = ele.getAllMarks();
			String stmt = ele.getCallStmt();

			HashSet<Integer> marksIndexSet = new HashSet<>();
			calculateMarksIndexSet(caller, marksIndexSet, "[caller]", feathers_indirectCall);
			calculateMarksIndexSet(target, marksIndexSet, "[target]", feathers_indirectCall);
			HashSet<String> tmp = new HashSet<>();
			tmp.add(stmt);
			calculateMarksIndexSet(tmp, marksIndexSet, "[stmt]", feathers_indirectCall);

			indirectCallMarksFeathersSet.add(marksIndexSet);
		}
		for (FrmkIndirectCallMark ele : notframeworkCallSet) {
			HashSet<String> caller = ele.getCallConfig();
			HashSet<String> target = ele.getAllMarks();
			String stmt = ele.getCallStmt();

			HashSet<Integer> marksIndexSet = new HashSet<>();
			calculateMarksIndexSet(caller, marksIndexSet, "[caller]", feathers_indirectCall);
			calculateMarksIndexSet(target, marksIndexSet, "[target]", feathers_indirectCall);
			HashSet<String> tmp = new HashSet<>();
			tmp.add(stmt);
			calculateMarksIndexSet(tmp, marksIndexSet, "[stmt]", feathers_indirectCall);

			notindirectCallMarksFeathersSet.add(marksIndexSet);
		}

		/* 5. call sequence */
		for(SequenceCallMark ele:callSeqSet) {
			HashSet<String> preSet = ele.getPreSet();
			HashSet<String> secSet = ele.getAllMarks();
			
			HashSet<Integer> marksIndexSet = new HashSet<>();
			calculateMarksIndexSet(preSet, marksIndexSet, "[pre]", feathers_indirectCall);
			calculateMarksIndexSet(secSet, marksIndexSet, "[sec]", feathers_indirectCall);
			
			callSeqMarksFeathersSet.add(marksIndexSet);
		}
		for(SequenceCallMark ele:notCallSeqSet) {
			HashSet<String> preSet = ele.getPreSet();
			HashSet<String> secSet = ele.getAllMarks();
			
			HashSet<Integer> marksIndexSet = new HashSet<>();
			calculateMarksIndexSet(preSet, marksIndexSet, "[pre]", feathers_indirectCall);
			calculateMarksIndexSet(secSet, marksIndexSet, "[sec]", feathers_indirectCall);
			
			notCallSeqMarksFeathersSet.add(marksIndexSet);
		}

	}

	private void processIndirectCallSet(HashSet<FrmkIndirectCallMark> frameworkCallSet2,
			HashSet<FrmkIndirectCallMark> notframeworkCallSet2) {
		HashMap<String, HashSet<FrmkIndirectCallMark>> stmt2Set = new HashMap<>();
		// 1. collect all elements that belong to same callstmt
		for (FrmkIndirectCallMark ele : frameworkCallSet2) {
			HashSet<String> caller = ele.getCallConfig();
			HashSet<String> target = ele.getAllMarks();
			String stmt = ele.getCallStmt();

			if (stmt2Set.containsKey(stmt)) {
				stmt2Set.get(stmt).add(ele);
			} else {
				HashSet<FrmkIndirectCallMark> tmp = new HashSet<>();
				tmp.add(ele);
				stmt2Set.put(stmt, tmp);
			}
		}

		// 2. if {A,B,C} and {A,B}, then add {C} into negative
		for (String key : stmt2Set.keySet()) {
			if (stmt2Set.get(key).size() > 1) {
				HashSet<FrmkIndirectCallMark> visited = new HashSet<>();
				for (FrmkIndirectCallMark ele1 : stmt2Set.get(key)) {
					HashSet<String> caller1 = ele1.getCallConfig();
					HashSet<String> target1 = ele1.getAllMarks();

					visited.add(ele1);
					for (FrmkIndirectCallMark ele2 : stmt2Set.get(key)) {
						HashSet<String> caller2 = ele2.getCallConfig();
						HashSet<String> target2 = ele2.getAllMarks();

						if (visited.contains(ele2))
							continue;

						if (SpecialHelper.isSame(caller1, caller2)) {
							boolean preContainsAfter = target1.containsAll(target2);
							boolean afterContainsPre = target2.containsAll(target1);
							if (preContainsAfter && !afterContainsPre) {
								HashSet<String> tmp1 = (HashSet<String>) target1.clone();
								tmp1.removeAll(target2);

								notframeworkCallSet2
										.add(new FrmkIndirectCallMark(caller1, CallType.Attribute, key, tmp1));
							} else if (!preContainsAfter && afterContainsPre) {
								HashSet<String> tmp2 = (HashSet<String>) target2.clone();
								tmp2.removeAll(target1);

								notframeworkCallSet2
										.add(new FrmkIndirectCallMark(caller1, CallType.Attribute, key, tmp2));
							}
						}
					}
				}
			}
		}
	}

	/**
	 * calculate the index Set that contains each element of marks the index of the
	 * element in feathers
	 * 
	 */
	private void calculateMarksIndexSet(HashSet<String> marks, HashSet<Integer> marksIndexSet, String attr,
			ArrayList<String> feathers) {
		for (String mark : marks) {
			String m = attr + mark;
			int index = feathers.indexOf(m);
			if (index == -1) {
				feathers.add(m);
				marksIndexSet.add(feathers.indexOf(m));
			} else {
				marksIndexSet.add(index);
			}
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
						add2EntryMarkSet(obj, notEntryMarkSet, false);
						break;
					case "Inject-Field":
						add2NormalMarkSet(obj, fieldInjectMarks, MarkScope.Field, "fieldMarks", false);
						break;
					case "No-Inject-Field":
						add2NormalMarkSet(obj, notFieldInjectMarks, MarkScope.Field, "fieldMarks", true);
						break;
					case "Field-Points-To":
						add2Points2MarkSet_withFieldInfo(obj, points2MarkSet);
						break;
					case "No-Field-Points-To":
						add2Points2MarkSet(obj, notPoints2MarkSet);
						break;
					case "Class-Alias":
						addSingleString2Set(obj, classAliasSet);
						break;
					case "Class-Managed":
						addSet2Set(obj, managedSet);
						break;
					case "Indirect-Call":
						add2IndirectCallSet(obj, frameworkCallSet);
						break;
					case "No-Indirect-Call":
						add2IndirectCallSet(obj, notframeworkCallSet);
						break;
					case "Call-Sequence":
						add2CallSeq(obj, callSeqSet, notCallSeqSet);
						break;
					}
				}
			});

		} catch (IOException e) {
			System.out.println("[error][IOException]" + f.getAbsolutePath());
		}
	}

	private void add2CallSeq(JSONObject obj, HashSet<SequenceCallMark> callSeqSet2,
			HashSet<SequenceCallMark> notCallSeqSet2) {
		HashSet<String> preSet = new HashSet<String>();
		HashSet<String> secSet = new HashSet<String>();

		if (obj.get("pre") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("pre"), preSet);
		}
		if (obj.get("after") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("after"), secSet);
		}

		callSeqSet.add(new SequenceCallMark(MarkScope.Method, preSet, secSet));
		notCallSeqSet.add(new SequenceCallMark(MarkScope.Method, secSet, preSet));

	}

	private void addSet2Set(JSONObject obj, HashSet<HashSet<String>> managedSet2) {
		HashSet<String> set1 = new HashSet<String>();

		if (obj.get("value") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("value"), set1);
		}

		if (!set1.isEmpty()) {
			managedSet2.add(set1);
		}

	}

	private void addSingleString2Set(JSONObject obj, HashSet<String> classAliasSet2) {

		if (obj.get("value") instanceof String) {
			String val = (String) obj.get("value");
			classAliasSet2.add(val);
		}

	}

	private void add2IndirectCallSet(JSONObject obj, HashSet<FrmkIndirectCallMark> set) {
		String callStmt = null;
		HashSet<String> callerSet = new HashSet<String>();
		HashSet<String> targetSet = new HashSet<String>();

		if (obj.get("invokeStmt") instanceof String) {
			callStmt = (String) obj.get("invokeStmt");
		}
		if (obj.get("caller") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("caller"), callerSet);
		}
		if (obj.get("target") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("target"), targetSet);
		}

		if (callStmt != null && !targetSet.isEmpty()) {
			set.add(new FrmkIndirectCallMark(callerSet, CallType.Attribute, callStmt, targetSet));
		}
	}

	private void add2Points2MarkSet_withFieldInfo(JSONObject obj, HashSet<Points2Mark> points2MarkSet2) {
		HashSet<String> valueDirectSet = new HashSet<String>();
		HashSet<String> valueAliasSet = new HashSet<String>();
		HashSet<String> fieldMarksSet = new HashSet<String>();
		boolean isDeclare = false;

		if (obj.get("isDeclare") instanceof Boolean)
			isDeclare = (Boolean) obj.get("isDeclare");

		if (obj.get("fieldMarks") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("fieldMarks"), fieldMarksSet);
		}
		if (obj.get("valueDirect") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("valueDirect"), valueDirectSet);
		}

		if (obj.get("valueAlias") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("valueAlias"), valueAliasSet);
		}

//		points2MarkSet2.add(new Points2Mark(valueDirectSet, valueAliasSet));
		points2MarkSet2.add(new Points2Mark(isDeclare, fieldMarksSet, valueDirectSet, valueAliasSet));
	}

	private void add2Points2MarkSet(JSONObject obj, HashSet<Points2Mark> points2MarkSet2) {
		HashSet<String> valueDirectSet = new HashSet<String>();
		HashSet<String> valueAliasSet = new HashSet<String>();

		if (obj.get("valueDirect") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("valueDirect"), valueDirectSet);

		}
		if (obj.get("valueAlias") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("valueAlias"), valueAliasSet);
		}

		points2MarkSet2.add(new Points2Mark(valueDirectSet, valueAliasSet));
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
//		if (b) {
//			if (mtdSet.contains("[xml]struts;package;action:method"))
//				System.out.println();
//			if (mtdSet.contains(
//					"[inheritance]javax.servlet.http.HttpServlet.doGet(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"))
//				System.out.println();
//			if (mtdSet.contains("[anno]Lorg/springframework/web/bind/annotation/GetMapping"))
//				System.out.println();
//			if (mtdSet.contains("[inheritance]com.opensymphony.xwork2.ActionSupport.execute()Ljava/lang/String;"))
//				System.out.println();
//			if (mtdSet.contains(
//					"[inheritance]org.springframework.context.ApplicationListener.onApplicationEvent(Lorg/springframework/context/ApplicationEvent;)V"))
//				System.out.println();
//			if (classSet.contains("[inheritance]Lcom/opensymphony/xwork2/ActionSupport"))
//				System.out.println();
//		}
	}

	private void add2NormalMarkSet(JSONObject obj, HashSet<NormalMark> set, MarkScope m, String str, boolean debug) {
		HashSet<String> fieldSet = new HashSet<String>();
		if (obj.get(str) instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get(str), fieldSet);
		}

		if (!fieldSet.isEmpty()) {
			if (debug)
				if (fieldSet.contains("[field][xml]beans;bean;property:name[class]beans;bean"))
					System.out.println();
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
				if (f.getAbsolutePath().endsWith(suffix)) {
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

	public ArrayList<String> getFeathers_points2() {
		return feathers_points2;
	}

	public ArrayList<HashSet<Integer>> getPoints2MarksFeathersSet() {
		return points2MarksFeathersSet;
	}

	public ArrayList<HashSet<Integer>> getNotPoints2MarksFeathersSet() {
		return notPoints2MarksFeathersSet;
	}

	public ArrayList<String> getFeathers_indirectCall() {
		return feathers_indirectCall;
	}

	public ArrayList<HashSet<Integer>> getIndirectCallMarksFeathersSet() {
		return indirectCallMarksFeathersSet;
	}

	public ArrayList<HashSet<Integer>> getNotIndirectCallMarksFeathersSet() {
		return notindirectCallMarksFeathersSet;
	}

	public HashSet<String> getClassAliasSet() {
		return classAliasSet;
	}

	public HashSet<HashSet<String>> getManagedSet() {
		return managedSet;
	}

}
