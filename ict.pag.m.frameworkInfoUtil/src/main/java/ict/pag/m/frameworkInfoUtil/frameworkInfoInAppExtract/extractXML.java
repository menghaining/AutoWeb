package ict.pag.m.frameworkInfoUtil.frameworkInfoInAppExtract;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.io.SAXReader;

import ict.pag.m.frameworkInfoUtil.customize.ConfigUtil;
import ict.pag.m.frameworkInfoUtil.customize.Util;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoPair;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoUnit;
import ict.pag.m.frameworkInfoUtil.infoEntity.marksLevel;
import ict.pag.m.frameworkInfoUtil.infoEntity.marksType;

public class extractXML {
	/**
	 * 1. get all application class name 2. get attribute value to compare with
	 */

	/**
	 * k means the depth of xml layer
	 */
	private int k = 4;
	private Set<String> bases = new HashSet<String>();

	/** all framework marks meets */
	private HashSet<String> allMarks = new HashSet<>();

	public Set<infoUnit> extract(String fileName) throws DocumentException {
		if (!fileName.endsWith(".xml"))
			return null;

		Set<infoUnit> infoUnits = new HashSet<infoUnit>();

		File file = new File(fileName);

		SAXReader reader = new SAXReader();
		Document document = reader.read(file);

		document.getDocType();
		/** k=1 */
		Element root = document.getRootElement();

		List<infoPair> fields = new ArrayList<infoPair>();
		praseCurrLayer(root, 0, fields, infoUnits);

		return infoUnits;
	}

	/**
	 * depth: current xml depth
	 */
	private void praseCurrLayer(Element root, int depth, List<infoPair> fields, Set<infoUnit> infoUnits) {
//		System.out.println("now root is " + root.getName());
		String pre = root.getNamespacePrefix();
		if (depth > k - 1) {
			depth--;
			return;
		}
		if (ConfigUtil.isStoreLayer(root.getNamespaceURI()) || ConfigUtil.isOtherConfig(root.getNamespaceURI()))
			return;
//		System.out.println("deal with root  " + root.getName());
		Namespace nsp = root.getNamespace();

		HashMap<String, String> infoPairValuesMap = new HashMap<String, String>();
		List<String> connectPoints = ConectPoints(root, infoPairValuesMap);
		String eleText = root.getText();
		if (!connectPoints.isEmpty()) {
			/** 1. add into infoPair value */
			fields.add(new infoPair(pre + ":" + root.getName(), infoPairValuesMap));
			for (String pp : connectPoints) {
				/** 2. add into infoUnits */
				List<infoPair> addedField = new ArrayList<infoPair>();
				addedField.addAll(fields);
//				if (ConfigUtil.isApplicationClass(pp) && !pp.contains("(")) {
				if (Util.hasApplicationClass(pp) && !pp.contains("(")) {
					infoUnits.add(new infoUnit(marksType.xml, marksLevel.Clazz, pp, depth + 1, addedField));
				} else {
					infoUnits.add(new infoUnit(marksType.xml, marksLevel.UNKNOWN, pp, depth + 1, addedField));
				}

				bases.add(pp);
			}
		} else {
			/**
			 * means current layer does not have connecting points and search lower layer
			 */
			fields.add(new infoPair(pre + ":" + root.getName(), null));

			if (!eleText.equals("") && !eleText.contains("\n") && !eleText.contains("\t")) {
				List<infoPair> addedField = new ArrayList<infoPair>();
				addedField.addAll(fields);
				addedField.add(new infoPair("pureText" + ":" + eleText, null));
				infoUnits.add(new infoUnit(marksType.xml, marksLevel.UNKNOWN, eleText, depth + 1, addedField));
			}

		}

		for (Object child0 : root.elements()) {
			List<infoPair> ch_fields = new ArrayList<infoPair>();
			ch_fields.addAll(fields);
			if (child0 instanceof Element) {
//				System.out.println(depth + ":" + ch_fields);
				praseCurrLayer((Element) child0, depth + 1, ch_fields, infoUnits);
			}
		}
	}

	private static List<String> ConectPoints(Element root, HashMap<String, String> infoPairValuesMap) {
		infoPairValuesMap.clear();

		List<String> contectPointsList = new ArrayList<String>();

		boolean flag = false;

		for (Object attr0 : root.attributes()) {
			if (attr0 instanceof Attribute) {
				Attribute attr = (Attribute) attr0;
				infoPairValuesMap.put(attr.getName(), attr.getValue());
				if (hasConectPoint(attr.getValue())) {
					flag = true;
					contectPointsList.add(attr.getValue());
				}
			}
		}

		if (!flag) {
			infoPairValuesMap.clear();
			contectPointsList.clear();
		}

		return contectPointsList;

	}

	/**
	 * TODO:more precise</br>
	 * whether this root's attribute-value has the connecting points with source
	 * code
	 */
	private static boolean hasConectPoint(String tagValue) {
		/** exclude some files */
		if (tagValue.toLowerCase().contains("http://") || tagValue.toLowerCase().contains("https://")) {
			return false;
		}
		if (tagValue.toLowerCase().startsWith("org.springframework")) {
			return false;
		}
		if (tagValue.toLowerCase().endsWith(".jsp") || tagValue.contains("WEB-INF")) {
			return false;
		}
		return true;

	}

	public Set<String> getBases() {
		return bases;
	}

	public static void main(String args[]) {
		try {
//			extract("F:\\struts2.xml");
			extractXML ex = new extractXML();
			ex.extract(
					"F:\\Framework\\Tesecases\\openmrs\\openmrs-core\\web\\src\\main\\resources\\openmrs-servlet.xml");

		} catch (DocumentException e) {
			System.err.println("[DocumentException]");
		}
	}

}
