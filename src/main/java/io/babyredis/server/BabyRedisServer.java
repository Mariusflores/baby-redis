package io.babyredis.server;

import io.babyredis.protocol.RespEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;

public class BabyRedisServer {
    private final SnapshotManager snapshotManager = new SnapshotManager(new File("snapshot.txt"));
    private static final Logger log = LoggerFactory.getLogger(BabyRedisServer.class);
    private final Set<Socket> activeConnections;


    private final InMemoryStore store = new InMemoryStore(snapshotManager);
    private final DelayQueue<ExpiringKey> expireQueue = new DelayQueue<>();
    private final Map<String, Long> expireQueueState = new ConcurrentHashMap<>();


    public BabyRedisServer() throws IOException {
        // Retrieves instigates snapshot retrieval
        readSnapshot();
        activeConnections = ConcurrentHashMap.newKeySet();
        // Daemon thread, tracks and removes items from expireQueue
        Runnable expireTrack = () -> {
            while (true) {
                try {
                    ExpiringKey key = expireQueue.take();

                    store.purge(key.getKey());
                    expireQueueState.remove(key.getKey());

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
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

                    writeSnapshot();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        };

        Thread.ofVirtual().start(syncSnapshot);
    }

    private void readSnapshot() {

        SnapshotData snapshot = store.readSnapshot();

        expireQueueState.putAll(snapshot.expiryQueueSnapshot());

        loadExpireQueue();
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
        writeSnapshot();
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

    private void writeSnapshot() {
        store.writeSnapshot(Map.copyOf(expireQueueState));
    }

    private String[] parseOperation(String line) {
        return line.trim().split(" ");
    }


}
