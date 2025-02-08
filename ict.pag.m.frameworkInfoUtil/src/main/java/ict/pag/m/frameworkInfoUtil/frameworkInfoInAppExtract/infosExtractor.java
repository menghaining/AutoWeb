package ict.pag.m.frameworkInfoUtil.frameworkInfoInAppExtract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dom4j.DocumentException;

import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;

import ict.pag.m.frameworkInfoUtil.customize.ConfigUtil;
import ict.pag.m.frameworkInfoUtil.customize.Util;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoUnit;
import ict.pag.m.frameworkInfoUtil.infoEntity.marksLevel;

public class infosExtractor {
	private List<String> xml_paths = null;

	private ClassHierarchy cha;
	private CHACallGraph chaCG;
	private Set<String> allCodeClazz;

	private Set<infoUnit> inheritanceMarksSet = new HashSet<>();
	private HashMap<String, HashSet<String>> class2AllAncestors_app = new HashMap<>();
	private HashMap<String, HashSet<String>> class2AllAncestors_frk = new HashMap<>();

	private Set<infoUnit> AnnoMarksSet = new HashSet<>();
	private Set<infoUnit> annos_clazz = new HashSet<>();
	private Set<infoUnit> annos_field = new HashSet<>();
	private Set<infoUnit> annos_mthd = new HashSet<>();
	private Set<String> annos_AOPAspectJ = new HashSet<>();

	private Set<infoUnit> xmlMarksSet = new HashSet<>();
	private Set<infoUnit> xmlMarksSet_clearClass = new HashSet<>();

	private Set<infoUnit> webxml = new HashSet<>();

	private Set<String> xmlConfigClass = new HashSet<>();
	private Set<String> xmlConfigField = new HashSet<>();

	public infosExtractor(ClassHierarchy cha2, List<String> xml_paths) {
		this.cha = cha2;
		this.xml_paths = xml_paths;
		init();
	}

	public infosExtractor(ClassHierarchy cha2, String xml_path) {
		this.cha = cha2;
		this.xml_paths = new ArrayList<String>();
		xml_paths.add(xml_path);
		init();
	}

	public infosExtractor(ClassHierarchy cha2, CHACallGraph chaCG2, List<String> xMLs) {
		this.cha = cha2;
		this.chaCG = chaCG2;
		this.xml_paths = xMLs;
		init();
	}

	private void init() {
		allCodeClazz = Util.getAllCodeClazz(cha);
	}

	/**
	 * just extract marks directly on the entity, do not care about
	 * extends/implements/override
	 */
	public void extract() {
		long beforeTime = System.nanoTime();
		/**
		 * 1. extract inheritance
		 */
		extractInheritance exInher = new extractInheritance();
		inheritanceMarksSet = exInher.extract(cha);
		class2AllAncestors_app = exInher.getClass2AllAncestors();
		if (ConfigUtil.printLogs) {
			System.out.println("====inheriatance marks====");
			inheritanceMarksSet.forEach(info -> {
				System.out.println(info);
			});
			System.out.println("====inheriatance marks====");
			System.out.println("[info][COUNT] inheritance marks kinds total " + exInher.getAllMarks().size());
			for (String m : exInher.getAllMarks()) {
				System.out.println("\t[info][mark]" + m);
			}
		}

		/**
		 * 2. extract annotations
		 */
		// extract annos
		extractAnnos exAnnos = new extractAnnos();
		AnnoMarksSet = exAnnos.extract(cha);
		annos_clazz = exAnnos.getInfoUnits_clazz();
		annos_field = exAnnos.getInfoUnits_field();
		annos_mthd = exAnnos.getInfoUnits_mthd();
		annos_AOPAspectJ = exAnnos.getAOPaspectJ_mark();

		if (ConfigUtil.printLogs) {
			System.out.println("====annotation marks====");
			annos_clazz.forEach(info -> {
				System.out.println(info);
			});
			annos_field.forEach(info -> {
				System.out.println(info);
			});
			annos_mthd.forEach(info -> {
				System.out.println(info);
			});
			System.out.println("====annotation marks====");
			System.out.println("[info][COUNT] annotation marks kinds total " + exAnnos.getAllMarks().size());
			for (String m : exAnnos.getAllMarks()) {
				System.out.println("\t[info][mark]" + m);
			}
		}

		/**
		 * 3. extract xmls
		 */
		if (xml_paths != null && xml_paths.size() != 0) {
			for (String p : xml_paths) {
//				System.out.println("[need to parse]" + p);
				extractXML exXML = new extractXML();

				try {
					Set<infoUnit> tmp = exXML.extract(p);
					if (p.toLowerCase().endsWith("web.xml")) {
						webxml.addAll(tmp);
					}
//					else {
					xmlMarksSet.addAll(tmp);
					for (infoUnit t : tmp) {
//						if (ConfigUtil.isApplicationClass(t.getBase())) {
						if (Util.hasApplicationClass(t.getBase())) {
							xmlConfigClass.add(t.getBase());
							if (t.getLevel().equals(marksLevel.Clazz))
								xmlMarksSet_clearClass.add(t);
						}
					}
//					}
				} catch (DocumentException e) {
					e.printStackTrace();
				}
			}

		}
		if (ConfigUtil.printLogs) {
			System.out.println("====xml all marks====");
			xmlMarksSet.forEach(info -> {
				System.out.println(info);
			});
			System.out.println("====xml all marks====");
			System.out.println("====web.xml====");
			webxml.forEach(info -> {
				System.out.println(info);
			});
			System.out.println("====web.xml====");
		}

		double buildTime = (System.nanoTime() - beforeTime) / 1E9;
		System.out.println("[TIME-LOG] All Frameworks Marks Parse Done in " + buildTime + " s!");
	}

	public Set<infoUnit> getInheritanceMarksSet() {
		return inheritanceMarksSet;
	}

	public Set<infoUnit> getAnnoMarksSet() {
		return AnnoMarksSet;
	}

	public Set<infoUnit> getAnnos_clazz() {
		return annos_clazz;
	}

	public Set<infoUnit> getAnnos_field() {
		return annos_field;
	}

	public Set<infoUnit> getAnnos_mthd() {
		return annos_mthd;
	}

	public Set<String> getAnnos_AOPAspectJ() {
		return annos_AOPAspectJ;
	}

	public Set<infoUnit> getXmlMarksSet() {
		return xmlMarksSet;
	}

	public Set<infoUnit> getXmlMarksSet_clear() {
		return xmlMarksSet_clearClass;
	}

	public Set<String> getXmlConfigClass() {
		return xmlConfigClass;
	}

	public HashMap<String, HashSet<String>> getClass2AllApplicationAncestors() {
		return class2AllAncestors_app;
	}

//	public static void main(String args[]) {
//		String path = args[0];
//		infosExtractor extractor = new infosExtractor(path);
//
//		extractor.extract();
//	}

}
