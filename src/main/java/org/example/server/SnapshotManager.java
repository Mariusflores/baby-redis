package org.example.server;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SnapshotManager {

    private final File snapshotFile;

    public SnapshotManager(File file) {
        snapshotFile = file;
    }


    public void write(
            Map<String, String> stringSnapshot,
            Map<String, Set<String>> setSnapshot,
            Map<String, Long> expiryQueue) {
        BufferedWriter fileWriter;
        try {
            File temp = new File("snapshot_temp.txt");

            fileWriter = new BufferedWriter(new FileWriter(temp));
            fileWriter.write("STRING" + "\n");
            for (Map.Entry<String, String> entry : stringSnapshot.entrySet()) {
                fileWriter.write(String.format("%s=%s\n", entry.getKey(), entry.getValue()));
            }
            fileWriter.write("SET\n");
            for (Map.Entry<String, Set<String>> entry : setSnapshot.entrySet()) {
                fileWriter.write(String.format("%s=[%s]\n", entry.getKey(), String.join(",", entry.getValue())));

            }
            fileWriter.write("EXPIRE\n");
            for (Map.Entry<String, Long> entry : expiryQueue.entrySet()) {
                fileWriter.write(String.format("%s=%d\n", entry.getKey(), entry.getValue()));
            }

            fileWriter.flush();
            fileWriter.close();


            if (snapshotFile.exists()) {
                snapshotFile.delete();
            }
            temp.renameTo(snapshotFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public SnapshotData read() {
        BufferedReader reader;
        Map<String, String> stringSnapshot = new HashMap<>();
        Map<String, Set<String>> setSnapshot = new HashMap<>();
        Map<String, Long> expiryQueueSnapshot = new HashMap<>();

        if (!snapshotFile.exists()) {
            return new SnapshotData(new HashMap<>(), new HashMap<>(), new HashMap<>());
        }
        try {
            reader = new BufferedReader(new FileReader(snapshotFile));
            String line;
            String section = "";

            while ((line = reader.readLine()) != null) {
                if (line.equalsIgnoreCase("STRING") ||
                        line.equalsIgnoreCase("SET") ||
                        line.equalsIgnoreCase("EXPIRE")) {
                    section = line;
                    continue;
                }

                switch (section.toUpperCase()) {
                    case "STRING" -> {
                        String[] parts = split(line);
                        stringSnapshot.put(parts[0], parts[1]);

                    }
                    case "SET" -> {
                        String[] parts = split(line);
                        String key = parts[0];
                        String[] values = parts[1].substring(1, parts[1].length() - 1).split(",");

                        setSnapshot.put(key, Set.of(values));
                    }
                    case "EXPIRE" -> {
                        String[] parts = split(line);
                        expiryQueueSnapshot.put(parts[0], Long.parseLong(parts[1]));
                    }
                }
            }
            reader.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new SnapshotData(stringSnapshot, setSnapshot, expiryQueueSnapshot);
    }

    private String[] split(String line) {
        return line.trim().split("=", 2);
    }
}
