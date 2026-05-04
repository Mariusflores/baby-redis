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

}
