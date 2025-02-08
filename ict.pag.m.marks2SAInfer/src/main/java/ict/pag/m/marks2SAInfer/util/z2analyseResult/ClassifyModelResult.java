package ict.pag.m.marks2SAInfer.util.z2analyseResult;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import ict.pag.m.frameworkInfoUtil.customize.ConfigUtil;

public class ClassifyModelResult {
	private static HashSet<String> concerned_frameworkMarks_entry = new HashSet<>();
	private static HashSet<String> concerned_frameworkMarks_entry_params = new HashSet<>();
	
	public static void main(String[] args) {
		String path = args[0];
		parseFrameworkModle_fromJsonFile(path);
	}

	private static void parseFrameworkModle_fromJsonFile(String path) {
		File file_p = new File(path);
		try {
			String content = FileUtils.readFileToString(file_p, "UTF-8");
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
//					case "MANAGED-CLASS":
//						processManagedClass(obj);
//						break;
//					case "MANAGED-CLASS-Alias":
//						processClassAlias(obj);
//						break;
//					case "MANAGED-Actual-Class":
//						processObjectActualClass(obj);
//						break;
//					case "MANAGED-Return-Class":
//						precessFrameworkReturnActualClass(obj);
//						break;
//					case "MANAGED-FIELD-Inject":
//						processInjectField(obj);
//						break;
//					case "ORDERED":
//						processSequence(obj);
//						break;
//					case "IndirectInvoke":
//						processIndirectlyCall(obj);
//						break;
					default:
						break;
					}
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void processEntry(JSONObject obj) {
		JSONArray array1 = (JSONArray) obj.get("classMarks");
		JSONArray array2 = (JSONArray) obj.get("methodMarks");

		array1.forEach(node -> {
			String str = resolveMarks(node.toString());
			if (ConfigUtil.isFrameworkMarks(str)) {
				if(str.contains("("))
					str=str.substring(0,str.lastIndexOf('.'));
				concerned_frameworkMarks_entry.add(str);
				System.out.println(str);
			}
		});
		array2.forEach(node -> {
			String str = resolveMarks(node.toString());
			if (ConfigUtil.isFrameworkMarks(str)) {
				if(str.contains("("))
					str=str.substring(0,str.lastIndexOf('.'));
				concerned_frameworkMarks_entry.add(str);
				System.out.println(str);
			}
		});
	}
	
	private static void processEntryParam(JSONObject obj) {
		String str = (String) obj.get("classMarks");
		if (ConfigUtil.isFrameworkMarks(str)) {
			concerned_frameworkMarks_entry_params.add(str);
			System.out.println("[param]"+str);
		}
	}

	public static String resolveMarks(String mark) {
		if (mark.startsWith("anno:class:")) {
			return mark.substring("anno:class:".length());
		} else if (mark.startsWith("inhe:class:")) {
			return mark.substring("inhe:class:".length());
		} else if (mark.startsWith("xml:class:")) {
			return mark.substring("xml:class:".length());
		} else if (mark.startsWith("anno:mtd:")) {
			return mark.substring("anno:mtd:".length());
		} else if (mark.startsWith("inhe:mtd:")) {
			return mark.substring("inhe:mtd:".length());
		} else if (mark.startsWith("xml:mtd:")) {
			return mark.substring("xml:mtd:".length());
		} else if (mark.startsWith("inhe:full:")) {
			return mark.substring("inhe:full:".length());
		} else if (mark.startsWith("inhe:")) {
			return mark.substring("inhe:".length());
		} else if (mark.startsWith("xml:")) {
			return mark.substring("xml:".length());
		} else if (mark.startsWith("anno:")) {
			return mark.substring("anno:".length());
		}
		return mark;
	}
}
