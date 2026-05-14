package io.babyredis.server.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AOFManagerTest {

    private static final File testFile = new File("test_aof.txt");
    private AOFManager aof;
    private AOFManager aof2;

    @AfterEach
    void cleanUp() throws Exception {
        if (aof != null) {
            aof.close();
        }
        
        if (aof2 != null) {
            aof2.close();
        }
    
        if (testFile.exists()) {
            testFile.delete();
        }
}

    @Test
    void logCommandAppendsToFile() {
        aof = new AOFManager(testFile);

        aof.logCommand("SET name alice");

        List<String> commands = aof.loadAfter(0);
        assertEquals(1, commands.size());
        assertEquals("SET name alice", commands.get(0));

    }

    @Test
    void sequenceIncrementsOnEachCommand() {
    aof = new AOFManager(testFile);

        aof.logCommand("SET a 1");
        aof.logCommand("SET b 2");
        aof.logCommand("SET c 3");

        assertEquals(3, aof.getCurrentSequence());
    }

    @Test
    void loadAfterFiltersCorrectly() {
        aof = new AOFManager(testFile);

        aof.logCommand("SET a 1");
        aof.logCommand("SET b 2");
        aof.logCommand("SET c 3");
        aof.logCommand("DELETE a");

        List<String> commands = aof.loadAfter(2);

        assertEquals(2, commands.size());
        assertEquals("SET c 3", commands.get(0));
        assertEquals("DELETE a", commands.get(1));
    }

    @Test
    void loadAfterZeroReturnsAllCommands() {
        aof = new AOFManager(testFile);

        aof.logCommand("SET x 1");
        aof.logCommand("SADD fruits apple");

        List<String> commands = aof.loadAfter(0);

        assertEquals(2, commands.size());
    }

    @Test
    void loadAfterCurrentSequenceReturnsEmpty() {
        aof = new AOFManager(testFile);

        aof.logCommand("SET a 1");
        aof.logCommand("SET b 2");

        List<String> commands = aof.loadAfter(2);

        assertTrue(commands.isEmpty());
    }

    @Test
    void initializeSequenceFromExistingFile() {
        aof = new AOFManager(testFile);

        aof.logCommand("SET a 1");
        aof.logCommand("SET b 2");
        aof.logCommand("SET c 3");

        // Simulate restart — new instance reads existing file
        aof2 = new AOFManager(testFile);

        assertEquals(3, aof2.getCurrentSequence());
    }

    @Test
    void newCommandsAfterRestartContinueSequence() {
        aof = new AOFManager(testFile);

        aof.logCommand("SET a 1");
        aof.logCommand("SET b 2");

        // Simulate restart
        aof2 = new AOFManager(testFile);
        aof2.logCommand("SET c 3");

        assertEquals(3, aof2.getCurrentSequence());

        List<String> commands = aof2.loadAfter(2);
        assertEquals(1, commands.size());
        assertEquals("SET c 3", commands.get(0));
    }

    @Test
    void emptyFileReturnsEmptyListAndZeroSequence() {
        aof = new AOFManager(testFile);

        assertEquals(0, aof.getCurrentSequence());

        List<String> commands = aof.loadAfter(0);
        assertTrue(commands.isEmpty());
    }

    @Test
    void commandWithColonInValuePreserved() {
        // Commands containing ':' must survive the split(":", 2) parsing
        aof = new AOFManager(testFile);

        aof.logCommand("SET url https://example.com:8080/path");

        List<String> commands = aof.loadAfter(0);
        assertEquals("SET url https://example.com:8080/path", commands.get(0));
    }
}