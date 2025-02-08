package ict.pag.webframework.preInstrumental.helper;

import java.util.HashSet;
import java.util.Set;

public class CollectionHelper {
	public static HashSet<String> findSameStringValue(Set<String> collection1, Set<String> collection2) {
		HashSet<String> sameVals = new HashSet<>();

		if (collection1 == null || collection2 == null)
			return sameVals;

		for (String v : collection1) {
			if (collection2.contains(v))
				sameVals.add(v);
		}

		return sameVals;
	}
}
