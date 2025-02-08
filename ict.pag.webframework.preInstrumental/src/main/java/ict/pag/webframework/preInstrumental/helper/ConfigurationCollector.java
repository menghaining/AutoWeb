package ict.pag.webframework.preInstrumental.helper;

import java.util.HashMap;
import java.util.HashSet;

import org.dom4j.Attribute;
import org.dom4j.Element;

public class ConfigurationCollector {
	/**
	 * collect the node that contains field, and record <node, field-attr>in
	 * node2attrs_fields
	 */
	public static void collectFieldXMLConfiguration(Element root, String fieldSig, String xmlFile, boolean findPreClass,
			HashMap<Element, HashSet<Attribute>> node2attrs_fields) {
		// base info
		String fieldFullName = fieldSig.substring(0, fieldSig.indexOf(':'));
		String fieldClass = fieldFullName.substring(0, fieldFullName.lastIndexOf('.'));
		String fieldName = fieldFullName.substring(fieldFullName.lastIndexOf('.') + 1);

		// current level
		boolean findClass = false;
		boolean findField = false;
		Attribute fieldAttr = null;
		HashSet<Attribute> candidateFieldAttrs = new HashSet<>();

		// text
		boolean hasText = false;
		String text = root.getText();
		if (text.equals(fieldClass)) {
			// is class
			findClass = true;
			hasText = true;
			System.out.println("[xml]" + fieldSig + "[class][text]" + root.getPath());
		} else if (text.contains(fieldClass) && text.contains(fieldName)) {
			// is method
			findField = true;
			hasText = true;
			System.out.println("[xml]" + fieldSig + "[field][text]" + root.getPath());
		}
		// attribute
		for (Object attr0 : root.attributes()) {
			if (attr0 instanceof Attribute) {
				Attribute attr = (Attribute) attr0;
				String val = attr.getValue();

				if (val.equals(fieldClass)) {
					// only class
					System.out.println("[xml]" + fieldSig + "[class][attribute]" + attr.getPath());
					findClass = true;
				} else if (val.contains(fieldClass) && val.contains(fieldName)) {
					// both class and method
					findField = true;
					System.out.println("[xml]" + fieldSig + "[field][attribute]" + attr.getPath());
				} else if (val.equals(fieldName)) {
					// only field
					candidateFieldAttrs.add(attr);
				}
			}
		}
		if (!findField) {
			if (!candidateFieldAttrs.isEmpty() && (findClass || findPreClass)) {
				findField = true;

				for (Attribute ele : candidateFieldAttrs) {
					// TODO: does this heuristic work?
					if (ele.getName().toLowerCase().equals("field")) {
						fieldAttr = ele;
						break;
					}
				}
				if (fieldAttr != null)
					System.out.println("[xml]" + fieldSig + "[field][attribute]" + fieldAttr.getPath());
				else
					for (Attribute ele : candidateFieldAttrs)
						System.out.println("[xml]" + fieldSig + "[field][attribute]" + ele.getPath());
			}
		}
		// find and add
		if (findField) {
			if (!node2attrs_fields.containsKey(root))
				node2attrs_fields.put(root, new HashSet<>());

			if (fieldAttr != null)
				node2attrs_fields.get(root).add(fieldAttr);
			else
				node2attrs_fields.get(root).addAll(candidateFieldAttrs);

			if (hasText) {
				Attribute tmp = null;
				node2attrs_fields.get(root).add(tmp);
			}
		}
		// child element
		for (Object child0 : root.elements()) {
			if (child0 instanceof Element) {
				collectFieldXMLConfiguration((Element) child0, fieldSig, xmlFile, (findPreClass || findClass), node2attrs_fields);
			}
		}
	}

	public static void collectMethodXMLConfiguration(Element root, String stmt, String xmlFile, boolean findPreClass,
			HashMap<Element, HashSet<Attribute>> node2attrs_class, HashMap<Element, HashSet<Attribute>> node2attrs_method) {
		// base info
		String className = stmt.substring(0, stmt.lastIndexOf('.'));
		String mtdName = stmt.substring(className.length() + 1, stmt.indexOf('('));

		// current level
		boolean findClass = false;
		boolean findMethod = false;
		Attribute mtdAttr = null;
		HashSet<Attribute> candidateMtdAttrs = new HashSet<>();
		HashSet<Attribute> candidateclassAttrs = new HashSet<>();
		boolean hasText = false;
		// text
		String text0 = root.getText().trim();
		String text = text0.replaceAll("[\t\n\r]", "");
		if (text.equals(className)) {
			// is class
			findClass = true;
			hasText = true;
			System.out.println("[xml]" + stmt + "[class][text]" + root.getPath());
		} else if (text.contains(className) && text.contains(mtdName)) {
			// is method
			findMethod = true;
			hasText = true;
			System.out.println("[xml]" + stmt + "[method][text]" + root.getPath());
		}
		// attribute
		for (Object attr0 : root.attributes()) {
			if (attr0 instanceof Attribute) {
				Attribute attr = (Attribute) attr0;
//				String name = attr.getName();
				String val = attr.getValue();

				if (val.equals(className)) {
					// only class
					System.out.println("[xml]" + stmt + "[class][attribute]" + attr.getPath());
					findClass = true;
					candidateclassAttrs.add(attr);
				} else if (val.contains(className) && val.contains(mtdName)) {
					// both class and method
					findMethod = true;
					System.out.println("[xml]" + stmt + "[method][attribute]" + attr.getPath());
				} else if (val.equals(mtdName)) {
					// only method
					candidateMtdAttrs.add(attr);
				}
			}
		}
		if (!findMethod) {
			if (!candidateMtdAttrs.isEmpty() && (findPreClass || findClass)) {
				findMethod = true;
				for (Attribute ele : candidateMtdAttrs) {
					// TODO: does this heuristic work? latter will deal with each attribute
					if (ele.getName().toLowerCase().equals("method") || ele.getName().toLowerCase().equals("mtd")) {
						mtdAttr = ele;
						break;
					}
				}
				if (mtdAttr != null)
					System.out.println("[xml]" + stmt + "[method][attribute]" + mtdAttr.getPath());
				else
					for (Attribute ele : candidateMtdAttrs)
						System.out.println("[xml]" + stmt + "[method][attribute]" + ele.getPath());
			}
		}

		// find and add;
		// when method and class find on same element level ,add to method set
		if (findMethod) {
			if (!node2attrs_method.containsKey(root))
				node2attrs_method.put(root, new HashSet<>());

			if (mtdAttr != null)
				node2attrs_method.get(root).add(mtdAttr);
//			else
//				node2attrs_method.get(root).addAll(candidateMtdAttrs);

			if (hasText) {
				Attribute tmp = null;
				node2attrs_method.get(root).add(tmp);
			}
		}
		if (findClass) {
			if (!node2attrs_class.containsKey(root))
				node2attrs_class.put(root, new HashSet<>());

			node2attrs_class.get(root).addAll(candidateclassAttrs);

			if (hasText) {
				Attribute tmp = null;
				node2attrs_class.get(root).add(tmp);
			}
		}
		// child element
		for (Object child0 : root.elements()) {
			if (child0 instanceof Element) {
				collectMethodXMLConfiguration((Element) child0, stmt, xmlFile, (findPreClass || findClass), node2attrs_class, node2attrs_method);
			}
		}
	}

	public static void collectClassXMLConfiguration(Element root, String fullClassName, HashMap<Element, HashSet<Attribute>> node2attrs_class) {

		boolean hasText = false;
		// text
		String text = root.getText();
		if (text.equals(fullClassName)) {
			// is class
			hasText = true;
		}
		if (hasText) {
			if (!node2attrs_class.containsKey(root))
				node2attrs_class.put(root, new HashSet<>());
			Attribute tmp = null;
			node2attrs_class.get(root).add(tmp);
		}
		// attribute
		for (Object attr0 : root.attributes()) {
			if (attr0 instanceof Attribute) {
				Attribute attr = (Attribute) attr0;
//				String name = attr.getName();
				String val = attr.getValue();

				if (val.equals(fullClassName)) {
					// only class
					if (!node2attrs_class.containsKey(root))
						node2attrs_class.put(root, new HashSet<>());
					node2attrs_class.get(root).add(attr);
				}
			}
		}
		// child element
		for (Object child0 : root.elements()) {
			if (child0 instanceof Element) {
				collectClassXMLConfiguration((Element) child0, fullClassName, node2attrs_class);
			}
		}
	}

	public static void collectMethodXMLConfigurationMarks(Element root, String stmt, String xmlFile, boolean findPreClass, HashSet<String> classMarks,
			HashSet<String> methodMarks) {
		// base info
		String className = stmt.substring(0, stmt.lastIndexOf('.'));
		String mtdName = stmt.substring(className.length() + 1, stmt.indexOf('('));

		// current level
		boolean findClass = false;
		boolean findMethod = false;
		Attribute mtdAttr = null;
		HashSet<Attribute> candidateMtdAttrs = new HashSet<>();
		HashSet<Attribute> candidateclassAttrs = new HashSet<>();
		boolean hasText = false;
		// text
		String text0 = root.getText();
		String text = text0.replaceAll("[\t\n\r]", "");
		if (text.equals(className)) {
			// is class
			findClass = true;
			hasText = true;
			// is method
			findMethod = true;
			hasText = true;
		}
		if (hasText)
			classMarks.add(root.getPath());
		
		// attribute
		for (Object attr0 : root.attributes()) {
			if (attr0 instanceof Attribute) {
				Attribute attr = (Attribute) attr0;
//				String name = attr.getName();
				String val = attr.getValue();

				if (val.equals(className)) {
					// only class
					findClass = true;
					candidateclassAttrs.add(attr);
				} else if (val.contains(className) && val.contains(mtdName)) {
					// both class and method
					findMethod = true;
				} else if (val.equals(mtdName)) {
					// only method
					candidateMtdAttrs.add(attr);
				}
			}
		}
		if (!findMethod) {
			if (!candidateMtdAttrs.isEmpty() && (findPreClass || findClass)) {
				findMethod = true;
				for (Attribute ele : candidateMtdAttrs) {
					// does this heuristic work? latter will deal with each attribute
					if (ele.getName().toLowerCase().equals("method") || ele.getName().toLowerCase().equals("mtd")) {
						mtdAttr = ele;
						break;
					}
				}
			}
		}

		// find and add;
		// when method and class find on same element level ,add to method set
		if (findMethod) {

			if (mtdAttr != null)
				methodMarks.add(mtdAttr.getPath());
			else {
				for (Attribute attr : candidateMtdAttrs) {
					methodMarks.add(attr.getPath());
				}
			}
		}
		if (findClass) {
			for (Attribute attr : candidateclassAttrs) {
				classMarks.add(attr.getPath());
			}
		}
		// child element
		for (Object child0 : root.elements()) {
			if (child0 instanceof Element) {
				collectMethodXMLConfigurationMarks((Element) child0, stmt, xmlFile, (findPreClass || findClass), classMarks, methodMarks);
			}
		}
	}

	public static void collectFieldXMLConfigurationMarks(Element root, String fieldSig, String xmlFile, boolean findPreClass, HashSet<String> fieldMarks) {
		// base info
		String fieldFullName = fieldSig.substring(0, fieldSig.indexOf(':'));
		String fieldClass = fieldFullName.substring(0, fieldFullName.lastIndexOf('.'));
		String fieldName = fieldFullName.substring(fieldFullName.lastIndexOf('.') + 1);

		// current level
		boolean findClass = false;
		boolean findField = false;
		Attribute fieldAttr = null;
		HashSet<Attribute> candidateFieldAttrs = new HashSet<>();

		// text
		boolean hasText = false;
		String text = root.getText();
		if (text.equals(fieldClass)) {
			// is class
			findClass = true;
			hasText = true;
		} else if (text.contains(fieldClass) && text.contains(fieldName)) {
			// is method
			findField = true;
			hasText = true;
		}
		// attribute
		for (Object attr0 : root.attributes()) {
			if (attr0 instanceof Attribute) {
				Attribute attr = (Attribute) attr0;
				String val = attr.getValue();

				if (val.equals(fieldClass)) {
					// only class
					findClass = true;
				} else if (val.contains(fieldClass) && val.contains(fieldName)) {
					// both class and method
					findField = true;
				} else if (val.equals(fieldName)) {
					// only field
					candidateFieldAttrs.add(attr);
				}
			}
		}
		if (!findField) {
			if (!candidateFieldAttrs.isEmpty() && (findClass || findPreClass)) {
				findField = true;

				for (Attribute ele : candidateFieldAttrs) {
					// TODO: does this heuristic work?
					if (ele.getName().toLowerCase().equals("field")) {
						fieldAttr = ele;
						break;
					}
				}
			}
		}
		// find and add
		if (findField) {
			if (fieldAttr != null)
				fieldMarks.add(fieldAttr.getPath());
			else {
				for (Attribute attr : candidateFieldAttrs) {
					fieldMarks.add(attr.getPath());
				}
			}

		}
		// child element
		for (Object child0 : root.elements()) {
			if (child0 instanceof Element) {
				collectFieldXMLConfigurationMarks((Element) child0, fieldSig, xmlFile, (findPreClass || findClass), fieldMarks);
			}
		}
	}
}
