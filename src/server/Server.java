package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final InMemoryStore store = new InMemoryStore();
    private final DelayQueue<Delayed> expireQueue = new DelayQueue<>();
    private final BufferedWriter fileWriter = new BufferedWriter(new FileWriter("queries.txt", true));

    public Server() throws IOException {
        replay();


        Runnable task = () -> {
            while (true) {
                try {
                    ExpiringKey key = (ExpiringKey) expireQueue.take();

                    store.purge(key.getKey());

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        Thread.ofVirtual().start(task);
    }

    private void replay() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("queries.txt"));
        String line;
        List<String> expiredKeys = new ArrayList<>();

        while ((line = reader.readLine()) != null) {
            String[] params = parseOperation(line);
            String operation = params[0];
            String key = params[1];

            switch (operation.toUpperCase()) {
                case "SET" -> {
                    var values = Arrays.copyOfRange(params, 2, params.length);
                    String value = String.join(" ", values).trim();

                    store.set(key, value);
                }

                case "DELETE" -> store.delete(key);
                case "SADD" -> {
                    var values = Arrays.copyOfRange(params, 2, params.length);
                    store.sAdd(key, values);
                }
                case "SREM" -> {
                    var values = Arrays.copyOfRange(params, 2, params.length);
                    store.sRem(key, values);
                }
                case "EXPIRE", "EXP" -> {
                    long expiryTimestamp = Long.parseLong(params[2]);

                    if (expiryTimestamp < System.currentTimeMillis()) {
                        expiredKeys.add(key);
                    } else {
                        ExpiringKey expiringKey = new ExpiringKey(key, expiryTimestamp);
                        expireQueue.add(expiringKey);
                    }
                }
            }
        }
        for (String expired : expiredKeys) {
            store.purge(expired);
        }
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
                writeToFile(line);
                return "OK";
            }
            case "GET" -> {
                String result = store.get(key);
                return result == null ? "NOT FOUND" : result;
            }
            case "DELETE" -> {
                store.delete(key);
                writeToFile(line);

                return "OK";
            }
            case "SADD" -> {
                var values = Arrays.copyOfRange(commands, 2, commands.length);
                store.sAdd(key, values);
                writeToFile(line);

                return "OK";
            }
            case "SREM" -> {
                var values = Arrays.copyOfRange(commands, 2, commands.length);

                store.sRem(key, values);
                writeToFile(line);
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
            case "EXPIRE", "EXP" -> {

                // Start simple parse the input, add to queue and write to file.
                // Error handling next iteration
                if (commands.length < 3) return "ERR missing arguments";

                long value = Long.parseLong(commands[2]);
                long expiryTimestamp = System.currentTimeMillis() + (value * 1000);


                ExpiringKey expiringKey = new ExpiringKey(key, expiryTimestamp);

                expireQueue.add(expiringKey);

                // Format file write line Current timestamp + given time

                commands[2] = Long.toString(expiryTimestamp);

                String newLine = String.join(" ", commands);

                writeToFile(newLine);

                return String.format("Expires in %d seconds", value);
            }

            default -> {
                return "ERR Unknown Command";
            }
        }
    }

    public void close() throws IOException {
        fileWriter.close();
    }

    private String[] parseOperation(String line) {
        return line.trim().split(" ");
    }

    private void writeToFile(String line) {
        try {
            fileWriter.write(line + "\n");
            fileWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) {
        try (
                // Create a ServerSocket listening on port 6379
                ServerSocket ss = new ServerSocket(6379);
                ExecutorService executor = Executors.newFixedThreadPool(10)
        ) {
            Server server = new Server();
            System.out.println("Starting server...");

            System.out.println("server.Server started listening on port 6379... ");

            // Shutdown hook to close server.Server file writer
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
                System.out.println("Client connected");

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
