package ict.pag.m.marks2SAInfer.staticInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeName;

import ict.pag.m.frameworkInfoUtil.customize.ConfigUtil;
import ict.pag.m.frameworkInfoUtil.customize.GraphBuilder;
import ict.pag.m.frameworkInfoUtil.customize.Util;
import ict.pag.m.frameworkInfoUtil.infoEntity.infoUnit;
import ict.pag.m.marks2SAInfer.util.reducedSetCollection;
import ict.pag.m.marks2SAInfer.util.resolveMarksUtil;

public class objectInstanceInfos {
	private GraphBuilder builder;
	private ClassHierarchy cha;
	private CHACallGraph chaCG;

	private Set<infoUnit> field_marks;
	private Set<infoUnit> method_marks;
	private Set<infoUnit> clazz_marks;
	private Set<String> entriesSigs;

	private Set<infoUnit> xmls_marks_class;
	private Set<infoUnit> xmls_marks_all;

	private Set<infoUnit> inheritance_marks;

	Set<String> instanceMarks = new HashSet<>();
	Set<String> instanceMarks_clazz = new HashSet<>();

	private reducedSetCollection managedClassMarks = new reducedSetCollection();
	private reducedSetCollection managedFieldMarks = new reducedSetCollection();
	private reducedSetCollection managedFunctionMarks = new reducedSetCollection();

	private reducedSetCollection managedSetdMarks = new reducedSetCollection();

	private Set<String> managedClass = new HashSet<>();

	public objectInstanceInfos(GraphBuilder builder2, Set<infoUnit> f_marks, Set<infoUnit> m_marks,
			Set<infoUnit> c_marks, Set<infoUnit> xml_marks, Set<infoUnit> xmlSet_all, Set<infoUnit> inheritanceSet) {
		this.xmls_marks_class = xml_marks;
		this.xmls_marks_all = xmlSet_all;

		builder = builder2;
		cha = builder.getCHA();
		chaCG = builder.getAppCHACG();

		field_marks = f_marks;
		method_marks = m_marks;
		clazz_marks = c_marks;

		inheritance_marks = inheritanceSet;

		entriesSigs = builder.getAllUnreachableEntryPoints();

		collectInstanceMarks();

	}

	private void collectInstanceMarks() {
		Set<String> instanceClazz = new HashSet<>();
		Set<String> entryInit = new HashSet<>();

		/** 1. for methods */
		// 1) all <init> method haven't be called
		// 2) all set method has marks
		cha.forEach(node -> {
			if (node.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
				if (node.isAbstract() || node.isInterface())
					return;

				// any <init> haven't called iff flag=true
				boolean flag = true;
				for (IMethod m : node.getDeclaredMethods()) {
					if (m == null)
						continue;
					if (m.getName().toString().equals("<init>")) {
						if (!entriesSigs.contains(m.getSignature())) {
							flag = false;
						}
					}
				}

				// whether methods except <init> have been called
				boolean normalMthdcalled = false;
				for (IMethod m : node.getDeclaredMethods()) {
					if (m == null || m.isStatic())
						continue;
					if (!entriesSigs.contains(m.getSignature().toString())) {
						normalMthdcalled = true;
						break;
					}
				}

				/**
				 * if satisfy situations below, the class is managed by framework</br>
				 * 1. any initial methods have not been called </br>
				 * 2. there is a methods exclude initial methods have been called
				 */
				if (flag && normalMthdcalled) {

					String clazzName = Util.format(node.getName().toString());
					managedClass.add(clazzName);
					// annotations
					ArrayList<String> need2add_anno = resolveMarksUtil.getClassAnnos_withDecorate(clazzName,
							clazz_marks, "class");
					if (!need2add_anno.isEmpty()) {
						managedClassMarks.add(new HashSet<>(need2add_anno));
					}
					// xml configured
					ArrayList<String> need2add_xml = resolveMarksUtil.getXMLMarks_withDecorate(clazzName,
							xmls_marks_class);
					if (!need2add_xml.isEmpty()) {
						managedClassMarks.add(new HashSet<>(need2add_xml));
					}
					// inheritance
					ArrayList<String> need2add_inhe = resolveMarksUtil.getInheritanceMarks_onclass(clazzName,
							inheritance_marks);
					if (!need2add_inhe.isEmpty()) {
						managedClassMarks.add(new HashSet<>(need2add_inhe));
					}
				}

				// all fields in this node
				Set<IField> allFields = new HashSet<>();
				allFields.addAll(node.getDeclaredInstanceFields());
				allFields.addAll(node.getDeclaredStaticFields());
				Collection<IField> totalFields = node.getAllFields();

				/* <field, putField or not in init/set functions> */
				HashMap<IField, Set<IMethod>> f2status = new HashMap<IField, Set<IMethod>>();
				// 1. collect where field has been stored

				chaCG.forEach(cgNode -> {
					if (cgNode.getMethod() == null)
						return;
					if (cgNode.getMethod().getDeclaringClass().equals(node)) {

						if (cgNode.getMethod().getName().toString().equals("<init>")) {
							calStore(f2status, allFields, cgNode);
						} else {
							calSetFieldFunc(f2status, allFields, cgNode);
						}

					}
				});
				// 2. if field has not been stored in init/set, then record field marks
				// else, record function marks
				for (IField f : f2status.keySet()) {
					Set<IMethod> status = f2status.get(f);
					if (status.isEmpty()) {
						// nowhere initialize f, record marks on f
						ArrayList<String> need2add = new ArrayList<>();
						// annotation
						ArrayList<String> annos_add = resolveMarksUtil.getClassAnnos_withDecorate(
								Util.format(f.getReference().getSignature()), field_marks, "field");
						if (!annos_add.isEmpty())
							need2add.addAll(annos_add);
						// xml
						TypeName f_class = f.getFieldTypeReference().getName();
						if ((f.getFieldTypeReference().getClassLoader().equals(ClassLoaderReference.Application)
								&& ConfigUtil.enableApplication) || ConfigUtil.isApplicationClass(f_class.toString())) {
							ArrayList<String> xml_add = resolveMarksUtil.getXMLMarks_field(f.getReference(),
									xmls_marks_all);
							if (!xml_add.isEmpty())
								need2add.addAll(xml_add);
						}
						// all
						if (!need2add.isEmpty()) {
							managedFieldMarks.add(new HashSet<>(need2add));
						}
					} else {
						boolean hasCaller = false;
						for (IMethod m : status) {
							if (!entriesSigs.contains(m.getSignature())) {
								hasCaller = true;
								ArrayList<String> need2add = new ArrayList<>();
								// annotation
								ArrayList<String> anno_add = resolveMarksUtil
										.getClassAnnos_withDecorate(m.getSignature(), method_marks, "mtd");
								if (!anno_add.isEmpty())
									need2add.addAll(anno_add);
								// xml
								ArrayList<String> xml_add = resolveMarksUtil.getXMLMarks_mthd(m.getSignature(),
										xmls_marks_all);
								if (!xml_add.isEmpty())
									need2add.addAll(xml_add);
								// all
								if (!need2add.isEmpty())
									managedFunctionMarks.add(new HashSet<>(need2add));
							}
						}
						if (!hasCaller) {
							ArrayList<String> need2add = new ArrayList<>();
							// annotation
							ArrayList<String> annos_add = resolveMarksUtil.getClassAnnos_withDecorate(
									Util.format(f.getReference().getSignature()), field_marks, "field");
							if (!annos_add.isEmpty())
								need2add.addAll(annos_add);
							// xml
							TypeName f_class = f.getFieldTypeReference().getName();
							if ((f.getFieldTypeReference().getClassLoader().equals(ClassLoaderReference.Application)
									&& ConfigUtil.enableApplication)
									|| ConfigUtil.isApplicationClass(f_class.toString())) {
								ArrayList<String> xml_add = resolveMarksUtil.getXMLMarks_field(f.getReference(),
										xmls_marks_all);
								if (!xml_add.isEmpty())
									need2add.addAll(xml_add);
							}
							// all
							if (!need2add.isEmpty()) {
								managedFieldMarks.add(new HashSet<>(need2add));
							}
						}
					}
				}

				// ------------

				for (IMethod m : node.getDeclaredMethods()) {
					if (m == null)
						continue;
					// <init>
					if (m.getName().toString().equals("<init>")) {
						if (flag) {
							// any <init> have not called
							entryInit.add(Util.format(node.getName().toString()));
							// add init method marks
							ArrayList<String> need2add = resolveMarksUtil.getClassAnnos_withDecorate(m.getSignature(),
									method_marks, "mtd");
//							// add class marks
//							need2add.addAll(resolveMarksUtil.getAnnosMarksOnly(m.getSignature(), clazz_marks));
							if (!need2add.isEmpty()) {
								instanceMarks.addAll(need2add);
								instanceClazz.add(Util.format(m.getDeclaringClass().getName().toString()));

							}
						}

					}

					if (flag)
						// set
						if (isSetFun(m, node)) {
							ArrayList<String> need2add = resolveMarksUtil.getClassAnnos_withDecorate(m.getSignature(),
									method_marks, "mtd");
							if (!need2add.isEmpty() && entriesSigs.contains(m.getSignature().toString())) {
								managedSetdMarks.add(new HashSet<>(need2add));
								instanceMarks.addAll(need2add);
								instanceClazz.add(Util.format(m.getDeclaringClass().getName().toString()));
							}
						}

				}
			}
		});
		System.out.println("1");
		/** 2. for fields */
		Set<String> allCodeClazz = Util.getAllCodeClazz(cha);
		for (infoUnit f : field_marks) {
			String base = f.getBase();
			String fieldType = base.substring(base.indexOf(" ") + 2);
			// if fieldType is user code class, record the class and find the marks
//			if (allCodeClazz.contains(fieldType) && entryInit.contains(fieldType)) {
			if (allCodeClazz.contains(fieldType)) {
				ArrayList<String> need2add = resolveMarksUtil.getClassAnnos_withDecorate(base, field_marks, "class");
				if (!need2add.isEmpty()) {
//					managedFieldMarks.add(new HashSet<>(need2add));
					instanceMarks.addAll(need2add);
					instanceClazz.add(fieldType);
				}
			}
		}
		// xml configured managed fields add in Main.calss
//		cha.forEach(nodeClazz->{
//			if (!nodeClazz.getClassLoader().getReference().equals(ClassLoaderReference.Application))
//				return;
//			
//			String node_c = Util.format(nodeClazz.getName().toString());
//			
//			Set<IField> allFields = new HashSet<>();
//			allFields.addAll(nodeClazz.getDeclaredInstanceFields());
//			allFields.addAll(nodeClazz.getDeclaredStaticFields());
//			for (IField f : allFields) {
//				String fieldType = Util.format(f.getReference().getFieldType().getName().toString());
//				String fieldSig = Util.format(f.getReference().getSignature());
//				String fieldName = f.getName().toString();
//
//				if (!ConfigUtil.isApplicationClass(fieldType))
//					continue;
//				
//				
//			}
//			
//		});

		/** 3. for class: 1-2 collect all marks of the object class */
		// the class that <init> have not called, and its marks in 1-2 collections
		cha.forEach(node -> {
			if (node.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
				String clazzName = Util.format(node.getName().toString());
				// the class have marks in 1-2
				// the all <init> of this class are not be called
				if (instanceClazz.contains(clazzName) && entryInit.contains(clazzName)) {
					ArrayList<String> need2add = resolveMarksUtil.getClassAnnos_withDecorate(clazzName, clazz_marks,
							"mtd");
					if (!need2add.isEmpty()) {
						// TODO: 这里需要修改，精简集合，找到关键标记
						instanceMarks_clazz.addAll(need2add);
					}
				}

			}
		});

	}

	// 1. set + field name
	// 2. putFieldInstruction
	private boolean calSetFieldFunc(HashMap<IField, Set<IMethod>> f2status, Set<IField> allFields, CGNode cgNode) {
		String mtdName = cgNode.getMethod().getName().toString();
		if (!mtdName.toLowerCase().startsWith("set"))
			return false;

		calStore(f2status, allFields, cgNode);

		return false;
	}

	private void calStore(HashMap<IField, Set<IMethod>> f2status, Set<IField> allFields, CGNode cgNode) {
		IR ir = cgNode.getIR();
		if (ir == null)
			return;

		SSAInstruction[] allInsts = ir.getInstructions();
		HashSet<FieldReference> fieldReferences = new HashSet<>();
		for (SSAInstruction inst : allInsts) {
			if (inst == null)
				continue;
			if (inst instanceof SSAPutInstruction) {
				FieldReference fieldRef = ((SSAPutInstruction) inst).getDeclaredField();
				fieldReferences.add(fieldRef);
			}
		}

		for (IField f : allFields) {
			Set<IMethod> statusSet;
			if (f2status.containsKey(f)) {
				statusSet = f2status.get(f);
			} else {
				statusSet = new HashSet<IMethod>();
			}

			FieldReference f_ref = f.getReference();

			for (FieldReference ffr : fieldReferences) {
				if (ffr.equals(f_ref)) {
					statusSet.add(cgNode.getMethod());
					break;
				}
			}
			f2status.put(f, statusSet);
		}
	}

	// set field function: set + fieldName
	private boolean isSetFun(IMethod m, IClass node) {
		String pattern = "set[A-Z].*";
		Pattern r = Pattern.compile(pattern);
		if (r.matcher(m.getName().toString()).matches()) {
			Set<IField> allFields = new HashSet<>();
			allFields.addAll(node.getDeclaredInstanceFields());
			allFields.addAll(node.getDeclaredStaticFields());

			Set<String> allFieldsNames = new HashSet<String>();
			for (IField f : allFields) {
				String f_name = f.getName().toString();
				allFieldsNames.add(f_name.toLowerCase().replaceAll("_", ""));
			}

			String tmp = m.getName().toString().substring(3);
			if (allFieldsNames.contains(tmp.toLowerCase().replaceAll("_", "")))
				return true;
		}

		return false;
	}

	public Set<String> getInstanceMarks() {
		return instanceMarks;
	}

	public Set<String> getInstanceMarks_clazz() {
		return instanceMarks_clazz;
	}

	public reducedSetCollection getManagedClassMarks() {
		return managedClassMarks;
	}

	public reducedSetCollection getManagedFieldMarks() {
		return managedFieldMarks;
	}

	public reducedSetCollection getManagedSetdMarks() {
		return managedSetdMarks;
	}

	public reducedSetCollection getManagedFunctionMarks() {
		return managedFunctionMarks;
	}

	public Set<String> getManagedClass() {
		return managedClass;
	}

}
