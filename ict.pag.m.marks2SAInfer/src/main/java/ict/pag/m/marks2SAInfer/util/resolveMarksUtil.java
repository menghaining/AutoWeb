package ict.pag.m.marks2SAInfer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.types.FieldReference;

import ict.pag.m.frameworkInfoUtil.customize.ConfigUtil;
import ict.pag.m.frameworkInfoUtil.customize.Util;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoPair;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoUnit;
import ict.pag.m.frameworkInfoUtil.infoEntity.marksLevel;

public class resolveMarksUtil {
	/**
	 * annotations on method call
	 * 
	 * @param s       method call signature
	 * @param annoSet
	 * @return marks directly on the method
	 */
	public static ArrayList<String> getAnnosMarksOnly(String s, Set<infoUnit> annoSet) {
		ArrayList<String> ret = new ArrayList<String>();
		for (infoUnit unit : annoSet) {
			if (unit.getBase().equals(s)) {
				// s has annos and s is method
				List<infoPair> marksPairs = unit.getFields();
				for (infoPair p : marksPairs) {
					String anno = p.getMark();
					if (!ret.contains(anno))
						if (ConfigUtil.isFrameworkMarks(anno))
							ret.add(anno);

				}
			}

		}
		return ret;
	}

	public static ArrayList<String> getAnnos_field_on_Class(String clazz, Set<infoUnit> annoSet_field) {
		ArrayList<String> ret = new ArrayList<String>();
		for (infoUnit unit : annoSet_field) {
			if (unit.getBase().startsWith(clazz)) {
				// s has annos and s is method
				List<infoPair> marksPairs = unit.getFields();
				for (infoPair p : marksPairs) {
					String anno = p.getMark();
					if (!ret.contains(anno))
						if (ConfigUtil.isFrameworkMarks(anno))
							ret.add(anno);

				}
			}

		}
		return ret;
	}

	/**
	 * annotations directly on class
	 * 
	 * @param s       method call signature
	 * @param annoSet
	 * @param tag     annotations on what
	 * @return marks directly on tag
	 */
	public static ArrayList<String> getClassAnnos_withDecorate(String s, Set<infoUnit> annoSet, String tag) {
		ArrayList<String> ret = new ArrayList<String>();
		for (infoUnit unit : annoSet) {
			if (unit.getBase().equals(s)) {
				// s has annos and s is method
				List<infoPair> marksPairs = unit.getFields();
				for (infoPair p : marksPairs) {
					String anno = p.getMark();
					String toaddanno = "anno:" + tag + ":" + anno;
					if (!ret.contains(toaddanno))
						if (ConfigUtil.isFrameworkMarks(anno))
							ret.add(toaddanno);

				}
			}

		}
		return ret;
	}

	/**
	 * annotations on method call
	 * 
	 * @param s       method call signature
	 * @param annoSet
	 * @return marks directly on the method and its class
	 */
	public static ArrayList<String> getAnnosMarks(String s, Set<infoUnit> annoSet) {
		ArrayList<String> ret = new ArrayList<String>();
		for (infoUnit unit : annoSet) {
			if (unit.getBase().equals(s)) {
				// s has annos and s is method
				List<infoPair> marksPairs = unit.getFields();
				for (infoPair p : marksPairs) {
					String anno = p.getMark();
					String toaddanno = "anno:mtd:" + anno;
					if (!ret.contains(toaddanno))
						if (ConfigUtil.isFrameworkMarks(anno))
							ret.add(toaddanno);

				}
			}
			/** add method class annotation */
			if (unit.getLevel().equals(marksLevel.Clazz)) {
				if (s.startsWith(unit.getBase())) {
					List<infoPair> marksPairs = unit.getFields();
					for (infoPair p : marksPairs) {
						String anno = p.getMark();
						String toaddanno = "anno:class:" + anno;
						if (!ret.contains(toaddanno))
							if (ConfigUtil.isFrameworkMarks(anno))
								ret.add(toaddanno);
					}
				}
			}

		}
		return ret;
	}

	public static ArrayList<String> getAnnosMarksWithSplit(String s, Set<infoUnit> annoSet) {
		ArrayList<String> ret = new ArrayList<String>();
		ArrayList<String> ret2 = new ArrayList<String>();
		for (infoUnit unit : annoSet) {
			if (unit.getBase().equals(s)) {
				// s has annos and s is method
				List<infoPair> marksPairs = unit.getFields();
				for (infoPair p : marksPairs) {
					String anno = p.getMark();
					if (!ret.contains(anno))
						if (ConfigUtil.isFrameworkMarks(anno)) {
							ret.add(anno);
						}
				}
			}

			/** add method class annotation */
			if (unit.getLevel().equals(marksLevel.Clazz)) {
				if (s.startsWith(unit.getBase())) {
					List<infoPair> marksPairs = unit.getFields();
					for (infoPair p : marksPairs) {
						String anno = p.getMark();
						if (!ret2.contains(anno))
							if (ConfigUtil.isFrameworkMarks(anno))
								ret2.add(anno);
					}
				}
			}

		}
		// add split
//		if (!ret.isEmpty() && !ret2.isEmpty()) {
		// 2021.7.30 delete ret2 because class can be null
//		if (!ret.isEmpty()) {
		// 2021.9.7 class and methods only one is not empty is enough
		if (!ret.isEmpty() || !ret2.isEmpty()) {
			ret.add("*");
			ret.addAll(ret2);
		} else {
			String signature = s.substring(s.lastIndexOf(".") + 1);
			if (signature.equals("main([Ljava/lang/String;)V")) {
				ret.add("main([Ljava/lang/String;)V");
				ret.add("*");
				ret.addAll(ret2);
			} else {
				ret.clear();
			}
		}
		return ret;
	}

	public static ArrayList<infoPair> getMethodAnno(String s, Set<infoUnit> annoSet) {
		ArrayList<infoPair> ret = new ArrayList<infoPair>();
		for (infoUnit unit : annoSet) {
			if (unit.getBase().equals(s)) {
				// s has annos and s is method
				List<infoPair> marksPairs = unit.getFields();
				for (infoPair p : marksPairs) {
					String anno = p.getMark();
					if (ConfigUtil.isFrameworkMarks(anno)) {
						if (!ret.contains(p))
							ret.add(p);
					}

				}
			}
		}

		return ret;
	}

	public static ArrayList<String> getAnnosMarks_withDecorate(String s, Set<infoUnit> annoSet) {
		ArrayList<String> ret = new ArrayList<String>();
		ArrayList<String> ret2 = new ArrayList<String>();
		for (infoUnit unit : annoSet) {
			if (unit.getBase().equals(s)) {
				// s has annos and s is method
				List<infoPair> marksPairs = unit.getFields();
				for (infoPair p : marksPairs) {
					String anno = p.getMark();
					if (ConfigUtil.isFrameworkMarks(anno)) {
						String m_anno = "anno:mtd:" + anno;
						if (!ret.contains(m_anno))
							ret.add(m_anno);
					}

				}
			}

			/** add method class annotation */
			if (unit.getLevel().equals(marksLevel.Clazz)) {
				if (s.startsWith(unit.getBase())) {
					List<infoPair> marksPairs = unit.getFields();
					for (infoPair p : marksPairs) {
						String anno = p.getMark();
						if (ConfigUtil.isFrameworkMarks(anno)) {
							String canno = "anno:class:" + anno;
							if (!ret2.contains(canno))
								ret2.add(canno);

						}
					}
				}
			}

		}
//		// add split
//		if (!ret.isEmpty() && !ret2.isEmpty()) {
//			ret.add("*");
//			ret.addAll(ret2);
//		} else {
//			ret.clear();
//		}
		ret.addAll(ret2);
		return ret;
	}

	public static ArrayList<String> getAnnosMarksOnly_onclass(String s, Set<infoUnit> annoSet) {
		ArrayList<String> ret = new ArrayList<String>();
		for (infoUnit unit : annoSet) {
			if (unit.getBase().equals(s)) {
				// s has annos and s is method
				List<infoPair> marksPairs = unit.getFields();
				for (infoPair p : marksPairs) {
					String anno = p.getMark();
					if (!ret.contains("anno:class:" + anno))
						if (ConfigUtil.isFrameworkMarks(anno))
							ret.add("anno:class:" + anno);

				}
			}

		}
		return ret;
	}

	/**
	 * inheritance directly on class find the marks directly on s</br>
	 * 
	 * @param s              class long name
	 * @param inheritanceSet
	 * @return marks directly on the class, including override method
	 */
	public static ArrayList<String> getInheritanceMarks(String s, Set<infoUnit> inheritanceSet) {
		ArrayList<String> ret = new ArrayList<String>();
		for (infoUnit unit : inheritanceSet) {
			String baseClazz = unit.getBase();
			if (s.startsWith(baseClazz + ".")) {
				// find the mark
				List<infoPair> marksPairs = unit.getFields();
				for (infoPair p : marksPairs) {

					Set<String> vals = p.getAllValues();
					for (String pri : vals) {
						/**
						 * org.springframework.web.servlet.HandlerInterceptor.preHandle(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;)Z
						 */
						String markString = pri + s.substring(baseClazz.length());
						if (!ret.contains(markString))
							ret.add(markString);
					}
				}

				break;
			}
		}
		return ret;
	}

	/**
	 * inheritance directly on class find the marks directly on s</br>
	 * 
	 * @param s              class long name
	 * @param inheritanceSet
	 * @return marks like
	 *         "inhe:full:org.springframework.web.servlet.HandlerInterceptor.preHandle(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;)Z"
	 */
	public static ArrayList<String> getInheritanceMarks_withDecorate(String s, Set<infoUnit> inheritanceSet) {
		ArrayList<String> ret = new ArrayList<String>();
		for (infoUnit unit : inheritanceSet) {
			String baseClazz = unit.getBase();
			if (s.startsWith(baseClazz + ".")) {
				// find the mark
				List<infoPair> marksPairs = unit.getFields();
				for (infoPair p : marksPairs) {

					Set<String> vals = p.getAllValues();
					boolean hasClass = false;
					for (String pri : vals) {
						/**
						 * org.springframework.web.servlet.HandlerInterceptor</br>
						 * .preHandle(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;)Z
						 */
//						String markString = "inhe:full:" + pri + s.substring(baseClazz.length());
						String clazzString = "inhe:class:" + pri;
						if (!ret.contains(clazzString)) {
							ret.add(clazzString);
							hasClass = true;
						}
					}
					if (hasClass) {
						ret.add("inhe:mtd:" + s.substring(baseClazz.length() + 1));
					}

				}

				break;
			}
		}
		return ret;
	}

	/**
	 * inheritance directly on class find the marks directly on s</br>
	 * 
	 * @param s              class long name
	 * @param inheritanceSet
	 * @return marks directly on the class, only class
	 */
	public static ArrayList<String> getInheritanceMarks_onclass(String s, Set<infoUnit> inheritanceSet) {
		ArrayList<String> ret = new ArrayList<String>();
		for (infoUnit unit : inheritanceSet) {
			String baseClazz = unit.getBase();
			if (s.equals(baseClazz)) {
				// find the mark
				List<infoPair> marksPairs = unit.getFields();
				for (infoPair p : marksPairs) {

					Set<String> vals = p.getAllValues();
					for (String pri : vals) {
						/**
						 * org.springframework.web.servlet.HandlerInterceptor
						 */
						String markString = pri;
						if (!ret.contains("inhe:class:" + markString))
							ret.add("inhe:class:" + markString);
					}
				}

				break;
			}
		}
		return ret;
	}

	public static ArrayList<String> getXMLMarks_withDecorate(String curr, Set<infoUnit> xmlSet) {
		ArrayList<String> ret = new ArrayList<String>();
		ArrayList<String> tmp = getXMLMarks(curr, xmlSet);
		for (String s : tmp) {
			ret.add("xml:" + s);
		}
		return ret;
	}

	public static ArrayList<String> getXMLMarks(String curr, Set<infoUnit> xmlSet) {
		ArrayList<String> ret = new ArrayList<String>();
		for (infoUnit unit : xmlSet) {
//			if (unit.getLevel().equals(level)) {
			String baseClazz = unit.getBase();
//			if (curr.startsWith(baseClazz)) {
			if (curr.equals(baseClazz)) {
				// find the mark
				StringBuilder s = new StringBuilder();
				List<infoPair> marksPairs = unit.getFields();

				for (int i = 0; i < marksPairs.size(); i++) {
					infoPair p = marksPairs.get(i);
					if (p.getValue() == null)
						continue;
					if (i == 0) {
						s.append(p.getMark());
					} else {
						s.append(";");
						s.append(p.getMark());
					}
					if (i == marksPairs.size() - 1) {
						for (String val : p.getValue().keySet()) {
							if (p.getValue().get(val).equals(baseClazz)) {
								s.append(";");
								s.append("myType:" + val);
							}
						}
					}

				}

				ret.add(s.toString());

			}
//			}
		}

		return ret;
	}

	/**
	 * curr must equals matches
	 */
	public static ArrayList<String> getXMLMarks_common_allMatch(String curr, Set<infoUnit> xmlSet) {
		ArrayList<String> ret = new ArrayList<String>();
		for (infoUnit unit : xmlSet) {
//			if (unit.getLevel().equals(level)) {
			String base = unit.getBase();
			if (curr.equals(base)) {
				// find the mark
				StringBuilder s = new StringBuilder();
				List<infoPair> marksPairs = unit.getFields();

				for (int i = 0; i < marksPairs.size(); i++) {
					infoPair p = marksPairs.get(i);
					if (p.getValue() == null)
						continue;
					if (i == 0) {
						s.append(p.getMark());
					} else {
						s.append(";");
						s.append(p.getMark());
					}
					if (i == marksPairs.size() - 1) {
						for (String val : p.getValue().keySet()) {
							if (p.getValue().get(val).equals(base)) {
								s.append(";");
								s.append("myType:" + val);
								break;
							}
						}
					}

				}

				ret.add(s.toString());

			}
//			}
		}

		return ret;
	}

	public static ArrayList<String> getXMLMarks_field(FieldReference fieldReference, Set<infoUnit> xmlSet) {
		ArrayList<String> ret = new ArrayList<String>();

		String fieldClass = Util.format(fieldReference.getFieldType().getName().toString());
		String fieldName = fieldReference.getName().toString();
		String fieldBelongTo = Util.format(fieldReference.getDeclaringClass().getName().toString());
		String fieldSig = Util.format(fieldReference.getSignature());

		for (infoUnit unit : xmlSet) {
			String base = unit.getBase();
			if (base.contains(fieldBelongTo)) {
				if (base.equals(fieldBelongTo)) {
					/* belong2class and field name defined in different string */
					List<infoPair> marksPairs = unit.getFields();
					infoPair valuesPair = marksPairs.get(marksPairs.size() - 1);
					StringBuilder s = new StringBuilder();

					if (valuesPair.getValue() == null)
						continue;

					// find the field value mark
					StringBuilder fieldLayerMark = new StringBuilder();
					for (String val : valuesPair.getValue().keySet()) {
						if (valuesPair.getValue().get(val).equals(fieldName)) {
							if (val.equals("field") || val.equals("f")) {
								fieldLayerMark.append(";");
								fieldLayerMark.append("myType:" + val);
								break;
							} else {
								StringBuilder tmp = new StringBuilder();
								tmp.append(";");
								tmp.append("myType:" + val);
								fieldLayerMark = tmp;
							}
						}
					}
					// find class
					StringBuilder classLayerMark = new StringBuilder();
					for (String val : valuesPair.getValue().keySet()) {
						if (valuesPair.getValue().get(val).equals(base)) {
							classLayerMark.append(";");
							classLayerMark.append("myType:" + val);

						}
					}
					if (fieldLayerMark.length() > 0) {
						for (int i = 0; i < marksPairs.size(); i++) {
							infoPair p = marksPairs.get(i);
							if (i == 0) {
								s.append(p.getMark());
							} else {
								s.append(";");
								s.append(p.getMark());
							}
						}
						ret.add("xml:class:" + s.toString() + classLayerMark.toString());
						ret.add("xml:field:" + s.toString() + fieldLayerMark.toString());
						return ret;
					}

				} else if (base.contains(fieldName)) {
					// field and belong2Class is in same string
					List<infoPair> marksPairs = unit.getFields();
					StringBuilder s = new StringBuilder();

					for (int i = 0; i < marksPairs.size(); i++) {
						infoPair p = marksPairs.get(i);
						if (p.getValue() == null)
							continue;
						if (i == 0) {
							s.append(p.getMark());
						} else {
							s.append(";");
							s.append(p.getMark());
						}
						if (i == marksPairs.size() - 1) {
							for (String val : p.getValue().keySet()) {
								if (p.getValue().get(val).equals(base)) {
									s.append(";");
									s.append("myType:" + val);
									break;
								}
							}
						}
					}
					ret.add("xml:full:" + s.toString());
					return ret;
				}
			}
		}

		return ret;

//		return getXMLMarks_common_allMatch(fieldReference, xmlSet);
	}

	public static ArrayList<String> getXMLMarks_mthd(String curr, Set<infoUnit> xmlSet) {
		ArrayList<String> ret = new ArrayList<String>();

		String declareClass = curr.substring(0, curr.lastIndexOf('.'));
//		System.out.println(curr);
		String mtdName = curr.substring(declareClass.length() + 1, curr.lastIndexOf('('));
		String mtdAndParams = curr.substring(declareClass.length() + 1);

		for (infoUnit unit : xmlSet) {
			String base = unit.getBase();
			if (base.contains(declareClass)) {
				if (base.equals(declareClass)) {
					/* belong2class and method name defined in different string */
					List<infoPair> marksPairs = unit.getFields();
					infoPair valuesPair = marksPairs.get(marksPairs.size() - 1);
					StringBuilder s = new StringBuilder();

					if (valuesPair.getValue() == null)
						continue;
					// find the method value mark
					StringBuilder fieldLayerMark = new StringBuilder();
					for (String val : valuesPair.getValue().keySet()) {
						if (valuesPair.getValue().get(val).equals(mtdAndParams)
								|| valuesPair.getValue().get(val).equals(mtdName)) {
							if (val.equals("method") || val.equals("mthd") || val.equals("mtd")) {
								fieldLayerMark.append(";");
								fieldLayerMark.append("myType:" + val);
								break;
							} else {
								StringBuilder tmp = new StringBuilder();
								tmp.append(";");
								tmp.append("myType:" + val);
								fieldLayerMark = tmp;
							}
						}
					}
					// find class
					StringBuilder classLayerMark = new StringBuilder();
					for (String val : valuesPair.getValue().keySet()) {
						if (valuesPair.getValue().get(val).equals(base)) {
							classLayerMark.append(";");
							classLayerMark.append("myType:" + val);

						}
					}
					if (fieldLayerMark.length() > 0) {
						for (int i = 0; i < marksPairs.size(); i++) {
							infoPair p = marksPairs.get(i);
							if (i == 0) {
								s.append(p.getMark());
							} else {
								s.append(";");
								s.append(p.getMark());
							}
						}
						ret.add("xml:class:" + s.toString() + classLayerMark.toString());
						ret.add("xml:mtd:" + s.toString() + fieldLayerMark.toString());
						return ret;
					}

				} else if (base.contains(mtdAndParams)) {
					// field and belong2Class is in same string
					List<infoPair> marksPairs = unit.getFields();
					StringBuilder s = new StringBuilder();

					for (int i = 0; i < marksPairs.size(); i++) {
						infoPair p = marksPairs.get(i);
						if (p.getValue() == null)
							continue;
						if (i == 0) {
							s.append(p.getMark());
						} else {
							s.append(";");
							s.append(p.getMark());
						}
						if (i == marksPairs.size() - 1) {
							for (String val : p.getValue().keySet()) {
								if (p.getValue().get(val).equals(base)) {
									s.append(";");
									s.append("myType:" + val);
									break;
								}
							}
						}
					}
					ret.add("xml:full:" + s.toString());
					return ret;
				}
			}
		}
		return ret;
//		return getXMLMarks_common_allMatch(curr, xmlSet);
	}

	public static ArrayList<String> getXMLMarks_class(String curr, Set<infoUnit> xmlSet) {
		ArrayList<String> ret = new ArrayList<String>();
		for (infoUnit unit : xmlSet) {
			String base = unit.getBase();
			if (curr.equals(base)) {
				// method name and class configured in different field

				// find the mark
				StringBuilder s = new StringBuilder();

				List<infoPair> marksPairs = unit.getFields();
				infoPair valuesPair = marksPairs.get(marksPairs.size() - 1);

				if (valuesPair.getValue() == null)
					continue;

				// find class
				StringBuilder classLayerMark = new StringBuilder();
				for (String val : valuesPair.getValue().keySet()) {
					if (valuesPair.getValue().get(val).equals(base)) {
						classLayerMark.append(";");
						classLayerMark.append("myType:" + val);

					}
				}
				for (int i = 0; i < marksPairs.size(); i++) {
					infoPair p = marksPairs.get(i);
					if (i == 0) {
						s.append(p.getMark());
					} else {
						s.append(";");
						s.append(p.getMark());
					}
				}
				ret.add(s.toString() + classLayerMark.toString());
				return ret;

			}
//			else if (curr.equals(base) || base.equals(currClass + currMethodName)) {
//				// whole method configured in the same field
//				getXMLMarks_common_allMatch(curr, xmlSet);
//			}
		}
		return ret;
	}

}
