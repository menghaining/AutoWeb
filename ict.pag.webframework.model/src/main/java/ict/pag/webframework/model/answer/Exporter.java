package ict.pag.webframework.model.answer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import ict.pag.webframework.model.core.calculator.CallSequenceCalculator;
import ict.pag.webframework.model.core.calculator.EntryCalculator2;
import ict.pag.webframework.model.core.calculator.FieldInjectCalculator2;
import ict.pag.webframework.model.core.calculator.IndirectCallCalculator2;
import ict.pag.webframework.model.marks.EntryMark;
import ict.pag.webframework.model.marks.FrmkIndirectCallMark;
import ict.pag.webframework.model.marks.NormalMark;
import ict.pag.webframework.model.marks.Points2Mark;
import ict.pag.webframework.model.marks.SequenceCallMark;
import ict.pag.webframework.model.option.ConfigUtil;

public class Exporter {
	private static String userDir = System.getProperty("user.dir");

	public static void export2Json(EntryCalculator2 entryCalculator, FieldInjectCalculator2 fieldCalculator)
			throws IOException {
		String dir = initFileLocation();

		String outf = ConfigUtil.g().getOutputFile();
		if (outf == null)
			outf = "FrameworkConfigs";
		File file = new File(dir + outf + ".json");
		System.out.println("ANSWERS write to " + file);
		if (!file.exists()) {
			file.createNewFile();
		}

		/* write to json file */
		JSONArray jsonArray = new JSONArray();
		/*
		 * 1. entry
		 */
		HashSet<EntryMark> entryMarkSet = entryCalculator.getEntryMarkSet();
		HashSet<EntryMark> noEntryMarkSet = entryCalculator.getNotEntryMarkSet();
		if (entryMarkSet != null) {
			for (EntryMark en : entryMarkSet) {
				JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
				jsonObject.put("kind", "Entry");
				jsonObject.put("classMarks", en.getAllMarks_class());
				jsonObject.put("methodMarks", en.getAllMarks_methods());

				jsonArray.put(jsonObject);
			}
		}
		if (entryMarkSet != null) {
			for (EntryMark en : noEntryMarkSet) {
				JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
				jsonObject.put("kind", "No-Entry");
				jsonObject.put("classMarks", en.getAllMarks_class());
				jsonObject.put("methodMarks", en.getAllMarks_methods());

				jsonArray.put(jsonObject);
			}
		}

		/*
		 * 2. field inject, points-to, alias
		 */
		HashSet<NormalMark> fieldInjectMarks = fieldCalculator.getFieldInjectMarks();
		HashSet<NormalMark> noFieldInjectMarks = fieldCalculator.getFieldNOTInjectMarks();
		if (fieldInjectMarks != null) {
			for (NormalMark ele : fieldInjectMarks) {
				JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
				jsonObject.put("kind", "Inject-Field");
				jsonObject.put("fieldMarks", ele.getAllMarks());

				jsonArray.put(jsonObject);
			}
		}
		if (fieldInjectMarks != null) {
			for (NormalMark ele : noFieldInjectMarks) {
				JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
				jsonObject.put("kind", "No-Inject-Field");
				jsonObject.put("fieldMarks", ele.getAllMarks());

				jsonArray.put(jsonObject);
			}
		}

		HashSet<Points2Mark> fieldPoints2Marks = fieldCalculator.getFieldPoints2Marks();
		HashSet<Points2Mark> fieldNOTPoints2Marks = fieldCalculator.getFieldNOTPoints2Marks();
		for (Points2Mark mark : fieldPoints2Marks) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("kind", "Field-Points-To");
			jsonObject.put("valueDirect", mark.getValuePoints2());
			jsonObject.put("valueAlias", mark.getValueAliasPoints2());
			jsonObject.put("fieldMarks", mark.getFieldMarks());
			jsonObject.put("isDeclare", mark.isDeclare());

			jsonArray.put(jsonObject);
		}
		for (Points2Mark mark : fieldNOTPoints2Marks) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("kind", "No-Field-Points-To");
			jsonObject.put("valueDirect", mark.getValuePoints2());
			jsonObject.put("valueAlias", mark.getValueAliasPoints2());

			jsonArray.put(jsonObject);
		}

		HashSet<String> classAliasMarks = fieldCalculator.getClassAliasMarks();
		for (String mark : classAliasMarks) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("kind", "Class-Alias");
			jsonObject.put("value", mark);

			jsonArray.put(jsonObject);
		}

		HashSet<NormalMark> managed = fieldCalculator.getManagedClassMarks();
		for (NormalMark ele : managed) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("kind", "Class-Managed");
			jsonObject.put("value", ele.getAllMarks());

			jsonArray.put(jsonObject);
		}

		// write to file
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "utf-8");
		BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
		String jsonString = jsonArray.toString();
		bufferedWriter.write(jsonString);
		bufferedWriter.flush();
		bufferedWriter.close();
	}

	/* export entry, inject, indirect call */
	public static void export2Json(EntryCalculator2 entryCalculator, FieldInjectCalculator2 fieldCalculator,
			IndirectCallCalculator2 indirectCallCalculator, CallSequenceCalculator callSeqCalculator)
			throws IOException {
		String dir = initFileLocation();

		String outf = ConfigUtil.g().getOutputFile();
		if (outf == null)
			outf = "FrameworkConfigs";
		File file = new File(dir + outf + ".json");
		System.out.println("ANSWERS write to " + file);
		if (!file.exists()) {
			file.createNewFile();
		}

		/* write to json file */
		JSONArray jsonArray = new JSONArray();
		/*
		 * 1. entry
		 */
		HashSet<EntryMark> entryMarkSet = entryCalculator.getEntryMarkSet();
		HashSet<EntryMark> noEntryMarkSet = entryCalculator.getNotEntryMarkSet();
		if (entryMarkSet != null) {
			for (EntryMark en : entryMarkSet) {
				JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
				jsonObject.put("kind", "Entry");
				jsonObject.put("classMarks", en.getAllMarks_class());
				jsonObject.put("methodMarks", en.getAllMarks_methods());

				jsonArray.put(jsonObject);
			}
		}
		if (entryMarkSet != null) {
			for (EntryMark en : noEntryMarkSet) {
				JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
				jsonObject.put("kind", "No-Entry");
				jsonObject.put("classMarks", en.getAllMarks_class());
				jsonObject.put("methodMarks", en.getAllMarks_methods());

				jsonArray.put(jsonObject);
			}
		}

		/*
		 * 2. field inject, points-to, alias
		 */
		HashSet<NormalMark> fieldInjectMarks = fieldCalculator.getFieldInjectMarks();
		HashSet<NormalMark> noFieldInjectMarks = fieldCalculator.getFieldNOTInjectMarks();
		if (fieldInjectMarks != null) {
			for (NormalMark ele : fieldInjectMarks) {
				JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
				jsonObject.put("kind", "Inject-Field");
				jsonObject.put("fieldMarks", ele.getAllMarks());

				jsonArray.put(jsonObject);
			}
		}
		if (fieldInjectMarks != null) {
			for (NormalMark ele : noFieldInjectMarks) {
				JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
				jsonObject.put("kind", "No-Inject-Field");
				jsonObject.put("fieldMarks", ele.getAllMarks());

				jsonArray.put(jsonObject);
			}
		}

		HashSet<Points2Mark> fieldPoints2Marks = fieldCalculator.getFieldPoints2Marks();
		HashSet<Points2Mark> fieldNOTPoints2Marks = fieldCalculator.getFieldNOTPoints2Marks();
		for (Points2Mark mark : fieldPoints2Marks) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("kind", "Field-Points-To");
			jsonObject.put("valueDirect", mark.getValuePoints2());
			jsonObject.put("valueAlias", mark.getValueAliasPoints2());
			jsonObject.put("fieldMarks", mark.getFieldMarks());
			jsonObject.put("isDeclare", mark.isDeclare());

			jsonArray.put(jsonObject);
		}
		for (Points2Mark mark : fieldNOTPoints2Marks) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("kind", "No-Field-Points-To");
			jsonObject.put("valueDirect", mark.getValuePoints2());
			jsonObject.put("valueAlias", mark.getValueAliasPoints2());

			jsonArray.put(jsonObject);
		}

		HashSet<String> classAliasMarks = fieldCalculator.getClassAliasMarks();
		for (String mark : classAliasMarks) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("kind", "Class-Alias");
			jsonObject.put("value", mark);

			jsonArray.put(jsonObject);
		}

		HashSet<NormalMark> managed = fieldCalculator.getManagedClassMarks();
		for (NormalMark ele : managed) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("kind", "Class-Managed");
			jsonObject.put("value", ele.getAllMarks());

			jsonArray.put(jsonObject);
		}

		/* 3. indirect call */
		HashSet<FrmkIndirectCallMark> marks = indirectCallCalculator.getFrameworkCallMarks();
		for (FrmkIndirectCallMark mark : marks) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("kind", "Indirect-Call");
			jsonObject.put("invokeStmt", mark.getCallStmt());
			jsonObject.put("caller", mark.getCallConfig());
			jsonObject.put("target", mark.getAllMarks());

			jsonArray.put(jsonObject);
		}

		HashSet<FrmkIndirectCallMark> marks_neg = indirectCallCalculator.getNotFrameworkCallMarks();
		for (FrmkIndirectCallMark mark : marks_neg) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("kind", "No-Indirect-Call");
			jsonObject.put("invokeStmt", mark.getCallStmt());
			jsonObject.put("caller", mark.getCallConfig());
			jsonObject.put("target", mark.getAllMarks());

			jsonArray.put(jsonObject);
		}

		/* 4. sequence call */
		for (SequenceCallMark mark : callSeqCalculator.getSeqConfigSet()) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("kind", "Call-Sequence");
			jsonObject.put("pre", mark.getPreSet());
			jsonObject.put("after", mark.getAllMarks());

			jsonArray.put(jsonObject);
		}

		// write to file
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "utf-8");
		BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
		String jsonString = jsonArray.toString();
		bufferedWriter.write(jsonString);
		bufferedWriter.flush();
		bufferedWriter.close();
	}

	/**
	 * write set to txt; and each element per line
	 * 
	 * @throws IOException
	 */
	public static void export2TXT(HashSet<String> runtimeMarks, String out1) throws IOException {
		String dir = initFileLocation();

		String outf = ConfigUtil.g().getOutputFile();
		if (outf == null)
			outf = "FrameworkConfigs" + "-" + out1;

		File file = new File(dir + outf + ".txt");
		System.out.println("RUN TIME CONFIGURATIONS write to " + file);
		if (!file.exists()) {
			file.createNewFile();
		}

		FileOutputStream fileOutputStream = new FileOutputStream(file);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "utf-8");
		BufferedWriter bw = new BufferedWriter(outputStreamWriter);

		for (String s : runtimeMarks) {
			bw.write(s);
			// System.getProperty("line.separator")
			bw.newLine();
		}

		bw.flush();
		bw.close();

	}

	/**
	 * write set to txt; and each element per line
	 * 
	 * @throws IOException
	 */
	public static void exportSetElement2TXT(HashSet<HashSet<String>> runtimeMarks, String out1) throws IOException {
		String dir = initFileLocation();

		String outf = ConfigUtil.g().getOutputFile();
		if (outf == null)
			outf = "FrameworkConfigs-managed" + "-" + out1;

		File file = new File(dir + outf + ".txt");
		System.out.println("RUN TIME CONFIGURATIONS write to " + file);
		if (!file.exists()) {
			file.createNewFile();
		}

		FileOutputStream fileOutputStream = new FileOutputStream(file);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "utf-8");
		BufferedWriter bw = new BufferedWriter(outputStreamWriter);

		for (HashSet<String> ele : runtimeMarks) {
			String res = "";
			for (String s : ele) {
				if (res.length() == 0) {
					res = res + s;
				} else {
					res = res + "," + s;
				}
			}
			bw.write(res);
			// System.getProperty("line.separator")
			bw.newLine();
		}

		bw.flush();
		bw.close();

	}

	/**
	 * create result directory to save result</br>
	 * at userDir/result-new/testcaseName/
	 */
	private static String initFileLocation() {
		String root;
		String subPath = ConfigUtil.appKind;

		if (!userDir.endsWith(File.separator)) {
			userDir = userDir + File.separator;
		}
		if (!subPath.equals("") && subPath.length() > 0) {
			root = userDir + "result-new" + File.separator + subPath + File.separator;
		} else {
			String tmpString = ConfigUtil.g().getAnalyseDir();
			if (tmpString.contains("/")) {
				tmpString = tmpString.substring(tmpString.lastIndexOf('/'));
				if (tmpString.endsWith("/"))
					tmpString = tmpString.substring(0, tmpString.length() - 1);
			} else if (tmpString.contains("\\")) {
				tmpString = tmpString.substring(tmpString.lastIndexOf('\\'));
				if (tmpString.endsWith("\\"))
					tmpString = tmpString.substring(0, tmpString.length() - 1);
			}
			root = userDir + "result-new" + File.separator + tmpString + File.separator;
		}
		File dir = new File(root);
		if (!dir.exists())
			dir.mkdirs();

		return root;

	}

}
