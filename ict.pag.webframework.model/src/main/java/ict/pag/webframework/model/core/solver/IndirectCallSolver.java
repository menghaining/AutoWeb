package ict.pag.webframework.model.core.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.dom4j.Element;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;

import ict.pag.webframework.model.answer.OptimizeAnswer;
import ict.pag.webframework.model.enumeration.CallType;
import ict.pag.webframework.model.enumeration.MarkScope;
import ict.pag.webframework.model.log.CallsiteInfo;
import ict.pag.webframework.model.marks.FrmkCallMark;
import ict.pag.webframework.model.marks.MarksHelper;
import ict.pag.webframework.model.marks.ResolveMarks;

public class IndirectCallSolver {
	/** Initialize */
	private HashMap<String, HashSet<Element>> class2XMLEle;
	private ClassHierarchy cha;

	/** Internal */
	private HashMap<String, HashSet<String>> visited = new HashMap<>();

	/** Answers */
	private HashSet<FrmkCallMark> frameworkCallMarks = new HashSet<>();

	/**
	 * Application call framework function, and then framework call back to
	 * application function</br>
	 * 
	 * 1. call back through parameter;</br>
	 * 2. call back through the marks of the called application function;</br>
	 * 3. call back through the inheritance relation. In other words, application
	 * function override framework function. (this case can be resolved by bytecode)
	 */
	public IndirectCallSolver(HashMap<String, HashSet<Element>> class2xmlEle, ClassHierarchy cha) {
		this.class2XMLEle = class2xmlEle;
		this.cha = cha;
	}

	public void solve(HashMap<CallsiteInfo, ArrayList<String>> callsite2target_frk) {
		long beforeTime = System.nanoTime();

		HashSet<FrmkCallMark> frameworkCallMarks_param = new HashSet<>();
		HashSet<FrmkCallMark> frameworkCallMarks_mark = new HashSet<>();
		for (CallsiteInfo callsite : callsite2target_frk.keySet()) {
			// 1. determine whether is framework call indirectly
			// 2. calculate the framework call indirectly marks
			ArrayList<String> mayInvokes = callsite2target_frk.get(callsite);

			calIndirectlyFrameworkInvoke(callsite, mayInvokes, frameworkCallMarks_param, frameworkCallMarks_mark);
		}

//		mergeAndADD(frameworkCallMarks_param, frameworkCallMarks_mark);
		frameworkCallMarks = OptimizeAnswer.resolveIndiectCallAnswer(frameworkCallMarks_param, frameworkCallMarks_mark);

		double buildTime = (System.nanoTime() - beforeTime) / 1E9;
		System.out.println("[TIME-LOG] Indirect Calls Infer Done in " + buildTime + " s!");
	}

	public HashSet<FrmkCallMark> getFrameworkCallMarks() {
		return frameworkCallMarks;
	}

	public void calIndirectlyFrameworkInvoke(CallsiteInfo callsite, ArrayList<String> mayInvokes,
			HashSet<FrmkCallMark> frameworkCallMarks_param, HashSet<FrmkCallMark> frameworkCallMarks_mark) {
		// framework callsite info
		String framework_csStmt = callsite.getCallStmt();
		String frameworkCsClass = framework_csStmt.substring(0, framework_csStmt.lastIndexOf('.'));

		String frmk_clazzStr = "L" + frameworkCsClass.replaceAll("\\.", "/");
		IClass frmk_clazz = cha.lookupClass(
				TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(frmk_clazzStr)));
		if (frmk_clazz == null)
			return;
		IMethod frameworkCallMtd = null;
		for (IMethod tmp : frmk_clazz.getAllMethods()) {
			if (tmp.getSignature().equals(framework_csStmt)) {
				frameworkCallMtd = tmp;
				break;
			}
		}
		if (frameworkCallMtd == null)
			return;

		HashSet<String> alltargets;
		if (visited.containsKey(framework_csStmt))
			alltargets = visited.get(framework_csStmt);
		else
			alltargets = new HashSet<>();

		for (String mayInvoke : mayInvokes) {
			if (alltargets.contains(mayInvoke))
				continue;
			alltargets.add(mayInvoke);

			// mayInvoke info
			String mayInvokeClassStr = mayInvoke.substring(0, mayInvoke.lastIndexOf('.'));
			String clazz0 = "L" + mayInvokeClassStr.replaceAll("\\.", "/");
			IClass mayInvokeClass = cha.lookupClass(
					TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(clazz0)));
			if (mayInvokeClass == null)
				continue;

			// 1. call back through parameter
			if (framework_csStmt.indexOf('(') + 1 != framework_csStmt.indexOf(')')) {
				// if has parameter
				String paramsString = framework_csStmt.substring(framework_csStmt.indexOf('(') + 1,
						framework_csStmt.indexOf(')'));

				String[] paramsTypes = new String[paramsString.length()];
				if (paramsString.contains(";")) {
					// split parameters
					for (int i = 0; i < paramsString.length();) {
						char c = paramsString.charAt(i);
						switch (c) {
						case 'Z':
						case 'B':
						case 'C':
						case 'S':
						case 'I':
						case 'J':
						case 'F':
						case 'D':
							i++;
							break;
						case 'L':
							String sub0 = paramsString.substring(i);
							String param1 = sub0.substring(0, sub0.indexOf(';'));
							paramsTypes[i] = param1;
							i = i + sub0.indexOf(';') + 1;
							break;
						default:
							i++;
						}
					}
				}

				for (int i = 0; i < paramsTypes.length; i++) {
					String type = paramsTypes[i];
					if (type == null)
						continue;
					IClass paramClass = cha.lookupClass(
							TypeReference.findOrCreate(ClassLoaderReference.Application, TypeName.findOrCreate(type)));
					if (paramClass == null)
						continue;

					if (!paramClass.getName().toString().equals("Ljava/lang/Object")
							&& !mayInvokeClass.getName().toString().equals("Ljava/lang/Object")) {
						if (cha.isSubclassOf(paramClass, mayInvokeClass) || cha.isSubclassOf(mayInvokeClass, paramClass)
								|| mayInvokeClass.equals(paramClass)) {
							int pos = i;
							if (frameworkCallMtd.isStatic())
								pos = i - 1;
							FrmkCallMark mark = new FrmkCallMark(CallType.Param, framework_csStmt, pos);
							frameworkCallMarks_param.add(mark);
						}
					}
				}
			}

			// 2. call back through the marks of the called application function
			IMethod mtd = null;
			for (IMethod tmp : mayInvokeClass.getAllMethods()) {
				if (tmp.getSignature().equals(mayInvoke)) {
					mtd = tmp;
					break;
				}
			}
			if (mtd != null) {
				HashSet<String> allMarks = new HashSet<>();
				HashSet<Annotation> annos_mtd0 = ResolveMarks.resolve_Annotation(mtd, MarkScope.Method);
				for (Annotation anno : annos_mtd0) {
					allMarks.add("[anno]" + MarksHelper.resolveAnnotationName(anno));
				}
				HashSet<String> xmlEles_mtd = ResolveMarks.resolve_XML_String(mtd, MarkScope.Method, class2XMLEle);
				for (String mark : xmlEles_mtd) {
					allMarks.add("[xml]" + mark);
				}
				if (!allMarks.isEmpty()) {
					FrmkCallMark mark = new FrmkCallMark(CallType.Attribute, framework_csStmt, allMarks);
					frameworkCallMarks_mark.add(mark);
				}
			}
		}

	}

}
