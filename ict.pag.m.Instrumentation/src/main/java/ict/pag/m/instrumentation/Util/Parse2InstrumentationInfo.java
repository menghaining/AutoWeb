package ict.pag.m.instrumentation.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ict.pag.m.instrumentation.Entity.InstrumentationPair;

public class Parse2InstrumentationInfo {

	private static String file_path = "F:\\myProject\\webFrameworkInfer\\InstrumentationInfoExtract\\instrumentationInfo.json";

	private static Set<InstrumentationPair> infoSet = new HashSet<InstrumentationPair>();

	private static Set<String> invokeSet = new HashSet<String>();

	public static void parseInfo() throws FileNotFoundException, IOException, ParseException {
		File file = new File(file_path);
		if (!file.exists())
			return;

		Object obj = new JSONParser().parse(new FileReader(file));
		JSONArray array = (JSONArray) obj;
		for (int i = 0; i < array.size(); i++) {
			JSONObject unit = (JSONObject) array.get(i);
			String kind = (String) unit.get("kind");
			String methodSignature = (String) unit.get("methodSignature");
			String belong2MethodSignature = (String) unit.get("belong2MethodSignature");

//			System.out.println(kind + " - " + methodSignature + " - " + belong2MethodSignature);
			infoSet.add(new InstrumentationPair(belong2MethodSignature, methodSignature));
			invokeSet.add(methodSignature);
		}

	}

	public static Set<InstrumentationPair> getInfoSet() {
		return infoSet;
	}

	public static Set<String> getInvokeSet() {
		return invokeSet;
	}
//
//	public static void main(String args[]) {
//		try {
//			Parse2InstrumentationInfo.parseInfo();
//		} catch (IOException | ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
}
