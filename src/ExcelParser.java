import java.io.*;
import java.util.*;

import org.apache.poi.xssf.usermodel.*;

/**
 * A utility class that parses the Excel document to determine the correct
 * (expected) values that Scan should produce
 */
public class ExcelParser {
	public static final int CLIENT_ID_COLUMN = 15;

	// Will return a Map(client ID -> Map(column title -> data))
	public static Map<String, List<String>> parseCorrectFile(String file, String[] dataColumns) {
		Map<String, List<String>> data = new HashMap<String, List<String>>();
		try {
			XSSFWorkbook wb = ExcelParser.readFile(file);
			String[] sheetNames = new String[] { "#2", "#3" };

			// For each sheet, iterate through all rows except for the 0th row
			for (String sheetName : sheetNames) {
				XSSFSheet sheet = wb.getSheet(sheetName);
				int rows = sheet.getPhysicalNumberOfRows();

				for (int r = 1; r < rows; r++) {
					List<String> currentRowData = new ArrayList<String>();
					XSSFRow row = sheet.getRow(r);
					if (row == null) {
						continue;
					}

					// Get the correct client ID from the table for that row
					String clientId = AccuracyChecker
					        .trimTrailingZeroes(getStringCellContent(row.getCell(CLIENT_ID_COLUMN)));

					// Collect data from all specified columns
					for (String colLetters : dataColumns) {
						int columnIndex = toIndex(colLetters);
						XSSFCell cell = row.getCell(columnIndex);
						String value = getStringCellContent(cell);
						currentRowData.add(value);
					}
					System.out.println("Processing client id " + clientId + " in excel document.");
					// Add this row to the map (with the clientID as a key)
					data.put(clientId, currentRowData);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;

	}

	/*
	 * Reads an XSSFWorkbook object from the given Excel file.
	 */
	private static XSSFWorkbook readFile(String filename) throws IOException {
		FileInputStream fis = new FileInputStream(filename);
		try {
			return new XSSFWorkbook(fis);
		} finally {
			fis.close();
		}
	}

	/*
	 * Gets a String version of the contents of the given cell.
	 */
	private static String getStringCellContent(XSSFCell cell) {
		if (null == cell) {
			return "";
		}
		String value = null;

		switch (cell.getCellType()) {

		case XSSFCell.CELL_TYPE_FORMULA:
			value = cell.getCellFormula();
			break;

		case XSSFCell.CELL_TYPE_NUMERIC:
			value = String.valueOf((int) cell.getNumericCellValue());
			break;

		case XSSFCell.CELL_TYPE_STRING:
			value = cell.getStringCellValue();
			break;

		default:
		}
		return value;
	}

	private static int toIndex(String columnLetters) {
		int total = 0;
		int power = 1;
		for (int i = columnLetters.length() - 1; i >= 0; i--) {
			char letter = columnLetters.charAt(i);
			int letterIndex = letter - 'A' + 1;
			total += (letterIndex * power);
			power *= 26;
		}
		total--;
		System.out.println("Column " + columnLetters + " to " + total);
		return total;
	}
}
