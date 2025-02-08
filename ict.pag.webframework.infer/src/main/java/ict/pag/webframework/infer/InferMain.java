package ict.pag.webframework.infer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;

import ict.pag.webframework.infer.WriteHelper.WriteHelper;
import ict.pag.webframework.infer.graph.GraphBuilder;
import ict.pag.webframework.infer.infoCollector.CollectMain;
import ict.pag.webframework.infer.marks.ClassMethodPair;
import ict.pag.webframework.infer.marks.One2OnePair;
import ict.pag.webframework.log.dynamicCG.MethodCalledType;

public class InferMain {
	static String outDir = System.getProperty("user.dir") + File.separator + "outs";

	public static void main(String[] args) {
		// single
//		singlePolicy(args);
		// read from configuration file
		try {
			useConfigFile(args);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void useConfigFile(String[] args) throws IOException {
//		String confFilePath = "F:\\myProject\\webframeworkmodelinfer\\ict.pag.webframework.preInstrumental\\config2.json";
		String confFilePath = "F:\\myProject\\webframeworkmodelinfer\\ict.pag.webframework.preInstrumental\\config_withJackEE.json";
		File confFile = new File(confFilePath);
		String testcaseBasicInfoPath = "F:\\myProject\\webframeworkmodelinfer\\ict.pag.webframework.infer\\outs\\testcaseInfo";

		ArrayList<Infer> infers = new ArrayList<>();
		ArrayList<String> apps = new ArrayList<>();

		/**
		 * 1. infer
		 */
		long beginTime1 = System.currentTimeMillis();
		try {
			String content = FileUtils.readFileToString(confFile, "UTF-8");
			JSONArray jsonArray = new JSONArray(content);

			jsonArray.forEach(line -> {
				if (line instanceof JSONObject) {
					JSONObject obj = (JSONObject) line;
					String logDir = (String) obj.get("logDir");
					String webappDir = (String) obj.get("webappDir");

					Infer infer = new Infer();

					Date date = new Date();
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					System.out.println("[" + formatter.format(date) + "] ...handling " + webappDir);

					GraphBuilder builder = new GraphBuilder(webappDir, true);
					ClassHierarchy cha = builder.getCHA();
					CHACallGraph chaCG = builder.getAppCHACG();

					// request log
					String runningLogsDir = logDir + File.separator + "testcaseLogs";
					String recordJson = logDir + File.separator + "out-preModify" + File.separator + "modifiy_details.json";
					infer.calculate(runningLogsDir, recordJson, cha, chaCG, MethodCalledType.request);

					// deploy log
					String deployLogsDir = logDir + File.separator + "deploy" + File.separator + "testcaseLogs";
					String deployRecordJson = logDir + File.separator + "deploy" + File.separator + "out-preModify" + File.separator + "modifiy_details.json";
					infer.calculate(deployLogsDir, deployRecordJson, cha, chaCG, MethodCalledType.configuration);

					infers.add(infer);

					File f = new File(webappDir);
					String appName = f.getName();
					apps.add(appName);

				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		long endTime1 = System.currentTimeMillis();
		System.out.println("1. infer process costs " + (endTime1 - beginTime1) / 1000 + " s");

		/**
		 * 2. collect all testcase basic information, including all configuration and
		 * reachable information
		 */
		File tmp_f = new File(testcaseBasicInfoPath);
		if (!tmp_f.exists()) {
			long beginTime2 = System.currentTimeMillis();
			CollectMain.collect();
			long endTime2 = System.currentTimeMillis();
			System.out.println("2. collect basic information process costs " + (endTime2 - beginTime2) / 1000 + " s");
		} else {
			System.out.println("2. use basic information alread exists");
		}

		/**
		 * 3. compare and write to file
		 */
		/* all answers */
		HashSet<One2OnePair> entryMarks_all = new HashSet<>();
		HashSet<String> entrySingleClasses_all = new HashSet<>();
		HashSet<String> fieldInjectMarks_field_all = new HashSet<>();
		HashSet<String> fieldInjectMarks_method_all = new HashSet<>();
		HashSet<One2OnePair> field2Targets_all = new HashSet<>();
		HashMap<String, HashSet<One2OnePair>> call2Marks_all = new HashMap<>();

		/* for calculate diff */
		HashSet<String> classMarks_reachable_all = new HashSet<>();
		HashSet<String> methodMarks_reachable_all = new HashSet<>();
		HashSet<String> fieldMarks_reachable_all = new HashSet<>();
		HashSet<String> cMarks_all = new HashSet<>();
		HashSet<String> mMarks_all = new HashSet<>();
		HashSet<String> fMarks_all = new HashSet<>();
		/* each */
		long beginTime3 = System.currentTimeMillis();
		for (int i = 0; i < infers.size(); i++) {
			/* for compare */
			HashSet<String> cMarks = new HashSet<>();
			HashSet<String> mMarks = new HashSet<>();
			HashSet<String> fMarks = new HashSet<>();

			Infer infer = infers.get(i);
			String appName = apps.get(i);
			// 1. entry
			HashSet<One2OnePair> entryMarks = new HashSet<>();
			for (ClassMethodPair entryMark : infer.getEntryConfigs()) {
				for (String m : entryMark.getAllMarks_mtd()) {
					mMarks.add(m);
					for (String c : entryMark.getAllMarks_class()) {
						cMarks.add(c);
						One2OnePair pair = new One2OnePair(c, m);
						if (!One2OnePair.setContains(entryMarks, pair))
							entryMarks.add(pair);
					}
					if (entryMark.getAllMarks_class().isEmpty()) {
						One2OnePair pair = new One2OnePair(null, m);
						if (!One2OnePair.setContains(entryMarks, pair))
							entryMarks.add(pair);
					}
				}
			}
			for (One2OnePair one : entryMarks) {
				if (!One2OnePair.setContains(entryMarks_all, one))
					entryMarks_all.add(one);
			}
			if (!infer.getEntrySingleClasses().isEmpty()) {
				entrySingleClasses_all.addAll(infer.getEntrySingleClasses());
				cMarks.addAll(infer.getEntrySingleClasses());
			}
			// 2. inject
			HashSet<String> fieldInjectMarks_field = new HashSet<>();
			for (HashSet<String> injects : infer.getFieldInject_onFieldConfigs()) {
				for (String inject : injects) {
					fieldInjectMarks_field.add(inject);
					fieldInjectMarks_field_all.add(inject);
					fMarks.add(inject);
				}
			}
			HashSet<String> fieldInjectMarks_method = new HashSet<>();
			for (HashSet<String> injects : infer.getFieldInject_onMethodConfigs()) {
				for (String inject : injects) {
					fieldInjectMarks_method.add(inject);
					fieldInjectMarks_method_all.add(inject);
					mMarks.add(inject);
				}
			}
			HashSet<One2OnePair> field2Targets = new HashSet<>();
			for (String field : infer.getField2TargetsConfigs().keySet()) {
				fMarks.add(field);
				for (String target : infer.getField2TargetsConfigs().get(field)) {
					cMarks.add(target);
					One2OnePair pair = new One2OnePair(field, target);
					if (!One2OnePair.setContains(field2Targets, pair))
						field2Targets.add(pair);
				}
			}
			for (One2OnePair one : field2Targets) {
				if (!One2OnePair.setContains(field2Targets_all, one))
					field2Targets_all.add(one);
			}
			// 3. indirect call
			HashMap<String, HashSet<One2OnePair>> call2Marks = new HashMap<>();
			for (String call : infer.getIndirectCallConfigs().keySet()) {
				if (!call2Marks.containsKey(call))
					call2Marks.put(call, new HashSet<One2OnePair>());
				HashSet<One2OnePair> marks = call2Marks.get(call);
				for (ClassMethodPair mark : infer.getIndirectCallConfigs().get(call)) {
					for (String m : mark.getAllMarks_mtd()) {
						for (String c : mark.getAllMarks_class()) {
							cMarks.add(c);
							One2OnePair pair = new One2OnePair(c, m, mark.getOtherInfos());
							if (!One2OnePair.setContains(marks, pair))
								marks.add(pair);
						}
						if (mark.getAllMarks_class().isEmpty()) {
							One2OnePair pair = new One2OnePair(null, m);
							if (!One2OnePair.setContains(marks, pair))
								marks.add(pair);
						}

						// record into alls
						mMarks.add(m);
						if (mark.getOtherInfos().size() == 4) {
							if (mark.getOtherInfos().get(1) != "null")
								mMarks.add(mark.getOtherInfos().get(1));
							if (mark.getOtherInfos().get(3) != "null")
								mMarks.add(mark.getOtherInfos().get(3));
							if (mark.getOtherInfos().get(0) != "null")
								cMarks.add(mark.getOtherInfos().get(0));
							if (mark.getOtherInfos().get(2) != "null")
								cMarks.add(mark.getOtherInfos().get(2));
						}
					}
				}
			}
			// add into all records
			for (String call : call2Marks.keySet()) {
				if (call2Marks_all.containsKey(call)) {
					for (One2OnePair one : call2Marks.get(call)) {
						if (!One2OnePair.setContains(call2Marks_all.get(call), one))
							call2Marks_all.get(call).add(one);
					}
				} else {
					call2Marks_all.put(call, call2Marks.get(call));
				}
			}
			/* write */
			WriteHelper.writeInfer2File(appName, entryMarks, null, fieldInjectMarks_field, fieldInjectMarks_method, field2Targets, call2Marks);

			/* compare */
			String basicInfoPath = testcaseBasicInfoPath + File.separator + appName + ".txt";
			try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(basicInfoPath)), "UTF-8"))) {
				HashSet<String> classMarks_reachable = new HashSet<>();
				HashSet<String> methodMarks_reachable = new HashSet<>();
				HashSet<String> fieldMarks_reachable = new HashSet<>();
				HashSet<String> classMarks = new HashSet<>();
				HashSet<String> methodMarks = new HashSet<>();
				HashSet<String> fieldMarks = new HashSet<>();

				List<String> list = bufferedReader.lines().collect(Collectors.toList());
				for (int j = list.indexOf("[all class marks]") + 1; j < list.indexOf("[reachable class marks]"); j++)
					classMarks.add(list.get(j));
				for (int j = list.indexOf("[reachable class marks]") + 1; j < list.indexOf("[all method marks]"); j++)
					classMarks_reachable.add(list.get(j));
				for (int j = list.indexOf("[all method marks]") + 1; j < list.indexOf("[reachable method marks]"); j++)
					methodMarks.add(list.get(j));
				for (int j = list.indexOf("[reachable method marks]") + 1; j < list.indexOf("[all field marks]"); j++)
					methodMarks_reachable.add(list.get(j));
				for (int j = list.indexOf("[all field marks]") + 1; j < list.indexOf("[reachable field marks]"); j++)
					fieldMarks.add(list.get(j));
				for (int j = list.indexOf("[reachable field marks]") + 1; j < list.size(); j++)
					fieldMarks_reachable.add(list.get(j));

				WriteHelper.writeInferDiff2file(appName, classMarks_reachable, methodMarks_reachable, fieldMarks_reachable, cMarks, mMarks, fMarks);

				if (!classMarks_reachable.isEmpty())
					classMarks_reachable_all.addAll(classMarks_reachable);
				if (!methodMarks_reachable.isEmpty())
					methodMarks_reachable_all.addAll(methodMarks_reachable);
				if (!fieldMarks_reachable.isEmpty())
					fieldMarks_reachable_all.addAll(fieldMarks_reachable);

				if (!cMarks.isEmpty())
					cMarks_all.addAll(cMarks);
				if (!mMarks.isEmpty())
					mMarks_all.addAll(mMarks);
				if (!fMarks.isEmpty())
					fMarks_all.addAll(fMarks);

				bufferedReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		WriteHelper.writeInfer2File("all-result", entryMarks_all, entrySingleClasses_all, fieldInjectMarks_field_all, fieldInjectMarks_method_all,
				field2Targets_all, call2Marks_all);
		WriteHelper.writeInferDiff2file("all-result", classMarks_reachable_all, methodMarks_reachable_all, fieldMarks_reachable_all, cMarks_all, mMarks_all,
				fMarks_all);
		long endTime3 = System.currentTimeMillis();
		System.out.println("3. collect basic information process costs " + (endTime3 - beginTime3) / 1000 + " s");
		System.out.println("All Processes Costs " + (endTime3 - beginTime1) / 1000 + " s");
	}

	@SuppressWarnings("unused")
	private static void singlePolicy(String[] args) {
		String runningLogsDir = args[0];
		String recordJson = args[1];

		String webApp = args[2];
		GraphBuilder builder = new GraphBuilder(webApp, true);
		ClassHierarchy cha = builder.getCHA();

		Infer infer = new Infer();
//		infer.calculate(runningLogsDir, recordJson, cha, MethodCalledType.request);
		infer.calculate(runningLogsDir, recordJson, cha, builder.getAppCHACG(), MethodCalledType.configuration);
		// write to file
		System.out.println();
	}

}
