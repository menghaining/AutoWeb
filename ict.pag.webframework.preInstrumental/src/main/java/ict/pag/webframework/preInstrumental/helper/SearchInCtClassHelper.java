package ict.pag.webframework.preInstrumental.helper;

import java.util.Set;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;

public class SearchInCtClassHelper {
	/**
	 * @param cc1:       CtClass
	 * @param fieldName: the simple name of the searched field
	 */
	public static CtField findCtField(CtClass cc1, String fieldName) {
		CtField field = null;
		for (CtField ctf : cc1.getDeclaredFields()) {
			String ctf_name = ctf.getName();
			if (ctf_name.equals(fieldName)) {
				field = ctf;
				break;
			}
		}
		return field;
	}

	/**
	 * @param cc:  CtClass
	 * @param sig: the simple name of the searched method
	 */
	public static CtMethod findCtMethod(CtClass cc, String sig) {
		CtMethod method = null;
		for (CtMethod mtd : cc.getDeclaredMethods()) {
			String mtd_longname = mtd.getLongName();
			String param = mtd.getSignature();
			String signature = mtd_longname.substring(0, mtd_longname.indexOf("(")) + param;
			if (sig.equals(signature)) {
				method = mtd;
				break;
			}
		}
		return method;
	}

	/** case not sensitive */
	public static StringMemberValue findAnnotationMemberValue(Annotation anno, String sameVal) {
		StringMemberValue memval = null;
		if (anno == null)
			return null;
		Set<String> mems = anno.getMemberNames();
		if (mems == null)
			return null;

		for (String mem : mems) {
			MemberValue val = anno.getMemberValue(mem);
			if (val instanceof StringMemberValue) {
				String value = ((StringMemberValue) val).getValue();
				if (value.toLowerCase().equals(sameVal.toLowerCase())) {
					memval = (StringMemberValue) val;
				}
			}
		}
		return memval;
	}
}
