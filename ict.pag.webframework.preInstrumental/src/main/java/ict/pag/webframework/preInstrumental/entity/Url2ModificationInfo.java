package ict.pag.webframework.preInstrumental.entity;

import java.util.ArrayList;

public class Url2ModificationInfo {
	private String url; // the original url
	private ArrayList<ModifiedInfo> modifiedRecord = new ArrayList<>();
	private ArrayList<Integer> testcaseIndex = new ArrayList<>();
	private ArrayList<String> triggerURL = new ArrayList<>(); // the urls trigger each testcase

	public Url2ModificationInfo(String url) {
		this.url = url;
	}

	/**
	 * the trigger url not change
	 */
	public void addModifiedRecord(ModifiedInfo info, int index) {
		modifiedRecord.add(info);
		testcaseIndex.add(index);
		triggerURL.add(url);
	}

	/**
	 * the trigger url changed
	 */
	public void addModifiedRecord(ModifiedInfo info, int index, String trigger) {
		modifiedRecord.add(info);
		testcaseIndex.add(index);
		triggerURL.add(trigger);
	}

	public int length() {
		return modifiedRecord.size();
	}

	public String getUrl() {
		return url;
	}

	public ModifiedInfo getModifiedInfo(int index) {
		return modifiedRecord.get(index);
	}

	public int getTestcaseIndex(int index) {
		return testcaseIndex.get(index);
	}

	public String getTriggerURL(int index) {
		return triggerURL.get(index);
	}
}
