package ict.pag.m.generateFrameworkModel4App.entity;

import java.util.HashSet;
import java.util.Set;

import ict.pag.m.marks2SAInfer.summarizeModels.CallsiteM2targetM;
import ict.pag.m.marks2SAInfer.util.reducedSetCollection;
import ict.pag.m.marks2SAInfer.util.structual.FrmkRetPoints2;
import ict.pag.m.marks2SAInfer.util.structual.set2setPair;

public class FrameworkModel {
	/** entries */
	HashSet<set2setPair> entries = new HashSet<>();
	HashSet<String> entries_params = new HashSet<>();

	/** managed class and points-to */
	/* framework managed classes */
	reducedSetCollection managedClasses = new reducedSetCollection();
	/* alias marks */
	HashSet<String> classAlias = new HashSet<>();
	/* the type that actual points-to */
	HashSet<String> objActualPoints2Alias = new HashSet<>();
	/* return actual type */
	HashSet<FrmkRetPoints2> frmwkRetAcutalClass = new HashSet<>();
	/* Inject field */
	Set<HashSet<String>> fieldsInject = new HashSet<>();

	/** may be Sequence */
	HashSet<set2setPair> callSequence = new HashSet<>();

	/** indirectly call */
	Set<CallsiteM2targetM> indirectCalls = new HashSet<>();

	public FrameworkModel() {

	}

	public void addEntry(set2setPair obj) {
		entries.add(obj);
	}

	public void addEntryParams(String obj) {
		entries_params.add(obj);
	}

	public void addManagedClass(HashSet<String> obj) {
		managedClasses.add(obj);
	}

	public void addClassAliasMark(String obj) {
		classAlias.add(obj);
	}

	public void addPoints2Alias(String obj) {
		objActualPoints2Alias.add(obj);
	}

	public void addFrmwkRetAcutalClass(FrmkRetPoints2 obj) {
		frmwkRetAcutalClass.add(obj);
	}

	// do not differ method or field because the prefix of marks will tell
	public void addFieldsInject(HashSet<String> obj) {
		fieldsInject.add(obj);
	}

	public void addCallSequence(set2setPair seq) {
		callSequence.add(seq);
	}

	public void addIndirectCalls(CallsiteM2targetM obj) {
		indirectCalls.add(obj);
	}

	public HashSet<set2setPair> getEntries() {
		return entries;
	}

	public reducedSetCollection getManagedClasses() {
		return managedClasses;
	}

	public HashSet<String> getClassAlias() {
		return classAlias;
	}

	public HashSet<String> getObjActualPoints2Alias() {
		return objActualPoints2Alias;
	}

	public HashSet<FrmkRetPoints2> getFrmwkRetAcutalClass() {
		return frmwkRetAcutalClass;
	}

	public Set<HashSet<String>> getFieldsInject() {
		return fieldsInject;
	}

	public HashSet<set2setPair> getCallSequence() {
		return callSequence;
	}

	public Set<CallsiteM2targetM> getIndirectCalls() {
		return indirectCalls;
	}

	public HashSet<String> getEntries_params() {
		return entries_params;
	}

}
