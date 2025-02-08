package ict.pag.webframework.apply.core;

import java.util.HashSet;

import ict.pag.webframework.model.marks.ConcreteValueMark;
import ict.pag.webframework.model.marks.EntryMark;
import ict.pag.webframework.model.marks.FrmkCallMark;
import ict.pag.webframework.model.marks.NormalMark;

public class FrameworkSpecification {
	public HashSet<EntryMark> entryMarkSet = new HashSet<>();

	private HashSet<NormalMark> managedClassMarks = new HashSet<>();
	private HashSet<NormalMark> fieldInjectMarks = new HashSet<>();
	private HashSet<ConcreteValueMark> fieldPoints2Marks = new HashSet<>();
	private HashSet<ConcreteValueMark> aliasMarks = new HashSet<>();
	private HashSet<ConcreteValueMark> frameworkCallReturnPoints2Marks = new HashSet<>();
	private HashSet<String> mayEntryPointFormalParameterSet = new HashSet<>();

	private HashSet<FrmkCallMark> frameworkCallMarks = new HashSet<>();

	public boolean isEmptySpecification() {
		if (entryMarkSet.isEmpty() && managedClassMarks.isEmpty() && fieldInjectMarks.isEmpty()
				&& fieldPoints2Marks.isEmpty() && aliasMarks.isEmpty() && frameworkCallReturnPoints2Marks.isEmpty()
				&& mayEntryPointFormalParameterSet.isEmpty() && frameworkCallMarks.isEmpty())
			return true;
		return false;
	}

	public void addEntryMarkSet(EntryMark obj) {
		entryMarkSet.add(obj);
	}

	public void addManagedClassMarks(NormalMark obj) {
		managedClassMarks.add(obj);
	}

	public void addFieldInjectMarks(NormalMark obj) {
		fieldInjectMarks.add(obj);
	}

	public void addFieldPoints2Marks(ConcreteValueMark obj) {
		fieldPoints2Marks.add(obj);
	}

	public void addAliasMarks(ConcreteValueMark obj) {
		aliasMarks.add(obj);
	}

	public void addFrameworkCallReturnPoints2Marks(ConcreteValueMark obj) {
		frameworkCallReturnPoints2Marks.add(obj);
	}

	public void addMayEntryPointFormalParameterSet(String obj) {
		mayEntryPointFormalParameterSet.add(obj);
	}

	public void addFrameworkCallMarks(FrmkCallMark obj) {
		frameworkCallMarks.add(obj);
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
