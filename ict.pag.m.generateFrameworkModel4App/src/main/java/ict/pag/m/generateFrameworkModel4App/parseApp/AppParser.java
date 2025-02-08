package ict.pag.m.generateFrameworkModel4App.parseApp;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;

import ict.pag.m.frameworkInfoUtil.customize.GraphBuilder;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.AnnotationExtractor;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.BeforeAfterCalculator;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.EntryCalculator;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.FieldPoints2Calculator;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.IndirectlyCallCalculator;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.InheritanceExtractor;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.XMLExtractor;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.FieldPoints2Entity;
import ict.pag.m.generateFrameworkModel4App.entity.FrameworkModel;
import ict.pag.m.generateFrameworkModel4App.myGraphBuilder.myCGBuilder;
import ict.pag.m.marks2SAInfer.summarizeModels.CallsiteM2targetM;
import ict.pag.m.marks2SAInfer.util.FileUtil;
import ict.pag.m.marks2SAInfer.util.structual.FrmkCallParamType;
import ict.pag.m.marks2SAInfer.util.structual.FrmkRetPoints2;
import ict.pag.m.marks2SAInfer.util.structual.set2setPair;

public class AppParser {
	private String app_path;
	private String framework_path;

	private FrameworkModel frameworkModel = new FrameworkModel();

	int concreteAppMtds;

	public AppParser(String framework_path, String path) {

		this.framework_path = framework_path;
		this.app_path = path;

		System.out.println("......[Dealing With] " + this.app_path);
		/** 1. extract framework model from file */
		parseFrameworkModle_fromJsonFile();

		/** 2. extract analysis-level concepts from application */
		// 2.1 cha and chaCG
		GraphBuilder builder = new GraphBuilder(app_path, false);
		ClassHierarchy cha = builder.getCHA();
		concreteAppMtds = builder.getConcreteApplicationMethodNumber();

		CHACallGraph chaCG = builder.getAppCHACG();
		ExplicitCallGraph cgWithU = builder.calCGwithU();
		if (cgWithU != null) {
			System.out.println("[Info][Unreachable][CG][0-CFA]");
			evaluating(cgWithU);
			System.out.println("[Info][Unreachable][CG][0-CFA]");
		}

//		cha.forEach(node -> {
//			if (node.toString().contains("PluginFileUtils")) {
//				Collection<? extends IMethod> mm = node.getDeclaredMethods();
//				mm.toArray();
//			}
//		});

		/* the methods even do not have caller in chaCG */
//		chaCG.forEach(node -> {
//			IR ir = node.getIR();
//			if (node.toString().contains("fakeRootMethod")) {
//				chaCG.getSuccNodes(node).forEachRemaining(n -> {
//					System.out.println();
//				});
//				System.out.println();
//
//			}
//
//			if (node.getMethod().getDeclaringClass().getClassLoader().getReference()
//					.equals(ClassLoaderReference.Application)) {
//
//				if (node.getMethod().getSignature().toString().contains("ParameterParser")) {
//					chaCG.getPredNodes(node).forEachRemaining(pre -> {
//						System.out.println(pre);
//					});
//				}
////				if (chaCG.getPredNodeCount(node) == 0)
////					System.out.println(node.getMethod().getSignature());
//			}
//		});

////		cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(node -> {
////			chaCG.node.iterateCallSites().forEachRemaining(callsite -> {
////				chaCG.getPossibleTargets(node, callsite).forEach(callee -> {
////					zeroIncomingMap.put(callee, false);
////				});
////			});
////		});

		HashSet<String> allMethodSignatures_original = builder.getAllMtdSigs();

		// 2.2 application marks extract
		// 2.2.1 annotation
		AnnotationExtractor annoExtractor = new AnnotationExtractor();
		annoExtractor.extract(cha);
		annoExtractor.getMark2classSet();
		// 2.2.2 inheritance
		InheritanceExtractor inheExtractor = new InheritanceExtractor();
		inheExtractor.extract(cha);
		// 2.2.3 xml
		List<String> XMLs = FileUtil.getAllConcernedXMLs(path);
		XMLExtractor xmlExtractor = new XMLExtractor();
		xmlExtractor.extract(XMLs);

		/** 3. match the application marks to framework model */
		/* 3.1 entry points */
		EntryCalculator entryCal = new EntryCalculator(cha, frameworkModel, annoExtractor, inheExtractor, xmlExtractor);
		entryCal.matches();
		List<String> entrieSigs = entryCal.getEntriesSignatures();
		System.out.println("[RESULT][collect entry-points total] : " + entryCal.getEntryCount());
//		System.out.println(entryCal.getClassesAllCount());

		/* 3.2 field points to */
		FieldPoints2Calculator points2 = new FieldPoints2Calculator(cha, frameworkModel, annoExtractor, xmlExtractor,
				inheExtractor);
		points2.matches();
		HashSet<FieldPoints2Entity> field2targets = points2.getFieldPoints2targetSet();
		System.out.println("[RESULT][collect references need to inject total] : " + points2.getInjectObjectsNumber());

		/* 3.3 before after */
		BeforeAfterCalculator order = new BeforeAfterCalculator(cha, chaCG, frameworkModel, annoExtractor, xmlExtractor,
				inheExtractor);
		order.matches();
		System.out.println("[RESULT][collect possible sequential pair total] : " + order.getSequencePairscount());

		/* 4.4 indirectly call */
		IndirectlyCallCalculator indirectlyCall = new IndirectlyCallCalculator(cha, chaCG, frameworkModel,
				annoExtractor, xmlExtractor, inheExtractor);
		indirectlyCall.matches();
		System.out.println(
				"[RESULT][collect possible indirectly calls total] : " + indirectlyCall.getIndirectlyCallsCount());

		/** 4. building call graph using extra information provided by framework */
		myCGBuilder mybuilder = new myCGBuilder(app_path, entrieSigs, field2targets);
		if (mybuilder.getBuilder() != null) {
//			// tmp add
//			ExplicitCallGraph cg = mybuilder.getBuilder().getCallGraph();
//			cg.forEach(node -> {
//				IR ir = node.getIR();
//				if (node.toString().contains("fakeRootMethod")) {
//					cg.getSuccNodes(node).forEachRemaining(n -> {
//						System.out.println();
//					});
//					System.out.println();
//
//				}
//
//				if (node.getMethod().getDeclaringClass().getClassLoader().getReference()
//						.equals(ClassLoaderReference.Application)) {
//
//					if (node.getMethod().getSignature().toString().contains("ParameterParser")) {
//						cg.getPredNodes(node).forEachRemaining(pre -> {
//							System.out.println(pre);
//						});
//					}
////					if (chaCG.getPredNodeCount(node) == 0)
////						System.out.println(node.getMethod().getSignature());
//				}
//			});

//			Collection<CGNode> ens = cg.getEntrypointNodes();
//			System.out.println(cg.getNumberOfNodes());
//			chaCG.forEach(n -> {
//				if (n.getMethod().getSignature().equals(
//						"org.owasp.webgoat.session.Course.getClassFile(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")) {
////					n.setGraphNodeId(cg.getMaxNumber() + 1);
////					cg.addNode(n);
//					try {
//						cg.findOrCreateNode(n.getMethod(), Everywhere.EVERYWHERE);
//					} catch (CancelException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//			});
//			System.out.println(cg.getNumberOfNodes());
			HashSet<String> allAppMtdSigs = evaluating(mybuilder.getBuilder().getCallGraph());
			unreachableAppMethodDetails(allMethodSignatures_original, allAppMtdSigs);
		}
	}

	private void unreachableAppMethodDetails(HashSet<String> allMethodSignatures_original,
			HashSet<String> allAppMtdSigs) {
		System.out.println("[info][unreachable app methods details]...");
		int less = 0;
		for (String original : allMethodSignatures_original) {
			if (!allAppMtdSigs.contains(original)) {
				less++;
				System.out.println("[info][unreachable app method] " + original);
			}
		}
		System.out.println("[info][unreachable app methods count] " + less);
	}

	/**
	 * Evaluating Results
	 */
	private HashSet<String> evaluating(ExplicitCallGraph cg) {
		HashSet<String> allAppMtdSigs = new HashSet<>();

		/* app information */
		int edgeNumber_app = 0;
		int nodeNumber_app = 0;
		HashSet<IClass> reachedClasses_app = new HashSet<>();
		HashSet<String> reachedClassesNames_app = new HashSet<>();
		int abstractMethod_app = 0;
		/* total information */
		int edgeNumber_total = 0;
		int nodeNumber_total = 0;

		Iterator<CGNode> nodeIterator = cg.iterator();
		while (nodeIterator.hasNext()) {
			CGNode node = nodeIterator.next();
			// app
			if (node.getMethod().getDeclaringClass().getClassLoader().getReference()
					.equals(ClassLoaderReference.Application)) {
//				System.out.println(node.getMethod().getSignature());
				nodeNumber_app++;
				edgeNumber_app = edgeNumber_app + cg.getEdgeManager().getSuccNodeNumbers(node).size();
				reachedClasses_app.add(node.getMethod().getDeclaringClass());
				reachedClassesNames_app.add(node.getMethod().getDeclaringClass().getReference().toString());
				allAppMtdSigs.add(node.getMethod().getSignature());
				if (node.getMethod().isAbstract())
					abstractMethod_app++;
			}
			// total
			nodeNumber_total++;
			edgeNumber_total = edgeNumber_total + cg.getEdgeManager().getSuccNodeNumbers(node).size();

		}
		System.out.println("[RESULT][total methods ] : " + nodeNumber_total);
		System.out.println("[RESULT][total cg edges ] : " + edgeNumber_total);
		System.out.println("[RESULT][app reachable methods ] : " + nodeNumber_app);
		System.out.println("[RESULT][app cg edges ] : " + edgeNumber_app);
		System.out.println("[RESULT][app reachable classes ] : " + reachedClasses_app.size());
		System.out.println("[RESULT][% of app reachable methods over app concerte methods] : "
				+ 100 * (((double) nodeNumber_app / (double) concreteAppMtds)) + "%");

		int interfaceClass = 0;
		int abstractClass = 0;
		int may = 0;
		for (IClass c : reachedClasses_app) {
			if (c.isInterface())
				interfaceClass++;
			if (c.isAbstract())
				abstractClass++;
			may = may + c.getDeclaredMethods().size();
		}
		System.out
				.println("[info][in reachable classes][interface] " + interfaceClass + " [abstract] " + abstractClass);
		System.out.println("[info][in reachable methods][abstract] " + abstractMethod_app);
		System.out.println(may);

//		cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(cn -> {
//
//			if (!reachedClassesNames.contains(cn.getReference().toString()))
//				System.out.println(cn.getReference().toString());
//		});

		return allAppMtdSigs;
	}

	private void parseFrameworkModle_fromJsonFile() {
		File file_p = new File(framework_path);
		try {
			String content = FileUtils.readFileToString(file_p, "UTF-8");
			JSONArray jsonArray = new JSONArray(content);
			jsonArray.forEach(line -> {
				if (line instanceof JSONObject) {
					JSONObject obj = (JSONObject) line;
					String kind = (String) obj.get("kind");
					switch (kind) {
					case "ENTRY":
						processEntry(obj);
						break;
					case "ENTRY-Param":
						processEntryParam(obj);
						break;
					case "MANAGED-CLASS":
						processManagedClass(obj);
						break;
					case "MANAGED-CLASS-Alias":
						processClassAlias(obj);
						break;
					case "MANAGED-Actual-Class":
						processObjectActualClass(obj);
						break;
					case "MANAGED-Return-Class":
						precessFrameworkReturnActualClass(obj);
						break;
					case "MANAGED-FIELD-Inject":
						processInjectField(obj);
						break;
					case "ORDERED":
						processSequence(obj);
						break;
					case "IndirectInvoke":
						processIndirectlyCall(obj);
						break;
					default:
						break;
					}
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void processEntryParam(JSONObject obj) {
		String mark = (String) obj.get("classMarks");
		frameworkModel.addEntryParams(mark);
	}

	private void processIndirectlyCall(JSONObject obj) {
		String frameworkInvoke = (String) obj.get("frameworkCall");
		String target = (String) obj.get("target");

		JSONArray array1 = (JSONArray) obj.get("callContext");
		HashSet<String> callContext = new HashSet<>();
		if (array1.get(0) instanceof String) {
			callContext.add(array1.get(0).toString());
		} else {
			((JSONArray) array1.get(0)).forEach(node -> {
				callContext.add(node.toString());
			});
		}
		JSONArray array2 = (JSONArray) obj.get("targetMarks");
		HashSet<String> targetMarks = new HashSet<>();
		if (array2.get(0) instanceof String) {
			targetMarks.add(array2.get(0).toString());
		} else {
			((JSONArray) array2.get(0)).forEach(node -> {
				targetMarks.add(node.toString());
			});
		}

		CallsiteM2targetM c2t = new CallsiteM2targetM(frameworkInvoke, target, callContext, targetMarks);
		frameworkModel.addIndirectCalls(c2t);

	}

	private void processSequence(JSONObject obj) {
		JSONArray array1 = (JSONArray) obj.get("before");

		HashSet<String> before = new HashSet<>();
		array1.forEach(node -> {
			before.add(node.toString());
		});
		JSONArray array2 = (JSONArray) obj.get("after");

		HashSet<String> after = new HashSet<>();
		array2.forEach(node -> {
			after.add(node.toString());
		});

		frameworkModel.addCallSequence(new set2setPair(before, after));

	}

	private void processInjectField(JSONObject obj) {
		JSONArray array1 = (JSONArray) obj.get("marks");
		HashSet<String> inject = new HashSet<>();
		array1.forEach(node -> {
			inject.add(node.toString());
		});

		frameworkModel.addFieldsInject(inject);
	}

	private void precessFrameworkReturnActualClass(JSONObject obj) {
		String frameworkInvoke = (String) obj.get("frameworkCall");
		String type = (String) obj.get("type");
		int parameterIndex = (int) obj.get("parameterIndex");
		switch (type) {
		case "FQName_text":
			FrmkRetPoints2 ret1 = new FrmkRetPoints2(frameworkInvoke, parameterIndex, FrmkCallParamType.FQName_text);
			frameworkModel.addFrmwkRetAcutalClass(ret1);
			break;
		case "FQName_class":
			FrmkRetPoints2 ret2 = new FrmkRetPoints2(frameworkInvoke, parameterIndex, FrmkCallParamType.FQName_class);
			frameworkModel.addFrmwkRetAcutalClass(ret2);
			break;
		case "Alias_text":
			FrmkRetPoints2 ret3 = new FrmkRetPoints2(frameworkInvoke, parameterIndex, FrmkCallParamType.Alias_text);
			frameworkModel.addFrmwkRetAcutalClass(ret3);
			break;
		}

	}

	private void processObjectActualClass(JSONObject obj) {
		String mark = (String) obj.get("mark");
		frameworkModel.addPoints2Alias(mark);
	}

	private void processClassAlias(JSONObject obj) {
		String mark = (String) obj.get("mark");
		frameworkModel.addClassAliasMark(mark);
	}

	private void processManagedClass(JSONObject obj) {
		JSONArray array1 = (JSONArray) obj.get("classMarks");
		HashSet<String> obj_class = new HashSet<>();
		array1.forEach(node -> {
			obj_class.add(node.toString());
		});

		frameworkModel.addManagedClass(obj_class);
	}

	private void processEntry(JSONObject obj) {
		JSONArray array1 = (JSONArray) obj.get("classMarks");
		JSONArray array2 = (JSONArray) obj.get("methodMarks");

		HashSet<String> obj_class = new HashSet<>();
		array1.forEach(node -> {
			obj_class.add(node.toString());
		});
		HashSet<String> obj_mtd = new HashSet<>();
		array2.forEach(node -> {
			obj_mtd.add(node.toString());
		});

		frameworkModel.addEntry(new set2setPair(obj_class, obj_mtd));
	}

}
