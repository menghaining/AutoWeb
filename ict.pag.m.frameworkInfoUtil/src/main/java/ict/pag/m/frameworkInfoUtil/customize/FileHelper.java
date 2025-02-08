package ict.pag.m.frameworkInfoUtil.customize;

public class FileHelper {
	public static FileType getTypeBySuffix(String filePath) {
		if (!filePath.contains("."))
			return FileType.UNKOWN;
		String extension = filePath.substring(filePath.lastIndexOf("."));
		switch (extension) {
		case ".xml":
			return FileType.XML;
		case ".txt":
			return FileType.TXT;
		case ".jar":
			return FileType.JAR;
		case ".war":
			return FileType.WAR;
		case ".json":
			return FileType.JSON;
		case ".class":
			return FileType.CLASS;
		case ".java":
			return FileType.JAVA;
		case ".apk":
			return FileType.APK;
		default:
			return FileType.UNKOWN;
		}
	}
	
	/**
	 * 
	 * @param original,like com.example.service.StudentService.getAllStus()
	 * @return like 
	 */
	public static String formateSignature(String original) {
		
		return null;
	}
}
