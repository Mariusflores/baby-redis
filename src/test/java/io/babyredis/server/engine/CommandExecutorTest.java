package io.babyredis.server.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.babyredis.server.store.InMemoryStore;

public class CommandExecutorTest {

    CommandExecutor executor;
    ExpiryManager expiryManager;
    StubAOFManager stubAOF;

    @BeforeEach
    void setup() {
        stubAOF = new StubAOFManager();
        InMemoryStore store = new InMemoryStore();
        expiryManager = new ExpiryManager(key -> {});
        executor = new CommandExecutor(stubAOF, store, expiryManager);
    }

    // --- PING ---

    @Test
    void pingReturnsPong() {
        String result = executor.execute("PING");
        assertEquals("+PONG\r\n", result);
    }

    // --- SET / GET ---

    @Test
    void setAndGetReturnsValue() {
        executor.execute("SET name alice");
        String result = executor.execute("GET name");
        assertEquals("$5\r\nalice\r\n", result);
    }

    @Test
    void setOverwritesPreviousValue() {
        executor.execute("SET name alice");
        executor.execute("SET name bob");
        String result = executor.execute("GET name");
        assertEquals("$3\r\nbob\r\n", result);
    }

    @Test
    void setWithMultiWordValue() {
        executor.execute("SET greeting hello world");
        String result = executor.execute("GET greeting");
        assertEquals("$11\r\nhello world\r\n", result);
    }

    @Test
    void getMissingKeyReturnsError() {
        String result = executor.execute("GET nonexistent");
        assertEquals("-ERR Not found\r\n", result);
    }

    @Test
    void setMissingValueReturnsError() {
        String result = executor.execute("SET key");
        assertEquals("-ERR Value isn't provided\r\n", result);
    }

    // --- DELETE ---

    @Test
    void deleteRemovesKey() {
        executor.execute("SET name alice");
        executor.execute("DELETE name");
        String result = executor.execute("GET name");
        assertEquals("-ERR Not found\r\n", result);
    }

    @Test
    void deleteAlsoRemovesExpiry() {
        executor.execute("SET name alice");
        executor.execute("EXPIRE name 1000");
        executor.execute("DELETE name");
        String result = executor.execute("TTL name");
        assertEquals(":-1\r\n", result);
    }

    // --- SADD / SMEMBERS / SREM / SISMEMBER ---

    @Test
    void saddThenSmembersReturnsMembers() {
        executor.execute("SADD fruits apple banana");
        String result = executor.execute("SMEMBERS fruits");
        // Array response — verify it contains both members
        assertTrue(result.contains("apple"));
        assertTrue(result.contains("banana"));
    }

    @Test
    void sremRemovesMembers() {
        executor.execute("SADD fruits apple banana kiwi");
        executor.execute("SREM fruits banana");
        String result = executor.execute("SMEMBERS fruits");
        assertTrue(result.contains("apple"));
        assertTrue(result.contains("kiwi"));
        assertTrue(!result.contains("banana") || result.contains("*2"));
    }

    @Test
    void sismemberReturnsOneForExistingMember() {
        executor.execute("SADD fruits apple");
        String result = executor.execute("SISMEMBER fruits apple");
        assertEquals(":1\r\n", result);
    }

    @Test
    void sismemberReturnsZeroForMissingMember() {
        executor.execute("SADD fruits apple");
        String result = executor.execute("SISMEMBER fruits banana");
        assertEquals(":0\r\n", result);
    }

    @Test
    void sismemberMissingValueReturnsError() {
        executor.execute("SADD fruits apple");
        String result = executor.execute("SISMEMBER fruits");
        assertEquals("-ERR Value isn't provided\r\n", result);
    }

    // --- EXPIRE / TTL / EXPIREAT ---

    @Test
    void expireSetsTTL() {
        executor.execute("SET name alice");
        executor.execute("EXPIRE name 500");
        String result = executor.execute("TTL name");
        String ttlStr = result.replace(":", "").replace("\r\n", "");
        int ttl = Integer.parseInt(ttlStr);
        assertTrue(ttl > 0 && ttl <= 500);
    }

    @Test
    void ttlOnKeyWithNoExpiryReturnsNegativeOne() {
        executor.execute("SET name alice");
        String result = executor.execute("TTL name");
        assertEquals(":-1\r\n", result);
    }

    @Test
    void expireatSetsAbsoluteExpiry() {
        executor.execute("SET name alice");
        long futureTimestamp = System.currentTimeMillis() + 60000;
        executor.execute(String.format("EXPIREAT name %d", futureTimestamp));
        String result = executor.execute("TTL name");
        String ttlStr = result.replace(":", "").replace("\r\n", "");
        int ttl = Integer.parseInt(ttlStr);
        assertTrue(ttl > 0 && ttl <= 60);
    }

    @Test
    void expireMissingArgsReturnsError() {
        String result = executor.execute("EXPIRE name");
        assertEquals("-ERR missing arguments\r\n", result);
    }

    // --- KEYS ---

    @Test
    void keysStarReturnsAllKeys() {
        executor.execute("SET a 1");
        executor.execute("SET b 2");
        executor.execute("SADD c 3");
        String result = executor.execute("KEYS *");
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
    }

    @Test
    void keysPrefixFiltersCorrectly() {
        executor.execute("SET fruit:apple red");
        executor.execute("SET fruit:banana yellow");
        executor.execute("SET veg:carrot orange");
        String result = executor.execute("KEYS fruit:*");
        assertTrue(result.contains("fruit:apple"));
        assertTrue(result.contains("fruit:banana"));
        assertTrue(!result.contains("veg:carrot"));
    }

    // --- FLUSHDB ---

    @Test
    void flushdbRemovesAllKeys() {
        executor.execute("SET a 1");
        executor.execute("SET b 2");
        executor.execute("FLUSHDB *");
        String result = executor.execute("KEYS *");
        // Empty array
        assertEquals("*0\r\n", result);
    }

    @Test
    void flushdbWithPatternRemovesOnlyMatching() {
        executor.execute("SET fruit:apple red");
        executor.execute("SET veg:carrot orange");
        executor.execute("FLUSHDB fruit:*");
        String resultFruit = executor.execute("GET fruit:apple");
        assertEquals("-ERR Not found\r\n", resultFruit);
        String resultVeg = executor.execute("GET veg:carrot");
        assertEquals("$6\r\norange\r\n", resultVeg);
    }

    @Test
    void flushdbAlsoRemovesExpiry() {
        executor.execute("SET name alice");
        executor.execute("EXPIRE name 1000");
        executor.execute("FLUSHDB *");
        String result = executor.execute("TTL name");
        assertEquals(":-1\r\n", result);
    }

    // --- Error handling ---

    @Test
    void unknownCommandReturnsError() {
        String result = executor.execute("FOOBAR key");
        assertEquals("-ERR Unknown Command\r\n", result);
    }

    @Test
    void tooFewArgumentsReturnsError() {
        String result = executor.execute("X");
        assertEquals("-ERR expected at least <2> arguments\r\n", result);
    }

    // --- AOF logging ---

    @Test
    void writeCommandsLogToAOF() {
        executor.execute("SET name alice");
        executor.execute("SADD fruits apple");
        executor.execute("DELETE name");
        assertEquals(3, stubAOF.getCurrentSequence());
    }

    @Test
    void readCommandsDoNotLogToAOF() {
        executor.execute("SET name alice");
        int seqAfterSet = stubAOF.getCurrentSequence();
        executor.execute("GET name");
        executor.execute("SMEMBERS fruits");
        executor.execute("TTL name");
        executor.execute("KEYS *");
        executor.execute("PING");
        assertEquals(seqAfterSet, stubAOF.getCurrentSequence());
    }

    @Test
    void replayModeDoesNotLogToAOF() {
        executor.execute("SET name alice", true);
        executor.execute("SADD fruits apple", true);
        executor.execute("DELETE name", true);
        assertEquals(0, stubAOF.getCurrentSequence());
    }

    @Test
    void expireLogsAsExpireatInAOF() {
        executor.execute("SET name alice");
        executor.execute("EXPIRE name 500");
        var commands = stubAOF.getLoggedCommands();
        assertTrue(commands.get(1).startsWith("EXPIREAT name"));
    }
}