package io.babyredis.server;

import java.io.*;
import java.util.*;

/**
 * Manages the snapshotting of the Baby Redis server's in-memory data to a file and reading it back during server startup.
 * The SnapshotManager class provides methods to write the current state of the in-memory store (including string key-value pairs, sets, and expiring keys) to a snapshot file and to read the snapshot file to restore the in-memory state when the server starts.
 * The snapshot file is written in a simple text format, with sections for strings, sets, and expiring keys, allowing for easy serialization and deserialization of the server's state.
 */
public class SnapshotManager {

    private final File snapshotFile;
    private final static String STRING_SECTION = "STRING";
    private final static String SET_SECTION = "SET";
    private final static String EXPIRE_SECTION = "EXPIRE";

    /**
     * Constructs a new SnapshotManager with the specified snapshot file. The snapshot file is used to store the serialized state of the Baby Redis server's in-memory data, including string key-value pairs, sets, and expiring keys. The SnapshotManager provides methods to write the current state to the snapshot file and to read the snapshot file to restore the in-memory state during server startup.
     *
     * @param file the file to be used for storing the snapshot of the Baby Redis server's in-memory data
     */
    public SnapshotManager(File file) {
        snapshotFile = file;
    }

    /**
     * Writes the current state of the Baby Redis server's in-memory data to the snapshot file. This method takes three parameters: a map of string key-value pairs representing the string data, a map of sets representing the set data, and a map of expiring keys with their corresponding expiration timestamps. The method serializes this data into a simple text format, with sections for strings, sets, and expiring keys, and writes it to a temporary file. Once the writing is complete, the temporary file is renamed to the snapshot file, ensuring that the snapshot is saved atomically.
     *
     * @param stringSnapshot a map of string key-value pairs representing the string data to be snapshotted
     * @param setSnapshot    a map of sets representing the set data to be snapshotted
     * @param expiryQueue    a map of expiring keys with their corresponding expiration timestamps to be snapshotted
     */
    public void write(
            Map<String, String> stringSnapshot,
            Map<String, Set<String>> setSnapshot,
            Map<String, Long> expiryQueue) {
        File temp = new File("snapshot_temp.txt");
        try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(temp))) {

            fileWriter.write(STRING_SECTION + "\n");
            for (Map.Entry<String, String> entry : stringSnapshot.entrySet()) {
                fileWriter.write(String.format("%s=%s\n", entry.getKey(), entry.getValue()));
            }
            fileWriter.write(SET_SECTION + "\n");
            for (Map.Entry<String, Set<String>> entry : setSnapshot.entrySet()) {
                fileWriter.write(String.format("%s=[%s]\n", entry.getKey(), String.join(",", entry.getValue())));

            }
            fileWriter.write(EXPIRE_SECTION + "\n");
            for (Map.Entry<String, Long> entry : expiryQueue.entrySet()) {
                fileWriter.write(String.format("%s=%d\n", entry.getKey(), entry.getValue()));
            }

            fileWriter.flush();


            if (snapshotFile.exists()) {
                snapshotFile.delete();
            }
            temp.renameTo(snapshotFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Reads the snapshot file and restores the state of the Baby Redis server's in-memory data. This method reads the snapshot file, which is expected to be in a specific text format with sections for strings, sets, and expiring keys. It parses the file line by line, identifying the current section (STRING, SET, or EXPIRE) and populating the corresponding data structures (maps for strings and sets, and a map for expiring keys) based on the content of each line. Once the entire file has been read and parsed, it returns a SnapshotData record containing the restored state of the in-memory data, which can then be used to initialize the server's state during startup.
     *
     * @return a SnapshotData record containing the restored state of the in-memory data, including string key-value pairs, sets, and expiring keys, read from the snapshot file
     */
    public SnapshotData read() {

        Map<String, String> stringSnapshot = new HashMap<>();
        Map<String, Set<String>> setSnapshot = new HashMap<>();
        Map<String, Long> expiryQueueSnapshot = new HashMap<>();

        // If the snapshot file does not exist, return an empty SnapshotData record with empty maps for strings, sets, and expiring keys.
        if (!snapshotFile.exists()) {
            return new SnapshotData(new HashMap<>(), new HashMap<>(), new HashMap<>());
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(snapshotFile))) {

            String line;
            String section = "";

            while ((line = reader.readLine()) != null) {
                // Identify the current section based on the line content. If the line matches "STRING", "SET", or "EXPIRE", update the section variable accordingly and continue to the next iteration to read the data lines for that section.
                if (line.equalsIgnoreCase(STRING_SECTION) ||
                        line.equalsIgnoreCase(SET_SECTION) ||
                        line.equalsIgnoreCase(EXPIRE_SECTION)) {
                    section = line;
                    // Continue to the next iteration to avoid processing the section header line as data.  
                    continue;
                }

                // Based on the current section, parse the line to extract the relevant data and populate the corresponding data structures (stringSnapshot for STRING section, setSnapshot for SET section, and expiryQueueSnapshot for EXPIRE section). The line is expected to be in a specific format (e.g., "key=value" for strings, "key=[val1,val2]" for sets, and "key=timestamp" for expiring keys), and the method uses string manipulation to extract the key and value(s) accordingly.
                switch (section.toUpperCase()) {
                    case "STRING" -> {
                        String[] parts = split(line);
                        stringSnapshot.put(parts[0], parts[1]);

                    }
                    case "SET" -> {
                        String[] parts = split(line);
                        String key = parts[0];
                        String[] values = parts[1].substring(1, parts[1].length() - 1).split(",");
                        Set<String> valueSet = (values.length == 1 && values[0].isEmpty()) ? new HashSet<>() : new HashSet<>(Arrays.asList(values));

                        setSnapshot.put(key, valueSet);
                    }
                    case "EXPIRE" -> {
                        String[] parts = split(line);
                        expiryQueueSnapshot.put(parts[0], Long.parseLong(parts[1]));
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new SnapshotData(stringSnapshot, setSnapshot, expiryQueueSnapshot);
    }

    // Helper method to split a line into key and value parts based on the first occurrence of the "=" character. This method is used to parse lines in the snapshot file that are expected to be in the format "key=value". It trims the line and splits it into two parts: the key (everything before the "=") and the value (everything after the "="). The method returns an array containing the key and value as separate elements.
    private String[] split(String line) {
        return line.trim().split("=", 2);
    }
}
