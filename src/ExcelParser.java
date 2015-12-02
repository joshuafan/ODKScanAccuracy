import java.io.*;
import java.nio.file.*;
import java.util.Map;

import org.apache.poi.xssf.usermodel.*;

public class ExcelParser {

	// Crawl entire "output" directory, go through each subdirectory
	public static void crawlDirectories() {
		Path dir = Paths.get("C:\\Downloads\\...");
		DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
			public boolean accept(Path file) throws IOException {
				try {
					return (Files.isDirectory(file));
				} catch (IOException x) {
					// Failed to determine if it's a directory.
					System.err.println(x);
					return false;
				}
			}
		};

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {

			// This loops through all subdirectories
			for (Path entry : stream) {
				System.out.println(entry.getFileName());
				System.out.println(entry.toString());

				// Find output.json and client
				int clientId = getClientId();
				Map<String, String> estimatedResults = getEstimatedResults();
				// compare estimatedResults, actualResults.get(clientId)
			}
		} catch (IOException x) {
			System.err.println(x);
		}
	}

	public static void main(String[] args) {
		String correctFileName = "src/data/Master Excel_all data_w Access.xlsx";
		parseCorrectFile(correctFileName);
	}

	// Will return a Map(client ID -> Map(column title -> data))
	public static void parseCorrectFile(String file) {
		try {
			XSSFWorkbook wb = ExcelParser.readFile(file);
			XSSFSheet sheet = wb.getSheet("#1");
			int rows = sheet.getPhysicalNumberOfRows();
			System.out.println("Rows: " + rows);
			for (int r = 1; r < rows; r++) {
				XSSFRow row = sheet.getRow(r);
				if (row == null) {
					continue;
				}
				int cells = row.getPhysicalNumberOfCells();
				for (int c = 0; c < cells; c++) {
					XSSFCell cell = row.getCell(c);
					String value = null;

					switch (cell.getCellType()) {

					case XSSFCell.CELL_TYPE_FORMULA:
						value = "FORMULA value=" + cell.getCellFormula();
						break;

					case XSSFCell.CELL_TYPE_NUMERIC:
						value = "NUMERIC value=" + cell.getNumericCellValue();
						break;

					case XSSFCell.CELL_TYPE_STRING:
						value = "STRING value=" + cell.getStringCellValue();
						break;

					default:
					}
					System.out.println("CELL col=" + cell.getColumnIndex() + " VALUE=" + value);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static XSSFWorkbook readFile(String filename) throws IOException {
		FileInputStream fis = new FileInputStream(filename);
		try {
			return new XSSFWorkbook(fis);
		} finally {
			fis.close();
		}
	}
}
