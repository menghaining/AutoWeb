package ict.pag.webframework.model.answer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import ict.pag.webframework.model.enumeration.CallType;
import ict.pag.webframework.model.marks.ConcreteValueMark;
import ict.pag.webframework.model.marks.EntryMark;
import ict.pag.webframework.model.marks.FrmkCallMark;
import ict.pag.webframework.model.marks.NormalMark;
import ict.pag.webframework.model.option.ConfigUtil;

public class AnswerExporter {
	private String root;

	private HashSet<EntryMark> entryMarkSet;
	private HashSet<NormalMark> managedClassMarks;
	private HashSet<NormalMark> fieldInjectMarks;
	private HashSet<ConcreteValueMark> fieldPoints2Marks;
	private HashSet<ConcreteValueMark> aliasMarks;
	private HashSet<ConcreteValueMark> frameworkCallReturnPoints2Marks;
	private HashSet<String> mayEntryPointFormalParameterSet;
	private HashSet<FrmkCallMark> frameworkCallMarks;

	public AnswerExporter(HashSet<EntryMark> entryMarkSet, HashSet<NormalMark> managedClassMarks,
			HashSet<NormalMark> fieldInjectMarks, HashSet<ConcreteValueMark> fieldPoints2Marks,
			HashSet<ConcreteValueMark> aliasMarks, HashSet<ConcreteValueMark> frameworkCallReturnPoints2Marks,
			HashSet<String> mayEntryPointFormalParameterSet, HashSet<FrmkCallMark> frameworkCallMarks) {

		this.entryMarkSet = entryMarkSet;
		this.managedClassMarks = managedClassMarks;
		this.fieldInjectMarks = fieldInjectMarks;
		this.fieldPoints2Marks = fieldPoints2Marks;
		this.aliasMarks = aliasMarks;
		this.frameworkCallReturnPoints2Marks = frameworkCallReturnPoints2Marks;
		this.mayEntryPointFormalParameterSet = mayEntryPointFormalParameterSet;
		this.frameworkCallMarks = frameworkCallMarks;

		createResultDir();
	}

	public void export2Json() throws IOException {
		long beforeTime = System.nanoTime();

		File file;
		String subName = ConfigUtil.appKind;
		if (!subName.equals("") && subName.length() > 0) {
			file = new File(root + subName + ".json");
		} else {
			file = new File(root + "FrameworkSemantics.json");
		}
		System.out.println("ANSWERS write to " + file);
		if (!file.exists()) {
			file.createNewFile();
		}

		JSONArray jsonArray = new JSONArray();

		/*
		 * 1. entry
		 */
		if (entryMarkSet != null) {
			for (EntryMark en : entryMarkSet) {
				JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
				jsonObject.put("kind", "Entry");
				jsonObject.put("classMarks", en.getAllMarks_class());
				jsonObject.put("methodMarks", en.getAllMarks_methods());

				jsonArray.put(jsonObject);
			}
		}

		if (mayEntryPointFormalParameterSet != null) {
			for (String param : mayEntryPointFormalParameterSet) {
				JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
				jsonObject.put("kind", "Entry-Param");
				jsonObject.put("paramType", param);

				jsonArray.put(jsonObject);
			}
		}

		/*
		 * 2. field inject, points-to, alias
		 */
		if (managedClassMarks != null) {
			for (NormalMark ele : managedClassMarks) {
				JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
				jsonObject.put("kind", "Generate-Class");
				jsonObject.put("classMarks", ele.getAllMarks());

				jsonArray.put(jsonObject);
			}
		}
		if (aliasMarks != null) {
			for (ConcreteValueMark ele : aliasMarks) {
				JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
				jsonObject.put("kind", "Generate-Class-Alias");
				jsonObject.put("classMarks", ele.getAllMarks());
				jsonObject.put("aliasFrom", ele.getVal());
				jsonObject.put("aliasFromAttribute", ele.getAttributeName());

				jsonArray.put(jsonObject);
			}
		}
		if (fieldInjectMarks != null) {
			for (NormalMark ele : fieldInjectMarks) {
				JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
				jsonObject.put("kind", "Inject-Field");
				jsonObject.put("fieldMarks", ele.getAllMarks());

				jsonArray.put(jsonObject);
			}
		}
		if (fieldPoints2Marks != null) {
			for (ConcreteValueMark ele : fieldPoints2Marks) {
				JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
				jsonObject.put("kind", "Inject-Field-Points2");
				jsonObject.put("fieldMarks", ele.getAllMarks());
				jsonObject.put("objectFrom", ele.getVal());
				jsonObject.put("objectFromAttribute", ele.getAttributeName());

				jsonArray.put(jsonObject);
			}
		}
		if (frameworkCallReturnPoints2Marks != null) {
			for (ConcreteValueMark ele : frameworkCallReturnPoints2Marks) {
				HashSet<String> tmpset = ele.getAllMarks();
				String call = null;
				for (String t : tmpset) {
					call = t;
					break;
				}
				if (call == null)
					continue;

				JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
				jsonObject.put("kind", "Return-Points2");
				jsonObject.put("callStmt", call);
				jsonObject.put("objectFrom", ele.getVal());
				jsonObject.put("objectFromAttribute", ele.getAttributeName());

				jsonArray.put(jsonObject);
			}
		}

		/*
		 * 3. indirect calls
		 */
		if (frameworkCallMarks != null) {
			for (FrmkCallMark ele : frameworkCallMarks) {
				JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
				jsonObject.put("kind", "IndirectCall");

				jsonObject.put("callStmt", ele.getCallStmt());
				if (CallType.Param.equals(ele.getType())) {
					if (ele.getParamIndex() != -1) {
						jsonObject.put("paramIndex", ele.getParamIndex());
					}
				} else {
					if (ele.getAllMarks() != null && !ele.getAllMarks().isEmpty()) {
						jsonObject.put("methodMarks", ele.getAllMarks());
					}
				}

				jsonArray.put(jsonObject);
			}
		}

		// write to file
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "utf-8");
		BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
		String jsonString = jsonArray.toString();
		bufferedWriter.write(jsonString);
		bufferedWriter.flush();
		bufferedWriter.close();

		double buildTime = (System.nanoTime() - beforeTime) / 1E9;
		System.out.println("[TIME-LOG] Answer Export Done in " + buildTime + " s!");
	}

	private void createResultDir() {
		String subPath = ConfigUtil.appKind;

		String userDir = System.getProperty("user.dir");
		if (!userDir.endsWith(File.separator)) {
			userDir = userDir + File.separator;
		}
		if (!subPath.equals("") && subPath.length() > 0) {
			root = userDir + "result" + File.separator + subPath + File.separator;
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
			root = userDir + "result" + File.separator + tmpString + File.separator;
		}
		File dir = new File(root);
		if (!dir.exists())
			dir.mkdirs();
	}
}
