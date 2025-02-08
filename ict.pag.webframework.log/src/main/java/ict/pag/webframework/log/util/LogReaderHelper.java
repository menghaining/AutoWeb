package ict.pag.webframework.log.util;

public class LogReaderHelper {
	/**
	 * [header_type][id]...infos </br>
	 * extract id
	 */
	public static int getThreadID(String lineContent, boolean newLogForm) {
		if (!isOriginalLogFormat(lineContent, newLogForm))
			return -1;

		String subString = lineContent.substring(lineContent.indexOf(']') + 1);
		String sid = subString.substring(1, subString.indexOf(']'));

		int ret = -1;
		try {
			ret = Integer.parseInt(sid);
		} catch (Exception e) {
			System.err.println("errer convert string to int");
			return ret;
		}

		return ret;
	}

	/**
	 * all startsWith defined when instrumental
	 */
	public static boolean isOriginalLogFormat(String lineContent, boolean newLogForm) {
		if (!(lineContent.startsWith("[ReqURL]") || lineContent.startsWith("[ReqURL_end]")
				|| lineContent.startsWith("[call method]") || lineContent.startsWith("[call method finished]")
				|| lineContent.startsWith("[callsite]") || lineContent.startsWith("[returnSite]")
				|| lineContent.startsWith("[field write]") || lineContent.startsWith("[field read]")
				|| (newLogForm && (lineContent.startsWith("[ReqStart]") || lineContent.startsWith("[ReqEnd]")
						|| lineContent.startsWith("[base ")))))
			return false;
		return true;
	}
}
