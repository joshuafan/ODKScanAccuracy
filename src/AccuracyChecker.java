import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * This program compares the data that Scan read from some forms (which is
 * contained in an output folder) to the actual correct values of the form
 * (listed in an Excel file). Then, prints out statistics on how accurately Scan
 * was able to read/process the forms.
 * 
 * Note: for this program to build, you will need to download Apache's POI Excel
 * API (https://poi.apache.org/download.html) and the JSON Processing API
 * (https://jsonp.java.net/download.html), and reference those in the project.
 * 
 * In particular, you will need to reference these libraries:
 * 
 * poi-3.13/poi-3.13-20150929.jar, poi-3.13/poi-ooxml-3.13-20150929.jar,
 * poi-3.13/poi-ooxml-schemas-3.13-20150929.jar,
 * poi-3.13/ooxml-lib/xmlbeans-2.6.0.jar, javax.json-api-1.0.jar,
 * javax.json-1.0.4.jar
 */
public class AccuracyChecker {
	// private static final String CURRENT_FOLDER_PATH =
	// "C:\\Users\\Joshua\\Downloads\\scanOutput";
	private static final String EXCEL_FILE = "src/data/Master Excel_with column codes_a.xlsx";

	// The letters of the Excel columns to check
	private static final String[] EXCEL_DATA_COLUMNS = { // "P" // client ID
	        "AA", // age
			/*"AP", // EDD
			"BB", // num_preg
			"BM", // live_births
			"BY", // regCCPF (bubble)
			"CK", // CCPF_form (bubble)
			"CZ", // monthpreg_ANC
			"DH", // ANC_v1
			"EB", // ANC_v3
			"EW", // TTV2
			"GD", // health_cond (bubble many)
			"HF", // date_delivery
			"IY", // V1_topics (bubble many)
			"JH" // V2_date*/
	};

	/**
	 * Runs the Scan accuracy checker.
	 * 
	 * Command-line argument:
	 * 
	 * [0]: The path to the root of the folder containing the Scan output. The
	 * folder should contain sub-folders for each form that was scanned.
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Command-line argument: <Root of scan output folder>");
			System.exit(1);
		}
		String scanOutputRoot = args[0];
		Map<String, List<String>> expectedData = ExcelParser.parseCorrectFile(EXCEL_FILE, EXCEL_DATA_COLUMNS);
		Map<String, List<String>> actualOutput = crawlDirectories(scanOutputRoot);
		compareResults(actualOutput, expectedData);
	}

	/**
	 * Crawl entire "output" directory by going through each sub-directory.
	 * Compares the data stored in the "output.json" file with the expected
	 * data, and prints out the accuracy rate of each field.
	 * 
	 * @param scanOutputRoot The root of the scan output directory; this folder
	 *        should contain sub-folders for each form that was scanned.
	 * @return A map containing the actual data outputted by Scan
	 */
	public static Map<String, List<String>> crawlDirectories(String scanOutputRoot) {

		// Filter all items in CURRENT_FOLDER_PATH to select the
		// sub-directories
		Path dir = Paths.get(scanOutputRoot);
		DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
			public boolean accept(Path file) throws IOException {
				return (Files.isDirectory(file));
			}
		};

		Set<String> ids = new TreeSet<String>();
		Map<String, List<String>> actualData = new HashMap<String, List<String>>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {

			// This loops through all sub-directories
			for (Path entry : stream) {
				// Get the client ID of the current sub-directory
				Scanner clientIdScanner = new Scanner(new File(entry.toString() + "\\clientID.txt"));
				String clientId = clientIdScanner.next();
				clientIdScanner.close();

				// Parse the actual results from the JSON, as well as the
				// correct results from the corresponding row in the Excel file
				// (with the matching ClientID)
				List<String> actualResult = JsonParser.parseActualJsonFile(entry.toString() + "\\output.json");
				System.out.println("Folder: " + entry.toString() + "\tClient ID: " + clientId);
				if (!ids.add(clientId)) {
					System.out.println("DUPLICATE ID " + clientId);
				}
				actualData.put(clientId, actualResult);
			}
		} catch (IOException x) {
			System.err.println(x);
		}
		return actualData;
	}

	/**
	 * 
	 * @param actual
	 * @param expected A map containing the actual verified data for each form.
	 *        This map is stored as a map from a Client ID (representing the
	 *        Client ID of a particular form) to a List of Strings that
	 *        represent the correct values for each field within that form.
	 */
	public static void compareResults(Map<String, List<String>> actual, Map<String, List<String>> expected) {
		int[] numCorrect = new int[EXCEL_DATA_COLUMNS.length];
		int[] numTotal = new int[EXCEL_DATA_COLUMNS.length];
		for (String clientId : expected.keySet()) {
			List<String> actualResults = actual.get(clientId);
			if (actualResults == null) {
				System.out.println("Client ID " + clientId + " is ONLY found in Excel file.");
			} else {
				List<String> expectedResults = expected.get(clientId);

				String excelValue = expectedResults.get(1);
				String actualValue = actualResults.get(0);
				if (excelValue != null && !excelValue.equals(actualValue)) {
					System.out.println("Client ID " + clientId + ": Scan discrepancy - excel " + excelValue + " vs. "
					        + actualValue);
				}
				compareResults(actualResults, expectedResults, numCorrect, numTotal, clientId);
			}
		}

		// Print out final results
		System.out.println("FINAL RESULTS");
		for (int i = 0; i < numCorrect.length; i++) {
			System.out.println("Index " + i + ": " + numCorrect[i] + "/" + numTotal[i] + " correct");
		}

		Set<String> matching = new TreeSet<String>();
		Set<String> onlyExcel = new TreeSet<String>();
		Set<String> notInExcel = new TreeSet<String>();
		for (String s : actual.keySet()) {
			if (s != null && expected.containsKey(s)) {
				matching.add(s);
			} else {
				notInExcel.add(s);
			}
		}
		for (String s : expected.keySet()) {
			if (s != null && !actual.containsKey(s))
				onlyExcel.add(s);
		}

		System.out.println();
		System.out.println("Matching Client IDs (" + matching.size() + "):");
		for (String s : matching) {
			System.out.println(s);
		}
		System.out.println();
		System.out.println("Only in Excel file (" + onlyExcel.size() + "):");
		for (String s : onlyExcel) {
			System.out.println(s);
		}
		System.out.println();
		System.out.println("Not in Excel file (" + notInExcel.size() + "):");
		for (String s : notInExcel) {
			System.out.println(s);
		}
	}

	/*
	 * For a single instance of a form, compares the correct values of the form
	 * (passed in as "expectedResult") with the results obtained by Scan after
	 * it processes the form (passed in as "actualResult"). Note that
	 * "actualResult" and "expectedResult" must have corresponding indexes; for
	 * example, index 1 of actualResult and index 1 of expectedResult must
	 * represent the result of the same field.
	 * 
	 * @requires No parameters are null, actualResult.size() ==
	 *           expectedResult.size(), numCorrect.length ==
	 *           actualResult.size(), numTotal.length == actualResult.size()
	 * @param actualResult List of form values produced by Scan
	 * @param expectedResult List of correct/expected form values
	 * @param numCorrect An output array that will hold the number of correct
	 *        digits for each field.
	 * @param numTotal An output array that will hold the number of total
	 *        characters for each field.
	 */
	public static void compareResults(List<String> actualResult, List<String> expectedResult, int[] numCorrect,
	        int[] numTotal, String clientId) {
		assert actualResult.size() == expectedResult.size();

		// Loop through each field in the form
		for (int i = 0; i < actualResult.size(); i++) {

			// Grab the correct expected value for that field in the form, as
			// well as the value that Scan produced.

			// String actual = trimTrailingZeroes(actualResult.get(i));
			// String expected = trimTrailingZeroes(expectedResult.get(i));
			String actual = actualResult.get(i);
			String expected = expectedResult.get(i);

			// If either the expected or actual value of that field is null or
			// empty, move on to the next field
			if (actual == null || expected == null || expected.equals("null")) {
				continue;
			}

			// If the strings represent dates, process them
			if (isDate(expected)) {
				String[] actualDate = actual.split("/");
				String[] expectedDate = expected.split("/");
				if (isDate(actual)) {
					for (int j = 0; j < actualDate.length; j++) {
						String actualDateSection = actualDate[j];
						String expectedDateSection = expectedDate[j];

						// Handle the special case where the year is a 4-digit
						// string. Since the data was recorded inconsistently,
						// we will only compare the last two digits of the year
						// in this case.
						if (j == 2) {
							if (actualDateSection.length() == 4) {
								actualDateSection = actualDateSection.substring(2);
							}
							if (expectedDateSection.length() == 4) {
								expectedDateSection = expectedDateSection.substring(2);
							}
						}

						int[] comparison = compareNumberStrings(actualDateSection, expectedDateSection);
						numCorrect[i] += comparison[0];
						numTotal[i] += comparison[1];
						System.out.println("Field " + i + ": actual = " + actual + ", expected = " + expected + " ("
						        + comparison[0] + "/" + comparison[1] + " correct)");
					}
				}
			} else if (expected.length() > 0 && expected.charAt(0) == '[') {
				// compare by words
			} else if (isNumber(expected)) {
				int[] comparison = compareNumberStrings(actual, expected);
				numCorrect[i] += comparison[0];
				numTotal[i] += comparison[1];
				System.out.println("ClientId " + clientId + ": actual = " + actual + ", expected = " + expected + " ("
				        + comparison[0] + "/" + comparison[1] + " correct)");
			} else {
				numTotal[i]++;
				if (actual.trim().equals(expected.trim())
				        || (expected.trim().equals("null") && actual.trim().equals(""))) {
					numCorrect[i]++;
					System.out
					        .println("Field " + i + ": actual = " + actual + ", expected = " + expected + " (correct)");
				} else {
					System.out.println("Field " + i + ": actual = " + actual + ", expected = " + expected + " (wrong)");
				}

			}
		}
	}

	/*
	 * Returns true iff the string contains at least one numerical character.
	 */
	private static boolean isNumber(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) >= '0' && s.charAt(i) <= '9') {
				return true;
			}
		}
		return false;
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
			// Ignore characters that we expect to be non-numeric
			if (expected.charAt(expectedIndex) < '0' || expected.charAt(expectedIndex) > '9') {
				actualIndex--;
				expectedIndex--;
				continue;
			}
			if (actual.charAt(actualIndex) == expected.charAt(expectedIndex)) {
				numSame++;
				numTotal++;
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
	 * Trims any leading zeroes from the given string. However, if a string
	 * contains only zeroes, a String with a single "0" is returned.
	 * 
	 * @param s The string to trim
	 * @return A string without the leading zeroes
	 */
	public static String trimTrailingZeroes(String s) {
		if (s == null) {
			System.out.println("String is null!");
			return null;
		}
		int firstNonZeroCharIndex = 0;

		// If the string is just "0", then retain that single zero. Otherwise,
		// chop off all zeroes.
		while (firstNonZeroCharIndex < s.length() - 1 && s.charAt(firstNonZeroCharIndex) == '0') {
			firstNonZeroCharIndex++;
		}
		return s.substring(firstNonZeroCharIndex);
	}

	/*
	 * Attempts to determine if the given string represents a date.
	 * 
	 * @param s The string to analyze
	 * @return true if the String can be parsed as a date
	 */
	private static boolean isDate(String s) {
		if (s == null || s.length() <= 2) {
			return false;
		}

		// Check to make sure there are two "slashes"
		String[] parts = s.split("/");
		if (parts.length != 3) {
			return false;
		}
		for (int i = 0; i < parts.length; i++) {
			if (parts[i].length() > 4 || parts[i].length() < 1) {
				return false;
			}
			for (int j = 0; j < parts[i].length(); j++) {
				char currentCharacter = parts[i].charAt(j);

				// If any character that is not a digit or space is found,
				// return false.
				if (currentCharacter != ' ' && (currentCharacter > '9' || currentCharacter < '0')) {
					return false;
				}
			}
		}
		return true;
	}
}
