package org.example.server;

import io.babyredis.server.SnapshotData;
import io.babyredis.server.SnapshotManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SnapshotManagerTest {

    private static final File testFile = new File("test.txt");

    SnapshotManager manager = new SnapshotManager(testFile);

    @AfterAll
    static void cleanUp() {
        if (testFile.exists()) {
            testFile.delete();

        }
    }

    @Test
    void RoundTripSnapshotTest() {

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

        manager.write(strings, sets, expiry);

        assertTrue(testFile.exists());

        SnapshotData snapshot = manager.read();

        assertEquals(snapshot.stringSnapshot(), strings);
        assertEquals(snapshot.setSnapshot(), sets);
        assertEquals(snapshot.expiryQueueSnapshot(), expiry);

    }
}
