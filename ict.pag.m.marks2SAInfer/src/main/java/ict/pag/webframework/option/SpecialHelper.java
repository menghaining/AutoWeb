package ict.pag.webframework.option;

import org.python.antlr.PythonParser.return_stmt_return;

import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import com.ibm.wala.shrikeCT.InvalidClassFileException;

public class SpecialHelper {
	/**
	 * 
	 * @param base like Lcom/example/service/StudentService/getAllStus()
	 * @return like com.example.service.StudentService.getAllStus()
	 */
	public static String formatSignature(String base) {
		if (!base.contains("/"))
			return base;
		String newS = null;
		newS = base.replace('/', '.').substring(1);

		return newS;
	}

	/**
	 * 
	 * @param base like com.example.service.StudentService.getAllStus()
	 * @return like Lcom/example/service/StudentService/getAllStus()
	 */
	public static String reformatSignature(String base) {
		String newStr = "L";
		return newStr + base.replaceAll("\\.", "/") + ";";

	}

	public static SourcePosition getSourcePosition(IMethod method, int instIndex) {
		try {
			if (method instanceof IBytecodeMethod) {
				return ((IBytecodeMethod) method)
						.getSourcePosition(((IBytecodeMethod) method).getBytecodeIndex(instIndex));
			}
		} catch (ArrayIndexOutOfBoundsException | InvalidClassFileException e) {
			// TODO: handle exception
			return null;
		}

		return null;
	}

}
