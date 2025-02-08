package ict.pag.m.marks2SAInfer.util.structual;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SequenceSet {
	private HashMap<sequencePair, Integer> sequence2occurence = new HashMap<>();
	private Set<String> occurMarks = new HashSet<>();

	public void add(sequencePair p) {
		if (!p.getPre().equals("START"))
			if (!occurMarks.contains(p.getPre()))
				occurMarks.add(p.getPre());
		if (!p.getPost().equals("END"))
			if (!occurMarks.contains(p.getPost()))
				occurMarks.add(p.getPost());

		sequencePair in = getSequence(p);
		if (in != null) {
			int count = sequence2occurence.get(in);
			sequence2occurence.put(in, count + 1);
		} else {
			sequence2occurence.put(p, 1);
		}
	}

	private sequencePair getSequence(sequencePair p) {
		for (sequencePair tmp : sequence2occurence.keySet()) {
			if (tmp.getPre().equals(p.getPre()) && tmp.getPost().equals(p.getPost()))
				return tmp;
		}
		return null;
	}

	public String toString() {
		String ret = "";
		for (sequencePair seq : sequence2occurence.keySet()) {
			ret += seq.toString() + " : " + sequence2occurence.get(seq) + "\n";
		}

		return ret;

	}

	public Set<String> getOccurMarks() {
		return occurMarks;
	}

}
