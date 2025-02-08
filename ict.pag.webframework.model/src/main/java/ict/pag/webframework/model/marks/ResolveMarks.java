package ict.pag.webframework.model.marks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.dom4j.Attribute;
import org.dom4j.Element;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.annotations.Annotation;

import ict.pag.webframework.model.enumeration.MarkScope;
import ict.pag.webframework.model.option.ConfigUtil;
import ict.pag.webframework.model.option.SpecialHelper;

public class ResolveMarks {

	/**
	 * the annotation Set only contains framework annotations
	 * 
	 * @return return the annotation Set that corresponding to the given Object
	 */
	public static HashSet<Annotation> resolve_Annotation(Object obj, MarkScope type) {
		HashSet<Annotation> ret = new HashSet<>();
		if (type.equals(MarkScope.Clazz)) {
			if (obj instanceof IClass) {
				IClass clazz = (IClass) obj;
				Collection<Annotation> allAnnos = clazz.getAnnotations();
				if (allAnnos != null && !allAnnos.isEmpty()) {
					for (Annotation anno : allAnnos) {
//						ClassLoaderReference cref = anno.getType().getClassLoader();
						String annoStr = anno.getType().getName().toString();
//						if (!ConfigUtil.isApplicationClass(annoStr) && ConfigUtil.isFrameworkMarks(annoStr)) {
						if (ConfigUtil.isFrameworkMarks(annoStr)) {
							ret.add(anno);
						}
					}
				}
			}

		} else if (type.equals(MarkScope.Method)) {
			if (obj instanceof IMethod) {
				IMethod mtd = (IMethod) obj;
				Collection<Annotation> allAnnos = mtd.getAnnotations();
				if (allAnnos != null && !allAnnos.isEmpty()) {
					for (Annotation anno : allAnnos) {
//						ClassLoaderReference cref = anno.getType().getClassLoader();
						String annoStr = anno.getType().getName().toString();
//						if (!ConfigUtil.isApplicationClass(annoStr) && ConfigUtil.isFrameworkMarks(annoStr)) {
						if (ConfigUtil.isFrameworkMarks(annoStr)) {
							ret.add(anno);
						}
					}
				}
			}

		} else if (type.equals(MarkScope.Field)) {
			if (obj instanceof IField) {
				IField field = (IField) obj;
				Collection<Annotation> allAnnos = field.getAnnotations();
				if (allAnnos != null && !allAnnos.isEmpty()) {
					for (Annotation anno : allAnnos) {
//						ClassLoaderReference cref = anno.getType().getClassLoader();
						String annoStr = anno.getType().getName().toString();
//						if (!ConfigUtil.isApplicationClass(annoStr) && ConfigUtil.isFrameworkMarks(annoStr)) {
						if (ConfigUtil.isFrameworkMarks(annoStr)) {
							ret.add(anno);
						}
					}
				}
			}
		}

		return ret;
	}

	public static HashSet<Element> resolve_XML(Object obj, MarkScope type,
			HashMap<String, HashSet<Element>> class2XMLEle) {
		HashSet<Element> ret = new HashSet<>();

		if (class2XMLEle.isEmpty())
			return ret;

		if (type.equals(MarkScope.Clazz)) {
			if (obj instanceof IClass) {
				IClass clazz = (IClass) obj;
				String clazzString = SpecialHelper.formatSignature(clazz.getName().toString());
				if (class2XMLEle.containsKey(clazzString)) {
					HashSet<Element> xmlEles = class2XMLEle.get(clazzString);
					for (Element xmlEle : xmlEles) {
						for (Object attr0 : xmlEle.attributes()) {
							if (attr0 instanceof Attribute) {
								String val = ((Attribute) attr0).getValue();
								String name = ((Attribute) attr0).getName();
								if (val != null && name != null)
									if (val.equals(clazzString) && (name.toLowerCase().equals("class")
											|| name.toLowerCase().equals("clazz"))) {
										ret.add(xmlEle);
									}
							}
						}
					}
				}
			}
		} else if (type.equals(MarkScope.Method)) {
			/** for field, return the element of method level */
			if (obj instanceof IMethod) {
				IMethod mtd = (IMethod) obj;
				String fullSig = mtd.getSignature();
				String classString = fullSig.substring(0, fullSig.lastIndexOf('.'));
				String fullMethodName = fullSig.substring(0, fullSig.indexOf('('));
				String sig = fullSig.substring(classString.length() + 1);
				String mthName = sig.substring(0, sig.indexOf('('));
				if (class2XMLEle.containsKey(classString)) {
					HashSet<Element> xmlEles = class2XMLEle.get(classString);
					for (Element xmlEle : xmlEles) {
						// current level find
						boolean find = false;
						for (Object attr0 : xmlEle.attributes()) {
							if (attr0 instanceof Attribute) {
								String val = ((Attribute) attr0).getValue();
								String name = ((Attribute) attr0).getName();
								// val equals or contains 'p1.p2.p3.c1.method'
								// val equals or contains 'methodName'
								if (val != null && name != null)
									if (val.contains(fullMethodName) || val.contains(mthName)) {
										if (name.toLowerCase().equals("method")) {
											ret.add(xmlEle);
											find = true;
										}
									}
							}
						}
						if (find)
							continue;
						String text = xmlEle.getText();
						if (text.contains(fullMethodName) || text.contains(mthName)) {
							ret.add(xmlEle);
							find = true;
						}
						if (find)
							continue;
						// sub-level find
						for (Object child0 : xmlEle.elements()) {
							if (child0 instanceof Element) {
								for (Object attr0 : ((Element) child0).attributes()) {
									String val = ((Attribute) attr0).getValue();
									String name = ((Attribute) attr0).getName();
									// val equals or contains 'p1.p2.p3.c1.method'
									// val equals or contains 'methodName'
									if (val != null && name != null)
										if (val.contains(fullMethodName) || val.contains(mthName)) {
											if (name.toLowerCase().equals("method")) {
												ret.add((Element) child0);
											}
										}
								}
							}
						}
					}
				}
			}

		} else if (type.equals(MarkScope.Field)) {
			/** for field, return the element of field level */
			if (obj instanceof IField) {
				IField field = (IField) obj;
				String fieldName = field.getName().toString();
				String classString = SpecialHelper
						.formatSignature(field.getReference().getDeclaringClass().getName().toString());
				if (class2XMLEle.containsKey(classString)) {
					HashSet<Element> xmlEles = class2XMLEle.get(classString);
					for (Element xmlEle : xmlEles) {
						// current level
						boolean find = false;
						for (Object attr0 : xmlEle.attributes()) {
							if (attr0 instanceof Attribute) {
								String val = ((Attribute) attr0).getValue();
								String name = ((Attribute) attr0).getName();
								if (val != null && name != null && !name.toLowerCase().contains("method")
										&& !name.toLowerCase().equals("class"))
//									if (val.toLowerCase().equals(fieldName.toLowerCase())) {
									if (val.equals(fieldName)) {
										ret.add(xmlEle);
										find = true;
									}
							}
						}
						if (find)
							continue;
						// sub-level
						for (Object child0 : xmlEle.elements()) {
							if (child0 instanceof Element) {
								for (Object attr0 : ((Element) child0).attributes()) {
									String val = ((Attribute) attr0).getValue();
									String name = ((Attribute) attr0).getName();
									if (val != null && name != null && !name.toLowerCase().contains("method")
											&& !name.toLowerCase().equals("class"))
//										if (val.toLowerCase().equals(fieldName.toLowerCase())) {
										if (val.equals(fieldName)) {
											ret.add((Element) child0);
										}
								}
							}
						}
					}
				}
			}

		}
		return ret;
	}

	public static HashSet<String> resolve_XML_String_notGivenAttr(Object obj, MarkScope type,
			HashMap<String, HashSet<Element>> class2XMLEle) {
		HashSet<String> ret = new HashSet<>();
		if (class2XMLEle.isEmpty())
			return ret;
		if (type.equals(MarkScope.Field)) {
			if (obj instanceof IField) {
				IField field = (IField) obj;
				String fieldName = field.getName().toString();
				String classString = SpecialHelper
						.formatSignature(field.getReference().getDeclaringClass().getName().toString());
				if (class2XMLEle.containsKey(classString)) {
					HashSet<Element> xmlEles = class2XMLEle.get(classString);
					for (Element xmlEle : xmlEles) {
						String path = xmlEle.getName();
						Element parent = xmlEle.getParent();
						while (parent != null) {
							path = parent.getName().concat(";").concat(path);
							parent = parent.getParent();
						}
						// class configured
						String classConfig = "[class]" + path;

						// current level find
						boolean find = false;
						HashSet<String> tmp = new HashSet<>();
						boolean findAttr = false;
						for (Object attr0 : xmlEle.attributes()) {
							if (attr0 instanceof Attribute) {
								String val = ((Attribute) attr0).getValue();
								String name = ((Attribute) attr0).getName();
								if (val != null && name != null && !name.toLowerCase().contains("method")
										&& !name.toLowerCase().equals("class"))
//									if (val.toLowerCase().equals(fieldName.toLowerCase())) {
									if (val.equals(fieldName)) {
										find = true;
										findAttr = true;
									} else {
										String path3 = path.concat(":").concat(name);
										path3 = path3 + classConfig;
										tmp.add(path3);
									}
							}
						}
						if (findAttr)
							if (!tmp.isEmpty())
								ret.addAll(tmp);
						tmp.clear();
						if (find) {
							continue;
						}
						// sub-level
						tmp.clear();
						for (Object child0 : xmlEle.elements()) {
							String tmpPath = path.concat(";").concat(((Element) child0).getName());
							if (child0 instanceof Element) {
								findAttr = false;
								for (Object attr0 : ((Element) child0).attributes()) {
									String val = ((Attribute) attr0).getValue();
									String name = ((Attribute) attr0).getName();
									if (val != null && name != null && !name.toLowerCase().contains("method")
											&& !name.toLowerCase().equals("class"))
//										if (val.toLowerCase().equals(fieldName.toLowerCase())) {
										if (val.equals(fieldName)) {
//											String path2 = tmpPath.concat(":").concat(name);
//											path2 = path2 + classConfig;
//											ret.add(path2);
											find = true;
											findAttr = true;
										} else {
											String path3 = tmpPath.concat(":").concat(name);
											path3 = path3 + classConfig;
											tmp.add(path3);
										}
								}
								if (findAttr)
									if (!tmp.isEmpty())
										ret.addAll(tmp);
								tmp.clear();
							}
						}
					}
				}
			}
		}

		return ret;

	}

	public static HashSet<String> resolve_XML_String(Object obj, MarkScope type,
			HashMap<String, HashSet<Element>> class2XMLEle) {
		HashSet<String> ret = new HashSet<>();

		if (class2XMLEle.isEmpty())
			return ret;

		if (type.equals(MarkScope.Clazz)) {
			if (obj instanceof IClass) {
				IClass clazz = (IClass) obj;
				String clazzString = SpecialHelper.formatSignature(clazz.getName().toString());
				if (class2XMLEle.containsKey(clazzString)) {
					HashSet<Element> xmlEles = class2XMLEle.get(clazzString);
					for (Element xmlEle : xmlEles) {
						String path = xmlEle.getName();
						Element parent = xmlEle.getParent();
						while (parent != null) {
							path = parent.getName().concat(";").concat(path);
							parent = parent.getParent();
						}

						// attribute
						for (Object attr0 : xmlEle.attributes()) {
							if (attr0 instanceof Attribute) {
								String val = ((Attribute) attr0).getValue();
								String name = ((Attribute) attr0).getName();
								if (val != null && name != null)
									if (val.equals(clazzString) || val.toLowerCase().equals(
											clazzString.substring(clazzString.lastIndexOf('.') + 1).toLowerCase())) {
										if (name.toLowerCase().equals("class") || name.toLowerCase().equals("clazz")) {
											String path2 = path.concat(":").concat(name);
											ret.add(path2);
										}
									} else if (val.equals(clazzString)) {
										String path2 = path.concat(":").concat(name);
										ret.add(path2);
									}
							}
						}
						// text
						String text = xmlEle.getText();
						if (text.equals(clazzString)) {
							path = path.concat(":").concat("[text]");
							ret.add(path);
						}
					}
				}
			}
		} else if (type.equals(MarkScope.Method)) {
			if (obj instanceof IMethod) {
				IMethod mtd = (IMethod) obj;
				String fullSig = mtd.getSignature();
				String classString = fullSig.substring(0, fullSig.lastIndexOf('.'));
				String fullMethodName = fullSig.substring(0, fullSig.indexOf('('));
				String sig = fullSig.substring(classString.length() + 1);
				String mthName = sig.substring(0, sig.indexOf('('));
				// find the class first
				if (class2XMLEle.containsKey(classString)) {
					HashSet<Element> xmlEles = class2XMLEle.get(classString);
					for (Element xmlEle : xmlEles) {
						String path = xmlEle.getName();
						Element parent = xmlEle.getParent();
						while (parent != null) {
							path = parent.getName().concat(";").concat(path);
							parent = parent.getParent();
						}
						// class configured
						String classConfig = "[class]" + path;

						// current level find
						boolean find = false;
						for (Object attr0 : xmlEle.attributes()) {
							if (attr0 instanceof Attribute) {
								String val = ((Attribute) attr0).getValue();
								String name = ((Attribute) attr0).getName();
								// val equals or contains 'p1.p2.p3.c1.method'
								// val equals or contains 'methodName'
								if (val != null && name != null)
									if (val.contains(fullMethodName) || val.contains(mthName)) {
										if (name.toLowerCase().equals("method")) {
											String path2 = path.concat(":").concat(name);
											path2 = path2 + classConfig;
											ret.add(path2);
											find = true;
										}
									}
							}
						}
						if (find)
							continue;
						String text = xmlEle.getText();
						if (text.contains(fullMethodName) || text.contains(mthName)) {
							path = path.concat(":").concat("[text]");
							path = path + classConfig;
							ret.add(path);
						}
						// sub-level find
						if (find)
							continue;
						// sub-level find
						for (Object child0 : xmlEle.elements()) {
							if (child0 instanceof Element) {
								String tmpPath = path.concat(";").concat(((Element) child0).getName());
								for (Object attr0 : ((Element) child0).attributes()) {
									String val = ((Attribute) attr0).getValue();
									String name = ((Attribute) attr0).getName();
									// val equals or contains 'p1.p2.p3.c1.method'
									// val equals or contains 'methodName'
									if (val != null && name != null)
										if (val.contains(fullMethodName) || val.contains(mthName)) {
											if (name.toLowerCase().equals("method")) {
												String path2 = tmpPath.concat(":").concat(name);
												path2 = path2 + classConfig;
												ret.add(path2);
											}
										}
								}
							}
						}

					}
				}
			}

		} else if (type.equals(MarkScope.Field)) {
			if (obj instanceof IField) {
				IField field = (IField) obj;
				String fieldName = field.getName().toString();
				String classString = SpecialHelper
						.formatSignature(field.getReference().getDeclaringClass().getName().toString());
				if (class2XMLEle.containsKey(classString)) {
					HashSet<Element> xmlEles = class2XMLEle.get(classString);
					for (Element xmlEle : xmlEles) {
						String path = xmlEle.getName();
						Element parent = xmlEle.getParent();
						while (parent != null) {
							path = parent.getName().concat(";").concat(path);
							parent = parent.getParent();
						}
						// class configured
						String classConfig = "[class]" + path;

						// current level find
						boolean find = false;
						for (Object attr0 : xmlEle.attributes()) {
							if (attr0 instanceof Attribute) {
								String val = ((Attribute) attr0).getValue();
								String name = ((Attribute) attr0).getName();
								if (val != null && name != null && !name.toLowerCase().contains("method")
										&& !name.toLowerCase().equals("class"))
//									if (val.toLowerCase().equals(fieldName.toLowerCase())) {
									if (val.equals(fieldName)) {
										String path2 = path.concat(":").concat(name);
										path2 = path2 + classConfig;
										ret.add(path2);
										find = true;
									}
							}
						}
						if (find)
							continue;
						// sub-level
						for (Object child0 : xmlEle.elements()) {
							String tmpPath = path.concat(";").concat(((Element) child0).getName());
							if (child0 instanceof Element) {
								for (Object attr0 : ((Element) child0).attributes()) {
									String val = ((Attribute) attr0).getValue();
									String name = ((Attribute) attr0).getName();
									if (val != null && name != null && !name.toLowerCase().contains("method")
											&& !name.toLowerCase().equals("class"))
//										if (val.toLowerCase().equals(fieldName.toLowerCase())) {
										if (val.equals(fieldName)) {
											String path2 = tmpPath.concat(":").concat(name);
											path2 = path2 + classConfig;
											ret.add(path2);
										}
								}
							}
						}
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Find the Framework class/interfaces that given obj extends/implements</br>
	 * </br>
	 * If the declared super-class of obj is <B>not</B> framework class/interfaces,
	 * iterative to lookup until find framework defined class/interfaces, or return
	 * empty. </br>
	 * 
	 * @param obj
	 * @param type
	 * @param cha  the cha is application and lib in all<br>
	 * @return like 'Ljava/lang/String'
	 */
	public static HashSet<String> resolve_Inheritance(Object obj, MarkScope type, ClassHierarchy cha) {
		HashSet<String> ret = new HashSet<>();

		if (type.equals(MarkScope.Clazz)) {
			if (obj instanceof IClass) {
				IClass objClass = (IClass) obj;
				IClass clazz = cha.getLoader(ClassLoaderReference.Application).lookupClass(objClass.getName());
				HashSet<IClass> tmp = new HashSet<>();
				collectAllFrameworkInheritance(clazz, tmp);
				for (IClass t : tmp) {
					// like 'Ljava/lang/String'
					ret.add(t.getName().toString());
				}
			}
		} else if (type.equals(MarkScope.Method)) {
			if (obj instanceof IMethod) {
				IMethod objMtd = (IMethod) obj;
				IClass clazz = objMtd.getDeclaringClass();
				HashSet<String> superSet = resolve_Inheritance(clazz, MarkScope.Clazz, cha);
				for (String ss : superSet) {
					IClass superClass = cha.getLoader(ClassLoaderReference.Application)
							.lookupClass(TypeName.findOrCreate(ss));
					if (superClass == null)
						continue;
					for (IMethod dcC : superClass.getDeclaredMethods()) {
						if (mayOverriteMethod(objMtd.getSignature(), dcC.getSignature())) {
							ret.add(dcC.getSignature());
							break;
						}
					}
				}
			}
		}
		return ret;
	}

	public static HashSet<String> resolve_ExtendOrImplement(Object obj, MarkScope type, ClassHierarchy cha) {
		HashSet<String> ret = new HashSet<>();

		if (type.equals(MarkScope.Clazz)) {
			if (obj instanceof IClass) {
				IClass objClass = (IClass) obj;
				IClass clazz = cha.getLoader(ClassLoaderReference.Application).lookupClass(objClass.getName());
				HashSet<IClass> tmp = new HashSet<>();
				collectAllFrameworkInheritance(clazz, tmp);
				for (IClass t : tmp) {
					// like 'Ljava/lang/String'
					if (t.isInterface())
						ret.add(t.getName().toString() + "[implement]");
					else
						ret.add(t.getName().toString() + "[extends]");
				}
			}
		} else if (type.equals(MarkScope.Method)) {
			if (obj instanceof IMethod) {
				IMethod objMtd = (IMethod) obj;
				IClass clazz = objMtd.getDeclaringClass();
				HashSet<String> superSet = resolve_ExtendOrImplement(clazz, MarkScope.Clazz, cha);
				for (String ss : superSet) {
					ss = ss.substring(0, ss.lastIndexOf('['));
					IClass superClass = cha.getLoader(ClassLoaderReference.Application)
							.lookupClass(TypeName.findOrCreate(ss));
					if (superClass == null)
						continue;
					for (IMethod dcC : superClass.getDeclaredMethods()) {
						if (mayOverriteMethod(objMtd.getSignature(), dcC.getSignature())) {
							if (superClass.isInterface())
								ret.add(dcC.getSignature() + "[implement]");
							else
								ret.add(dcC.getSignature() + "[extends]");
							break;
						}
					}
				}
				String sig = objMtd.getSignature();
				String sig2 = sig.substring(sig.lastIndexOf('.') + 1);
				if (objMtd.isStatic() && sig2.equals("main([Ljava/lang/String;)V"))
					ret.add(sig2 + "[MAIN]");
			}
		}
		return ret;
	}

	private static void collectAllFrameworkInheritance(IClass clazz, HashSet<IClass> ret) {
		if (clazz.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
			// 1. find the inheritance relation immediately on it
			// 2. find the inheritance relation of its super class or interfaces
			if (clazz.isInterface()) {
				Collection<? extends IClass> superClassSet = clazz.getDirectInterfaces();
				for (IClass superClass : superClassSet) {
					if (superClass != null) {
						if (superClass.getClassLoader().getReference().equals(ClassLoaderReference.Primordial)) {
							if (ConfigUtil
									.isFrameworkMarks(SpecialHelper.formatSignature(superClass.getName().toString()))) {
								ret.add(superClass);
							}
						} else {
							collectAllFrameworkInheritance(superClass, ret);
						}
					}
				}
			} else {
				// super class
				IClass superClazz = clazz.getSuperclass();
				if (superClazz != null) {
					if (superClazz.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
						collectAllFrameworkInheritance(superClazz, ret);
					} else {
						if (ConfigUtil
								.isFrameworkMarks(SpecialHelper.formatSignature(superClazz.getName().toString()))) {
							ret.add(superClazz);
						}
					}
				}

				// interface
				Collection<? extends IClass> superClassSet = clazz.getDirectInterfaces();
				for (IClass superClass : superClassSet) {
					if (superClass != null) {
						if (superClass.getClassLoader().getReference().equals(ClassLoaderReference.Primordial)) {
							if (ConfigUtil
									.isFrameworkMarks(SpecialHelper.formatSignature(superClass.getName().toString()))) {
								ret.add(superClass);
							}
						} else {
							collectAllFrameworkInheritance(superClass, ret);
						}
					}
				}
			}

		}
	}

	/**
	 * signature1 and signature2 both full signature of method
	 * 
	 */
	private static boolean mayOverriteMethod(String signature1, String signature2) {
		String sig1 = signature1.substring(signature1.lastIndexOf('.') + 1);
		String sig2 = signature2.substring(signature2.lastIndexOf('.') + 1);

		// signature all same
		if (sig1.equals(sig2))
			return true;

		// method name is same
		// parameters are same and return type are same
		String name1 = sig1.substring(0, sig1.indexOf('('));
		String name2 = sig2.substring(0, sig2.indexOf('('));
		String paramsList1 = sig1.substring(sig1.indexOf('(') + 1);
		String paramsList2 = sig2.substring(sig2.indexOf('(') + 1);
		ArrayList<String> list1 = calculateParams(paramsList1);
		ArrayList<String> list2 = calculateParams(paramsList2);
		boolean isSame = false;
		if (name1.equals(name2) && list1.size() == list2.size()) {
			isSame = true;
			for (int i = 0; i < list1.size(); i++) {
				String t1 = list1.get(i);
				String t2 = list2.get(i);
				if (t1.contains("/") && t2.contains("/")) {
					// TODO: is inheritance?
					// ignore
					continue;
				}
				if (!t1.equals(t2)) {
					isSame = false;
					break;
				}
			}
		}
		if (isSame)
			return true;

		return false;
	}

	private static ArrayList<String> calculateParams(String s) {
		ArrayList<String> ret = new ArrayList<>();
		// params
		if (s.indexOf('(') + 1 != s.indexOf(')')) {
			// if has parameter
			String paramsString = s.substring(s.indexOf('(') + 1, s.indexOf(')'));

			for (int i = 0; i < paramsString.length();) {
				char c = paramsString.charAt(i);
				switch (c) {
				case 'Z':
					i++;
					ret.add("Z");
					break;
				case 'B':
					i++;
					ret.add("B");
					break;
				case 'C':
					i++;
					ret.add("C");
					break;
				case 'S':
					i++;
					ret.add("S");
					break;
				case 'I':
					i++;
					ret.add("I");
					break;
				case 'J':
					i++;
					ret.add("J");
					break;
				case 'F':
					i++;
					ret.add("F");
					break;
				case 'D':
					i++;
					ret.add("D");
					break;
				case 'L':
					String sub0 = paramsString.substring(i);
					String param1 = sub0.substring(0, sub0.indexOf(';'));
					ret.add(param1);
					i = i + sub0.indexOf(';') + 1;
					break;
				default:
					i++;
				}
			}
		}
		// return
		ret.add(s.substring(s.indexOf(')') + 1));
		return ret;
	}
}
