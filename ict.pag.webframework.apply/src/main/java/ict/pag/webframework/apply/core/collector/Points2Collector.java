package ict.pag.webframework.apply.core.collector;

import java.util.HashMap;
import java.util.HashSet;

import org.dom4j.Element;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.annotations.Annotation;

import ict.pag.webframework.apply.core.FrameworkSpecification;
import ict.pag.webframework.model.enumeration.MarkScope;
import ict.pag.webframework.model.marks.ConcreteValueMark;
import ict.pag.webframework.model.marks.MarksHelper;
import ict.pag.webframework.model.marks.NormalMark;
import ict.pag.webframework.model.marks.ResolveMarks;

public class Points2Collector {

	public static HashMap<String, HashSet<String>> collectFieldPoints2(ClassHierarchy cha,
			HashMap<String, HashSet<Element>> class2XMLElement, FrameworkSpecification frmkSpecification) {
		HashSet<NormalMark> need2InjectMarksSet = frmkSpecification.getFieldInjectMarks();
		HashSet<ConcreteValueMark> field2ConcreteMarksSet = frmkSpecification.getFieldPoints2Marks();
		HashSet<ConcreteValueMark> classAliasMarksSet = frmkSpecification.getAliasMarks();
		HashSet<NormalMark> managedClassMarksSet = frmkSpecification.getManagedClassMarks();

		// TODO: 1. find all fields that need to inject, set as keySet
		HashSet<IField> injectFieldsSet = new HashSet<>();
		cha.getLoader(ClassLoaderReference.Application).iterateAllClasses().forEachRemaining(klass -> {
			for (IField f : klass.getDeclaredInstanceFields()) {
				HashSet<Annotation> annos_field0 = ResolveMarks.resolve_Annotation(f, MarkScope.Field);
				HashSet<String> annos_field = new HashSet<>();
				for (Annotation anno : annos_field0) {
					annos_field.add(MarksHelper.resolveAnnotationName(anno));
				}
				HashSet<String> xmlEles_field = ResolveMarks.resolve_XML_String(f, MarkScope.Field, class2XMLElement);

				for (NormalMark mark : need2InjectMarksSet) {

				}
			}
		});

		// TODO: 2. find the class that field actual points-to, if not specified,use the
		// same policy with jackee
		/**
		 * Main policy (JackEE): If the argument type has concrete subtypes in the
		 * application, pass them all as mock objects If it does not have concrete
		 * subtypes but it's a concrete type, pass that the mock object of the itself as
		 * argument.
		 **/

		/* deal with answers */
		HashMap<String, HashSet<String>> field2Class = new HashMap<>();

		return field2Class;
	}

}
