package ict.pag.m.marks2SAInfer.util;

public class StringUtil {

	public static CallsiteInfo calCallsiteInfo(String curr) {

		String[] strs = curr.split("]");
		String line = strs[1].substring(strs[1].lastIndexOf('[') + 1);
		String callsiteStmt = strs[1].substring(0, strs[1].lastIndexOf('['));
//		String belongTo = strs[2].substring(1);
		String belongTo = strs[2];
//		String belongTo = curr.substring(curr.lastIndexOf('[') + 1, curr.length() - 1);
//
//		String curr2 = curr.substring(10, curr.indexOf(belongTo) - 1);
//		String line = curr2.substring(curr2.indexOf('[') + 1, curr2.length() - 1);

//		String callsiteStmt = curr2.substring(0, curr2.indexOf('['));

		return new CallsiteInfo(belongTo, line, callsiteStmt);

	}

}
