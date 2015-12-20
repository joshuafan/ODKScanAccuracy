import java.io.*;
import java.util.*;

import org.apache.poi.xssf.usermodel.*;

/**
 * A utility class that parses the Excel document to determine the correct
 * (expected) values that Scan should produce.
 */
public class ExcelParser {
	public static final int CLIENT_ID_COLUMN = 15;

	// Will return a Map(client ID -> Map(column title -> data))
	public static Map<String, List<String>> parseCorrectFile(String file, String[] dataColumns) {
		Map<String, List<String>> data = new HashMap<String, List<String>>();
		try {
			XSSFWorkbook wb = ExcelParser.readFile(file);
			String[] sheetNames = new String[] { "#3" };

			// For each sheet, iterate through all rows except for the 0th row
			for (String sheetName : sheetNames) {
				XSSFSheet sheet = wb.getSheet(sheetName);
				int rows = sheet.getPhysicalNumberOfRows();
				XSSFRow firstRow = sheet.getRow(0);
				System.out.println("COLUMNS:");
				for (String colLetters : dataColumns) {
					int columnIndex = toIndex(colLetters);
					System.out.println(getStringCellContent(firstRow.getCell(columnIndex)));
				}
				System.out.println("Number of rows: " + rows);
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
					// Add this row to the map (with the clientID as a key)
					if (data.containsKey(clientId)) {
						System.out.println("DUPLICATE EXCEL ID " + clientId);
					}
					XSSFCell actualExcelValue = row.getCell(toIndex("Z"));
					currentRowData.add(getStringCellContent(actualExcelValue));
					data.put(clientId, currentRowData);
					System.out.println("Excel client ID " + clientId + " value: " + currentRowData.get(0));
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

	/**
	 * Converts an Excel column index (e.g. BD) to its integer index (if the
	 * Excel table is treated as a zero-based 2-D array)
	 * 
	 * @param columnLetters The letter-name of the column (e.g. BD)
	 * @requires columnLetters is a valid Excel column index and contains only
	 *           upper-case alphabetic characters
	 * @return The integer index of the column
	 */
	private static int toIndex(String columnLetters) {
		int total = 0;
		int power = 1;
		for (int i = columnLetters.length() - 1; i >= 0; i--) {
			char letter = columnLetters.charAt(i);
			if (letter < 'A' || letter > 'Z') {
				throw new IllegalArgumentException("Column index " + columnLetters
				        + " is not a valid Excel column index because it contains an illegal character " + letter
				        + ".");
			}
			int letterIndex = letter - 'A' + 1;
			total += (letterIndex * power);
			power *= 26;
		}
		total--;
		return total;
	}
}
