package ict.pag.webframework.model.answer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;

import ict.pag.webframework.model.marks.EntryMark;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import jxl.write.WriteException;

public class XMLResultChecker {
	public static void main(String[] args) {
		ArrayList<Integer> res = new ArrayList<>();
		readExcel(res,
				"F:\\myProject\\webframeworkmodelinfer\\ict.pag.webframework.model\\result-matrix\\1227\\res2.xls",
				"Sheet");

		ArrayList<String> feathers = new ArrayList<>();
		int[][] vals = readVerifiedData(feathers,
				"F:\\myProject\\webframeworkmodelinfer\\ict.pag.webframework.model\\result-matrix\\1227\\Verified-Data-Entry.xls",
				"data");

		// 1. extract the set position in res when value = 1
		ArrayList<Integer> pos_res = new ArrayList<>();
		ArrayList<Integer> neg_res = new ArrayList<>();
		for (int i = 0; i < res.size(); i++) {
			int value = res.get(i);
			if (value == 1) {
				pos_res.add(i);
			} else {
				neg_res.add(i);
			}
		}

		// 2. find all marks index in pos_res
		HashSet<ArrayList<Integer>> set = new HashSet<>();
		for (Integer i : pos_res) {
			ArrayList<Integer> tmp = new ArrayList<Integer>();

			for (int j = 0; j < vals[0].length; j++) {
				int v = vals[i][j];
				if (v == 1) {
					tmp.add(j);
				}
			}

			if (tmp.isEmpty())
				System.err.println("!!!");
			else
				set.add(tmp);

		}

		HashSet<ArrayList<Integer>> set_neg = new HashSet<>();
		for (Integer i : neg_res) {
			ArrayList<Integer> tmp = new ArrayList<Integer>();

			for (int j = 0; j < vals[0].length; j++) {
				int v = vals[i][j];
				if (v == 1) {
					tmp.add(j);
				}
			}

			if (tmp.isEmpty())
				System.err.println("!!!");
			else
				set_neg.add(tmp);

		}

		// 3. calculate the minimal set
		HashSet<ArrayList<Integer>> duplicated = new HashSet<>();
		for (ArrayList<Integer> s1 : set) {
			for (ArrayList<Integer> s2 : set) {
				if (s1.equals(s2)) {
//					System.out.println("equal");
				} else {
					if (s1.containsAll(s2)) {
						duplicated.add(s1);
//						System.out.println("contains");
					}
//					else {
//						System.out.println("not contains");
//					}
				}
			}
		}

		for (ArrayList<Integer> rm : duplicated) {
			set.remove(rm);
		}
		// ----------------
		HashSet<ArrayList<Integer>> duplicated2 = new HashSet<>();
		for (ArrayList<Integer> s1 : set_neg) {
			for (ArrayList<Integer> s2 : set_neg) {

				if (!s1.equals(s2) && s1.containsAll(s2)) {
					duplicated2.add(s1);
				}

			}
		}

		for (ArrayList<Integer> rm : duplicated2) {
			set_neg.remove(rm);
		}

		HashSet<String> allEntryMarks = new HashSet<>();
		// 4. export result
		HashSet<EntryMark> entries = new HashSet<>();
		for (ArrayList<Integer> s : set) {
			HashSet<String> clazzMarks = new HashSet<>();
			HashSet<String> mtdMarks = new HashSet<>();

			for (Integer i : s) {
				String mark = feathers.get(i);
				if (mark.startsWith("[class]")) {
					clazzMarks.add(mark.substring("[class]".length()));
				} else if (mark.startsWith("[method]")) {
					mtdMarks.add(mark.substring("[method]".length()));
				} else {
					System.err.println("--------------");
				}
			}

			entries.add(new EntryMark(clazzMarks, mtdMarks));
			allEntryMarks.addAll(clazzMarks);
			allEntryMarks.addAll(mtdMarks);
		}

		// ------

		HashSet<EntryMark> not_entries = new HashSet<>();
		for (ArrayList<Integer> s : set_neg) {
			HashSet<String> clazzMarks = new HashSet<>();
			HashSet<String> mtdMarks = new HashSet<>();

			for (Integer i : s) {
				String mark = feathers.get(i);
				if (mark.startsWith("[class]")) {
					clazzMarks.add(mark.substring("[class]".length()));
				} else if (mark.startsWith("[method]")) {
					mtdMarks.add(mark.substring("[method]".length()));
				} else {
					System.err.println("--------------");
				}
			}

			not_entries.add(new EntryMark(clazzMarks, mtdMarks));
		}

		AnswerExporter2 exporter = new AnswerExporter2(entries, not_entries, null, null);
		try {
			exporter.export2Json();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("..");
	}

	private static int[][] readVerifiedData(ArrayList<String> feathers, String excelPath, String sheetName) {
		try {
			FileInputStream excelFile = new FileInputStream(excelPath);
			Workbook workbook = Workbook.getWorkbook(excelFile);
			Sheet excelSheet = workbook.getSheet(sheetName);

			int rows = excelSheet.getRows();
			Cell[] cell = excelSheet.getRow(0);
			int columnNum = cell.length;

			// 1. read feathers
			for (int col = 0; col < columnNum; ++col) {
				String ret = ReadData(excelSheet, 0, col);
				feathers.add(ret);
			}

			// 2. read subs
			int[][] vals = new int[rows - 1][columnNum];
			for (int row = 1; row < rows; row++) {
				for (int col = 0; col < columnNum; col++) {
					String ret = ReadData(excelSheet, row, col);
					Integer i = Integer.valueOf(ret);
					if (i != null) {
						vals[row - 1][col] = i;
					} else {
						System.out.println("error");
					}
				}
			}

			workbook.close();

			return vals;
		} catch (FileNotFoundException e) {
			System.out.println("找不到该文件");
		} catch (BiffException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;

	}

	public static void readExcel(ArrayList<Integer> res, String excelPath, String sheetName) {
		try {
			FileInputStream excelFile = new FileInputStream(excelPath);
			Workbook workbook = Workbook.getWorkbook(excelFile);
			Sheet excelSheet = workbook.getSheet(sheetName);

			int rows = excelSheet.getRows();
			Cell[] cell = excelSheet.getRow(0);
			int columnNum = cell.length;

//			if (columnNum != 1) {
//				System.out.println("error");
//				return;
//			}

			for (int row = 1; row < rows; ++row) {
				int col = 2;
				String ret = ReadData(excelSheet, row, col);
				Integer i = Integer.valueOf(ret);
				if (i != null) {
					res.add(i);
				} else {
					System.out.println("error");
				}
//				for (int col = 0; col < columnNum; ++col) {
//					String ret = ReadData(excelSheet, row, col);
//					Integer i = Integer.valueOf(ret);
//					if (i != null) {
//						res.add(i);
//					} else {
//						System.out.println("error");
//					}
//				}
			}
			workbook.close();
		} catch (FileNotFoundException e) {
			System.out.println("找不到该文件");
		} catch (BiffException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String ReadData(Sheet excelSheet, int row, int col) {
		try {
			String CellData = "";
			Cell cell = excelSheet.getRow(row)[col];
			CellData = cell.getContents().toString();
			return CellData;
		} catch (Exception e) {
			return "";
		}
	}
}
