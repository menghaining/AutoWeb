package ict.pag.webframework.model.logprase;

public class RuntimeFieldInfo {
	/* type */
	FieldOPKind kind;

	/* declared information */
	String lineNumber;
	String fieldName;
	String declareType;
	String belongToMthd;
	

	/* runtime information */
	String baseRTType;
	String fieldRTType;
	// only for field read and is collection
	String returnRTType;
}
