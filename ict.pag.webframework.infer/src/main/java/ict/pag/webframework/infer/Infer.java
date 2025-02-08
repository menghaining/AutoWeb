package ict.pag.webframework.infer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import ict.pag.webframework.infer.helper.GraphHelper;
import ict.pag.webframework.infer.helper.SpecialHelper;
import ict.pag.webframework.infer.marks.ClassMethodPair;
import ict.pag.webframework.log.RequestInfoExtractor;
import ict.pag.webframework.log.dynamicCG.CallsiteInfo;
import ict.pag.webframework.log.dynamicCG.DynamicCG;
import ict.pag.webframework.log.dynamicCG.DynamicCGBuilder;
import ict.pag.webframework.log.dynamicCG.DynamicCGNode;
import ict.pag.webframework.log.dynamicCG.MethodCalledType;
import ict.pag.webframework.log.util.LogFormatHepler;
import ict.pag.webframework.preInstrumental.entity.Modification;
import ict.pag.webframework.preInstrumental.entity.ModifiedInfo;
import ict.pag.webframework.preInstrumental.entity.ModifiedSemantic;

public class Infer {
	/** Answers */
	private HashSet<ClassMethodPair> entryConfigs = new HashSet<>();
	private HashMap<String, HashSet<ClassMethodPair>> indirectCallConfigs = new HashMap<>();
	private HashSet<HashSet<String>> fieldInject_onFieldConfigs = new HashSet<>();
	private HashSet<HashSet<String>> fieldInject_onMethodConfigs = new HashSet<>();
	private HashMap<String, HashSet<String>> field2TargetsConfigs = new HashMap<>();

	private HashSet<String> entrySingleClasses = new HashSet<String>();/* the class configuration that without method */

	public HashSet<ClassMethodPair> getEntryConfigs() {
		return entryConfigs;
	}

	public HashMap<String, HashSet<ClassMethodPair>> getIndirectCallConfigs() {
		return indirectCallConfigs;
	}

	public HashSet<HashSet<String>> getFieldInject_onFieldConfigs() {
		return fieldInject_onFieldConfigs;
	}

	public HashSet<HashSet<String>> getFieldInject_onMethodConfigs() {
		return fieldInject_onMethodConfigs;
	}

	public HashMap<String, HashSet<String>> getField2TargetsConfigs() {
		return field2TargetsConfigs;
	}

	public HashSet<String> getEntrySingleClasses() {
		return entrySingleClasses;
	}

	public void calculate(String runningLogsDir, String recordJson, ClassHierarchy cha, CHACallGraph chaCG, MethodCalledType logType) {
		/* 1. read json file and record */
		ArrayList<ModifiedInfo> modifiedRecord = new ArrayList<>();
		ArrayList<Integer> indexes = new ArrayList<>();
		ArrayList<String> urls = new ArrayList<>();
		readJsonFile(new File(recordJson), modifiedRecord, indexes, urls);

		/* 2. collect semantic inofs from each running log */
		HashMap<String, ClassMethodPair> stmt2Configuration_Entry = new HashMap<>();
		HashMap<String, HashMap<String, ClassMethodPair>> frameweorkInvoke2StmtConfigs = new HashMap<>();
		HashMap<String, HashSet<String>> field2InjectedConfig = new HashMap<>();
		HashMap<String, HashSet<String>> field2InjectMethodConfig = new HashMap<>();
		HashMap<String, HashMap<String, HashSet<String>>> field2InjectFieldTargetsConfig = new HashMap<>();

		for (int id = 1;; id++) {
			File dir = new File(runningLogsDir + File.separator + id);
			if (!dir.exists())
				break;
			String logPath = runningLogsDir + File.separator + id + File.separator + "catalina.txt";
			RequestInfoExtractor extractor = new RequestInfoExtractor();
			extractor.readAndParse(logPath);

			int currIndex = indexes.indexOf(id);
			ModifiedInfo info = modifiedRecord.get(currIndex);

			if (logType.equals(MethodCalledType.request)) {
				String url = urls.get(currIndex);

				ArrayList<Integer> matchedIndexes = new ArrayList<>();
				for (int i = 0; i < extractor.getUrlRequest().size(); i++) {
					String requestUrl = extractor.getUrlRequest().get(i);
					if (urlsMatch(requestUrl, url)) {
						matchedIndexes.add(i);
					}
				}

				ModifiedSemantic type = info.getType();
				// remove @2023/0201
//				if (type.equals(ModifiedSemantic.Entry) && matchedIndexes.isEmpty()) {
//					checkSemanticEntry(new ArrayList<>(), info, stmt2Configuration_Entry, logType);
//				}

				ArrayList<ArrayList<String>> allSeqs = new ArrayList<>();
				for (Integer matchedIndex : matchedIndexes) {
					ArrayList<String> seq = extractor.getSeqs().get(matchedIndex);
					allSeqs.add(seq);
				}
				switch (type.name()) {
				case "Entry":
					if (extractor.deploySuccess) {
						boolean add = true;
						HashMap<String, ClassMethodPair> tmp = new HashMap<>();
						for (ArrayList<String> seqs : allSeqs) {
							boolean ret = checkSemanticEntry(seqs, info, tmp, logType);
							if (!ret)
								add = false;
						}
						if (add && !tmp.isEmpty()) {
							for (String statement : tmp.keySet()) {
								if (stmt2Configuration_Entry.containsKey(statement)) {
									if (!tmp.get(statement).getAllMarks_class().isEmpty())
										stmt2Configuration_Entry.get(statement).getAllMarks_class().addAll(tmp.get(statement).getAllMarks_class());
									if (!tmp.get(statement).getAllMarks_mtd().isEmpty())
										stmt2Configuration_Entry.get(statement).getAllMarks_mtd().addAll(tmp.get(statement).getAllMarks_mtd());

								} else
									stmt2Configuration_Entry.put(statement, tmp.get(statement));
							}
						}
					}
					break;
				case "Field_Inject":
					boolean add2 = true;
					HashMap<String, HashSet<String>> tmp2 = new HashMap<>();
					for (ArrayList<String> seqs : allSeqs) {
						boolean ret = checkSemanticFieldInject(seqs, info, tmp2, cha, chaCG);
						if (!ret)
							add2 = false;
					}
					if (add2 && !tmp2.isEmpty()) {
						for (String f : tmp2.keySet()) {
							if (field2InjectedConfig.containsKey(f)) {
								field2InjectedConfig.get(f).addAll(tmp2.get(f));
							} else {
								field2InjectedConfig.put(f, tmp2.get(f));
							}
						}
					}
					break;
				case "Field_Inject_method":
					for (ArrayList<String> seqs : allSeqs)
						checkSemanticFieldInjectMethod(seqs, info, field2InjectMethodConfig);
					break;
				case "Field_Point2":
					for (ArrayList<String> seqs : allSeqs)
						checkSemanticPoints2(seqs, info, field2InjectFieldTargetsConfig);
					break;
				case "IndirectCall":
					for (ArrayList<String> seqs : allSeqs)
						checkSemanticIndirectCall(seqs, info, frameweorkInvoke2StmtConfigs);
					for (Integer matchedIndex : matchedIndexes) {
						ArrayList<String> seqs = extractor.getOtherSeqs().get(matchedIndex);
						checkSemanticIndirectCall(seqs, info, frameweorkInvoke2StmtConfigs);
					}
					break;
				}
			} else if (logType.equals(MethodCalledType.configuration)) {
				ModifiedSemantic type = info.getType();
				ArrayList<ArrayList<String>> sequences = extractor.getConfigsSeqs();
				if (type.equals(ModifiedSemantic.Entry) && extractor.deploySuccess) {
					if (sequences.size() > 1) {
						HashMap<String, ClassMethodPair> tmp = new HashMap<>();
						boolean add = true;
						for (ArrayList<String> seqs : sequences) {
							boolean ret = checkSemanticEntry(seqs, info, tmp, logType);
							if (!ret && info.getWay().equals(Modification.remove))
								add = false;
							if (ret && info.getWay().equals(Modification.add)) {
								add = true;
								break;
							}
						}
						if (add && !tmp.isEmpty()) {
							for (String statement : tmp.keySet()) {
								if (stmt2Configuration_Entry.containsKey(statement)) {
									if (!tmp.get(statement).getAllMarks_class().isEmpty())
										stmt2Configuration_Entry.get(statement).getAllMarks_class().addAll(tmp.get(statement).getAllMarks_class());
									if (!tmp.get(statement).getAllMarks_mtd().isEmpty())
										stmt2Configuration_Entry.get(statement).getAllMarks_mtd().addAll(tmp.get(statement).getAllMarks_mtd());

								} else
									stmt2Configuration_Entry.put(statement, tmp.get(statement));
							}
						}
					} else if (sequences.size() == 1) {
						checkSemanticEntry(sequences.get(0), info, stmt2Configuration_Entry, logType);
					} else {
						// the sequence is empty
						// 1. some exception occurs
						// 2. run normal but null
					}
				}
				for (ArrayList<String> seqs : sequences) {
//					if (type.equals(ModifiedSemantic.Entry)) {
//						checkSemanticEntry(seqs, info, stmt2Configuration_Entry, logType);
//					} else 
					if (type.equals(ModifiedSemantic.Field_Inject)) {
						checkSemanticFieldInject(seqs, info, field2InjectedConfig, cha, chaCG);
					} else if (type.equals(ModifiedSemantic.Field_Inject_method)) {
						checkSemanticFieldInjectMethod(seqs, info, field2InjectMethodConfig);
					} else if (type.equals(ModifiedSemantic.Field_Point2)) {
						checkSemanticPoints2(seqs, info, field2InjectFieldTargetsConfig);
					} else if (type.equals(ModifiedSemantic.IndirectCall)) {
						checkSemanticIndirectCall(seqs, info, frameweorkInvoke2StmtConfigs);
					}
				}
			}
		}

		/* 3. filter and check the answer */
		// 3.1 entry
		for (String stmt : stmt2Configuration_Entry.keySet()) {
			ClassMethodPair pair = stmt2Configuration_Entry.get(stmt);
			// pre: remove notEntryClass configuration
			if (!isXML(pair.getAllMarks_mtd())) {
				HashSet<String> class_configs = SpecialHelper.excludeElementsFrom(pair.getAllMarks_class(), notEntry_Class);
				pair.setAllMarks_class(class_configs);
			}

			if (pair.isAllEmpty())
				continue;

			if (!pair.getAllMarks_class().isEmpty()) {
				// 1. find inheritance
				String clazz = "L" + stmt.substring(0, stmt.lastIndexOf('.')).replace('.', '/');
				IClass givenClass = cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
				HashSet<String> superMethods = GraphHelper.findOverrideFromFramework(givenClass, stmt);
				// 2. also maybe main
				String mtdSig = stmt.substring(stmt.lastIndexOf('.') + 1);
				if (mtdSig.equals("main([Ljava/lang/String;)V"))
					superMethods.add("main([Ljava/lang/String;)V");
				if (!superMethods.isEmpty())
					pair.getAllMarks_mtd().addAll(superMethods);
			}

			if (pair.getAllMarks_mtd().isEmpty()) {
				entrySingleClasses.addAll(pair.getAllMarks_class());
				continue;
			}
			if (!SpecialHelper.contains(entryConfigs, pair))
				entryConfigs.add(pair);
		}
		// 3.2 indirect call
		for (String frameworkInvoke : frameweorkInvoke2StmtConfigs.keySet()) {
			HashSet<ClassMethodPair> configs = new HashSet<>();
			for (String stmt : frameweorkInvoke2StmtConfigs.get(frameworkInvoke).keySet()) {
				ClassMethodPair pair = frameweorkInvoke2StmtConfigs.get(frameworkInvoke).get(stmt);
				if (pair.isAllEmpty())
					continue;
				// find inheritance
				String clazz = "L" + stmt.substring(0, stmt.lastIndexOf('.')).replace('.', '/');
				IClass givenClass = cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
				HashSet<String> superMethods = GraphHelper.findOverrideFromFramework(givenClass, stmt);

				if (!superMethods.isEmpty())
					pair.getAllMarks_mtd().addAll(superMethods);
				if (pair.getAllMarks_mtd().isEmpty())
					continue;

				if (!SpecialHelper.contains(configs, pair))
					configs.add(pair);
			}
			if (!configs.isEmpty()) {
				if (!indirectCallConfigs.containsKey(frameworkInvoke))
					indirectCallConfigs.put(frameworkInvoke, new HashSet<>());
				indirectCallConfigs.get(frameworkInvoke).addAll(configs);
			}
		}
		// 3.3 field Inject
		// field-level
		for (String fSig : field2InjectedConfig.keySet()) {
			HashSet<String> configs = field2InjectedConfig.get(fSig);
			if (!configs.isEmpty()) {
				// filter configs from notInject
				HashSet<String> configs2 = SpecialHelper.excludeElementsFrom(configs, notInject);
				if (!configs2.isEmpty())
					if (!SpecialHelper.setSetContains(fieldInject_onFieldConfigs, configs2))
						fieldInject_onFieldConfigs.add(configs2);
			}
		}
		// method-level
		for (String stmt : field2InjectMethodConfig.keySet()) {
			HashSet<String> configs = field2InjectMethodConfig.get(stmt);
			if (!configs.isEmpty())
				if (!SpecialHelper.setSetContains(fieldInject_onMethodConfigs, configs))
					fieldInject_onMethodConfigs.add(configs);
		}
		// 3.4 field points to
		for (String fSig : field2InjectFieldTargetsConfig.keySet()) {
			HashMap<String, HashSet<String>> field2Tars = field2InjectFieldTargetsConfig.get(fSig);
			for (String fieldConfig : field2Tars.keySet()) {
				HashSet<String> tarConfigs = field2Tars.get(fieldConfig);
				if (!tarConfigs.isEmpty()) {
					if (!field2TargetsConfigs.containsKey(fieldConfig))
						field2TargetsConfigs.put(fieldConfig, new HashSet<>());
					field2TargetsConfigs.get(fieldConfig).addAll(tarConfigs);
				}
			}
		}

		System.out.println("...Infer done...");
	}

	private boolean isXML(HashSet<String> allMarks_mtd) {
		if (allMarks_mtd == null || allMarks_mtd.isEmpty())
			return false;
		for (String mark : allMarks_mtd) {
			if (!mark.contains("/"))
				return false;
		}
		return true;
	}

	/**
	 * return true iff urlAttr, method, queryString and param are same
	 */
	private boolean urlsMatch(String url1, String url2) {
		String uu1 = url1.substring("[ReqStart]".length());
		String uu2 = url2.substring("[ReqStart]".length());

		String urlAddr1 = uu1.substring(uu1.indexOf(']') + 1, url1.indexOf("[hashcode]"));
		String urlAddr2 = uu2.substring(uu2.indexOf(']') + 1, url2.indexOf("[hashcode]"));
		if (!urlAddr1.equals(urlAddr2))
			return false;

		String urlMtd1 = uu1.substring(uu1.indexOf("[method]") + "[method]".length(), uu1.indexOf("[queryString]"));
		String urlMtd2 = uu2.substring(uu2.indexOf("[method]") + "[method]".length(), uu2.indexOf("[queryString]"));
		if (!urlMtd1.equals(urlMtd2))
			return false;

		String urlQStr1 = uu1.substring(uu1.indexOf("[queryString]") + "[queryString]".length(), uu1.indexOf("[param]"));
		String urlQStr2 = uu2.substring(uu2.indexOf("[queryString]") + "[queryString]".length(), uu2.indexOf("[param]"));
//		if (!urlQStr1.equals(urlQStr2))
		if (!maySame(urlQStr1, urlQStr2))
			return false;

		String urlParam1 = uu1.substring(uu1.indexOf("[param]") + "[param]".length(), uu1.indexOf("[pathInfo]"));
		String urlParam2 = uu2.substring(uu2.indexOf("[param]") + "[param]".length(), uu2.indexOf("[pathInfo]"));
//		if (!urlParam1.equals(urlParam2))
		if (!maySame(urlParam1, urlParam2))
			return false;

		return true;
	}

	private boolean maySame(String p1, String p2) {
		if (p1.length() == 0 && p2.length() != 0)
			return false;
		if (p2.length() == 0 && p1.length() != 0)
			return false;
		if (p1.equals(p2))
			return true;
		if (p1.contains(p2) || p2.contains(p1))
			return true;

		return false;
	}

	private HashSet<String> notEntry_Class = new HashSet<String>();

	private boolean checkSemanticEntry(ArrayList<String> seqs, ModifiedInfo info, HashMap<String, ClassMethodPair> stmt2Configuration_Entry,
			MethodCalledType logType) {
		boolean find = false;

		String statement = info.getStatement();
		String checkbody = info.getCheckBody().get(0);
		boolean appear = info.isAppear();
		String config = info.getConfigurationContent().get(0);
		int positionTag = info.getPosition();
		Modification way = info.getWay();

		// modify@11.17: the output checkBody is the full statement of method call
//		if (way.equals(Modification.add))
//			checkbody = statement.substring(0, statement.lastIndexOf('.')) + '.' + checkbody + statement.substring(statement.indexOf('('));

		DynamicCGBuilder builder = new DynamicCGBuilder();
		builder.buildCGNodes(LogFormatHepler.removeThreadID(seqs), logType);
		DynamicCG cg = builder.getDynamicCallGraph();
		ArrayList<DynamicCGNode> entryNodes = cg.getRoots();

		boolean entry_appear = false;
		boolean isClosed = true;
		for (DynamicCGNode node : entryNodes) {
			if (node.getStmt().equals(checkbody)) {
				entry_appear = true;
				if (!node.isClosed())
					isClosed = false;
			}
		}
		// 0208: no need do this
//		if (entry_appear != appear && positionTag == 0 && way.equals(Modification.remove))
//			notEntry_Class.add(config);
		if ((entry_appear == appear) || (logType.equals(MethodCalledType.configuration) && !isClosed && !appear)) {
			find = true;
			// this configuration is critical
			if (!stmt2Configuration_Entry.containsKey(statement))
				stmt2Configuration_Entry.put(statement, new ClassMethodPair());
			ClassMethodPair mark = stmt2Configuration_Entry.get(statement);
			if (positionTag == 0) {
				// class
				mark.getAllMarks_class().add(config);
			} else if (positionTag == 1) {
				// method
				mark.getAllMarks_mtd().add(config);
			}
		}
		return find;
	}

	private void checkSemanticIndirectCall(ArrayList<String> seqs, ModifiedInfo info,
			HashMap<String, HashMap<String, ClassMethodPair>> frameweorkInvoke2StmtConfigs) {
		String statement = info.getStatement();
		String config = info.getConfigurationContent().get(0);
		boolean appear = info.isAppear();
		int positionTag = info.getPosition();
		String frameworkInvoke = info.getCheckBody().get(0);
		String invokedApp = info.getCheckBody().get(1);
		Modification way = info.getWay();

		List<String> otherInfos = null;
		if (info.getConfigurationContent().size() == 5) {
			otherInfos = info.getConfigurationContent().subList(1, 5);
		}

		// modify@11.17: the output checkBody is the full statement of method call
//		if (way.equals(Modification.add))
//			invokedApp = statement.substring(0, statement.lastIndexOf('.')) + '.' + invokedApp + statement.substring(statement.indexOf('('));

		DynamicCGBuilder builder = new DynamicCGBuilder();
		builder.buildCGNodes(LogFormatHepler.removeThreadID(seqs), MethodCalledType.request);
		DynamicCG cg = builder.getDynamicCallGraph();

		boolean add = true;
		boolean occur = false;
		for (DynamicCGNode node : cg.getNodes()) {
			for (CallsiteInfo cs : node.getCallsites()) {
				String csStmt = cs.getStmt();
				String csPureStmt0 = csStmt.substring("[callsite]".length());
				String csPureStmt1 = csPureStmt0.substring(0, csPureStmt0.indexOf(']'));
				String csPureStmt = csPureStmt1.substring(0, csPureStmt1.lastIndexOf('['));
				if (csPureStmt.equals(frameworkInvoke)) {
					occur = true;
					boolean findAppInvoke = false;
					for (DynamicCGNode targetCall : cs.getActual_targets()) {
						String targetStmt = targetCall.getStmt();
						if (targetStmt.equals(invokedApp)) {
							findAppInvoke = true;
						}
					}
					if (findAppInvoke != appear) {
						add = false;
						break;
					}
				}
			}
		}
		if (occur && add) {
			if (!frameweorkInvoke2StmtConfigs.containsKey(frameworkInvoke))
				frameweorkInvoke2StmtConfigs.put(frameworkInvoke, new HashMap<>());
			HashMap<String, ClassMethodPair> map = frameweorkInvoke2StmtConfigs.get(frameworkInvoke);
			if (!map.containsKey(statement))
				map.put(statement, new ClassMethodPair());
			ClassMethodPair pair = map.get(statement);
			if (positionTag == 0) {
				// class
				pair.getAllMarks_class().add(config);
			} else if (positionTag == 1) {
				// method
				pair.getAllMarks_mtd().add(config);
			}
			// add other informations
			if (otherInfos != null) {
				pair.addOtherInfos(otherInfos);
			}
		}
	}

	private HashSet<String> notInject = new HashSet<String>();

	private boolean checkSemanticFieldInject(ArrayList<String> seqs, ModifiedInfo info, HashMap<String, HashSet<String>> field2InjectedConfig,
			ClassHierarchy cha, CHACallGraph chaCG) {
		boolean find = false;

		String concernedFieldSig = info.getStatement(); /* means check which field */
		String config = info.getConfigurationContent().get(0);
		boolean appear = info.isAppear();
		String checkField = info.getCheckBody().get(0); /* actual check */
		Modification way = info.getWay();
		// build checkBody
		String prefix = concernedFieldSig.substring(0, concernedFieldSig.indexOf(':'));
		String fieldBelong2Class = prefix.substring(0, prefix.lastIndexOf('.'));
		if (way.equals(Modification.add)) {
			checkField = fieldBelong2Class + "." + checkField + concernedFieldSig.substring(prefix.length());
		} else if (!checkField.startsWith(fieldBelong2Class)) {
			checkField = fieldBelong2Class + "." + checkField + concernedFieldSig.substring(prefix.length());
		}
		String tmp_str1 = checkField.substring(0, checkField.indexOf(':'));
		String checkFieldName = tmp_str1.substring(tmp_str1.lastIndexOf('.') + 1);
		String concernedFieldName = prefix.substring(prefix.lastIndexOf('.') + 1);

		String fieldType = concernedFieldSig.substring(concernedFieldSig.indexOf(':') + 1);
		if (isBasicType(fieldType)) {
			return true;
		}

		// if this field has initial value, return
		String clazz = "L" + fieldBelong2Class.replace('.', '/');
		IClass givenClass = cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz)));
		if (givenClass != null) {
			for (IMethod m : givenClass.getAllMethods()) {

				if (m.isInit()) {
					if (chaCG != null) {
						CGNode node = chaCG.getNode(m, Everywhere.EVERYWHERE);
						if (node != null) {
							IR ir = node.getIR();
							if (ir != null) {
								SSAInstruction[] insts = ir.getInstructions();
								for (SSAInstruction inst : insts) {
									if (inst instanceof SSAPutInstruction) {
										SSAPutInstruction putInst = (SSAPutInstruction) inst;
										String initFieldName = putInst.getDeclaredField().getName().toString();
										if (initFieldName.equals(checkFieldName) || initFieldName.equals(concernedFieldName)) {
											// has init value
											return true;
										}
									}
								}
							}
						}
					}
				}
			}
		}

		// new version has more element in checkBody: the method the field first find
		// not null
		String wapperMtd = null;
		if (info.getCheckBody().size() > 1)
			wapperMtd = info.getCheckBody().get(1);

		boolean add = true;
		boolean occur = false;
		String baseClass = null;
		String callMtdName = null;
		String callMtdFullName = null;
		for (String line : seqs) {
			if (line.startsWith("[call method]")) {
				String tmp1 = line.substring("[call method]".length());
				String callMtd = tmp1.substring(tmp1.indexOf(']') + 1);
				callMtdName = callMtd.substring(callMtd.lastIndexOf('.') + 1, callMtd.indexOf('('));
				callMtdFullName = callMtd.substring(0, callMtd.indexOf('('));
				continue;
			}

			// restrict to all set/get current class declaring fields
			// reason: there are too many setField/getField function disturb, we want to
			// focus on the field use points
			// for getter/setter, we only concern on the field write
			boolean checkIt = true;
//			if (callMtdName.startsWith("set") || callMtdName.startsWith("get")) {
//				checkIt = false;
//			}
			if (wapperMtd == null) {
				if (callMtdName.startsWith("set") || callMtdName.startsWith("get")) {
					checkIt = false;
				}
			} else if (!wapperMtd.startsWith(callMtdFullName)) {
				// only concerned on the function that the field first occurred not null
				checkIt = false;
			}

//			if (line.startsWith("[base class]") && checkIt) {
			if (line.startsWith("[base class]")) {
				String tmp1 = line.substring("[base class]".length());
				baseClass = tmp1.substring(tmp1.indexOf(']') + 1);
				continue;
			}
			if (line.startsWith("[base field]") && checkIt) {
				String str0 = line.substring("[base field]".length());
				String str1 = str0.substring(str0.indexOf(']') + 1);
				String[] strs = str1.split(":");
				String fieldName = strs[0];
				String fieldRTType = strs[2];
				String fieldSig = baseClass + "." + fieldName + ":" + strs[1];

				if (checkField.equals(fieldSig)) {
					occur = true;
					boolean injected;
					if (!fieldRTType.equals("null"))
						injected = true;
					else
						injected = false;

					if (appear) {
						// if appear, add
						if (injected) {
							// find
							add = true;
							break;
						} else {
							add = false;
						}
					} else {
						// if all not appear, add
						if (injected) {
							// not
							add = false;
							break;
						} else {
							add = true;
						}
					}
//					if (appear != injected) {
//						add = false;
//						break;
//					}
				}
			} else if (line.startsWith("[field read]") && checkIt) {
				String fieldSig;
				String rtType;

				String fieldSig0;
				if (line.contains("[runtimeType]")) {
					fieldSig0 = line.substring(line.indexOf("[signature]") + "[signature]".length(), line.indexOf("[runtimeType]"));

					String str1 = line.substring(line.indexOf("[runtimeType]") + "[runtimeType]".length(), line.lastIndexOf("[base]"));
					if (str1.contains("[collection]"))
						rtType = str1.substring(0, str1.indexOf("[collection]"));
					else
						rtType = str1;
				} else {
					fieldSig0 = line.substring(line.indexOf("[signature]") + "[signature]".length(), line.indexOf("[fieldObject]"));
					rtType = "null";
				}
				String fieldDec = fieldSig0.substring(fieldSig0.indexOf(':') + 1);
				if (fieldDec.startsWith("L") && fieldDec.endsWith(";")) {
					String fieldDec1 = fieldDec.replace('/', '.').substring(1, fieldDec.length() - 1);
					fieldSig = fieldSig0.substring(0, fieldSig0.indexOf(':')) + ":" + fieldDec1;
				} else
					fieldSig = fieldSig0.substring(0, fieldSig0.indexOf(':')) + ":" + fieldDec;

				if (checkField.equals(fieldSig)) {
					occur = true;
					boolean injected;
					if (!rtType.equals("null"))
						injected = true;
					else
						injected = false;
//					if (appear != injected) {
//						add = false;
//						break;
//					}
					if (appear) {
						// if appear, add
						if (injected) {
							// find
							add = true;
							break;
						} else {
							add = false;
						}
					} else {
						// if all not appear, add
						if (injected) {
							// not
							add = false;
							break;
						} else {
							add = true;
						}
					}
				}
			} else if (line.startsWith("[field write]")) {
				String fieldSig;
				String rtType;

				String fieldSig0;
				if (line.contains("[runtimeType]")) {
					// write
					fieldSig0 = line.substring(line.indexOf("[signature]") + "[signature]".length(), line.indexOf("[runtimeType]"));
					rtType = line.substring(line.indexOf("[runtimeType]") + "[runtimeType]".length(), line.lastIndexOf("[base]"));
				} else {
					fieldSig0 = line.substring(line.indexOf("[signature]") + "[signature]".length(), line.indexOf("[fieldObject]"));
					rtType = "null";
				}
				String fieldDec = fieldSig0.substring(fieldSig0.indexOf(':') + 1);
				if (fieldDec.startsWith("L") && fieldDec.endsWith(";")) {
					String fieldDec1 = fieldDec.replace('/', '.').substring(1, fieldDec.length() - 1);
					fieldSig = fieldSig0.substring(0, fieldSig0.indexOf(':')) + ":" + fieldDec1;
				} else
					fieldSig = fieldSig0.substring(0, fieldSig0.indexOf(':')) + ":" + fieldDec;

				if (checkField.equals(fieldSig)) {
					occur = true;
					boolean injected;
					if (!rtType.equals("null"))
						injected = true;
					else
						injected = false;
//					if (appear != injected) {
//						add = false;
//						break;
//					}
					if (appear) {
						// if appear, add
						if (injected) {
							// find
							add = true;
							break;
						} else {
							add = false;
						}
					} else {
						// if all not appear, add
						if (injected) {
							// not
							add = false;
							break;
						} else {
							add = true;
						}
					}
				}
			}
		}

		if (occur && add) {
			find = true;
			if (!notInject.contains(config)) {
				if (!field2InjectedConfig.containsKey(concernedFieldSig))
					field2InjectedConfig.put(concernedFieldSig, new HashSet<>());
				field2InjectedConfig.get(concernedFieldSig).add(config);
			}
		} else if (!occur) {
			find = true;
		} else if (occur && !add && way.equals(Modification.remove)) {
			notInject.add(config);
		} else if (occur && !add && way.equals(Modification.add)) {
			notInject.add(config);
		}
		return find;
	}

	private boolean isBasicType(String fieldType) {
		if (fieldType.equals("int") || fieldType.equals("long") || fieldType.equals("byte") || fieldType.equals("float") || fieldType.equals("double")
				|| fieldType.equals("char"))
			return true;
		return false;
	}

	private void checkSemanticFieldInjectMethod(ArrayList<String> seqs, ModifiedInfo info, HashMap<String, HashSet<String>> field2InjectMethodConfig) {
		String injectMethod = info.getStatement();
		String config = info.getConfigurationContent().get(0);
		String checkField = info.getCheckBody().get(0);
		String checkMtd = info.getCheckBody().get(1);
		Modification way = info.getWay();
//		if (way.equals(Modification.add))
//			checkMtd = injectMethod.substring(0, injectMethod.lastIndexOf('.')) + '.' + checkMtd + injectMethod.substring(injectMethod.indexOf('('));
		Integer pos = info.getPosition();
		boolean appear = info.isAppear();

		if (pos == 0) // is on class
			return;

		DynamicCGBuilder builder = new DynamicCGBuilder();
		builder.buildCGNodes(LogFormatHepler.removeThreadID(seqs), MethodCalledType.request);
		DynamicCG cg = builder.getDynamicCallGraph();
		for (DynamicCGNode node : cg.getNodes()) {
			String stmt = node.getStmt();
			if (stmt.equals(checkMtd) && appear) {
				if (!field2InjectMethodConfig.containsKey(checkField))
					field2InjectMethodConfig.put(checkField, new HashSet<>());
				field2InjectMethodConfig.get(checkField).add(config);
			}
		}
	}

	private void checkSemanticPoints2(ArrayList<String> seqs, ModifiedInfo info,
			HashMap<String, HashMap<String, HashSet<String>>> field2InjectFieldTargetsConfig) {
		String concernedFieldSig = info.getStatement(); /* means check which field */
		String configField = info.getConfigurationContent().get(0);
		String configTarget = info.getConfigurationContent().get(1);
		boolean appear = info.isAppear();
		String checkField = info.getCheckBody().get(0); /* actual check */
		String checkTarget = info.getCheckBody().get(1);
		Modification way = info.getWay();
		if (way.equals(Modification.add)) {
			String ss1 = concernedFieldSig.substring(0, concernedFieldSig.indexOf(':'));
			checkField = ss1.substring(0, ss1.lastIndexOf('.')) + "." + checkField + ":" + checkTarget;
		}

		HashSet<String> runtimeTypes = null;
		String baseClass = null;
		for (String line : seqs) {
			if (line.startsWith("[base class]")) {
				String tmp1 = line.substring("[base class]".length());
				baseClass = tmp1.substring(tmp1.indexOf(']') + 1);
				continue;
			}
			if (line.startsWith("[base field]")) {
				String str0 = line.substring("[base field]".length());
				String str1 = str0.substring(str0.indexOf(']') + 1);
				String[] strs = str1.split(":");
				String fieldName = strs[0];
				String fieldRTType = strs[2];
				String fieldSig = baseClass + "." + fieldName + ":" + strs[1];

				if (checkField.equals(fieldSig)) {
					if (runtimeTypes == null) {
						runtimeTypes = new HashSet<>();
						if (!fieldRTType.equals("null"))
							runtimeTypes.add(fieldRTType);
					}
				}
			} else if (line.startsWith("[field read]")) {
				String fieldSig;
				String rtType;

				String fieldSig0;
				if (line.contains("[runtimeType]")) {
					fieldSig0 = line.substring(line.indexOf("[signature]") + "[signature]".length(), line.indexOf("[runtimeType]"));

					String str1 = line.substring(line.indexOf("[runtimeType]") + "[runtimeType]".length(), line.lastIndexOf("[base]"));
					if (str1.contains("[collection]"))
						rtType = str1.substring(0, str1.indexOf("[collection]"));
					else
						rtType = str1;
				} else {
					fieldSig0 = line.substring(line.indexOf("[signature]") + "[signature]".length(), line.indexOf("[fieldObject]"));
					rtType = "null";
				}
				String fieldDec = fieldSig0.substring(fieldSig0.indexOf(':') + 1);
				if (fieldDec.startsWith("L") && fieldDec.endsWith(";")) {
					String fieldDec1 = fieldDec.replace('/', '.').substring(1, fieldDec.length() - 1);
					fieldSig = fieldSig0.substring(0, fieldSig0.indexOf(':')) + ":" + fieldDec1;
				} else
					fieldSig = fieldSig0.substring(0, fieldSig0.indexOf(':')) + ":" + fieldDec;

				if (checkField.equals(fieldSig)) {
					if (runtimeTypes == null) {
						runtimeTypes = new HashSet<>();
						if (!rtType.equals("null"))
							runtimeTypes.add(rtType);
					}
				}
			} else if (line.startsWith("[field write]")) {
				String fieldSig;
				String rtType;

				String fieldSig0;
				if (line.contains("[runtimeType]")) {
					// write
					fieldSig0 = line.substring(line.indexOf("[signature]") + "[signature]".length(), line.indexOf("[runtimeType]"));
					rtType = line.substring(line.indexOf("[runtimeType]") + "[runtimeType]".length(), line.lastIndexOf("[base]"));
				} else {
					fieldSig0 = line.substring(line.indexOf("[signature]") + "[signature]".length(), line.indexOf("[fieldObject]"));
					rtType = "null";
				}
				String fieldDec = fieldSig0.substring(fieldSig0.indexOf(':') + 1);
				if (fieldDec.startsWith("L") && fieldDec.endsWith(";")) {
					String fieldDec1 = fieldDec.replace('/', '.').substring(1, fieldDec.length() - 1);
					fieldSig = fieldSig0.substring(0, fieldSig0.indexOf(':')) + ":" + fieldDec1;
				} else
					fieldSig = fieldSig0.substring(0, fieldSig0.indexOf(':')) + ":" + fieldDec;

				if (checkField.equals(fieldSig)) {
					if (runtimeTypes == null) {
						runtimeTypes = new HashSet<>();
						if (!rtType.equals("null"))
							runtimeTypes.add(rtType);
					}
				}
			}
		}

		boolean matched = false;
		if (runtimeTypes != null) {
			// injected
			for (String rtType : runtimeTypes) {
				if (rtType.contains(checkTarget) || checkTarget.contains(rtType)) {
					matched = true;
					break;
				}
			}
		}
		if (matched == appear) {
			if (!field2InjectFieldTargetsConfig.containsKey(concernedFieldSig))
				field2InjectFieldTargetsConfig.put(concernedFieldSig, new HashMap<>());
			HashMap<String, HashSet<String>> field2TargetConfigs = field2InjectFieldTargetsConfig.get(concernedFieldSig);
			if (!field2TargetConfigs.containsKey(configField))
				field2TargetConfigs.put(configField, new HashSet<>());
			field2TargetConfigs.get(configField).add(configTarget);
		}
	}

	private void readJsonFile(File recordJson, ArrayList<ModifiedInfo> modifiedRecord, ArrayList<Integer> indexes, ArrayList<String> urls) {
		try {
			String content = FileUtils.readFileToString(recordJson, "UTF-8");
			JSONArray jsonArray = new JSONArray(content);
			jsonArray.forEach(line -> {
				if (line instanceof JSONObject) {
					// 1. obtain the url and its group testcases
					JSONObject obj = (JSONObject) line;
					String uu = (String) obj.get("url");
//					String uu1 = uu.substring("[ReqStart]".length(), uu.indexOf("[hashcode]"));
//					String url = uu1.substring(uu1.indexOf(']') + 1);
					JSONArray testcasesArray = (JSONArray) obj.get("testcases");

					// 2. for each testcase
					Iterator<Object> it = testcasesArray.iterator();
					while (it.hasNext()) {
						JSONObject testcaseObj = (JSONObject) it.next();
						int index = (int) testcaseObj.get("testcase");
						String statement = (String) testcaseObj.get("statement");
						Integer position = (Integer) testcaseObj.get("position");
						String typeStr = (String) testcaseObj.get("type");
						ModifiedSemantic type = ModifiedSemantic.valueOf(typeStr);
						String wayStr = (String) testcaseObj.get("way");
						Modification way = Modification.valueOf(wayStr);
						JSONArray confContentArray = (JSONArray) testcaseObj.get("configurationContent");
						JSONObject contentObj = (JSONObject) confContentArray.get(0);
						ArrayList<String> configurationContent = new ArrayList<>();
						if (contentObj.keySet().size() > 2 && type.equals(ModifiedSemantic.IndirectCall)) {
							if (contentObj.keySet().contains("class"))
								configurationContent.add(contentObj.getString("class"));
							else
								configurationContent.add(contentObj.getString("method"));
							if (contentObj.keySet().contains("caller_class"))
								configurationContent.add(contentObj.getString("caller_class"));
							else
								configurationContent.add("null");
							if (contentObj.keySet().contains("caller_method"))
								configurationContent.add(contentObj.getString("caller_method"));
							else
								configurationContent.add("null");
							if (contentObj.keySet().contains("target_class"))
								configurationContent.add(contentObj.getString("target_class"));
							else
								configurationContent.add("null");
							if (contentObj.keySet().contains("target_method"))
								configurationContent.add(contentObj.getString("target_method"));
							else
								configurationContent.add("null");
						} else {
							// deal as old
							for (String key : contentObj.keySet()) {
								if (key.equals("target")) {
									configurationContent.add(contentObj.getString(key));
								} else {
									configurationContent.add(0, contentObj.getString(key));
								}
							}
						}
						JSONArray checkBodyArray = (JSONArray) testcaseObj.get("checkBody");
						JSONObject checkObj = (JSONObject) checkBodyArray.get(0);
						ArrayList<String> checkBody = new ArrayList<>();
						for (String key : checkObj.keySet()) {
							if (key.equals("target")) {
								checkBody.add(checkObj.getString(key));
							} else {
								checkBody.add(0, checkObj.getString(key));
							}
						}
						boolean appear = (boolean) testcaseObj.get("appear");

						ModifiedInfo info = new ModifiedInfo(statement, position, type, way, configurationContent, checkBody, appear);
						modifiedRecord.add(info);
						indexes.add(index);

						Object triggerurl = testcaseObj.get("trigger");
						if (triggerurl == null)
							urls.add(uu);
						else
							urls.add((String) triggerurl);
					}
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
