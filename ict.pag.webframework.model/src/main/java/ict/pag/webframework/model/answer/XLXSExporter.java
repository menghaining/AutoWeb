package ict.pag.webframework.model.answer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class XLXSExporter {
	private String userDir = System.getProperty("user.dir");
	private String root;

	public XLXSExporter() {
		if (!userDir.endsWith(File.separator)) {
			userDir = userDir + File.separator;
		}
		root = userDir + "result-matrix" + File.separator;
		File dir = new File(root);
		if (!dir.exists())
			dir.mkdirs();
	}

	/**
	 * @FileName: endsWith .xlsx
	 */
	public void exportPositiveAndNegtiveData(ArrayList<HashSet<Integer>> positiveFeathersSet,
			ArrayList<HashSet<Integer>> negativeFeathersSet, ArrayList<String> feathers, String fileName,
			String sheetName, boolean append) throws IOException {
		File file = new File(root + fileName);
		System.out.println("XLXS File write to " + file);
		if (!file.exists()) {
			file.createNewFile();
		} else {
			if (!append) {
				file.delete();
				file.createNewFile();
			}
		}

		Workbook workbook = null;
		String fileType = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
		if (fileType.equals("xls")) {
			workbook = new HSSFWorkbook();
		} else if (fileType.equals("xlsx")) {
			workbook = new XSSFWorkbook();
		}

		if (workbook != null) {
			Sheet sheet = workbook.getSheet(sheetName);
			if (sheet == null) {
				sheet = workbook.createSheet(sheetName);
			}

			int rowNum = 0;

			// 1. add attributes: features name
			Row row0 = sheet.createRow(rowNum);
			for (int i = 0; i < feathers.size(); i++) {
				String feather = feathers.get(i);
				Cell cell = row0.createCell(i);
				cell.setCellValue(feather);
			}
			row0.createCell(feathers.size()).setCellValue("IS");
			row0.createCell(feathers.size() + 1).setCellValue("NOT");

			rowNum++;

			// 2. value = 1, means is entry
			for (int i = 0; i < positiveFeathersSet.size(); i++) {
				HashSet<Integer> vals0 = positiveFeathersSet.get(i);

				Row row = sheet.createRow(rowNum);
				for (int j = 0; j < feathers.size(); j++) {
					if (vals0.contains(j)) {
						row.createCell(j).setCellValue("1");
					} else {
						row.createCell(j).setCellValue("0");
					}
				}

				// value
				row.createCell(feathers.size()).setCellValue("1");
				row.createCell(feathers.size() + 1).setCellValue("0");

				rowNum++;
			}

			// 3. value = -1, means not entry
			for (int i = 0; i < negativeFeathersSet.size(); i++) {
				HashSet<Integer> vals0 = negativeFeathersSet.get(i);

				Row row = sheet.createRow(rowNum);
				for (int j = 0; j < feathers.size(); j++) {
					if (vals0.contains(j)) {
						row.createCell(j).setCellValue("1");
					} else {
						row.createCell(j).setCellValue("0");
					}
				}

				// value
				row.createCell(feathers.size()).setCellValue("0");
				row.createCell(feathers.size() + 1).setCellValue("1");

				rowNum++;
			}

			OutputStream stream = new FileOutputStream(file);
			workbook.write(stream);
			stream.close();
		}

	}
}
