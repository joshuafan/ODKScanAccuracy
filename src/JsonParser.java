import java.io.*;
import java.util.*;

import javax.json.*;

/**
 * Contains methods to crawl a directory and examine/parse the output.json files
 * found within the directory's sub-directories.
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
			System.out.println(file);
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
			System.out.println("JSON Index: " + (JSON_ARRAY_INDEXES[i]));
			System.out.println("Current object: " + currentObject);
			String classificationValue = currentObject.getString("value");
			System.out.println("value: " + classificationValue);
			actualData.add(classificationValue);
		}
		actualData.add(array.getJsonObject(3).getString("value"));
		return actualData;
	}
}
