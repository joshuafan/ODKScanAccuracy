import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AccuracyChecker {
	private static final String CURRENT_FOLDER_PATH = "C:\\Users\\Joshua\\Downloads\\scanOutput";
	private static final String[] EXCEL_DATA_COLUMNS = { "P" }; // "W", "AH",
	                                                            // "AQ", "BZ",
	                                                            // "CC", "CL",
	                                                            // "CX", "DM",
	                                                            // "FD", "GR",
	// "BY", "BR", "EI", "GD" };

	public static void main(String[] args) {
		String correctFileName = "src/data/Master Excel_with column codes_a.xlsx";
		Map<String, List<String>> data = ExcelParser.parseCorrectFile(correctFileName, EXCEL_DATA_COLUMNS);
		crawlDirectories(data);
	}

	/**
	 * Crawl entire "output" directory by going through each sub-directory.
	 * Compares the data stored in the "output.json" file with the expected
	 * data, and prints out the accuracy rate of each field.
	 * 
	 * @param expectedData
	 */
	public static void crawlDirectories(Map<String, List<String>> expectedData) {

		// Filter all items in CURRENT_FOLDER_PATH to select the
		// sub-directories
		Path dir = Paths.get(CURRENT_FOLDER_PATH);
		DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
			public boolean accept(Path file) throws IOException {
				return (Files.isDirectory(file));
			}
		};

		// Build up records of the number of correct vs. total digits for each
		// field
		int[] numCorrect = new int[EXCEL_DATA_COLUMNS.length];
		int[] numTotal = new int[EXCEL_DATA_COLUMNS.length];
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {

			// This loops through all sub-directories
			for (Path entry : stream) {
				// Get the client ID of the current sub-directory
				System.out.println(entry.toString() + "\\clientID.txt");
				Scanner clientIdScanner = new Scanner(new File(entry.toString() + "\\clientID.txt"));
				String clientId = clientIdScanner.next();
				clientIdScanner.close();

				// Parse the actual results from the JSON, as well as the
				// correct results from the corresponding row in the Excel file
				// (with the matching ClientID)
				List<String> actualResult = JsonParser.parseActualJsonFile(entry.toString() + "\\output.json");
				System.out.println("Client ID: " + clientId);
				List<String> expectedResult = expectedData.get(clientId);
				if (null == expectedResult) {
					System.out.println("No matching Excel data for client ID " + clientId + "!");
					continue;
				}
				actualResult.remove(actualResult.size() - 1);
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

	public static void compareResults(List<String> actualResult, List<String> expectedResult, int[] numCorrect,
	        int[] numTotal) {
		assert actualResult.size() == expectedResult.size();
		for (int i = 0; i < expectedResult.size(); i++) {
			String actual = trimTrailingZeroes(actualResult.get(i));
			String expected = trimTrailingZeroes(expectedResult.get(i));
			System.out.println("Field " + i + ": actual = " + actual + ", expected = " + expected);
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
			} else if (actual.equals("yes") || actual.equals("no") || actual.equals("null")) {
				numTotal[i]++;
				if (actual.equals(expected)) {
					numCorrect[i]++;
				}
			} else {
				int[] comparison = compareNumberStrings(actual, expected);
				numCorrect[i] += comparison[0];
				numTotal[i] += comparison[1];
			}
		}

		for (int i = 0; i < numCorrect.length; i++) {
			System.out.println("Field " + i + ": Correct = " + numCorrect[i] + ", Total = " + numTotal[i]);
		}
	}

	/*
	 * Compares two numerical-digit strings ("actual" and "expected") with each
	 * other, and returns an array which contains the number of matching/correct
	 * digits at index 0, and the total number of digits at index 1. Note that
	 * all non-digit characters are ignored. If the lengths of the strings are
	 * unequal, compares starting from the right and ignores extraneous
	 * characters on the left side of the longer string.
	 */
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
				actualIndex--;
				expectedIndex--;
			} else {
				numTotal++;
				actualIndex--;
				expectedIndex--;
			}
		}
		return new int[] { numSame, numTotal };
	}

	/**
	 * Trims any trailing zeroes from the given string.
	 * 
	 * @param s
	 * @return
	 */
	public static String trimTrailingZeroes(String s) {
		if (s == null) {
			System.out.println("String is null!");
			return null;
		}
		int firstNonZeroCharIndex = 0;
		while (firstNonZeroCharIndex < s.length() && s.charAt(firstNonZeroCharIndex) == '0') {
			firstNonZeroCharIndex++;
		}
		return s.substring(firstNonZeroCharIndex);
	}
}
