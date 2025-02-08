package ict.pag.m.instrumentation.Entity;

import java.util.HashSet;
import java.util.Set;

public class InstrumentationPosition {
	/**
	 * normalMethodPosition means instrument at the begining and ending of methods
	 */
	private Set<InstrumentationPair> normalMethodPosition = new HashSet<InstrumentationPair>();
	/**
	 * invokeMethodPosition means instrument at special invoke position
	 */
	private Set<InstrumentationPair> invokeMethodPosition = new HashSet<InstrumentationPair>();

}
