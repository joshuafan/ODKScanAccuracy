package main;

import java.util.*;

/**
 * This program compares the data that Scan read from some forms (which is
 * contained in an output folder) to the actual correct values of the form
 * (listed in an Excel file). Then, prints out statistics on how accurately Scan
 * was able to read/process the forms.
 * 
 * Note: for this program to build, you you will need to reference these
 * libraries (contained inside the "lib" folder):
 * 
 * poi-3.13/poi-3.13-20150929.jar, poi-3.13/poi-ooxml-3.13-20150929.jar,
 * poi-3.13/poi-ooxml-schemas-3.13-20150929.jar,
 * poi-3.13/ooxml-lib/xmlbeans-2.6.0.jar, javax.json-api-1.0.jar,
 * javax.json-1.0.4.jar
 */
public class AccuracyChecker {
    private static final String EXCEL_FILE = "src/data/Master Excel_with column codes_a.xlsx";

    // The sheets in the Excel file to parse
    private static final String[] EXCEL_SHEETS = { "#3" };

    // The letters of the Excel columns containing the expected data
    private static final String[] EXCEL_DATA_COLUMNS = { "P", // client ID
            "AA", // age
            "AP", // EDD
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
            "JH" // V2_date
    };

    // Whether each field is a bubble field or not.
    private static final boolean[] IS_BUBBLE = { false, false, false, false, false, true, true, false, false, false,
            false, true, false, true, false };

    // Hard-coded strings for the bubble fields
    private static final String[] COLUMN_NAMES = { "client_id", "age", "EDD", "num_preg", "live_births", "regCCPF",
            "CCPF_form", "monthpreg_ANC", "ANC_v1", "ANC_v3", "TTV2", "health_cond", "date_delivery", "V1_topics",
            "V2_date" };

    private static final String[][] HEALTH_CONDITIONS = { { "1", "hypertension/pre-eclampsia" }, { "2", "diabetes" },
            { "3", "under the age of 20" }, { "4", "underweight" }, { "5", "carrying twins or triplets" },
            { "6", "history of preterm delivery" }, { "7", "history of stillbirth or neonatal death" },
            { "8", "other1" }, { "9", "other2" } };

    private static final String[][] V1_TOPICS = { { "1", "pregnancy danger signs" }, { "2", "malaria prophylaxis" },
            { "3", "HIV/TB counseling" }, { "4", "activity level" }, { "5", "nutrition" }, { "6", "birth plan" },
            { "7", "breastfeeding" }, { "8", "family planning" }, { "9", "postnatal danger signs" },
            { "10", "neonatal care/ danger signs" } };

    /**
     * Runs the Scan accuracy checker.
     * 
     * Command-line argument:
     * 
     * [0]: The path to the root of the folder containing the Scan output (i.e.
     * C:\\Users\\Joshua\\Downloads\\scanOutput). The folder should contain
     * sub-folders for each form that was scanned.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Command-line argument: <Root of scan output folder>");
            System.exit(1);
        }
        String scanOutputRoot = args[0];

        // Get the expected data from the Excel file
        Map<String, List<String>> expectedData = ExcelParser.parseCorrectFile(EXCEL_FILE, EXCEL_SHEETS,
                EXCEL_DATA_COLUMNS);

        // Get the actual data outputted by Scan
        Map<String, ScanOutput> actualOutput = JsonParser.crawlDirectories(scanOutputRoot);

        // Compare the results and print out statistics
        compareResults(actualOutput, expectedData);
    }

    /**
     * Compares the expected results listed in the Excel file to the actual
     * results produced by Scan.
     * 
     * @param actual A map containing the actual data produced by Scan. This map
     *        is stored as a map from a Client ID (representing the Client ID of
     *        a particular form) to a ScanOutput object containing the list of
     *        values read by Scan for each field, as well as the name of the
     *        output folder.
     * @param expected A map containing the expected verified data for each
     *        form. This map is stored as a map from a Client ID (representing
     *        the Client ID of a particular form) to a List of Strings that
     *        represent the correct values for each field within that form.
     */
    public static void compareResults(Map<String, ScanOutput> actual, Map<String, List<String>> expected) {
        // Contains the number of correct/total digits for the i-th field
        /*int[] numCorrectLittle = new int[EXCEL_DATA_COLUMNS.length];
        int[] numCorrectModerate = new int[EXCEL_DATA_COLUMNS.length];
        int[] numCorrectMajor = new int[EXCEL_DATA_COLUMNS.length];
        int[] numTotalLittle = new int[EXCEL_DATA_COLUMNS.length];
        int[] numTotalModerate = new int[EXCEL_DATA_COLUMNS.length];
        int[] numTotalMajor = new int[EXCEL_DATA_COLUMNS.length];*/

        int[] numCorrect = new int[EXCEL_DATA_COLUMNS.length];
        int[] numTotal = new int[EXCEL_DATA_COLUMNS.length];

        /*Set<String> littleShadow = FolderUtils
                .getClientIds("C:\\Users\\Joshua\\Downloads\\ScanPreAlignedImages\\shadow-little");
        Set<String> moderateShadow = FolderUtils
                .getClientIds("C:\\Users\\Joshua\\Downloads\\ScanPreAlignedImages\\shadow-moderate");
        Set<String> majorShadow = FolderUtils
                .getClientIds("C:\\Users\\Joshua\\Downloads\\ScanPreAlignedImages\\shadow-major");*/

        /*for (String clientId : littleShadow) {
            List<String> actualResults = actual.get(clientId);
            List<String> expectedResults = expected.get(clientId);
            if (null != actualResults && null != expectedResults) {
                compareResults(actualResults, expectedResults, numCorrectLittle, numTotalLittle, clientId);
                comps++;
            }
        }
        for (String clientId : moderateShadow) {
            List<String> actualResults = actual.get(clientId);
            List<String> expectedResults = expected.get(clientId);
            if (null != actualResults && null != expectedResults) {
                compareResults(actualResults, expectedResults, numCorrectModerate, numTotalModerate, clientId);
                comps++;
            }
        }
        for (String clientId : majorShadow) {
            List<String> actualResults = actual.get(clientId);
            List<String> expectedResults = expected.get(clientId);
            if (null != actualResults && null != expectedResults) {
                compareResults(actualResults, expectedResults, numCorrectMajor, numTotalMajor, clientId);
                comps++;
            }
        }*/

        // Loop through all client IDs in the expected data set, and try to find
        // a matching client ID in the actual data set. If there is a match,
        // compare these results.
        for (String clientId : expected.keySet()) {
            ScanOutput output = actual.get(clientId);
            if (null != output) {
                List<String> actualResults = output.outputData;
                List<String> expectedResults = expected.get(clientId);
                compareResults(actualResults, expectedResults, numCorrect, numTotal, clientId, output.folderName);
            }
        }

        /*printResults(numCorrectLittle, numTotalLittle, "Little shadow");
        printResults(numCorrectModerate, numTotalModerate, "Moderate shadow");
        printResults(numCorrectMajor, numTotalMajor, "Major shadow");*/
        printResults(numCorrect, numTotal, "TOTAL");

        // Stats on how many client IDs we were able to match
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
        System.out.println("Matching Client IDs: " + matching.size());
        System.out.println("Only in Excel file: " + onlyExcel.size());
        System.out.println("Not in Excel file: " + notInExcel.size());
    }

    /**
     * Print out final results.
     * 
     * @param numCorrect number of correct digits or bubbles for each attribute
     * @param numTotal number of total digits or bubbles for each attribute
     * @param s String describing results
     */
    public static void printResults(int[] numCorrect, int[] numTotal, String s) {
        System.out.println();
        System.out.println("Final Results: " + s);
        int bubbleCorrect = 0;
        int bubbleTotal = 0;
        int digitCorrect = 0;
        int digitTotal = 0;

        for (int i = 0; i < numCorrect.length; i++) {
            double percentage = numCorrect[i] * 100.0 / numTotal[i];
            String type = "digit";
            if (IS_BUBBLE[i]) {
                type = "bubble";
            }
            System.out.printf("Field " + i + " (" + COLUMN_NAMES[i] + ": " + type + "): " + numCorrect[i] + "/"
                    + numTotal[i] + " correct (%.2f%%)\n", percentage);
            if (IS_BUBBLE[i]) {
                bubbleCorrect += numCorrect[i];
                bubbleTotal += numTotal[i];
            } else {
                digitCorrect += numCorrect[i];
                digitTotal += numTotal[i];
            }
        }

        // Print out overall combined stats for bubble and digit fields
        System.out.println();
        double bubblePercentage = bubbleCorrect * 100.0 / bubbleTotal;
        System.out.printf("BUBBLE FIELDS: " + bubbleCorrect + "/" + bubbleTotal + " correct (%.2f%%)\n",
                bubblePercentage);
        double digitPercentage = digitCorrect * 100.0 / digitTotal;
        System.out.printf("DIGIT FIELDS: " + digitCorrect + "/" + digitTotal + " correct (%.2f%%)\n", digitPercentage);
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
     * @param clientId The client ID of this form instance
     * @param folderName The output folder associated with this form instance
     */
    public static void compareResults(List<String> actualResult, List<String> expectedResult, int[] numCorrect,
            int[] numTotal, String clientId, String folderName) {
        assert actualResult.size() == expectedResult.size();

        System.out.println("CLIENT ID " + clientId + " (Output folder: " + folderName + ")");

        // Loop through each field in the form
        for (int i = 0; i < actualResult.size(); i++) {

            // Grab the correct expected value for that field in the form, as
            // well as the value that Scan produced.
            String actual = actualResult.get(i);
            String expected = expectedResult.get(i);

            // If either the expected or actual value of that field is null or
            // empty, move on to the next field
            if (actual == null || expected == null || expected.equals("") || expected.equals("null")) {
                continue;
            }

            // If this is a "select-many" bubble field, tell the program what
            // the options were
            String[][] bubbleOptions = null;
            if (i == 11) {
                bubbleOptions = HEALTH_CONDITIONS;
            }
            if (i == 13) {
                bubbleOptions = V1_TOPICS;
            }

            // If this is a Client ID and is not 5 digits, pad the left side
            // with zeroes until it is 5 digits.
            if (i == 0) {
                actual = padWithZeroes(actual);
                expected = padWithZeroes(expected);
            }

            // Compare results
            int[] comparison = compareSingleResult(actual, expected, bubbleOptions);

            // If results were different (# total != # correct), print the
            // discrepancy
            if (comparison[0] != comparison[1]) {
                System.out.println("Field " + i + " (" + COLUMN_NAMES[i] + "): actual = " + actual + ", expected = "
                        + expected + " (" + comparison[0] + "/" + comparison[1] + " correct)");
            }
            numCorrect[i] += comparison[0];
            numTotal[i] += comparison[1];
        }
        System.out.println();
    }

    /**
     * Compares the actual vs. expected results for a single form field.
     * 
     * @param actual The actual result
     * @param expected The expected result
     * @param bubbleOptions If this field was a "bubble many" field, then this
     *        parameter should be set to a 2-D array. The "Expected" string is
     *        supposed to contain a list of numbers representing what bubbles
     *        should have been checked (i.e. "1,2,3,4"), whereas the "actual"
     *        string is supposed to contain a String representing a
     *        concatenation of the strings (e.g.
     *        "pregnancy danger signs malaria prophylaxis HIV/TB counseling").
     *        To compare them, the caller should pass in a 2-D array; each entry
     *        of the array will represent a particular bubble, and will have the
     *        number code of the bubble at index 0 and the bubble's text at
     *        index 1.
     * @requires actual != null && expected != null
     * @return An array, with the number of correct digits in index 0, and the
     *         total number of digits in index 1
     */
    public static int[] compareSingleResult(String actual, String expected, String[][] bubbleOptions) {

        // If either the expected or actual value of that field is null or
        // empty, ignore it
        if (expected.trim().equals("") || expected.trim().equals("null") || expected.trim().equals("inconclusive")) {
            return new int[] { 0, 0 };
        }

        // If the strings represent dates, process them
        if (isDate(expected)) {
            return compareDateStrings(actual, expected);
        }

        // If it is a bubble field, Run through each bubble and determine if the
        // actual "bubble status" (filled or unfilled) matches the expected
        // bubble status.
        else if (bubbleOptions != null) {
            int[] comparison = new int[2];
            comparison[1] = bubbleOptions.length;
            String[] selectedBubblesExpected = expected.split(",");
            for (int i = 0; i < bubbleOptions.length; i++) {
                String numericalCode = bubbleOptions[i][0];
                boolean expectedContains = Arrays.asList(selectedBubblesExpected).contains(numericalCode);
                String bubbleText = bubbleOptions[i][1];
                // boolean expectedContains = expected.contains(bubbleText);
                boolean actualContains = actual.contains(bubbleText);

                // Check if the recorded "bubble status" matches what we expect
                if (expectedContains == actualContains) {
                    comparison[0]++;
                }
            }
            return comparison;
        }

        // If it is a number field, compare it that way
        else if (isNumber(expected)) {
            return compareNumberStrings(actual, expected);
        }

        // Otherwise, assume that it is a "yes/no" field, so check for
        // equality
        else {
            if (actual.trim().equals(expected.trim())) {
                return new int[] { 2, 2 };
            } else {
                return new int[] { 0, 2 };
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
     * Compares two date strings with each other, and returns an array
     * containing the number of matching digits at index 0, and the total
     * number of digits at index 1. Note that non-digit characters are ignored.
     */
    private static int[] compareDateStrings(String actual, String expected) {
        String[] actualDate = actual.split("/");
        String[] expectedDate = expected.split("/");
        int[] result = new int[2];
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
                result[0] += comparison[0];
                result[1] += comparison[1];
            }
        }
        return result;
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
            // Ignore characters that we expect to be non-numeric (and
            // non-space)
            char expectedChar = expected.charAt(expectedIndex);
            if (expectedChar != ' ' && (expectedChar < '0' || expectedChar > '9')) {
                actualIndex--;
                expectedIndex--;
                continue;
            }

            if (actual.charAt(actualIndex) == expectedChar) {
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

    /*
     * If the given string has less than 5 digits, pad it on the left with
     * zeroes so that it has a length of 5. Also, replaces any spaces with
     * the character 0.
     */
    public static String padWithZeroes(String s) {
        s = s.replace(' ', '0');
        if (s.length() >= 5) {
            return s;
        }
        int numZeroes = 5 - s.length();
        String paddedString = "";
        for (int i = 0; i < numZeroes; i++) {
            paddedString += "0";
        }
        return paddedString + s;
    }
}
