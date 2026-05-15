package io.babyredis.server.engine;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import io.babyredis.server.store.InMemoryStore;

public class ExpiryManagerTest {

    @Test
    void addKeyAddsToQueue() {

        String testKey = "testKey";
        long expireAt = 1000;

        ExpiryManager expiryManager = new ExpiryManager(key -> {});

        expiryManager.addKey(testKey, expireAt);

        Map<String, Long> result = expiryManager.getQueueState();

        assertTrue(result.containsKey(testKey));
        assertTrue(result.containsValue(expireAt));

    }

    @Test
    void removeKeyRemovesFromQueue() {

        String testKey = "testKey";
        long expireAt = 1000;

        ExpiryManager expiryManager = new ExpiryManager(key -> {});

        expiryManager.addKey(testKey, expireAt);

        Map<String, Long> result = expiryManager.getQueueState();

        assertTrue(result.containsKey(testKey));

        expiryManager.removeKey(testKey);
        result = expiryManager.getQueueState();

        assertFalse(result.containsKey(testKey));

    }

    @Test
    void RemoveKeyOnNonExistentDoesNotThrow() {

        ExpiryManager expiryManager = new ExpiryManager(key -> {});

        assertDoesNotThrow(() -> {
            expiryManager.removeKey("Nonexistent");

        });
    }

    @Test
    void getQueueStateReturnsDefensiveCopy() {

        ExpiryManager expiryManager = new ExpiryManager(key -> {});

        String insertKey = "Test";
        long ttl = 1000;

        Map<String, Long> result = expiryManager.getQueueState();

        assertThrows(UnsupportedOperationException.class, () -> result.put(insertKey, ttl));

    }

    @Test
    void setQueueStateAndLoadPopulatesCorrectly() {
        Map<String, Long> inputMap = new HashMap<>();
        inputMap.put("Test1", 1000l);
        inputMap.put("Test2", 1500l);
        inputMap.put("Test3", 900l);

        ExpiryManager expiryManager = new ExpiryManager(key -> {});

        expiryManager.setQueueState(Map.copyOf(inputMap));

        expiryManager.loadQueue();

        Map<String, Long> result = expiryManager.getQueueState();

        assertEquals(inputMap.keySet(), result.keySet());

    }

    @Test
    void daemonThreadPurgesExpiredKeys() {

        try {
            String key = "testKey";
            String value = "Test Value";
            long expireAt = System.currentTimeMillis() + 100;

            InMemoryStore store = new InMemoryStore();
            Consumer<String> purgeCallback = store::purge;
            ExpiryManager expiryManager = new ExpiryManager(purgeCallback);

            store.set(key, value);

            expiryManager.addKey(key, expireAt);

            //Ensure sleeps enough for purge to take place
            Thread.sleep(300);

            assertNull(store.get(key));
        } catch (InterruptedException e) {
            System.out.println("Error");
        }

    }

}
