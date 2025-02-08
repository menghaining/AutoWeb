package ict.pag.m.marks2SAInfer.summarizeModels;

import java.io.File;
import java.util.HashSet;

public class Summarize {

	public static void main(String[] args) {
		String path = args[0];

		File file_p = new File(path);
		File[] fileList = file_p.listFiles();

		HashSet<File> list = new HashSet<>();
		for (File f : fileList) {
			if (f.isFile()) {
				if (f.getAbsolutePath().endsWith(".json")) {
					list.add(f);
				}
			}
		}

		Conclude cal = new Conclude(list);
		cal.concludes();
		HashSet<String> allmarks = cal.getAllMarks();
		System.out.println("[info][COUNT]  marks kinds total :" + allmarks.size());
		for (String m : allmarks) {
			System.out.println("\t[info][mark]" + m);
		}
		cal.write2Json();

	}

}
