package ict.pag.webframework.infer.WriteHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ict.pag.webframework.infer.marks.One2OnePair;

public class WriteHelper {
	public static void writeInfer2File(String appName, Set<One2OnePair> entryMarks, Set<String> entrySingleClasses, Set<String> fieldInjectMarks_field,
			Set<String> fieldInjectMarks_method, Set<One2OnePair> field2Targets, Map<String, HashSet<One2OnePair>> call2Marks) throws IOException {
		String outDir = System.getProperty("user.dir") + File.separator + "outs";
		String d = outDir + File.separator + "runnable";
		File ddir = new File(d);
		if (!ddir.exists())
			ddir.mkdirs();
		String outPath = d + File.separator + appName + ".txt";
		System.out.println("write to " + outPath);
		File outFile = new File(outPath);
		if (!outFile.exists())
			outFile.createNewFile();

		FileWriter fw = new FileWriter(outFile);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write("[entry]\n");
		for (One2OnePair one : entryMarks)
			bw.write(one.getPre() + "\t" + one.getPost() + "\n");
		bw.write("[inject on field]\n");
		for (String s : fieldInjectMarks_field)
			bw.write(s + "\n");
		bw.write("[inject on method]\n");
		for (String s : fieldInjectMarks_method)
			bw.write(s + "\n");
		bw.write("[field to target]\n");
		for (One2OnePair one : field2Targets)
			bw.write(one.getPre() + "\t" + one.getPost() + "\n");
		bw.write("[framework call to target]\n");
		for (String call : call2Marks.keySet()) {
			bw.write(call + "\n");
			for (One2OnePair one : call2Marks.get(call))
//				bw.write("\t" + one.getPre() + "\t" + one.getPost() + "\n");
				bw.write("\t" + one.getPre() + "\t" + one.getPost() + "\t" + one.getOtherInfos() + "\n");
		}
		if (entrySingleClasses != null) {
			bw.write("[entry single class marks]\n");
			for (String s : entrySingleClasses)
				bw.write(s + "\n");
		}
		bw.close();
		fw.close();

	}

	public static void writeInferDiff2file(String appName, HashSet<String> classMarks, HashSet<String> methodMarks, HashSet<String> fieldMarks,
			HashSet<String> classMarks_actual, HashSet<String> methodMarks_actual, HashSet<String> fieldMarks_actual) throws IOException {
		String outDir = System.getProperty("user.dir") + File.separator + "outs";
		String d = outDir + File.separator + "runnable";
		File ddir = new File(d);
		if (!ddir.exists())
			ddir.mkdirs();
		String outPath = d + File.separator + appName + "_diff.txt";
		File outFile = new File(outPath);
		System.out.println("write to " + outPath);
		if (!outFile.exists())
			outFile.createNewFile();

		FileWriter fw = new FileWriter(outFile);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write("[Reachable Class Marks] " + classMarks.size() + "\n");
		bw.write("[Reachable Method Marks] " + methodMarks.size() + "\n");
		bw.write("[Reachable Field Marks] " + fieldMarks.size() + "\n");
		bw.write("[Actual Class Marks] " + classMarks_actual.size() + "\n");
		bw.write("[Actual Method Marks] " + methodMarks_actual.size() + "\n");
		bw.write("[Actual Field Marks] " + fieldMarks_actual.size() + "\n");

		bw.write("[Reachable class marks more]\n");
		classMarks = format(classMarks);
		for (String m : classMarks) {
			if (!classMarks_actual.contains(m))
				bw.write(m + "\n");
		}
		bw.write("[Actual class marks more]\n");
		for (String m : classMarks_actual) {
			if (!classMarks.contains(m))
				bw.write(m + "\n");
		}

		bw.write("[Reachable method marks more]\n");
		methodMarks = format(methodMarks);
		for (String m : methodMarks) {
			if (!methodMarks_actual.contains(m))
				bw.write(m + "\n");
		}
		bw.write("[Actual method marks more]\n");
		for (String m : methodMarks_actual) {
			if (!methodMarks.contains(m))
				bw.write(m + "\n");
		}

		bw.write("[Reachable field marks more]\n");
		fieldMarks = format(fieldMarks);
		for (String m : fieldMarks) {
			if (!fieldMarks_actual.contains(m))
				bw.write(m + "\n");
		}
		bw.write("[Actual field marks more]\n");
		for (String m : fieldMarks_actual) {
			if (!fieldMarks.contains(m))
				bw.write(m + "\n");
		}

		bw.close();
		fw.close();

	}

	private static HashSet<String> format(HashSet<String> marks) {
		HashSet<String> ret = new HashSet<>();
		for (String m0 : marks) {
			String m;
			if (m0.startsWith("L") && m0.contains("/"))
				m = m0.replace('/', '.').substring(1) + "\n";
			else
				m = m0;
			ret.add(m);
		}
		return ret;
	}

	public static void writeCollect2file(String appName, int appClasses, int allClasses, int reachableAppClasses, HashSet<String> classMarks,
			HashSet<String> methodMarks, HashSet<String> fieldMarks, HashSet<String> classMarks_reachable, HashSet<String> methodMarks__reachable,
			HashSet<String> fieldMarks__reachable) throws IOException {
		String outDir = System.getProperty("user.dir") + File.separator + "outs";
		String d = outDir + File.separator + "testcaseInfo";
		File ddir = new File(d);
		if (!ddir.exists())
			ddir.mkdirs();
		String outPath = d + File.separator + appName + ".txt";
		File outFile = new File(outPath);
		if (!outFile.exists())
			outFile.createNewFile();
		FileWriter fw = new FileWriter(outFile);
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write("[app classes] " + appClasses + "\n");
		bw.write("[all classes] " + allClasses + "\n");
		bw.write("[reachable app classes] " + reachableAppClasses + "\n");
		bw.write("[all class marks]\n");
		for (String m : classMarks) {
			if (m.startsWith("L") && m.contains("/"))
				bw.write(m.replace('/', '.').substring(1) + "\n");
			else
				bw.write(m + "\n");
		}
		bw.write("[reachable class marks]\n");
		for (String m : classMarks_reachable) {
			if (m.startsWith("L") && m.contains("/"))
				bw.write(m.replace('/', '.').substring(1) + "\n");
			else
				bw.write(m + "\n");
		}
		bw.write("[all method marks]\n");
		for (String m : methodMarks) {
			if (m.startsWith("L") && m.contains("/"))
				bw.write(m.replace('/', '.').substring(1) + "\n");
			else
				bw.write(m + "\n");
		}
		bw.write("[reachable method marks]\n");
		for (String m : methodMarks__reachable) {
			if (m.startsWith("L") && m.contains("/"))
				bw.write(m.replace('/', '.').substring(1) + "\n");
			else
				bw.write(m + "\n");
		}
		bw.write("[all field marks]\n");
		for (String m : fieldMarks) {
			if (m.startsWith("L") && m.contains("/"))
				bw.write(m.replace('/', '.').substring(1) + "\n");
			else
				bw.write(m + "\n");
		}
		bw.write("[reachable field marks]\n");
		for (String m : fieldMarks__reachable) {
			if (m.startsWith("L") && m.contains("/"))
				bw.write(m.replace('/', '.').substring(1) + "\n");
			else
				bw.write(m + "\n");
		}
		bw.close();
		fw.close();

	}
}
