package ict.pag.webframework.preInstrumental;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;

public class writeAll2Jar {

	public static void main(String[] args) {
		String toModifyDirPath = args[0];
		String targetApth = args[1];
		String jarPath = args[2];

		File toModifyDir = new File(toModifyDirPath);
		for (int i = 1;; i++) {
			String modifyPath = toModifyDir.getAbsolutePath() + File.separator + i;
			File modifyDir = new File(modifyPath);
			if (!modifyDir.exists())
				break;

			File toPath = new File(targetApth + File.separator + i);
			if (!toPath.exists())
				toPath.mkdirs();

			File jarFile = new File(jarPath);
			String jarName0 = jarFile.getName();
			String jarName = jarName0.substring(0, jarName0.lastIndexOf('.'));
			File outedJar0 = new File(toPath.getAbsoluteFile() + File.separator + jarName + ".jar");
			File outedJar1 = new File(toPath.getAbsoluteFile() + File.separator + jarName + "-updated.jar");

			// 1. write to runnable jar
			HashSet<String> files = new HashSet<>();
			collectAllFiles(modifyDir, files);
			for (String path : files) {
				if(path.contains("requestsTrigger.txt"))
					continue;
				File file = new File(path);
				try {
					byte[] byteOut = Files.readAllBytes(file.toPath());

					String relativeFilePath0 = path.substring(modifyPath.length() + 1);
					String relativeFilePath = relativeFilePath0.replace(File.separatorChar, '/');

					JarHandler jarHandler = new JarHandler();
					jarHandler.replaceSingleJarFile(jarPath, byteOut, relativeFilePath, "-updated", toPath.getAbsolutePath());

					// rename
					if (outedJar0.exists())
						outedJar0.delete();
					outedJar1.renameTo(outedJar0);

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// rename to -updated.jar
			if (outedJar1.exists())
				outedJar1.delete();
			outedJar0.renameTo(outedJar1);

			// 2. copy the requestsTrigger.txt
			File from = new File(modifyPath + File.separator + "requestsTrigger.txt");
			File to = new File(toPath.getAbsoluteFile() + File.separator + "requestsTrigger.txt");
			copy(from, to);

		}

		// json file
		File from = new File(toModifyDirPath + File.separator + "modifiy_details.json");
		File to = new File(targetApth + File.separator + "modifiy_details.json");
		copy(from, to);

		System.out.println("..build complete");
	}

	private static void collectAllFiles(File modifyDir, HashSet<String> files) {
		func(modifyDir, files);
	}

	public static void func(File file, HashSet<String> files) {
		File[] fs = file.listFiles();
		for (File f : fs) {
			if (f.isDirectory())
				func(f, files);
			if (f.isFile()) {
				if (!file.getName().contains("requestsTrigger.txt"))
					files.add(f.getPath());
			}
		}
	}

	private static void copy(File from, File to) {
		try {
			if (to.exists())
				to.delete();
			Files.copy(from.toPath(), to.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
