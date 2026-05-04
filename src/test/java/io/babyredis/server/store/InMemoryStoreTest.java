package io.babyredis.server.store;

import org.junit.jupiter.api.Test;

import io.babyredis.server.snapshot.SnapshotManager;

import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryStoreTest {

    private final File testFile = new File("test.txt");
    SnapshotManager testManager = new SnapshotManager(testFile);

    InMemoryStore testStore = new InMemoryStore(testManager);


    @Test
    void setUpdatesGetRetrieves() {
        String testAlert = "Test Alert";
        String testKey = "test";
        testStore.set(testKey, testAlert);

        assertEquals(testAlert, testStore.get(testKey));
    }

    @Test
    void getOnMissingKeyReturnsNull() {
        assertNull(testStore.get("non_existent"));
    }

    @Test
    void deleteRemovesKey() {
        String testAlert = "Test Alert";
        String testKey = "test";
        testStore.set(testKey, testAlert);

        testStore.delete(testKey);

        assertNull(testStore.get(testKey));
    }

    @Test
    void sAddThenSMembersReturnsValues() {

        String testKey = "Fruits";
        String[] values = {"Kiwi", "Apple", "Banana", "Pear"};
        testStore.sAdd(testKey, values);

        var results = testStore.sMembers(testKey);

        assertEquals(Set.of(values), results);

    }

    @Test
    void sRemRemovesKey() {
        String testKey = "Fruits";
        String[] values = {"Kiwi", "Apple", "Banana", "Pear"};
        testStore.sAdd(testKey, values);

        testStore.sRem(testKey, values);

        var results = testStore.sMembers(testKey);

        assertTrue(results.isEmpty());

    }

    @Test
    void sIsMemberReturnsTrueOrFalseCorrectly() {
        String testKey = "Fruits";
        String[] values = {"Kiwi", "Apple", "Banana"};
        testStore.sAdd(testKey, values);

        assertTrue(testStore.sIsMember(testKey, values[0]));

        assertFalse(testStore.sIsMember(testKey, "Cucumber"));
    }

    @Test
    void purgeDeletesFromBothStores() {
        String testKey = "greetings";
        String[] values = {"Hello", "World"};
        String value = "Hello world!";

        testStore.set(testKey, value);
        testStore.sAdd(testKey, values);

        testStore.purge(testKey);

        assertNull(testStore.get(testKey));
        assertTrue(testStore.sMembers(testKey).isEmpty());
    }

    @Test
    void getAllKeysMatchingPatternReturnsCorrectKeys() {
        testStore.set("Fruit:Apple", "Red");
        testStore.set("Fruit:Banana", "Yellow");
        testStore.set("Veg:Carrot", "Orange");
        testStore.sAdd("Fruit:Berry", "Blueberry");

        String[] allKeys = testStore.getAllKeysMatchingPattern("*");
        assertTrue(Set.of(allKeys).containsAll(Set.of("Fruit:Apple", "Fruit:Banana", "Veg:Carrot", "Fruit:Berry")));

        String[] fruitKeys = testStore.getAllKeysMatchingPattern("Fruit:*");
        assertTrue(Set.of(fruitKeys).containsAll(Set.of("Fruit:Apple", "Fruit:Banana", "Fruit:Berry")));
        assertFalse(Set.of(fruitKeys).contains("Veg:Carrot"));
    }

    @Test
    void flushMatchingPatternRemovesOnlyMatchingKeys() {
        testStore.set("Fruit:Apple", "Red");
        testStore.set("Fruit:Banana", "Yellow");
        testStore.set("Veg:Carrot", "Orange");
        testStore.sAdd("Fruit:Berry", "Blueberry");

        var flushed = testStore.flushMatchingPattern("Fruit:*");
        assertTrue(flushed.contains("Fruit:Apple"));
        assertTrue(flushed.contains("Fruit:Banana"));
        assertTrue(flushed.contains("Fruit:Berry"));
        assertFalse(flushed.contains("Veg:Carrot"));

        // Only Veg:Carrot should remain
        String[] remaining = testStore.getAllKeysMatchingPattern("*");
        assertArrayEquals(new String[]{"Veg:Carrot"}, remaining);
    }

    @Test
    void flushMatchingPatternAllRemovesAllKeys() {
        testStore.set("A", "1");
        testStore.set("B", "2");
        testStore.sAdd("C", "3");

        var flushed = testStore.flushMatchingPattern("*");
        assertTrue(flushed.contains("A"));
        assertTrue(flushed.contains("B"));
        assertTrue(flushed.contains("C"));

        String[] remaining = testStore.getAllKeysMatchingPattern("*");
        assertEquals(0, remaining.length);
    }

}
