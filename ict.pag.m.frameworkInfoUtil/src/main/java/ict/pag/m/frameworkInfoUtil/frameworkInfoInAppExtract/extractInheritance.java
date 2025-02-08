package ict.pag.m.frameworkInfoUtil.frameworkInfoInAppExtract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.BytecodeClass;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;

import ict.pag.m.frameworkInfoUtil.customize.ConfigUtil;
import ict.pag.m.frameworkInfoUtil.customize.GraphBuilder;
import ict.pag.m.frameworkInfoUtil.customize.Util;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoPair;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoUnit;
import ict.pag.m.frameworkInfoUtil.infoEntity.marksLevel;
import ict.pag.m.frameworkInfoUtil.infoEntity.marksType;

public class extractInheritance {
	private Set<String> baseSets = new HashSet<String>();

	/** inheritance marks including all ancestors */
	private Set<infoUnit> infoUnits = new HashSet<infoUnit>();

	/* all information including application */
	private HashMap<String, HashSet<String>> allInfoOnOneLayer = new HashMap<>();

	private HashMap<String, HashSet<String>> class2AllAncestors = new HashMap<>();

	/** all framework marks meets */
	private HashSet<String> allMarks = new HashSet<>();

	public HashSet<String> getAllMarks() {
		return allMarks;
	}

	public Set<infoUnit> extract(ClassHierarchy cha) {

		cha.forEach(nodeClazz -> {
			if (nodeClazz.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {

				String base = Util.format(nodeClazz.getName().toString());

//				System.out.println(nodeClazz);

				List<infoPair> clazzUnitFs = new ArrayList<>();

				// superClass
				if (nodeClazz.getSuperclass() != null) {
					if (nodeClazz instanceof ShrikeClass) {
						/** "Lcom/example/service/StudentServiceAbstract" */
						String superClazz = ((ShrikeClass) nodeClazz).getSuperName().toString();

						if (superClazz != null
								&& ((nodeClazz.getClassLoader().getReference().equals(ClassLoaderReference.Application)
										&& ConfigUtil.enableApplication) || ConfigUtil.isApplicationClass(superClazz)))
							addIntoAllInfos(base, "super:" + Util.format(superClazz));

						if (ConfigUtil.isFrameworkMarks(superClazz)) {
							Map<String, String> clazz_key_value = new HashMap<String, String>();
							clazz_key_value.put("value", Util.format(superClazz));
							infoPair p1 = new infoPair("extends", clazz_key_value);
							clazzUnitFs.add(p1);
							allMarks.add(Util.format(superClazz));
						}

					}

				}
				// Interfaces
				if (nodeClazz instanceof BytecodeClass) {
					Collection<String> allInterf = ((BytecodeClass) nodeClazz).getAllInterfacesSigs();
					if (nodeClazz.getDirectInterfaces() != null) {
						/** "Lcom/example/controller/LoginControllerInterface" */
						if (allInterf != null && !allInterf.isEmpty()) {
							for (String i : allInterf) {

								if (i != null && ((nodeClazz.getClassLoader().getReference()
										.equals(ClassLoaderReference.Application) && ConfigUtil.enableApplication)
										|| ConfigUtil.isApplicationClass(i)))
									addIntoAllInfos(base, "interface:" + Util.format(i));

								Map<String, String> interface_key_value = new HashMap<String, String>();
								if (ConfigUtil.isFrameworkMarks(i)) {
									interface_key_value.put("value", Util.format(i));
									infoPair p2 = new infoPair("implements", interface_key_value);
									clazzUnitFs.add(p2);
									allMarks.add(Util.format(i));
								}

							}

						}

					}
				}

				if (!clazzUnitFs.isEmpty()) {
					infoUnits.add(new infoUnit(marksType.Inheritance, marksLevel.Clazz, base, clazzUnitFs));
					baseSets.add(base);
				}
			}
		});
		calAllInheritanceMarks();
		return infoUnits;
	}

	/**
	 * Return inheritance marks including all ancestors
	 */
	public void calAllInheritanceMarks() {

		for (String child : allInfoOnOneLayer.keySet()) {

			HashSet<String> alls = allInfoOnOneLayer.get(child);
			HashSet<String> fathers = new HashSet<>();
			for (String fa : alls) {
				if (fa.startsWith("super:")) {
					String tmp = fa.substring(6);
					fathers.add(tmp);
					if (allInfoOnOneLayer.keySet().contains(tmp)) {
						findAllFathers(allInfoOnOneLayer.get(tmp), fathers);
					}
				} else if (fa.startsWith("interface:")) {
					String tmp = fa.substring(10);
					addIntoClass2AllAncestors(child, tmp);
				}
			}

			if (fathers.isEmpty())
				continue;

			for (String fa : fathers) {
				addIntoClass2AllAncestors(child, fa);
			}

			List<infoPair> fathersMarks = new ArrayList<>();
			infoUnit childInfo = null;
			// add all marks
			for (infoUnit info : infoUnits) {
				String base = info.getBase();
				if (fathers.contains(base)) {
					List<infoPair> fs = info.getFields();
					fathersMarks.addAll(fs);
				} else if (child.equals(base)) {
					childInfo = info;
				}
			}
			if (fathersMarks.isEmpty())
				continue;
			if (childInfo == null) {
				infoUnits.add(new infoUnit(marksType.Inheritance, marksLevel.Clazz, child, fathersMarks));
			} else {
				childInfo.getFields().addAll(fathersMarks);
			}

		}

	}

	private void addIntoClass2AllAncestors(String child, String tmp) {
		if (class2AllAncestors.containsKey(child))
			class2AllAncestors.get(child).add(tmp);
		else {
			HashSet<String> tmpSet = new HashSet<>();
			tmpSet.add(tmp);
			class2AllAncestors.put(child, tmpSet);
		}

	}

	private void findAllFathers(HashSet<String> alls, HashSet<String> fathers) {
		for (String fa : alls) {
			if (fa.startsWith("super:")) {
				String tmp = fa.substring(6);
				fathers.add(tmp);
				if (allInfoOnOneLayer.keySet().contains(tmp)) {
					findAllFathers(allInfoOnOneLayer.get(tmp), fathers);
				} else {
					return;
				}
			} else if (fa.startsWith("interface:")) {
				String tmp = fa.substring(10);
				fathers.add(tmp);
			}
		}
	}

	private void addIntoAllInfos(String base, String superClazz) {
		if (allInfoOnOneLayer.containsKey(base)) {
			allInfoOnOneLayer.get(base).add(superClazz);
		} else {
			HashSet<String> tmp = new HashSet<>();
			tmp.add(superClazz);
			allInfoOnOneLayer.put(base, tmp);
		}

	}

	public Set<String> getBaseSets() {
		return baseSets;
	}

	public HashMap<String, HashSet<String>> getClass2AllAncestors() {
		return class2AllAncestors;
	}

	public static void main(String args[]) {
		String path = args[0];
		GraphBuilder builder = new GraphBuilder(path);

		extractInheritance ex = new extractInheritance();
//		ex.getAllCodeClazz(builder.getCHA());
//		System.out.println(clazzSigs);
		ex.extract(builder.getCHA());
	}
}
