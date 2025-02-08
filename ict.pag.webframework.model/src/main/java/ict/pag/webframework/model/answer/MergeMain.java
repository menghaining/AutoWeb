package ict.pag.webframework.model.answer;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import ict.pag.webframework.model.enumeration.CallType;
import ict.pag.webframework.model.enumeration.MarkScope;
import ict.pag.webframework.model.enumeration.ValueFrom;
import ict.pag.webframework.model.marks.ConcreteValueMark;
import ict.pag.webframework.model.marks.EntryMark;
import ict.pag.webframework.model.marks.FrmkCallMark;
import ict.pag.webframework.model.marks.NormalMark;

public class MergeMain {
	public static HashSet<EntryMark> entryMarkSet = new HashSet<>();

	private static HashSet<NormalMark> managedClassMarks = new HashSet<>();
	private static HashSet<NormalMark> fieldInjectMarks = new HashSet<>();
	private static HashSet<ConcreteValueMark> fieldPoints2Marks = new HashSet<>();
	private static HashSet<ConcreteValueMark> aliasMarks = new HashSet<>();
	private static HashSet<ConcreteValueMark> frameworkCallReturnPoints2Marks = new HashSet<>();
	private static HashSet<String> mayEntryPointFormalParameterSet = new HashSet<>();

	private static HashSet<FrmkCallMark> frameworkCallMarks = new HashSet<>();

	public static void main(String[] args) {
		String dirPath = args[0];
		File dir = new File(dirPath);

		HashSet<File> list = new HashSet<>();
		findAllJsonFiles(dir, list);

		for (File f : list) {
			extractFrameworkModle_fromJsonFile(f);
		}

		mergeAllAnswers();

		// write to json file
		AnswerExporter exporter = new AnswerExporter(entryMarkSet, managedClassMarks, fieldInjectMarks,
				fieldPoints2Marks, aliasMarks, frameworkCallReturnPoints2Marks, mayEntryPointFormalParameterSet,
				frameworkCallMarks);
		try {
			exporter.export2Json();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void mergeAllAnswers() {
		OptimizeAnswer.resolveEntryAnswer(entryMarkSet);

		OptimizeAnswer.resolveNormalMarkAnswers(managedClassMarks);
		OptimizeAnswer.resolveNormalMarkAnswers(fieldInjectMarks);
		OptimizeAnswer.resovleConcreteValueMarkAnswer(fieldPoints2Marks);
		OptimizeAnswer.resovleConcreteValueMarkAnswer(aliasMarks);
		OptimizeAnswer.resovleConcreteValueMarkAnswer(frameworkCallReturnPoints2Marks);

		frameworkCallMarks = OptimizeAnswer.resolveIndiectCallAnswer(frameworkCallMarks);
	}

	private static void findAllJsonFiles(File dir, HashSet<File> list) {
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

	private static void extractFrameworkModle_fromJsonFile(File f) {
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
						add2EntrySet(obj);
						break;
					case "IndirectCall":
						add2IndirectCallSet(obj);
						break;
					case "Entry-Param":
						add2EntryParam(obj);
						break;
					case "Generate-Class":
						add2GenerateClass(obj);
						break;
					case "Generate-Class-Alias":
						add2ClassAlias(obj);
						break;
					case "Inject-Field":
						add2FieldInject(obj);
						break;
					case "Inject-Field-Points2":
						add2FieldPoints2(obj);
						break;
					case "Return-Points2":
						add2ReturnPoints2(obj);
						break;
					}
				}
			});

		} catch (IOException e) {
			System.out.println("[error][IOException]" + f.getAbsolutePath());
		}
	}

	private static void add2ReturnPoints2(JSONObject obj) {
		HashSet<String> callsSet = new HashSet<String>();
		String callString = obj.get("callStmt").toString();
		callsSet.add(callString);
		ValueFrom val = ValueFrom.valueOf(obj.get("objectFrom").toString());
		String attr = obj.get("objectFromAttribute").toString();

		frameworkCallReturnPoints2Marks.add(new ConcreteValueMark(callsSet, MarkScope.Method, attr, val));
	}

	private static void add2FieldPoints2(JSONObject obj) {
		HashSet<String> fieldSet = new HashSet<String>();
		if (obj.get("fieldMarks") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("fieldMarks"), fieldSet);
		}
		ValueFrom val = ValueFrom.valueOf(obj.get("objectFrom").toString());
		String attr = obj.get("objectFromAttribute").toString();

		if (!fieldSet.isEmpty())
			fieldPoints2Marks.add(new ConcreteValueMark(fieldSet, MarkScope.Field, attr, val));
	}

	private static void add2FieldInject(JSONObject obj) {
		HashSet<String> fieldSet = new HashSet<String>();
		if (obj.get("fieldMarks") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("fieldMarks"), fieldSet);
		}

		if (!fieldSet.isEmpty()) {
			fieldInjectMarks.add(new NormalMark(fieldSet, MarkScope.Field));
		}

	}

	private static void add2ClassAlias(JSONObject obj) {
		HashSet<String> classSet = new HashSet<String>();

		if (obj.get("classMarks") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("classMarks"), classSet);
		}
		ValueFrom val = ValueFrom.valueOf(obj.get("aliasFrom").toString());
		String attr = obj.get("aliasFromAttribute").toString();

		if (!classSet.isEmpty())
			aliasMarks.add(new ConcreteValueMark(classSet, MarkScope.Clazz, attr, val));
	}

	private static void add2GenerateClass(JSONObject obj) {
		HashSet<String> classSet = new HashSet<String>();

		if (obj.get("classMarks") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("classMarks"), classSet);
		}

		if (!classSet.isEmpty()) {
			managedClassMarks.add(new NormalMark(classSet, MarkScope.Clazz));
		}
	}

	private static void add2IndirectCallSet(JSONObject obj) {
		String callString = obj.get("callStmt").toString();

		if (obj.has("paramIndex")) {
			String index = obj.get("paramIndex").toString();
			if (!index.equals("-1")) {
				frameworkCallMarks.add(new FrmkCallMark(CallType.Param, callString, Integer.parseInt(index)));
			}
		} else if (obj.has("methodMarks")) {
			HashSet<String> mtdSet = new HashSet<String>();
			if (obj.get("methodMarks") instanceof JSONArray) {
				addJsonArray2StringSet((JSONArray) obj.get("methodMarks"), mtdSet);
			}
			if (!mtdSet.isEmpty()) {
				frameworkCallMarks.add(new FrmkCallMark(CallType.Attribute, callString, mtdSet));
			}
		}

	}

	private static void add2EntryParam(JSONObject obj) {
		mayEntryPointFormalParameterSet.add(obj.get("paramType").toString());
	}

	private static void add2EntrySet(JSONObject obj) {
		HashSet<String> classSet = new HashSet<String>();
		HashSet<String> mtdSet = new HashSet<String>();

		if (obj.get("classMarks") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("classMarks"), classSet);

		}
		if (obj.get("methodMarks") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("methodMarks"), mtdSet);
		}

		entryMarkSet.add(new EntryMark(classSet, mtdSet));
	}

	private static void addJsonArray2StringSet(JSONArray jsonArray, HashSet<String> classSet) {
		jsonArray.forEach(n -> {
			classSet.add(n.toString());
		});

	}

}
