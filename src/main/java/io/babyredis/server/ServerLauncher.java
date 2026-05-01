package io.babyredis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerLauncher {
    private static final Logger log = LoggerFactory.getLogger(ServerLauncher.class);

    public static void main(String[] args) {
        try (
                // Create a ServerSocket listening on port 6379
                ServerSocket ss = new ServerSocket(6379);
                // Create an executor service for handling client execution threads
                ExecutorService executor = Executors.newFixedThreadPool(10)
        ) {
            BabyRedisServer server = new BabyRedisServer();
            log.info("Server started listening on port 6379");

            // Shutdown hook to save snapshot and close connections
            Thread closeServerHook = new Thread(() -> {
                try {
                    server.close();
                } catch (IOException e) {
                    log.error("Shutdown error", e);
                }
            });
            Runtime.getRuntime().addShutdownHook(closeServerHook);

            while (true) {

                // Accept a connection from a client
                Socket s = ss.accept();
                server.addSocket(s);
                log.info("Client connected");

                Runnable task = handleClient(s, server);

                executor.submit(task);
            }
        } catch (IOException e) {
            log.error("Error connecting to client", e);
        }
    }

    private static Runnable handleClient(Socket s, BabyRedisServer server) {
        return () -> {
            try (      // Declare a buffered reader
                       BufferedReader reader = new BufferedReader(
                               new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                       // Declare an Output Writer
                       PrintWriter out = new PrintWriter(
                               new OutputStreamWriter(s.getOutputStream()), true
                       )
            ) {


                String line;

                while ((line = reader.readLine()) != null) {
                    log.debug("Command: {}", line);

                    if (line.trim().equalsIgnoreCase("QUIT")) {
                        break;
                    }
                    String result = server.execute(line);

                    out.print(result);
                    out.flush();

                }

            } catch (IOException e) {
                log.error("Client handler error: ", e);
            } finally {
                // Assures socket closes even though exception is thrown
                try {

                    server.closeSocket(s);
                } catch (IOException e) {
                    log.error("Error closing socket", e);
                }
            }


        };
    }
}
