package ict.pag.webframework.log.util;

import java.util.ArrayList;

public class LogFormatHepler {
	public static ArrayList<String> removeThreadID(ArrayList<String> sequence) {
		ArrayList<String> ret = new ArrayList<String>();

		for (String line : sequence) {
			// remove header_tag
			String line1 = line.substring(line.indexOf(']') + 1);
			// remove id
			String line2 = line1.substring(line1.indexOf(']') + 1);
			/** in the beginning of a call */
			if (line.startsWith("[call method]")) {
				ret.add(line2);
				continue;
			}

			/** when finishing this call method */
			if (line.startsWith("[call method finished]")) {
				ret.add(line2 + "[end]");
				continue;
			}

			/** callsite */
			if (line.startsWith("[callsite]")) {
				ret.add("[callsite]" + line2);
				continue;
			}

			/** returnSite */
			if (line.startsWith("[returnSite]")) {
				ret.add("[returnSite]" + line2);
				continue;
			}

			/** field write */
			if (line.startsWith("[field write]")) {
				ret.add("[field write]" + line2);
				continue;
			}

			/** field read */
			if (line.startsWith("[field read]")) {
				ret.add("[field read]" + line2);
			}

			if (line.startsWith("[base ")) {
				/** add the run time info about fields of 'this' object */
				ret.add(line.substring(0, line.indexOf(']') + 1) + line2);

			}
		}

		return ret;

	}

}
