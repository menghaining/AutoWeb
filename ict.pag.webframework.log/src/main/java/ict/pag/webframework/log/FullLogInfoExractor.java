package ict.pag.webframework.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import ict.pag.webframework.log.dynamicCG.DynamicCG;
import ict.pag.webframework.log.dynamicCG.DynamicCGBuilder;

public class FullLogInfoExractor {
	public static void main(String[] args) {
		LogReader.setForm(true);
		HashMap<Integer, ArrayList<String>> id2callList = LogReader.calId2Sequence(args[0]);

		DynamicCGBuilder builder = new DynamicCGBuilder();
		for (Integer id : id2callList.keySet()) {
			builder.calculate(id2callList.get(id));

		}

		DynamicCG cg = builder.getDynamicCallGraph();

		System.out.println();
	}

}
