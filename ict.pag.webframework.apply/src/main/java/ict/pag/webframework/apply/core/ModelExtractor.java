package ict.pag.webframework.apply.core;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.io.FileUtils;
import org.dom4j.Element;
import org.json.JSONArray;
import org.json.JSONObject;

import com.ibm.wala.ipa.cha.ClassHierarchy;

import ict.pag.webframework.XML.XMLMarksExtractor;
import ict.pag.webframework.apply.option.Configuration_backend;
import ict.pag.webframework.model.enumeration.CallType;
import ict.pag.webframework.model.enumeration.MarkScope;
import ict.pag.webframework.model.enumeration.ValueFrom;
import ict.pag.webframework.model.graph.GraphBuilder;
import ict.pag.webframework.model.marks.ConcreteValueMark;
import ict.pag.webframework.model.marks.EntryMark;
import ict.pag.webframework.model.marks.FrmkCallMark;
import ict.pag.webframework.model.marks.NormalMark;
import ict.pag.webframework.model.option.FileHelper;

public class ModelExtractor {
	/** need to initialize */
	private String app_path;
	private String framework_path;

	/** internal */
	private FrameworkSpecification frmkSpecification = new FrameworkSpecification();

	/** Answer */
	private FrameworkModel4App model_app;

	public ModelExtractor(String app_path, String framework_path) {
		this.app_path = app_path;
		this.framework_path = framework_path;
	}

	/**
	 * Extract specific application specification about framework from given
	 * framework rules
	 */
	public void extract() {
		System.out.println("......[Dealing With] " + this.app_path);
		long beforeTime = System.nanoTime();

		/* 1. extract framework specification from file */
		parseFrameworkModel_fromJsonFile();

		/** Information prepare */
		GraphBuilder builder = new GraphBuilder(Configuration_backend.g().getAppPath(), true,
				Configuration_backend.g().getLibDir());
		ClassHierarchy cha = builder.getCHA();
		HashSet<String> applicationClasses = builder.getApplicationClasses();

		/* 2. extract xml marks from given application */
		XMLMarksExtractor xmlex = new XMLMarksExtractor(FileHelper.getAllConcernedXMLs(app_path), applicationClasses);
		HashMap<String, HashSet<Element>> class2XMLEle = xmlex.getClass2XMLElement();

		/* 3. generate specific framework model for given application */
		FrameworkModel4App modelGenerator = new FrameworkModel4App(cha, class2XMLEle, frmkSpecification);
		

		double buildTime = (System.nanoTime() - beforeTime) / 1E9;
		System.out.println("[TIME-LOG]Model for Specific Application Extracted Done " + buildTime + " s!");

	}

	private void parseFrameworkModel_fromJsonFile() {
		File file_p = new File(framework_path);
		if (!file_p.exists()) {
			System.err.println("[ERROR]Framework Specification File Do Not Exist!");
			System.exit(0);
		}
		try {
			String content = FileUtils.readFileToString(file_p, "UTF-8");
			JSONArray jsonArray = new JSONArray(content);
			jsonArray.forEach(line -> {
				if (line instanceof JSONObject) {
					JSONObject obj = (JSONObject) line;
					String kind = (String) obj.get("kind");
					switch (kind) {
					case "Entry":
						add2EntrySet(obj);
						break;
					case "IndirectCall":
						add2IndirectCallSet(obj);
						break;
					case "Entry-Param":
						add2EntryParam(obj);
						break;
					case "Generate-Class":
						add2GenerateClass(obj);
						break;
					case "Generate-Class-Alias":
						add2ClassAlias(obj);
						break;
					case "Inject-Field":
						add2FieldInject(obj);
						break;
					case "Inject-Field-Points2":
						add2FieldPoints2(obj);
						break;
					case "Return-Points2":
						add2ReturnPoints2(obj);
						break;
					}
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void add2ReturnPoints2(JSONObject obj) {
		HashSet<String> callsSet = new HashSet<String>();
		String callString = obj.get("callStmt").toString();
		callsSet.add(callString);
		ValueFrom val = ValueFrom.valueOf(obj.get("objectFrom").toString());
		String attr = obj.get("objectFromAttribute").toString();

		frmkSpecification
				.addFrameworkCallReturnPoints2Marks(new ConcreteValueMark(callsSet, MarkScope.Method, attr, val));
	}

	private void add2FieldPoints2(JSONObject obj) {
		HashSet<String> fieldSet = new HashSet<String>();
		if (obj.get("fieldMarks") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("fieldMarks"), fieldSet);
		}
		ValueFrom val = ValueFrom.valueOf(obj.get("objectFrom").toString());
		String attr = obj.get("objectFromAttribute").toString();

		if (!fieldSet.isEmpty()) {
			frmkSpecification.addFieldPoints2Marks(new ConcreteValueMark(fieldSet, MarkScope.Field, attr, val));
		}
	}

	private void add2FieldInject(JSONObject obj) {

		HashSet<String> fieldSet = new HashSet<String>();
		if (obj.get("fieldMarks") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("fieldMarks"), fieldSet);
		}

		if (!fieldSet.isEmpty()) {
			frmkSpecification.addFieldInjectMarks(new NormalMark(fieldSet, MarkScope.Field));
		}
	}

	private void add2ClassAlias(JSONObject obj) {
		HashSet<String> classSet = new HashSet<String>();

		if (obj.get("classMarks") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("classMarks"), classSet);
		}
		ValueFrom val = ValueFrom.valueOf(obj.get("aliasFrom").toString());
		String attr = obj.get("aliasFromAttribute").toString();

		if (!classSet.isEmpty())
			frmkSpecification.addAliasMarks(new ConcreteValueMark(classSet, MarkScope.Clazz, attr, val));
	}

	private void add2GenerateClass(JSONObject obj) {
		HashSet<String> classSet = new HashSet<String>();

		if (obj.get("classMarks") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("classMarks"), classSet);
		}

		if (!classSet.isEmpty()) {
			frmkSpecification.addManagedClassMarks(new NormalMark(classSet, MarkScope.Clazz));
		}
	}

	private void add2IndirectCallSet(JSONObject obj) {
		String callString = obj.get("callStmt").toString();

		if (obj.has("paramIndex")) {
			String index = obj.get("paramIndex").toString();
			if (!index.equals("-1")) {
				frmkSpecification
						.addFrameworkCallMarks(new FrmkCallMark(CallType.Param, callString, Integer.parseInt(index)));
			}
		} else if (obj.has("methodMarks")) {
			HashSet<String> mtdSet = new HashSet<String>();
			if (obj.get("methodMarks") instanceof JSONArray) {
				addJsonArray2StringSet((JSONArray) obj.get("methodMarks"), mtdSet);
			}
			if (!mtdSet.isEmpty()) {
				frmkSpecification.addFrameworkCallMarks(new FrmkCallMark(CallType.Attribute, callString, mtdSet));
			}
		}

	}

	private void add2EntryParam(JSONObject obj) {
		frmkSpecification.addMayEntryPointFormalParameterSet(obj.get("paramType").toString());
	}

	private void add2EntrySet(JSONObject obj) {
		HashSet<String> classSet = new HashSet<String>();
		HashSet<String> mtdSet = new HashSet<String>();

		if (obj.get("classMarks") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("classMarks"), classSet);

		}
		if (obj.get("methodMarks") instanceof JSONArray) {
			addJsonArray2StringSet((JSONArray) obj.get("methodMarks"), mtdSet);
		}

		frmkSpecification.addEntryMarkSet(new EntryMark(classSet, mtdSet));
	}

	private void addJsonArray2StringSet(JSONArray jsonArray, HashSet<String> classSet) {
		jsonArray.forEach(n -> {
			classSet.add(n.toString());
		});
	}

	/**
	 * Export specific application model results to file
	 */
	public void exportModel2File() {

	}

	public FrameworkModel4App getFrameworkModel4Application() {
		return model_app;
	}

}
