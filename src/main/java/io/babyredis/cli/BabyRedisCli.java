package io.babyredis.cli;


import io.babyredis.BabyRedisClient;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * A simple command-line interface (CLI) for interacting with a Baby Redis server.
 * The CLI allows users to enter Redis commands and see the responses from the server.
 * It provides a help command to display available commands and their descriptions,
 * and a quit command to disconnect from the server.
 * The CLI uses a BabyRedisClient instance to send commands to the server and handle the communication.
 */

public class BabyRedisCli {

    /**
     * Displays a help message with the available commands and their descriptions.
     * This method is called when the user types "HELP" in the CLI,
     * providing guidance on how to use the various Redis commands supported by the Baby Redis server.
     */
    public static void help() {
        System.out.println("=== Baby Redis ===");
        System.out.println("String commands:");
        System.out.println("  SET <key> <value>        Store a value");
        System.out.println("  GET <key>                Retrieve a value");
        System.out.println("  DELETE <key>             Remove a key");
        System.out.println(" ");
        System.out.println("Set commands:");
        System.out.println("  SADD <key> <val> [val]   Add values to a set");
        System.out.println("  SREM <key> <val> [val]   Remove values from a set");
        System.out.println("  SISMEMBER/SIM <key> <val>    Check if value is in set");
        System.out.println("  SMEMBERS/SM <key>           List all values in set");
        System.out.println(" ");
        System.out.println("Expiry commands:");
        System.out.println(" EXPIRE <key> <seconds>     Sets expiry countdown");
        System.out.println(" TTL <key>                  Shows how long until key expires");
        System.out.println("  HELP                     Show this message");
        System.out.println("  QUIT                     Disconnect");
    }

    /**
     * The main method that runs the Baby Redis CLI. It creates a BabyRedisClient instance to connect to the Redis server, and then enters a loop to read user input from the command line.
     * The user can enter Redis commands, which are sent to the server using the BabyRedisClient instance.
     * The CLI also handles special commands like "HELP" to display the help message and "QUIT" to disconnect from the
     * server. The CLI validates the commands entered by the user and prints the responses received from the server.
     *
     * @param args Command-line arguments (not used in this application).
     */
    public static void main(String[] args) {
        BabyRedisClient client = new BabyRedisClient("localhost", 6379);

        List<String> allowedCommands =
                Arrays.asList("SET", "GET", "DELETE", "QUIT", "SADD", "SREM", "SISMEMBER", "SMEMBERS", "SIM", "SM", "EXPIRE", "EXP", "TTL");


        Scanner scanner = new Scanner(System.in);

        BabyRedisCli.help();

        while (scanner.hasNext()) {
            String line = scanner.nextLine();

            // Show help if user types HELP, continue to next iteration to avoid sending HELP command to server
            if (line.equalsIgnoreCase("HELP")) {
                BabyRedisCli.help();
                continue;
            }

            String[] parts = line.split("\\s+");
            String command = parts[0].toUpperCase();

            // Handle QUIT command to close the client connection and exit the loop
            if (command.equals("QUIT")) {
                client.close();
                break;
            }

            // Validate the command against the list of allowed commands. If the command is not recognized, print an error message and prompt the user again.
            if (!allowedCommands.contains(command)) {
                System.out.println("Unknown command. Type HELP.");
                continue;
            }
            // Send the valid command to the Baby Redis server using the BabyRedisClient instance and print the response received from the server.
            String response = client.send(line);
            System.out.println(response);
        }

    }
}
