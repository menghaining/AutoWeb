package ict.pag.webframework.model.core.calculator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Element;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.shrikeCT.AnnotationsReader.ElementValue;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;

import ict.pag.webframework.model.enumeration.CallType;
import ict.pag.webframework.model.enumeration.MarkScope;
import ict.pag.webframework.model.logprase.RTInfoDetailsClassifer;
import ict.pag.webframework.model.logprase.RuntimeCallsiteInfo;
import ict.pag.webframework.model.marks.FrmkCallMark;
import ict.pag.webframework.model.marks.FrmkIndirectCallMark;
import ict.pag.webframework.model.marks.MarksHelper;
import ict.pag.webframework.model.marks.ResolveMarks;
import ict.pag.webframework.model.option.SpecialHelper;

public class IndirectCallCalculator2 {
	/** Initialize */
	private HashMap<String, HashSet<Element>> class2XMLEle;
	private ClassHierarchy cha;
	private RTInfoDetailsClassifer classfier;

	/** Answer */
	HashSet<FrmkIndirectCallMark> frameworkCallMarks = new HashSet<>();
	HashSet<FrmkIndirectCallMark> notFrameworkCallMarks = new HashSet<>();

	/**
	 * Application call framework function, and then framework call back to
	 * application function</br>
	 * 
	 * 1. call back through parameter;</br>
	 * 2. call back through the marks of the called application function;</br>
	 * 3. call back through the inheritance relation. In other words, application
	 * function override framework function. (this case can be resolved by bytecode)
	 */
	public IndirectCallCalculator2(ClassHierarchy cha, HashMap<String, HashSet<Element>> class2xmlEle,
			RTInfoDetailsClassifer classfier) {
		this.class2XMLEle = class2xmlEle;
		this.cha = cha;
		this.classfier = classfier;

		solve();
	}

	private void solve() {
		HashMap<RuntimeCallsiteInfo, HashSet<String>> frmk2seq = classfier.getFrameworkcallsite2seq();
		for (RuntimeCallsiteInfo frmkcs : frmk2seq.keySet()) {
			HashSet<String> seq = frmk2seq.get(frmkcs);

			HashSet<String> mtdallMarks = new HashSet<>();
			HashSet<Annotation> mtdAnnos = new HashSet<>();
			HashSet<Element> mtdXmlEles = new HashSet<>();
			HashSet<String> mtdOverrides = new HashSet<>();
			// 1. find all possible configurations of the method that callsite belong to
			String frmkcall = frmkcs.getCallStmt();
			String belong2methodStr = frmkcs.getBelongToMthd();
			String belong2classStr = "L"
					+ (belong2methodStr.substring(0, belong2methodStr.lastIndexOf('.'))).replaceAll("\\.", "/");
			IClass belong2class = cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Application,
					TypeName.findOrCreate(belong2classStr)));
			if (belong2class != null) {
				IMethod belong2Mtd = null;
				for (IMethod tmp : belong2class.getAllMethods()) {
					if (tmp.getSignature().equals(belong2methodStr)) {
						belong2Mtd = tmp;
						break;
					}
				}
				if (belong2Mtd != null) {
					mtdAnnos = ResolveMarks.resolve_Annotation(belong2Mtd, MarkScope.Method);
					mtdXmlEles = ResolveMarks.resolve_XML(belong2Mtd, MarkScope.Method, class2XMLEle);
					mtdOverrides = ResolveMarks.resolve_ExtendOrImplement(belong2Mtd, MarkScope.Method, cha);
					for (Annotation anno : mtdAnnos) {
						mtdallMarks.add("[anno]" + MarksHelper.resolveAnnotationName(anno));
					}
					for (String mark : ResolveMarks.resolve_XML_String(belong2Mtd, MarkScope.Method, class2XMLEle)) {
						mtdallMarks.add("[xml]" + mark);
					}
					for (String mark : mtdOverrides) {
						mtdallMarks.add("[inheritance]" + mark);
					}
				}
			}

			// 2. find configuration of all possible targets
			for (String mayTar : seq) {
				// mayInvoke info
				String mayInvokeClassStr = mayTar.substring(0, mayTar.lastIndexOf('.'));
				String clazz0 = "L" + mayInvokeClassStr.replaceAll("\\.", "/");
				IClass mayTarClass = cha.lookupClass(
						TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz0)));
				if (mayTarClass == null)
					continue;

				IMethod mtd = null;
				for (IMethod tmp : mayTarClass.getAllMethods()) {
					if (tmp.getSignature().equals(mayTar)) {
						mtd = tmp;
						break;
					}
				}
				if (mtd != null) {
					HashSet<Annotation> annos_tar = ResolveMarks.resolve_Annotation(mtd, MarkScope.Method);
					HashSet<Element> xmlEles_tar = ResolveMarks.resolve_XML(mtd, MarkScope.Method, class2XMLEle);
					if (!annos_tar.isEmpty() || !xmlEles_tar.isEmpty()) {
						HashSet<String> tarAllMarks = new HashSet<>();
						boolean findMatchedValue = false;

						/* policy 2: compare the value of each configuration */
						// if find the equal value, record the equal value mark as positive
						// and record the other mark as negative
						// otherwise, only record the configuration mark
						HashSet<String> matchedMtdAllMarks = new HashSet<>();
						HashSet<String> mtdTmpMarks = new HashSet<>();
						// a. belong2method-anno-values
						for (Annotation anno : mtdAnnos) {
							Map<String, ElementValue> map = anno.getNamedArguments();
							boolean finded = false;
							for (String key : map.keySet()) {
								ElementValue val0 = map.get(key);
								String val = SpecialHelper.reFormatAnnosValue(val0);
								if (val != null) {
									finded = findMatchedConfigurationValue(val, annos_tar, xmlEles_tar, tarAllMarks,
											mtd);
									if (finded) {
										break;
									}
								}
							}
							if (finded) {
								matchedMtdAllMarks.add("[anno]" + MarksHelper.resolveAnnotationName(anno));
								findMatchedValue = true;
							} else {
								mtdTmpMarks.add("[anno]" + MarksHelper.resolveAnnotationName(anno));
							}
						}
						// b. belong2method-xml-values
						for (Element xmlEle : mtdXmlEles) {
							HashSet<String> mtdConfs = search4XMLPath(xmlEle, mtd.getSignature());

							String path = xmlEle.getName();
							Element parent = xmlEle.getParent();
							while (parent != null) {
								path = parent.getName().concat(";").concat(path);
								parent = parent.getParent();
							}
							boolean finded = false;
							// this layer
							for (Object attr0 : xmlEle.attributes()) {
								if (attr0 instanceof Attribute) {
									String val = ((Attribute) attr0).getValue();
									String name = ((Attribute) attr0).getName();
									finded = findMatchedConfigurationValue(val, annos_tar, xmlEles_tar, tarAllMarks,
											mtd);

									if (finded) {
										break;
									}
								}
							}
							if (finded) {
								findMatchedValue = true;
								if (!mtdConfs.isEmpty())
									matchedMtdAllMarks.addAll(mtdConfs);
							} else {
								// sub-layer
								for (Object child0 : xmlEle.elements()) {
									if (child0 instanceof Element) {
										for (Object attr0 : ((Element) child0).attributes()) {
											String path2 = path.concat(";").concat(((Element) child0).getName());
											if (attr0 instanceof Attribute) {
												String val = ((Attribute) attr0).getValue();
												String name = ((Attribute) attr0).getName();
												finded = findMatchedConfigurationValue(val, annos_tar, xmlEles_tar,
														tarAllMarks, mtd);
												if (finded) {
													break;
												}
											}
										}
									}
								}
								if (finded) {
									findMatchedValue = true;
									if (!mtdConfs.isEmpty())
										matchedMtdAllMarks.addAll(mtdConfs);
								} else {
									if (!mtdConfs.isEmpty())
										mtdTmpMarks.addAll(mtdConfs);
								}
							}
						}
						if (findMatchedValue) {
							// a. add into positive set
							FrmkIndirectCallMark mark = new FrmkIndirectCallMark(matchedMtdAllMarks, CallType.Attribute,
									frmkcall, tarAllMarks);
							frameworkCallMarks.add(mark);
							// b. add into negative set
							HashSet<String> tmp = new HashSet<>();
							for (Annotation anno : annos_tar) {
								String s = "[anno]" + MarksHelper.resolveAnnotationName(anno);
								if (!tarAllMarks.contains(s))
									tmp.add(s);
							}
							for (String m : ResolveMarks.resolve_XML_String(mtd, MarkScope.Method, class2XMLEle)) {
								String s = "[xml]" + m;
								if (!tarAllMarks.contains(s))
									tmp.add(s);
							}
							if (!tmp.isEmpty()) {
								FrmkIndirectCallMark mark_neg = new FrmkIndirectCallMark(mtdTmpMarks,
										CallType.Attribute, frmkcall, tmp);
								notFrameworkCallMarks.add(mark_neg);
							}

						} else {
							/* policy 1: only record the configuration */
							for (Annotation anno : annos_tar) {
								tarAllMarks.add("[anno]" + MarksHelper.resolveAnnotationName(anno));
							}
							for (String mark : ResolveMarks.resolve_XML_String(mtd, MarkScope.Method, class2XMLEle)) {
								tarAllMarks.add("[xml]" + mark);
							}
							if (!tarAllMarks.isEmpty()) {
								FrmkIndirectCallMark mark = new FrmkIndirectCallMark(mtdallMarks, CallType.Attribute,
										frmkcall, tarAllMarks);
								frameworkCallMarks.add(mark);
							}
						}

					}

				}
			}
		}

	}

	private HashSet<String> search4XMLPath(Element xmlEle, String fullSig) {
		HashSet<String> ret = new HashSet<>();

		String fullMethodName = fullSig.substring(0, fullSig.indexOf('('));
		String classString = fullSig.substring(0, fullSig.lastIndexOf('.'));
		String sig = fullSig.substring(classString.length() + 1);
		String mthName = sig.substring(0, sig.indexOf('('));

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
			return ret;
		String text = xmlEle.getText();
		if (text.contains(fullMethodName) || text.contains(mthName)) {
			path = path.concat(":").concat("[text]");
			path = path + classConfig;
			ret.add(path);
		}
		// sub-level find
		if (find)
			return ret;
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
		return ret;
	}

	/**
	 * @param val         : the value to matched
	 * @param tarAllMarks : if matched, add the mark into this set
	 */
	private boolean findMatchedConfigurationValue(String value, HashSet<Annotation> annos_tar,
			HashSet<Element> xmlEles_tar, HashSet<String> tarAllMarks, IMethod mtd) {
		// 1. anno
		for (Annotation anno : annos_tar) {
			Map<String, ElementValue> map = anno.getNamedArguments();
			for (String key : map.keySet()) {
				ElementValue val0 = map.get(key);
				String val = SpecialHelper.reFormatAnnosValue(val0);
				if (val != null) {
					if (val.equals(value)) {
						tarAllMarks.add("[anno]" + MarksHelper.resolveAnnotationName(anno));
						return true;
					}
				}
			}
		}

		// 2. xml
		for (Element xmlEle : xmlEles_tar) {
			HashSet<String> mtdConfs = search4XMLPath(xmlEle, mtd.getSignature());

			String path = xmlEle.getName();
			Element parent = xmlEle.getParent();
			while (parent != null) {
				path = parent.getName().concat(";").concat(path);
				parent = parent.getParent();
			}
			// this layer
			for (Object attr0 : xmlEle.attributes()) {
				if (attr0 instanceof Attribute) {
					String val = ((Attribute) attr0).getValue();
					String name = ((Attribute) attr0).getName();
					if (val.equals(value)) {
						if (!mtdConfs.isEmpty())
							tarAllMarks.addAll(mtdConfs);
						return true;
					}
				}
			}
			// sub-layer
			for (Object child0 : xmlEle.elements()) {
				if (child0 instanceof Element) {
					for (Object attr0 : ((Element) child0).attributes()) {
						String path2 = path.concat(";").concat(((Element) child0).getName());
						if (attr0 instanceof Attribute) {
							String val = ((Attribute) attr0).getValue();
							String name = ((Attribute) attr0).getName();
							if (val.equals(value)) {
								if (!mtdConfs.isEmpty())
									tarAllMarks.addAll(mtdConfs);
								return true;
							}
						}
					}
				}
			}

		}
		return false;
	}

	public HashSet<FrmkIndirectCallMark> getFrameworkCallMarks() {
		return frameworkCallMarks;
	}

	public HashSet<FrmkIndirectCallMark> getNotFrameworkCallMarks() {
		return notFrameworkCallMarks;
	}

}
