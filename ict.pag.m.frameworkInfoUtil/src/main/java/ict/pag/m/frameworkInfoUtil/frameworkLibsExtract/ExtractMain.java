package ict.pag.m.frameworkInfoUtil.frameworkLibsExtract;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import org.json.JSONArray;

public class ExtractMain {
	/**
	 * for a class, get all methods belong to it, </br>
	 * including the methods decalred in superclass</br>
	 * example. </br>
	 * ClassA extends ClassB{ function_a(){}}</br>
	 * ClassB{ function_b(){}}</br>
	 * when recording ClassA's methods, including ClassA.function_b();
	 */
	public static void main(String[] args) {
		String lib = args[0];
		ExtractLibs frameworkInheritanceCal = new ExtractLibs(lib);
		HashSet<String> frameworkInheritance = frameworkInheritanceCal.getAllFrameworkInheritanceRelation();

		try {
			writeFile(frameworkInheritance);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void writeFile(HashSet<String> frameworkInheritance) throws IOException {
		// write to json file
		String userDir = System.getProperty("user.dir");
		if (!userDir.endsWith(File.separator))
			userDir = userDir + File.separator;

		File dir = new File(userDir);
		if (!dir.exists())
			dir.mkdirs();

		File file = new File(dir + File.separator + "FrameworkSignatures.json");
		System.out.println("write to " + file);
		if (!file.exists()) {
			file.createNewFile();
		}

		JSONArray jsonArray = new JSONArray();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "utf-8");
		BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

		for (String line : frameworkInheritance) {
//			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
//			jsonObject.put("signature", line);
//			jsonArray.put(jsonObject);
			jsonArray.put(line);
		}

		String jsonString = jsonArray.toString();
		bufferedWriter.write(jsonString);
		bufferedWriter.flush();
		bufferedWriter.close();

	}
}
