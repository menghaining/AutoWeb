package ict.pag.m.marks2SAInfer.answersPrinter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import ict.pag.m.frameworkInfoUtil.customize.ConfigUtil;
import ict.pag.m.marks2SAInfer.summarizeModels.CallsiteM2targetM;
import ict.pag.m.marks2SAInfer.util.structual.FrmkCallParamType;
import ict.pag.m.marks2SAInfer.util.structual.FrmkRetPoints2;
import ict.pag.m.marks2SAInfer.util.structual.frameworkIndirectInvoke;
import ict.pag.m.marks2SAInfer.util.structual.sequencePair2;
import ict.pag.m.marks2SAInfer.util.structual.set2setPair;
import ict.pag.m.marks2SAInfer.util.structual.str2Set;

public class writeAnswers {
	private String root;

	public writeAnswers() {
		String subPath = ConfigUtil.appKind;

		String userDir = System.getProperty("user.dir");
		if (!userDir.endsWith(File.separator)) {
			userDir = userDir + File.separator;
		}
		if (!subPath.equals("") && subPath.length() > 0) {
			root = userDir + "result" + File.separator + subPath + File.separator;
		} else {
			root = userDir + "result" + File.separator;
		}
		File dir = new File(root);
		if (!dir.exists())
			dir.mkdirs();
	}

	public void writeEntry(HashMap<set2setPair, Integer> answers1) throws IOException {

		File file = new File(root + "Entry.json");
		System.out.println("ANSWERS-ENTRY write to " + file);
		if (!file.exists()) {
			file.createNewFile();
		}

		JSONArray jsonArray = new JSONArray();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "utf-8");
		BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

		for (set2setPair ans : answers1.keySet()) {
			Set<String> classMarks = ans.getFirst();
			Set<String> methodMarks = ans.getSecond();

			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("kind", "ENTRY");
			jsonObject.put("classMarks", classMarks);
			jsonObject.put("methodMarks", methodMarks);
			jsonArray.put(jsonObject);

		}

		String jsonString = jsonArray.toString();
		bufferedWriter.write(jsonString);
		bufferedWriter.flush();
		bufferedWriter.close();

	}

	public void writeManaged(Set<HashSet<String>> answer2, HashSet<String> answer3, HashSet<String> answer4,
			Set<HashSet<String>> answer5, Set<HashSet<String>> answer6) throws IOException {
		File file = new File(root + "Managed.json");
		System.out.println("ANSWERS-MANAGED write to " + file);
		if (!file.exists()) {
			file.createNewFile();
		}

		JSONArray jsonArray = new JSONArray();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "utf-8");
		BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

		// 1. managed class
		for (HashSet<String> ans : answer2) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("kind", "MANAGED-CLASS");
			jsonObject.put("class", ans);
			jsonArray.put(jsonObject);
		}

		// 2. class alias
		for (String ans : answer3) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("kind", "MANAGED-CLASS_alias");
			jsonObject.put("class", ans);
			jsonArray.put(jsonObject);
		}

		// 3. alias apply
		for (String ans : answer4) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("kind", "MANAGED-APPLY_alias");
			jsonObject.put("field", ans);
			jsonArray.put(jsonObject);
		}

		// 4. Inject field
		for (HashSet<String> ans : answer5) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("kind", "MANAGED-FIELD-INJECT");
			jsonObject.put("field", ans);
			jsonArray.put(jsonObject);
		}

		// 5. Inject method
		for (HashSet<String> ans : answer6) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("kind", "MANAGED-METHOD-INJECT");
			jsonObject.put("method", ans);
			jsonArray.put(jsonObject);
		}

		String jsonString = jsonArray.toString();
		bufferedWriter.write(jsonString);
		bufferedWriter.flush();
		bufferedWriter.close();

	}

	public void writeSequence(HashSet<sequencePair2> sequence, HashMap<sequencePair2, HashSet<String>> connectionMap,
			Set<HashSet<String>> config, Set<HashSet<String>> filter) throws IOException {
		File file = new File(root + "Sequence.json");
		System.out.println("ANSWERS-SEQUENCE write to " + file);
		if (!file.exists()) {
			file.createNewFile();
		}

		JSONArray jsonArray = new JSONArray();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "utf-8");
		BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

		// 1. framework call sequence
		Set<sequencePair2> allseqsWithConnection = connectionMap.keySet();

		for (sequencePair2 seq : sequence) {
			HashSet<String> connect = null;
			for (sequencePair2 seq2 : allseqsWithConnection) {
				if (seq2.equals(seq)) {
					connect = connectionMap.get(seq2);
					break;
				}
			}

			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("kind", "Before-After");
			jsonObject.put("before", seq.getPre().getCollection());
			jsonObject.put("after", seq.getPost().getCollection());
			if (connect != null && !connect.isEmpty())
				jsonObject.put("connect", connect);
			jsonArray.put(jsonObject);
		}

		// 2. configure
		for (HashSet<String> ans : config) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("kind", "CONFIG");
			jsonObject.put("class", ans);
			jsonArray.put(jsonObject);
		}

		// 3. filter
		for (HashSet<String> ans : filter) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("kind", "FILTER");
			jsonObject.put("class", ans);
			jsonArray.put(jsonObject);
		}

		String jsonString = jsonArray.toString();
		bufferedWriter.write(jsonString);
		bufferedWriter.flush();
		bufferedWriter.close();
	}

	public void writeFrameworkIndirectlyInvoke(Set<frameworkIndirectInvoke> answer11) throws IOException {
		File file = new File(root + "IndirectInvoke.json");
		System.out.println("ANSWERS-INDIRECTLY-INVOKE write to " + file);
		if (!file.exists()) {
			file.createNewFile();
		}

		JSONArray jsonArray = new JSONArray();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "utf-8");
		BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

		for (frameworkIndirectInvoke frameworkInvoke : answer11) {
			str2Set callsite = frameworkInvoke.getCallsite();
			String invokeInst = callsite.getName();
			HashSet<String> invokeInstMarks = callsite.getVals();

//			ArrayList<str2Set> targets = frameworkInvoke.getTarget();
//			for (str2Set target : targets) {
//				String targetInst = target.getName();
//				HashSet<String> targetMarks = target.getVals();
//
//				JSONObject jsonObject = new JSONObject(new LinkedHashMap());
//				jsonObject.put("kind", "IndirectInvoke");
//				jsonObject.put("callsite", invokeInst);
//				jsonObject.put("callsite marks", invokeInstMarks);
//				jsonObject.put("target", targetInst);
//				jsonObject.put("target marks", targetMarks);
//				jsonArray.put(jsonObject);
//			}

			str2Set target = frameworkInvoke.getTarget();

			String targetInst = target.getName();
			HashSet<String> targetMarks = target.getVals();

			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("kind", "IndirectInvoke");
			jsonObject.put("callsite", invokeInst);
			jsonObject.put("callsite marks", invokeInstMarks);
			jsonObject.put("target", targetInst);
			jsonObject.put("target marks", targetMarks);
			jsonArray.put(jsonObject);

		}

		String jsonString = jsonArray.toString();
		bufferedWriter.write(jsonString);
		bufferedWriter.flush();
		bufferedWriter.close();
	}

	public void writeAll(HashMap<set2setPair, Integer> entries, HashSet<String> mayEntryPointFormalParameterSet,
			Set<HashSet<String>> managedClasses, HashSet<String> alias, HashSet<String> alias_apply,
			Set<HashSet<String>> fieldsInject1, Set<HashSet<String>> fieldsInject2,
			HashSet<FrmkRetPoints2> frmwkRetAcutalClass, HashSet<sequencePair2> callSequence,
			HashMap<sequencePair2, HashSet<String>> connectionMap, Set<frameworkIndirectInvoke> indirectCalls)
			throws IOException {
		File file;
		String subName = ConfigUtil.appKind;
		if (!subName.equals("") && subName.length() > 0) {
			file = new File(root + subName + ".json");
		} else {
			file = new File(root + "FrameworkSemantics.json");
		}

		System.out.println("ANSWERS write to " + file);
		if (!file.exists()) {
			file.createNewFile();
		}

		JSONArray jsonArray = new JSONArray();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "utf-8");
		BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

		/** 1. entries */
		for (set2setPair ans : entries.keySet()) {
			Set<String> classMarks = ans.getFirst();
			Set<String> methodMarks = ans.getSecond();
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("kind", "ENTRY");
			jsonObject.put("classMarks", classMarks);
			jsonObject.put("methodMarks", methodMarks);
			jsonArray.put(jsonObject);
		}

		for (String param : mayEntryPointFormalParameterSet) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("kind", "ENTRY-Param");
			jsonObject.put("classMarks", param);
			jsonArray.put(jsonObject);
		}

		/** 2. Framework managed, object-points-to, object-field-points-to */
		// 1. managed class
		for (HashSet<String> ans : managedClasses) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("classMarks", ans);
			jsonObject.put("kind", "MANAGED-CLASS");
			jsonArray.put(jsonObject);
		}
		// 2. class alias
		for (String ans : alias) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("mark", ans);
			jsonObject.put("type", "value");
			jsonObject.put("kind", "MANAGED-CLASS-Alias");
			jsonArray.put(jsonObject);
		}
		// 3. alias apply
		for (String ans : alias_apply) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("mark", ans);
			jsonObject.put("type", "value-alias");
			jsonObject.put("kind", "MANAGED-Actual-Class");
			jsonArray.put(jsonObject);
		}
		// 4. Inject on field
		for (HashSet<String> ans : fieldsInject1) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("marks", ans);
			jsonObject.put("type", "field");
			jsonObject.put("kind", "MANAGED-FIELD-Inject");
			jsonArray.put(jsonObject);
		}
		// 5. Inject by method
		for (HashSet<String> ans : fieldsInject2) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("method", ans);
			jsonObject.put("type", "method");
			jsonObject.put("kind", "MANAGED-FIELD-Inject");
			jsonArray.put(jsonObject);
		}
		// 6. framework call return actual points-to
		for (FrmkRetPoints2 ele : frmwkRetAcutalClass) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("parameterIndex", ele.getParamPosition());
			jsonObject.put("type", ele.getType());
			jsonObject.put("frameworkCall", ele.getFrmkCall());
			jsonObject.put("kind", "MANAGED-Return-Class");
			jsonArray.put(jsonObject);
		}

		/** 3. call sequence */
		Set<sequencePair2> allseqsWithConnection = connectionMap.keySet();
		for (sequencePair2 seq : callSequence) {
			HashSet<String> connect = null;
			for (sequencePair2 seq2 : allseqsWithConnection) {
				if (seq2.equals(seq)) {
					connect = connectionMap.get(seq2);
					break;
				}
			}

			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("kind", "ORDERED");
			jsonObject.put("before", seq.getPre().getCollection());
			if (connect != null && !connect.isEmpty())
				jsonObject.put("middle", connect);
			jsonObject.put("after", seq.getPost().getCollection());

			jsonArray.put(jsonObject);
		}

		/** 4. indirectly calls */
		for (frameworkIndirectInvoke frameworkInvoke : indirectCalls) {
			str2Set callsite = frameworkInvoke.getCallsite();
			String invokeInst = callsite.getName();
			HashSet<String> invokeInstMarks = callsite.getVals();

//			ArrayList<str2Set> targets = frameworkInvoke.getTarget();
//			for (str2Set target : targets) {
//				String targetInst = target.getName();
//				HashSet<String> targetMarks = target.getVals();
//
//				JSONObject jsonObject = new JSONObject(new LinkedHashMap<Object, Object>());
//				jsonObject.put("target", targetInst);
//				jsonObject.put("targetMarks", targetMarks);
//				jsonObject.put("callContext", invokeInstMarks);
//				jsonObject.put("frameworkCall", invokeInst);
//				jsonObject.put("kind", "IndirectInvoke");
//				jsonArray.put(jsonObject);
//			}

			str2Set target = frameworkInvoke.getTarget();
			String targetInst = target.getName();
			HashSet<String> targetMarks = target.getVals();

			JSONObject jsonObject = new JSONObject(new LinkedHashMap<Object, Object>());
			jsonObject.put("target", targetInst);
			jsonObject.put("targetMarks", targetMarks);
			jsonObject.put("callContext", invokeInstMarks);
			jsonObject.put("frameworkCall", invokeInst);
			jsonObject.put("kind", "IndirectInvoke");
			jsonArray.put(jsonObject);
		}

		String jsonString = jsonArray.toString();
		bufferedWriter.write(jsonString);
		bufferedWriter.flush();
		bufferedWriter.close();
	}

	public void writeSummaries(HashSet<set2setPair> entries, HashSet<String> params,
			Set<HashSet<String>> managedClasses, HashSet<String> classAlias, HashSet<String> objActualPoints2,
			HashSet<FrmkRetPoints2> frmwkRetAcutalClass, Set<HashSet<String>> fieldsInject,
			HashSet<set2setPair> callSequence, Set<CallsiteM2targetM> indirectCalls) throws IOException {
		File file = new File(root + "FrameworkSemantics.json");
		System.out.println("ANSWERS write to " + file);
		if (!file.exists()) {
			file.createNewFile();
		}

		JSONArray jsonArray = new JSONArray();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "utf-8");
		BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

		/** 1. entries */
		for (set2setPair ans : entries) {
			Set<String> classMarks = ans.getFirst();
			Set<String> methodMarks = ans.getSecond();
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("kind", "ENTRY");
			jsonObject.put("classMarks", classMarks);
			jsonObject.put("methodMarks", methodMarks);
			jsonArray.put(jsonObject);
		}

		for (String param : params) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("kind", "ENTRY-Param");
			jsonObject.put("classMarks", param);
			jsonArray.put(jsonObject);
		}

		/** 2. Framework managed, object-points-to, object-field-points-to */
		// 1. managed class
		for (HashSet<String> ans : managedClasses) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("classMarks", ans);
			jsonObject.put("kind", "MANAGED-CLASS");
			jsonArray.put(jsonObject);
		}
		// 2. class alias
		for (String ans : classAlias) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("mark", ans);
			jsonObject.put("type", "value");
			jsonObject.put("kind", "MANAGED-CLASS-Alias");
			jsonArray.put(jsonObject);
		}
		// 3. alias apply
		for (String ans : objActualPoints2) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("mark", ans);
//			jsonObject.put("type", "value-alias");
			jsonObject.put("type", FrmkCallParamType.Alias_text);
			jsonObject.put("kind", "MANAGED-Actual-Class");
			jsonArray.put(jsonObject);
		}
		// 4. framework call return actual points-to
		for (FrmkRetPoints2 ele : frmwkRetAcutalClass) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("parameterIndex", ele.getParamPosition());
			jsonObject.put("type", ele.getType());
			jsonObject.put("frameworkCall", ele.getFrmkCall());
			jsonObject.put("kind", "MANAGED-Return-Class");
			jsonArray.put(jsonObject);
		}
		// 5. Inject
		for (HashSet<String> ans : fieldsInject) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap());
			jsonObject.put("marks", ans);
			jsonObject.put("type", "field");
			jsonObject.put("kind", "MANAGED-FIELD-Inject");
			jsonArray.put(jsonObject);
		}

		/** 3. call sequence */

		for (set2setPair pair : callSequence) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<>());
			jsonObject.put("kind", "ORDERED");
			jsonObject.put("before", pair.getFirst());
			jsonObject.put("after", pair.getSecond());

			jsonArray.put(jsonObject);
		}

		/** 4. indirectly calls */
		for (CallsiteM2targetM frameworkInvoke : indirectCalls) {
			JSONObject jsonObject = new JSONObject(new LinkedHashMap<Object, Object>());
			jsonObject.put("target", frameworkInvoke.getTarget());
			jsonObject.put("targetMarks", frameworkInvoke.getTargetMarks());
			jsonObject.put("callContext", frameworkInvoke.getCallsiteMarks());
			jsonObject.put("frameworkCall", frameworkInvoke.getCall());
			jsonObject.put("kind", "IndirectInvoke");
			jsonArray.put(jsonObject);
		}

		String jsonString = jsonArray.toString();
		bufferedWriter.write(jsonString);
		bufferedWriter.flush();
		bufferedWriter.close();

	}

}
