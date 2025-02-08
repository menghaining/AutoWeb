package ict.pag.webframework.preInstrumental;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import ict.pag.webframework.XML.XMLMarksExtractor;
import ict.pag.webframework.log.RequestInfoExtractor;
import ict.pag.webframework.preInstrumental.entity.ModifiedInfo;
import ict.pag.webframework.preInstrumental.entity.ModifiedSemantic;
import ict.pag.webframework.preInstrumental.entity.Url2ModificationInfo;
import javassist.bytecode.ClassFile;

public class ModifyMain {

	public static void main(String[] args) {
		CLIOption cliOptions = new CLIOption(args);
		String appDirPath = cliOptions.getAnalyseDir();/* web app */
		String logPath = cliOptions.getLogFile();/* original log path */
		String libPath = cliOptions.getLibsDir();/* Library path, only exists in directory */
		String externalPath = cliOptions.getExtraDir();/* some jars like javax */
		String outPath = cliOptions.getOutPath();/* the modified content directory */

		boolean isJar = cliOptions.isJar();
		String jarPath = cliOptions.getJarPath();/* active only when isJar==true */

		/* step1. iterate current directory, find all class files and xml files */
		ArrayList<String> classFiles = new ArrayList<>();
		ArrayList<String> xmlFiles = new ArrayList<>();

		File webFile = new File(appDirPath);
		IterateWebApplication.iterateAllClassAndXMLFiles(webFile, classFiles, xmlFiles);

		// collect application classes
		HashSet<String> applicationClasses = new HashSet<String>();
		for (String ele : classFiles) {
			try {
				DataInputStream dis;
				FileInputStream inputStream = new FileInputStream(new File(ele));
				dis = new DataInputStream(inputStream);

				ClassFile classFile = new ClassFile(dis);
				String name = classFile.getName();
				applicationClasses.add(name);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		XMLMarksExtractor xmlex = new XMLMarksExtractor(xmlFiles, applicationClasses);

		RequestInfoExtractor extractor = new RequestInfoExtractor();
		extractor.readAndParse(logPath);

		/* step2. modify configuration */
		// 1. request
		String outPathReq = outPath + File.separator + "out-preModify";
		PreModifier2 modifier1 = new PreModifier2(appDirPath, libPath, externalPath, outPathReq, xmlex, extractor, classFiles, isJar, jarPath);
		modifier1.setThresdhold(5);
		modifier1.modifyRequestSequence();

		// 2. deploy
		String outPathDeploy = outPath + File.separator + "deploy" + File.separator + "out-preModify";
		PreModifier2 modifier2 = new PreModifier2(appDirPath, libPath, externalPath, outPathDeploy, xmlex, extractor, classFiles, isJar, jarPath);
		modifier2.setThresdhold(5);
		modifier2.modifyDeploySequence();

		/* step3. modify details write to file */
		write2File(outPathReq, modifier1);
		write2File(outPathDeploy, modifier2);

		System.out.println("...modified done!");
	}

	private static void write2File(String outPath, PreModifier modifier) {
		File dir = new File(outPath);
		if (!dir.exists())
			dir.mkdirs();
		File outf = new File(outPath + File.separator + "modifiy_details.json");
		System.out.println("MODIFY DETAILS write to " + outf);

		if (!outf.exists()) {
			try {
				outf.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		JSONArray jsonArray_URL = new JSONArray();
		for (Url2ModificationInfo info : modifier.getUrl2ModifyedInfo()) {
			if (info.length() == 0)
				continue;

			JSONArray jsonArray_ele = new JSONArray();
			for (int i = 0; i < info.length(); i++) {
				ModifiedInfo ele = info.getModifiedInfo(i);
				int testcaseIndex = info.getTestcaseIndex(i);
				String triggerurl = info.getTriggerURL(i);

				JSONObject jobj = new JSONObject(new LinkedHashMap<>());
				jobj.put("testcase", testcaseIndex);
				jobj.put("statement", ele.getStatement());
				jobj.put("position", ele.getPosition());
				jobj.put("type", ele.getType());
				jobj.put("way", ele.getWay());
				jobj.put("trigger", triggerurl);

				JSONArray jsonArray_content = new JSONArray();
				if (ele.getConfigurationContent().size() == 1) {
					jobj.put("configurationContent", jsonArray_content);
					JSONObject jjobj = new JSONObject(new LinkedHashMap<>());
					if (ele.getPosition().intValue() == 0)
						jjobj.put("class", ele.getConfigurationContent().get(0));
					else if (ele.getPosition().intValue() == 1)
						jjobj.put("method", ele.getConfigurationContent().get(0));
					if (ele.getPosition().intValue() == -1)
						jjobj.put("field", ele.getConfigurationContent().get(0));
					jsonArray_content.put(jjobj);
				} else {
					// add more configuration when indirect call
					if (ele.getType().equals(ModifiedSemantic.IndirectCall)) {
						if (ele.getConfigurationContent().size() == 5) {
							jobj.put("configurationContent", jsonArray_content);
							JSONObject jjobj = new JSONObject(new LinkedHashMap<>());
							// the target configuration
							if (ele.getPosition().intValue() == 0)
								jjobj.put("class", ele.getConfigurationContent().get(0));
							else if (ele.getPosition().intValue() == 1)
								jjobj.put("method", ele.getConfigurationContent().get(0));
							// caller and target connection
							jjobj.put("caller_class", ele.getConfigurationContent().get(1));
							jjobj.put("caller_method", ele.getConfigurationContent().get(2));
							jjobj.put("target_class", ele.getConfigurationContent().get(3));
							jjobj.put("target_method", ele.getConfigurationContent().get(4));
							jsonArray_content.put(jjobj);
						}
					} else {
						// field and its target
						jobj.put("configurationContent", jsonArray_content);
						JSONObject jjobj = new JSONObject(new LinkedHashMap<>());
						jjobj.put("field", ele.getConfigurationContent().get(0));
						jjobj.put("target", ele.getConfigurationContent().get(1));
						jsonArray_content.put(jjobj);
					}
				}
				JSONArray jsonArray_check = new JSONArray();

				if (ele.getCheckBody().size() == 1) {
					JSONObject jjobj = new JSONObject(new LinkedHashMap<>());
					if (ele.getPosition().intValue() == 0)
						jjobj.put("class", ele.getCheckBody().get(0));
					else if (ele.getPosition().intValue() == 1)
						jjobj.put("method", ele.getCheckBody().get(0));
					if (ele.getPosition().intValue() == -1)
						jjobj.put("field", ele.getCheckBody().get(0));
					jsonArray_check.put(jjobj);
				} else {
					JSONObject jjobj = new JSONObject(new LinkedHashMap<>());
					if (ele.getType().equals(ModifiedSemantic.IndirectCall)) {
						jjobj.put("frameworkCall", ele.getCheckBody().get(0));
						jjobj.put("target", ele.getCheckBody().get(1));
					} else if (ele.getType().equals(ModifiedSemantic.Field_Inject_method)) {
						jjobj.put("field", ele.getCheckBody().get(0));
						jjobj.put("target", ele.getCheckBody().get(1));
					} else {
						jjobj.put("field", ele.getCheckBody().get(0));
						jjobj.put("target", ele.getCheckBody().get(1));
					}

					jsonArray_check.put(jjobj);
				}
				jobj.put("checkBody", jsonArray_check);

				jobj.put("appear", ele.isAppear());

				jsonArray_ele.put(jobj);
			}

			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("url", info.getUrl());
			jsonObject.put("testcases", jsonArray_ele);

			jsonArray_URL.put(jsonObject);
		}

		// write to file
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(outf);
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "utf-8");
			BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
			String jsonString = jsonArray_URL.toString();
			bufferedWriter.write(jsonString);
			bufferedWriter.flush();
			bufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
