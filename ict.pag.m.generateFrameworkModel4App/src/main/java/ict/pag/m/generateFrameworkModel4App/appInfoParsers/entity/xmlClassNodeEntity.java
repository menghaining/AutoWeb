package ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity;

import java.util.HashMap;
import java.util.HashSet;

public class xmlClassNodeEntity {
	private String clazz;
	private HashMap<String, String> name2value = new HashMap<>();

	/* sub layer information if any */
	/* name: subTag:attributeName::value */
	private HashSet<SubInfo> subInfos = new HashSet<>();

	public xmlClassNodeEntity(String clazz, HashMap<String, String> name2value) {
		this.clazz = clazz;
		this.name2value = name2value;
	}

	public void addSubInformation(HashSet<SubInfo> subValues) {
		this.subInfos = subValues;
	}

	public void addSingleSubInformation(SubInfo subValue) {
		this.subInfos.add(subValue);
	}

	public void addName2Value(String name, String value) {
		name2value.put(name, value);
	}

	public boolean isSameClass(xmlClassNodeEntity obj) {
		if (this.clazz.equals(obj.getClazz()))
			return true;
		return false;
	}

	public String getClazz() {
		return clazz;
	}

	public HashMap<String, String> getName2value() {
		return name2value;
	}

	public HashSet<SubInfo> getSubInfos() {
		return subInfos;
	}

	public String toString() {
		return clazz + ", " + name2value;
	}

	public class SubInfo {
		private String tagName;
		private HashMap<String, String> name2value_sub = new HashMap<>();

		public SubInfo(String tagName, HashMap<String, String> name2value_sub) {
			this.tagName = tagName;
			this.name2value_sub = name2value_sub;
		}

		public String getTagName() {
			return tagName;
		}

		public HashMap<String, String> getName2value_sub() {
			return name2value_sub;
		}

	}
}
