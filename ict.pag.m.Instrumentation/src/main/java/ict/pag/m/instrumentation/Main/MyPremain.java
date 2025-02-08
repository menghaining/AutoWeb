package ict.pag.m.instrumentation.Main;

import java.lang.instrument.Instrumentation;


public class MyPremain {
	public static void premain(String agentArgs, Instrumentation inst) {
		System.out.println("angent action begins ...");
//		inst.addTransformer(new OnlyPrintURLs(agentArgs));
		inst.addTransformer(new PrintTransformer4(agentArgs));
		System.out.println("angent action over ...");
		
	}

}
