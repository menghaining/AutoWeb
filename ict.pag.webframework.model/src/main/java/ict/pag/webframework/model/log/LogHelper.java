package ict.pag.webframework.model.log;

import java.util.ArrayList;

public class LogHelper {
	
	public static int concernedField(String todoStmt, String todoLine, ArrayList<String> callsiteConcernedList) {
		String todo = todoStmt + "[" + todoLine + "]";
		for (int i = 0; i < callsiteConcernedList.size(); i++) {
			String con = callsiteConcernedList.get(i);
			if (con.equals(todo))
				return i;

			// only stmt also OK
			// change precise
//			String tmp = con.substring(0, con.lastIndexOf('['));
//			if (tmp.equals(todoStmt))
//				return i;
		}
		return -1;
	}
}
