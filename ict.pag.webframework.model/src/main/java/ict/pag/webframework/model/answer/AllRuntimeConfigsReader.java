package ict.pag.webframework.model.answer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;

import ict.pag.webframework.model.option.ConfigUtil;

public class AllRuntimeConfigsReader {
	private static String suffix = ".txt";

	private static HashSet<String> alls = new HashSet<>();

	public static void main(String[] args) {
		String dirPath = args[0];
		File dir = new File(dirPath);

		HashSet<File> list = new HashSet<>();
		findAllTXTFiles(dir, list);

		readContext(list);

		try {
			export2TXT(alls);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.out.println();

	}

	private static void readContext(HashSet<File> list) {
		for (File f : list) {
			InputStreamReader read = null;
			try {
				read = new InputStreamReader(new FileInputStream(f), "utf-8");
				BufferedReader br = new BufferedReader(read);

				String content = br.readLine();
				while (content != null) {
					alls.add(content);
					content = br.readLine();
				}

				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (read != null)
					try {
						read.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}

		}

	}

	private static void findAllTXTFiles(File dir, HashSet<File> list) {
		File[] fileList = dir.listFiles();

		for (File f : fileList) {
			if (f.isFile()) {
				if (f.getAbsolutePath().endsWith(suffix)) {
					list.add(f);
				}
			} else if (f.isDirectory()) {
				findAllTXTFiles(f, list);
			}
		}

	}

	public static void export2TXT(HashSet<String> runtimeMarks) throws IOException {
		String userDir = System.getProperty("user.dir");
		if (!userDir.endsWith(File.separator)) {
			userDir = userDir + File.separator;
		}

		String root = userDir + "result" + File.separator;
		
		File dir = new File(root);
		if (!dir.exists())
			dir.mkdirs();

		String outf = "run-time-configurations";
		File file = new File(root + outf + ".txt");
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
}
