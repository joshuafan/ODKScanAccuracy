import java.io.*;
import java.util.*;

import org.apache.poi.xssf.usermodel.*;

/**
 * A utility class that parses the Excel document to determine the correct
 * (expected) values that Scan should produce
 */
public class ExcelParser {

	private static final int[] NUM_DATA_COLUMNS = { 11, 22, 33, 42, 80, 89, 101, 116, 159, 199 };
	public static final int CLIENT_ID_COLUMN = 15;

	// Will return a Map(client ID -> Map(column title -> data))
	public static Map<String, List<String>> parseCorrectFile(String file) {
		Map<String, List<String>> data = new HashMap<String, List<String>>();
		try {
			XSSFWorkbook wb = ExcelParser.readFile(file);
			String[] sheetNames = new String[] { "#3", "Sheet1" };

			// For each sheet, iterate through all rows except for the 0th row
			for (String sheetName : sheetNames) {
				XSSFSheet sheet = wb.getSheet(sheetName);
				int rows = sheet.getPhysicalNumberOfRows();

				for (int r = 1; r < rows; r++) {
					System.out.println("ROW " + r);
					List<String> currentRowData = new ArrayList<String>();
					XSSFRow row = sheet.getRow(r);
					if (row == null) {
						continue;
					}

					// Get the correct client ID from the table for that row
					String clientId = getStringCellContent(row.getCell(CLIENT_ID_COLUMN));
					System.out.println("Client ID: " + clientId);

					// Collect data from all specified columns
					for (int c : NUM_DATA_COLUMNS) {
						XSSFCell cell = row.getCell(c);
						String value = getStringCellContent(cell);
						currentRowData.add(value);
					}

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
			value = String.valueOf(cell.getNumericCellValue());
			break;

		case XSSFCell.CELL_TYPE_STRING:
			value = cell.getStringCellValue();
			break;

		default:
		}
		return value;
	}
}
