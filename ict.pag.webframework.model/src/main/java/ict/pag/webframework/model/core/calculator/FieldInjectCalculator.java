package ict.pag.webframework.model.core.calculator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.dom4j.Element;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.annotations.Annotation;

import ict.pag.webframework.model.enumeration.MarkScope;
import ict.pag.webframework.model.log.Callsite2CallSeqMapTool;
import ict.pag.webframework.model.marks.MarksHelper;
import ict.pag.webframework.model.marks.NormalMark;
import ict.pag.webframework.model.marks.ResolveMarks;
import ict.pag.webframework.model.option.SpecialHelper;

//TODO : collection info parser
public class FieldInjectCalculator {
	/** Initialize */
	private ClassHierarchy cha;
	private CHACallGraph chaCG;
	private HashMap<String, HashSet<Element>> class2XMLEle;
	private HashMap<Integer, ArrayList<String>> id2group;

	/* each group record the base object information context when running */
	/* <callStmt, <field, {actual types}>> */
	private HashMap<String, HashMap<String, HashSet<String>>> runtimeMtdCallsContexts;
	private HashMap<IField, HashSet<String>> runtimeMtdCallsContexts_noctx = new HashMap<>();

	/* internal result */
	/* these fields not the finnal results */
	private HashMap<IField, HashSet<String>> mayInjectFields = new HashMap<>();
	/* injected field may be injected by methods */
	HashMap<FieldReference, HashSet<IMethod>> field2PutFieldMtd = new HashMap<>();
	private HashSet<FieldReference> usedFieldSet = new HashSet<>();

	/** final answer: the field need to inject and the injected values */
	private HashMap<IField, HashSet<String>> injectedField2Tars = new HashMap<>();
	/** The field not framework Injected */
	private HashSet<IField> notInjectFields = new HashSet<>();

	private HashSet<NormalMark> fieldInjectMarks = new HashSet<>();
	private HashSet<NormalMark> fieldNOTInjectMarks = new HashSet<>();

	public FieldInjectCalculator(ClassHierarchy cha, CHACallGraph chaCG, HashMap<String, HashSet<Element>> class2xmlEle,
			HashMap<Integer, ArrayList<String>> id2group) {
		this.cha = cha;
		this.chaCG = chaCG;
		this.class2XMLEle = class2xmlEle;
		this.id2group = id2group;
	}

	public HashSet<NormalMark> getFieldInjectMarks() {
		return fieldInjectMarks;
	}

	public HashSet<NormalMark> getFieldNOTInjectMarks() {
		return fieldNOTInjectMarks;
	}

	public void calculate(Callsite2CallSeqMapTool tool) {
		// 0. prepare field infos: a. calculate all fields context
		getFieldDec2Actuals();
		// 0. prepare field infos: b. extract infos
		extractFieldInfos();
		// 1. calculate inject and inject targets
		HashMap<FieldReference, String> fieldRef2nonDecTars_extra = tool.getFieldRef2target_app();
		if (tool.getFieldRef2target_app_same() != null && !tool.getFieldRef2target_app_same().isEmpty())
			fieldRef2nonDecTars_extra.putAll(tool.getFieldRef2target_app_same());
		calculateAllInjectFieldRef(fieldRef2nonDecTars_extra);
		// 2. extract all marks
		calculateMarks();
	}

	private void calculateMarks() {
		// 1. inject
		for (IField f : injectedField2Tars.keySet()) {
			if (!usedFieldSet.contains(f.getReference()))
				continue;

			if (field2PutFieldMtd.containsKey(f.getReference())) {
				HashSet<IMethod> initMtds = field2PutFieldMtd.get(f.getReference());
				// all putFields method has no caller
				for (IMethod mtd : initMtds) {
					HashSet<String> marks = new HashSet<>();
					HashSet<Annotation> annos = ResolveMarks.resolve_Annotation(mtd, MarkScope.Method);
					for (Annotation anno : annos) {
						marks.add("[method][anno]"
								+ SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
					}
					HashSet<String> xmlEles = ResolveMarks.resolve_XML_String(mtd, MarkScope.Method, class2XMLEle);
					for (String ele : xmlEles) {
						marks.add("[method][xml]" + ele);
					}
					if (!marks.isEmpty()) {
						NormalMark nor = new NormalMark(marks, MarkScope.Method);
						fieldInjectMarks.add(nor);
					}
				}
			}

			// marks on fields
			HashSet<String> marks = new HashSet<>();
			HashSet<Annotation> annos = ResolveMarks.resolve_Annotation(f, MarkScope.Field);
			for (Annotation anno : annos) {
				marks.add("[field][anno]" + SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
			}
			HashSet<String> xmlEles = ResolveMarks.resolve_XML_String(f, MarkScope.Field, class2XMLEle);
			for (String ele : xmlEles) {
				marks.add("[field][xml]" + ele);
			}
			if (!marks.isEmpty()) {
				NormalMark nor = new NormalMark(marks, MarkScope.Field);
				fieldInjectMarks.add(nor);
			}
		}

		// 2. not inject
		for (IField f : notInjectFields) {
			// marks on fields
			HashSet<String> marks = new HashSet<>();
			HashSet<Annotation> annos = ResolveMarks.resolve_Annotation(f, MarkScope.Field);
			for (Annotation anno : annos) {
				marks.add("[field][anno]" + SpecialHelper.formatSignature(MarksHelper.resolveAnnotationName(anno)));
			}
			HashSet<String> xmlEles = ResolveMarks.resolve_XML_String(f, MarkScope.Field, class2XMLEle);
			for (String ele : xmlEles) {
				marks.add("[field][xml]" + ele);
			}
			if (!marks.isEmpty()) {
				NormalMark nor = new NormalMark(marks, MarkScope.Field);
				fieldNOTInjectMarks.add(nor);
			}
		}

	}

	/**
	 * The field that framework inject object into it</br>
	 * 1. all methods putField within, have no caller 2. runtime info is not null
	 */
	private void calculateAllInjectFieldRef(HashMap<FieldReference, String> fieldRef2nonDecTars_extra) {
		HashSet<FieldReference> fieldNotInject = new HashSet<>();
		chaCG.forEach(cgNode -> {
			if (cgNode == null || cgNode.getMethod() == null)
				return;
			IMethod mtd = cgNode.getMethod();
			// reason for: only concerns about the function may initialize fields
			if (!mtd.isInit() || !mtd.isClinit() || !isSetFieldFunction(mtd))
				return;

			if (cgNode.getMethod().getDeclaringClass().getClassLoader().getReference()
					.equals(ClassLoaderReference.Application)) {
				Integer preCount = 0;
				Iterator<CGNode> it = chaCG.getPredNodes(cgNode);
				while (it.hasNext()) {
					CGNode pre = it.next();
					if (pre.getMethod().getDeclaringClass().getClassLoader().getReference()
							.equals(ClassLoaderReference.Application)) {
						preCount++;
					}
				}

				IR ir = cgNode.getIR();
				if (ir != null) {
					SSAInstruction[] insts = ir.getInstructions();
					for (SSAInstruction inst : insts) {
						if (inst instanceof SSAPutInstruction) {
							SSAPutInstruction putInst = (SSAPutInstruction) inst;
							FieldReference initField = putInst.getDeclaredField();
							if (preCount == 0) {
								if (field2PutFieldMtd.containsKey(initField)) {
									field2PutFieldMtd.get(initField).add(cgNode.getMethod());
								} else {
									HashSet<IMethod> tmp = new HashSet<>();
									tmp.add(cgNode.getMethod());
									field2PutFieldMtd.put(initField, tmp);
								}
							} else {
								fieldNotInject.add(initField);
							}
						} else if (inst instanceof SSAGetInstruction) {
							// use field from two methods:
							// 1. getField
							// 2. call getFieldMethod

							String mtdName = cgNode.getMethod().getName().toString();
							if (mtdName.startsWith("get") && preCount == 0)
								continue;

							SSAGetInstruction getInst = (SSAGetInstruction) inst;
							FieldReference field = getInst.getDeclaredField();
							usedFieldSet.add(field);
						}
					}
				}
			}
		});

		// extract the fields that actually need to inject
		// and the injected objects are what
		for (IField f : mayInjectFields.keySet()) {
			FieldReference ref = f.getReference();
			if (!fieldNotInject.contains(ref)) {
				HashSet<String> targets = new HashSet<>();
				if (fieldRef2nonDecTars_extra.containsKey(ref)) {
					String tar = fieldRef2nonDecTars_extra.get(ref).substring(0,
							fieldRef2nonDecTars_extra.get(ref).lastIndexOf('.'));
					targets.add(tar);
				}
				targets.addAll(mayInjectFields.get(f));
				injectedField2Tars.put(f, targets);
			} else {
				if (field2PutFieldMtd.containsKey(ref))
					field2PutFieldMtd.remove(ref);
			}
		}

	}

	private boolean isSetFieldFunction(IMethod mtd) {
		if (mtd.getName().toString().toLowerCase().startsWith("set"))
			return true;
		return false;
	}

	/**
	 * remove the callsite of field runtime information</br>
	 * only use field as key, and its actual as values
	 */
	private void extractFieldInfos() {
		if (runtimeMtdCallsContexts == null) {
			System.err.println("[ERROR] null runtimeMtdCallsContexts!");
		} else {
			cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(clazz -> {
				Collection<IField> allFields = clazz.getDeclaredInstanceFields();

				for (IMethod m : clazz.getDeclaredMethods()) {
					String mtdSig = m.getSignature();
					if (runtimeMtdCallsContexts.containsKey(mtdSig)) {
						HashMap<String, HashSet<String>> runtimeField2Actuals = runtimeMtdCallsContexts.get(mtdSig);
						for (String field : runtimeField2Actuals.keySet()) {
							if (field.equals("this"))
								continue;
							IField f = getBelong2(field, allFields);
							if (f != null) {
								HashSet<String> actuals = runtimeField2Actuals.get(field);
								if (runtimeMtdCallsContexts_noctx.containsKey(f)) {
									runtimeMtdCallsContexts_noctx.get(f).addAll(actuals);
								} else {
									runtimeMtdCallsContexts_noctx.put(f, actuals);
								}
							}
						}
					}
				}
			});

			for (IField key : runtimeMtdCallsContexts_noctx.keySet()) {
				HashSet<String> tos = new HashSet<>();
				for (String actual : runtimeMtdCallsContexts_noctx.get(key)) {
					if (!actual.equals("null")) {
						tos.add(actual);
					}
				}
				if (tos.isEmpty()) {
					// runtime always null
					notInjectFields.add(key);
				} else {
					mayInjectFields.put(key, tos);
				}
			}
		}

	}

	private IField getBelong2(String field, Collection<IField> allFields) {
		String declare0 = field.substring(0, field.lastIndexOf('.'));
		String name0 = field.substring(declare0.length() + 1);

		for (IField f : allFields) {
			String declare = SpecialHelper.formatSignature(f.getReference().getFieldType().getName().toString());
			String name = f.getReference().getName().toString();
			if (name.equals(name0) && declare.equals(declare0))
				return f;
		}
		return null;
	}

	/** @return field declare type and actual types */
	public HashMap<String, HashMap<String, HashSet<String>>> getFieldDec2Actuals() {
		if (runtimeMtdCallsContexts != null)
			return runtimeMtdCallsContexts;

		runtimeMtdCallsContexts = new HashMap<>();
		for (Integer id : id2group.keySet()) {
			ArrayList<String> sequence = id2group.get(id);

			for (int i = 0; i < sequence.size();) {
				String stmt = sequence.get(i);

				if (stmt.startsWith("[base ") || stmt.startsWith("[callsite]") || stmt.endsWith("[end]")
						|| stmt.startsWith("url started") || stmt.contains("url finished")) {
					i++;
					continue;
				} else {
					// call start
					HashMap<String, HashSet<String>> field2Actuals;
					if (runtimeMtdCallsContexts.keySet().contains(stmt)) {
						field2Actuals = runtimeMtdCallsContexts.get(stmt);
					} else {
						field2Actuals = new HashMap<>();
						runtimeMtdCallsContexts.put(stmt, field2Actuals);
					}

					int j = i + 1;
					for (; j < sequence.size(); j++) {
						String curr = sequence.get(j);
						if (curr.startsWith("[base class]")) {
							// this = actual base class
							if (field2Actuals.keySet().contains("this")) {
								field2Actuals.get("this").add(curr.substring("[base class]".length()));
							} else {
								HashSet<String> tmp = new HashSet<>();
								tmp.add(curr.substring("[base class]".length()));
								field2Actuals.put("this", tmp);
							}
						} else if (curr.startsWith("[base field]")) {
							String curr0 = curr.substring("[base field]".length()).trim();
							String[] splits = curr0.split(":");
							if (splits.length != 3) {
								System.err.println("[ERROR in log parser - field]");
								j++;
								break;
							}

							String field = splits[1] + "." + splits[0];
							String actual = splits[2];

							if (field2Actuals.keySet().contains(field)) {
								field2Actuals.get(field).add(actual);
							} else {
								HashSet<String> tmp = new HashSet<>();
								tmp.add(actual);
								field2Actuals.put(field, tmp);
							}

						} else {
							break;
						}
					}
					i = j;
				}
			}
		}
		return runtimeMtdCallsContexts;
	}

}
