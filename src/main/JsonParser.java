package main;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import javax.json.*;

/**
 * Contains methods to parse output.json files found within the directory's
 * sub-directories.
 *
 */
public class JsonParser {
    // The indices of the JSON array that we are examining. NOTE that the values
    // of these indexes are ONE LESS than the "value" field in the actual JSON
    // document for each entry, to account for zero-based indexing.
    private static final int[] JSON_ARRAY_INDEXES = { 3, // client ID
            5, // age
            7, // EDD
            8, // num_preg
            9, // live_births
            10, // regCCPF (bubble)
            11, // CCPFform (bubble)
            13, // monthpreg_ANC
            14, // ANC_v1
            16, // ANC_v3
            20, // TTV2
            28, // health_cond (bubble many)
            35, // date_delivery
            47, // V1_topics (bubble many)
            48 // V2_date
    };

    /**
     * Crawls entire "output" directory by going through each sub-directory.
     * Parses the data stored in each "output.json" file, and returns the data
     * as a Map from each Client ID to a List of Strings representing the values
     * of each field for that client ID's form. Note that if there are duplicate
     * client IDs, neither duplicate entry is included.
     * 
     * @param scanOutputRoot The root of the scan output directory; this folder
     *        should contain sub-folders for each form that was scanned.
     * @return A map containing the actual data outputted by Scan, mapping from
     *         each Client ID to a List of Strings representing the values of
     *         each field for that client ID's form. (Does NOT contain records
     *         for any client ID that was duplicated.)
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

        // We don't want to include any client IDs that appear multiple times,
        // so keep track of those
        Set<String> duplicateClientIds = new TreeSet<String>();
        Map<String, List<String>> actualData = new HashMap<String, List<String>>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {

            // Loop through all sub-directories
            for (Path entry : stream) {
                // Get the client ID of the current sub-directory (found in the
                // clientID.txt file)
                Scanner clientIdScanner = new Scanner(new File(entry.toString() + "\\clientID.txt"));
                String clientId = clientIdScanner.next();
                clientIdScanner.close();

                // Parse the actual results from the JSON, as well as the
                // correct results from the corresponding row in the Excel file
                // (with the matching ClientID)
                List<String> actualResult = JsonParser.parseActualJsonFile(entry.toString() + "\\output.json");

                // If there are multiple folders with the same Client ID, throw
                // all of those out
                if (actualData.containsKey(clientId)) {
                    actualData.remove(clientId);
                    duplicateClientIds.add(clientId);
                    continue;
                }
                if (duplicateClientIds.contains(clientId)) {
                    continue;
                }
                actualData.put(clientId, actualResult);
            }
        } catch (IOException x) {
            System.err.println(x);
        }
        return actualData;
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
            actualData.add(classificationValue);
        }
        return actualData;
    }
}
