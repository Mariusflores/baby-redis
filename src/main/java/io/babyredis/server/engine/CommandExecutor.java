package io.babyredis.server.engine;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;

import io.babyredis.protocol.RespEncoder;
import io.babyredis.server.persistence.AppendOnlyPersistence;
import io.babyredis.server.store.ExpiringKey;
import io.babyredis.server.store.InMemoryStore;

public class CommandExecutor {
    private final AppendOnlyPersistence aofManager;
    private final InMemoryStore store;
    private final ExpiryManager expiryManager;

    public CommandExecutor(
        AppendOnlyPersistence aofManager,
        InMemoryStore store,
        ExpiryManager expiryManager
    
    ) {
        this.aofManager = aofManager;
        this.store = store;
        this.expiryManager = expiryManager;

    }


        // Method for executing commands, takes a command line string as input and returns the response string. The method parses the command line, identifies the operation and its arguments, and performs the corresponding action on the in-memory store. It also handles logging of commands to the AOF file for persistence and returns appropriate responses based on the command execution results.
    public String execute(String line) {
        return execute(line, false);
    }

    // Overloaded execute method for AOF replay, takes an additional boolean parameter to indicate whether the command is being executed as part of AOF replay. This allows for different handling of commands during normal execution versus AOF replay, such as skipping AOF logging during replay to avoid duplicate entries in the AOF file.
    public String execute(String line, boolean isAOFReplay) {
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
                if (!isAOFReplay) {
                    aofManager.logCommand(line);
                }
                return RespEncoder.encodeString("OK");
            }
            case "GET" -> {
                String result = store.get(key);
                return result == null ? RespEncoder.encodeError("ERR Not found") : RespEncoder.encodeBulkString(result);
            }
            case "DELETE" -> {
                // Delete existing expire countdown
                    expiryManager.removeKey(key);
                
                // Delete key from store
                store.delete(key);
                if (!isAOFReplay) {
                    aofManager.logCommand(line);
                }
                return RespEncoder.encodeString("OK");
            }
            case "SADD" -> {
                var values = Arrays.copyOfRange(commands, 2, commands.length);
                store.sAdd(key, values);
                if (!isAOFReplay) {
                    aofManager.logCommand(line);
                }

                return RespEncoder.encodeString("OK");
            }
            case "SREM" -> {
                var values = Arrays.copyOfRange(commands, 2, commands.length);

                store.sRem(key, values);
                if (!isAOFReplay) {
                    aofManager.logCommand(line);
                }
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
                Long timestamp = expiryManager.getQueueState().get(key);

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
                if (commands.length < 3)
                    return RespEncoder.encodeError("ERR missing arguments");

                long value = Long.parseLong(commands[2]);
                long ttlMillis = System.currentTimeMillis() + (value * 1000);

                expiryManager.addKey(key, ttlMillis);

                // Format file write line Current timestamp + given time

                if (!isAOFReplay) {
                    aofManager.logCommand(
                        String.format("%s %s %s", "EXPIREAT", key, ttlMillis)
                    );
                }


                return RespEncoder.encodeInteger(1);
            }

            // Similar to EXPIRE but with absolute timestamp instead of relative time, used
            // for AOF Replay
            case "EXPIREAT" -> {
                if (commands.length < 3)
                    return RespEncoder.encodeError("ERR missing arguments");

                long expiryTimestamp = Long.parseLong(commands[2]);

                expiryManager.addKey(key, expiryTimestamp);

                return RespEncoder.encodeInteger(1);
            }

            case "KEYS" -> {
                if (commands.length > 2) {
                    // For simplicity, only support KEYS * for now
                    return RespEncoder.encodeError("ERR Unsupported pattern, only KEYS * or KEYS prefix* is supported");
                }
                String[] keys = store.getAllKeysMatchingPattern(commands.length == 2 ? commands[1] : "*");

                return RespEncoder.encodeArray(keys);
            }

            case "FLUSHDB" -> {

                String pattern = "*";

                if (commands.length == 2) {
                    pattern = commands[1];
                }

                // Clear store
                List<String> flushedKeys = store.flushMatchingPattern(pattern);
                // Clear expire queue and state

                for (String flushedKey : flushedKeys) {
                    expiryManager.removeKey(flushedKey);
                }

                if(!isAOFReplay) {
                    aofManager.logCommand(line);
                }
                return RespEncoder.encodeString("OK");
            }

            default -> {
                return RespEncoder.encodeError("ERR Unknown Command");
            }
        }
    }

    private String[] parseOperation(String line) {
        return line.trim().split(" ");
    }


}
