package main;

import java.util.*;

import org.apache.poi.xssf.usermodel.*;

/**
 * Parses the Excel file's alignment evaluations to compute an "alignment score"
 * for each form. Higher scores indicate worse alignment.
 * 
 * @author Joshua
 *
 */
public class AlignmentScore {

    private static final String[] MISALIGNMENT_COLUMNS = { "Q", "AF", "AU", "BF", "BR", "DA", "DM", "EG", "FB", "HK",
            "JJ", "CD", "CP", "GG", "JA" };
    private static final int CLIENT_ID_COLUMN = 15;

    public static void main(String[] args) {
        // Create ID -> alignment score map
        Map<String, Double> idToAlignmentScore = getAlignmentRatingFromExcel();

        // Create ID -> folder name map
        String relevantExamplesPath = "C:\\Users\\Joshua\\Downloads\\ScanPreAlignedImages\\relevant-training-examples\\";
        Map<String, String> idToFolderName = new HashMap<String, String>();
        FolderUtils.buildMap(relevantExamplesPath, idToFolderName, false);

        // Produce a folder name -> alignment score tree map (sorted)
        Map<String, Double> folderNameToAlignmentScore = new TreeMap<String, Double>();
        for (String clientId : idToFolderName.keySet()) {
            String folderName = idToFolderName.get(clientId);
            double alignmentScore = idToAlignmentScore.get(clientId);
            folderNameToAlignmentScore.put(folderName, alignmentScore);
        }

        // Print out results in sorted order
        for (String folderName : folderNameToAlignmentScore.keySet()) {
            System.out.println(folderName + ": " + folderNameToAlignmentScore.get(folderName));
        }
    }

    public static Map<String, Double> getAlignmentRatingFromExcel() {
        Map<String, Double> data = new HashMap<String, Double>();
        String file = "src/data/Master Excel_with column codes_a.xlsx";
        String[] sheets = { "#3" };
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
                    XSSFRow row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }

                    // Get the correct client ID from the table for that row.
                    // Note that any trailing zeroes are trimmed from the Client
                    // ID.
                    String clientId = AccuracyChecker
                            .trimTrailingZeroes(ExcelParser.getStringCellContent(row.getCell(CLIENT_ID_COLUMN)));

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

                    // Throw out ones in light pencil or user error
                    /*int notesIndex = toIndex("T");
                    XSSFCell cellT = row.getCell(notesIndex);
                    String valueT = getStringCellContent(cellT);
                    if (valueT != null) {
                        if ((valueT.contains("pencil") && valueT.contains("light")) || valueT.contains("error")) {
                            continue;
                        }
                    }*/

                    // For each "misalignment" column, add to the misalignment
                    // score based on the cell content.
                    int misalignmentScore = 0;
                    int count = 0; // number of columns with misalignment data
                    for (String colLetters : MISALIGNMENT_COLUMNS) {
                        int columnIndex = ExcelParser.toIndex(colLetters);
                        XSSFCell cell = row.getCell(columnIndex);
                        String value = ExcelParser.getStringCellContent(cell);
                        if (value != null && !value.isEmpty()) {
                            value = value.trim();
                            if (value.equals("small")) {
                                misalignmentScore += 2;
                                count++;
                            } else if (value.equals("medium")) {
                                misalignmentScore += 5;
                                count++;
                            } else if (value.equals("large")) {
                                misalignmentScore += 10;
                                count++;
                            } else if (value.equals("none")) {
                                misalignmentScore += 0;
                                count++;
                            } else if (value.equals("Misalignment")) {
                                // Do nothing
                            } else {
                                System.out.println("Non-standardized value! " + value);
                            }
                        }
                    }

                    // Add this row to the map (with the clientID as a key)
                    // XSSFCell actualExcelValue = row.getCell(toIndex("Z"));
                    // currentRowData.add(getStringCellContent(actualExcelValue));
                    data.put(clientId, misalignmentScore * 1.0 / count);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;

    }
}
