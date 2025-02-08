package ict.pag.webframework.marks;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentType;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class XMLMarksExtractor {
	private HashSet<String> applicationClasses;

	/* Answers */
	private HashMap<String, HashSet<Element>> class2XMLElement = new HashMap<>();

	public XMLMarksExtractor(List<String> allConcernedXMLs, HashSet<String> applicationClasses2) {
		this.applicationClasses = applicationClasses2;

		try {
			extract(allConcernedXMLs);
		} catch (DocumentException e) {
//			e.printStackTrace();
			System.err.println("[error][DocumentException]" + e.getMessage());
		}
	}

	private void extract(List<String> allConcernedXMLs) throws DocumentException {
		for (String p : allConcernedXMLs) {
			if (!p.endsWith(".xml"))
				continue;

			File file = new File(p);

			SAXReader reader = new SAXReader();
			Document document = reader.read(file);

			/* exclude database configure */
			DocumentType docType = document.getDocType();
			if (docType != null)
				if ((docType.getPublicID() != null && docType.getPublicID().toLowerCase().contains("mybatis"))
						|| (docType.getSystemID() != null && docType.getSystemID().toLowerCase().contains("mybatis"))
						|| (docType.getPublicID() != null && docType.getPublicID().toLowerCase().contains("hibernate"))
						|| (docType.getSystemID() != null
								&& docType.getSystemID().toLowerCase().contains("hibernate"))) {
					continue;
				}
			Element root = document.getRootElement();

			/* find all root that the */
			praseCurrLayer(root);

		}
	}

	public HashMap<String, HashSet<Element>> getClass2XMLElement() {
		return class2XMLElement;
	}

	private void praseCurrLayer(Element root) {
		// text
		String text = root.getText();
		String tmp = findClass(text);
		if (tmp != null && !tmp.equals("")) {
			add2Res(tmp, root);
		}
		// attribute
		for (Object attr0 : root.attributes()) {
			if (attr0 instanceof Attribute) {
				Attribute attr = (Attribute) attr0;
//				String name = attr.getName();
				String val = attr.getValue();
				String ret = findClass(val);
				if (ret != null && !ret.equals("")) {
					add2Res(ret, root);
				}
			}
		}
		for (Object child0 : root.elements()) {
			if (child0 instanceof Element) {
				praseCurrLayer((Element) child0);
			}
		}
	}

	private void add2Res(String val, Element root) {
		if (class2XMLElement.containsKey(val)) {
			class2XMLElement.get(val).add(root);
		} else {
			HashSet<Element> tmp = new HashSet<Element>();
			tmp.add(root);
			class2XMLElement.put(val, tmp);
		}

	}

	/** @return the application class if the node contains, else null */
	private String findClass(String val) {
		if (val.equals(""))
			return null;
		if (val.contains("/")) {
			val = val.replaceAll("/", ".");
		}
		for (String app : applicationClasses) {
			if (val.startsWith(app))
				return app;
			if (app.startsWith(val))
				return val;
		}
		return null;
	}

}
