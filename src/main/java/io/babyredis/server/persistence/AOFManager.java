package io.babyredis.server.persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class AOFManager implements AppendOnlyPersistence {

    private final File aofFile;
    private final BufferedWriter writer;
    int currentSequence = 0;

    public AOFManager(File file) {
        this.aofFile = file;
        initializeSequence();
        try {
            this.writer = new BufferedWriter(new FileWriter(aofFile, true));
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize AOF file", e);
        }
    }

    @Override
    public void logCommand(String command) {
        // Implementation to log the command to the AOF file
        try {
            currentSequence++;

            writer.write(String.format("%d:%s\n", currentSequence, command)); // Write the command with the current
                                                                              // sequence number
            writer.flush();

        } catch (IOException e) {
            throw new RuntimeException("Failed to log command to AOF file", e);
        }

    }

    @Override
    public List<String> loadAfter(int lastSequence) {
        List<String> commands = new java.util.ArrayList<>();
        if (!aofFile.exists()) {
            currentSequence = 0;
            return commands; // Return empty list if AOF file does not exist
        }
        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(aofFile))) {
            // Read the AOF file and load commands with sequence numbers greater than
            // lastSequence

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length != 2) {
                    continue; // Skip malformed lines
                }
                int sequence = Integer.parseInt(parts[0]);
                String command = parts[1];
                if (sequence > lastSequence) {
                    commands.add(command);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load commands from AOF file", e);
        }
        // Implementation to load commands from the AOF file after the given sequence
        // number
        return commands;
    }

    @Override
    public int getCurrentSequence() {
        // Implementation to return the current sequence number of the AOF log
        return currentSequence;
    }

    private void initializeSequence() {
        // Read the AOF file to determine the last sequence number and set
        // currentSequence accordingly

        if (!aofFile.exists()) {
            currentSequence = 0;
            return;
        }
        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(aofFile))) {

            String line;
            int maxSequence = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length != 2) {
                    continue; // Skip malformed lines
                }
                int sequence = Integer.parseInt(parts[0]);
                if (sequence > maxSequence) {
                    maxSequence = sequence;
                }
            }
            currentSequence = maxSequence;
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize AOF sequence number", e);
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }



}
