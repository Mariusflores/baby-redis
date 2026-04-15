package org.example.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;

public class Server {
    private final SnapshotManager snapshotManager = new SnapshotManager(new File("snapshot.txt"));

    private final InMemoryStore store = new InMemoryStore(snapshotManager);
    private final DelayQueue<ExpiringKey> expireQueue = new DelayQueue<>();
    private final Map<String, Long> expireQueueState = new ConcurrentHashMap<>();


    public Server() throws IOException {
        // Retrieves instigates snapshot retrieval
        readSnapshot();
        // Daemon thread, tracks and removes items from expireQueue
        Runnable expireTrack = () -> {
            while (true) {
                try {
                    ExpiringKey key = (ExpiringKey) expireQueue.take();

                    store.purge(key.getKey());
                    expireQueueState.remove(key.getKey());

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Thread.ofVirtual().start(expireTrack);

        // Sync snapshot every 30 seconds
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

    private String delegate(String line) {
        String[] commands = parseOperation(line);
        if (commands.length < 2) {
            return "ERR expected at least <2> arguments";
        }
        // Input format i.e. SET hello world
        String operation = commands[0];
        String key = commands[1];

        switch (operation.toUpperCase()) {
            case "SET" -> {
                var values = Arrays.copyOfRange(commands, 2, commands.length);
                String value = String.join(" ", values).trim();
                if (value.isEmpty()) {
                    return "ERR Value isn't provided";
                }
                store.set(key, value);
                return "OK";
            }
            case "GET" -> {
                String result = store.get(key);
                return result == null ? "NOT FOUND" : result;
            }
            case "DELETE" -> {
                // Delete existing expire countdown
                if (expireQueueState.containsKey(key)){
                    expireQueueState.remove(key);
                    expireQueue.removeIf(entry -> entry.getKey().equals(key));

                }
                // Delete key from store
                store.delete(key);

                return "OK";
            }
            case "SADD" -> {
                var values = Arrays.copyOfRange(commands, 2, commands.length);
                store.sAdd(key, values);

                return "OK";
            }
            case "SREM" -> {
                var values = Arrays.copyOfRange(commands, 2, commands.length);

                store.sRem(key, values);
                return "OK";
            }
            case "SISMEMBER", "SIM" -> {
                String value = "";
                if (commands.length > 2) {
                    value = commands[2];
                }
                if (value.isEmpty()) {
                    return "ERR Value isn't provided";

                }
                return store.sIsMember(key, value) ? "TRUE" : "FALSE";

            }
            case "SMEMBERS", "SM" -> {

                var set = store.sMembers(key);

                return String.join(",", set);
            }
            case "TTL" -> {
                Long timestamp = expireQueueState.get(key);

                if(timestamp == null){
                    return "No expiry set";
                }

                long ttl = (timestamp - System.currentTimeMillis()) / 1000;
                if(ttl < 0){
                    return "Expired.";
                }
                return String.format("Time to Expiry: %d s", ttl);
            }
            case "EXPIRE", "EXP" -> {

                // Start simple parse the input, add to queue and write to file.
                // Error handling next iteration
                if (commands.length < 3) return "ERR missing arguments";

                long value = Long.parseLong(commands[2]);
                long expiryTimestamp = System.currentTimeMillis() + (value * 1000);


                ExpiringKey expiringKey = new ExpiringKey(key, expiryTimestamp);

                expireQueue.add(expiringKey);
                expireQueueState.put(key, expiryTimestamp);

                // Format file write line Current timestamp + given time


                return String.format("Expires in %d seconds", value);
            }

            default -> {
                return "ERR Unknown Command";
            }
        }
    }

    public void close() throws IOException {
        writeSnapshot();
    }

    private void writeSnapshot() {
        store.writeSnapshot(Map.copyOf(expireQueueState));
    }

    private String[] parseOperation(String line) {
        return line.trim().split(" ");
    }


    public static void main(String[] args) {
        try (
                // Create a ServerSocket listening on port 6379
                ServerSocket ss = new ServerSocket(6379);
                ExecutorService executor = Executors.newFixedThreadPool(10)
        ) {
            Server server = new Server();
            System.out.println("Starting main.java.org.example.server...");

            System.out.println("main.java.org.example.server.Server started listening on port 6379... ");

            // Shutdown hook to close main.java.org.example.server.Server file writer
            Thread closeServerHook = new Thread(() -> {
                try {
                    server.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            Runtime.getRuntime().addShutdownHook(closeServerHook);

            while (true) {


                // Accept a connection from a client
                Socket s = ss.accept();
                System.out.println("org.example.Client connected");

                Runnable task = handleClient(s, server);

                executor.submit(task);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Runnable handleClient(Socket s, Server server) {
        return () -> {
            try {
                // Declare a buffered reader
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));

                // Declare an Output Writer
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(s.getOutputStream()), true
                );


                String line;


                while ((line = reader.readLine()) != null) {
                    System.out.println("Command " + line);

                    if (line.trim().split(" ").length == 1 &&
                            line.trim().split(" ")[0].equalsIgnoreCase("QUIT")) {
                        // Close connections
                        break;
                    }

                    String result = server.delegate(line);

                    out.println(result);

                }

                reader.close();
                out.close();
                s.close();

            } catch (IOException e) {
                System.out.println("Error: " + e);
            }


        };
    }
}
