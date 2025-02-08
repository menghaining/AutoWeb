package ict.pag.webframework.XML;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentType;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ict.pag.webframework.XML.Util.XMLConfigurationUtil;

public class XMLMarksExtractor {
	private HashSet<String> applicationClasses;

	/* Answers */
	/**
	 * <class String, XMLElemet> Map
	 */
	private HashMap<String, HashSet<Element>> class2XMLElement = new HashMap<>();
	/** class: in which xml file */
	private HashMap<String, HashSet<String>> class2XMLFile = new HashMap<>();

	public XMLMarksExtractor(List<String> allConcernedXMLs, HashSet<String> applicationClasses2) {
		this.applicationClasses = applicationClasses2;

		extract(allConcernedXMLs);
	}

	private void extract(List<String> allConcernedXMLs) {
		for (String p : allConcernedXMLs) {
			if (!p.endsWith(".xml"))
				continue;

			File file = new File(p);
			readFile(file);

		}
	}

	private void readFile(File file) {
		SAXReader reader = new SAXReader();
		reader.setValidation(false);
		reader.setEntityResolver(new EntityResolver() {
			@Override
			public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
				return new InputSource(new ByteArrayInputStream("".getBytes()));
			}
		});
		try {
			Document document = reader.read(file);
			/* exclude database configure */
			DocumentType docType = document.getDocType();
			if (docType != null)
				if (XMLConfigurationUtil.notConcernedXMLID(docType.getPublicID())
						|| XMLConfigurationUtil.notConcernedXMLID(docType.getSystemID()))
					return;

			Element root = document.getRootElement();

			/* find all root that the */
			praseCurrLayer(root, file.getPath());
		} catch (DocumentException e) {
			System.err.println("[error][DocumentException]" + e.getMessage() + " when parse " + file);
		}

	}

	public HashMap<String, HashSet<Element>> getClass2XMLElement() {
		return class2XMLElement;
	}

	public HashMap<String, HashSet<String>> getClass2XMLFile() {
		return class2XMLFile;
	}

	private void praseCurrLayer(Element root, String path) {
		// text
		String text = root.getText();
		String tmp = findClass(text);
		if (tmp != null && !tmp.equals("")) {
			add2Res(tmp, root, path);
		}
		// attribute
		for (Object attr0 : root.attributes()) {
			if (attr0 instanceof Attribute) {
				Attribute attr = (Attribute) attr0;
				String name = attr.getName();
				String val = attr.getValue();
//				String ret = findClass(val);
				/* 3.5: collect all element about class */
				HashSet<String> rets = findClass(name, val);

				for (String res : rets) {
					add2Res(res, root, path);
				}
//				if (ret != null && !ret.equals("")) {
//					add2Res(ret, root);
//				}
			}
		}
		for (Object child0 : root.elements()) {
			if (child0 instanceof Element) {
				praseCurrLayer((Element) child0, path);
			}
		}
	}

	private void add2Res(String val, Element root, String path) {
		if (class2XMLElement.containsKey(val)) {
			class2XMLElement.get(val).add(root);
		} else {
			HashSet<Element> tmp = new HashSet<Element>();
			tmp.add(root);
			class2XMLElement.put(val, tmp);
		}

		if (class2XMLFile.containsKey(val)) {
			class2XMLFile.get(val).add(path);
		} else {
			HashSet<String> tmp = new HashSet<>();
			tmp.add(path);
			class2XMLFile.put(val, tmp);
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

	/** @return the application class if the node contains, else null */
	private HashSet<String> findClass(String name, String val) {
		HashSet<String> ret = new HashSet<>();
		if (val.equals(""))
			return ret;
		if (val.contains("/")) {
			val = val.replaceAll("/", ".");
		}
		for (String app : applicationClasses) {
			if (val.equals(app) || app.toLowerCase().endsWith(val.toLowerCase()))
				ret.add(app);
		}
		return ret;
	}

}
