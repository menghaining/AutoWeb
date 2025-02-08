package ict.pag.webframework.model.core.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.dom4j.Element;

import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.FieldReference;

import ict.pag.webframework.model.graph.GraphBuilder;
import ict.pag.webframework.model.log.Callsite2CallSeqMapTool;
import ict.pag.webframework.model.log.CallsiteInfo;
import ict.pag.webframework.model.log.LogParser;
import ict.pag.webframework.model.log.LogParser2;
import ict.pag.webframework.model.log.SplitStmt4OneFunction;
import ict.pag.webframework.model.marks.ConcreteValueMark;
import ict.pag.webframework.model.marks.EntryMark;
import ict.pag.webframework.model.marks.FrmkCallMark;
import ict.pag.webframework.model.marks.NormalMark;

public class FrameworkMarksSolver {
	/** Initialize-application code features */
	private Set<String> unreachableRoots;
	private HashMap<String, HashSet<Element>> class2XMLEle;
//	private GraphBuilder builder;
	private ClassHierarchy cha;
	private CHACallGraph chaCG;
	private HashSet<String> applicationClasses;

	/** Initialize-log */
	private HashMap<Integer, ArrayList<String>> id2group_unreachable;
	private HashMap<Integer, ArrayList<String>> id2group_removeException;

	private HashMap<String, Set<ArrayList<String>>> outer2Seq;
	private Callsite2CallSeqMapTool tool;

	/** internal answers */
	HashSet<String> running_unreachableCalls;
	/** Final Answers */
	private HashSet<EntryMark> entryMarkSet;

	private HashSet<NormalMark> managedClassMarks;
	private HashSet<NormalMark> fieldInjectMarks;
	private HashSet<ConcreteValueMark> fieldPoints2Marks;
	private HashSet<ConcreteValueMark> aliasMarks;
	private HashSet<ConcreteValueMark> frameworkCallReturnPoints2Marks;
	private HashSet<String> mayEntryPointFormalParameterSet;

	private HashSet<FrmkCallMark> frameworkCallMarks;

	/** use the old log parser */
	@Deprecated
	public FrameworkMarksSolver(GraphBuilder builder2, LogParser logparser1,
			HashMap<String, HashSet<Element>> class2XMLEle2) {
		this.cha = builder2.getCHA();
		this.chaCG = builder2.getAppCHACG();
		this.unreachableRoots = builder2.getAllUnreachableEntryPoints();
		this.applicationClasses = builder2.getApplicationClasses();

		this.id2group_unreachable = logparser1.getId2group_OnlyUnreachable();
		this.id2group_removeException = LogParser.removeExceptionCallStmts(logparser1.getId2group_fullInfo());

		this.class2XMLEle = class2XMLEle2;
	}

	/** use new parser */
	public FrameworkMarksSolver(GraphBuilder builder2, LogParser2 logparser2,
			HashMap<String, HashSet<Element>> class2XMLEle2) {
		this.cha = builder2.getCHA();
		this.chaCG = builder2.getAppCHACG();

		this.applicationClasses = builder2.getApplicationClasses();
		this.unreachableRoots = builder2.calAllUnreachableEntryPoints();

		this.outer2Seq = logparser2.getOuter2Seq();
		this.tool = logparser2.getCallsite2CallSeqTool();

		this.id2group_removeException = logparser2.getId2group_fullInfo();
		this.running_unreachableCalls = logparser2.getFrameworkInvokesCalls();

		this.class2XMLEle = class2XMLEle2;
	}

	public void sovle() {

		/* preparation: get more precise log */
		long beforeTime = System.nanoTime();

		if (outer2Seq == null)
			outer2Seq = SplitStmt4OneFunction.split(id2group_removeException);
		if (tool == null) {
			tool = new Callsite2CallSeqMapTool(chaCG, outer2Seq, applicationClasses);
			tool.dealWith();
		}

		// record the callsite to real target when the real target and the base object
		// declare type are different. The call site is application codes.
		HashMap<FieldReference, String> fieldRef2target_app = tool.getFieldRef2target_app();
		// the framework and its targets
		HashMap<CallsiteInfo, ArrayList<String>> callsite2target_frk = tool.getCallsite2target_frk();
		// record call sequence after one line callsite
		/** framework return depends on configuration */
		HashMap<CallsiteInfo, String> callsite2target_app = tool.getCallsite2target_app();

		double buildTime = (System.nanoTime() - beforeTime) / 1E9;
		System.out.println("[TIME-LOG] Log Refining Done in " + buildTime + " s!");

		/* 1. calculate entry */
//		EntrySolver entrySolver = new EntrySolver(cha, class2XMLEle, unreachableRoots);
//		entrySolver.calculateEntry(id2group_unreachable);
		EntrySolver2 entrySolver = new EntrySolver2(unreachableRoots, cha, class2XMLEle);
		entrySolver.calculateEntry(running_unreachableCalls);
		entryMarkSet = entrySolver.getEntryMarkSet();

		/* 2. calculate mock object, field inject, and points-to relation */
		ObjectGenerateSolver objectGenSolver = new ObjectGenerateSolver(cha, chaCG, class2XMLEle);
		objectGenSolver.solve(running_unreachableCalls, fieldRef2target_app, callsite2target_app);
		managedClassMarks = objectGenSolver.getManagedClassMarks();
		fieldInjectMarks = objectGenSolver.getFieldInjectMarks();
		fieldPoints2Marks = objectGenSolver.getFieldPoints2Marks();
		aliasMarks = objectGenSolver.getAliasMarks();
		frameworkCallReturnPoints2Marks = objectGenSolver.getFrameworkCallReturnPoints2Marks();
		mayEntryPointFormalParameterSet = objectGenSolver.getMayEntryPointFormalParameterSet();

		/* 3. calculate indirectly calls */
		IndirectCallSolver callsSolver = new IndirectCallSolver(class2XMLEle, cha);
		callsSolver.solve(callsite2target_frk);
		frameworkCallMarks = callsSolver.getFrameworkCallMarks();
	}

	public HashSet<EntryMark> getEntryMarkSet() {
		return entryMarkSet;
	}

	public HashSet<NormalMark> getManagedClassMarks() {
		return managedClassMarks;
	}

	public HashSet<NormalMark> getFieldInjectMarks() {
		return fieldInjectMarks;
	}

	public HashSet<ConcreteValueMark> getFieldPoints2Marks() {
		return fieldPoints2Marks;
	}

	public HashSet<ConcreteValueMark> getAliasMarks() {
		return aliasMarks;
	}

	public HashSet<ConcreteValueMark> getFrameworkCallReturnPoints2Marks() {
		return frameworkCallReturnPoints2Marks;
	}

	public HashSet<String> getMayEntryPointFormalParameterSet() {
		return mayEntryPointFormalParameterSet;
	}

	public HashSet<FrmkCallMark> getFrameworkCallMarks() {
		return frameworkCallMarks;
	}

}
