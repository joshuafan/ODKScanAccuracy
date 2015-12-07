import java.io.*;
import java.nio.file.*;
import java.util.*;

import javax.json.*;

import org.apache.poi.xssf.usermodel.*;

public class ExcelParser {

	private static final int[] NUM_DATA_COLUMNS = { 11, 22, 33, 42 }; // missing
	// "live_births", 80,
	// 89, 101,
	// 116, 159,
	// 199 };
	private static final int[] JSON_ARRAY_INDEXES = { 4 };

	/**
	 * Crawl entire "output" directory by going through each subdirectory
	 * 
	 * @param expectedData
	 */
	public static void crawlDirectories(Map<String, List<String>> expectedData) {
		System.out.println();
		System.out.println("crawling directories:");
		Path dir = Paths.get("C:\\Users\\Joshua\\Downloads\\scanOutput");
		DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
			public boolean accept(Path file) throws IOException {
				// try {
				return (Files.isDirectory(file));
				/*} catch (IOException x) {
					// Failed to determine if it's a directory.
					System.err.println(x);
					return false;
				}*/
			}
		};
		int[] numCorrect = new int[NUM_DATA_COLUMNS.length];
		int[] numTotal = new int[NUM_DATA_COLUMNS.length];
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {

			// This loops through all subdirectories
			for (Path entry : stream) {
				// Find output.json and client
				Scanner clientIdScanner = new Scanner(new File(entry.toString() + "\\clientID.txt"));
				String clientId = clientIdScanner.next();
				clientIdScanner.close();
				List<String> actualResult = parseActualJsonFile(entry.toString() + "\\output.json");
				List<String> expectedResult = expectedData.get(clientId);

				compareResults(actualResult, expectedResult, numCorrect, numTotal);

				// JsonArray segments = clientIdObject.getJsonArray("segments");

				/*
				for (int s = 0; s < segments.size(); s++) {
					JsonObject segment = segments.getJsonObject(s);
					JsonArray items = segment.getJsonArray("items");
					
					
					for (int i = 0; i < items.size(); i++) {
						JsonObject item = items.getJsonObject(i);
						JsonObject classification = item.getJsonObject("classification");
						int classificationValue = classification.getInt("classification");
						if (classificationValue >= 10 || classificationValue < 0) {
							throw new IllegalStateException("classification value must be a single digit");
						}
						
						System.out.println(classificationValue);
					}
				}*/
				// Map<String, String> estimatedResults = getEstimatedResults();
				// compare estimatedResults, actualResults.get(clientId)*/
			}
		} catch (IOException x) {
			System.err.println(x);
		}
	}

	private static void compareResults(List<String> actualResult, List<String> expectedResult, int[] numCorrect,
	        int[] numTotal) {
		assert actualResult.size() == expectedResult.size();
		for (int i = 0; i < actualResult.size(); i++) {
			String actual = trimTrailingZeroes(actualResult.get(i));
			String expected = trimTrailingZeroes(expectedResult.get(i));

			// If the result contains a slash, assume that it is a date
			if (actual.contains("/")) {
				String[] actualDate = actual.split("/");
				String[] expectedDate = actual.split("/");
				if (actualDate.length == 3 && expectedDate.length == 3) {
					for (int j = 0; j < actualDate.length; j++) {
						int[] comparison = compareNumberStrings(actualDate[j], expectedDate[j]);
						numCorrect[i] += comparison[0];
						numTotal[i] += comparison[1];
					}
				} else {
					System.out.println("Date did not have two slashes: " + actual);
					throw new IllegalArgumentException();
				}
			} else if (actual.charAt(0) == '[') {
				// compare by words
			} else {
				int[] comparison = compareNumberStrings(actual, expected);
				numCorrect[i] += comparison[0];
				numTotal[i] += comparison[1];
			}
		}
	}

	private static int[] compareNumberStrings(String actual, String expected) {
		int numTotal = 0;
		int numSame = 0;
		int actualIndex = actual.length() - 1;
		int expectedIndex = expected.length() - 1;
		while (actualIndex >= 0 && expectedIndex >= 0) {
			// Ignore non-alphabetic characters
			if (actual.charAt(actualIndex) < '0' || actual.charAt(actualIndex) > '9') {
				actualIndex--;
				expectedIndex--;
				continue;
			}
			if (actual.charAt(actualIndex) == expected.charAt(expectedIndex)) {
				numTotal++;
				numSame++;
			} else {
				numTotal++;
			}
		}
		return new int[] { numSame, numTotal };
	}

	public static void main(String[] args) {
		String correctFileName = "src/data/Master Excel_with column codes_a";
		Map<String, List<String>> data = parseCorrectFile(correctFileName);
		crawlDirectories(data);
	}

	/**
	 * Parses the actual JSON file, and returns a list of the values stored in
	 * the JSON at the indexes specified by JSON_ARRAY_INDEXES.
	 * 
	 * @param file The path to the JSON file to parse
	 * @return A list of the entries stored in the JSON at the indexes specified
	 *         by JSON_ARRAY_INDEXES.
	 */
	public static List<String> parseActualJsonFile(String file) {
		FileReader reader = null;
		try {
			reader = new FileReader(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		JsonReader jsonReader = Json.createReader(reader);
		JsonObject object = jsonReader.readObject();
		JsonArray array = object.getJsonArray("fields");
		List<String> actualData = new ArrayList<String>();

		// For each field that we are extracting from the JSON file, grab the
		// corresponding object, and store its "value" in the list.
		for (int i = 0; i < JSON_ARRAY_INDEXES.length; i++) {
			JsonObject currentObject = array.getJsonObject(JSON_ARRAY_INDEXES[i]);
			String classificationValue = currentObject.getString("value");
			System.out.println("value: " + classificationValue);
			actualData.add(classificationValue);
		}
		return actualData;
	}

	// Will return a Map(client ID -> Map(column title -> data))
	public static Map<String, List<String>> parseCorrectFile(String file) {
		Map<String, List<String>> data = new HashMap<String, List<String>>();
		try {
			XSSFWorkbook wb = ExcelParser.readFile(file);
			String[] sheetNames = new String[] { "#1", "#2", "#3" };

			// For each sheet, iterate through all rows except for the 0th row
			for (String sheetName : sheetNames) {
				XSSFSheet sheet = wb.getSheet(sheetName);
				int rows = sheet.getPhysicalNumberOfRows();
				XSSFRow firstRow = sheet.getRow(0);

				for (int r = 1; r < rows; r++) {
					System.out.println("ROW " + r);
					List<String> currentRowData = new ArrayList<String>();
					XSSFRow row = sheet.getRow(r);
					if (row == null) {
						continue;
					}
					String clientId = getStringCellContent(row.getCell(11));

					for (int c : NUM_DATA_COLUMNS) {
						XSSFCell cell = row.getCell(c);
						String value = getStringCellContent(cell);

						String columnName = firstRow.getCell(c).getStringCellValue();

						System.out.println("column name:" + columnName + " value: " + value);
						currentRowData.add(value);
					}
					data.put(clientId, currentRowData);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;

	}

	private static XSSFWorkbook readFile(String filename) throws IOException {
		FileInputStream fis = new FileInputStream(filename);
		try {
			return new XSSFWorkbook(fis);
		} finally {
			fis.close();
		}
	}

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

	private static String trimTrailingZeroes(String s) {
		int firstNonZeroCharIndex = 0;
		while (s.charAt(firstNonZeroCharIndex) == '0') {
			firstNonZeroCharIndex++;
		}
		return s.substring(firstNonZeroCharIndex);
	}
}
