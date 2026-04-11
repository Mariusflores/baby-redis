import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final InMemoryStore store = new InMemoryStore();

    private String delegate(String[] commands) {
        if (commands.length < 2) {
            return "ERR expected at least <2> arguments";
        }
        // Input format i.e. SET hello world
        String command = commands[0];
        String key = commands[1];


        switch (command.toUpperCase()) {
            case "SET" -> {
                StringBuilder builder = new StringBuilder();
                var values = Arrays.copyOfRange(commands, 2, commands.length);

                if (commands.length > 2) {
                    for (String value : values) {
                        builder.append(value).append(" ");
                    }

                }
                if (builder.toString().isEmpty()) {
                    return "ERR Value isnt provided";
                }
                store.set(key, builder.toString().trim());
                return "OK";
            }
            case "GET" -> {
                String result = store.get(key);
                return result == null ? "NOT FOUND" : result;
            }
            case "DELETE" -> {
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
                    return "ERR Value isnt provided";

                }
                return store.sIsMember(key, value) ? "TRUE" : "FALSE";

            }
            case "SMEMBERS", "SM" -> {

                var set = store.sMembers(key);

                return String.join(",", set);
            }

            default -> {
                return "ERR Unknown Command";
            }
        }
    }


    public static void main(String[] args) {
        Server server = new Server();

        System.out.println("Starting server...");

        try (
                ServerSocket ss = new ServerSocket(6379);
                ExecutorService executor = Executors.newFixedThreadPool(10)
        ) {
            // Create a ServerSocket listening on port 6379

            System.out.println("Server started listening on port 6379... ");


            //noinspection InfiniteLoopStatement
            while (true) {


                // Accept a connection from a client
                Socket s = ss.accept();
                System.out.println("Client connected");
                //Declare a file writer
                BufferedWriter fileWriter = new BufferedWriter(new FileWriter("queries.txt"));

                Runnable task = () -> {
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

                            fileWriter.write(line + "\n");
                            String[] commands = line.trim().split(" ");

                            if (commands.length == 1 && commands[0].equalsIgnoreCase("QUIT")) {
                                // Close connections
                                break;
                            }

                            String result = server.delegate(commands);

                            out.println(result);

                        }

                        reader.close();
                        fileWriter.close();
                        out.close();
                        s.close();
                    } catch (IOException e) {
                        System.out.println("Error: " + e);
                    }


                };

                executor.submit(task);


            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
