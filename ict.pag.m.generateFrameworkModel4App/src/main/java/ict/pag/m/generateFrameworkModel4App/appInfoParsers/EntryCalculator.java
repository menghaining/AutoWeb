package ict.pag.m.generateFrameworkModel4App.appInfoParsers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

import ict.pag.m.frameworkInfoUtil.customize.Util;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.AnnotationEntity;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.xmlClassNodeEntity;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.xmlClassNodeEntity.SubInfo;
import ict.pag.m.generateFrameworkModel4App.entity.FrameworkModel;
import ict.pag.m.generateFrameworkModel4App.util.Configuration_backend;
import ict.pag.m.generateFrameworkModel4App.util.SpecialHelper;
import ict.pag.m.marks2SAInfer.util.structual.set2setPair;

public class EntryCalculator {
	private ClassHierarchy cha;
	private FrameworkModel frameworkModel;

	private AnnotationExtractor annoExtractor;
	private XMLExtractor xmlExtractor;
	private InheritanceExtractor inheExtractor;

	/* framework model information */
	private HashSet<set2setPair> EPmarks;

	/** Answers */
	private List<String> entriesSigs = new ArrayList<>();

	private HashSet<String> entriesSigs_pure = new HashSet<>();

	private HashSet<IClass> classesAll = new HashSet<>();

	public EntryCalculator(ClassHierarchy cha, FrameworkModel frameworkModel, AnnotationExtractor anno,
			InheritanceExtractor inheExtractor2, XMLExtractor xml) {
		this.cha = cha;

		this.frameworkModel = frameworkModel;
		/* framework model information */
		this.EPmarks = frameworkModel.getEntries();

		this.annoExtractor = anno;
		this.xmlExtractor = xml;
		this.inheExtractor = inheExtractor2;
	}

	public void matches() {
		matches_annotation();
//		matches_inheritance();
		matches_xml();

		/* add parameters */
		dealwithParams();

		/* Servlet Class is nature entry point class, framework-independent */
		if (Configuration_backend.g().servlet)
			matches_Servlet();

	}

	public int getEntryCount() {
		return entriesSigs_pure.size();
	}

	public int getClassesAllCount() {
		return classesAll.size();
	}

	public void matches_annotation() {

		/* application information */
		HashMap<String, HashSet<AnnotationEntity>> marks2classes = annoExtractor.getMark2classSet();
		HashMap<String, HashSet<AnnotationEntity>> marks2methods = annoExtractor.getMark2methodSet();

		for (set2setPair epm : EPmarks) {
			Set<String> methodmarks = epm.getSecond();
			for (String mm : methodmarks) {
				if (mm.startsWith("anno:mtd:")) {
					mm = mm.substring("anno:mtd:".length());
					if (marks2methods.containsKey(mm)) {
						HashSet<AnnotationEntity> candidates = marks2methods.get(mm);
						for (AnnotationEntity can : candidates) {
							if (can.getType().equals("method")) {
								IMethod mtd = can.getMethod();
								IClass declareClassName = mtd.getDeclaringClass();

								Set<String> classmarks = epm.getFirst();
								for (String cm : classmarks) {
									if (cm.startsWith("anno:class:")) {
										cm = cm.substring("anno:class:".length());
										if (marks2classes.containsKey(cm)) {
											HashSet<AnnotationEntity> entities = marks2classes.get(cm);
											for (AnnotationEntity canno : entities) {
												if (canno.getType().equals("class")) {
													IClass cclazz = canno.getClazz();
													if (declareClassName.toString().equals(cclazz.toString())) {
														if (!entriesSigs.contains(mtd.getSignature()))
															entriesSigs.add(mtd.getSignature());
														entriesSigs_pure.add(mtd.getSignature());
														classesAll.add(mtd.getDeclaringClass());
														/* add the parameter and return type init */
														createAllParameters(mtd);

														break;
													}
												}
											}
										}
									}
								}

							}
						}
					}
				}
			}

			/* entry class methods */
			Set<String> classmarks = epm.getFirst();
			for (String cm : classmarks) {
				if (cm.startsWith("anno:class:")) {
					cm = cm.substring("anno:class:".length());
					if (marks2classes.containsKey(cm)) {
						HashSet<AnnotationEntity> entities = marks2classes.get(cm);
						for (AnnotationEntity canno : entities) {
							if (canno.getType().equals("class")) {
								IClass cclazz = canno.getClazz();

								for (IMethod m : cclazz.getDeclaredMethods()) {
									/* add the parameter and return type init */
									createAllParameters(m);
									if (!entriesSigs.contains(m.getSignature()))
										entriesSigs.add(m.getSignature());
								}
							}
						}
					}
				}
			}
		}

		/* managed classes <init> may be entry class */
		for (HashSet<String> managed : frameworkModel.getManagedClasses().getCollection()) {
			for (String fmk_m : managed) {
				if (fmk_m.startsWith("anno:class:")) {
					fmk_m = fmk_m.substring("anno:class:".length());
					if (marks2classes.containsKey(fmk_m)) {
						HashSet<AnnotationEntity> classes = marks2classes.get(fmk_m);
						for (AnnotationEntity clazzEntity : classes) {
							IClass c1 = clazzEntity.getClazz();
							if (c1 instanceof ShrikeClass) {
								ShrikeClass sc1 = (ShrikeClass) c1;
								try {
									if (sc1.isInnerClass()) {
										if (sc1.getOuterClass() != null) {
											TypeReference outerClass = sc1.getOuterClass();
											IClass outer = cha.getLoader(ClassLoaderReference.Application)
													.lookupClass(outerClass.getName());
											if (outer != null) {
												for (IMethod m : outer.getDeclaredMethods()) {
													if (!entriesSigs.contains(m.getSignature()))
														entriesSigs.add(m.getSignature());
												}
												if (outer instanceof ShrikeClass) {
													Set<IMethod> initMethods = ((ShrikeClass) outer).getInitMethod();
													for (IMethod m : initMethods) {
														if (!entriesSigs.contains(m.getSignature()))
															entriesSigs.add(m.getSignature());
														classesAll.add(m.getDeclaringClass());
													}
												}
											}
										}
									}
								} catch (InvalidClassFileException e) {
									// cannot find outer class
								}

							}
//							for (IMethod m : c1.getDeclaredMethods()) {
//								if (!m.isAbstract())
//									entriesSigs.add(m.getSignature());
//							}

							addConstructorsIntoEntriesSigs(c1);
						}
					}
				}
			}
		}
	}

	private void createAllParameters(IMethod mtd) {
		// parameter
		int i = 1;
		if (mtd.isStatic())
			i = 0;
		for (; i < mtd.getNumberOfParameters(); i++) {
			IClass pc = cha.getLoader(ClassLoaderReference.Application).lookupClass(mtd.getParameterType(i).getName());
			if (pc != null) {
				addConstructorsIntoEntriesSigs(pc);
				for (IClass sub : cha.computeSubClasses(pc.getReference())) {
					addConstructorsIntoEntriesSigs(sub);
				}

			}
		}

	}

	private void addConstructorsIntoEntriesSigs(IClass c1) {
		for (IMethod m1 : SpecialHelper.findAllInitMethods(c1)) {
			if (!entriesSigs.contains(m1.getSignature()))
				entriesSigs.add(m1.getSignature());
			classesAll.add(m1.getDeclaringClass());
		}
		for (IMethod m1 : SpecialHelper.findAllClinitMethods(c1)) {
			if (!entriesSigs.contains(m1.getSignature()))
				entriesSigs.add(m1.getSignature());
			classesAll.add(m1.getDeclaringClass());
		}

	}

	public List<String> getEntriesSignatures() {
		return entriesSigs;
	}

	public void matches_inheritance() {
		/* application information */
		HashMap<String, HashSet<IClass>> marks2classes = inheExtractor.getMark2Class();

		for (set2setPair epm : EPmarks) {
			Set<String> methodmarks = epm.getSecond();
			for (String mm : methodmarks) {
				if (mm.startsWith("inhe:mtd:")) {
					mm = mm.substring("inhe:mtd:".length());
					String name1 = mm.substring(mm.lastIndexOf('.') + 1, mm.indexOf('('));

					Iterator<IClass> allclassIterator = cha.getLoader(ClassLoaderReference.Application)
							.iterateAllClasses();
					while (allclassIterator.hasNext()) {
						IClass clazz = allclassIterator.next();
						for (IMethod m : clazz.getAllMethods()) {
							String sig = m.getSignature();
							String msig = sig.substring(sig.lastIndexOf('.') + 1);
							String name2 = msig.substring(msig.lastIndexOf('.') + 1, msig.indexOf('('));
							if (mm.equals(msig) || name1.equals(name2)) {
								Set<String> clazzmarks = epm.getFirst();
								for (String cm : clazzmarks) {
									if (cm.startsWith("inhe:class:")) {
										cm = cm.substring("inhe:class:".length());
										if (marks2classes.containsKey(cm)) {
											for (IClass c : marks2classes.get(cm)) {
												if (clazz.toString().equals(c.toString())) {
													if (!entriesSigs.contains(m.getSignature()))
														entriesSigs.add(m.getSignature());
													entriesSigs_pure.add(m.getSignature());
													classesAll.add(m.getDeclaringClass());
													/* add the parameter and return type init */
													createAllParameters(m);
													break;
												}
											}
										}
									}
								}

							}
						}
					}
				}
			}
			/* entry class methods */
			Set<String> clazzmarks = epm.getFirst();
			for (String cm : clazzmarks) {
				if (cm.startsWith("inhe:class:")) {
					cm = cm.substring("inhe:class:".length());
					if (marks2classes.containsKey(cm)) {
						for (IClass c : marks2classes.get(cm)) {
							for (IMethod m : c.getDeclaredMethods()) {
								if (!entriesSigs.contains(m.getSignature()))
									entriesSigs.add(m.getSignature());
								/* add the parameter and return type init */
								createAllParameters(m);
							}
						}
					}
				}
			}
		}

		/* managed classes constructers may be entry class */
		for (HashSet<String> managed : frameworkModel.getManagedClasses().getCollection()) {
			for (String fmk_m : managed) {
				if (fmk_m.startsWith("inhe:class:")) {
					fmk_m = fmk_m.substring("inhe:class:".length());
					if (marks2classes.containsKey(fmk_m)) {

						HashSet<IClass> classes = marks2classes.get(fmk_m);
						for (IClass c1 : classes) {
							if (c1 instanceof ShrikeClass) {
								ShrikeClass sc1 = (ShrikeClass) c1;
								try {
									if (sc1.isInnerClass()) {
										if (sc1.getOuterClass() != null) {
											TypeReference outerClass = sc1.getOuterClass();
											IClass outer = cha.getLoader(ClassLoaderReference.Application)
													.lookupClass(outerClass.getName());
											if (outer != null) {
												for (IMethod m : outer.getDeclaredMethods()) {
													if (!entriesSigs.contains(m.getSignature()))
														entriesSigs.add(m.getSignature());
												}
												if (outer instanceof ShrikeClass) {
													Set<IMethod> initMethods = ((ShrikeClass) outer).getInitMethod();
													for (IMethod m : initMethods) {
														if (!entriesSigs.contains(m.getSignature()))
															entriesSigs.add(m.getSignature());
														classesAll.add(m.getDeclaringClass());
													}
												}
											}
										}
									}
								} catch (InvalidClassFileException e) {
									// cannot find outer class
								}

							}
//							for (IMethod m : c1.getDeclaredMethods()) {
//								if (!m.isAbstract())
//									entriesSigs.add(m.getSignature());
//							}

							addConstructorsIntoEntriesSigs(c1);
						}
					}
				}
			}
		}
	}

	public void matches_xml() {
		/* application information */
		HashMap<String, HashSet<xmlClassNodeEntity>> marks2classEntities = xmlExtractor.getMark2classSet();
		Set<String> appMarks = marks2classEntities.keySet();

		for (set2setPair epm : EPmarks) {
			Set<String> methodmarks = epm.getSecond();
			for (String mm : methodmarks) {
				if (mm.startsWith("xml:mtd:")) {
					mm = mm.substring("xml:mtd:".length());

					String newMtdMarks = "";
					String attrName = "";
					String[] alls = mm.split(";");
					for (int i = 0; i < alls.length; i++) {
						String curr = alls[i];
						if (i == alls.length - 1) {
							attrName = curr.substring(curr.indexOf(':') + 1);
						} else {
							newMtdMarks = newMtdMarks + ";" + curr.substring(curr.indexOf(':') + 1);
						}

					}
					newMtdMarks = newMtdMarks.substring(1);
					for (String appm : appMarks) {
						if (appm.startsWith(newMtdMarks)) {
							for (xmlClassNodeEntity entity : marks2classEntities.get(appm)) {
								if (entity.getName2value().containsKey(attrName)) {
									// this layer
									// find the entry method
									String mtdName = entity.getName2value().get(attrName);
									String clazzName = entity.getClazz();
									addXMLEntryIntoEntriesSigs(clazzName, mtdName);
								} else {
									// sub layer
									for (SubInfo sub : entity.getSubInfos()) {
										String path = newMtdMarks + ";" + sub.getTagName();
										if (appm.startsWith(path)) {
											if (sub.getName2value_sub().containsKey(attrName)) {
												// find the entry method
												String mtdName = sub.getName2value_sub().get(attrName);
												String clazzName = entity.getClazz();
												addXMLEntryIntoEntriesSigs(clazzName, mtdName);
											}
										}
									}
								}

							}
						}
					}

				}
			}
		}

		/* managed classes init may be entry class */
		for (HashSet<String> managed : frameworkModel.getManagedClasses().getCollection()) {
			for (String fmk_m : managed) {
				if (fmk_m.startsWith("xml:")) {
					fmk_m = fmk_m.substring("xml:".length());
					String path_fmk = "";
					String attr_fmk = "";
					String[] alls = fmk_m.split(";");
					for (int i = 0; i < alls.length; i++) {
						String curr = alls[i];
						if (i == alls.length - 1) {
							attr_fmk = curr.substring(curr.indexOf(':') + 1);
						} else {
							path_fmk = path_fmk + ";" + curr.substring(curr.indexOf(':') + 1);
						}

					}
					path_fmk = path_fmk.substring(1);

					for (String appm : appMarks) {
						if (appm.contains(path_fmk)) {
							// find class
							for (xmlClassNodeEntity en : marks2classEntities.get(appm)) {
								addXMLEntryIntoEntriesSigs(en.getClazz(), "<init>");
								addXMLEntryIntoEntriesSigs(en.getClazz(), "<clinit>");
							}
						}
					}
				}
			}
		}

	}

	private void addXMLEntryIntoEntriesSigs(String clazzName, String mtdName) {
		cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(c1 -> {
			String clazz = Util.format(c1.getName().toString());
			if (clazz.equals(clazzName)) {
				classesAll.add(c1);
				if (c1 instanceof ShrikeClass) {
					ShrikeClass sc1 = (ShrikeClass) c1;
					try {
						if (sc1.isInnerClass()) {
							if (sc1.getOuterClass() != null) {
								TypeReference outerClass = sc1.getOuterClass();
								IClass outer = cha.getLoader(ClassLoaderReference.Application)
										.lookupClass(outerClass.getName());
								if (outer != null) {
									for (IMethod m : outer.getDeclaredMethods()) {
										if (!entriesSigs.contains(m.getSignature()))
											entriesSigs.add(m.getSignature());
									}
									if (outer instanceof ShrikeClass) {
										Set<IMethod> initMethods = ((ShrikeClass) outer).getInitMethod();
										for (IMethod m : initMethods) {
											if (!entriesSigs.contains(m.getSignature()))
												entriesSigs.add(m.getSignature());
											classesAll.add(m.getDeclaringClass());

										}
									}
								}
							}
						}
					} catch (InvalidClassFileException e) {
						// cannot find outer class
					}

				}
				for (IMethod m : c1.getDeclaredMethods()) {

					/* managed class */
					if (!entriesSigs.contains(m.getSignature()))
						entriesSigs.add(m.getSignature());

					if (m.getSignature().contains(mtdName)) {
						if (!entriesSigs.contains(m.getSignature()))
							entriesSigs.add(m.getSignature());
						entriesSigs_pure.add(m.getSignature());

						/* add the parameter and return type init */
						createAllParameters(m);
					}
				}
			}
		});

	}

	private void dealwithParams() {
		HashSet<String> concernedParamTypesSet = frameworkModel.getEntries_params();
		// when formal parameters' type is concerned, collect the declare class of the
		// classes
		HashSet<IClass> servletClasses = new HashSet<>();
		cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(c -> {
			c.getDeclaredMethods().forEach(m -> {
				for (int i = 0; i < m.getNumberOfParameters(); i++) {
					TypeReference paramType = m.getParameterType(i);
					TypeName name = paramType.getName();
					String name2 = Util.format(name.toString());
					if (hasElement(concernedParamTypesSet, name2)) {
						// is the same type of concerned type
						servletClasses.add(m.getDeclaringClass());
					} else {
						// may subclass of concerned type
						HashMap<String, HashSet<IClass>> marks2classes = inheExtractor.getMark2Class();
						if (marks2classes.containsKey(name2)) {
							for (IClass c0 : marks2classes.get(name2)) {
								if (hasElement(concernedParamTypesSet, Util.format(c0.getName().toString()))) {
									servletClasses.add(m.getDeclaringClass());
									break;
								}
							}
						}
					}

				}
			});
		});

		/**
		 * For every entry point class we make its methods entry points(the same as
		 * JackEE)
		 **/
		for (IClass c1 : servletClasses) {
			for (IMethod m : c1.getDeclaredMethods()) {
				if (!entriesSigs.contains(m.getSignature()))
					entriesSigs.add(m.getSignature());
				/* add the parameter and return type init */
				createAllParameters(m);
			}
		}

	}

	private boolean hasElement(HashSet<String> concernedParamTypesSet, String name2) {
		for (String ele : concernedParamTypesSet) {
			if (ele.equals(name2))
				return true;
		}
		return false;
	}

	/**
	 * do not execute as default</br>
	 * use '-servlet' to enable
	 */
	private void matches_Servlet() {
		HashSet<IClass> servletClasses = new HashSet<>();

		HashMap<String, HashSet<IClass>> marks2classes = inheExtractor.getMark2Class();
		if (marks2classes.containsKey("javax.servlet.http.HttpServletRequest")) {
			servletClasses.addAll(marks2classes.get("javax.servlet.http.HttpServletRequest"));
		} else if (marks2classes.containsKey("javax.servlet.http.HttpServletResponse")) {
			servletClasses.addAll(marks2classes.get("javax.servlet.http.HttpServletResponse"));
		} else if (marks2classes.containsKey("javax.servlet.ServletRequest")) {
			servletClasses.addAll(marks2classes.get("javax.servlet.ServletRequest"));
		} else if (marks2classes.containsKey("javax.servlet.ServletResponse")) {
			servletClasses.addAll(marks2classes.get("javax.servlet.ServletResponse"));
		}

		cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(c -> {
			c.getDeclaredMethods().forEach(m -> {
				for (int i = 0; i < m.getNumberOfParameters(); i++) {
					TypeReference paramType = m.getParameterType(i);
					TypeName name = paramType.getName();
					if (name.toString().equals("Ljavax/servlet/http/HttpServletRequest")
							|| name.toString().equals("Ljavax/servlet/http/HttpServletResponse")
							|| name.toString().equals("Ljavax/servlet/ServletRequest")
							|| name.toString().equals("Ljavax/servlet/ServletResponse")) {
						servletClasses.add(m.getDeclaringClass());
					}

				}
			});
		});

		// add
		for (IClass c1 : servletClasses) {
			for (IMethod m : c1.getDeclaredMethods()) {
				if (!entriesSigs.contains(m.getSignature()))
					entriesSigs.add(m.getSignature());
				/* add the parameter and return type init */
				createAllParameters(m);
			}
		}
	}

}
