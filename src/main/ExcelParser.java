package main;

import java.io.*;
import java.util.*;

import org.apache.poi.xssf.usermodel.*;

/**
 * A utility class that parses the Excel document to determine the correct
 * (expected) values that Scan should produce.
 */
public class ExcelParser {
    // The column index containing the correct Client ID
    public static final int CLIENT_ID_COLUMN = 15;

    /**
     * Parses the Excel file containing the expected data, and returns the data
     * as a Map from each Client ID to a List of Strings representing the values
     * of each field. (The fields to be extracted are specified in the
     * "dataColumns" parameter.) Note that any IDs which appear multiple times
     * in the Excel file are not included in the returned map.
     * 
     * @param file The Excel file to parse
     * @param sheets The sheets within that Excel file to parse
     * @param dataColumns The indexes of the columns to extract
     * 
     * @return a Map from (client ID) to a List of Strings, which contains the
     *         expected results for that Client ID for each requested field.
     *         Note that any duplicate Client IDs are excluded.
     */
    public static Map<String, List<String>> parseCorrectFile(String file, String[] sheets, String[] dataColumns) {
        Map<String, List<String>> data = new HashMap<String, List<String>>();
        try {
            XSSFWorkbook wb = ExcelParser.readFile(file);

            // We don't want to include any IDs that appear in multiple rows, so
            // keep track of those
            Set<String> duplicateClientIds = new HashSet<String>();

            // For each sheet, iterate through all rows except for the 0th row
            for (String sheetName : sheets) {
                XSSFSheet sheet = wb.getSheet(sheetName);

                // Iterate through each data row
                for (int r = 1; r < sheet.getPhysicalNumberOfRows(); r++) {
                    List<String> currentRowData = new ArrayList<String>();
                    XSSFRow row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }

                    // Get the correct client ID from the table for that row.
                    // Note that any trailing zeroes are trimmed from the Client
                    // ID.
                    String clientId = AccuracyChecker
                            .trimTrailingZeroes(getStringCellContent(row.getCell(CLIENT_ID_COLUMN)));

                    // If this is a duplicate ID, we throw that ID out from the
                    // collected data
                    if (data.containsKey(clientId)) {
                        data.remove(clientId);
                        duplicateClientIds.add(clientId);
                        continue;
                    }
                    if (duplicateClientIds.contains(clientId)) {
                        continue;
                    }

                    // Collect data from all specified columns and put each
                    // piece of data into the List
                    for (String colLetters : dataColumns) {
                        int columnIndex = toIndex(colLetters);
                        XSSFCell cell = row.getCell(columnIndex);
                        String value = getStringCellContent(cell);
                        currentRowData.add(value);
                    }

                    // Add this row to the map (with the clientID as a key)
                    // XSSFCell actualExcelValue = row.getCell(toIndex("Z"));
                    // currentRowData.add(getStringCellContent(actualExcelValue));
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
