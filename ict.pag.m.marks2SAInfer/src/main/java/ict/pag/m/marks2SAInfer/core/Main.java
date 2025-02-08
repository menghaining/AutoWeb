package ict.pag.m.marks2SAInfer.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.FieldReference;

import ict.pag.m.frameworkInfoUtil.infoEntity.infoPair;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoUnit;
import ict.pag.m.frameworkInfoUtil.customize.CLIOption;
import ict.pag.m.frameworkInfoUtil.customize.ConfigUtil;
import ict.pag.m.frameworkInfoUtil.customize.GraphBuilder;
import ict.pag.m.frameworkInfoUtil.customize.Util;
import ict.pag.m.frameworkInfoUtil.frameworkInfoInAppExtract.infosExtractor;
import ict.pag.m.marks2SAInfer.answersPrinter.writeAnswers;
import ict.pag.m.marks2SAInfer.core.calculator.ActualReturnTypeCalculator;
import ict.pag.m.marks2SAInfer.core.calculator.CallSequenceCalculator;
import ict.pag.m.marks2SAInfer.core.calculator.EntryPointCalculator;
import ict.pag.m.marks2SAInfer.core.calculator.FrameworkIndirectInvokeCalculator;
import ict.pag.m.marks2SAInfer.dynamicInfo.ManagedClass;
import ict.pag.m.marks2SAInfer.staticInfo.entryMarksIgnoreInfos;
import ict.pag.m.marks2SAInfer.staticInfo.objectInstanceInfos;
import ict.pag.m.marks2SAInfer.util.CallsiteInfo;
import ict.pag.m.marks2SAInfer.util.CollectionUtil;
import ict.pag.m.marks2SAInfer.util.FileUtil;
import ict.pag.m.marks2SAInfer.util.reducedSetCollection;
import ict.pag.m.marks2SAInfer.util.resolveMarksUtil;
import ict.pag.m.marks2SAInfer.util.structual.EntrySet;
import ict.pag.m.marks2SAInfer.util.structual.FrmkRetPoints2;
import ict.pag.m.marks2SAInfer.util.structual.SequenceSet2;
import ict.pag.m.marks2SAInfer.util.structual.frameworkIndirectInvoke;
import ict.pag.m.marks2SAInfer.util.structual.sequencePair2;
import ict.pag.m.marks2SAInfer.util.structual.set2setPair;

public class Main {

	/**
	 * arg[0]: classes directory including configuration files</br>
	 * arg[1]: running log txt file
	 */
	public static void main(String args[]) {
		long beforeTime0 = System.nanoTime();

		CLIOption cliOptions = new CLIOption(args);
		ConfigUtil.g().loadConfiguration(cliOptions);

//		HashSet<String> frameworkInheritance = FileUtil.getAllFrameworkLibs();
//		System.out.println("[info] read frameworks information done");

		// application classes path
		String path = ConfigUtil.g().getAnalyseDir();
		System.out.println("......[info][start]" + path);

		/* cha and chaCG only for application codes */
		GraphBuilder builder = new GraphBuilder(path);
		ClassHierarchy cha = builder.getCHA();
		CHACallGraph chaCG = builder.getAppCHACG();

		/**
		 * Step 1. deal with running logs
		 */
		String original_log = ConfigUtil.g().getLogFile();
		LogParser logparser = new LogParser(original_log, builder);
		HashMap<Integer, ArrayList<String>> id2group = logparser.getId2group();
		HashMap<Integer, ArrayList<String>> id2group2 = logparser.getId2group_all();
		// exclude exception call logs
		HashMap<Integer, ArrayList<String>> id2group_removeBad = LogParser.removeExceptionCallStmts(id2group2);

		/**
		 * Step 2. get all kinds of marks
		 */
		List<String> XMLs = FileUtil.getAllConcernedXMLs(ConfigUtil.g().getConfigFiles());
		infosExtractor extractor = new infosExtractor(cha, chaCG, XMLs);
		extractor.extract();
		/** parse inheritance and annotation marks */
		Set<infoUnit> inheritanceSet = extractor.getInheritanceMarksSet();

		// only key=class
		Set<infoUnit> xmlSet = extractor.getXmlMarksSet_clear();
		// all
		Set<infoUnit> xmlSet_all = extractor.getXmlMarksSet();

		Set<infoUnit> AnnoSet = extractor.getAnnoMarksSet();
		Set<infoUnit> f_annos = extractor.getAnnos_field();
		Set<infoUnit> m_annos = extractor.getAnnos_mthd();
		Set<infoUnit> c_annos = extractor.getAnnos_clazz();

		HashMap<String, HashSet<String>> class2AllApplicationAncestors = extractor.getClass2AllApplicationAncestors();

		/* calculate all runnable tags */
		HashSet<String> allrunnableMarks = new HashSet<>();
		HashSet<String> dynamicCoversClasses = new HashSet<>();
		for (String mtd : logparser.getAllRunnableMethods()) {
			String clazzName = mtd.substring(0, mtd.lastIndexOf('.'));
			dynamicCoversClasses.add(clazzName);
			/* class level */
			// annotations
			ArrayList<String> need2add_anno = resolveMarksUtil.getClassAnnos_withDecorate(clazzName, c_annos, "class");
			if (!need2add_anno.isEmpty()) {
				allrunnableMarks.addAll(need2add_anno);
			}
			// xml configured
			ArrayList<String> need2add_xml = resolveMarksUtil.getXMLMarks_withDecorate(clazzName, xmlSet);
			if (!need2add_xml.isEmpty()) {
				allrunnableMarks.addAll(need2add_xml);
			}
			// inheritance
			ArrayList<String> need2add_inhe = resolveMarksUtil.getInheritanceMarks_onclass(clazzName, inheritanceSet);
			if (!need2add_inhe.isEmpty()) {
				allrunnableMarks.addAll(need2add_inhe);
			}
			/* method level */
			ArrayList<String> need2add_anno2 = resolveMarksUtil.getAnnosMarks(mtd, m_annos);
			if (!need2add_anno2.isEmpty()) {
				allrunnableMarks.addAll(need2add_anno2);
			}
			for (String t : resolveMarksUtil.getInheritanceMarks(mtd, inheritanceSet)) {
				HashSet<String> tmpSet = ConfigUtil.g().getAllFrameworkParentsCalls(t);
				if (!tmpSet.isEmpty())
					allrunnableMarks.add("inhe:full:" + t);
				else
					allrunnableMarks.add("inhe:class:" + t.substring(0, t.lastIndexOf('.')));
//				for (String tmp : tmpSet) {
//					allrunnableMarks.add(tmp);
//				}
//				String tmp = CollectionUtil.frameworkInheritanceSetContainsEle(frameworkInheritance, t);
//				if (tmp == null || tmp.equals(""))
//					continue;
//				allrunnableMarks.add(tmp);
			}
			ArrayList<String> xml_alls2 = resolveMarksUtil.getXMLMarks_mthd(mtd, xmlSet);
			if (xml_alls2 != null)
				allrunnableMarks.addAll(xml_alls2);
			// field in the declaring class
			ArrayList<String> f_annos_toadd = resolveMarksUtil.getAnnos_field_on_Class(clazzName, f_annos);
			if (f_annos_toadd != null)
				allrunnableMarks.addAll(f_annos_toadd);

		}

		System.out.println("[info][running covers classes] " + dynamicCoversClasses.size());
		System.out.println("[info][running covers methods] " + logparser.getAllRunnableMethods().size());
		System.out.println("=========[runnable covers mark]===========");
		for (String m : allrunnableMarks)
			System.out.println("[runnable covers mark] " + m);
		System.out.println("=========[runnable covers mark]===========");
		System.out.println("=========[runnable trigger calls]===========");
		for (String m : ConfigUtil.g().getKeyCalls())
			System.out.println("[runnable trigger calls] " + m);
		System.out.println("=========[runnable trigger calls]===========");

		/**
		 * Step 3. calculate the normal marks :</br>
		 * for class, all methods do not have caller</br>
		 * for methods, all with this mark do not have caller
		 * 
		 */
		entryMarksIgnoreInfos ignoreEntryMark = new entryMarksIgnoreInfos(builder, AnnoSet, inheritanceSet, xmlSet);
		// normal marks means the marks that has caller in code
		Set<String> normalMarksSet = ignoreEntryMark.getAllMarks();

		/**
		 * Step 4. get the marks that maybe object managed by framework(like. spring
		 * beans)
		 * 
		 */
		long beforeTime1 = System.nanoTime();

		objectInstanceInfos obj_collector = new objectInstanceInfos(builder, f_annos, m_annos, c_annos, xmlSet,
				xmlSet_all, inheritanceSet);
		// Step 4 Answer1: managedClassSet means which Class managed by framework,the
		// class type that need to generate object
//		reducedSetCollection managedClassMarksSet = obj_collector.getManagedClassMarks();
		reducedSetCollection managedClassMarksSet = new reducedSetCollection();
		ManagedClass dynamicClassCalculater = new ManagedClass(id2group, c_annos, xmlSet, inheritanceSet);
		managedClassMarksSet.addAll(dynamicClassCalculater.getManagedClassMarks());
		// Step 4 Answer2: where to inject object and fields point to which object
		reducedSetCollection managedFieldMarksSet = obj_collector.getManagedFieldMarks();
		reducedSetCollection managedFunctionMarksSet = obj_collector.getManagedFunctionMarks();

		/* outer2Seq: in 'outer' function, directly actual invokes */
		HashMap<String, Set<ArrayList<String>>> outer2Seq = SplitStmtPerFunctionLayer.split(id2group_removeBad);

		MapCallsite2CallSeq tools = new MapCallsite2CallSeq(chaCG, outer2Seq);
		tools.dealWith();
		// record the callsite to real target when the real target and the base object
		// declare type are different. The call site is application codes.
		HashMap<FieldReference, String> fieldRef2target_app = tools.getFieldRef2target_app();
		// the framework and its targets
		HashMap<CallsiteInfo, ArrayList<String>> callsite2target_frk = tools.getCallsite2target_frk();
		// record call sequence after one line callsite
		HashSet<ArrayList<String>> actualSeqAfterOneLineCallsite = tools.getActualSeqAfterOneLineCallsite();
		// map:<callsite-match, actualCalls>, record call sequence after one line
		// callsite
		HashMap<String, ArrayList<String>> actualSeqAfterOneLineCallsiteMap = tools
				.getActualSeqAfterOneLineCallsiteMap();

		// callsite and its actual calls map,
		// but the actual calls not the same class with callsite declared
		HashMap<String, HashSet<String>> frkInheritance = tools.getFrameworkInternalInheritanceRelation();

		/**
		 * marks cal: alias Marks and alias apply marks infer field points to which type
		 * accurately
		 */
//		HashMap<String ,String> alias2Full = new HashMap<>();
		// TODO: different type of marks need to support mixed
		// Step 4 Answer3: alias marks, alias the value of mark
		HashSet<String> classAliasMarks = new HashSet<String>();
		// Step 4 Answer4: point to actual type, which may have alias in mark value
		HashSet<String> fieldActualTypeMarks = new HashSet<String>();
		for (FieldReference fieldRef : fieldRef2target_app.keySet()) {
			// actual call
			String target = fieldRef2target_app.get(fieldRef);
			String actualcallType = target.substring(0, target.lastIndexOf('.'));
			// field
			String fieldSig = Util.format(fieldRef.getSignature());
			String fieldName = fieldRef.getName().toString();
			String fieldBelongTo = Util.format(fieldRef.getDeclaringClass().getName().toString());

			/* Mark Type 1. annotation configured field */
			// field annotations
			List<infoPair> fieldMarks_annos = null;
			for (infoUnit info : f_annos) {
				if (info.getBase().equals(fieldSig)) {
					fieldMarks_annos = info.getFields();
					break;
				}
			}
			// actual target class annotations
			for (infoUnit info : c_annos) {
				if (info.getBase().equals(actualcallType)) {
					List<infoPair> classAnnoSet = info.getFields();
					for (infoPair mark2attrvals : classAnnoSet) {
						Set<String> classvals = mark2attrvals.getAllValues_tolower();
						if (fieldMarks_annos == null)
							continue;
						for (infoPair f_markvals : fieldMarks_annos) {
							Set<String> fieldsvals = f_markvals.getAllValues_tolower();
							Set<String> sameSet = CollectionUtil.calSameElements(classvals, fieldsvals);
							if (!sameSet.isEmpty()) {

								String class_mark = mark2attrvals.getMark();
								String field_mark = f_markvals.getMark();

								classAliasMarks.add("anno:" + class_mark);
								fieldActualTypeMarks.add("anno:" + field_mark);

//								for(String s:sameSet) {
//									alias2Full.put(s, actualcallType);
//								}

							} else {
								String className = info.getBase().substring(info.getBase().lastIndexOf(".") + 1)
										.toLowerCase();
								// class naturally has alias that className and full Name
								if (fieldsvals.contains(className)
										|| fieldsvals.contains(info.getBase().toLowerCase())) {
									String field_mark = f_markvals.getMark();

									fieldActualTypeMarks.add("anno:" + field_mark);
								}
							}
						}

					}

					break;
				}
			}

			// Mark Type 2. xml configured field
			for (infoUnit info : xmlSet) {
				if (info.getBase().equals(actualcallType)) {

					List<infoPair> classXmlSet = info.getFields();
					infoPair currLayer = classXmlSet.get(classXmlSet.size() - 1);
					Map<String, String> currLayerInfo = currLayer.getValue();

					String xmlAlised = null;
					for (String kk : currLayerInfo.keySet()) {
						if (currLayerInfo.get(kk).equals(actualcallType)) {
							xmlAlised = kk;
							break;
						}
					}

					for (String kk : currLayerInfo.keySet()) {
						// calculate the candidate class alias
						Set<infoUnit> equivalentSet = new HashSet<>();
						if (!currLayerInfo.get(kk).equals(actualcallType)) {
							String tmpName = currLayerInfo.get(kk);
							for (infoUnit info2 : xmlSet_all) {
								if (info2.getBase().equals(tmpName)) {
									if (!CollectionUtil.isSameFields(info2.getFields(), classXmlSet))
										equivalentSet.add(info2);
								}
							}
						}

						if (equivalentSet.isEmpty())
							continue;

						// calculate the class xml marks
						// like, :beans;:bean;myType:id
						StringBuilder aliasMark_tmp = new StringBuilder();
						for (int i = 0; i < classXmlSet.size(); i++) {
							infoPair p = classXmlSet.get(i);
							if (i == 0) {
								aliasMark_tmp.append(p.getMark());
							} else {
								aliasMark_tmp.append(";");
								aliasMark_tmp.append(p.getMark());
							}
						}
//						aliasMark_tmp.append(";myType:" + kk);
						// last field is <alias,aliaed>
						aliasMark_tmp.append(";<" + kk + "," + xmlAlised + ">");

						if (!equivalentSet.isEmpty()) {
							for (infoUnit mem : equivalentSet) {
								String tmp_base = mem.getBase();
								List<infoPair> toDealFields = mem.getFields();
								boolean hasClass = false;
								boolean hasField = false;

								StringBuilder fieldMark = new StringBuilder();
								StringBuilder fieldActualTypeMark = new StringBuilder();

								StringBuilder common = new StringBuilder();
								// TODO: increase the way of identify field configuration in xml
								for (infoPair tmp_info : toDealFields) {
									if (fieldActualTypeMark.length() == 0) {
										fieldActualTypeMark.append(tmp_info.getMark());
										fieldMark.append(tmp_info.getMark());
										common.append(tmp_info.getMark());
									} else {
										fieldActualTypeMark.append(";" + tmp_info.getMark());
										if (!hasField) {
											fieldMark.append(";" + tmp_info.getMark());
											common.append(";" + tmp_info.getMark());
										}
									}

									if (tmp_info.getAllValues().contains(fieldName)) {
										hasField = true;
										tmp_info.getValue().forEach((k, v) -> {
											if (v.equals(fieldName)) {
												fieldMark.append(";myType:" + k);
												return;
											}
										});
									}
									if (tmp_info.getAllValues().contains(fieldBelongTo)) {
										hasClass = true;
//									}
									}
								}

								if (toDealFields != null && toDealFields.get(toDealFields.size() - 1) != null
										&& toDealFields.get(toDealFields.size() - 1).getValue() != null) {
									toDealFields.get(toDealFields.size() - 1).getValue().forEach((k, v) -> {
										if (v.equals(tmp_base)) {
											fieldActualTypeMark.append(";myType:" + k);
											return;
										}
									});

									String ref = fieldMark.toString();
									if (fieldMark.length() > common.length())
										ref = fieldMark.substring(common.length() + 1);
									String tarType = fieldActualTypeMark.toString();
									if (fieldActualTypeMark.length() > common.length())
										tarType = fieldActualTypeMark.substring(common.length() + 1);

									if (hasClass && hasField) {
										classAliasMarks.add("xml:" + aliasMark_tmp);
//									fieldActualTypeMarks.add("xml:" + fieldActualTypeMark);
										fieldActualTypeMarks.add("xml:" + common + ";<" + ref + "," + tarType + ">");

										HashSet<String> tmp2add = new HashSet<String>();
										tmp2add.add("xml:" + fieldMark.toString());
										managedFieldMarksSet.add(tmp2add);
									}
								}
							}
						}

					}
				}
			}
		}

		double buildTime1 = (System.nanoTime() - beforeTime1) / 1E9;
		System.out.println("[TIME-LOG] Framework Mananged Objects Done in " + buildTime1 + " s!");

		/** framework indirectly invoke marks */
		long beforeTime2 = System.nanoTime();
		FrameworkIndirectInvokeCalculator indrectInvokeCalculator = new FrameworkIndirectInvokeCalculator(
				inheritanceSet, AnnoSet, xmlSet);
		indrectInvokeCalculator.calFrameworkIndirectInvoke(callsite2target_frk);
		Set<frameworkIndirectInvoke> allIndirectFrameworkInvokeSet = indrectInvokeCalculator
				.getAllIndirectFrameworkInvokeSet();

		HashSet<String> indirectlyTargets = indrectInvokeCalculator.getIndirectlySameTargetSet();
		double buildTime2 = (System.nanoTime() - beforeTime2) / 1E9;
		System.out.println("[TIME-LOG] Framework Indirectly Invoke Done in " + buildTime2 + " s!");

		/** framework return depends on configuration */
		HashMap<CallsiteInfo, String> callsite2target_app = tools.getCallsite2target_app();
		ActualReturnTypeCalculator returnCalculator = new ActualReturnTypeCalculator(callsite2target_app, chaCG,
				AnnoSet, xmlSet_all);
		HashSet<FrmkRetPoints2> frmwkRetAcutalClass = returnCalculator.getFrameworkRetActualPoints2();

		/**
		 * STEP 5. get the marks that maybe entries</br>
		 * represents pair-<{class marks}, {method marks}> marks
		 */
		long beforeTime4 = System.nanoTime();
		HashSet<String> entryIgnoreCalls = tools.getEntryIgnores();
		if (!indirectlyTargets.isEmpty())
			entryIgnoreCalls.addAll(indirectlyTargets);
		EntryPointCalculator entrypointcal = new EntryPointCalculator(entryIgnoreCalls, normalMarksSet, id2group,
				inheritanceSet, AnnoSet, xmlSet, xmlSet_all);
		EntrySet entries = entrypointcal.getEntries();
		HashSet<String> mayEntryPointFormalParameterSet = entrypointcal.getMayEntryPointFormalParameterSet();

		double buildTime4 = (System.nanoTime() - beforeTime4) / 1E9;
		System.out.println("[TIME-LOG] EntryPoints Infer Done in " + buildTime4 + " s!");

		/** STEP 6. framework before-after call sequence calculate */
		/** marks cal: framework call sequence between two calls */
		long beforeTime3 = System.nanoTime();
		CallSequenceCalculator seqCalculator = new CallSequenceCalculator(inheritanceSet, AnnoSet, xmlSet,
				normalMarksSet);
		seqCalculator.setEntryCalls(entrypointcal.getEntryCalls());
		seqCalculator.setIgnores(indirectlyTargets);
		for (ArrayList<String> actualCalls : actualSeqAfterOneLineCallsite) {
			// 1. remove reachable calls
//			ArrayList<String> actualCalls2 = new ArrayList<>();
//			for (String call : actualCalls) {
//				if (!call.endsWith("[normal]"))
//					actualCalls2.add(call);
//			}
			// 2. calculate the call sequence after callsite
//			seqCalculator.calSequence(actualCalls2, actualCalls);
//			seqCalculator.calSequence2(actualCalls);
			seqCalculator.calSequence3(actualCalls);
		}

		// 3. if have sequence<s1,s2>&&<s2,s1>, then dismiss
		SequenceSet2 tmpseq = seqCalculator.getSequenceSet2();
		/** Answer */
		HashSet<sequencePair2> sequencePairSet = new HashSet<>();
		for (sequencePair2 pair1 : tmpseq.getAllSequencePair2()) {
			boolean add = true;
			for (sequencePair2 pair2 : tmpseq.getAllSequencePair2()) {
				if (reducedSetCollection.elementEquals(pair1.getPre(), pair2.getPost())
						&& reducedSetCollection.elementEquals(pair2.getPre(), pair1.getPost())) {
					add = false;
					break;
				}
			}
			if (add)
				sequencePairSet.add(pair1);
		}
		double buildTime3 = (System.nanoTime() - beforeTime3) / 1E9;
		System.out.println("[TIME-LOG] Framework Call Sequence Done in " + buildTime3 + " s!");

		/** SETP 7. calculate configure code marks and filter marks */
		HashMap<Integer, ArrayList<String>> id2group_config = new HashMap<>();
		reducedSetCollection config_class_marks = new reducedSetCollection();
		config_class_marks.addIgnoreClassMarks(normalMarksSet);
		reducedSetCollection filter_class_marks = new reducedSetCollection();
		filter_class_marks.addIgnoreClassMarks(normalMarksSet);

// ------------------------------------------------------------------------------
//		HashSet<String> allConfigureUnreachableInvokes = new HashSet<>();
//		for (Integer i : id2group.keySet()) {
//			ArrayList<String> sequences = id2group.get(i);
//			boolean hasReq = false;
//			for (String cc : sequences) {
//				if (cc.startsWith("url started")) {
//					hasReq = true;
//					break;
//				}
//			}
//
//			if (!hasReq) {
//				ArrayList<String> newSeq = sequences;
//				for (String cc : newSeq) {
//					allConfigureUnreachableInvokes.add(cc);
//					// do not calculate the framework indirectly call
//					if (indirectlyTargets.contains(cc))
//						continue;
//					String className = cc.substring(0, cc.lastIndexOf("."));
//					String mthdName = cc.substring(cc.lastIndexOf(".") + 1);
//					// some class may has init methods for configuration class call
////					if (mthdName.toLowerCase().startsWith("init"))
////						continue;
//
//					// annotations marks
//					ArrayList<String> need2add = new ArrayList<String>();
////					ArrayList<String> need2add_anno_class = resolveMarksUtil.getAnnosMarksOnly_onclass(className, c_annos);
//					ArrayList<String> need2add_anno = resolveMarksUtil.getAnnosMarks_withDecorate(cc, AnnoSet);
//					if (!need2add_anno.isEmpty()) {
//						need2add.addAll(need2add_anno);
//					}
//					// inheritance marks
//					ArrayList<String> need2add_inher = resolveMarksUtil.getInheritanceMarks_withDecorate(cc,
//							inheritanceSet);
//					if (!need2add_inher.isEmpty()) {
//						need2add.addAll(need2add_inher);
//					}
//					// xml-configured marks
//					ArrayList<String> need2add_xml = resolveMarksUtil.getXMLMarks(className, xmlSet);
//					if (!need2add_xml.isEmpty()) {
//						need2add.addAll(need2add_xml);
//					}
//
//					if (!need2add.isEmpty()) {
//						config_class_marks.add(new HashSet<>(need2add));
//					}
//
//					// TODO: 非configure类的init函数在初始化时被调用，所在类被错误的认为是配置类
//				}
//				id2group_config.put(i, sequences);
//			} else {
//				boolean isFilter = true;
//				for (String cc : sequences) {
//					if (cc.startsWith("url started")) {
//						isFilter = false;
//					} else if (cc.startsWith("url finished")) {
//						isFilter = true;
//					} else if (isFilter) {
//						// do not calculate the framework indirectly call
//						if (indirectlyTargets.contains(cc))
//							continue;
//						String className = cc.substring(0, cc.lastIndexOf("."));
//						// annotations marks
//						ArrayList<String> need2add = new ArrayList<String>();
//						ArrayList<String> need2add_anno = resolveMarksUtil.getAnnosMarksOnly_onclass(className,
//								c_annos);
//						if (!need2add_anno.isEmpty()) {
//							need2add.addAll(need2add_anno);
//						}
//						// inheritance marks
//						ArrayList<String> need2add_inher = resolveMarksUtil.getInheritanceMarks_onclass(className,
//								inheritanceSet);
//						if (!need2add_inher.isEmpty()) {
//							need2add.addAll(need2add_inher);
//						}
//						// xml-configured marks
//						ArrayList<String> need2add_xml = resolveMarksUtil.getXMLMarks(className, xmlSet);
//						if (!need2add_xml.isEmpty()) {
//							need2add.addAll(need2add_xml);
//						}
//
//						if (!need2add.isEmpty()) {
//							filter_class_marks.add(new HashSet<>(need2add));
//						}
//					}
//				}
//			}
//
//		}

//		HashSet<String> actualCalls = new HashSet<>();
//		for (String key : outer2Seq.keySet()) {
//			Set<ArrayList<String>> calls = outer2Seq.get(key);
//			for (ArrayList<String> seq : calls) {
//				for (String stmt : seq) {
//					if (stmt.startsWith("[callsite]") || stmt.endsWith("[end]") || stmt.startsWith("url started")
//							|| stmt.contains("url finished")) {
//						continue;
//					}
//
//					if (stmt.endsWith("[normal]")) {
//						stmt = stmt.substring(0, stmt.indexOf("[normal]"));
//					}
//
//					actualCalls.add(stmt);
//
//				}
//			}
//		}

// ------------------------------------------------------------------------------
		System.out.println();

		/** print results */
		/* 1. entry */
		HashMap<set2setPair, Integer> answer1 = entries.getEntrySet();
		/* 2. framework managed object and class */
		/* 2.1 managed class and alias */
		Set<HashSet<String>> answer2 = managedClassMarksSet.getAllElements();
		HashSet<String> answer3 = classAliasMarks;
		/* 2.2 Inject position and alias apply position */
		HashSet<String> answer4 = fieldActualTypeMarks;
		Set<HashSet<String>> answer5 = managedFieldMarksSet.getAllElements();
		Set<HashSet<String>> answer6 = managedFunctionMarksSet.getAllElements();
		/* 3 call sequence */
		HashSet<sequencePair2> answer7 = sequencePairSet;
		HashMap<sequencePair2, HashSet<String>> answer8 = seqCalculator.getConnectPointMap();
		Set<HashSet<String>> answer9 = config_class_marks.getCollection();
		Set<HashSet<String>> answer10 = filter_class_marks.getCollection();
		/* 4 indirectly invoke */
		Set<frameworkIndirectInvoke> answer11 = allIndirectFrameworkInvokeSet;

		writeAnswers writeAnswers1 = new writeAnswers();
		try {
			long beforeTime5 = System.nanoTime();
			writeAnswers1.writeEntry(answer1);
			writeAnswers1.writeManaged(answer2, answer3, answer4, answer5, answer6);
			writeAnswers1.writeSequence(answer7, answer8, answer9, answer10);
			writeAnswers1.writeFrameworkIndirectlyInvoke(answer11);

			writeAnswers1.writeAll(entries.getEntrySet(), mayEntryPointFormalParameterSet,
					managedClassMarksSet.getAllElements(), classAliasMarks, fieldActualTypeMarks,
					managedFieldMarksSet.getAllElements(), managedFunctionMarksSet.getAllElements(),
					frmwkRetAcutalClass, sequencePairSet, seqCalculator.getConnectPointMap(),
					allIndirectFrameworkInvokeSet);
			double buildTime5 = (System.nanoTime() - beforeTime5) / 1E9;
			System.out.println("[TIME-LOG] Exprot Answers Done in " + buildTime5 + " s!");
		} catch (IOException e) {
			e.printStackTrace();
		}

		double Alltime = (System.nanoTime() - beforeTime0) / 1E9;
		System.out.println("[TIME-LOG]" + " Spends " + Alltime + " s");

	}

}
