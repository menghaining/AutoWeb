package ict.pag.m.marks2SAInfer.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ContainsSet {
	/** caller mark set, callee mark sets */
	HashMap<HashSet<String>, reducedSetCollection> caller2calleeMap = new HashMap<>();

	public void add(HashSet<String> callerMarkSet, HashSet<String> calleeMarkSet) {
		// 1. reduce collection
		HashMap<HashSet<String>, HashSet<String>> need2Reput = new HashMap<>();
		for (HashSet<String> key : caller2calleeMap.keySet()) {
			if (CollectionUtil.contains(key, callerMarkSet)) {
				HashSet<String> diff = CollectionUtil.calDifferenceSet(key, callerMarkSet);
				HashSet<String> tmp = new HashSet<>();
				tmp.addAll(key);

				if (!diff.isEmpty()) {
					for (String rm : diff) {
						tmp.remove(rm);
					}
				}
				need2Reput.put(key, tmp);
			} else if (CollectionUtil.contains(callerMarkSet, key)) {
				HashSet<String> diff = CollectionUtil.calDifferenceSet(callerMarkSet, key);
				for (String rm : diff) {
					callerMarkSet.remove(rm);
				}
			}
		}
		for (HashSet<String> key : need2Reput.keySet()) {
			reducedSetCollection vals = caller2calleeMap.get(key);
			caller2calleeMap.remove(key);
			caller2calleeMap.put(need2Reput.get(key), vals);
		}
		// 2. add into collection
		boolean nofound = true;
		for (HashSet<String> key : caller2calleeMap.keySet()) {
			if (elementEquals(key, callerMarkSet)) {
				reducedSetCollection tmpVals = caller2calleeMap.get(key);
				tmpVals.add(calleeMarkSet);
				caller2calleeMap.put(key, tmpVals);
				nofound = false;
				break;
			}
		}
		if (nofound) {
			reducedSetCollection tmpSet = new reducedSetCollection();
			tmpSet.add(calleeMarkSet);
			caller2calleeMap.put(callerMarkSet, tmpSet);
		}
	}

	/** whether c1 elements equals c2 elements */
	public boolean elementEquals(Set<String> c1, Set<String> c2) {
		if (CollectionUtil.contains(c1, c2) && CollectionUtil.contains(c2, c1))
			return true;
		return false;
	}
}
