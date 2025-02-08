package ict.pag.m.marks2SAInfer.util.structual;

import java.util.HashSet;

import ict.pag.m.marks2SAInfer.util.CollectionUtil;

public class str2Set {
	String name;
	HashSet<String> vals;

	public str2Set(String name, HashSet<String> vals) {
		this.name = name;
		this.vals = vals;
	}

	public String getName() {
		return name;
	}

	public HashSet<String> getVals() {
		return vals;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((vals == null) ? 0 : vals.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		str2Set other = (str2Set) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;

		if (vals == null) {
			if (other.vals != null)
				return false;
		} else {
			if (!CollectionUtil.isSame(vals, other.vals))
				return false;
		}

		return true;
	}

}
