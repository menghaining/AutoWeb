package ict.pag.webframework.preInstrumental;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class FurtherModifyAppJar {

	public static void main(String[] args) {
		String jarDirPath  =args[0];
		String toModifyPath = args[1];
		String targetApth = args[2];
//		String jarDirPath = "F:\\Framework\\InputRunnableTestcases\\apache-tomcat-8-with-wars\\tomcat-logicaldoc\\tomcat\\webapps-original\\modifyInput\\logicaldoc\\WEB-INF\\lib";
//		String toModifyPath = "F:\\myProject\\webframeworkmodelinfer\\ict.pag.webframework.preInstrumental\\outs\\logicaldoc\\out-preModify1";
//		String targetApth = "F:\\myProject\\webframeworkmodelinfer\\ict.pag.webframework.preInstrumental\\outs\\logicaldoc\\out-preModify";

		HashSet<String> jarNames = new HashSet<>();
		File jarDir = new File(jarDirPath);
		if (jarDir.isDirectory()) {
			for (String jar : jarDir.list()) {
				jarNames.add(jar);
			}
		}

		File toModifyFile = new File(toModifyPath);
		for (int i = 1;; i++) {
			String tmpPath = toModifyFile.getAbsolutePath() + File.separator + i;
			File tmp = new File(tmpPath);
			if (!tmp.exists())
				break;

			File toPath = new File(targetApth + File.separator + i);
			if (!toPath.exists())
				toPath.mkdirs();

			// 1. copy the application file
			ArrayList<String> files = new ArrayList<>();
			IterateWebApplication.iterateAllClassAndXMLFiles(tmp, files, files);

			HashMap<String, String> modifiedJar = new HashMap<>();
			for (String path : files) {
				File from = new File(path);
				String fileName = from.getName();
				String subPath = path.substring(tmp.getAbsolutePath().length() + 1, path.lastIndexOf(File.separatorChar));

				if (path.contains(File.separator + "lib-classes" + File.separator)) {
					// build lib jar
					String subOutPath = subPath.substring(0, subPath.indexOf("lib-classes") - 1);
					String tmpSubPath = subPath.substring(subPath.indexOf("lib-classes") + "lib-classes".length() + 1);
					String jarName = tmpSubPath.substring(0, tmpSubPath.indexOf(File.separatorChar));
					String internalPath = tmpSubPath.substring(jarName.length() + 1); /* sub path internal jar */

					String jarOutPath = toPath.getAbsolutePath() + File.separator + subOutPath + File.separator + "lib";
					File f = new File(jarOutPath);
					if (!f.exists())
						f.mkdirs();

					if (jarNames.contains(jarName + ".jar")) {
						String jarFilePath;
						if (modifiedJar.containsKey(jarName)) {
							jarFilePath = modifiedJar.get(jarName);
						} else {
							jarFilePath = jarDirPath + File.separator + jarName + ".jar";
							modifiedJar.put(jarName, jarOutPath + File.separator + jarName + ".jar");
						}

						File file = new File(path);
						try {
							byte[] byteOut = Files.readAllBytes(file.toPath());

							String relativeFilePath0 = internalPath + File.separator + fileName;
							String relativeFilePath = relativeFilePath0.replace(File.separatorChar, '/');

							JarHandler jarHandler = new JarHandler();
							jarHandler.replaceSingleJarFile(jarFilePath, byteOut, relativeFilePath, "-updated", jarOutPath);

							// rename
							File outedJar0 = new File(jarOutPath + File.separator + jarName + ".jar");
							File outedJar1 = new File(jarOutPath + File.separator + jarName + "-updated.jar");
							if (outedJar0.exists())
								outedJar0.delete();
							outedJar1.renameTo(outedJar0);

						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} else {
					// copy directly
					File toDir = new File(toPath.getAbsoluteFile() + File.separator + subPath);
					if (!toDir.exists())
						toDir.mkdirs();
					File to = new File(toDir.getAbsoluteFile() + File.separator + fileName);
					copy(from, to);
				}
			}

			// 2. copy the requestsTrigger.txt
			File from = new File(tmpPath + File.separator + "requestsTrigger.txt");
			File to = new File(toPath.getAbsoluteFile() + File.separator + "requestsTrigger.txt");
			copy(from, to);
		}

		// json file
		File from = new File(toModifyPath + File.separator + "modifiy_details.json");
		File to = new File(targetApth + File.separator + "modifiy_details.json");
		copy(from, to);

		System.out.println("..build complete");
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
