package ict.pag.webframework.apply.core;

import java.util.HashMap;
import java.util.HashSet;

import org.dom4j.Element;

import com.ibm.wala.ipa.cha.ClassHierarchy;

import ict.pag.webframework.apply.core.collector.EntryPointCollector;

public class FrameworkModel4App {
	/** Initialize */
	private ClassHierarchy cha;
	private HashMap<String, HashSet<Element>> class2XMLElement;
	private FrameworkSpecification frmkSpecification;

	/** Answer */
	private HashSet<String> entriesSigs = new HashSet<>();

	public FrameworkModel4App(ClassHierarchy cha, HashMap<String, HashSet<Element>> class2xmlElement,
			FrameworkSpecification frmkSpecification) {
		this.cha = cha;
		this.class2XMLElement = class2xmlElement;
		this.frmkSpecification = frmkSpecification;

		solve();
	}

	private void solve() {
		entriesSigs = EntryPointCollector.collect(cha, class2XMLElement, frmkSpecification);

	}

}
