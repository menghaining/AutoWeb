package ict.pag.m.generateFrameworkModel4App.appInfoParsers;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.xmlClassNodeEntity;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.xmlClassNodeEntity.SubInfo;

public class XMLExtractor {

	/** Answers */
	HashMap<String, HashSet<xmlClassNodeEntity>> mark2classSet = new HashMap<>();

	public HashMap<String, HashSet<xmlClassNodeEntity>> getMark2classSet() {
		return mark2classSet;
	}

	/** heuristic to extract informations in a class */
	public void extract(List<String> xMLs) {
		if (xMLs != null && xMLs.size() != 0) {
			for (String fileName : xMLs) {
				if (!fileName.endsWith(".xml"))
					continue;
				try {
					File file = new File(fileName);
					if (fileName.contains("applicationContext"))
						System.out.println();

					SAXReader reader = new SAXReader();
					Document document = reader.read(file);

					Element root = document.getRootElement();

					HashSet<Element> classNodes = new HashSet<>();
					findClassNode(root, classNodes);

					for (Element node : classNodes) {
						calculateClassNodeInformation(node);
					}

				} catch (DocumentException ex) {
					System.err.println("[xml fail]" + fileName);
				}
			}
		}

	}

	private void calculateClassNodeInformation(Element node) {
		if (node.getName().equals("class")) {
			// tag name is class
			HashMap<String, String> name2value = new HashMap<>();
			String clazz = "unknown";

			for (Object attr0 : node.attributes()) {
				if (attr0 instanceof Attribute) {
					Attribute attr = (Attribute) attr0;
					String attrName = attr.getName();
					String val = attr.getValue();
					name2value.put(attrName, val);
				}
			}

			xmlClassNodeEntity entity = new xmlClassNodeEntity(clazz, name2value);
			String path = calculateXMLNodePath(node);
			// add into results
			String base = path.substring(1) + ";" + "class";
			if (mark2classSet.containsKey(base)) {
				mark2classSet.get(base).add(entity);
			} else {
				HashSet<xmlClassNodeEntity> tmp = new HashSet<>();
				tmp.add(entity);
				mark2classSet.put(base, tmp);
			}

		} else {
			// attribute is class
			// 1. same layer attributes
			HashMap<String, String> name2value = new HashMap<>();
			String clazz = null;
			for (Object attr0 : node.attributes()) {
				if (attr0 instanceof Attribute) {
					Attribute attr = (Attribute) attr0;
					String attrName = attr.getName();
					String val = attr.getValue();
					if (attrName.equals("class")) {
						clazz = val;
					} else {
						name2value.put(attrName, val);
					}
				}
			}
			String path = calculateXMLNodePath(node);
			if (clazz != null) {
				xmlClassNodeEntity entity = new xmlClassNodeEntity(clazz, name2value);

				// 2. lower layer if any
				for (Object child0 : node.elements()) {
					if (child0 instanceof Element) {
						Element child = (Element) child0;
						String subName = child.getName();
						HashMap<String, String> name2value_sub = new HashMap<>();
						for (Object at0 : child.attributes()) {
							if (at0 instanceof Attribute) {
								Attribute at = (Attribute) at0;
								name2value_sub.put(at.getName(), at.getValue());
							}
						}
						SubInfo sub1 = entity.new SubInfo(subName, name2value_sub);
						entity.addSingleSubInformation(sub1);
					}
				}

				// add into results
				String base = path.substring(1) + ";" + "class";
				if (mark2classSet.containsKey(base)) {
					mark2classSet.get(base).add(entity);
				} else {
					HashSet<xmlClassNodeEntity> tmp = new HashSet<>();
					tmp.add(entity);
					mark2classSet.put(base, tmp);
				}
			}

		}

	}

	private String calculateXMLNodePath(Element node) {
		String ret = "";
		Element par = node.getParent();
		while (par != null) {
			String name = par.getName();
			ret = ret + ";" + name;
			par = par.getParent();

		}
		ret = ret + ";" + node.getName();
		return ret;
	}

	private void findClassNode(Element root, HashSet<Element> classNodes) {
		String name = root.getName();

		if (name.equals("class")) {
			classNodes.add(root);
		} else {
			boolean find = false;
			for (Object attr0 : root.attributes()) {
				if (attr0 instanceof Attribute) {
					Attribute attr = (Attribute) attr0;
					String attrName = attr.getName();
					if (attrName.equals("class")) {
						find = true;
						classNodes.add(root);
					}
				}
			}
			if (!find) {
				for (Object child0 : root.elements()) {
					if (child0 instanceof Element) {
						findClassNode((Element) child0, classNodes);
					}
				}

			}
		}

	}

}
