package ict.pag.webframework.preInstrumental;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentType;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ict.pag.m.instrumentation.Util.ConfigureUtil;
import ict.pag.webframework.XML.XMLMarksExtractor;
import ict.pag.webframework.XML.Util.XMLConfigurationUtil;
import ict.pag.webframework.log.RequestInfoExtractor;
import ict.pag.webframework.log.dynamicCG.CallsiteInfo;
import ict.pag.webframework.log.dynamicCG.DynamicCG;
import ict.pag.webframework.log.dynamicCG.DynamicCGBuilder;
import ict.pag.webframework.log.dynamicCG.DynamicCGNode;
import ict.pag.webframework.log.dynamicCG.MethodCalledType;
import ict.pag.webframework.log.util.LogFormatHepler;
import ict.pag.webframework.preInstrumental.entity.ModifiedInfo;
import ict.pag.webframework.preInstrumental.entity.Modification;
import ict.pag.webframework.preInstrumental.entity.ModifiedSemantic;
import ict.pag.webframework.preInstrumental.entity.Url2ModificationInfo;
import ict.pag.webframework.preInstrumental.helper.CollectionHelper;
import ict.pag.webframework.preInstrumental.helper.ConfigurationCollector;
import ict.pag.webframework.preInstrumental.helper.CtObjectCopyHelper;
import ict.pag.webframework.preInstrumental.helper.SearchInCtClassHelper;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ClassFile;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;

/**
 * 1. determine which element concerned?</br>
 * 2. find all configurations on this element</br>
 * 3. change one and generate an output
 */
public class PreModifier {
	/**
	 * input
	 */
	String appPath;/* web app */
	String libPath;/* Library path, only exists in directory */
	String externalPath;/* some jars like javax */
	String outPath;/* the modified content directory */
	XMLMarksExtractor xmlex;
	RequestInfoExtractor extractor;
	HashMap<String, HashSet<String>> class2XMLFile;
	ArrayList<String> classFiles;/* all classFiles */

	boolean isJar = false;
	String jarPath; /* exist only when isJar is true */

	public PreModifier(String appPath, String libPath, String externalPath, String outPath, XMLMarksExtractor xmlex, RequestInfoExtractor extractor,
			ArrayList<String> classFiles2, boolean isJar, String jarP) {
		this.appPath = appPath;
		this.libPath = libPath;
		this.externalPath = externalPath;
		this.outPath = outPath;

		this.classFiles = classFiles2;

		this.isJar = isJar;
		this.jarPath = jarP;

		this.class2XMLFile = xmlex.getClass2XMLFile();
		this.extractor = extractor;
	}

	/**
	 * Answer
	 */
	/** this group share same list index */
	// index also represent the modified artifact suffix
//	protected ArrayList<ModifiedInfo> modifiedRecord = new ArrayList<>();
	protected int INDEX = 0;
	protected ArrayList<Url2ModificationInfo> url2ModifyedInfo = new ArrayList<>();
	/**
	 * this group use to record the request that has content, and the modified
	 * content start index
	 */
	protected ArrayList<String> request = new ArrayList<>();
	protected ArrayList<String> response = new ArrayList<>();
	protected ArrayList<Integer> requestModifiedIndex = new ArrayList<>();/*-1 means this request have no modified content; other is from index*/

	/** 1. deploy; 2. request */
	public void modify() {
		modifyDeploySequence();
		modifyRequestSequence();
	}

	public void setThresdhold(int thresdhold) {
		this.thresdhold = thresdhold;
	}

	int thresdhold = 5;

	/** deal with the sequence that deploy triggered */
	public void modifyDeploySequence() {
		try {
			/* record visited */
			HashSet<String> visited_entry = new HashSet<>();
			HashMap<String, HashSet<String>> visited_indirectCall = new HashMap<>();
			HashSet<String> visited_field = new HashSet<>();

			/* record visited */
			HashMap<String, Integer> visitedTags2Count_entry = new HashMap<>();
			HashMap<String, Integer> visitedTags2Count_indirectCall = new HashMap<>();
			HashMap<String, Integer> visitedTags2Count_field = new HashMap<>();// only inject

			// for modify bytecode
			ClassPool pool = ClassPool.getDefault();
			if (externalPath.length() != 0)
				pool.insertClassPath(externalPath + File.separator + "*");
			if (libPath.length() != 0)
				pool.insertClassPath(libPath + File.separator + "*");
			pool.insertClassPath(appPath);

			/* field inject Internals */
			HashMap<String, HashSet<String>> field2RTClass = new HashMap<>();/* class.field:org.classA */
			ArrayList<String> fields_write = new ArrayList<>();
			ArrayList<String> fields_write_tar = new ArrayList<>();
			ArrayList<DynamicCGNode> fields_write_method = new ArrayList<>();/* is entry or not */

			ArrayList<ArrayList<String>> sequences = extractor.getConfigsSeqs();
			if (!sequences.isEmpty()) {
				for (ArrayList<String> sequence : sequences) {
					if (sequence.isEmpty())
						continue;
					System.out.println("...start dealing ");
					request.add("deploy");
					response.add("deploy");
					// add the first url to validate whether deploy is success
					if (extractor.getUrlRequest().size() > 0) {
						request.add(extractor.getUrlRequest().get(0));
						response.add(extractor.getUrlResponse().get(0));
					}
					requestModifiedIndex.add(-1); // -1 is a placeholder
//					int startIndex = modifiedRecord.size();
					int startIndex = INDEX;
					url2ModifyedInfo.add(new Url2ModificationInfo("deploy"));

					// inter-request
					DynamicCGBuilder builder = new DynamicCGBuilder();
					builder.buildCGNodes(LogFormatHepler.removeThreadID(sequence), MethodCalledType.configuration);
					DynamicCG cg = builder.getDynamicCallGraph();

					/**
					 * 1. entry
					 */
					modify_Entry(cg, pool, visited_entry, visitedTags2Count_entry);
					/**
					 * 2. indirect call
					 */
					modify_IndirectCall(cg, pool, visited_indirectCall, visitedTags2Count_indirectCall);
					/**
					 * 3. field inject
					 */
					/**
					 * including: on field: inject? inject what?</br>
					 * on class whether inject this class?
					 */
					modify_Field(cg, pool, visited_field, field2RTClass, fields_write, fields_write_tar, fields_write_method, visitedTags2Count_field);

//					if (modifiedRecord.size() == startIndex) {
					if (INDEX == startIndex) {
						// no modified content
						continue;
					} else {
						requestModifiedIndex.remove(requestModifiedIndex.size() - 1);
						requestModifiedIndex.add(startIndex);
					}
				}
			}
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
	}

	/** only concerned on the request triggered log sequence */
	public void modifyRequestSequence() {
		try {
			/* record visited */
			HashSet<String> visited_entry = new HashSet<>();
			HashMap<String, HashSet<String>> visited_indirectCall = new HashMap<>();
			HashSet<String> visited_field = new HashSet<>();

			/* record visited */
			HashMap<String, Integer> visitedTags2Count_entry = new HashMap<>();
			HashMap<String, Integer> visitedTags2Count_indirectCall = new HashMap<>();
			HashMap<String, Integer> visitedTags2Count_field = new HashMap<>();// only inject

			// for modify bytecode
			ClassPool pool = ClassPool.getDefault();
			if (externalPath.length() != 0)
				pool.insertClassPath(externalPath + File.separator + "*");
			if (libPath.length() != 0)
				pool.insertClassPath(libPath + File.separator + "*");
			pool.insertClassPath(appPath);

			/* field inject Internals */
			HashMap<String, HashSet<String>> field2RTClass = new HashMap<>();/* class.field:org.classA */
			ArrayList<String> fields_write = new ArrayList<>();
			ArrayList<String> fields_write_tar = new ArrayList<>();
			ArrayList<DynamicCGNode> fields_write_method = new ArrayList<>();/* is entry or not */

			// for all requests that trigger application invoke
			for (int i = 0; i < extractor.getLength(); i++) {
				// TODO:delete!;for debug
//				if (i != 16)
//					continue;
				// only cares about the url that trigger application code
				if (!extractor.getSeqs().get(i).isEmpty()) {
					System.out.println("...start dealing " + extractor.getUrlRequest().get(i));
					request.add(extractor.getUrlRequest().get(i));
					response.add(extractor.getUrlResponse().get(i));
					requestModifiedIndex.add(-1);
//					int startIndex = modifiedRecord.size();
					int startIndex = INDEX;
					url2ModifyedInfo.add(new Url2ModificationInfo(extractor.getUrlRequest().get(i)));

					// inter-request
					DynamicCGBuilder builder = new DynamicCGBuilder();
					builder.buildCGNodes(LogFormatHepler.removeThreadID(extractor.getSeqs().get(i)), MethodCalledType.request);
					DynamicCG cg = builder.getDynamicCallGraph();

					/**
					 * 1. entry
					 */
					modify_Entry(cg, pool, visited_entry, visitedTags2Count_entry);
					/**
					 * 2. indirect call
					 */
					modify_IndirectCall(cg, pool, visited_indirectCall, visitedTags2Count_indirectCall);
					/**
					 * 3. field inject
					 */
					/**
					 * including: on field: inject? inject what?</br>
					 * on class whether inject this class?
					 */
					modify_Field(cg, pool, visited_field, field2RTClass, fields_write, fields_write_tar, fields_write_method, visitedTags2Count_field);

//					if (modifiedRecord.size() == startIndex) {
					if (INDEX == startIndex) {
						// no modified content
						continue;
					} else {
						requestModifiedIndex.remove(requestModifiedIndex.size() - 1);
						requestModifiedIndex.add(startIndex);
					}
				}
			}
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 1. entry
	 */
	public void modify_Entry(DynamicCG cg, ClassPool pool, HashSet<String> visited_entry, HashMap<String, Integer> visitedTags2Count_entry) {
		ArrayList<DynamicCGNode> entryNodes = cg.getRoots();/* ordered */
		for (DynamicCGNode node : entryNodes) {
			// find configuration for this node
			String stmt = node.getStmt();
			if (visited_entry.contains(stmt))
				continue;
			visited_entry.add(stmt);

			/** core1: modify annotations! */
			String fullClass0 = stmt.substring(0, stmt.lastIndexOf('.'));
			String f = findMatchedClassFile(fullClass0);
			if (f != null) {
				dealWithMethod_Annotation(f, stmt, pool, ModifiedSemantic.Entry, false, null, visitedTags2Count_entry, true);
			}

			/** core2: modify xmls! */
			if (class2XMLFile.keySet().contains(fullClass0)) {
				for (String xmlFile : class2XMLFile.get(fullClass0)) {
					dealWithMethod_XML(f, stmt, pool, xmlFile, ModifiedSemantic.Entry, null, visitedTags2Count_entry, true);
				}
			}
		}
	}

	/**
	 * 2. indirect call
	 * 
	 * @param visitedTags2Count_indirectCall
	 */
	public void modify_IndirectCall(DynamicCG cg, ClassPool pool, HashMap<String, HashSet<String>> visited_indirectCall,
			HashMap<String, Integer> visitedTags2Count_indirectCall) {
		for (DynamicCGNode node : cg.getNodes()) {
			for (CallsiteInfo cs : node.getCallsites()) {
				String csStmt = cs.getStmt();
				String cl0 = csStmt.substring("[callsite]".length(), csStmt.indexOf('('));
				String csClassName = cl0.replace('.', '/');

				String csPureStmt0 = csStmt.substring("[callsite]".length());
				String csPureStmt1 = csPureStmt0.substring(0, csPureStmt0.indexOf(']'));
				String csPureStmt = csPureStmt1.substring(0, csPureStmt1.lastIndexOf('['));
				if (!ConfigureUtil.isApplicationClass(csClassName) && !ConfigureUtil.isCommonJavaClass(csClassName)) {
					// add 22-10-31, only deal with the closed pair
					// TODO: test: current only for logicaldoc-deploy
					// modify @22-12-07 when community
					// && !node.getStmt().contains(".main([Ljava/lang/String;)V")
					if (!cs.isClosed())
						continue;

					// only concern about framework invoke
					for (DynamicCGNode targetCall : cs.getActual_targets()) {
						String targetStmt = targetCall.getStmt();
						if (visited_indirectCall.containsKey(csPureStmt) && visited_indirectCall.get(csPureStmt).contains(targetStmt))
							continue;
						if (!visited_indirectCall.containsKey(csPureStmt)) {
							visited_indirectCall.put(csPureStmt, new HashSet<>());
						}
						visited_indirectCall.get(csPureStmt).add(targetStmt);

						/** core1: modify annotations! */
						String fullClass0 = targetStmt.substring(0, targetStmt.lastIndexOf('.'));
						String f = findMatchedClassFile(fullClass0);
						if (f != null) {
							ArrayList<String> tmp = new ArrayList<String>();
							tmp.add(csPureStmt);
							dealWithMethod_Annotation(f, targetStmt, pool, ModifiedSemantic.IndirectCall, false, tmp, visitedTags2Count_indirectCall, false);
						}

						/** core2: modify xmls! */
						if (class2XMLFile.keySet().contains(fullClass0)) {
							for (String xmlFile : class2XMLFile.get(fullClass0)) {
								ArrayList<String> tmp = new ArrayList<String>();
								tmp.add(csPureStmt);
								dealWithMethod_XML(f, targetStmt, pool, xmlFile, ModifiedSemantic.IndirectCall, tmp, visitedTags2Count_indirectCall, false);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 3. field inject
	 * 
	 * @param visitedTags2Count_field
	 */
	public void modify_Field(DynamicCG cg, ClassPool pool, HashSet<String> visited_field, HashMap<String, HashSet<String>> field2RTClass,
			ArrayList<String> fields_write, ArrayList<String> fields_write_tar, ArrayList<DynamicCGNode> fields_write_method,
			HashMap<String, Integer> visitedTags2Count_field) {
		/**
		 * including: on field: inject? inject what?</br>
		 * on class whether inject this class?
		 */
		for (DynamicCGNode node : cg.getNodes()) {
			String stmt = node.getStmt();
			// need to use base class
			String baseClass = null;
			ArrayList<String> fields = new ArrayList<>();// example.class.pag.Class.fieldname
			ArrayList<String> fields_tar = new ArrayList<>();// example.runtime.ClassActual

			// 1. collect all field that injected
			for (String line : node.getSequenceinfo()) {
				if (line.startsWith("[base class]")) {
					baseClass = line.substring("[base class]".length());
					continue;
				}
//				if (baseClass != null) {
				String fieldSig = null;
				String rtType = null;
				// 1.1 parse log
				if (line.startsWith("[base field]")) {
					String str1 = line.substring("[base field]".length());
					String[] strs = str1.split(":");
					String fieldName = strs[0];
					String fieldRTType = strs[2];

					// may be basic type like long/int
					fieldSig = baseClass + "." + fieldName + ":" + strs[1];
					rtType = fieldRTType;
				} else if (line.startsWith("[field read]")) {
					String fieldSig0;
					if (line.contains("[runtimeType]")) {
						fieldSig0 = line.substring(line.indexOf("[signature]") + "[signature]".length(), line.indexOf("[runtimeType]"));
						if (line.contains("[base]")) {
							String str1 = line.substring(line.indexOf("[runtimeType]") + "[runtimeType]".length(), line.lastIndexOf("[base]"));
							if (str1.contains("[collection]")) {
								rtType = str1.substring(0, str1.indexOf("[collection]"));
							} else {
								rtType = str1;
							}
						}
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

				} else if (line.startsWith("[field write]")) {
					String writeContent;
					String fieldSig0;
					if (line.contains("[runtimeType]")) {
						// write
						fieldSig0 = line.substring(line.indexOf("[signature]") + "[signature]".length(), line.indexOf("[runtimeType]"));

						writeContent = line.substring(line.indexOf("[runtimeType]") + "[runtimeType]".length(), line.lastIndexOf("[base]"));
					} else {
						fieldSig0 = line.substring(line.indexOf("[signature]") + "[signature]".length(), line.indexOf("[fieldObject]"));
						writeContent = "null";
					}
					rtType = writeContent;

					String fieldDec = fieldSig0.substring(fieldSig0.indexOf(':') + 1);
					if (fieldDec.startsWith("L") && fieldDec.endsWith(";")) {
						String fieldDec1 = fieldDec.replace('/', '.').substring(1, fieldDec.length() - 1);
						fieldSig = fieldSig0.substring(0, fieldSig0.indexOf(':')) + ":" + fieldDec1;
					} else
						fieldSig = fieldSig0.substring(0, fieldSig0.indexOf(':')) + ":" + fieldDec;

					fields_write.add(fieldSig);
					fields_write_tar.add(writeContent);
					fields_write_method.add(node);

				}
				// 1.2 add to set that need to analyze
				if (fieldSig != null && rtType != null && baseClass != null) {
					if (!field2RTClass.containsKey(fieldSig))
						field2RTClass.put(fieldSig, new HashSet<String>());

					if (field2RTClass.get(fieldSig).contains(rtType))
						continue;

					// if current set do not have null field, that field had been handled before
					if (field2RTClass.get(fieldSig).size() > 0 && !field2RTClass.get(fieldSig).contains("null")) {
						field2RTClass.get(fieldSig).add(rtType);
						continue;
					}
					// has at least one not null field, and had been handled
					if (field2RTClass.get(fieldSig).size() > 2) {
						field2RTClass.get(fieldSig).add(rtType);
						continue;
					}

					field2RTClass.get(fieldSig).add(rtType);
					if (rtType.equals("null")) {
						continue;
					} else {
						fields.add(fieldSig);
						fields_tar.add(rtType);
					}
				}
//				}
			}
			// 1.3 modify
			for (int ii = 0; ii < fields.size(); ii++) {
				String fieldSig = fields.get(ii);
				if (visited_field.contains(fieldSig))
					continue;
				visited_field.add(fieldSig);

				String actualTar = fields_tar.get(ii);

				String fieldFullName = fieldSig.substring(0, fieldSig.indexOf(':'));
				String field_fullClass0 = fieldFullName.substring(0, fieldFullName.lastIndexOf('.'));
				String fieldName = fieldFullName.substring(fieldFullName.lastIndexOf('.') + 1);
				String field_classFile = findMatchedClassFile(field_fullClass0);

				if (field_classFile == null) // field class will be modified regardless anno/xml
					continue;

				// q1: inject?
				// a1.1: focus on field configuration
				boolean hasanno = false;
				boolean hasxml = false;
				HashSet<String> containsClass2XMLFile = new HashSet<>();
				/** core1: modify annotations! */
				hasanno = dealWithField_Annotation(field_classFile, stmt, fieldSig, pool, ModifiedSemantic.Field_Inject, visitedTags2Count_field);
				/** core2: modify xmls! */
				if (class2XMLFile.keySet().contains(field_fullClass0)) {
					for (String xmlFile : class2XMLFile.get(field_fullClass0)) {
						boolean ret = dealWithField_XML(field_classFile, stmt, fieldSig, pool, xmlFile, ModifiedSemantic.Field_Inject, visitedTags2Count_field);
						if (ret) {
							hasxml = true;
							containsClass2XMLFile.add(xmlFile);
						}
					}
				}

				// a1.2 on field write method
				int index = fields_write.indexOf(fieldSig);
				if (index != -1) {
					// on method that inject field
					DynamicCGNode injectFieldNode = fields_write_method.get(index);
					if (injectFieldNode.getFather() == null) {
						String methodStmt = injectFieldNode.getStmt();
						String methodClassName = methodStmt.substring(0, methodStmt.lastIndexOf('.'));
						String methodName = methodStmt.substring(methodClassName.length() + 1, methodStmt.indexOf('('));
//						if (methodName != null && methodName.equals("set" + fieldName)) {
						if (methodName != null && methodName.startsWith("set")) {
							ArrayList<String> tmp = new ArrayList<>();
							tmp.add(fieldSig);
							/** annotation */
							boolean ret1 = dealWithMethod_Annotation(field_classFile, methodStmt, pool, ModifiedSemantic.Field_Inject_method, true, tmp,
									visitedTags2Count_field, false);
							if (ret1)
								hasanno = true;

							/** xml */
							if (class2XMLFile.keySet().contains(field_fullClass0)) {
								for (String xmlFile : class2XMLFile.get(field_fullClass0)) {
									boolean ret = dealWithMethod_XML(field_classFile, methodStmt, pool, xmlFile, ModifiedSemantic.Field_Inject_method, tmp,
											visitedTags2Count_field, false);
									if (ret)
										hasxml = true;
								}
							}
						}
					}
				}

				/* if field injected has inject configuration, then find the inject content */
				if (!hasanno && !hasxml)
					continue;
				// q2: inject content? only concerned on the application class object
				if (!ConfigureUtil.isApplicationClass(actualTar.replace('.', '/')))
					continue;
				// target class info
				String staticTarClassName = actualTar.substring(actualTar.lastIndexOf('.') + 1);
				String staticFullTarClass = actualTar;
				if (staticTarClassName.contains("$$")) {
					staticTarClassName = staticTarClassName.substring(0, staticTarClassName.indexOf("$$"));
					staticFullTarClass = staticFullTarClass.substring(0, staticFullTarClass.indexOf("$$"));
				}
				String targetClassFilePath = findMatchedClassFile(staticFullTarClass);

				// step 1. collect all attributes value on field and target class
				// value all toLowerCase
				HashSet<String> values_field = new HashSet<>();
				HashSet<String> values_class = new HashSet<>();
				HashMap<String, HashSet<String>> val2AnnotationType_field = new HashMap<>();
				HashMap<String, HashSet<String>> val2XMLEle_field = new HashMap<>();
				HashMap<String, HashSet<String>> val2AnnotationType_class = new HashMap<>();
				HashMap<String, HashSet<String>> val2XMLEle_class = new HashMap<>();
				collectAllAttrValues_field(field_classFile, fieldSig, pool, containsClass2XMLFile, val2AnnotationType_field, val2XMLEle_field, values_field);
				collectAllAttrValues_class(staticFullTarClass, pool, val2AnnotationType_class, val2XMLEle_class, values_class);

				if (targetClassFilePath == null || targetClassFilePath.equals(field_classFile))
					continue;

				// step 2. compare and find
				/**
				 * All compare situations show below:</br>
				 * 1.a field-anno & class-anno</br>
				 * 1.b field-anno & class-xml</br>
				 * 1.c field-xml & class-anno</br>
				 * 1.d field-xml & class-xml</br>
				 * 2.a field-anno & class-name</br>
				 * 2.b field-xml & class-name</br>
				 * 3.a field-name & class-anno</br>
				 * 3.b field-name & class-xml</br>
				 * 
				 * Policy:</br>
				 * 1. When copy target class for checking field inject target change, it needs
				 * to copy new field since old field type is not conform to new copied target
				 * class.</br>
				 * 2. Since once target class copy created, it can be use for each field. The
				 * copied class is a new CtClass differ from original class, thus copy a target
				 * class when need. </br>
				 * 3. Copy a field including: 1. copy field itself; 2. whether change declare
				 * type; 3. modify the class that field belong to. Thus, when copy field, we
				 * read and make the CtClass again.
				 */
				HashSet<String> sameVals_classanno_fieldanno = CollectionHelper.findSameStringValue(val2AnnotationType_class.keySet(),
						val2AnnotationType_field.keySet()); // 1.a
				HashSet<String> sameVals_classanno_fieldxml = CollectionHelper.findSameStringValue(val2AnnotationType_class.keySet(),
						val2XMLEle_field.keySet()); // 1.c
				HashSet<String> sameVals_classxml_fieldanno = CollectionHelper.findSameStringValue(val2XMLEle_class.keySet(),
						val2AnnotationType_field.keySet()); // 1.b
				HashSet<String> sameVals_classxml_fieldxml = CollectionHelper.findSameStringValue(val2XMLEle_class.keySet(), val2XMLEle_field.keySet()); // 1.c

				File clFile_class = new File(targetClassFilePath);
				try (DataInputStream dis_class = new DataInputStream(new FileInputStream(clFile_class))) {
					ClassFile classFile_tar = new ClassFile(dis_class);
					CtClass tar_cc = pool.makeClass(classFile_tar);

					String addClassName = tar_cc.getPackageName() + "." + "mhnClass_" + tar_cc.getSimpleName();
					String addClassSimpleName = "mhnClass_" + tar_cc.getSimpleName();
					if (tar_cc.getPackageName() == null)
						addClassName = addClassSimpleName;
					File clFile_field = new File(field_classFile);

					/* 1.a field-anno & class-anno */
					if (!sameVals_classanno_fieldanno.isEmpty()) {
						try (DataInputStream dis_field = new DataInputStream(new FileInputStream(clFile_field))) {
							ClassFile classFile_field = new ClassFile(dis_field);
							CtClass field_cc = pool.makeClass(classFile_field);
							CtField field = SearchInCtClassHelper.findCtField(field_cc, fieldName);
							if (field == null)
								continue;
							String addFieldName = "mhnField_" + field.getName();

							// modify field_cc and copy tar_cc
							CtClass addTarClass = CtObjectCopyHelper.addNewClassCopy(tar_cc, addClassName, false);
							CtField addfield = CtObjectCopyHelper.addNewFieldCopy(field_cc, field, addFieldName, false, addTarClass);

							AnnotationsAttribute tarClassAttr = (AnnotationsAttribute) addTarClass.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
							if (tarClassAttr == null)
								continue;
							AnnotationsAttribute field_attr = (AnnotationsAttribute) addfield.getFieldInfo().getAttribute(AnnotationsAttribute.visibleTag);
							if (field_attr == null)
								continue;

							for (String sameVal : sameVals_classanno_fieldanno) {
								String newVal = "mhn_" + sameVal;

								for (String tarAnnoAttrName : val2AnnotationType_class.get(sameVal)) {
									Annotation class_anno = tarClassAttr.getAnnotation(tarAnnoAttrName);
									if (class_anno == null)
										continue;
									StringMemberValue class_anno_memval = SearchInCtClassHelper.findAnnotationMemberValue(class_anno, sameVal);// class-key
									if (class_anno_memval == null)
										continue;

									for (String fieldAnnoAttrName : val2AnnotationType_field.get(sameVal)) {
										Annotation field_anno = field_attr.getAnnotation(fieldAnnoAttrName);
										if (field_anno == null)
											continue;
										StringMemberValue field_anno_memval = SearchInCtClassHelper.findAnnotationMemberValue(field_anno, sameVal);// field-key
										if (field_anno_memval == null)
											continue;

										String originalValue_field = field_anno_memval.getValue();/* same value are lower-case */
										String originalValue_class = class_anno_memval.getValue();/* same value are lower-case */
										// set to same value
										field_anno_memval.setValue(newVal);
										class_anno_memval.setValue(newVal);
										field_attr.addAnnotation(field_anno);
										tarClassAttr.addAnnotation(class_anno);
										// record
										ArrayList<String> tmp_Configs = new ArrayList<>();
										ArrayList<String> tmp_body = new ArrayList<>();
										tmp_Configs.add(field_anno.getTypeName());
										tmp_Configs.add(class_anno.getTypeName());
										tmp_body.add(addFieldName);
										tmp_body.add(addClassName);
										// if the statement is getField/setField, also change the statement
										String stmt2 = stmt;
										String str1 = stmt.substring(0, stmt.indexOf('('));
										if (str1.toLowerCase().endsWith(field.getName().toLowerCase())) {
											String str2 = str1.substring(str1.lastIndexOf('.'));
											String prefix = str2.substring(0, str2.length() - field.getName().length());
											stmt2 = str1.substring(0, stmt.lastIndexOf('.')) + prefix + addFieldName.substring(0, 1).toUpperCase()
													+ addFieldName.substring(1) + stmt.substring(stmt.indexOf('('));
										}
										tmp_body.add(stmt2); // check position, a function call statement
//										modifiedRecord.add(
//												new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2, Modification.add, tmp_Configs, tmp_body, true));
//										url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
//												new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2, Modification.add, tmp_Configs, tmp_body, true),
//												modifiedRecord.size());
										INDEX++;
										url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
												new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2, Modification.add, tmp_Configs, tmp_body, true),
												INDEX);
										// write multiple files
										// write to file
										String outSubPath1 = calSubPath(tar_cc.getName(), targetClassFilePath);
										String outSubPath2 = calSubPath(field_cc.getName(), field_classFile);
										if (isJar) {
											ArrayList<CtClass> ctClasses = new ArrayList<>();
											ArrayList<String> subPaths = new ArrayList<>();
											ctClasses.add(addTarClass);
											ctClasses.add(field_cc);
											subPaths.add(outSubPath1);
											subPaths.add(outSubPath2);
											write2JarFiles(ctClasses, subPaths, new ArrayList<>(), new ArrayList<>());
										} else {
											write2SingleClassFile(addTarClass, outSubPath1);
											write2SingleClassFile(field_cc, outSubPath2);
										}
										writeRequestSequence();
										// recover configuration value
										field_anno_memval.setValue(originalValue_field);
										class_anno_memval.setValue(originalValue_class);
										field_attr.addAnnotation(field_anno);
										tarClassAttr.addAnnotation(class_anno);
									}
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					/* 1.b field-anno & class-xml */
					if (!sameVals_classxml_fieldanno.isEmpty()) {
						try (DataInputStream dis_field = new DataInputStream(new FileInputStream(clFile_field))) {
							ClassFile classFile_field = new ClassFile(dis_field);
							CtClass field_cc = pool.makeClass(classFile_field);
							CtField field = SearchInCtClassHelper.findCtField(field_cc, fieldName);
							if (field == null)
								continue;
							String addFieldName = "mhnField_" + field.getName();

							// modify field_cc and copy tar_cc
							CtClass addTarClass = CtObjectCopyHelper.addNewClassCopy(tar_cc, addClassName, true);
							CtField addfield = CtObjectCopyHelper.addNewFieldCopy(field_cc, field, addFieldName, false, addTarClass);

							AnnotationsAttribute field_attr = (AnnotationsAttribute) addfield.getFieldInfo().getAttribute(AnnotationsAttribute.visibleTag);
							if (field_attr == null)
								continue;
							if (!class2XMLFile.containsKey(staticFullTarClass))
								continue;

							/* modify class xml also need to modify the class index attr */
							for (String xmlFile : class2XMLFile.get(staticFullTarClass)) {
								SAXReader reader = new SAXReader();
								reader.setIgnoreComments(true);
								reader.setValidation(false);
								reader.setEntityResolver(new EntityResolver() {
									@Override
									public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
										return new InputSource(new ByteArrayInputStream("".getBytes()));
									}
								});
								try {
									Document document = reader.read(xmlFile);
									Element root = document.getRootElement();
									HashMap<Element, HashSet<Attribute>> node2attrs_class = new HashMap<>();
									ConfigurationCollector.collectClassXMLConfiguration(root, staticFullTarClass, node2attrs_class);

									for (String sameVal : sameVals_classxml_fieldanno) {
										String newVal = "mhn_" + sameVal;

										// field-level
										for (String fieldAnnoAttrName : val2AnnotationType_field.get(sameVal)) {
											Annotation field_anno = field_attr.getAnnotation(fieldAnnoAttrName);
											if (field_anno == null)
												continue;
											StringMemberValue field_anno_memval = SearchInCtClassHelper.findAnnotationMemberValue(field_anno, sameVal);// field-key
											if (field_anno_memval == null)
												continue;
											// class-level: xml
											for (Element ele : node2attrs_class.keySet()) {
												Element parent = ele.getParent();
												if (parent == null)
													continue;
												Element copyedEle = ele.createCopy();
												// class-level: 1. modify copied class index
												for (Object attr0 : copyedEle.attributes()) {
													if (attr0 == null)
														continue;
													Attribute attr = (Attribute) attr0;
													if (attr.getValue().equals(staticFullTarClass)) {
														attr.setValue(addClassName);
														break;
													}
												}
												// class-level: 2. modify class xml alias value
												for (Object attr0 : copyedEle.attributes()) {
													if (attr0 == null)
														continue;
													Attribute attr = (Attribute) attr0;
													String value0 = attr.getValue();
													if (value0.toLowerCase().equals(sameVal)) {
														String originalValue_field = field_anno_memval.getValue();
														String originalValue_class = value0;
														// set to same value
														field_anno_memval.setValue(newVal);
														field_attr.addAnnotation(field_anno);
														attr.setValue(newVal);
														parent.add(copyedEle);
														// record
														ArrayList<String> tmp_Configs = new ArrayList<>();
														ArrayList<String> tmp_body = new ArrayList<>();
														tmp_Configs.add(field_anno.getTypeName());
														tmp_Configs.add(attr.getPath());
														tmp_body.add(addFieldName);
														tmp_body.add(addClassName);
														// if the statement is getField/setField, also change the statement
														String stmt2 = stmt;
														String str1 = stmt.substring(0, stmt.indexOf('('));
														if (str1.toLowerCase().endsWith(field.getName().toLowerCase())) {
															String str2 = str1.substring(str1.lastIndexOf('.'));
															String prefix = str2.substring(0, str2.length() - field.getName().length());
															stmt2 = str1.substring(0, stmt.lastIndexOf('.')) + prefix
																	+ addFieldName.substring(0, 1).toUpperCase() + addFieldName.substring(1)
																	+ stmt.substring(stmt.indexOf('('));
														}
														tmp_body.add(stmt2); // check position, a function call statement
//														modifiedRecord.add(new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2, Modification.add,
//																tmp_Configs, tmp_body, true));
//														url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(new ModifiedInfo(fieldSig, -1,
//																ModifiedSemantic.Field_Point2, Modification.add, tmp_Configs, tmp_body, true),
//																modifiedRecord.size());
														INDEX++;
														url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(new ModifiedInfo(fieldSig, -1,
																ModifiedSemantic.Field_Point2, Modification.add, tmp_Configs, tmp_body, true), INDEX);
														// write multiple files
														// write to file
														String outSubPath1 = calSubPath(tar_cc.getName(), targetClassFilePath);
														String outSubPath2 = calSubPath(field_cc.getName(), field_classFile);
														if (isJar) {
															ArrayList<CtClass> ctClasses = new ArrayList<>();
															ArrayList<String> subPaths = new ArrayList<>();
															ArrayList<Document> documents = new ArrayList<>();
															ArrayList<String> xmlFiles = new ArrayList<>();
															ctClasses.add(addTarClass);
															ctClasses.add(field_cc);
															subPaths.add(outSubPath1);
															subPaths.add(outSubPath2);
															documents.add(document);
															xmlFiles.add(xmlFile);
															write2JarFiles(ctClasses, subPaths, documents, xmlFiles);

														} else {
															write2SingleXMLFile(document, xmlFile);
															write2SingleClassFile(addTarClass, outSubPath1);
															write2SingleClassFile(field_cc, outSubPath2);
														}
														writeRequestSequence();
														// recover configuration value
														parent.remove(copyedEle);/* recover */
														attr.setValue(originalValue_class);
														field_anno_memval.setValue(originalValue_field);
														field_attr.addAnnotation(field_anno);
													}
												}
											}
										}
									}
								} catch (DocumentException e) {
									e.printStackTrace();
								}
							}

						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					/* 1.c field-xml & class-anno */
					if (!sameVals_classanno_fieldxml.isEmpty()) {
						try (DataInputStream dis_field = new DataInputStream(new FileInputStream(clFile_field))) {
							ClassFile classFile_field = new ClassFile(dis_field);
							CtClass field_cc = pool.makeClass(classFile_field);
							CtField field = SearchInCtClassHelper.findCtField(field_cc, fieldName);
							if (field == null)
								continue;
							String addFieldName = "mhnField_" + field.getName();

							// modify field_cc and copy tar_cc
							CtClass addTarClass = CtObjectCopyHelper.addNewClassCopy(tar_cc, addClassName, false);
							CtField addfield = CtObjectCopyHelper.addNewFieldCopy(field_cc, field, addFieldName, true, addTarClass); // add a new field in to
																																		// given class

							AnnotationsAttribute tarClassAttr = (AnnotationsAttribute) addTarClass.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
							if (tarClassAttr == null)
								continue;

							for (String xmlFile : containsClass2XMLFile) {
								SAXReader reader = new SAXReader();
								reader.setIgnoreComments(true);
								reader.setValidation(false);
								reader.setEntityResolver(new EntityResolver() {
									@Override
									public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
										return new InputSource(new ByteArrayInputStream("".getBytes()));
									}
								});
								try {
									Document field_document = reader.read(xmlFile);
									Element field_root = field_document.getRootElement();
									HashMap<Element, HashSet<Attribute>> node2attrs_fields = new HashMap<>();
									ConfigurationCollector.collectFieldXMLConfiguration(field_root, fieldSig, xmlFile, false, node2attrs_fields);

									for (String sameVal : sameVals_classanno_fieldxml) {
										String newVal = "mhn_" + sameVal;
										// field-level:xml
										for (Element field_ele : node2attrs_fields.keySet()) {
											Element field_parent = field_ele.getParent();
											if (field_parent == null)
												continue;
											Element field_copyedEle = field_ele.createCopy();

											HashSet<Attribute> fieldPointers = new HashSet<>();
											HashSet<Attribute> fieldNameAttrs = new HashSet<>();
											for (Object attr0 : field_copyedEle.attributes()) {
												if (attr0 == null)
													continue;
												Attribute attr = (Attribute) attr0;
												String value0 = attr.getValue();
												// find the field name configured attributes
												if (value0.toLowerCase().equals(fieldName.toLowerCase())) {
													fieldNameAttrs.add(attr);
												}
												// find the field pointer configured attributes
												if (value0.toLowerCase().equals(sameVal)) {
													fieldPointers.add(attr);
												}
											}
											// for each field, modify its possible pointer attributes
											for (Attribute fieldNameAttr : fieldNameAttrs) {
												for (Attribute fieldPointer : fieldPointers) {
													if (fieldNameAttr.getPath().equals(fieldPointer.getPath()))
														continue;

													// class-level:anno
													for (String tarAnnoAttrName : val2AnnotationType_class.get(sameVal)) {
														Annotation class_anno = tarClassAttr.getAnnotation(tarAnnoAttrName);
														if (class_anno == null)
															continue;
														StringMemberValue class_anno_memval = SearchInCtClassHelper.findAnnotationMemberValue(class_anno,
																sameVal);// class-key
														if (class_anno_memval == null)
															continue;
														String originalValue_class = class_anno_memval.getValue();
														String originalValue_fieldPointer = fieldPointer.getValue();
														String originalValue_fieldName = fieldNameAttr.getValue();
														// 1. set to same value
														class_anno_memval.setValue(newVal);
														tarClassAttr.addAnnotation(class_anno);
														fieldPointer.setValue(newVal);
														fieldNameAttr.setValue(addFieldName);
														field_parent.add(field_copyedEle);
														// 2. record
														ArrayList<String> tmp_Configs = new ArrayList<>();
														ArrayList<String> tmp_body = new ArrayList<>();
														tmp_Configs.add(fieldPointer.getPath());
														tmp_Configs.add(class_anno.getTypeName());
														tmp_body.add(addFieldName);
														tmp_body.add(addClassName);
														// if the statement is getField/setField, also change the statement
														String stmt2 = stmt;
														String str1 = stmt.substring(0, stmt.indexOf('('));
														if (str1.toLowerCase().endsWith(field.getName().toLowerCase())) {
															String str2 = str1.substring(str1.lastIndexOf('.'));
															String prefix = str2.substring(0, str2.length() - field.getName().length());
															stmt2 = str1.substring(0, stmt.lastIndexOf('.')) + prefix
																	+ addFieldName.substring(0, 1).toUpperCase() + addFieldName.substring(1)
																	+ stmt.substring(stmt.indexOf('('));
														}
														tmp_body.add(stmt2); // check position, a function call statement
//														modifiedRecord.add(new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2, Modification.add,
//																tmp_Configs, tmp_body, true));
//														url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(new ModifiedInfo(fieldSig, -1,
//																ModifiedSemantic.Field_Point2, Modification.add, tmp_Configs, tmp_body, true),
//																modifiedRecord.size());
														INDEX++;
														url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(new ModifiedInfo(fieldSig, -1,
																ModifiedSemantic.Field_Point2, Modification.add, tmp_Configs, tmp_body, true), INDEX);
														// 3. write to file
														String outSubPath1 = calSubPath(tar_cc.getName(), targetClassFilePath);
														String outSubPath2 = calSubPath(field_cc.getName(), field_classFile);
														if (isJar) {
															ArrayList<CtClass> ctClasses = new ArrayList<>();
															ArrayList<String> subPaths = new ArrayList<>();
															ArrayList<Document> documents = new ArrayList<>();
															ArrayList<String> xmlFiles = new ArrayList<>();
															ctClasses.add(addTarClass);
															subPaths.add(outSubPath1);
															ctClasses.add(field_cc);
															subPaths.add(outSubPath2);
															documents.add(field_document);
															xmlFiles.add(xmlFile);
															write2JarFiles(ctClasses, subPaths, documents, xmlFiles);
														} else {
															write2SingleClassFile(addTarClass, outSubPath1);
															write2SingleClassFile(field_cc, outSubPath2);
															write2SingleXMLFile(field_document, xmlFile);
														}
														writeRequestSequence();
														// 4. recover configuration value
														class_anno_memval.setValue(originalValue_class);
														tarClassAttr.addAnnotation(class_anno);
														fieldPointer.setValue(originalValue_fieldPointer);
														fieldNameAttr.setValue(originalValue_fieldName);
														field_parent.remove(field_copyedEle);
													}
												}
											}
										}
									}
								} catch (DocumentException e) {
									e.printStackTrace();
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					/* 1.d field-xml & class-xml */ /* 2.b field-xml & class-name */
					if (!sameVals_classxml_fieldxml.isEmpty() || (val2XMLEle_field.containsKey(staticTarClassName.toLowerCase())
							|| val2XMLEle_field.containsKey(staticFullTarClass.toLowerCase()))) {
						try (DataInputStream dis_field = new DataInputStream(new FileInputStream(clFile_field))) {
							ClassFile classFile_field = new ClassFile(dis_field);
							CtClass field_cc = pool.makeClass(classFile_field);
							CtField field = SearchInCtClassHelper.findCtField(field_cc, fieldName);
							if (field == null)
								continue;
							String addFieldName = "mhnField_" + field.getName();

							// modify field_cc and copy tar_cc
							CtClass addTarClass = CtObjectCopyHelper.addNewClassCopy(tar_cc, addClassName, true);
							CtField addfield = CtObjectCopyHelper.addNewFieldCopy(field_cc, field, addFieldName, true, addTarClass);// just add field

							// field level:xml
							for (String field_xmlFile : containsClass2XMLFile) {
								SAXReader reader = new SAXReader();
								reader.setIgnoreComments(true);
								reader.setValidation(false);
								reader.setEntityResolver(new EntityResolver() {
									@Override
									public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
										return new InputSource(new ByteArrayInputStream("".getBytes()));
									}
								});
								try {
									Document field_document = reader.read(field_xmlFile);
									Element field_root = field_document.getRootElement();
									HashMap<Element, HashSet<Attribute>> node2attrs_fields = new HashMap<>();
									ConfigurationCollector.collectFieldXMLConfiguration(field_root, fieldSig, field_xmlFile, false, node2attrs_fields);

									/* 1.d field-xml & class-xml */
									if (class2XMLFile.containsKey(staticFullTarClass)) {
										for (String sameVal : sameVals_classxml_fieldxml) {
											String newVal = "mhn_" + sameVal;
											// field-level:xml
											for (Element field_ele : node2attrs_fields.keySet()) {
												Element field_parent = field_ele.getParent();
												if (field_parent == null)
													continue;
												Element field_copyedEle = field_ele.createCopy();
												HashSet<Attribute> fieldPointers = new HashSet<>();
												HashSet<Attribute> fieldNameAttrs = new HashSet<>();
												for (Object fieldattr0 : field_copyedEle.attributes()) {
													if (fieldattr0 == null)
														continue;
													Attribute field_attr = (Attribute) fieldattr0;
													String field_value = field_attr.getValue();

													// find the field name configured attributes
													if (field_value.toLowerCase().equals(fieldName.toLowerCase())) {
														fieldNameAttrs.add(field_attr);
													}
													// find the field pointer configured attributes
													if (field_value.toLowerCase().equals(sameVal)) {
														fieldPointers.add(field_attr);
													}
												}

												// for each field, modify its possible pointer attributes
												for (Attribute fieldNameAttr : fieldNameAttrs) {
													for (Attribute fieldPointer : fieldPointers) {
														if (fieldNameAttr.getPath().equals(fieldPointer.getPath()))
															continue;

														// class-level: xml
														for (String class_xmlFile : class2XMLFile.get(staticFullTarClass)) {
															if (class_xmlFile.equals(field_xmlFile)) {
																HashMap<Element, HashSet<Attribute>> node2attrs_class = new HashMap<>();
																ConfigurationCollector.collectClassXMLConfiguration(field_root, staticFullTarClass,
																		node2attrs_class);
																for (Element ele : node2attrs_class.keySet()) {
																	Element class_parent = ele.getParent();
																	if (class_parent == null)
																		continue;
																	Element class_copyedEle = ele.createCopy();
																	// 1. modify copied class index
																	for (Object classattr0 : class_copyedEle.attributes()) {
																		if (classattr0 == null)
																			continue;
																		Attribute attr = (Attribute) classattr0;
																		if (attr.getValue().equals(staticFullTarClass)) {
																			attr.setValue(addClassName);
																			break;
																		}
																	}
																	// 2. modify class xml alias value
																	for (Object classattr0 : class_copyedEle.attributes()) {
																		if (classattr0 == null)
																			continue;
																		Attribute class_attr = (Attribute) classattr0;
																		String class_value = class_attr.getValue();
																		if (class_value.toLowerCase().equals(sameVal)) {
																			String originalValue_class = class_value;
																			String originalValue_fieldPointer = fieldPointer.getValue();
																			String originalValue_fieldName = fieldNameAttr.getValue();

																			// set to same value
																			class_attr.setValue(newVal);
																			class_parent.add(class_copyedEle);
																			fieldPointer.setValue(newVal);
																			fieldNameAttr.setValue(addFieldName);
																			field_parent.add(field_copyedEle);
																			// record
																			ArrayList<String> tmp_Configs = new ArrayList<>();
																			ArrayList<String> tmp_body = new ArrayList<>();
																			tmp_Configs.add(fieldPointer.getPath());
																			tmp_Configs.add(class_attr.getPath());
																			tmp_body.add(addFieldName);
																			tmp_body.add(addClassName);
																			// if the statement is getField/setField, also change the statement
																			String stmt2 = stmt;
																			String str1 = stmt.substring(0, stmt.indexOf('('));
																			if (str1.toLowerCase().endsWith(field.getName().toLowerCase())) {
																				String str2 = str1.substring(str1.lastIndexOf('.'));
																				String prefix = str2.substring(0, str2.length() - field.getName().length());
																				stmt2 = str1.substring(0, stmt.lastIndexOf('.')) + prefix
																						+ addFieldName.substring(0, 1).toUpperCase() + addFieldName.substring(1)
																						+ stmt.substring(stmt.indexOf('('));
																			}
																			tmp_body.add(stmt2); // check position, a function call statement
//																			modifiedRecord.add(new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2,
//																					Modification.add, tmp_Configs, tmp_body, true));
//																			url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
//																					new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2,
//																							Modification.add, tmp_Configs, tmp_body, true),
//																					modifiedRecord.size());
																			INDEX++;
																			url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
																					new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2,
																							Modification.add, tmp_Configs, tmp_body, true),
																					INDEX);
																			// write to file
																			String outSubPath1 = calSubPath(tar_cc.getName(), targetClassFilePath);
																			String outSubPath2 = calSubPath(field_cc.getName(), field_classFile);
																			if (isJar) {
																				ArrayList<CtClass> ctClasses = new ArrayList<>();
																				ArrayList<String> subPaths = new ArrayList<>();
																				ArrayList<Document> documents = new ArrayList<>();
																				ArrayList<String> xmlFiles = new ArrayList<>();
																				ctClasses.add(addTarClass);
																				subPaths.add(outSubPath1);
																				ctClasses.add(field_cc);
																				subPaths.add(outSubPath2);
																				documents.add(field_document);
																				xmlFiles.add(field_xmlFile);
																				write2JarFiles(ctClasses, subPaths, documents, xmlFiles);
																			} else {
																				write2SingleXMLFile(field_document,
																						field_xmlFile);/* since field and class in the same file */
																				write2SingleClassFile(addTarClass, outSubPath1);
																				write2SingleClassFile(field_cc, outSubPath2);
																			}
																			writeRequestSequence();
																			// recover configuration value
																			class_parent.remove(class_copyedEle);/* recover */
																			class_attr.setValue(originalValue_class);/* recover */
																			fieldPointer.setValue(originalValue_fieldPointer);
																			fieldNameAttr.setValue(originalValue_fieldName);
																			field_parent.remove(field_copyedEle);
																		}
																	}
																}
															} else {
																SAXReader reader2 = new SAXReader();
																reader2.setValidation(false);
																reader2.setEntityResolver(new EntityResolver() {
																	@Override
																	public InputSource resolveEntity(String publicId, String systemId)
																			throws SAXException, IOException {
																		return new InputSource(new ByteArrayInputStream("".getBytes()));
																	}
																});
																try {
																	Document class_document = reader.read(class_xmlFile);
																	Element class_root = class_document.getRootElement();
																	HashMap<Element, HashSet<Attribute>> node2attrs_class = new HashMap<>();
																	ConfigurationCollector.collectClassXMLConfiguration(class_root, staticFullTarClass,
																			node2attrs_class);

																	for (Element ele : node2attrs_class.keySet()) {
																		Element class_parent = ele.getParent();
																		if (class_parent == null)
																			continue;
																		Element class_copyedEle = ele.createCopy();
																		// 1. modify copied class index
																		for (Object classattr0 : class_copyedEle.attributes()) {
																			if (classattr0 == null)
																				continue;
																			Attribute attr = (Attribute) classattr0;
																			if (attr.getValue().equals(staticFullTarClass)) {
																				attr.setValue(addClassName);
																				break;
																			}
																		}
																		// 2. modify class xml alias value
																		for (Object classattr0 : class_copyedEle.attributes()) {
																			if (classattr0 == null)
																				continue;
																			Attribute class_attr = (Attribute) classattr0;
																			String class_value = class_attr.getValue();
																			if (!class_value.toLowerCase().equals(sameVal))
																				continue;
																			String originalValue_class = class_value;
																			String originalValue_fieldPointer = fieldPointer.getValue();
																			String originalValue_fieldName = fieldNameAttr.getValue();
																			// set to same value
																			class_attr.setValue(newVal);
																			class_parent.add(class_copyedEle);
																			fieldPointer.setValue(newVal);
																			fieldNameAttr.setValue(addFieldName);
																			field_parent.add(field_copyedEle);
																			// record
																			ArrayList<String> tmp_Configs = new ArrayList<>();
																			ArrayList<String> tmp_body = new ArrayList<>();
																			tmp_Configs.add(fieldPointer.getPath());
																			tmp_Configs.add(class_attr.getPath());
																			tmp_body.add(addFieldName);
																			tmp_body.add(addClassName);
																			// if the statement is getField/setField, also change the statement
																			String stmt2 = stmt;
																			String str1 = stmt.substring(0, stmt.indexOf('('));
																			if (str1.toLowerCase().endsWith(field.getName().toLowerCase())) {
																				String str2 = str1.substring(str1.lastIndexOf('.'));
																				String prefix = str2.substring(0, str2.length() - field.getName().length());
																				stmt2 = str1.substring(0, stmt.lastIndexOf('.')) + prefix
																						+ addFieldName.substring(0, 1).toUpperCase() + addFieldName.substring(1)
																						+ stmt.substring(stmt.indexOf('('));
																			}
																			tmp_body.add(stmt2); // check position, a function call statement
//																			modifiedRecord.add(new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2,
//																					Modification.add, tmp_Configs, tmp_body, true));
//																			url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
//																					new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2,
//																							Modification.add, tmp_Configs, tmp_body, true),
//																					modifiedRecord.size());
																			INDEX++;
																			url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
																					new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2,
																							Modification.add, tmp_Configs, tmp_body, true),
																					INDEX);
																			// write multiple files
																			String outSubPath1 = calSubPath(tar_cc.getName(), targetClassFilePath);
																			String outSubPath2 = calSubPath(field_cc.getName(), field_classFile);
																			if (isJar) {
																				ArrayList<CtClass> ctClasses = new ArrayList<>();
																				ArrayList<String> subPaths = new ArrayList<>();
																				ArrayList<Document> documents = new ArrayList<>();
																				ArrayList<String> xmlFiles = new ArrayList<>();
																				ctClasses.add(addTarClass);
																				subPaths.add(outSubPath1);
																				ctClasses.add(field_cc);
																				subPaths.add(outSubPath2);
																				documents.add(field_document);
																				xmlFiles.add(field_xmlFile);
																				documents.add(class_document);
																				xmlFiles.add(class_xmlFile);
																				write2JarFiles(ctClasses, subPaths, documents, xmlFiles);
																			} else {
																				// write to file
																				write2SingleXMLFile(field_document, field_xmlFile);
																				write2SingleXMLFile(class_document, class_xmlFile);
																				write2SingleClassFile(addTarClass, outSubPath1);
																				write2SingleClassFile(field_cc, outSubPath2);
																			}
																			writeRequestSequence();
																			// recover configuration value
																			class_parent.remove(class_copyedEle);/* recover */
																			class_attr.setValue(originalValue_class);/* recover */
																			fieldPointer.setValue(originalValue_fieldPointer);
																			fieldNameAttr.setValue(originalValue_fieldName);
																			field_parent.remove(field_copyedEle);
																		}
																	}
																} catch (DocumentException e) {
																	e.printStackTrace();
																}
															}
														}
													}
												}
											}
										}
									}
									/* 2.b field-xml & class-name */
									HashSet<String> sameClassNames = new HashSet<>();
									if (val2XMLEle_field.containsKey(staticTarClassName.toLowerCase()))
										sameClassNames.add(staticTarClassName);
									if (val2XMLEle_field.containsKey(staticFullTarClass.toLowerCase()))
										sameClassNames.add(staticFullTarClass);

									// class-level: class name
									for (String className : sameClassNames) {
										String newVal = addClassSimpleName;
										if (className.contains("."))
											newVal = addClassName;

										// field-level: xml
										for (Element field_ele : node2attrs_fields.keySet()) {
											Element field_parent = field_ele.getParent();
											if (field_parent == null)
												continue;
											Element field_copyedEle = field_ele.createCopy();
											HashSet<Attribute> fieldPointers = new HashSet<>();
											HashSet<Attribute> fieldNameAttrs = new HashSet<>();
											for (Object fieldattr0 : field_copyedEle.attributes()) {
												if (fieldattr0 == null)
													continue;
												Attribute field_attr = (Attribute) fieldattr0;
												String field_value = field_attr.getValue();

												// find the field name configured attributes
												if (field_value.toLowerCase().equals(fieldName.toLowerCase())) {
													fieldNameAttrs.add(field_attr);
												}
												// find the field pointer configured attributes
												if (field_value.toLowerCase().equals(className.toLowerCase())) {
													fieldPointers.add(field_attr);
												}
											}

											// for each field, modify its possible pointer attributes
											for (Attribute fieldNameAttr : fieldNameAttrs) {
												for (Attribute fieldPointer : fieldPointers) {
													if (fieldNameAttr.getPath().equals(fieldPointer.getPath()))
														continue;

													String originalValue_fieldPointer = fieldPointer.getValue();
													String originalValue_fieldName = fieldNameAttr.getValue();
													// set to class name
													fieldPointer.setValue(newVal);
													fieldNameAttr.setValue(addFieldName);
													field_parent.add(field_copyedEle);
													// record
													ArrayList<String> tmp_Configs = new ArrayList<>();
													ArrayList<String> tmp_body = new ArrayList<>();
													tmp_Configs.add(fieldPointer.getPath());
													tmp_Configs.add("[className]");
													tmp_body.add(addFieldName);
													tmp_body.add(addClassName);
													// if the statement is getField/setField, also change the statement
													String stmt2 = stmt;
													String str1 = stmt.substring(0, stmt.indexOf('('));
													if (str1.toLowerCase().endsWith(field.getName().toLowerCase())) {
														String str2 = str1.substring(str1.lastIndexOf('.'));
														String prefix = str2.substring(0, str2.length() - field.getName().length());
														stmt2 = str1.substring(0, stmt.lastIndexOf('.')) + prefix + addFieldName.substring(0, 1).toUpperCase()
																+ addFieldName.substring(1) + stmt.substring(stmt.indexOf('('));
													}
													tmp_body.add(stmt2); // check position, a function call statement
//													modifiedRecord.add(new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2, Modification.add,
//															tmp_Configs, tmp_body, true));
//													url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(new ModifiedInfo(fieldSig, -1,
//															ModifiedSemantic.Field_Point2, Modification.add, tmp_Configs, tmp_body, true),
//															modifiedRecord.size());
													INDEX++;
													url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(new ModifiedInfo(fieldSig, -1,
															ModifiedSemantic.Field_Point2, Modification.add, tmp_Configs, tmp_body, true), INDEX);
													// wirte to multiple files
													String outSubPath1 = calSubPath(tar_cc.getName(), targetClassFilePath);
													String outSubPath2 = calSubPath(field_cc.getName(), field_classFile);
													if (isJar) {
														ArrayList<CtClass> ctClasses = new ArrayList<>();
														ArrayList<String> subPaths = new ArrayList<>();
														ArrayList<Document> documents = new ArrayList<>();
														ArrayList<String> xmlFiles = new ArrayList<>();
														ctClasses.add(addTarClass);
														subPaths.add(outSubPath1);
														ctClasses.add(field_cc);
														subPaths.add(outSubPath2);
														documents.add(field_document);
														xmlFiles.add(field_xmlFile);
														write2JarFiles(ctClasses, subPaths, documents, xmlFiles);
													} else {
														// write to file
														write2SingleXMLFile(field_document, field_xmlFile);
														write2SingleClassFile(addTarClass, outSubPath1);
														write2SingleClassFile(field_cc, outSubPath2);
													}
													writeRequestSequence();
													// recover configuration value
													fieldPointer.setValue(originalValue_fieldPointer);
													fieldNameAttr.setValue(originalValue_fieldName);
													field_parent.remove(field_copyedEle);
												}
											}
										}
									}
								} catch (DocumentException e) {
									e.printStackTrace();
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					/* 2.a field-anno & class-name */
					if (val2AnnotationType_field.containsKey(staticTarClassName.toLowerCase())
							|| val2AnnotationType_field.containsKey(staticFullTarClass.toLowerCase())) {
						try (DataInputStream dis_field = new DataInputStream(new FileInputStream(clFile_field))) {
							ClassFile classFile_field = new ClassFile(dis_field);
							CtClass field_cc = pool.makeClass(classFile_field);
							CtField field = SearchInCtClassHelper.findCtField(field_cc, fieldName);
							if (field == null)
								continue;
							String addFieldName = "mhnField_" + field.getName();

							// modify field_cc and copy tar_cc
							CtClass addTarClass = CtObjectCopyHelper.addNewClassCopy(tar_cc, addClassName, true);
							CtField addfield = CtObjectCopyHelper.addNewFieldCopy(field_cc, field, addFieldName, false, addTarClass);

							AnnotationsAttribute field_attr = (AnnotationsAttribute) addfield.getFieldInfo().getAttribute(AnnotationsAttribute.visibleTag);
							if (field_attr == null)
								continue;

							HashSet<String> sameClassNames = new HashSet<>();
							if (val2AnnotationType_field.containsKey(staticTarClassName.toLowerCase()))
								sameClassNames.add(staticTarClassName);
							if (val2AnnotationType_field.containsKey(staticFullTarClass.toLowerCase()))
								sameClassNames.add(staticFullTarClass);

							// class-level: class name
							for (String className : sameClassNames) {
								String newVal = addClassSimpleName;
								if (className.contains("."))
									newVal = addClassName;

								if (val2AnnotationType_field.get(className.toLowerCase()) != null) {
									// field-level
									for (String fieldAnnoAttrName : val2AnnotationType_field.get(className.toLowerCase())) {
										Annotation field_anno = field_attr.getAnnotation(fieldAnnoAttrName);
										if (field_anno == null)
											continue;
										StringMemberValue field_anno_memval = SearchInCtClassHelper.findAnnotationMemberValue(field_anno,
												className.toLowerCase());// field-key
										if (field_anno_memval == null)
											continue;
										String originalValue_field = field_anno_memval.getValue();
										// set to class name
										field_anno_memval.setValue(newVal);
										field_attr.addAnnotation(field_anno);
										// record
										ArrayList<String> tmp_Configs = new ArrayList<>();
										ArrayList<String> tmp_body = new ArrayList<>();
										tmp_Configs.add(field_anno.getTypeName());
										tmp_Configs.add("[className]");
										tmp_body.add(addFieldName);
										tmp_body.add(addClassName);
										// if the statement is getField/setField, also change the statement
										String stmt2 = stmt;
										String str1 = stmt.substring(0, stmt.indexOf('('));
										if (str1.toLowerCase().endsWith(field.getName().toLowerCase())) {
											String str2 = str1.substring(str1.lastIndexOf('.'));
											String prefix = str2.substring(0, str2.length() - field.getName().length());
											stmt2 = str1.substring(0, stmt.lastIndexOf('.')) + prefix + addFieldName.substring(0, 1).toUpperCase()
													+ addFieldName.substring(1) + stmt.substring(stmt.indexOf('('));
										}
										tmp_body.add(stmt2); // check position, a function call statement
//										modifiedRecord.add(
//												new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2, Modification.add, tmp_Configs, tmp_body, true));
//										url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
//												new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2, Modification.add, tmp_Configs, tmp_body, true),
//												modifiedRecord.size());
										INDEX++;
										url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
												new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2, Modification.add, tmp_Configs, tmp_body, true),
												INDEX);
										// write to multiple files
										String outSubPath1 = calSubPath(tar_cc.getName(), targetClassFilePath);
										String outSubPath2 = calSubPath(field_cc.getName(), field_classFile);
										if (isJar) {
											ArrayList<CtClass> ctClasses = new ArrayList<>();
											ArrayList<String> subPaths = new ArrayList<>();
											ArrayList<Document> documents = new ArrayList<>();
											ArrayList<String> xmlFiles = new ArrayList<>();
											ctClasses.add(addTarClass);
											subPaths.add(outSubPath1);
											ctClasses.add(field_cc);
											subPaths.add(outSubPath2);
											write2JarFiles(ctClasses, subPaths, documents, xmlFiles);
										} else {
											// write to file
											write2SingleClassFile(addTarClass, outSubPath1);
											write2SingleClassFile(field_cc, outSubPath2);
										}
										writeRequestSequence();
										// recover configuration value
										field_anno_memval.setValue(originalValue_field);
										field_attr.addAnnotation(field_anno);
									}
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					/* 3.a field-name & class-anno */
					if (val2AnnotationType_class.containsKey(fieldName.toLowerCase())) {
						try (DataInputStream dis_field = new DataInputStream(new FileInputStream(clFile_field))) {
							ClassFile classFile_field = new ClassFile(dis_field);
							CtClass field_cc = pool.makeClass(classFile_field);
							CtField field = SearchInCtClassHelper.findCtField(field_cc, fieldName);
							if (field == null)
								continue;
							String addFieldName = "mhnField_" + field.getName();

							// modify field_cc and copy tar_cc
							CtClass addTarClass = CtObjectCopyHelper.addNewClassCopy(tar_cc, addClassName, false);
//							CtField addfield = CtObjectCopyHelper.addNewFieldCopy(field_cc, field, addFieldName, true, addTarClass);
							CtField addfield = CtObjectCopyHelper.addNewFieldCopy(field_cc, field, addFieldName, false, addTarClass);
							AnnotationsAttribute tarClassAttr = (AnnotationsAttribute) addTarClass.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
							if (tarClassAttr == null)
								continue;

							// field-level: field name
							// newVal = addFieldName
							for (String tarAnnoAttrName : val2AnnotationType_class.get(fieldName.toLowerCase())) {
								Annotation class_anno = tarClassAttr.getAnnotation(tarAnnoAttrName);
								if (class_anno == null)
									continue;
								StringMemberValue class_anno_memval = SearchInCtClassHelper.findAnnotationMemberValue(class_anno, fieldName.toLowerCase());// class-key
								if (class_anno_memval == null)
									continue;

								String originalValue_class = class_anno_memval.getValue();/* same value are lower-case */
								// set to new field name
								class_anno_memval.setValue(addFieldName);
								tarClassAttr.addAnnotation(class_anno);
								// record
								ArrayList<String> tmp_Configs = new ArrayList<>();
								ArrayList<String> tmp_body = new ArrayList<>();
								tmp_Configs.add("[fieldName]");
								tmp_Configs.add(class_anno.getTypeName());
								tmp_body.add(addFieldName);
								tmp_body.add(addClassName);
								// if the statement is getField/setField, also change the statement
								String stmt2 = stmt;
								String str1 = stmt.substring(0, stmt.indexOf('('));
								if (str1.toLowerCase().endsWith(field.getName().toLowerCase())) {
									String str2 = str1.substring(str1.lastIndexOf('.'));
									String prefix = str2.substring(0, str2.length() - field.getName().length());
									stmt2 = str1.substring(0, stmt.lastIndexOf('.')) + prefix + addFieldName.substring(0, 1).toUpperCase()
											+ addFieldName.substring(1) + stmt.substring(stmt.indexOf('('));
								}
								tmp_body.add(stmt2); // check position, a function call statement
//								modifiedRecord
//										.add(new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2, Modification.add, tmp_Configs, tmp_body, true));
//								url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
//										new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2, Modification.add, tmp_Configs, tmp_body, true),
//										modifiedRecord.size());
								INDEX++;
								url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
										new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2, Modification.add, tmp_Configs, tmp_body, true), INDEX);
								// write to multiple files
								String outSubPath1 = calSubPath(tar_cc.getName(), targetClassFilePath);
								String outSubPath2 = calSubPath(field_cc.getName(), field_classFile);
								if (isJar) {
									ArrayList<CtClass> ctClasses = new ArrayList<>();
									ArrayList<String> subPaths = new ArrayList<>();
									ArrayList<Document> documents = new ArrayList<>();
									ArrayList<String> xmlFiles = new ArrayList<>();
									ctClasses.add(addTarClass);
									subPaths.add(outSubPath1);
									ctClasses.add(field_cc);
									subPaths.add(outSubPath2);
									write2JarFiles(ctClasses, subPaths, documents, xmlFiles);
								} else {
									// write to file
									write2SingleClassFile(addTarClass, outSubPath1);
									write2SingleClassFile(field_cc, outSubPath2);
								}
								writeRequestSequence();
								// recover configuration value
								class_anno_memval.setValue(originalValue_class);
								tarClassAttr.addAnnotation(class_anno);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					/* 3.b field-name & class-xml */
					if (val2XMLEle_class.containsKey(fieldName.toLowerCase())) {
						try (DataInputStream dis_field = new DataInputStream(new FileInputStream(clFile_field))) {
							ClassFile classFile_field = new ClassFile(dis_field);
							CtClass field_cc = pool.makeClass(classFile_field);
							CtField field = SearchInCtClassHelper.findCtField(field_cc, fieldName);
							if (field == null)
								continue;
							String addFieldName = "mhnField_" + field.getName();

							// modify field_cc and copy tar_cc
							CtClass addTarClass = CtObjectCopyHelper.addNewClassCopy(tar_cc, addClassName, true);
							CtField addfield = CtObjectCopyHelper.addNewFieldCopy(field_cc, field, addFieldName, true, addTarClass);

							if (!class2XMLFile.containsKey(staticFullTarClass))
								continue;
							// field-level: fieldName
							// newVal = addFieldName
							for (String xmlFile : class2XMLFile.get(staticFullTarClass)) {
								SAXReader reader = new SAXReader();
								reader.setIgnoreComments(true);
								reader.setValidation(false);
								reader.setEntityResolver(new EntityResolver() {
									@Override
									public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
										return new InputSource(new ByteArrayInputStream("".getBytes()));
									}
								});
								try {
									Document document = reader.read(xmlFile);
									Element root = document.getRootElement();
									HashMap<Element, HashSet<Attribute>> node2attrs_class = new HashMap<>();
									ConfigurationCollector.collectClassXMLConfiguration(root, staticFullTarClass, node2attrs_class);

									// class-level: xml
									for (Element ele : node2attrs_class.keySet()) {
										Element parent = ele.getParent();
										if (parent == null)
											continue;
										Element copyedEle = ele.createCopy();
										// class-level: 1. modify copied class index
										for (Object attr0 : copyedEle.attributes()) {
											if (attr0 == null)
												continue;
											Attribute attr = (Attribute) attr0;
											if (attr.getValue().equals(staticFullTarClass)) {
												attr.setValue(addClassName);
												break;
											}
										}
										// class-level: 2. modify class xml alias value
										for (Object attr0 : copyedEle.attributes()) {
											if (attr0 == null)
												continue;
											Attribute attr = (Attribute) attr0;
											String value0 = attr.getValue();
											if (value0.toLowerCase().equals(fieldName.toLowerCase())) {
												String originalValue_class = value0;
												// set to new field name
												attr.setValue(addFieldName);/**/
												parent.add(copyedEle);
												// record
												ArrayList<String> tmp_Configs = new ArrayList<>();
												ArrayList<String> tmp_body = new ArrayList<>();
												tmp_Configs.add("[fieldName]");
												tmp_Configs.add(attr.getPath());
												tmp_body.add(addFieldName);
												tmp_body.add(addClassName);
												// if the statement is getField/setField, also change the statement
												String stmt2 = stmt;
												String str1 = stmt.substring(0, stmt.indexOf('('));
												if (str1.toLowerCase().endsWith(field.getName().toLowerCase())) {
													String str2 = str1.substring(str1.lastIndexOf('.'));
													String prefix = str2.substring(0, str2.length() - field.getName().length());
													stmt2 = str1.substring(0, stmt.lastIndexOf('.')) + prefix + addFieldName.substring(0, 1).toUpperCase()
															+ addFieldName.substring(1) + stmt.substring(stmt.indexOf('('));
												}
												tmp_body.add(stmt2); // check position, a function call statement
//												modifiedRecord.add(new ModifiedInfo(fieldSig, -1, ModifiedSemantic.Field_Point2, Modification.add, tmp_Configs,
//														tmp_body, true));
//												url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(new ModifiedInfo(fieldSig, -1,
//														ModifiedSemantic.Field_Point2, Modification.add, tmp_Configs, tmp_body, true), modifiedRecord.size());
												INDEX++;
												url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(new ModifiedInfo(fieldSig, -1,
														ModifiedSemantic.Field_Point2, Modification.add, tmp_Configs, tmp_body, true), INDEX);
												// wirte to multiple files
												String outSubPath1 = calSubPath(tar_cc.getName(), targetClassFilePath);
												String outSubPath2 = calSubPath(field_cc.getName(), field_classFile);
												if (isJar) {
													ArrayList<CtClass> ctClasses = new ArrayList<>();
													ArrayList<String> subPaths = new ArrayList<>();
													ArrayList<Document> documents = new ArrayList<>();
													ArrayList<String> xmlFiles = new ArrayList<>();
													ctClasses.add(addTarClass);
													subPaths.add(outSubPath1);
													ctClasses.add(field_cc);
													subPaths.add(outSubPath2);
													documents.add(document);
													xmlFiles.add(xmlFile);
													write2JarFiles(ctClasses, subPaths, documents, xmlFiles);
												} else {
													// write to file
													write2SingleXMLFile(document, xmlFile);
													write2SingleClassFile(addTarClass, outSubPath1);
													write2SingleClassFile(field_cc, outSubPath2);
												}
												writeRequestSequence();
												// recover configuration value
												parent.remove(copyedEle);/* recover */
												attr.setValue(originalValue_class);
											}
										}
									}
								} catch (DocumentException e) {
									e.printStackTrace();
								}
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

//	protected boolean classDirAbsolutePathAdded = false;
	protected HashSet<String> insertedClassDirPath = new HashSet<>();
	protected String subDir = "";

	protected boolean dealWithField_Annotation(String classFilePath, String stmt, String fieldSig, ClassPool pool, ModifiedSemantic type,
			HashMap<String, Integer> visitedTags2Count) {
		boolean modified = false;
		if (classFilePath == null)
			return false;

		// directory
		File clFile = new File(classFilePath);
		try (DataInputStream dis = new DataInputStream(new FileInputStream(clFile))) {
			ClassFile classFile = new ClassFile(dis);
			CtClass cc = pool.makeClass(classFile);

			String name = classFile.getName();
			checkClassDirAbsolutePathAdded(clFile.getAbsolutePath(), name, pool);

			String fieldFullName = fieldSig.substring(0, fieldSig.indexOf(':'));
			String fieldName = fieldFullName.substring(fieldFullName.lastIndexOf('.') + 1);
			String name2 = fieldFullName.substring(0, fieldFullName.lastIndexOf('.'));

			if (!name.equals(name2))
				return false;

			if (cc.isFrozen()) {
				cc.defrost();
			}

			CtField field = SearchInCtClassHelper.findCtField(cc, fieldName);
			// field level
			if (field != null) {
				modified = modifyOnField_annotation(field, fieldSig, cc, classFilePath, stmt, type, visitedTags2Count);
			}
		} catch (IOException e) {
			System.err.println("[IOException]Modifing Annotations in Directory");
			e.printStackTrace();
		}
		return modified;
	}

	/**
	 * solve a method and its declare class annotation configurations</br>
	 * 
	 * @param urlEnable : whether enable matching url
	 */
	protected boolean dealWithMethod_Annotation(String classFilePath, String stmt, ClassPool pool, ModifiedSemantic type, boolean onlyMethod,
			ArrayList<String> otherinfo, HashMap<String, Integer> visitedTags2Count, boolean urlEnable) {
		boolean modified = false;
		if (classFilePath == null)
			return false;

		try {
			DataInputStream dis;
			FileInputStream inputStream = new FileInputStream(new File(classFilePath));
			dis = new DataInputStream(inputStream);
			ClassFile classFile = new ClassFile(dis);
			CtClass cc = pool.makeClass(classFile);

			String name = classFile.getName();
//			String className = name.replaceAll("\\.", "/");
			String className = name.replace('.', '/');

			File clFile = new File(classFilePath);
			String abPath = clFile.getAbsolutePath();
			checkClassDirAbsolutePathAdded(abPath, name, pool);

			String name2 = stmt.substring(0, stmt.lastIndexOf('.'));
			if (!name.equals(name2))
				return modified;

			if (ConfigureUtil.isApplicationClass(className)) {
				CtMethod method = SearchInCtClassHelper.findCtMethod(cc, stmt);
				// 1. method level
				if (method != null) {
					modified = modifyOnMethod_annotation(method, classFile, classFilePath, stmt, type, otherinfo, visitedTags2Count, urlEnable);
				}
				if (onlyMethod)
					return modified;

				// 2. class level
				modified = modifyOnClass_annotation(classFile, classFilePath, stmt, type, otherinfo, visitedTags2Count, urlEnable);
			}
		} catch (IOException e) {
			System.err.println("[IOException]Modifing Annotations in Directory");
			e.printStackTrace();
		}
		return modified;
	}

	/**
	 * find which xml attr is key configuration
	 */
	protected boolean dealWithField_XML(String classFilePath, String stmt, String fieldSig, ClassPool pool, String xmlFile, ModifiedSemantic type,
			HashMap<String, Integer> visitedTags2Count) {
		boolean modified = false;

		SAXReader reader = new SAXReader();
		reader.setIgnoreComments(true);
		reader.setValidation(false);
		reader.setEntityResolver(new EntityResolver() {
			@Override
			public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
				return new InputSource(new ByteArrayInputStream("".getBytes()));
			}
		});

		try {
			Document document = reader.read(xmlFile);
			/* exclude database configure */
			DocumentType docType = document.getDocType();
			if (docType != null)
				if (XMLConfigurationUtil.notConcernedXMLID(docType.getPublicID()) || XMLConfigurationUtil.notConcernedXMLID(docType.getSystemID()))
					return modified;

			Element root = document.getRootElement();

			// 1. collect all node needs to modified for this stmt
			/* attribute value may be null, means text */
			HashMap<Element, HashSet<Attribute>> node2attrs_fields = new HashMap<>();
			ConfigurationCollector.collectFieldXMLConfiguration(root, fieldSig, xmlFile, false, node2attrs_fields);
			// 2. for each : mutate and write to file, then recover
			if (node2attrs_fields.size() > 0 && classFilePath != null) {
				modified = modifyOnField_XML(fieldSig, document, node2attrs_fields, classFilePath, stmt, pool, xmlFile, type, visitedTags2Count);
			}
		} catch (DocumentException e) {
			System.err.println("[error][DocumentException]" + e.getMessage() + " when parse " + xmlFile);
		}

		return modified;
	}

	/**
	 * solve a method and its declare class xml configurations</br>
	 * 
	 * @param visitedTags2Count
	 * @param urlEnable         : whether enable match url
	 * 
	 */
	protected boolean dealWithMethod_XML(String classFilePath, String stmt, ClassPool pool, String xmlFile, ModifiedSemantic type, ArrayList<String> otherinfo,
			HashMap<String, Integer> visitedTags2Count, boolean urlEnable) {
		boolean modified = false;

		SAXReader reader = new SAXReader();
		reader.setIgnoreComments(true);
		reader.setValidation(false);
		reader.setEntityResolver(new EntityResolver() {
			@Override
			public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
				return new InputSource(new ByteArrayInputStream("".getBytes()));
			}
		});
		try {
			Document document = reader.read(xmlFile);
			/* exclude database configure */
			DocumentType docType = document.getDocType();
			if (docType != null)
				if (XMLConfigurationUtil.notConcernedXMLID(docType.getPublicID()) || XMLConfigurationUtil.notConcernedXMLID(docType.getSystemID()))
					return modified;

			Element root = document.getRootElement();

			// 1. collect all node needs to modified for this stmt
			/* attribute value may be null, means text */
			HashMap<Element, HashSet<Attribute>> node2attrs_class = new HashMap<>();
			HashMap<Element, HashSet<Attribute>> node2attrs_method = new HashMap<>();
			ConfigurationCollector.collectMethodXMLConfiguration(root, stmt, xmlFile, false, node2attrs_class, node2attrs_method);
			// 2. for each :remove and write to file, then recover
			// class
			modified = modifyOnClass_XML(document, node2attrs_class, classFilePath, stmt, pool, xmlFile, type, otherinfo, visitedTags2Count, urlEnable);
			// method
			boolean ret = modifyOnMethod_XML(document, node2attrs_method, classFilePath, stmt, pool, xmlFile, type, otherinfo, visitedTags2Count, urlEnable);
			modified = modified || ret;

		} catch (DocumentException e) {
			System.err.println("[error][DocumentException]" + e.getMessage() + " when parse " + xmlFile);
		}

		return modified;
	}

	protected void removeXMLElementRecordRecover(Document document, Element ele, ModifiedInfo methodModifiedInfo, String xmlFile) {
		Element parentEle = ele.getParent();
		boolean flag = parentEle.remove(ele);
		if (flag) {
//			modifiedRecord.add(methodModifiedInfo);
//			url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(methodModifiedInfo, modifiedRecord.size());
			INDEX++;
			url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(methodModifiedInfo, INDEX);
			// write
			write2SingleXMLFile(document, xmlFile);
			writeRequestSequence();
			// recover
			parentEle.add(ele);
		} else {
			System.out.println("remove failed! in file " + xmlFile);
		}

	}

	protected void write2SingleXMLFile(Document document, String xmlFile) {
		if (isJar) {
			// write xml in byte[] format
			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				XMLWriter xmlWriter = new XMLWriter(outputStream, OutputFormat.createCompactFormat());
				xmlWriter.write(document);
				xmlWriter.close();
				byte[] byteOut = outputStream.toByteArray();

				File tF = new File(xmlFile);
				String tfpath = tF.getAbsolutePath();
				String relativePath = tfpath.substring(appPath.length() + 1);
				String tarXMLname = relativePath.replace(File.separatorChar, '/');/* the xml path internal jar */

//				String xmlDirPath = outPath + File.separator + modifiedRecord.size();
				String xmlDirPath = outPath + File.separator + INDEX;
				File f = new File(xmlDirPath);
				if (!f.exists())
					f.mkdirs();
				JarHandler jarHandler = new JarHandler();
				jarHandler.replaceSingleJarFile(jarPath, byteOut, tarXMLname, "-updated", xmlDirPath);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else {
			File f1 = new File(xmlFile);
			File f2 = new File(appPath);
			// TODO: may exception in other OS
			String relativePath = f1.getAbsolutePath().substring(f2.getAbsolutePath().length());
//			String res = outPath + File.separator + modifiedRecord.size() + relativePath;
			String res = outPath + File.separator + INDEX + relativePath;
			String dir = res.substring(0, res.lastIndexOf('\\'));
			File d = new File(dir);
			if (!d.exists())
				d.mkdirs();
			try {
				File file = new File(res);
				if (!file.exists())
					file.createNewFile();
				XMLWriter writer = new XMLWriter(new FileOutputStream(file));
				writer.write(document);
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected boolean write2JarFiles(ArrayList<CtClass> ctclasses, ArrayList<String> subPaths, ArrayList<Document> documents, ArrayList<String> xmlFiles) {
		boolean modified = false;
		ArrayList<byte[]> fileByteCodes = new ArrayList<>();
		ArrayList<String> fileNames = new ArrayList<>();
		// 1. build classes
		for (int i = 0; i < ctclasses.size(); i++) {
			try {
				CtClass cc = ctclasses.get(i);
				byte[] b = cc.toBytecode();

				String subPath = subPaths.get(i);
				String className = cc.getName();
				String tarClassname = subPath.replace(File.separatorChar, '/') + "/" + className.replace('.', '/') + ".class";

				fileByteCodes.add(b);
				fileNames.add(tarClassname);
			} catch (CannotCompileException | IOException e) {
				e.printStackTrace();
			}
		}

		// 2. build xml if need
		for (int i = 0; i < documents.size(); i++) {
			Document document = documents.get(i);
			String xmlFile = xmlFiles.get(i);
			if (document != null) {
				// write xml in byte[] format
				try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
					XMLWriter xmlWriter = new XMLWriter(outputStream, OutputFormat.createCompactFormat());
					xmlWriter.write(document);
					xmlWriter.close();
					byte[] byteOut = outputStream.toByteArray();

					File tF = new File(xmlFile);
					String tfpath = tF.getAbsolutePath();
					String relativePath = tfpath.substring(appPath.length() + 1);
					String tarXMLname = relativePath.replace(File.separatorChar, '/');/* the xml path internal jar */

					fileByteCodes.add(byteOut);
					fileNames.add(tarXMLname);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		// 3. write
		String classesDirPath = outPath + File.separator + INDEX;
		File f = new File(classesDirPath);
		if (!f.exists())
			f.mkdirs();

		JarHandler jarHandler = new JarHandler();
		try {
			jarHandler.replaceMultiJarFiles(jarPath, fileByteCodes, fileNames, "-updated", classesDirPath);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// 4. defrost
		for (CtClass cc : ctclasses)
			cc.defrost();
		return modified;
	}

	/**
	 * write to file and defrost</br>
	 * outPath depends on modifiedRecord.size() and outPath
	 */
	protected boolean write2SingleClassFile(CtClass cc, String subPath) {
		boolean modified = false;
		if (subPath == null)
			subPath = subDir;
		try {
			if (isJar) {
				byte[] b = cc.toBytecode();
				String className = cc.getName();
//				String tarClassname = "BOOT-INF/classes/" + className.replace('.', '/') + ".class";
				String tarClassname = subPath.replace(File.separatorChar, '/') + "/" + className.replace('.', '/') + ".class";
//				String classesDirPath = outPath + File.separator + modifiedRecord.size();
				String classesDirPath = outPath + File.separator + INDEX;
				File f = new File(classesDirPath);
				if (!f.exists())
					f.mkdirs();
				JarHandler jarHandler = new JarHandler();
				jarHandler.replaceSingleJarFile(jarPath, b, tarClassname, "-updated", classesDirPath);
				cc.defrost();
				modified = true;
			} else {
//				String classesDirPath = outPath + File.separator + modifiedRecord.size() + File.separator + subPath;
				String classesDirPath = outPath + File.separator + INDEX + File.separator + subPath;
				cc.writeFile(classesDirPath);
				cc.defrost();
				modified = true;
			}
		} catch (CannotCompileException | IOException e) {
			e.printStackTrace();
		}
		return modified;
	}

	/** write the request sequence to trigger this testcase */
	protected void writeRequestSequence() {
//		String path = outPath + File.separator + modifiedRecord.size() + File.separator + "requestsTrigger.txt";
		String path = outPath + File.separator + INDEX + File.separator + "requestsTrigger.txt";

		FileOutputStream out = null;
		OutputStreamWriter outWriter = null;
		BufferedWriter buffer = null;
		try {
			out = new FileOutputStream(new File(path));
			outWriter = new OutputStreamWriter(out, "UTF-8");
			buffer = new BufferedWriter(outWriter);

			for (int i = 0; i < request.size(); i++) {
				buffer.append(request.get(i) + "\n");
				buffer.append(response.get(i) + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {

			if (buffer != null)
				try {
					buffer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (outWriter != null)
				try {
					outWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (out != null)
				try {
					out.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		}
	}

	protected void checkClassDirAbsolutePathAdded(String path1, String name, ClassPool pool) {
//		if (!classDirAbsolutePathAdded) {
		String clazz = name.replace('.', File.separatorChar);
		String prefixPath = path1.substring(0, path1.indexOf(clazz) - 1);
		if (insertedClassDirPath.contains(prefixPath))
			return;
		try {
			pool.insertClassPath(prefixPath); /* insert the classes prefix path */
//				classDirAbsolutePathAdded = true;
			File tF = new File(appPath);
			String tfpath = tF.getAbsolutePath();
			subDir = prefixPath.substring(tfpath.length() + 1);
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
//		}
	}

	/**
	 * @param fullClass0: like com.example.ClassA
	 */
	protected String findMatchedClassFile(String fullClass0) {
		String ret = null;
		char sep = File.separatorChar;
//		if (isJar)
//			sep = '/';
		for (String f : classFiles) {
			String f2 = f.replace(sep, '.');
			if (f2.endsWith(fullClass0 + ".class")) {
				ret = f;
				return ret;
			}
		}
		return ret;
	}

	/** not case sensitive */
	protected void collectAllAttrValues_field(String classFilePath, String fieldSig, ClassPool pool, HashSet<String> xmlFiles,
			HashMap<String, HashSet<String>> val2AnnotationType, HashMap<String, HashSet<String>> val2xmlEle, HashSet<String> values) {
		String fieldFullName = fieldSig.substring(0, fieldSig.indexOf(':'));
		String fieldName = fieldFullName.substring(fieldFullName.lastIndexOf('.') + 1);

		// annotation
		if (classFilePath != null) {
			File clFile = new File(classFilePath);
			try (DataInputStream dis = new DataInputStream(new FileInputStream(clFile))) {
				ClassFile classFile = new ClassFile(dis);
				CtClass cc = pool.makeClass(classFile);

				CtField field = SearchInCtClassHelper.findCtField(cc, fieldName);
				if (field != null) {
					FieldInfo info = field.getFieldInfo();
					AttributeInfo fieldAttrInfo = info.getAttribute(AnnotationsAttribute.visibleTag);
					if (fieldAttrInfo instanceof AnnotationsAttribute) {
						AnnotationsAttribute fieldAttr = (AnnotationsAttribute) fieldAttrInfo;
						for (Annotation anno : fieldAttr.getAnnotations()) {
							String annoname = anno.getTypeName();
							if (ConfigureUtil.isApplicationClass(annoname.replace('.', '/')))
								continue;
							Set<String> mems = anno.getMemberNames();
							if (mems != null) {
								for (String mem : mems) {
									MemberValue val = anno.getMemberValue(mem);
									if (val instanceof StringMemberValue) {
										String value0 = ((StringMemberValue) val).getValue();
										String value = value0.toLowerCase();
										values.add(value);
										// add
										if (val2AnnotationType.containsKey(value)) {
											val2AnnotationType.get(value).add(anno.getTypeName());
										} else {
											HashSet<String> tmp = new HashSet<String>();
											tmp.add(anno.getTypeName());
											val2AnnotationType.put(value, tmp);
										}
									}
								}
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// xml
		for (String xmlFile : xmlFiles) {
			SAXReader reader = new SAXReader();
			reader.setIgnoreComments(true);
			reader.setValidation(false);
			reader.setEntityResolver(new EntityResolver() {
				@Override
				public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
					return new InputSource(new ByteArrayInputStream("".getBytes()));
				}
			});
			try {
				Document document = reader.read(xmlFile);
				Element root = document.getRootElement();

				HashMap<Element, HashSet<Attribute>> node2attrs_fields = new HashMap<>();
				ConfigurationCollector.collectFieldXMLConfiguration(root, fieldSig, xmlFile, false, node2attrs_fields);

				for (Element ele : node2attrs_fields.keySet()) {
					for (Object attr0 : ele.attributes()) {
						if (attr0 == null)
							continue;
						Attribute attr = (Attribute) attr0;
						String value0 = attr.getValue();
						if (value0.length() > 0) {
							String value = value0.toLowerCase();
							values.add(value);
							if (val2xmlEle.containsKey(value)) {
								val2xmlEle.get(value).add(attr.getPath());
							} else {
								HashSet<String> tmp = new HashSet<>();
								tmp.add(attr.getPath());
								val2xmlEle.put(value, tmp);
							}
						}
					}
				}
			} catch (DocumentException e) {
				e.printStackTrace();
			}
		}
	}

	/** not case sensitive */
	protected void collectAllAttrValues_class(String actualTarClass, ClassPool pool, HashMap<String, HashSet<String>> val2AnnotationType,
			HashMap<String, HashSet<String>> val2xmlEle, HashSet<String> values) {
		String staticClassName = actualTarClass;
		if (actualTarClass.contains("$$")) {
			// is dynamic generated
			staticClassName = actualTarClass.substring(0, actualTarClass.indexOf("$$"));
		}

		// anno
		String classFilePath = findMatchedClassFile(staticClassName);
		if (classFilePath != null) {
			File clFile = new File(classFilePath);
			try (DataInputStream dis = new DataInputStream(new FileInputStream(clFile))) {
				ClassFile classFile = new ClassFile(dis);
				CtClass cc = pool.makeClass(classFile);

				AttributeInfo classAttrInfo = cc.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
				if (classAttrInfo != null) {
					if (classAttrInfo instanceof AnnotationsAttribute) {
						AnnotationsAttribute classAttr = (AnnotationsAttribute) classAttrInfo;
						for (Annotation anno : classAttr.getAnnotations()) {
							String annoname = anno.getTypeName();
							if (ConfigureUtil.isApplicationClass(annoname.replace('.', '/')))
								continue;
							Set<String> mems = anno.getMemberNames();
							if (mems != null) {
								for (String mem : mems) {
									MemberValue val = anno.getMemberValue(mem);
									if (val instanceof StringMemberValue) {
										String value0 = ((StringMemberValue) val).getValue();
										String value = value0.toLowerCase();
										// adds
										if (!val2AnnotationType.containsKey(value))
											val2AnnotationType.put(value, new HashSet<>());
										val2AnnotationType.get(value).add(anno.getTypeName());
										values.add(value);
									}
								}
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// xml
		if (class2XMLFile.keySet().contains(staticClassName)) {
			for (String xmlFile : class2XMLFile.get(staticClassName)) {
				SAXReader reader = new SAXReader();
				reader.setIgnoreComments(true);
				reader.setValidation(false);
				reader.setEntityResolver(new EntityResolver() {
					@Override
					public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
						return new InputSource(new ByteArrayInputStream("".getBytes()));
					}
				});
				try {
					Document document = reader.read(xmlFile);
					Element root = document.getRootElement();

					HashMap<Element, HashSet<Attribute>> node2attrs_class = new HashMap<>();
					ConfigurationCollector.collectClassXMLConfiguration(root, staticClassName, node2attrs_class);

					for (Element ele : node2attrs_class.keySet()) {
						for (Object attr0 : ele.attributes()) {
							if (attr0 == null)
								continue;
							Attribute attr = (Attribute) attr0;
							String value0 = attr.getValue();
							String value = value0.toLowerCase();
							if (!val2xmlEle.containsKey(value))
								val2xmlEle.put(value, new HashSet<>());
							val2xmlEle.get(value).add(attr.getPath());
							values.add(value);
						}
					}
				} catch (DocumentException e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected String calSubPath(String name, String classFilePath) {
		String clazz = name.replace('.', File.separatorChar);
		String p1 = classFilePath.substring(0, classFilePath.indexOf(clazz) - 1);
		File tF = new File(appPath);
		String tfpath = tF.getAbsolutePath();
		String subPath = p1.substring(tfpath.length() + 1);
		return subPath;
	}

	public ArrayList<Url2ModificationInfo> getUrl2ModifyedInfo() {
		return url2ModifyedInfo;
	}

	/** mutate the annotation on class: remove */
	protected boolean modifyOnClass_annotation(ClassFile classFile, String classFilePath, String stmt, ModifiedSemantic type, ArrayList<String> otherinfo,
			HashMap<String, Integer> visitedTags2Count, boolean urlEnable) {
		boolean modified = false;
		CtClass cc = ClassPool.getDefault().makeClass(classFile);
		AttributeInfo classAttrInfo = cc.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
		if (classAttrInfo != null) {
			if (classAttrInfo instanceof AnnotationsAttribute) {
				AnnotationsAttribute classAttr = (AnnotationsAttribute) classAttrInfo;
				HashSet<Annotation> candidateMtdAnnos = new HashSet<>();

				// 1.1. collect annos on this class
				if (classAttr.getAnnotations().length > 0) {
					for (Annotation anno : classAttr.getAnnotations()) {
						String annoname = anno.getTypeName();
						if (!ConfigureUtil.isApplicationClass(annoname.replace('.', '/')))
							candidateMtdAnnos.add(anno);
					}
				}
				// 1.2 remove and record each annotation
				for (Annotation anno : candidateMtdAnnos) {
					if (cc.isFrozen()) {
						cc.defrost();
					}

					// remove duplicate
					if (!visitedTags2Count.containsKey(anno.getTypeName()))
						visitedTags2Count.put(anno.getTypeName(), 0);
					if (visitedTags2Count.get(anno.getTypeName()) > thresdhold)
						continue;
					visitedTags2Count.put(anno.getTypeName(), visitedTags2Count.get(anno.getTypeName()) + 1);

					// 1. record, modify current annotation and write
					ArrayList<String> tmp_Configs = new ArrayList<>();
					ArrayList<String> tmp_body = new ArrayList<>();
					tmp_Configs.add(anno.getTypeName());
					if (otherinfo != null && !otherinfo.isEmpty())
						tmp_body.add(otherinfo.get(0));
					tmp_body.add(stmt);
//					modifiedRecord.add(new ModifiedInfo(stmt, 0, type, Modification.remove, tmp_Configs, tmp_body, false));
//					url2ModifyedInfo.get(url2ModifyedInfo.size() - 1)
//							.addModifiedRecord(new ModifiedInfo(stmt, 0, type, Modification.remove, tmp_Configs, tmp_body, false), modifiedRecord.size());
					INDEX++;
					url2ModifyedInfo.get(url2ModifyedInfo.size() - 1)
							.addModifiedRecord(new ModifiedInfo(stmt, 0, type, Modification.remove, tmp_Configs, tmp_body, false), INDEX);
					classAttr.removeAnnotation(anno.getTypeName());
					String outSubPath1 = calSubPath(cc.getName(), classFilePath);
					boolean success = write2SingleClassFile(cc, outSubPath1);
					writeRequestSequence();
					if (success)
						System.out.println(" [anno]" + anno.getTypeName() + " on class: " + cc.getName());
					// 2. recover this annotation
					classAttr.addAnnotation(anno);
					modified = true;
				}
			}
		}
		return modified;
	}

	/**
	 * mutate the annotation on method: remove
	 * 
	 * @param urlEnable : whether enable url match when modifying
	 */
	protected boolean modifyOnMethod_annotation(CtMethod method, ClassFile classFile, String classFilePath, String stmt, ModifiedSemantic type,
			ArrayList<String> otherinfo, HashMap<String, Integer> visitedTags2Count, boolean urlEnable) {
		boolean modified = false;
		MethodInfo info = method.getMethodInfo();
		AttributeInfo mtdAttrInfo = info.getAttribute(AnnotationsAttribute.visibleTag);
		if (mtdAttrInfo instanceof AnnotationsAttribute) {
			// is annotation, now modify!
			HashSet<Annotation> candidateMtdAnnos = new HashSet<>();

			// 2.1. collect annos on this method
			AnnotationsAttribute mtdAttr = (AnnotationsAttribute) mtdAttrInfo;
			if (mtdAttr.getAnnotations().length > 0) {
				for (Annotation anno : mtdAttr.getAnnotations()) {
					String annoname = anno.getTypeName();
					if (!ConfigureUtil.isApplicationClass(annoname.replace('.', '/')))
						candidateMtdAnnos.add(anno);
				}
			}

			// 2.2 remove and record each annotation
			/* remove and write, then recover */
			for (Annotation anno : candidateMtdAnnos) {
				CtClass cc = ClassPool.getDefault().makeClass(classFile);
				if (cc.isFrozen()) {
					cc.defrost();
				}
				// remove duplicate
				if (!visitedTags2Count.containsKey(anno.getTypeName()))
					visitedTags2Count.put(anno.getTypeName(), 0);
				if (visitedTags2Count.get(anno.getTypeName()) > thresdhold)
					continue;
				visitedTags2Count.put(anno.getTypeName(), visitedTags2Count.get(anno.getTypeName()) + 1);

				// 1. modify current annotation and write
				ArrayList<String> tmp_Configs = new ArrayList<>();
				ArrayList<String> tmp_body = new ArrayList<>();
				tmp_Configs.add(anno.getTypeName());
				if (otherinfo != null && !otherinfo.isEmpty())
					tmp_body.add(otherinfo.get(0));
				tmp_body.add(stmt);
//				modifiedRecord.add(new ModifiedInfo(stmt, 1, type, Modification.remove, tmp_Configs, tmp_body, false));
//				url2ModifyedInfo.get(url2ModifyedInfo.size() - 1)
//						.addModifiedRecord(new ModifiedInfo(stmt, 1, type, Modification.remove, tmp_Configs, tmp_body, false), modifiedRecord.size());
				INDEX++;
				url2ModifyedInfo.get(url2ModifyedInfo.size() - 1)
						.addModifiedRecord(new ModifiedInfo(stmt, 1, type, Modification.remove, tmp_Configs, tmp_body, false), INDEX);
				mtdAttr.removeAnnotation(anno.getTypeName());
				String outSubPath1 = calSubPath(cc.getName(), classFilePath);
				boolean success = write2SingleClassFile(cc, outSubPath1);
				writeRequestSequence();
				if (success) {
					System.out.println("[anno]" + anno.getTypeName() + " on method: " + method.getLongName());
				}
				// 2. recover this annotation
				mtdAttr.addAnnotation(anno);
				modified = true;
			}
		}
		return modified;
	}

	/** mutate the annotation on field: remove */
	protected boolean modifyOnField_annotation(CtField field, String fieldSig, CtClass cc, String classFilePath, String stmt, ModifiedSemantic type,
			HashMap<String, Integer> visitedTags2Count) {
		boolean modified = false;

		FieldInfo info = field.getFieldInfo();
		AttributeInfo fieldAttrInfo = info.getAttribute(AnnotationsAttribute.visibleTag);
		if (fieldAttrInfo instanceof AnnotationsAttribute) {
			// is annotation, now modify!
			HashSet<Annotation> candidateFieldAnnos = new HashSet<>();

			// 1. collect annos on this method
			AnnotationsAttribute fieldAttr = (AnnotationsAttribute) fieldAttrInfo;
			if (fieldAttr.getAnnotations().length > 0) {
				for (Annotation anno : fieldAttr.getAnnotations()) {
					String annoname = anno.getTypeName();
					if (!ConfigureUtil.isApplicationClass(annoname.replace('.', '/')))
						candidateFieldAnnos.add(anno);
				}
			}

			// 2.2 remove and record each annotation
			/* remove and write, then recover */
			for (Annotation anno : candidateFieldAnnos) {
				String path = anno.getTypeName();
				// remove duplicate
				if (!visitedTags2Count.containsKey(path))
					visitedTags2Count.put(path, 0);
				if (visitedTags2Count.get(path) > thresdhold)
					continue;
				visitedTags2Count.put(path, visitedTags2Count.get(path) + 1);

				// 1. modify current annotation and write
				ArrayList<String> tmp_Configs = new ArrayList<>();
				ArrayList<String> tmp_body = new ArrayList<>();
				tmp_Configs.add(anno.getTypeName());
				tmp_body.add(fieldSig);
				tmp_body.add(stmt); // check position, a function call statement
//				modifiedRecord.add(new ModifiedInfo(fieldSig, -1, type, Modification.remove, tmp_Configs, tmp_body, false));
//				url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
//						new ModifiedInfo(fieldSig, -1, type, Modification.remove, tmp_Configs, tmp_body, false), modifiedRecord.size());
				INDEX++;
				url2ModifyedInfo.get(url2ModifyedInfo.size() - 1)
						.addModifiedRecord(new ModifiedInfo(fieldSig, -1, type, Modification.remove, tmp_Configs, tmp_body, false), INDEX);
				fieldAttr.removeAnnotation(anno.getTypeName());
				String outSubPath1 = calSubPath(cc.getName(), classFilePath);
				boolean success = write2SingleClassFile(cc, outSubPath1);
				writeRequestSequence();
				if (success) {
					System.out.println("[anno][remove]" + anno.getTypeName() + " on field: " + fieldSig);
					modified = true;
				}
				// 2. recover this annotation
				fieldAttr.addAnnotation(anno);
			}
		}
		return modified;
	}

	/**
	 * mutate the XML path on class: remove
	 */
	protected boolean modifyOnClass_XML(Document document, HashMap<Element, HashSet<Attribute>> node2attrs_class, String classFilePath, String stmt,
			ClassPool pool, String xmlFile, ModifiedSemantic type, ArrayList<String> otherinfo, HashMap<String, Integer> visitedTags2Count, boolean urlEnable) {
		boolean modified = false;

		for (Element ele : node2attrs_class.keySet()) {
			HashSet<Attribute> attrs = node2attrs_class.get(ele);
			// for each attribute
			for (Attribute attr : attrs) {
				String path;
				if (attr == null) {
					path = ele.getPath();
				} else {
					path = attr.getPath();
				}

				// remove duplicate
				if (!visitedTags2Count.containsKey(path))
					visitedTags2Count.put(path, 0);
				if (visitedTags2Count.get(path) > thresdhold)
					continue;
				visitedTags2Count.put(path, visitedTags2Count.get(path) + 1);
				// mutate, record, write and recover
				ArrayList<String> tmp_Configs = new ArrayList<>();
				ArrayList<String> tmp_body = new ArrayList<>();
				tmp_Configs.add(path);
				if (otherinfo != null && !otherinfo.isEmpty())
					tmp_body.add(otherinfo.get(0));
				tmp_body.add(stmt);
				removeXMLElementRecordRecover(document, ele, new ModifiedInfo(stmt, 0, type, Modification.remove, tmp_Configs, tmp_body, false), xmlFile);
				modified = true;
			}
		}
		return modified;
	}

	/**
	 * mutate the XML path on method: change the method xml-node-attr
	 * 
	 * @param enable: whether enable match url
	 */
	protected boolean modifyOnMethod_XML(Document document, HashMap<Element, HashSet<Attribute>> node2attrs_method, String classFilePath, String stmt,
			ClassPool pool, String xmlFile, ModifiedSemantic type, ArrayList<String> otherinfo, HashMap<String, Integer> visitedTags2Count, boolean urlEnable) {
		boolean modified = false;

		for (Element ele : node2attrs_method.keySet()) {
			HashSet<Attribute> attrs = node2attrs_method.get(ele);

			// for each matched method attr, copy a method and modify one xml attribute
			if (classFilePath != null) {
				File clFile = new File(classFilePath);
				try (DataInputStream dis = new DataInputStream(new FileInputStream(clFile))) {
					ClassFile classFile = new ClassFile(dis);
					CtClass cc = pool.makeClass(classFile);
					String name = classFile.getName();

					checkClassDirAbsolutePathAdded(clFile.getAbsolutePath(), name, pool);

					String name2 = stmt.substring(0, stmt.lastIndexOf('.'));
					if (!name.equals(name2))
						return modified;

					CtMethod method = SearchInCtClassHelper.findCtMethod(cc, stmt);
					if (method != null) {
						String newMtdName = "mhn_" + method.getName();
						CtMethod addMtd = CtObjectCopyHelper.addNewMethodCopy(cc, method, newMtdName, true);/* modify class */
						if (addMtd != null) {
							for (Attribute attr1 : attrs) {
								if (attr1 == null)
									continue;
								String path = attr1.getPath();
								// remove duplicate
								if (!visitedTags2Count.containsKey(path))
									visitedTags2Count.put(path, 0);
								if (visitedTags2Count.get(path) > thresdhold)
									continue;
								visitedTags2Count.put(path, visitedTags2Count.get(path) + 1);

								String originalVal = attr1.getValue();
								attr1.setValue(newMtdName); // modify current method attr points2
								// record
								ArrayList<String> tmp_Configs = new ArrayList<>();
								ArrayList<String> tmp_body = new ArrayList<>();
								tmp_Configs.add(attr1.getPath());
								if (otherinfo != null && !otherinfo.isEmpty())
									tmp_body.add(otherinfo.get(0));
								String stmt1 = stmt.substring(0, stmt.lastIndexOf('.')) + "." + newMtdName + stmt.substring(stmt.indexOf('('));
								tmp_body.add(stmt1);
//								tmp_body.add(newMtdName);
//								modifiedRecord.add(new ModifiedInfo(stmt, 1, type, Modification.add, tmp_Configs, tmp_body, true));
//								url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
//										new ModifiedInfo(stmt, 1, type, Modification.add, tmp_Configs, tmp_body, true), modifiedRecord.size());
								INDEX++;
								url2ModifyedInfo.get(url2ModifyedInfo.size() - 1)
										.addModifiedRecord(new ModifiedInfo(stmt, 1, type, Modification.add, tmp_Configs, tmp_body, true), INDEX);
								// write multiple files
								String outSubPath1 = calSubPath(cc.getName(), classFilePath);
								if (isJar) {
									ArrayList<CtClass> ctClasses = new ArrayList<>();
									ArrayList<String> subPaths = new ArrayList<>();
									ArrayList<Document> documents = new ArrayList<>();
									ArrayList<String> xmlFiles = new ArrayList<>();
									ctClasses.add(cc);
									subPaths.add(outSubPath1);
									documents.add(document);
									xmlFiles.add(xmlFile);
									write2JarFiles(ctClasses, subPaths, documents, xmlFiles);
								} else {
									// write to file
									// 1. xml write
									write2SingleXMLFile(document, xmlFile);
									attr1.setValue(originalVal);/* recover */
									// 2. class write
									// modify current annotation and write
									write2SingleClassFile(cc, outSubPath1);
								}
								writeRequestSequence();
								modified = true;
								break;
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return modified;
	}

	/**
	 * mutate the XML path on field: duplicate and change the method xml-node-attr
	 */
	protected boolean modifyOnField_XML(String fieldSig, Document document, HashMap<Element, HashSet<Attribute>> node2attrs_fields, String classFilePath,
			String stmt, ClassPool pool, String xmlFile, ModifiedSemantic type, HashMap<String, Integer> visitedTags2Count) {
		boolean modified = false;
		// 2.1 copy a field
		File clFile = new File(classFilePath);
		try (DataInputStream dis = new DataInputStream(new FileInputStream(clFile))) {
			ClassFile classFile = new ClassFile(dis);
			CtClass cc = pool.makeClass(classFile);
			String name = classFile.getName();

			checkClassDirAbsolutePathAdded(clFile.getAbsolutePath(), name, pool);

			String fieldFullName = fieldSig.substring(0, fieldSig.indexOf(':'));
			String fieldName = fieldFullName.substring(fieldFullName.lastIndexOf('.') + 1);

			CtField field = SearchInCtClassHelper.findCtField(cc, fieldName);
			if (field != null) {
				String newFieldName = "mhn_" + field.getName();

				CtField addfield = CtObjectCopyHelper.addNewFieldCopy(cc, field, newFieldName, true, null);
				if (addfield != null) {
					for (Element ele : node2attrs_fields.keySet()) {
						if (ele.getParent() == null)
							continue;
						Element copyedEle = ele.createCopy();
						for (Object attr0 : copyedEle.attributes()) {
							if (attr0 == null)
								continue;
							if (attr0 instanceof Attribute) {
								Attribute attr = (Attribute) attr0;
								if (attr.getValue().equals(fieldName)) { // field the fieldName xml-attr
									attr.setValue(newFieldName);
									ele.getParent().add(copyedEle);

									String path = attr.getPath();
									// remove duplicate
									if (!visitedTags2Count.containsKey(path))
										visitedTags2Count.put(path, 0);
									if (visitedTags2Count.get(path) > thresdhold) {
										ele.getParent().remove(copyedEle);
										continue;
									}
									visitedTags2Count.put(path, visitedTags2Count.get(path) + 1);

									// record
									ArrayList<String> tmp_Configs = new ArrayList<>();
									ArrayList<String> tmp_body = new ArrayList<>();
									tmp_Configs.add(attr.getPath());
									tmp_body.add(newFieldName);
									// if the statement is getField/setField, also change the statement
									String stmt2 = stmt;
									String str1 = stmt.substring(0, stmt.indexOf('('));
									if (str1.toLowerCase().endsWith(field.getName().toLowerCase())) {
										String str2 = str1.substring(str1.lastIndexOf('.'));
										String prefix = str2.substring(0, str2.length() - field.getName().length());
										stmt2 = str1.substring(0, stmt.lastIndexOf('.')) + prefix + newFieldName.substring(0, 1).toUpperCase()
												+ newFieldName.substring(1) + stmt.substring(stmt.indexOf('('));
									}
									tmp_body.add(stmt2); // check position, a function call statement
//									modifiedRecord.add(new ModifiedInfo(fieldSig, -1, type, Modification.add, tmp_Configs, tmp_body, true));
//									url2ModifyedInfo.get(url2ModifyedInfo.size() - 1).addModifiedRecord(
//											new ModifiedInfo(fieldSig, -1, type, Modification.add, tmp_Configs, tmp_body, true), modifiedRecord.size());
									INDEX++;
									url2ModifyedInfo.get(url2ModifyedInfo.size() - 1)
											.addModifiedRecord(new ModifiedInfo(fieldSig, -1, type, Modification.add, tmp_Configs, tmp_body, true), INDEX);
									// write multiple files
									String outSubPath = calSubPath(name, classFilePath);
									if (isJar) {
										ArrayList<CtClass> ctClasses = new ArrayList<>();
										ArrayList<String> subPaths = new ArrayList<>();
										ArrayList<Document> documents = new ArrayList<>();
										ArrayList<String> xmlFiles = new ArrayList<>();
										ctClasses.add(cc);
										subPaths.add(outSubPath);
										documents.add(document);
										xmlFiles.add(xmlFile);
										write2JarFiles(ctClasses, subPaths, documents, xmlFiles);
									} else {
										// write to file
										// 1. xml write
										write2SingleXMLFile(document, xmlFile);
										// 2. class write
										// modify current annotation and write
										write2SingleClassFile(cc, outSubPath);
									}
									writeRequestSequence();
									/* recover */
									ele.getParent().remove(copyedEle);
									attr.setValue(fieldName);
									modified = true;
								}
							}
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return modified;
	}
}
