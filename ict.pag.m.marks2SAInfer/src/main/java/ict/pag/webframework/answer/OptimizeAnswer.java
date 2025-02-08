package ict.pag.webframework.answer;

import java.util.HashSet;
import java.util.Iterator;

import ict.pag.webframework.marks.ConcreteValueMark;
import ict.pag.webframework.marks.EntryMark;
import ict.pag.webframework.marks.FrmkCallMark;
import ict.pag.webframework.marks.NormalMark;

public class OptimizeAnswer {

	public static HashSet<EntryMark> resolveEntryAnswer(HashSet<EntryMark> entryMarkSet) {
		HashSet<String> ignore_class = new HashSet<>();
		HashSet<String> ignore_mtd = new HashSet<>();

		// 1. merge each other
		HashSet<EntryMark> entryMarkSet_merged = new HashSet<>();
		Iterator<EntryMark> it0 = entryMarkSet.iterator();
		while (it0.hasNext()) {
			EntryMark mark = it0.next();
			if (entryMarkSet_merged.isEmpty()) {
				entryMarkSet_merged.add(mark);
				continue;
			}

			Iterator<EntryMark> it = entryMarkSet_merged.iterator();
			while (it.hasNext()) {
				EntryMark curr = it.next();
				mark.mergeMarks(curr, ignore_class, ignore_mtd);
			}
			entryMarkSet_merged.add(mark);
		}

		entryMarkSet.clear();
		entryMarkSet.addAll(entryMarkSet_merged);
		// entryMarkSet = entryMarkSet_merged;

		// 2. Remove Ignores
		if (!ignore_class.isEmpty() || !ignore_mtd.isEmpty()) {
			Iterator<EntryMark> it1 = entryMarkSet.iterator();
			while (it1.hasNext()) {
				EntryMark mark = it1.next();
				if (mark.isAllEmpty()) {
					it1.remove();
				} else {
					if (!ignore_class.isEmpty()) {
						HashSet<String> allmarks_classes = mark.getAllMarks_class();
						for (String ignore : ignore_class) {
							if (allmarks_classes.contains(ignore))
								allmarks_classes.remove(ignore);
						}
					}
					if (!ignore_mtd.isEmpty()) {
						HashSet<String> allmarks_mtds = mark.getAllMarks_methods();
						for (String ignore : ignore_mtd) {
							if (allmarks_mtds.contains(ignore))
								allmarks_mtds.remove(ignore);
						}
					}
				}
			}
		}

		// 3. Remove Duplicated
		HashSet<EntryMark> entryMarkSet_new = new HashSet<>();
		for (EntryMark mark : entryMarkSet) {
			if (entryMarkSet_new.isEmpty()) {
				entryMarkSet_new.add(mark);
				continue;
			}

			boolean has = false;
			for (EntryMark curr : entryMarkSet_new) {
				if (curr.isSame(mark)) {
					has = true;
					break;
				}
			}
			if (!has) {
				if (!mark.isAllEmpty())
					entryMarkSet_new.add(mark);
			}
		}

		entryMarkSet.clear();
		entryMarkSet.addAll(entryMarkSet_new);
		// MergeMain.entryMarkSet = entryMarkSet;
		return entryMarkSet;
	}

	public static HashSet<FrmkCallMark> resolveIndiectCallAnswer(HashSet<FrmkCallMark> frameworkCallMarks) {
		HashSet<FrmkCallMark> frameworkCallMarks_param = new HashSet<FrmkCallMark>();
		HashSet<FrmkCallMark> frameworkCallMarks_mark = new HashSet<FrmkCallMark>();

		// split in to two set depends on the representation
		for (FrmkCallMark mark : frameworkCallMarks) {
			if (mark.getParamIndex() == -1) {
				if (!mark.getAllMarks().isEmpty())
					frameworkCallMarks_mark.add(mark);
			} else {
				frameworkCallMarks_param.add(mark);
			}
		}

		return resolveIndiectCallAnswer(frameworkCallMarks_param, frameworkCallMarks_mark);
	}

	/**
	 * @param frameworkCallMarks_param the set of index representation
	 * @param frameworkCallMarks_mark  the set of marks representation
	 */
	public static HashSet<FrmkCallMark> resolveIndiectCallAnswer(HashSet<FrmkCallMark> frameworkCallMarks_param,
			HashSet<FrmkCallMark> frameworkCallMarks_mark) {
		HashSet<FrmkCallMark> frameworkCallMarks = new HashSet<>();

		// 1. param
		HashSet<FrmkCallMark> tmpSet1 = new HashSet<FrmkCallMark>();
		Iterator<FrmkCallMark> it1 = frameworkCallMarks_param.iterator();
		while (it1.hasNext()) {
			FrmkCallMark tmp = it1.next();
			if (tmpSet1.isEmpty()) {
				tmpSet1.add(tmp);
				continue;
			}

			Iterator<FrmkCallMark> it11 = tmpSet1.iterator();
			boolean add = true;
			while (it11.hasNext()) {
				FrmkCallMark tmp1 = it11.next();
				if (tmp1.getCallStmt().equals(tmp.getCallStmt()) && tmp1.getParamIndex() == tmp.getParamIndex()) {
					add = false;
					break;
				}
			}
			if (add) {
				tmpSet1.add(tmp);
			}
		}
		// 2. markSet
		HashSet<FrmkCallMark> tmpSet2 = new HashSet<>();
		HashSet<String> ignoreSet = new HashSet<>();
		// 2.1. merge
		// reduce to minimal collection and merge
		Iterator<FrmkCallMark> it2 = frameworkCallMarks_mark.iterator();
		while (it2.hasNext()) {
			FrmkCallMark mark = it2.next();
			if (tmpSet2.isEmpty()) {
				tmpSet2.add(mark);
				continue;
			}

			Iterator<FrmkCallMark> it = tmpSet2.iterator();
			while (it.hasNext()) {
				FrmkCallMark curr = it.next();
				if (mark.getCallStmt().equals(curr.getCallStmt()))
					mark.mergeMarks(curr, ignoreSet);
			}
		}
		// 2.2 remove ignores
		if (!ignoreSet.isEmpty()) {
			Iterator<FrmkCallMark> it3 = frameworkCallMarks_mark.iterator();
			while (it3.hasNext()) {
				FrmkCallMark mark = it3.next();
				if (mark.isAllEmpty()) {
					it3.remove();
				} else {
					HashSet<String> allmarks_classes = mark.getAllMarks();
					for (String ignore : ignoreSet) {
						if (allmarks_classes.contains(ignore))
							allmarks_classes.remove(ignore);
					}
				}
			}
		}
		// 3.Remove Duplicated
		HashSet<FrmkCallMark> tmp = new HashSet<>();
		for (FrmkCallMark mark : frameworkCallMarks_mark) {
			if (tmp.isEmpty()) {
				tmp.add(mark);
				continue;
			}

			boolean has = false;
			for (FrmkCallMark curr : tmp) {
				if (curr.isSame(mark) && mark.getCallStmt().equals(curr.getCallStmt())) {
					has = true;
					break;
				}
			}
			if (!has) {
				tmp.add(mark);
			}
		}

		/* Finally, merge two type set to answer */
		if (!tmpSet1.isEmpty())
			frameworkCallMarks.addAll(tmpSet1);
		if (!tmp.isEmpty())
			frameworkCallMarks.addAll(tmp);

		return frameworkCallMarks;
	}

	public static void resolveNormalMarkAnswers(HashSet<NormalMark> marksSet) {
		HashSet<String> ignoreSet = new HashSet<>();
		// 1. merge
		// reduce to minimal collection and merge
		HashSet<NormalMark> markSet_merged = new HashSet<>();
		Iterator<NormalMark> it0 = marksSet.iterator();
		while (it0.hasNext()) {
			NormalMark mark = it0.next();
			if (markSet_merged.isEmpty()) {
				markSet_merged.add(mark);
				continue;
			}

			Iterator<NormalMark> it = markSet_merged.iterator();
			while (it.hasNext()) {
				NormalMark curr = it.next();
				mark.mergeMarks(curr, ignoreSet);
			}
			markSet_merged.add(mark);
		}

		// 2. Remove Ignores
		if (!ignoreSet.isEmpty()) {
			Iterator<NormalMark> it1 = markSet_merged.iterator();
			while (it1.hasNext()) {
				NormalMark mark = it1.next();
				if (mark.isAllEmpty()) {
					it1.remove();
				} else {
					HashSet<String> allmarks_classes = mark.getAllMarks();
					for (String ignore : ignoreSet) {
						if (allmarks_classes.contains(ignore))
							allmarks_classes.remove(ignore);
					}
				}
			}
		}

		// 3.Remove Duplicated
		HashSet<NormalMark> marksSet_new = new HashSet<>();
		for (NormalMark mark : markSet_merged) {
			if (marksSet_new.isEmpty()) {
				marksSet_new.add(mark);
				continue;
			}

			boolean has = false;
			for (NormalMark curr : marksSet_new) {
				if (curr.isSame(mark)) {
					has = true;
					break;
				}
			}
			if (!has) {
				marksSet_new.add(mark);
			}
		}

		marksSet.clear();
		marksSet.addAll(marksSet_new);
//		marksSet = marksSet_new;
	}

	public static void resovleConcreteValueMarkAnswer(HashSet<ConcreteValueMark> marksSet) {
		HashSet<String> ignoreSet = new HashSet<>();
		// 1. merge
		// reduce to minimal collection and merge
		HashSet<ConcreteValueMark> markSet_merged = new HashSet<>();
		Iterator<ConcreteValueMark> it0 = marksSet.iterator();
		while (it0.hasNext()) {
			ConcreteValueMark mark = it0.next();
			if (markSet_merged.isEmpty()) {
				markSet_merged.add(mark);
				continue;
			}

			Iterator<ConcreteValueMark> it = markSet_merged.iterator();
			while (it.hasNext()) {
				ConcreteValueMark curr = it.next();
				mark.mergeMarks(curr, ignoreSet);
			}
			markSet_merged.add(mark);
		}

		// 2. Remove Ignores
		if (!ignoreSet.isEmpty()) {
			Iterator<ConcreteValueMark> it1 = markSet_merged.iterator();
			while (it1.hasNext()) {
				ConcreteValueMark mark = it1.next();
				if (mark.isAllEmpty()) {
					it1.remove();
				} else {
					HashSet<String> allmarks_classes = mark.getAllMarks();
					for (String ignore : ignoreSet) {
						if (allmarks_classes.contains(ignore))
							allmarks_classes.remove(ignore);
					}
				}
			}
		}

		// 3.Remove Duplicated
		HashSet<ConcreteValueMark> tmp = new HashSet<>();
		for (ConcreteValueMark mark : markSet_merged) {
			if (tmp.isEmpty()) {
				tmp.add(mark);
				continue;
			}

			boolean has = false;
			for (ConcreteValueMark curr : tmp) {
				if (curr.isSame(mark)) {
					has = true;
					break;
				}
			}
			if (!has) {
				tmp.add(mark);
			}
		}

		marksSet.clear();
		marksSet.addAll(tmp);
//		marksSet = tmp;
	}
}
