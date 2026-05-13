package io.babyredis.server;

import io.babyredis.error.BabyRedisException;
import io.babyredis.protocol.RespEncoder;
import io.babyredis.server.persistence.SnapshotData;
import io.babyredis.server.persistence.SnapshotPersistence;
import io.babyredis.server.store.ExpiringKey;
import io.babyredis.server.store.InMemoryStore;
import io.babyredis.server.store.StoreState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;

public class BabyRedisServer {
    private final SnapshotPersistence snapshotManager;
    private static final Logger log = LoggerFactory.getLogger(BabyRedisServer.class);
    private final Set<Socket> activeConnections;


    private final InMemoryStore store = new InMemoryStore();
    private final DelayQueue<ExpiringKey> expireQueue = new DelayQueue<>();
    private final Map<String, Long> expireQueueState = new ConcurrentHashMap<>();


    public BabyRedisServer(
        SnapshotPersistence snapshotManager

    ) throws IOException {
        // Retrieves instigates snapshot retrieval
        this.snapshotManager = snapshotManager;
        loadSnapshot();
        activeConnections = ConcurrentHashMap.newKeySet();
        // Daemon thread, tracks and removes items from expireQueue
        Runnable expireTrack = () -> {
            while (true) {
                try {
                    ExpiringKey key = expireQueue.take();

                    store.purge(key.getKey());
                    expireQueueState.remove(key.getKey());

                } catch (InterruptedException e) {
                    throw new BabyRedisException(
                        "Error occurred while tracking expired keys", 
                        e);
                }
            }
        };
        Thread.ofVirtual().start(expireTrack);

        // Daemon thread, Sync snapshot every 30 seconds
        // TODO confirm snapshot throws on error
        Runnable syncSnapshot = () -> {
            while (true) {
                try {
                    Thread.sleep(30000);

                    saveSnapshot();
                } catch (InterruptedException e) {
                    throw new BabyRedisException(
                        "Error occurred while syncing snapshot", 
                        e
                    );
                }
            }

        };

        Thread.ofVirtual().start(syncSnapshot);
    }

    private void loadExpireQueue() {
        expireQueueState.forEach((k, v) ->
                expireQueue.add(new ExpiringKey(k, v))
        );
    }

    public String execute(String line) {
        String[] commands = parseOperation(line);

        if (commands.length == 1 && commands[0].equalsIgnoreCase("PING")) {

            return RespEncoder.encodeString("PONG");
        }

        if (commands.length < 2) {
            return RespEncoder.encodeError("ERR expected at least <2> arguments");
        }
        // Input format i.e. SET hello world
        String operation = commands[0];
        String key = commands[1];

        switch (operation.toUpperCase()) {
            case "SET" -> {
                var values = Arrays.copyOfRange(commands, 2, commands.length);
                String value = String.join(" ", values).trim();
                if (value.isEmpty()) {
                    return RespEncoder.encodeError("ERR Value isn't provided");
                }
                store.set(key, value);
                return RespEncoder.encodeString("OK");
            }
            case "GET" -> {
                String result = store.get(key);
                return result == null ? RespEncoder.encodeError("ERR Not found") : RespEncoder.encodeBulkString(result);
            }
            case "DELETE" -> {
                // Delete existing expire countdown
                if (expireQueueState.containsKey(key)) {
                    expireQueueState.remove(key);
                    expireQueue.removeIf(entry -> entry.getKey().equals(key));

                }
                // Delete key from store
                store.delete(key);

                return RespEncoder.encodeString("OK");
            }
            case "SADD" -> {
                var values = Arrays.copyOfRange(commands, 2, commands.length);
                store.sAdd(key, values);

                return RespEncoder.encodeString("OK");
            }
            case "SREM" -> {
                var values = Arrays.copyOfRange(commands, 2, commands.length);

                store.sRem(key, values);
                return RespEncoder.encodeString("OK");
            }
            case "SISMEMBER", "SIM" -> {
                String value = "";
                if (commands.length > 2) {
                    value = commands[2];
                }
                if (value.isEmpty()) {
                    return RespEncoder.encodeError("ERR Value isn't provided");

                }
                return store.sIsMember(key, value) ? RespEncoder.encodeInteger(1) : RespEncoder.encodeInteger(0);

            }
            case "SMEMBERS", "SM" -> {

                var set = store.sMembers(key);

                return RespEncoder.encodeArray(set.toArray(new String[0]));
            }
            case "TTL" -> {
                Long timestamp = expireQueueState.get(key);

                if (timestamp == null) {
                    // return -1 when expiry not set
                    return RespEncoder.encodeInteger(-1);
                }

                long ttl = (timestamp - System.currentTimeMillis()) / 1000;
                if (ttl < 0) {
                    // Return -2 when expired
                    return RespEncoder.encodeInteger(-2);
                }
                return RespEncoder.encodeInteger((int) ttl);
            }
            case "EXPIRE", "EXP" -> {

                // Start simple parse the input, add to queue and write to file.
                // Error handling next iteration
                if (commands.length < 3) return RespEncoder.encodeError("ERR missing arguments");

                long value = Long.parseLong(commands[2]);
                long expiryTimestamp = System.currentTimeMillis() + (value * 1000);


                ExpiringKey expiringKey = new ExpiringKey(key, expiryTimestamp);

                expireQueue.add(expiringKey);
                expireQueueState.put(key, expiryTimestamp);

                // Format file write line Current timestamp + given time


                return RespEncoder.encodeInteger(1);
            }

            case "KEYS" -> {
                if(commands.length > 2){
                    // For simplicity, only support KEYS * for now
                    return RespEncoder.encodeError("ERR Unsupported pattern, only KEYS * or KEYS prefix* is supported");
                }
                String[] keys = store.getAllKeysMatchingPattern( commands.length == 2 ? commands[1] : "*");

                return RespEncoder.encodeArray(keys);
            }

            case "FLUSHDB" -> {

                String pattern = "*";

                if(commands.length == 2){
                    pattern = commands[1];
                }

                // Clear store
                List <String > flushedKeys = store.flushMatchingPattern(pattern);
                // Clear expire queue and state

                for(String flushedKey : flushedKeys){
                    expireQueue.removeIf(k -> k.getKey().equals(flushedKey));
                    expireQueueState.remove(flushedKey);
                }

                return RespEncoder.encodeString("OK");
            }

            default -> {
                return RespEncoder.encodeError("ERR Unknown Command");
            }
        }
    }

    public void addSocket(Socket s) {
        activeConnections.add(s);
    }

    public void closeSocket(Socket s) throws IOException {
        if (activeConnections.contains(s)) {
            activeConnections.remove(s);
            s.close();
        }
    }

    public void close() throws IOException {
        saveSnapshot();
        closeAllConnections();
    }

    private void closeAllConnections() {
        for (Socket s : Set.copyOf(activeConnections)) {
            try {
                closeSocket(s);
            } catch (IOException e) {
                log.error("Error closing socket", e);
            }
        }
    }

    private void loadSnapshot() {

        SnapshotData snapshot = snapshotManager.load();
        store.loadState(new StoreState(snapshot.stringSnapshot(), snapshot.setSnapshot()));

        expireQueueState.putAll(snapshot.expiryQueueSnapshot());

        loadExpireQueue();
    }

    private void saveSnapshot() {
        StoreState state = store.exportState();
         snapshotManager.save(new SnapshotData(state.stringStore(), state.setStore(), Map.copyOf(expireQueueState)));
    }

    private String[] parseOperation(String line) {
        return line.trim().split(" ");
    }

}
