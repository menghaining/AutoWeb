package ict.pag.m.generateFrameworkModel4App.appInfoParsers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;

import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.AnnotationEntity;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.SequencePair;
import ict.pag.m.generateFrameworkModel4App.appInfoParsers.entity.xmlClassNodeEntity;
import ict.pag.m.generateFrameworkModel4App.entity.FrameworkModel;
import ict.pag.m.generateFrameworkModel4App.util.SpecialHelper;
import ict.pag.m.marks2SAInfer.util.structual.set2setPair;

public class BeforeAfterCalculator {
	private ClassHierarchy cha;
	private CHACallGraph chaCG;

	private FrameworkModel frameworkModel;

	private AnnotationExtractor annoExtractor;
	private XMLExtractor xmlExtractor;
	private InheritanceExtractor inheExtractor;

	/* Framework information */
	HashSet<set2setPair> sequenceCallsEntities;

	/* Application information */
	HashMap<String, HashSet<AnnotationEntity>> mark2method_anno;
	HashMap<String, HashSet<IClass>> mark2class_inhe;
	HashMap<String, HashSet<xmlClassNodeEntity>> mark2class_xml;

	/** Answers */
	HashSet<SequencePair> sequencePairs = new HashSet<>();

	public BeforeAfterCalculator(ClassHierarchy cha, CHACallGraph chaCG2, FrameworkModel frameworkModel2,
			AnnotationExtractor anno, XMLExtractor xml, InheritanceExtractor inheExtractor2) {
		this.cha = cha;
		this.chaCG = chaCG2;
		this.frameworkModel = frameworkModel2;
		this.annoExtractor = anno;
		this.xmlExtractor = xml;
		this.inheExtractor = inheExtractor2;

		sequenceCallsEntities = frameworkModel.getCallSequence();

		mark2method_anno = annoExtractor.getMark2methodSet();
		mark2class_inhe = inheExtractor.getMark2Class();
		mark2class_xml = xmlExtractor.getMark2classSet();
	}

	public int getSequencePairscount() {
		return sequencePairs.size();
	}

	public void matches() {
		for (set2setPair pair_marks : sequenceCallsEntities) {
			Set<String> beforemarkSet_frmk = pair_marks.getFirst();
			Set<String> aftermarkSet_frmk = pair_marks.getSecond();

			// 1. calculate find the before method
			HashSet<IMethod> beforeMethods = findMatchedMethods(beforemarkSet_frmk);
			// 2. calculate the after method
			HashSet<IMethod> afterMethods = findMatchedMethods(aftermarkSet_frmk);
			// 3. pair
			if (!beforeMethods.isEmpty() && !afterMethods.isEmpty()) {
				for (IMethod before : beforeMethods) {
					for (IMethod after : afterMethods) {
						SequencePair pair = new SequencePair(before, after);
						sequencePairs.add(pair);
					}
				}
			}

		}

		removeDuplicate();
	}

	private void removeDuplicate() {
		HashSet<SequencePair> answer = new HashSet<>();
		for (SequencePair pair : sequencePairs) {
			if (needAdd(answer, pair)) {
				answer.add(pair);
			}
		}
		sequencePairs = answer;

	}

	private boolean needAdd(HashSet<SequencePair> answer, SequencePair pair) {
		boolean has = false;
		for (SequencePair an : answer) {
			if (an.equals(pair)) {
				has = true;
				break;
			}
		}
		if (has)
			return false;
		else
			return true;
	}

	private HashSet<IMethod> findMatchedMethods(Set<String> marks) {
		HashSet<IMethod> ret = new HashSet<>();

		for (String bm_f : marks) {
			if (bm_f.startsWith("inhe:full:")) {
				String fullInhe = bm_f.substring("inhe:full:".length());
				String inheclass = fullInhe.substring(0, fullInhe.lastIndexOf('.'));
				String inheMtdSig = fullInhe.substring(fullInhe.lastIndexOf('.') + 1);
				if (mark2class_inhe.containsKey(inheclass)) {
					HashSet<IClass> candidateMethods = mark2class_inhe.get(inheclass);
					for (IClass candidate : candidateMethods) {
						HashSet<IMethod> mset = SpecialHelper.findSpecificMethod(candidate, inheMtdSig);
						if (!mset.isEmpty())
							ret.addAll(mset);
					}
				}

				break;
			} else if (bm_f.startsWith("anno:mtd:")) {
				String mtdAnno = bm_f.substring("anno:mtd:".length());
				if (mark2method_anno.containsKey(mtdAnno)) {
					HashSet<AnnotationEntity> candidateMethods = mark2method_anno.get(mtdAnno);
					for (AnnotationEntity candidate : candidateMethods) {
						IMethod mtd = candidate.getMethod();
						ret.add(mtd);
					}
				}
				break;
			}
		}

		return ret;
	}

}
