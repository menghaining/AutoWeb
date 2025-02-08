package ict.pag.webframework.model.core.calculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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

import ict.pag.webframework.model.enumeration.MarkScope;
import ict.pag.webframework.model.logprase.RTInfoDetailsClassifer;
import ict.pag.webframework.model.marks.MarksHelper;
import ict.pag.webframework.model.marks.ResolveMarks;
import ict.pag.webframework.model.marks.SequenceCallMark;
import ict.pag.webframework.model.option.SpecialHelper;

public class CallSequenceCalculator {
	/** Initialize */
	private HashMap<String, HashSet<Element>> class2XMLEle;
	private ClassHierarchy cha;
	private RTInfoDetailsClassifer classfier;

	/** Answer **/
	private HashSet<SequenceCallMark> seqConfigSet = new HashSet<>();

	public CallSequenceCalculator(HashMap<String, HashSet<Element>> class2xmlEle, ClassHierarchy cha,
			RTInfoDetailsClassifer classfier) {
		class2XMLEle = class2xmlEle;
		this.cha = cha;
		this.classfier = classfier;

		solve();
	}

	/**
	 * 1. find ordered pair</br>
	 * 2. match path
	 */
	private void solve() {
		// step1
		// arraylist only contains two objets
		HashSet<ArrayList<String>> pairSet = new HashSet<>();
		for (ArrayList<String> seq : classfier.getCallSeqSet()) {
			if (seq.size() < 2)
				continue;

			for (int i = 0; i < seq.size(); i++) {
				for (int j = i + 1; j < seq.size(); j++) {
					ArrayList<String> pair = new ArrayList<String>();
					if (!seq.get(i).equals(seq.get(j))) {
						pair.add(seq.get(i));
						pair.add(seq.get(j));
						pairSet.add(pair);
					}

				}
			}
		}
		// step 2. find the possible matched path value
		for (ArrayList<String> pair : pairSet) {
			String pre = pair.get(0);
			String sec = pair.get(1);

			IMethod mtd1 = findMethod(pre);
			IMethod mtd2 = findMethod(sec);

			if (mtd1 == null || mtd2 == null)
				continue;

			HashSet<Annotation> annos_pre = ResolveMarks.resolve_Annotation(mtd1, MarkScope.Method);
			HashSet<Element> xmlEles_pre = ResolveMarks.resolve_XML(mtd1, MarkScope.Method, class2XMLEle);

			HashSet<Annotation> annos_sec = ResolveMarks.resolve_Annotation(mtd2, MarkScope.Method);
			HashSet<Element> xmlEles_sec = ResolveMarks.resolve_XML(mtd2, MarkScope.Method, class2XMLEle);

			if ((annos_pre.isEmpty() && xmlEles_pre.isEmpty()) || (annos_sec.isEmpty() && xmlEles_sec.isEmpty()))
				continue;

			// find all value that may be path
			HashSet<String> preAllValues = collectAllConfiguredValues(annos_pre, xmlEles_pre);
			HashSet<String> secAllValues = collectAllConfiguredValues(annos_sec, xmlEles_pre);
			if (preAllValues.isEmpty() || secAllValues.isEmpty())
				continue;

			// match possible path
			boolean find = false;
			for (String s1 : preAllValues) {
				for (String s2 : secAllValues) {
					// equal
					if (s1.equals(s2)) {
						find = true;
						break;
					}

					// regular match
					if (isRegularMatch(s1, s2)) {
						find = true;
						break;
					}

					// may matched
					if (s1.startsWith(s2) || s2.startsWith(s1)) {
						find = true;
						break;
					}

				}
			}

			if (find) {
				/* record answer */
				HashSet<String> preConfigSet = buildConfigs(annos_pre, mtd1);
				HashSet<String> ConfigSet = buildConfigs(annos_sec, mtd2);

				seqConfigSet.add(new SequenceCallMark(MarkScope.Method, preConfigSet, ConfigSet));
			}
		}
	}

	private boolean isRegularMatch(String s1, String s2) {
		try {
			if (Pattern.matches(s1, s2)) {
				return true;
			} else if (Pattern.matches(s2, s1)) {
				return true;
			}
		} catch (PatternSyntaxException e) {
			return false;
		}
		return false;
	}

	private HashSet<String> buildConfigs(HashSet<Annotation> annos, IMethod mtd) {
		HashSet<String> ret = new HashSet<>();
		for (Annotation anno : annos) {
			ret.add("[anno]" + MarksHelper.resolveAnnotationName(anno));
		}
		for (String mark : ResolveMarks.resolve_XML_String(mtd, MarkScope.Method, class2XMLEle)) {
			ret.add("[xml]" + mark);
		}
		return ret;
	}

	private HashSet<String> collectAllConfiguredValues(HashSet<Annotation> annos, HashSet<Element> xmlEles) {
		HashSet<String> ret = new HashSet<>();
		for (Annotation anno : annos) {
			Map<String, ElementValue> map = anno.getNamedArguments();
			for (String key : map.keySet()) {
				ElementValue val0 = map.get(key);
				String val = SpecialHelper.reFormatAnnosValue(val0);
				if (val != null) {
					if (val.length() != 0)
						ret.add(val);
				}
			}
		}

		for (Element xmlEle : xmlEles) {
			// this layer
			for (Object attr0 : xmlEle.attributes()) {
				if (attr0 instanceof Attribute) {
					String val = ((Attribute) attr0).getValue();
					if (val != null && val.length() != 0)
						ret.add(val);
				}
			}
			// sub-layer
			for (Object child0 : xmlEle.elements()) {
				if (child0 instanceof Element) {
					for (Object attr0 : ((Element) child0).attributes()) {
						if (attr0 instanceof Attribute) {
							String val = ((Attribute) attr0).getValue();
							if (val != null && val.length() != 0)
								ret.add(val);
						}
					}
				}
			}
		}

		return ret;
	}

	private IMethod findMethod(String pre) {
		String preClassStr = "L" + pre.substring(0, pre.lastIndexOf('.')).replaceAll("\\.", "/");
		IClass preClass = cha.lookupClass(
				TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(preClassStr)));
		if (preClass == null)
			return null;

		IMethod mtd1 = null;
		for (IMethod tmp : preClass.getAllMethods()) {
			if (tmp.getSignature().equals(pre)) {
				mtd1 = tmp;
				break;
			}
		}
		return mtd1;
	}

	public HashSet<SequenceCallMark> getSeqConfigSet() {
		return seqConfigSet;
	}

}
