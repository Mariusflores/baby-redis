package io.babyredis.server.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SnapshotManagerTest {

    private static final File testFile = new File("test_snapshot.txt");

    SnapshotManager manager = new SnapshotManager(testFile);

    @AfterEach
    void cleanUp() {
        if (testFile.exists()) {
            testFile.delete();
        }
    }

    @Test
    void roundTripSnapshotTest() {
        Map<String, String> strings = Map.of(
                "name", "alice",
                "greeting", "hello world",
                "url", "https://example.com"
        );

        Map<String, Set<String>> sets = Map.of(
                "fruits", Set.of("apple", "banana", "kiwi"),
                "languages", Set.of("Java", "TypeScript")
        );

        Map<String, Long> expiry = Map.of(
                "name", System.currentTimeMillis() + 60000L
        );

        manager.save(new SnapshotData(strings, sets, expiry), 0);

        assertTrue(testFile.exists());

        SnapshotData snapshot = manager.load();

        assertEquals(strings, snapshot.stringSnapshot());
        assertEquals(sets, snapshot.setSnapshot());
        assertEquals(expiry, snapshot.expiryQueueSnapshot());
    }

    @Test
    void loadFromNonExistentFileReturnsEmptyData() {
        SnapshotData snapshot = manager.load();

        assertTrue(snapshot.stringSnapshot().isEmpty());
        assertTrue(snapshot.setSnapshot().isEmpty());
        assertTrue(snapshot.expiryQueueSnapshot().isEmpty());
    }

    @Test
    void saveEmptySnapshotThenLoadReturnsEmpty() {
        manager.save(new SnapshotData(
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        ), 0);

        SnapshotData snapshot = manager.load();

        assertTrue(snapshot.stringSnapshot().isEmpty());
        assertTrue(snapshot.setSnapshot().isEmpty());
        assertTrue(snapshot.expiryQueueSnapshot().isEmpty());
    }

    @Test
    void saveOverwritesPreviousSnapshot() {
        manager.save(new SnapshotData(
                Map.of("old", "data"),
                new HashMap<>(),
                new HashMap<>()
        ), 1);

        manager.save(new SnapshotData(
                Map.of("new", "data"),
                new HashMap<>(),
                new HashMap<>()
        ), 2);

        SnapshotData snapshot = manager.load();

        assertNull(snapshot.stringSnapshot().get("old"));
        assertEquals("data", snapshot.stringSnapshot().get("new"));
    }

    @Test
    void valueContainingEqualsSignPreserved() {
        manager.save(new SnapshotData(
                Map.of("url", "https://example.com?a=1&b=2"),
                new HashMap<>(),
                new HashMap<>()
        ), 0);

        SnapshotData snapshot = manager.load();

        assertEquals("https://example.com?a=1&b=2", snapshot.stringSnapshot().get("url"));
    }

    @Test
    void sequenceNumberRoundTrips() {
        manager.save(new SnapshotData(
                Map.of("key", "value"),
                new HashMap<>(),
                new HashMap<>()
        ), 42);

        manager.load();

        assertEquals(42, manager.getLastSequence());
    }

    @Test
    void sequenceDefaultsToZeroWhenFileDoesNotExist() {
        manager.load();

        assertEquals(0, manager.getLastSequence());
    }
}