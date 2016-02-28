package main;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class FolderUtils {

    public static void createSubset(Set<String> clientIds) {
        List<String> paths = new ArrayList<String>();
        paths.add("C:\\Users\\Joshua\\Downloads\\ScanPreAlignedImages\\August Scan Output");
        paths.add("C:\\Users\\Joshua\\Downloads\\ScanPreAlignedImages\\August Scan Output\\Additional files");

        Map<String, String> clientIdToFolderName = clientIdsToFolderName(paths);
        String destination = "C:\\Users\\Joshua\\Downloads\\ScanPreAlignedImages\\relevant-training-examples\\";
        for (String id : clientIds) {
            if (!clientIdToFolderName.containsKey(id)) {
                System.out.println("Client id " + id + " not found!");
                continue;
            }
            copyDirectory(clientIdToFolderName.get(id), destination);
        }
    }

    /**
     * Given a list of folders ("paths"), returns a map mapping client IDs
     * (emdedded at the end of the folder name) to the name of that folder.
     * 
     * @param paths
     * @return
     */
    public static Map<String, String> clientIdsToFolderName(List<String> paths) {
        Map<String, String> idToFolder = new HashMap<String, String>();
        for (String path : paths) {
            buildMap(path, idToFolder, false);
        }
        return idToFolder;
    }

    public static Set<String> getClientIds(String path) {
        Map<String, String> map = new HashMap<String, String>();
        buildMap(path, map, true);
        System.out.println();
        return map.keySet();
    }

    // Adds <client id>, <folder name> pairs to the map, based off of the
    // folders inside "path". Assumes that folders inside "path" are of the
    // format xxxxxxx_id_yyy, where yyy is the client id.
    public static void buildMap(String path, Map<String, String> idToFolder, boolean fullPath) {
        // Filter all items inside "path" to select the
        // sub-directories
        Path dir = Paths.get(path);
        DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
            public boolean accept(Path file) throws IOException {
                return (Files.isDirectory(file));
            }
        };

        // We don't want to include any client IDs that appear multiple times,
        // so keep track of those
        Set<String> duplicateClientIds = new TreeSet<String>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {

            // Loop through all sub-directories
            for (Path entry : stream) {

                String pathToFolder = entry.toString();
                String folderName = getFolderName(pathToFolder);
                String[] underScoreSplit = folderName.split("_");
                if (underScoreSplit.length < 2 || !underScoreSplit[underScoreSplit.length - 2].equals("id")) {
                    continue;
                }
                String clientId = underScoreSplit[underScoreSplit.length - 1];
                if (idToFolder.containsKey(clientId)) {
                    System.out.println("Duplicate client id " + clientId);
                    idToFolder.remove(clientId);
                    duplicateClientIds.add(clientId);
                }
                if (duplicateClientIds.contains(clientId)) {
                    continue;
                }
                if (fullPath) {
                    idToFolder.put(clientId, pathToFolder);
                } else {
                    idToFolder.put(clientId, folderName);
                }
            }
        } catch (IOException x) {
            System.err.println(x);
        }
    }

    public static void copyDirectory(String path, String destinationString) {
        Path source = Paths.get(path);
        Path target = Paths.get(destinationString + getFolderName(path));
        try {
            Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                                throws IOException {
                            Path targetdir = target.resolve(source.relativize(dir));
                            try {
                                Files.copy(dir, targetdir);
                            } catch (FileAlreadyExistsException e) {
                                if (!Files.isDirectory(targetdir))
                                    throw e;
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                            Files.copy(file, target.resolve(source.relativize(file)));

                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Returns the name of the actual name of the folder/file (without the path)
    private static String getFolderName(String pathToFolder) {
        String[] pathArray = pathToFolder.split("\\\\");
        String folderName = pathArray[pathArray.length - 1];
        return folderName;
    }
}
