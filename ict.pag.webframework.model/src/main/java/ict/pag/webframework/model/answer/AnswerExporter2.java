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

import ict.pag.webframework.model.marks.EntryMark;
import ict.pag.webframework.model.marks.NormalMark;
import ict.pag.webframework.model.option.ConfigUtil;

public class AnswerExporter2 {
	private String root;

	private HashSet<EntryMark> entryMarkSet;
	private HashSet<EntryMark> noEntryMarkSet;
	private HashSet<NormalMark> fieldInjectMarks;
	private HashSet<NormalMark> noFieldInjectMarks;

	public AnswerExporter2(HashSet<EntryMark> entry, HashSet<EntryMark> noEntry, HashSet<NormalMark> fieldInject,
			HashSet<NormalMark> noFieldInject) {

		this.entryMarkSet = entry;
		this.noEntryMarkSet = noEntry;
		this.fieldInjectMarks = fieldInject;
		this.noFieldInjectMarks = noFieldInject;

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
//				if (en.getAllMarks_methods().isEmpty())
//					continue;
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
			root = userDir + "result-matrix" + File.separator + subPath + File.separator;
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
