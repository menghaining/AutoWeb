package ict.pag.m.marks2SAInfer.util.structual;

import java.util.HashMap;
import java.util.Set;

import ict.pag.m.marks2SAInfer.util.reducedSetCollection;

public class SequenceSet2 {
	private HashMap<sequencePair2, Integer> sequence2occurence = new HashMap<>();

	public void add(sequencePair2 p) {
		sequencePair2 in = getSequence(p);

		if (in != null) {
			int count = sequence2occurence.get(in);
			sequence2occurence.put(in, count + 1);
		} else {
			sequence2occurence.put(p, 1);
		}
	}

	private sequencePair2 getSequence(sequencePair2 p) {
		for (sequencePair2 tmp : sequence2occurence.keySet()) {
			if (reducedSetCollection.elementEquals(tmp.getPre(), p.getPre()))
				if (reducedSetCollection.elementEquals(tmp.getPost(), p.getPost()))
					return tmp;
		}
		return null;
	}

	public String toString() {
		String ret = "";
		for (sequencePair2 seq : sequence2occurence.keySet()) {
			ret += seq.toString() + " : " + sequence2occurence.get(seq) + "\n";
		}

		return ret;

	}

	public Set<sequencePair2> getAllSequencePair2() {
		return sequence2occurence.keySet();
	}

}
