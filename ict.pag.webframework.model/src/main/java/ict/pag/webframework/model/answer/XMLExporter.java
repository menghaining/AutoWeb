package ict.pag.webframework.model.answer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import ict.pag.webframework.model.marks.EntryMark;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

public class XMLExporter {
	private String userDir = System.getProperty("user.dir");
	private String root;

	public XMLExporter() {
		if (!userDir.endsWith(File.separator)) {
			userDir = userDir + File.separator;
		}
		root = userDir + "result-matrix" + File.separator;
		File dir = new File(root);
		if (!dir.exists())
			dir.mkdirs();
	}

	/*
	 * Warning: Could not add cell at A257 because it exceeds the maximum column
	 * limit
	 */
	public void exportPositiveAndNegtiveData(ArrayList<HashSet<Integer>> positiveFeathersSet,
			ArrayList<HashSet<Integer>> negativeFeathersSet, ArrayList<String> feathers, String fileName,
			String sheetName, boolean append) throws IOException, RowsExceededException, WriteException {
		File file = new File(root + fileName);
		System.out.println("XML File write to " + file);
		if (!file.exists()) {
			file.createNewFile();
		} else {
			if (!append) {
				file.delete();
				file.createNewFile();
			}
		}

		WritableWorkbook workbook = Workbook.createWorkbook(file);
		WritableSheet sheet = workbook.getSheet(sheetName);
		// if already has xls file, it will over-write existence
		if (sheet == null) {
			sheet = workbook.createSheet(sheetName, workbook.getNumberOfSheets());
		}

		// 1. add attributes: features name
		for (int i = 0; i < feathers.size(); i++) {
			String feather = feathers.get(i);
			sheet.addCell(new Label(i, 0, feather));
//			sheet.addCell(new Label(0, i, feather));
		}
//		sheet.addCell(new Label(feathers.size(), 0, "VALUE"));
//		sheet.addCell(new Label(0, feathers.size(), "IS"));
//		sheet.addCell(new Label(0, feathers.size() + 1, "NOT"));
		sheet.addCell(new Label(feathers.size(), 0, "IS"));
		sheet.addCell(new Label(feathers.size() + 1, 0, "NOT"));

		// 2. value = 1, means is entry
		for (int i = 0; i < positiveFeathersSet.size(); i++) {
			HashSet<Integer> vals0 = positiveFeathersSet.get(i);

			for (int j = 0; j < feathers.size(); j++) {
				Label l = null;
				if (vals0.contains(j)) {
					l = new Label(j, i + 1, "1");
//					l = new Label(i + 1, j, "1");
				} else {
					l = new Label(j, i + 1, "0");
//					l = new Label(i + 1, j, "0");
				}
				sheet.addCell(l);
			}
			// value
//			sheet.addCell(new Label(feathers.size(), i + 1, "1"));
//			sheet.addCell(new Label(i + 1, feathers.size(), "1"));
//			sheet.addCell(new Label(i + 1, feathers.size() + 1, "0"));
			sheet.addCell(new Label(feathers.size(), i + 1, "1"));
			sheet.addCell(new Label(feathers.size() + 1, i + 1, "0"));
		}

		// 3. value = -1, means not entry
		int rows = sheet.getRows();
		for (int i = 0; i < negativeFeathersSet.size(); i++) {
			HashSet<Integer> vals0 = negativeFeathersSet.get(i);

			for (int j = 0; j < feathers.size(); j++) {
				Label l = null;
				if (vals0.contains(j)) {
					l = new Label(j, i + rows, "1");
//					l = new Label(i + rows, j, "1");
				} else {
					l = new Label(j, i + rows, "0");
//					l = new Label(i + rows, j, "0");
				}
				sheet.addCell(l);
			}
			// value
//			sheet.addCell(new Label(feathers.size(), i + rows, "-1"));
//			sheet.addCell(new Label(i + rows, feathers.size(), "0"));
//			sheet.addCell(new Label(i + rows, feathers.size() + 1, "1"));
			sheet.addCell(new Label(feathers.size(), i + rows, "0"));
			sheet.addCell(new Label(feathers.size() + 1, i + rows, "1"));
		}

		workbook.write();
		workbook.close();
	}

	public void exportVerifiedData(HashSet<HashSet<Integer>> verifiedEntryMarksSet, ArrayList<String> feathers_entries,
			String fileName, String sheetName, boolean append)
			throws RowsExceededException, WriteException, IOException {
		File file = new File(root + fileName);
		System.out.println("XML File write to " + file);
		if (!file.exists()) {
			file.createNewFile();
		} else {
			if (!append) {
				file.delete();
				file.createNewFile();
			}
		}

		WritableWorkbook workbook = Workbook.createWorkbook(file);
		WritableSheet sheet = workbook.createSheet(sheetName, 0);

		// 1. add attributes: features name
		for (int i = 0; i < feathers_entries.size(); i++) {
			String feather = feathers_entries.get(i);
			sheet.addCell(new Label(i, 0, feather));
		}

		// 2. write to file
		int row = 0;
		for (HashSet<Integer> vals0 : verifiedEntryMarksSet) {
			for (int column = 0; column < feathers_entries.size(); column++) {
				Label l = null;
				if (vals0.contains(column)) {
					l = new Label(column, row + 1, "1");
				} else {
					l = new Label(column, row + 1, "0");
				}
				sheet.addCell(l);
			}

			row++;
		}
		workbook.write();
		workbook.close();
	}

	public void exportPositiveAndNegtiveData(HashSet<EntryMark> entryMarkSet, HashSet<EntryMark> notEntryMarkSet,
			String fileName, String sheetName, boolean append) throws IOException, WriteException {
		File file = new File(root + fileName);
		System.out.println("XML File write to " + file);
		if (!file.exists()) {
			file.createNewFile();
		} else {
			if (!append) {
				file.delete();
				file.createNewFile();
			}
		}

		WritableWorkbook workbook = Workbook.createWorkbook(file);
		WritableSheet sheet = workbook.getSheet(sheetName);
		// if already has xls file, it will over-write existence
		if (sheet == null) {
			sheet = workbook.createSheet(sheetName, workbook.getNumberOfSheets());
		}

		int row = 0;
		// add entry
		for (EntryMark ele : entryMarkSet) {
			int column = 0;
			for (String m : ele.getAllMarks_class()) {
				sheet.addCell(new Label(column, row, m));
				column++;
			}
			for (String m : ele.getAllMarks_methods()) {
				sheet.addCell(new Label(column, row, m));
				column++;
			}
			sheet.addCell(new Label(column, row, "isEntry"));
			row++;
		}
		// add not entry
		for (EntryMark ele : notEntryMarkSet) {
			int column = 0;
			for (String m : ele.getAllMarks_class()) {
				sheet.addCell(new Label(column, row, m));
				column++;
			}
			for (String m : ele.getAllMarks_methods()) {
				sheet.addCell(new Label(column, row, m));
				column++;
			}
			sheet.addCell(new Label(column, row, "notEntry"));
			row++;
		}

		workbook.write();
		workbook.close();

	}

}
