package ict.pag.m.frameworkInfoUtil.infoEntity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * for annos, <annoName, list<anno-values>></br>
 * for inheritance, <extends/implements, list<class-names>></br>
 */
public class infoPair {
	public String mark;
	public Map<String, String> value;

	public infoPair(String mark, Map<String, String> value) {
		this.mark = mark;
		this.value = value;
	}

	public String toString() {
		return "<" + mark + ", " + value + ">";

	}

	public String getMark() {
		return mark;
	}

	public Map<String, String> getValue() {
		return value;
	}

	/** return all distinguished 'value's values */
	public Set<String> getAllValues() {
		Set<String> ret = new HashSet<String>();
		if (value == null)
			return ret;
		for (String key : value.keySet()) {
			String tmp = value.get(key);
			if (!ret.contains(tmp))
				ret.add(tmp);
		}

		return ret;
	}

	public Set<String> getAllValues_tolower() {
		Set<String> ret = new HashSet<String>();
		for (String key : value.keySet()) {
			String tmp = value.get(key);
			if (!ret.contains(tmp))
				ret.add(tmp.toLowerCase());
		}

		return ret;
	}

}
